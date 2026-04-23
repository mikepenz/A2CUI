/**
 * Mastra configuration for the A2CUI booking agent.
 *
 * Mastra has first-class AG-UI support, but the `CUSTOM` event with
 * `name: "a2ui"` is A2CUI-specific — we attach it from `server.ts` via a
 * streaming endpoint that mirrors Mastra's AG-UI envelope and layers the
 * A2UI frames on top of the standard run lifecycle.
 */
import { Mastra } from "@mastra/core";
import { Agent } from "@mastra/core/agent";
import { openai } from "@ai-sdk/openai";

const modelId = process.env.A2UI_MODEL ?? "gpt-4o-mini";

export const bookingAgent = new Agent({
  name: "a2ui-booking",
  instructions:
    "You narrate UI rendering in one short sentence (<= 12 words). " +
    "Never produce markdown or code — only natural language.",
  model: process.env.OPENAI_API_KEY ? openai(modelId) : (undefined as any),
});

export const mastra = new Mastra({
  agents: { bookingAgent },
});

export default mastra;
