/**
 * Express + SSE bridge that serves an AG-UI-compatible stream at
 *
 *   GET /agent/a2ui-booking
 *
 * The endpoint mirrors Mastra's standard AG-UI event envelope and adds
 * `CUSTOM` events with `name: "a2ui"` carrying A2UI v0.9 frames — the
 * contract A2CUI's Kotlin bridge expects.
 *
 * Design note: Mastra's AG-UI adapter emits the standard run lifecycle for
 * free, but A2UI CUSTOM events are application-specific. We drive the whole
 * envelope here so the file is fully self-contained and easy to audit. If
 * you already have a `registerAgui()`-style Mastra server, you can instead
 * publish the CUSTOM frames via `stream.customEvent({ name: "a2ui", value })`
 * (API name may vary per Mastra version).
 */
import express, { type Request, type Response } from "express";
import { bookingAgent } from "./mastra.config.js";
import { bookingComponents, createSurface, seedEmail } from "./frames.js";

const PORT = Number(process.env.PORT ?? 3000);
const SURFACE_ID = process.env.A2UI_SURFACE_ID ?? "demo";
const FRAME_DELAY_MS = Number(process.env.A2UI_FRAME_DELAY_MS ?? 300);

function sse(res: Response, payload: unknown) {
  res.write(`data: ${JSON.stringify(payload)}\n\n`);
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

async function narrate(): Promise<string> {
  if (!process.env.OPENAI_API_KEY) return "Rendering booking form…";
  try {
    const result = await bookingAgent.generate(
      "The client is about to render a booking form.",
    );
    return (result?.text ?? "").trim() || "Rendering booking form…";
  } catch (err) {
    return `Rendering booking form… (narration skipped: ${(err as Error).name})`;
  }
}

const app = express();

app.get("/agent/a2ui-booking", async (req: Request, res: Response) => {
  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");
  res.flushHeaders?.();

  const threadId = `thread-${Math.floor(Math.random() * 1_000_000)}`;
  const runId = `run-${Math.floor(Math.random() * 1_000_000)}`;
  const messageId = "msg-1";

  sse(res, { type: "RUN_STARTED", threadId, runId });
  await sleep(FRAME_DELAY_MS);

  const text = await narrate();
  sse(res, { type: "TEXT_MESSAGE_START", messageId, role: "assistant" });
  sse(res, { type: "TEXT_MESSAGE_CONTENT", messageId, delta: text });
  sse(res, { type: "TEXT_MESSAGE_END", messageId });
  await sleep(FRAME_DELAY_MS);

  for (const frame of [
    createSurface(SURFACE_ID),
    bookingComponents(SURFACE_ID),
    seedEmail(SURFACE_ID),
  ]) {
    sse(res, { type: "CUSTOM", name: "a2ui", value: frame });
    await sleep(FRAME_DELAY_MS);
  }

  sse(res, { type: "RUN_FINISHED", threadId, runId, outcome: "success" });
  res.end();
});

app.get("/health", (_req, res) => res.json({ status: "ok", surfaceId: SURFACE_ID }));

app.listen(PORT, () => {
  console.log(`[mastra] A2CUI agent listening on http://localhost:${PORT}/agent/a2ui-booking`);
});
