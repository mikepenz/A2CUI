"""
Google ADK → AG-UI → A2UI bridge with **free-form prompt generation** and
**round-trip client actions** on a shared multi-turn session.

Users type a prompt like "login form with email, password, submit button" in
the Compose client. The server passes the prompt to a Gemini-backed ADK agent.
The agent replies with a short narration, then calls the `emit_ui` tool with a
list of A2UI ComponentNodes describing the requested UI. The server forwards
those nodes as a CUSTOM(name=a2ui) event carrying one `updateComponents` frame.

When the user later clicks a Button / submits a form, the Compose client POSTs
an ``A2uiClientMessage.Action`` envelope to ``POST /events``. The same ADK
session is reused (keyed off an ``X-Thread-Id`` header), so the agent can
refine the UI conversationally.

Endpoints
---------

    GET  /events?prompt=<url-encoded>    # streams SSE for the given prompt
    GET  /events                         # streams SSE for the default demo
    POST /events                         # action body; streams SSE response
    GET  /health

Every response includes the resolved ``X-Thread-Id`` header (echoing the
request header when supplied, otherwise generating a fresh one). Clients
should persist this value for the duration of a conversation.

Env vars
--------

    A2UI_SURFACE_ID       default "demo"
    A2UI_FRAME_DELAY_MS   pacing between emitted frames (default 200ms)
    A2UI_MODEL            ADK model id (default "gemini-flash-latest")
    GOOGLE_API_KEY        Gemini API key (free tier at https://aistudio.google.com)
    PORT                  default 8100
"""

from __future__ import annotations

import asyncio
import json
import os
import random
from typing import Any, AsyncIterator, Optional

from fastapi import FastAPI, Header, Query, Request
from fastapi.responses import JSONResponse
from sse_starlette.sse import EventSourceResponse

SURFACE_ID = os.environ.get("A2UI_SURFACE_ID", "demo")
FRAME_DELAY_MS = int(os.environ.get("A2UI_FRAME_DELAY_MS", "200"))
MODEL_ID = os.environ.get("A2UI_MODEL", "gemini-flash-latest")

# In-memory thread-id -> ADK session-id store. A single process instance is
# assumed; persistence across restarts is out of scope for this example.
THREAD_SESSIONS: dict[str, str] = {}


# --- A2UI v0.9 frame builders -----------------------------------------------

def create_surface(surface_id: str) -> dict[str, Any]:
    return {
        "version": "v0.9",
        "createSurface": {
            "surfaceId": surface_id,
            "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
        },
    }


def update_components(surface_id: str, components: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "version": "v0.9",
        "updateComponents": {
            "surfaceId": surface_id,
            "components": components,
        },
    }


def default_booking_components(surface_id: str) -> dict[str, Any]:
    return update_components(surface_id, [
        {"id": "root", "component": "Column", "spacing": 12,
         "children": ["title", "sub", "email", "name", "submit"]},
        {"id": "title", "component": "Text", "text": "Book your table", "variant": "h2"},
        {"id": "sub", "component": "Text", "text": "Default demo — submit a prompt to regenerate.", "variant": "body"},
        {"id": "email", "component": "TextField", "label": "Email", "value": {"path": "/form/email"}},
        {"id": "name", "component": "TextField", "label": "Name", "value": {"path": "/form/name"}},
        {"id": "submit", "component": "Button", "text": "Submit booking",
         "action": {"event": {"name": "submit_booking", "context": {
             "email": {"path": "/form/email"},
             "name": {"path": "/form/name"},
         }}}},
    ])


# --- Catalog description handed to the model --------------------------------

ALLOWED_COMPONENTS = {
    "Text", "Column", "Row", "Card", "Button", "TextField", "CheckBox", "Slider",
    "ChoicePicker", "DateTimeInput", "List", "Tabs", "Modal", "Image", "Icon",
}

