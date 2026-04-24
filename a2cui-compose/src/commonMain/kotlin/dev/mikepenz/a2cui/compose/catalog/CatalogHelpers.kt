package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * Resolve [key] on [node] through the scope's binding resolver.
 *
 * **Reactive**: when the property is anything other than a plain literal (i.e. a `{path}`
 * binding, a `{call}`, or a `{if}` conditional that depends on the data model), this helper
 * subscribes to the data-model root inside the calling composable so that downstream changes
 * trigger recomposition. Plain-literal properties never read the root flow, so static text /
 * sizes / colours don't pay any subscription cost.
 *
 * Without this, Text/Image/Icon/Button-text bound to `{path}` would snapshot at first
 * composition and never update when the bound value changed — a class of silent bugs that
 * affected every display component before this rewrite.
 */
@Composable
public fun RenderScope.resolve(node: ComponentNode, key: String): JsonElement {
    val raw = node.properties[key] ?: return JsonNull
    val classified = PropertyValue.classify(raw)
    if (classified is PropertyValue.Literal) return classified.value
    // Non-literal: subscribe to root so we recompose when any data-model write happens.
    // resolver.resolve reads dataModel.read(...) under the hood, which uses the latest snapshot.
    val root by dataModel.root.collectAsState()
    @Suppress("UNUSED_EXPRESSION") root // ensure read inside this composable
    return resolver.resolve(raw)
}

/** Resolve [key] as a string; falls back to [default] when missing or non-primitive. */
@Composable
public fun RenderScope.resolveString(node: ComponentNode, key: String, default: String = ""): String =
    (resolve(node, key) as? JsonPrimitive)?.contentOrNull ?: default

@Composable
public fun RenderScope.resolveBool(node: ComponentNode, key: String, default: Boolean = false): Boolean =
    (resolve(node, key) as? JsonPrimitive)?.booleanOrNull ?: default

@Composable
public fun RenderScope.resolveInt(node: ComponentNode, key: String, default: Int = 0): Int =
    (resolve(node, key) as? JsonPrimitive)?.intOrNull ?: default

@Composable
public fun RenderScope.resolveDouble(node: ComponentNode, key: String, default: Double = 0.0): Double =
    (resolve(node, key) as? JsonPrimitive)?.doubleOrNull ?: default

@Composable
public fun RenderScope.resolveStringList(node: ComponentNode, key: String): List<String> {
    val el = resolve(node, key)
    if (el !is kotlinx.serialization.json.JsonArray) return emptyList()
    return el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

/** If [key] is a `{path}` binding, return the raw pointer string; otherwise `null`. */
public fun pathOf(properties: JsonObject, key: String): String? {
    val raw = properties[key] ?: return null
    return (PropertyValue.classify(raw) as? PropertyValue.Path)?.path
}

/** Parsed view of a component's `action` property (v0.9 shape). */
public data class ActionSpec(
    val eventName: String? = null,
    val eventContext: JsonObject? = null,
    val callFn: String? = null,
    val callArgs: JsonObject? = null,
)

public fun parseAction(raw: JsonElement?): ActionSpec? {
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
 * current data model produce concrete values. Not `@Composable` — called from `onClick`
 * lambdas where reactivity isn't meaningful (the click already implies "now").
 */
public fun RenderScope.emitAction(node: ComponentNode, overrides: JsonObject = JsonObject(emptyMap())) {
    val spec = parseAction(node.properties["action"]) ?: return
    if (spec.eventName != null) {
        val context = spec.eventContext ?: JsonObject(emptyMap())
        val resolved = JsonObject(context.mapValues { resolver.resolve(it.value) } + overrides)
        emit(EventSpec(name = spec.eventName, sourceComponentId = node.id, context = resolved))
    }
    // functionCall dispatch is deferred to the catalog component itself (e.g. openUrl).
}

/** Convert an arbitrary [JsonElement] to a display string (stable, no `JsonNull` text). */
public fun JsonElement?.displayString(): String = when (this) {
    null, JsonNull -> ""
    is JsonPrimitive -> contentOrNull ?: ""
    else -> toString()
}
