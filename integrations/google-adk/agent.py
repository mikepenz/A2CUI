"""
Google ADK booking agent exposing an AG-UI-compatible SSE stream at GET /events.

Wire contract (matches `:a2cui-agui-mock-server` and the Pydantic AI sibling):

  RUN_STARTED
  TEXT_MESSAGE_START / TEXT_MESSAGE_CONTENT / TEXT_MESSAGE_END
  CUSTOM  { name: "a2ui", value: createSurface(...)      }
  CUSTOM  { name: "a2ui", value: updateComponents(...)   }
  CUSTOM  { name: "a2ui", value: updateDataModel(...)    }
  RUN_FINISHED

Each CUSTOM carries ONE A2UI v0.9 frame. A2CUI's Kotlin bridge converts `a2ui`
CUSTOM events into `A2uiFrame`s and renders them via Compose.

How it works
------------

Google ADK has no built-in AG-UI adapter (as of the time of writing). Agents are
driven through `Runner.run_async()`, which yields ADK `Event`s. We:

1. Register a tool `render_booking_form(surface_id)` on the ADK agent. When the
   model calls the tool, the tool returns the three A2UI frames it wants
   rendered (createSurface / updateComponents / updateDataModel).
2. We iterate ADK's event stream. Text deltas become AG-UI `TEXT_MESSAGE_*`
   events; tool-result events carrying A2UI frames become AG-UI `CUSTOM` events
   with `name: "a2ui"`.

If no Gemini credentials are available, a deterministic fallback narration and
frames are streamed so the demo still runs offline.

Run
---

    pip install -r requirements.txt
    export GOOGLE_API_KEY=...          # OR: use `gcloud auth application-default login`
    export A2UI_MODEL=gemini-flash-latest
    python agent.py
    # → GET http://localhost:8100/events

Env vars
--------

    A2UI_SURFACE_ID       default "demo"
    A2UI_FRAME_DELAY_MS   inter-frame pacing (default 300ms)
    A2UI_MODEL            ADK model id (default "gemini-flash-latest")
    PORT                  default 8100

Point `:a2cui-sample-live` at this server with:

    AGUI_URL=http://localhost:8100/events ./gradlew :a2cui-sample-live:run
"""

from __future__ import annotations

import asyncio
import json
import os
import random
from typing import Any, AsyncIterator

from fastapi import FastAPI
from sse_starlette.sse import EventSourceResponse

SURFACE_ID = os.environ.get("A2UI_SURFACE_ID", "demo")
FRAME_DELAY_MS = int(os.environ.get("A2UI_FRAME_DELAY_MS", "300"))
MODEL_ID = os.environ.get("A2UI_MODEL", "gemini-flash-latest")

# --- A2UI v0.9 frame builders (mirror SampleA2uiFrames.kt) -------------------

def create_surface(surface_id: str) -> dict[str, Any]:
    return {
        "version": "v0.9",
        "createSurface": {
            "surfaceId": surface_id,
            "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
        },
    }


def booking_components(surface_id: str) -> dict[str, Any]:
    return {
        "version": "v0.9",
        "updateComponents": {
            "surfaceId": surface_id,
            "components": [
                {"id": "root", "component": "Column", "spacing": 12,
                 "children": ["title", "sub", "email", "name", "subscribe", "submit"]},
                {"id": "title", "component": "Text", "text": "Book your table", "variant": "h2"},
                {"id": "sub", "component": "Text", "text": "Live agent demo — Google ADK.", "variant": "body"},
                {"id": "email", "component": "TextField", "label": "Email", "value": {"path": "/form/email"}},
                {"id": "name", "component": "TextField", "label": "Name", "value": {"path": "/form/name"}},
                {"id": "subscribe", "component": "CheckBox", "label": "Email me the receipt",
                 "value": {"path": "/form/subscribe"}},
                {"id": "submit", "component": "Button", "text": "Submit booking",
                 "action": {"event": {"name": "submit_booking", "context": {
                     "email": {"path": "/form/email"},
                     "name": {"path": "/form/name"},
                     "subscribe": {"path": "/form/subscribe"},
                 }}}},
            ],
        },
    }


def seed_email(surface_id: str) -> dict[str, Any]:
    return {
        "version": "v0.9",
        "updateDataModel": {
            "surfaceId": surface_id,
            "path": "/form/email",
            "value": "hello@example.com",
        },
    }


BOOKING_FRAMES = lambda sid=SURFACE_ID: [  # noqa: E731
    create_surface(sid),
    booking_components(sid),
    seed_email(sid),
]

# --- ADK tool -----------------------------------------------------------------
# The tool is a plain Python function; ADK introspects its signature + docstring
# to expose it to the model. When the agent decides to call the tool, ADK emits
# a tool-call event followed by a tool-result event carrying this return value.

