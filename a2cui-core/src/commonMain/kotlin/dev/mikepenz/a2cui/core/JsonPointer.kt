package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * RFC 6901 JSON Pointer utilities.
 *
 * Pointer syntax:
 *  - `""` — the root document.
 *  - `"/foo"` — key `foo` of the root object.
 *  - `"/foo/0"` — index `0` of the array at `/foo`.
 *  - Escape `~` as `~0` and `/` as `~1` inside segments.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901</a>
 */
public object JsonPointer {

    /**
     * Parse a RFC 6901 pointer into its path segments. `""` yields an empty list.
     * Throws [IllegalArgumentException] if the pointer does not start with `/` and is non-empty.
     */
    public fun parse(pointer: String): List<String> {
        if (pointer.isEmpty()) return emptyList()
        require(pointer.startsWith('/')) { "JSON Pointer must start with '/' or be empty: '$pointer'" }
        return pointer.substring(1).split('/').map { it.replace("~1", "/").replace("~0", "~") }
    }

    /** Encode a list of segments back to a RFC 6901 pointer string. */
    public fun encode(segments: List<String>): String {
        if (segments.isEmpty()) return ""
        return segments.joinToString("/", prefix = "/") {
            it.replace("~", "~0").replace("/", "~1")
        }
    }

    /**
     * Resolve [pointer] against [root]. Returns the referenced element, or `null` if any segment
     * is missing / out of bounds / cannot be resolved.
     */
    public fun read(root: JsonElement, pointer: String): JsonElement? {
        var node: JsonElement = root
        for (seg in parse(pointer)) {
            node = when (node) {
                is JsonObject -> node[seg] ?: return null
                is JsonArray -> {
                    val idx = seg.toIntOrNull() ?: return null
                    if (idx !in node.indices) return null
                    node[idx]
                }
                else -> return null
            }
        }
        return node
    }

    /**
     * Immutably write [value] at [pointer] against [root], creating intermediate objects as
     * needed. Arrays only accept numeric indices that already exist or equal `size` (append).
     * Non-numeric segments hitting an array, or invalid indices, throw [IllegalArgumentException].
     */
    public fun write(root: JsonElement, pointer: String, value: JsonElement): JsonElement {
        val segments = parse(pointer)
        if (segments.isEmpty()) return value
        return writeRec(root, segments, 0, value)
    }

    private fun writeRec(
        node: JsonElement,
        segments: List<String>,
        index: Int,
        value: JsonElement,
    ): JsonElement {
        val seg = segments[index]
        val last = index == segments.lastIndex
        return when (node) {
            is JsonObject -> {
                val existing = node[seg] ?: JsonObject(emptyMap())
                val updated = if (last) value else writeRec(existing, segments, index + 1, value)
                buildJsonObject {
                    node.forEach { (k, v) -> if (k != seg) put(k, v) }
                    put(seg, updated)
                }
            }
            is JsonArray -> {
                val idx = seg.toIntOrNull()
                    ?: throw IllegalArgumentException("Non-numeric segment '$seg' applied to array")
                if (idx < 0 || idx > node.size) {
                    throw IllegalArgumentException("Array index $idx out of bounds for size ${node.size}")
                }
                val existing: JsonElement = if (idx < node.size) node[idx] else JsonNull
                val updated = if (last) value else writeRec(existing, segments, index + 1, value)
                buildJsonArray {
                    node.forEachIndexed { i, el -> if (i != idx) add(el) else add(updated) }
                    if (idx == node.size) add(updated)
                }
            }
            is JsonPrimitive, JsonNull -> {
                // Replace with a fresh object or array based on whether the next segment is numeric.
                val replacement: JsonElement = if (seg.toIntOrNull() != null) JsonArray(emptyList()) else JsonObject(emptyMap())
                writeRec(replacement, segments, index, value)
            }
        }
    }
}
