# A2CUI x Mastra

A [Mastra](https://mastra.ai/) agent wired to an **AG-UI**-compatible SSE endpoint at
`GET /agent/a2ui-booking`, serving as a drop-in replacement for `:a2cui-agui-mock-server`.

Mastra has first-class AG-UI support. The `CUSTOM` event with `name: "a2ui"` is
A2CUI-specific — we layer it on top of Mastra's standard run-lifecycle envelope
(`RUN_STARTED` → `TEXT_MESSAGE_*` → `CUSTOM` × N → `RUN_FINISHED`) so the A2CUI
bridge's state reducer observes a normal run.

## Files

- `mastra.config.ts` — Mastra agent definition (`bookingAgent`) + optional LLM model.
- `frames.ts` — A2UI v0.9 frame builders (mirrors `SampleA2uiFrames.kt`).
- `server.ts` — Express SSE endpoint that emits the AG-UI envelope and the CUSTOM A2UI frames.
- `package.json` / `tsconfig.json` — standard Node/TypeScript plumbing.

## Prerequisites

- Node.js >= 20
- (optional) `OPENAI_API_KEY` for LLM-generated narration; without it a static string is used.

## Install

```bash
cd integrations/mastra
npm install
```

## Run

```bash
# deterministic narration
npm run dev

# with OpenAI for narration
export OPENAI_API_KEY=sk-...
export A2UI_MODEL=gpt-4o-mini
npm run dev
```

Verify:

```bash
curl -N http://localhost:3000/agent/a2ui-booking
```

## Environment

| Var | Default | Purpose |
|---|---|---|
| `PORT` | `3000` | HTTP port |
| `A2UI_SURFACE_ID` | `demo` | Surface id inside A2UI frames — must match client |
| `A2UI_FRAME_DELAY_MS` | `300` | Delay between consecutive AG-UI events |
| `A2UI_MODEL` | `gpt-4o-mini` | OpenAI model id (only used when `OPENAI_API_KEY` is set) |
| `OPENAI_API_KEY` | — | Credential for narration |

## Point A2CUI at it

```bash
AGUI_URL=http://localhost:3000/agent/a2ui-booking ./gradlew :a2cui-sample-live:run
```

## Porting to a full Mastra AG-UI server

If you're running Mastra's built-in AG-UI HTTP adapter, register the agent there
and emit A2UI frames via your stream's custom-event API (e.g.
`stream.customEvent({ name: "a2ui", value: <frame> })` — exact method may vary
per Mastra version). The frame builders in `frames.ts` are the only A2CUI-specific
bit and drop straight into that handler.
