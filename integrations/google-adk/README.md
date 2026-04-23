# A2CUI × Google ADK

A live-agent backend that wires a [Google Agent Development Kit](https://adk.dev/)
agent to A2CUI's Compose client over AG-UI-compatible SSE.

ADK ships no built-in AG-UI adapter as of 2026-04. This example bridges the two
by wrapping each ADK run with a FastAPI + `sse-starlette` envelope that emits
`RUN_STARTED` / `TEXT_MESSAGE_*` / `CUSTOM(name=a2ui)` / `RUN_FINISHED` events.
A2CUI's `AguiA2cuiBridge` picks them up and renders the A2UI frames.

---

## What the agent does

- Defines one Gemini-backed `google.adk.agents.Agent` named `a2cui_booking_agent`.
- Registers a single tool `render_booking_form(surface_id)` which returns three
  A2UI frames (`createSurface`, `updateComponents`, `updateDataModel`).
- On every HTTP request to `GET /events`, starts a fresh ADK run, streams the
  model's text as `TEXT_MESSAGE_*` AG-UI events, and streams the tool-result
  frames as `CUSTOM { name: "a2ui", value: <frame> }` events.

If ADK isn't installed or no API credentials are set, a deterministic fallback
stream runs so the demo still works.

---

## Prerequisites

- Python 3.11 or newer.
- Either a Gemini API key (`GOOGLE_API_KEY`) **or** `gcloud auth
  application-default login` for Vertex AI.
- Network access to the Gemini endpoint.

---

## Setup

```bash
cd integrations/google-adk
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Authenticate

Pick **one**:

```bash
# Option A — Gemini API (quickstart)
export GOOGLE_API_KEY=AIza...

# Option B — Vertex AI (enterprise / gcloud)
gcloud auth application-default login
export GOOGLE_GENAI_USE_VERTEXAI=true
export GOOGLE_CLOUD_PROJECT=your-project-id
export GOOGLE_CLOUD_LOCATION=us-central1
```

## Run

```bash
python agent.py
# → Listening on http://localhost:8100/events
```

Smoke-test with curl — you should see a `RUN_STARTED` event arrive within a
second:

```bash
curl -N http://localhost:8100/events
```

## Connect A2CUI to it

In a second shell, point `:a2cui-sample-live` at the server:

```bash
cd ../..
AGUI_URL=http://localhost:8100/events ./gradlew :a2cui-sample-live:run
```

The sample window opens, displays the assistant narration, then renders the
booking form that the ADK agent emitted via `render_booking_form`.

---

## Env vars

| Var | Default | Purpose |
|---|---|---|
| `A2UI_SURFACE_ID` | `demo` | Surface id — must match the Kotlin host's `surfaceId` in `A2cuiSurface(surfaceId = ...)`. |
| `A2UI_FRAME_DELAY_MS` | `300` | Pacing between emitted frames (visual effect; doesn't affect correctness). |
| `A2UI_MODEL` | `gemini-flash-latest` | ADK model id; any Gemini model accepted by `google.adk.agents.Agent(model=...)`. |
| `PORT` | `8100` | HTTP port. |

---

## Wire contract (what A2CUI expects)

Each HTTP response is a single SSE stream containing:

```
event: <omitted — sse-starlette defaults to "message">
data: {"type":"RUN_STARTED","threadId":"...","runId":"..."}

data: {"type":"TEXT_MESSAGE_START","messageId":"msg-1","role":"assistant"}
data: {"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"Rendering…"}
data: {"type":"TEXT_MESSAGE_END","messageId":"msg-1"}

data: {"type":"CUSTOM","name":"a2ui","value":{<A2UI frame>}}
data: {"type":"CUSTOM","name":"a2ui","value":{<A2UI frame>}}
data: {"type":"CUSTOM","name":"a2ui","value":{<A2UI frame>}}

data: {"type":"RUN_FINISHED","threadId":"...","runId":"...","outcome":"success"}
```

Every `CUSTOM` event with `name = "a2ui"` carries exactly one A2UI v0.9 frame
(`createSurface`, `updateComponents`, `updateDataModel`, `deleteSurface`). See
[`a2cui-core` README](../../a2cui-core/README.md) for the frame shape.

---

## Extending this example

- **Swap the booking payload** — edit `render_booking_form()` in `agent.py` to
  return different A2UI frames. The tool's return shape is
  `{"surfaceId": str, "frames": list[A2uiFrame]}`.
- **Add more tools** — register additional ADK tools that return A2UI frames;
  the bridge forwards every tool result with a `version` field on to the
  client as a `CUSTOM(a2ui)` event.
- **Use a different model** — set `A2UI_MODEL=gemini-2.5-pro` (or any ADK-supported id).
- **Wire client → server events** — A2CUI's `SurfaceController.events` already
  emits outbound actions. To route them back into ADK, add a `POST /events`
  endpoint that decodes `A2uiClientMessage.Action` payloads and forwards them
  into a new ADK run as `types.Content(role="user", parts=[Part(text=...)])`.

---

## Troubleshooting

**`ImportError: google.adk` not found**
Install the package: `pip install google-adk`. Note the module path is
`google.adk` (from the `google-adk` package).

**`AuthenticationError` / `403 PERMISSION_DENIED`**
Either `GOOGLE_API_KEY` is invalid or your gcloud ADC doesn't have the Vertex
AI Gemini API enabled. Verify with `gcloud services list | grep generativelanguage`.

**Agent returns text but no `CUSTOM` frames**
The model didn't call the tool. Check the ADK server logs; sharpen the
`instruction` string in `agent.py` to make the tool contract clearer, or use a
stronger model.

**`:a2cui-sample-live` shows the mock sample instead**
`AGUI_URL` isn't set in the Gradle process. Export it in the same shell, or
pass `-Dagui.url=...` on the Gradle command line.

---

## See also

- [`integrations/pydantic-ai/`](../pydantic-ai/README.md) — sibling example using Pydantic AI.
- [`integrations/mastra/`](../mastra/README.md) — TypeScript / Node equivalent.
- [`a2cui-sample-live`](../../a2cui-sample-live/README.md) — Compose Desktop host.
- [`a2cui-agui-mock-server`](../../a2cui-agui-mock-server/README.md) — all-Kotlin reference server you can diff against.