CATALOG_SYSTEM_PROMPT = """You render interactive UIs by emitting A2UI v0.9 component nodes.

Hard rules (the client rejects any node that breaks these):

  * Every node MUST have these exact string keys:
      - "id"         (unique, lowercase, kebab-case)
      - "component"  (one of the allowed types below, case-sensitive)
  * Child nodes are referenced by id in the "children" array — NEVER nested inline.
  * Exactly one node has id = "root".
  * Every id listed in any "children" array must also appear as a sibling node.
  * DO NOT use "type" as a synonym for "component". The key is literally "component".

Allowed component types (Material 3 basic catalog) — emit ONLY these:

  Text         props: text (str, required), variant (one of h1,h2,h3,title,body,label,caption)
  Column       props: spacing (int, dp). children: [ids]
  Row          props: spacing (int, dp). children: [ids]
  Card         children: [ids]
  Button       props: text (str, required), variant (optional: outlined/text),
                      enabled (bool). Emits events via `action.event.name` with optional context.
  TextField    props: label (str), value ({"path":"/ptr"} binds to data model)
  CheckBox     props: label (str), value ({"path":"/ptr"})
  Slider       props: min, max, step (int). value ({"path":"/ptr"})
  ChoicePicker props: label (str), selected ({"path":"/ptr"}),
                      choices [{value, label}, ...]
  DateTimeInput props: label, value ({"path":"/ptr"}), format (date|time|datetime)
  List         props: spacing, items ({"path":"/arrayPointer"}). children: [templateId]
  Tabs         props: titles: [str]. children: [ids] (one per tab)
  Modal        props: title (str), open ({"path":"/ptr"}). children: [ids]
  Image        props: src (str, url), contentDescription (str)
  Icon         props: name (str)

Canonical example — emit this SHAPE verbatim (change content to match the user's request):

  [
    {"id": "root",  "component": "Column", "spacing": 12, "children": ["title","email","submit"]},
    {"id": "title", "component": "Text", "text": "Sign in", "variant": "h2"},
    {"id": "email", "component": "TextField", "label": "Email", "value": {"path":"/form/email"}},
    {"id": "submit","component": "Button", "text": "Sign in",
     "action": {"event": {"name": "submit", "context": {"email": {"path":"/form/email"}}}}}
  ]

Workflow for any request:
  1. Reply with ONE short narration sentence (< 15 words).
  2. If the request (or the user-triggered action it describes) asks for more UI,
     call the `emit_ui` tool exactly once with the full component list.
  3. Stop.

Never invent component types outside the allowed set. Never omit "id" or "component".
"""


# --- ADK tool ---------------------------------------------------------------

def emit_ui(components: list[dict[str, Any]], title: str = "") -> dict[str, Any]:
    """Render the given A2UI component list on the client surface."""
    repaired, warnings = _repair_components(components)
    return {
        "surfaceId": SURFACE_ID,
        "title": title,
        "components": repaired,
        "warnings": warnings,
    }


def _repair_components(components: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[str]]:
    warnings: list[str] = []
    fixed: list[dict[str, Any]] = []
    for raw in components:
        if not isinstance(raw, dict):
            warnings.append(f"Dropped non-object entry: {raw!r}")
            continue
        node = dict(raw)
        if "component" not in node:
            for alias in ("type", "componentType", "component_type"):
                if alias in node:
                    node["component"] = node.pop(alias)
                    warnings.append(f"Renamed '{alias}' -> 'component' on id={node.get('id')}")
                    break
        if "id" not in node:
            warnings.append(f"Dropped node missing 'id': component={node.get('component')}")
            continue
        if "component" not in node:
            warnings.append(f"Dropped node missing 'component': id={node['id']}")
            continue
        if node["component"] not in ALLOWED_COMPONENTS:
            warnings.append(
                f"Dropped unknown component '{node['component']}' for id={node['id']}"
            )
            continue
        fixed.append(node)

    surviving_ids = {n["id"] for n in fixed}
    for node in fixed:
        raw_children = node.get("children")
        if isinstance(raw_children, list):
            kept = [c for c in raw_children if c in surviving_ids]
            if len(kept) != len(raw_children):
                dropped = [c for c in raw_children if c not in surviving_ids]
                warnings.append(f"Stripped dangling child refs {dropped} from id={node['id']}")
            node["children"] = kept

    if not any(n["id"] == "root" for n in fixed) and fixed:
        fixed[0]["id"] = "root"
        warnings.append("Promoted first node to id='root'")

    return fixed, warnings


