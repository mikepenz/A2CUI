package dev.mikepenz.a2cui.actions

import kotlinx.serialization.json.JsonObject

/**
 * Pure metadata describing one agent-callable action — the piece the client serialises into
 * the system prompt / tool schema advertised to the LLM. The render function and lifecycle
 * hooks live in [ActionRegistration] alongside this.
 */
public data class ActionDescriptor(
    val name: String,
    val description: String,
    /** JSON-Schema-shaped parameter spec; usually produced by codegen or hand-authored. */
    val parameters: JsonObject = JsonObject(emptyMap()),
)
