package dev.mikepenz.a2cui.compose

import kotlinx.serialization.json.JsonObject

/**
 * Named collection of [ComponentFactory] registrations.
 *
 * A catalog's [id] should match the `catalogId` field used in A2UI `createSurface` frames so
 * the agent knows which vocabulary the client advertises. [toJsonSchema] returns a JSON
 * Schema-shaped descriptor of the catalog, surfaced to the agent's system prompt so the
 * LLM is hard-constrained to only emit components the client can render.
 */
public interface Catalog {
    public val id: String

    /** Register every component this catalog provides on [registry]. */
    public fun install(registry: ComponentRegistry)

    /** JSON Schema description of the catalog's components. Defaults to an empty object. */
    public fun toJsonSchema(): JsonObject = JsonObject(emptyMap())
}
