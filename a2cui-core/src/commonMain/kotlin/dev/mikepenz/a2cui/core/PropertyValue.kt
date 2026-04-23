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

    /**
     * **A2CUI experimental extension** — conditional property. On the wire:
     * `{"if": "/user/loggedIn", "then": <PropertyValue>, "else": <PropertyValue>}`.
     * The `if` field is an RFC 6901 pointer; the branch is picked from the data model's truthy
     * evaluation of that pointer. Either branch can be any `PropertyValue` (nested conditionals
     * are allowed). Hosts evaluate conditionals through [BindingResolver.resolve] transparently.
     */
    @ExperimentalA2uiDraft
    public data class Conditional(
        val condition: String,
        val thenBranch: JsonElement,
        val elseBranch: JsonElement,
    ) : PropertyValue

    public companion object {
        /**
         * Interpret a raw wire [element] as a [PropertyValue]. Single-key `{path}` or `{call}`
         * objects are promoted to their typed form; `{if, then, else}` objects become
         * [Conditional] (A2CUI experimental extension — requires opt-in for authors but decodes unconditionally
         * so incoming frames never fail); anything else is a [Literal].
         */
        @OptIn(ExperimentalA2uiDraft::class)
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
            val ifValue = element["if"]
            if (ifValue is kotlinx.serialization.json.JsonPrimitive &&
                element.keys.all { it == "if" || it == "then" || it == "else" }
            ) {
                val thenBranch = element["then"] ?: kotlinx.serialization.json.JsonNull
                val elseBranch = element["else"] ?: kotlinx.serialization.json.JsonNull
                return Conditional(ifValue.content, thenBranch, elseBranch)
            }
            return Literal(element)
        }
    }
}
