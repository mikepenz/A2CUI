package dev.mikepenz.a2cui.compose.catalog

import dev.mikepenz.a2cui.compose.EventSpec
import dev.mikepenz.a2cui.compose.RenderScope
import dev.mikepenz.a2cui.core.ComponentNode
import dev.mikepenz.a2cui.core.PropertyValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Resolve the property named [key] on [node] through the scope's binding resolver. */
internal fun RenderScope.resolve(node: ComponentNode, key: String): JsonElement =
    resolver.resolveProperty(node.properties, key)

/** Resolve [key] as a string; falls back to [default] when missing or non-primitive. */
internal fun RenderScope.resolveString(node: ComponentNode, key: String, default: String = ""): String =
    (resolve(node, key) as? JsonPrimitive)?.contentOrNull ?: default

internal fun RenderScope.resolveBool(node: ComponentNode, key: String, default: Boolean = false): Boolean =
    (resolve(node, key) as? JsonPrimitive)?.booleanOrNull ?: default

internal fun RenderScope.resolveInt(node: ComponentNode, key: String, default: Int = 0): Int =
    (resolve(node, key) as? JsonPrimitive)?.intOrNull ?: default

internal fun RenderScope.resolveDouble(node: ComponentNode, key: String, default: Double = 0.0): Double =
    (resolve(node, key) as? JsonPrimitive)?.doubleOrNull ?: default

internal fun RenderScope.resolveStringList(node: ComponentNode, key: String): List<String> {
    val el = resolve(node, key)
    if (el !is kotlinx.serialization.json.JsonArray) return emptyList()
    return el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** If [key] is a `{path}` binding, return the raw pointer string; otherwise `null`. */
internal fun pathOf(properties: JsonObject, key: String): String? {
    val raw = properties[key] ?: return null
    return (PropertyValue.classify(raw) as? PropertyValue.Path)?.path
}

/** Parsed view of a component's `action` property (v0.9 shape). */
internal data class ActionSpec(
    val eventName: String? = null,
    val eventContext: JsonObject? = null,
    val callFn: String? = null,
    val callArgs: JsonObject? = null,
)

internal fun parseAction(raw: JsonElement?): ActionSpec? {
    if (raw !is JsonObject) return null
    (raw["event"] as? JsonObject)?.let { ev ->
        val name = ev["name"]?.jsonPrimitive?.contentOrNull ?: return@let null
        val ctx = ev["context"] as? JsonObject ?: JsonObject(emptyMap())
        return ActionSpec(eventName = name, eventContext = ctx)
    }
    (raw["functionCall"] as? JsonObject)?.let { call ->
        val fn = call["name"]?.jsonPrimitive?.contentOrNull ?: return@let null
        val args = call["args"] as? JsonObject ?: JsonObject(emptyMap())
        return ActionSpec(callFn = fn, callArgs = args)
    }
    return null
}

/**
 * Fire an outbound event derived from [node]'s `action` property (if any). Context values in
 * the action are resolved through the binding resolver at emit time so `{path}` refs to the
 * current data model produce concrete values.
 */
internal fun RenderScope.emitAction(node: ComponentNode) {
    val spec = parseAction(node.properties["action"]) ?: return
    if (spec.eventName != null) {
        val context = spec.eventContext ?: JsonObject(emptyMap())
        val resolved = JsonObject(context.mapValues { resolver.resolve(it.value) })
        emit(EventSpec(name = spec.eventName, sourceComponentId = node.id, context = resolved))
    }
    // functionCall dispatch is deferred to the catalog component itself (e.g. openUrl).
}

/** Convert an arbitrary [JsonElement] to a display string (stable, no `JsonNull` text). */
internal fun JsonElement?.displayString(): String = when (this) {
    null, JsonNull -> ""
    is JsonPrimitive -> contentOrNull ?: ""
    else -> toString()
}
