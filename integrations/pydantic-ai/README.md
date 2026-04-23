# A2CUI x Pydantic AI

A minimal [`pydantic-ai`](https://ai.pydantic.dev/) agent that serves an **AG-UI**-compatible SSE stream
at `GET /events`, drop-in replacement for `:a2cui-agui-mock-server`.

The stream emits:

1. `RUN_STARTED`
2. `TEXT_MESSAGE_START` / `_CONTENT` / `_END` — a short assistant narration
   (optionally LLM-generated via Pydantic AI)
3. Three `CUSTOM` events (`name: "a2ui"`) carrying A2UI v0.9 frames:
   `createSurface`, `updateComponents` (booking form), `updateDataModel`
4. `RUN_FINISHED`

The A2UI payloads are identical in shape to the Kotlin mock — the A2CUI client renders
a booking form with email, name, subscribe checkbox, and submit button.

## Prerequisites

- Python >= 3.10
- (optional) an LLM API key for narration: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, etc.

## Install

```bash
cd integrations/pydantic-ai
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

## Run

```bash
# deterministic narration (no API key needed)
python agent.py

# with an LLM for narration
export OPENAI_API_KEY=sk-...
export A2UI_MODEL=openai:gpt-4o-mini
python agent.py
```

Verify:

```bash
curl -N http://localhost:8000/events
```

Expected: a sequence of `data: {"type":"RUN_STARTED",...}` lines followed by
`CUSTOM` events with embedded A2UI frames, ending with `RUN_FINISHED`.

## Environment

| Var | Default | Purpose |
|---|---|---|
| `PORT` | `8000` | HTTP port |
| `A2UI_SURFACE_ID` | `demo` | Surface id inside A2UI frames — must match client |
| `A2UI_FRAME_DELAY_MS` | `300` | Delay between consecutive AG-UI events |
| `A2UI_MODEL` | *(unset)* | Pydantic AI model id (e.g. `openai:gpt-4o-mini`); unset = static narration |
| `OPENAI_API_KEY` / ... | — | Credentials for the chosen model provider |

## Point A2CUI at it

```bash
AGUI_URL=http://localhost:8000/events ./gradlew :a2cui-sample-live:run
```

## Extending the frame script

Edit the A2UI builders at the top of `agent.py` (`create_surface`, `booking_components`,
`seed_email`) or push a Pydantic AI **tool** that returns an A2UI frame dict and
wrap the result in a `CUSTOM` event in `agui_stream()`.