# --- ADK-driven streaming ---------------------------------------------------

APP_NAME = "a2cui-adk-demo"
USER_ID = "u"


async def _resolve_session(session_service: Any, thread_id: str) -> str:
    """Return the ADK session id bound to ``thread_id``, creating one if new."""
    existing = THREAD_SESSIONS.get(thread_id)
    if existing is not None:
        return existing
    session_id = f"s-{random.randint(1, 1_000_000)}-{thread_id[-8:]}"
    await session_service.create_session(
        app_name=APP_NAME, user_id=USER_ID, session_id=session_id
    )
    THREAD_SESSIONS[thread_id] = session_id
    return session_id


# Shared per-process session service so multiple turns keyed on the same
# thread-id observe the same history.
_SESSION_SERVICE_SINGLETON: Any = None


def _session_service() -> Any:
    global _SESSION_SERVICE_SINGLETON
    if _SESSION_SERVICE_SINGLETON is None:
        from google.adk.sessions import InMemorySessionService  # type: ignore
        _SESSION_SERVICE_SINGLETON = InMemorySessionService()
    return _SESSION_SERVICE_SINGLETON


async def _adk_stream_for_prompt(prompt: str, thread_id: str) -> AsyncIterator[tuple[str, Any]]:
    """Yield ``(kind, payload)`` tuples from a live ADK run keyed off ``thread_id``.

    kind ∈ {"text", "a2ui"}. Falls back to a deterministic script when ADK is
    unavailable.
    """
    try:
        from google.adk.agents import Agent  # type: ignore
        from google.adk.runners import Runner  # type: ignore
        from google.genai import types  # type: ignore
    except ImportError:
        async for item in _fallback_stream(prompt):
            yield item
        return

    agent = Agent(
        name="a2cui_ui_generator",
        model=MODEL_ID,
        instruction=CATALOG_SYSTEM_PROMPT,
        tools=[emit_ui],
    )
    session_service = _session_service()
    session_id = await _resolve_session(session_service, thread_id)

    runner = Runner(agent=agent, app_name=APP_NAME, session_service=session_service)
    message = types.Content(role="user", parts=[types.Part(text=prompt)])

    try:
        async for event in runner.run_async(
            user_id=USER_ID,
            session_id=session_id,
            new_message=message,
        ):
            content = getattr(event, "content", None)
            if content is None:
                continue
            for part in getattr(content, "parts", []) or []:
                text = getattr(part, "text", None)
                if text:
                    yield ("text", text)
                fn_resp = getattr(part, "function_response", None)
                if fn_resp is not None:
                    payload = getattr(fn_resp, "response", None) or {}
                    if isinstance(payload, dict) and "components" in payload:
                        sid = payload.get("surfaceId") or SURFACE_ID
                        frame = update_components(sid, payload["components"])
                        yield ("a2ui", frame)
                    elif isinstance(payload, dict) and "version" in payload:
                        yield ("a2ui", payload)
    except Exception as exc:  # auth/network/model — never break the SSE stream
        yield ("text", f"(ADK stream error: {exc.__class__.__name__}; falling back.) ")
        async for item in _fallback_stream(prompt):
            yield item


async def _fallback_stream(prompt: str) -> AsyncIterator[tuple[str, Any]]:
    yield ("text", f"Rendering '{prompt or 'default demo'}' (offline fallback)… ")
    yield ("a2ui", default_booking_components(SURFACE_ID))


# --- AG-UI SSE envelope ------------------------------------------------------

def _evt(type_: str, **fields: Any) -> dict[str, str]:
    return {"data": json.dumps({"type": type_, **fields})}


def _ensure_thread_id(header_value: Optional[str]) -> str:
    if header_value and header_value.strip():
        return header_value.strip()
    return f"thread-{random.randint(1, 1_000_000_000)}"


