package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull

/**
 * Resolves a raw wire [JsonElement] into a concrete value by interpreting the v0.9 property
 * forms: plain literals, `{path}` references against [DataModel], and `{call, args}` client-side
 * function invocations.
 *
 * Call resolution is delegated to [callDispatcher]; the default implementation echoes the call
 * descriptor back as an unresolved [PropertyValue.Call], letting higher layers decide what to do.
 */
public class BindingResolver(
    public val dataModel: DataModel,
    private val callDispatcher: CallDispatcher = CallDispatcher.Noop,
    /**
     * Absolute pointer into [dataModel] acting as the scope root. When non-empty, absolute
     * paths in `{path}` bindings are reinterpreted as relative to this root (e.g. with root
     * `/users/0`, `{path:"/name"}` reads `/users/0/name`). Use `..` segments to walk up
     * toward the real root (e.g. `{path:"../header/title"}` resolves against `/users`).
     *
     * Empty string means "no scoping" — absolute paths resolve against the actual root, as
     * per RFC 6901.
     */
    public val scopeRoot: String = "",
) {

    public fun interface CallDispatcher {
        public fun invoke(function: String, args: JsonObject): JsonElement

        public companion object {
            public val Noop: CallDispatcher = CallDispatcher { _, _ -> JsonNull }
        }
    }

    /** Classify [raw] and resolve it. Missing pointer paths resolve to [JsonNull]. */
    @OptIn(ExperimentalA2uiDraft::class)
    public fun resolve(raw: JsonElement): JsonElement = when (val p = PropertyValue.classify(raw)) {
        is PropertyValue.Literal -> p.value
        is PropertyValue.Path -> dataModel.read(scopedPointer(p.path)) ?: JsonNull
        is PropertyValue.Call -> callDispatcher.invoke(p.function, p.args)
        is PropertyValue.Conditional -> {
            // Experimental extension: evaluate the condition pointer, pick a branch, resolve recursively
            // so nested {path}/{call}/{if} bindings inside the chosen branch still work.
            val truthy = isTruthy(dataModel.read(scopedPointer(p.condition)))
            resolve(if (truthy) p.thenBranch else p.elseBranch)
        }
    }

    private fun isTruthy(value: JsonElement?): Boolean = when (value) {
        null, JsonNull -> false
        is kotlinx.serialization.json.JsonPrimitive -> when {
            value.booleanOrNull != null -> value.boolean
            value.isString -> value.content.isNotEmpty()
            else -> value.content != "0" && value.content.isNotEmpty()
        }
        is JsonObject -> value.isNotEmpty()
        is kotlinx.serialization.json.JsonArray -> value.isNotEmpty()
    }

    /** Resolve a property by its key on a component's [properties] object. */
    public fun resolveProperty(properties: JsonObject, key: String): JsonElement =
        properties[key]?.let(::resolve) ?: JsonNull

    /**
     * Return a new [BindingResolver] whose [scopeRoot] is the given absolute pointer. Shares
     * the same [dataModel] and [callDispatcher]; bindings inside the scope resolve relative
     * to [rootPointer].
     *
     * Design trade-off: we chose *resolver scoping* over *pointer-prefix rewriting at the
     * call site* because nested components inside a List template don't need to be aware of
     * the scope — their existing `{path:"/foo"}` bindings transparently redirect to
     * `rootPointer/foo`. The alternative (prefixed `JsonPointer.atIndex(i)`) would require
     * every catalog factory to participate in index awareness, which scales poorly.
     */
    public fun withScope(rootPointer: String): BindingResolver =
        BindingResolver(dataModel, callDispatcher, rootPointer)

    /**
     * Compose a raw `{path}` pointer with the current [scopeRoot]. Handles `..` segments
     * walking up from [scopeRoot] toward the real root. A `path` of `""` returns [scopeRoot].
     */
    public fun scopedPointer(path: String): String {
        if (scopeRoot.isEmpty()) return path
        val rootSegs = JsonPointer.parse(scopeRoot).toMutableList()
        // Treat a single leading '/' as "path is absolute-within-scope"; strip it.
        val body = if (path.startsWith('/')) path.substring(1) else path
        if (body.isEmpty() && !path.startsWith('/')) return scopeRoot
        val segs = body.split('/').map { it.replace("~1", "/").replace("~0", "~") }
        for (seg in segs) {
            if (seg == "..") {
                if (rootSegs.isNotEmpty()) rootSegs.removeAt(rootSegs.lastIndex)
            } else if (seg.isNotEmpty()) {
                rootSegs += seg
            }
        }
        return JsonPointer.encode(rootSegs)
    }
}
