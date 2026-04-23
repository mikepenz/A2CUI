# `:a2cui-sample-live` — live AG-UI → A2UI demo

Compose Desktop host that wires an **AG-UI SSE stream** through
`SseTransport` → `AguiEventParser` → `AguiA2cuiBridge` → `SurfaceController`
→ `A2cuiSurface` and renders a live A2UI booking form.

Two panes:

- **Top**: the rendered A2UI surface (updates as `CUSTOM` `a2ui` frames stream in).
- **Bottom**: the raw AG-UI event log plus outbound events fired from the UI.

## Backend selection

The backend URL is resolved in this order:

1. `AGUI_URL` environment variable
2. `-Dagui.url=...` JVM system property
3. **Fallback** — an in-process `MockAguiServer` (from `:a2cui-agui-mock-server`)
   is spun up and consumed.

The client only requires that the backend stream standard AG-UI events, with A2UI
frames wrapped inside `CUSTOM` events named `a2ui`. See
[`integrations/README.md`](../integrations/README.md) for the wire contract.

## Three run modes

### 1. Mock (default, offline)

```bash
./gradlew :a2cui-sample-live:run
```

An in-process Ktor SSE server starts on an ephemeral port and scripts the
booking-form frames defined in
[`SampleA2uiFrames.kt`](../a2cui-agui-mock-server/src/main/kotlin/dev/mikepenz/a2cui/agui/mockserver/SampleA2uiFrames.kt).

### 2. Pydantic AI

Start the Python agent (see
[`integrations/pydantic-ai/README.md`](../integrations/pydantic-ai/README.md)):

```bash
cd integrations/pydantic-ai
pip install -r requirements.txt
python agent.py          # serves GET http://localhost:8000/events
```

Point the client at it:

```bash
AGUI_URL=http://localhost:8000/events ./gradlew :a2cui-sample-live:run
```

### 3. Mastra

Start the Mastra agent (see
[`integrations/mastra/README.md`](../integrations/mastra/README.md)):

```bash
cd integrations/mastra
npm install
npm run dev              # serves GET http://localhost:3000/agent/a2ui-booking
```

Point the client at it:

```bash
AGUI_URL=http://localhost:3000/agent/a2ui-booking ./gradlew :a2cui-sample-live:run
```

## JVM system property alternative

If you can't easily set env vars:

```bash
./gradlew :a2cui-sample-live:run -Dagui.url=http://localhost:8000/events
```

## Troubleshooting

- **Surface stays empty.** Confirm the backend actually emits `CUSTOM` events with
  `name: "a2ui"` — curl the endpoint and check each `data:` line. The `surfaceId`
  in the A2UI frames must equal `"demo"` (hard-coded in `A2cuiLiveApp`), or change
  both ends.
- **SSE hangs on connect.** Most AG-UI servers require `Accept: text/event-stream`
  — A2CUI's `SseTransport` sends that automatically. If you reverse-proxy through
  nginx, disable buffering (`proxy_buffering off;`).
- **LLM 401.** For Pydantic AI / Mastra narration, set the provider API key env
  var (`OPENAI_API_KEY`, etc.). Without a key both agents fall back to a static
  narration line — A2UI frames still stream normally.
