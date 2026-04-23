package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A resolved interpretation of an A2UI property value. On the wire, a property is a raw
 * [JsonElement]; [PropertyValue] is produced by [classify] and consumed by the binding resolver.
 *
 * Three forms from the v0.9 spec:
 *  - [Literal]: inline JSON scalar / object / array.
 *  - [Path]: a `{"path": "/..."}` object referencing the surface data model (RFC 6901 pointer).
 *  - [Call]: a `{"call": "fnName", "args": {...}}` object invoking a client-side function.
 */
public sealed interface PropertyValue {
    public data class Literal(val value: JsonElement) : PropertyValue
    public data class Path(val path: String) : PropertyValue
    public data class Call(val function: String, val args: JsonObject) : PropertyValue

    public companion object {
        /**
         * Interpret a raw wire [element] as a [PropertyValue]. Single-key `{path}` or `{call}`
         * objects are promoted to their typed form; anything else is a [Literal].
         */
        public fun classify(element: JsonElement): PropertyValue {
            if (element !is JsonObject) return Literal(element)
            val pathValue = element["path"]
            if (element.size == 1 && pathValue != null) {
                return Path(pathValue.jsonPrimitive.content)
            }
            val callValue = element["call"]
            if (callValue != null && element.keys.all { it == "call" || it == "args" }) {
                val args = element["args"] as? JsonObject ?: JsonObject(emptyMap())
                return Call(callValue.jsonPrimitive.content, args)
            }
            return Literal(element)
        }
    }
}
