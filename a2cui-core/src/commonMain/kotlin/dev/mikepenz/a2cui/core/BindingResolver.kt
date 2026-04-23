package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Resolves a raw wire [JsonElement] into a concrete value by interpreting the v0.9 property
 * forms: plain literals, `{path}` references against [DataModel], and `{call, args}` client-side
 * function invocations.
 *
 * Call resolution is delegated to [callDispatcher]; the default implementation echoes the call
 * descriptor back as an unresolved [PropertyValue.Call], letting higher layers decide what to do.
 */
public class BindingResolver(
    private val dataModel: DataModel,
    private val callDispatcher: CallDispatcher = CallDispatcher.Noop,
) {

    public fun interface CallDispatcher {
        public fun invoke(function: String, args: JsonObject): JsonElement

        public companion object {
            public val Noop: CallDispatcher = CallDispatcher { _, _ -> JsonNull }
        }
    }

    /** Classify [raw] and resolve it. Missing pointer paths resolve to [JsonNull]. */
    public fun resolve(raw: JsonElement): JsonElement = when (val p = PropertyValue.classify(raw)) {
        is PropertyValue.Literal -> p.value
        is PropertyValue.Path -> dataModel.read(p.path) ?: JsonNull
        is PropertyValue.Call -> callDispatcher.invoke(p.function, p.args)
    }

    /** Resolve a property by its key on a component's [properties] object. */
    public fun resolveProperty(properties: JsonObject, key: String): JsonElement =
        properties[key]?.let(::resolve) ?: JsonNull
}
