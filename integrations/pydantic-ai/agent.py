"""
Pydantic AI booking agent exposing an AG-UI-compatible SSE stream at GET /events.

Wire contract (matches `:a2cui-agui-mock-server`):

  RUN_STARTED
  TEXT_MESSAGE_START / TEXT_MESSAGE_CONTENT / TEXT_MESSAGE_END
  CUSTOM  { name: "a2ui", value: createSurface(...)      }
  CUSTOM  { name: "a2ui", value: updateComponents(...)   }
  CUSTOM  { name: "a2ui", value: updateDataModel(...)    }
  RUN_FINISHED

Each CUSTOM carries ONE A2UI v0.9 frame. A2CUI's Kotlin bridge converts `a2ui`
CUSTOM events to `A2uiFrame`s and renders them via Compose.

Run:
    pip install -r requirements.txt
    python agent.py
    # → GET http://localhost:8000/events

Optional LLM narration (the A2UI frames themselves are deterministic):
    export OPENAI_API_KEY=sk-...          # or ANTHROPIC_API_KEY, etc.
    export A2UI_MODEL=openai:gpt-4o-mini  # any pydantic-ai model id
"""

from __future__ import annotations

import asyncio
import json
import os
import random
from typing import Any, AsyncIterator, Optional

from fastapi import FastAPI
from sse_starlette.sse import EventSourceResponse

SURFACE_ID = os.environ.get("A2UI_SURFACE_ID", "demo")
FRAME_DELAY_MS = int(os.environ.get("A2UI_FRAME_DELAY_MS", "300"))

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
                {"id": "sub", "component": "Text", "text": "Live agent demo — Pydantic AI.", "variant": "body"},
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

# --- Optional pydantic-ai narration ------------------------------------------

async def narrate() -> str:
    """Generate the assistant pre-frame narration. Falls back when no key set."""
    model_id = os.environ.get("A2UI_MODEL")
    if not model_id:
        return "Rendering booking form…"
    try:
        from pydantic_ai import Agent  # type: ignore
        agent = Agent(model_id, system_prompt=(
            "You narrate UI rendering in one short sentence, past-progressive. "
            "Never exceed 12 words."
        ))
        result = await agent.run("The client is about to render a booking form.")
        return str(result.output).strip() or "Rendering booking form…"
    except Exception as exc:  # network, auth, import, model errors
        return f"Rendering booking form… (narration skipped: {exc.__class__.__name__})"

# --- AG-UI SSE event helpers -------------------------------------------------

def _evt(type_: str, **fields: Any) -> dict[str, str]:
    """Wrap a JSON payload as an SSE event dict for sse-starlette."""
    return {"data": json.dumps({"type": type_, **fields})}


async def agui_stream() -> AsyncIterator[dict[str, str]]:
    thread_id = f"thread-{random.randint(1, 1_000_000)}"
    run_id = f"run-{random.randint(1, 1_000_000)}"
    message_id = "msg-1"

    yield _evt("RUN_STARTED", threadId=thread_id, runId=run_id)
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    text = await narrate()
    yield _evt("TEXT_MESSAGE_START", messageId=message_id, role="assistant")
    yield _evt("TEXT_MESSAGE_CONTENT", messageId=message_id, delta=text)
    yield _evt("TEXT_MESSAGE_END", messageId=message_id)
    await asyncio.sleep(FRAME_DELAY_MS / 1000)

    for frame in (
        create_surface(SURFACE_ID),
        booking_components(SURFACE_ID),
        seed_email(SURFACE_ID),
    ):
        yield _evt("CUSTOM", name="a2ui", value=frame)
        await asyncio.sleep(FRAME_DELAY_MS / 1000)

    yield _evt("RUN_FINISHED", threadId=thread_id, runId=run_id, outcome="success")

# --- FastAPI app -------------------------------------------------------------

app = FastAPI(title="A2CUI Pydantic AI Agent")


@app.get("/events")
async def events() -> EventSourceResponse:
    return EventSourceResponse(agui_stream())


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "surfaceId": SURFACE_ID}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8000")))