def render_booking_form(surface_id: str = SURFACE_ID) -> dict[str, Any]:
    """Render a booking form on the A2CUI client surface. Returns the A2UI frames
    the client should apply, in order. Call this when the user asks to book a
    table / room / appointment.
    """
    return {"surfaceId": surface_id, "frames": BOOKING_FRAMES(surface_id)}

# --- ADK-driven streaming ----------------------------------------------------

async def _adk_stream_frames() -> AsyncIterator[tuple[str, Any]]:
    """Yield `(kind, payload)` tuples from a live ADK run.

    kind ∈ {"text", "a2ui"}:
      - ("text", delta) for assistant text chunks
      - ("a2ui", frame_dict) for each A2UI frame emitted by the tool

    Falls back to a deterministic script if ADK is unavailable or not configured.
    """
    try:
        from google.adk.agents import Agent  # type: ignore
        from google.adk.runners import Runner  # type: ignore
        from google.adk.sessions import InMemorySessionService  # type: ignore
        from google.genai import types  # type: ignore
    except ImportError:
        async for item in _fallback_stream():
            yield item
        return

    # Build the agent.
    agent = Agent(
        name="a2cui_booking_agent",
        model=MODEL_ID,
        instruction=(
            "You are an assistant that renders interactive UIs via the A2UI "
            "protocol. When asked to book a table, first say one short sentence "
            "acknowledging the request, then call `render_booking_form`. Keep "
            "narration under 15 words."
        ),
        tools=[render_booking_form],
    )

    session_service = InMemorySessionService()
    app_name = "a2cui-adk-demo"
    user_id = "u"
    session_id = f"s-{random.randint(1, 1_000_000)}"
    await session_service.create_session(app_name=app_name, user_id=user_id, session_id=session_id)

    runner = Runner(agent=agent, app_name=app_name, session_service=session_service)
    prompt = types.Content(
        role="user",
        parts=[types.Part(text="Please book me a table.")],
    )

    try:
        async for event in runner.run_async(
            user_id=user_id,
            session_id=session_id,
            new_message=prompt,
        ):
            # ADK events carry either streamed model text, tool calls, or tool results.
            # See https://google.github.io/adk-docs/events for the full shape.
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
                    # Accept either `{frames: [...]}` from our tool or a bare frame.
                    frames = payload.get("frames") if isinstance(payload, dict) else None
                    if frames:
                        for frame in frames:
                            yield ("a2ui", frame)
                    elif isinstance(payload, dict) and "version" in payload:
                        yield ("a2ui", payload)
    except Exception as exc:  # auth, network, model errors — never break the SSE stream
        yield ("text", f"(ADK stream aborted: {exc.__class__.__name__}; falling back.) ")
        async for item in _fallback_stream():
            yield item


async def _fallback_stream() -> AsyncIterator[tuple[str, Any]]:
    yield ("text", "Rendering booking form… ")
    for frame in BOOKING_FRAMES():
        yield ("a2ui", frame)


# --- AG-UI SSE envelope ------------------------------------------------------

def _evt(type_: str, **fields: Any) -> dict[str, str]:
    return {"data": json.dumps({"type": type_, **fields})}


async def agui_stream() -> AsyncIterator[dict[str, str]]:
    thread_id = f"thread-{random.randint(1, 1_000_000)}"
    run_id = f"run-{random.randint(1, 1_000_000)}"
    message_id = "msg-1"

    yield _evt("RUN_STARTED", threadId=thread_id, runId=run_id)
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    message_open = False
    saw_any_text = False
    async for kind, payload in _adk_stream_frames():
        if kind == "text":
            if not message_open:
                yield _evt("TEXT_MESSAGE_START", messageId=message_id, role="assistant")
                message_open = True
            yield _evt("TEXT_MESSAGE_CONTENT", messageId=message_id, delta=payload)
            saw_any_text = True
        elif kind == "a2ui":
            if message_open:
                yield _evt("TEXT_MESSAGE_END", messageId=message_id)
                message_open = False
                await asyncio.sleep(FRAME_DELAY_MS / 1000)
            yield _evt("CUSTOM", name="a2ui", value=payload)
            await asyncio.sleep(FRAME_DELAY_MS / 1000)

    if message_open:
        yield _evt("TEXT_MESSAGE_END", messageId=message_id)
    if not saw_any_text:
        # Guarantee a TEXT envelope so clients don't think the run was silent.
        yield _evt("TEXT_MESSAGE_START", messageId=message_id, role="assistant")
        yield _evt("TEXT_MESSAGE_CONTENT", messageId=message_id, delta="Done.")
        yield _evt("TEXT_MESSAGE_END", messageId=message_id)

    yield _evt("RUN_FINISHED", threadId=thread_id, runId=run_id, outcome="success")


# --- FastAPI app -------------------------------------------------------------

app = FastAPI(title="A2CUI Google ADK Agent")


@app.get("/events")
async def events() -> EventSourceResponse:
    return EventSourceResponse(agui_stream())


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "surfaceId": SURFACE_ID, "model": MODEL_ID}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8100")))
