package dev.mikepenz.a2cui.agui

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance for AG-UI events. Uses the default `type` class discriminator, which
 * matches the protocol's wire shape. Unknown keys are ignored for forward-compat with draft
 * events (reasoning, activity, interrupt).
 */
public val AguiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    explicitNulls = false
    classDiscriminator = "type"
}
