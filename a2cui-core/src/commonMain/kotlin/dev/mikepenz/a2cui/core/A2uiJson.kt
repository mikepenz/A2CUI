package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance configured for the A2UI wire format.
 *
 * - `ignoreUnknownKeys = true` — forward-compat: newer spec fields do not break old clients.
 * - `encodeDefaults = false` — keep outbound payloads small.
 * - `classDiscriminator` is not used (A2UI frames are discriminated by key-presence).
 */
public val A2uiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
    prettyPrint = false
}
