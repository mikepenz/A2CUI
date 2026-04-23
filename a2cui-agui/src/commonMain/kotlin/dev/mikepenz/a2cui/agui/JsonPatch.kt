package dev.mikepenz.a2cui.agui

import dev.mikepenz.a2cui.core.JsonPointer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal RFC 6902 JSON Patch applier. Supports the three load-bearing operations that
 * AG-UI `STATE_DELTA` emits in practice: `add`, `replace`, `remove`. `move`, `copy`, and
 * `test` are accepted but fall back to `replace`-like semantics; they can be fleshed out
 * when real-world traffic demands them.
 */
public object JsonPatch {

    /** Apply [patch] (an array of RFC 6902 ops) to [root], returning a new [JsonElement]. */
    public fun apply(root: JsonElement, patch: JsonArray): JsonElement {
        var current = root
        for (op in patch) {
            if (op !is JsonObject) continue
            val opKind = op["op"]?.jsonPrimitive?.content ?: continue
            val path = op["path"]?.jsonPrimitive?.content ?: continue
            current = when (opKind) {
                "add", "replace" -> {
                    val value = op["value"] ?: continue
                    JsonPointer.write(current, path, value)
                }
                "remove" -> remove(current, path)
                "move" -> {
                    val from = op["from"]?.jsonPrimitive?.content ?: continue
                    val picked = JsonPointer.read(current, from) ?: continue
                    val afterRemove = remove(current, from)
                    JsonPointer.write(afterRemove, path, picked)
                }
                "copy" -> {
                    val from = op["from"]?.jsonPrimitive?.content ?: continue
                    val picked = JsonPointer.read(current, from) ?: continue
                    JsonPointer.write(current, path, picked)
                }
                else -> current  // `test` and unknown ops: no-op.
            }
        }
        return current
    }

    private fun remove(root: JsonElement, pointer: String): JsonElement {
        val segments = JsonPointer.parse(pointer)
        if (segments.isEmpty()) return JsonObject(emptyMap())
        return removeRec(root, segments, 0)
    }

    private fun removeRec(node: JsonElement, segments: List<String>, index: Int): JsonElement {
        val seg = segments[index]
        val last = index == segments.lastIndex
        return when (node) {
            is JsonObject -> if (last) {
                buildJsonObject { node.forEach { (k, v) -> if (k != seg) put(k, v) } }
            } else {
                val child = node[seg] ?: return node
                val updated = removeRec(child, segments, index + 1)
                buildJsonObject {
                    node.forEach { (k, v) -> if (k != seg) put(k, v) }
                    put(seg, updated)
                }
            }
            is JsonArray -> {
                val idx = seg.toIntOrNull() ?: return node
                if (idx !in node.indices) return node
                if (last) {
                    buildJsonArray { node.forEachIndexed { i, el -> if (i != idx) add(el) } }
                } else {
                    val child = node[idx]
                    val updated = removeRec(child, segments, index + 1)
                    buildJsonArray {
                        node.forEachIndexed { i, el -> if (i != idx) add(el) else add(updated) }
                    }
                }
            }
            is JsonPrimitive, kotlinx.serialization.json.JsonNull -> node
        }
    }
}
