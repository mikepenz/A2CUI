"""
Google ADK → AG-UI → A2UI bridge with **free-form prompt generation**.

Users type a prompt like "login form with email, password, submit button" in
the Compose client. The server passes the prompt to a Gemini-backed ADK agent.
The agent replies with a short narration, then calls the `emit_ui` tool with a
list of A2UI ComponentNodes describing the requested UI. The server forwards
those nodes as a CUSTOM(name=a2ui) event carrying one `updateComponents` frame.

Endpoints
---------

    GET  /events?prompt=<url-encoded>    # streams SSE for the given prompt
    GET  /events                         # streams SSE for the default demo
    GET  /health

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

from fastapi import FastAPI, Query
from sse_starlette.sse import EventSourceResponse

SURFACE_ID = os.environ.get("A2UI_SURFACE_ID", "demo")
FRAME_DELAY_MS = int(os.environ.get("A2UI_FRAME_DELAY_MS", "200"))
MODEL_ID = os.environ.get("A2UI_MODEL", "gemini-flash-latest")


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
  2. Call the `emit_ui` tool exactly once with the full component list.
  3. Stop.

Never invent component types outside the allowed set. Never omit "id" or "component".
"""


# --- ADK tool ---------------------------------------------------------------

def emit_ui(components: list[dict[str, Any]], title: str = "") -> dict[str, Any]:
    """Render the given A2UI component list on the client surface.

    Args:
        components: A list of A2UI v0.9 ComponentNode objects. Must include
            exactly one node with id="root". Every child id must exist as a
            sibling. Only components from the Material 3 basic catalog.
        title: Optional human-readable title for logging. Not rendered.

    Returns:
        A frames list the server will forward to the client as CUSTOM(a2ui).
    """
    repaired, warnings = _repair_components(components)
    return {
        "surfaceId": SURFACE_ID,
        "title": title,
        "components": repaired,
        "warnings": warnings,
    }


def _repair_components(components: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[str]]:
    """Best-effort normalisation of model output before it hits the client.

    - Drops nodes missing `id`.
    - Maps common aliases for `component`: `type`, `component_type`, `componentType`.
    - Drops nodes whose component isn't in the allowed catalog; strips those ids from any
      `children` array so surviving parents don't reference missing nodes.
    - Ensures exactly one `root`; promotes the first node if needed.
    """
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
        warnings.append(f"Promoted first node to id='root'")

    return fixed, warnings


# --- ADK-driven streaming ---------------------------------------------------

async def _adk_stream_for_prompt(prompt: str) -> AsyncIterator[tuple[str, Any]]:
    """Yield `(kind, payload)` tuples from a live ADK run.

    kind ∈ {"text", "a2ui"}: text deltas or A2UI frames.
    Falls back to a deterministic script when ADK isn't available.
    """
    try:
        from google.adk.agents import Agent  # type: ignore
        from google.adk.runners import Runner  # type: ignore
        from google.adk.sessions import InMemorySessionService  # type: ignore
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
    session_service = InMemorySessionService()
    app_name = "a2cui-adk-demo"
    user_id = "u"
    session_id = f"s-{random.randint(1, 1_000_000)}"
    await session_service.create_session(app_name=app_name, user_id=user_id, session_id=session_id)

    runner = Runner(agent=agent, app_name=app_name, session_service=session_service)
    message = types.Content(role="user", parts=[types.Part(text=prompt)])

    try:
        async for event in runner.run_async(
            user_id=user_id,
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


async def agui_stream(prompt: Optional[str]) -> AsyncIterator[dict[str, str]]:
    thread_id = f"thread-{random.randint(1, 1_000_000)}"
    run_id = f"run-{random.randint(1, 1_000_000)}"
    message_id = "msg-1"

    yield _evt("RUN_STARTED", threadId=thread_id, runId=run_id)
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    # Every run first resets the surface so successive prompts don't accumulate state.
    yield _evt("CUSTOM", name="a2ui", value=create_surface(SURFACE_ID))
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    message_open = False
    saw_text = False

    async for kind, payload in _adk_stream_for_prompt(prompt or "Render the default booking form."):
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
async def events(prompt: Optional[str] = Query(default=None)) -> EventSourceResponse:
    return EventSourceResponse(agui_stream(prompt))


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "surfaceId": SURFACE_ID, "model": MODEL_ID}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8100")))
