# A2CUI External Agent Integrations

This directory ships reference **AG-UI compatible agent backends** that drive `:a2cui-sample-live` (or any
A2CUI client) in place of the built-in `:a2cui-agui-mock-server`. Each example emits the same booking-form
A2UI payloads the Kotlin mock does, wrapped inside AG-UI `CUSTOM` events with `name = "a2ui"`.

## Wire contract (mandatory)

The A2CUI client expects a normal AG-UI run envelope:

```
RUN_STARTED            { threadId, runId }
TEXT_MESSAGE_START     { messageId, role: "assistant" }
TEXT_MESSAGE_CONTENT   { messageId, delta: "<text>" }
TEXT_MESSAGE_END       { messageId }
CUSTOM                 { name: "a2ui", value: <A2uiFrame> }     # one frame per event
CUSTOM                 { name: "a2ui", value: <A2uiFrame> }
...
RUN_FINISHED           { threadId, runId, outcome: "success" }
```

The `value` is a full **A2UI v0.9** frame: `createSurface`, `updateComponents`, `updateDataModel`, etc.
Reference payloads live in
[`a2cui-agui-mock-server/src/main/kotlin/.../SampleA2uiFrames.kt`](../a2cui-agui-mock-server/src/main/kotlin/dev/mikepenz/a2cui/agui/mockserver/SampleA2uiFrames.kt).

Transport: **Server-Sent Events (SSE)**. Each SSE `data:` line is one JSON-encoded AG-UI event.

## Examples

| Directory | Stack | Endpoint (default) | Notes |
|---|---|---|---|
| [`pydantic-ai/`](./pydantic-ai/) | Python / FastAPI + `pydantic-ai` | `http://localhost:8000/events` | LLM optional. Hand-rolled SSE. |
| [`google-adk/`](./google-adk/) | Python / FastAPI + `google-adk` (Gemini) | `http://localhost:8100/events` | Agent calls a `render_booking_form` tool whose returned frames are forwarded as `CUSTOM(a2ui)`. |
| [`mastra/`](./mastra/) | TypeScript / Node / Mastra | `http://localhost:3000/agent/a2ui-booking` | Uses Mastra's AG-UI adapter. |

## Plugging into `:a2cui-sample-live`

`:a2cui-sample-live` reads the backend URL from:

1. `AGUI_URL` env var, then
2. `-Dagui.url=...` JVM system property, then
3. falls back to the in-process `MockAguiServer`.

```bash
# Pydantic AI
AGUI_URL=http://localhost:8000/events ./gradlew :a2cui-sample-live:run

# Google ADK
AGUI_URL=http://localhost:8100/events ./gradlew :a2cui-sample-live:run

# Mastra
AGUI_URL=http://localhost:3000/agent/a2ui-booking ./gradlew :a2cui-sample-live:run
```

See each subdirectory's `README.md` for setup, API keys, and run commands.