async def agui_stream(
    prompt: Optional[str],
    thread_id: str,
    reset_surface: bool,
) -> AsyncIterator[dict[str, str]]:
    run_id = f"run-{random.randint(1, 1_000_000)}"
    message_id = f"msg-{random.randint(1, 1_000_000)}"

    yield _evt("RUN_STARTED", threadId=thread_id, runId=run_id)
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    if reset_surface:
        yield _evt("CUSTOM", name="a2ui", value=create_surface(SURFACE_ID))
        await asyncio.sleep(FRAME_DELAY_MS / 1000)

    message_open = False
    saw_text = False

    async for kind, payload in _adk_stream_for_prompt(
        prompt or "Render the default booking form.", thread_id
    ):
        if kind == "text":
            if not message_open:
                yield _evt("TEXT_MESSAGE_START", messageId=message_id, role="assistant")
                message_open = True
            yield _evt("TEXT_MESSAGE_CONTENT", messageId=message_id, delta=payload)
            saw_text = True
        elif kind == "a2ui":
            if message_open:
                yield _evt("TEXT_MESSAGE_END", messageId=message_id)
                message_open = False
                await asyncio.sleep(FRAME_DELAY_MS / 1000)
            yield _evt("CUSTOM", name="a2ui", value=payload)
            await asyncio.sleep(FRAME_DELAY_MS / 1000)

    if message_open:
        yield _evt("TEXT_MESSAGE_END", messageId=message_id)
    if not saw_text:
        yield _evt("TEXT_MESSAGE_START", messageId=message_id, role="assistant")
        yield _evt("TEXT_MESSAGE_CONTENT", messageId=message_id, delta="Done.")
        yield _evt("TEXT_MESSAGE_END", messageId=message_id)

    yield _evt("RUN_FINISHED", threadId=thread_id, runId=run_id, outcome="success")


# --- FastAPI app -------------------------------------------------------------

app = FastAPI(title="A2CUI Google ADK Agent — Prompt-driven UI Generator")


@app.get("/events")
async def events(
    prompt: Optional[str] = Query(default=None),
    x_thread_id: Optional[str] = Header(default=None, alias="X-Thread-Id"),
) -> EventSourceResponse:
    thread_id = _ensure_thread_id(x_thread_id)
    return EventSourceResponse(
        agui_stream(prompt, thread_id, reset_surface=True),
        headers={"X-Thread-Id": thread_id},
    )


def _action_to_prompt(body: dict[str, Any]) -> str:
    """Turn an ``A2uiClientMessage.Action`` envelope into a user-turn prompt."""
    action = body.get("action") if isinstance(body, dict) else None
    if not isinstance(action, dict):
        return "The user triggered an action. Continue the conversation."
    name = action.get("name", "unnamed")
    context = action.get("context", {})
    try:
        ctx_json = json.dumps(context, ensure_ascii=False, sort_keys=True)
    except (TypeError, ValueError):
        ctx_json = str(context)
    return (
        f"The user triggered the '{name}' action with context {ctx_json}. "
        "If the action asks for more UI, call emit_ui with the appropriate "
        "component list; otherwise acknowledge briefly."
    )


@app.post("/events")
async def events_post(
    request: Request,
    x_thread_id: Optional[str] = Header(default=None, alias="X-Thread-Id"),
) -> Any:
    try:
        body = await request.json()
    except Exception:
        return JSONResponse({"error": "invalid JSON body"}, status_code=400)
    if not isinstance(body, dict):
        return JSONResponse({"error": "expected object body"}, status_code=400)

    thread_id = _ensure_thread_id(x_thread_id)
    prompt = _action_to_prompt(body)
    # Follow-up turns don't reset the surface — the client keeps its current UI
    # and the agent patches it (or narrates) conversationally.
    return EventSourceResponse(
        agui_stream(prompt, thread_id, reset_surface=False),
        headers={"X-Thread-Id": thread_id},
    )


@app.get("/health")
async def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "surfaceId": SURFACE_ID,
        "model": MODEL_ID,
        "activeThreads": len(THREAD_SESSIONS),
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8100")))
