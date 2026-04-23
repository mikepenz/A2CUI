package dev.mikepenz.a2cui.actions

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Tolerant accumulator for `TOOL_CALL_ARGS` delta streams.
 *
 * LLMs stream tool-call arguments as partial JSON (`{"city":"San`, then ` Francisco"}`). A
 * naive `Json.parse` fails on mid-stream input; this merger appends every delta to a buffer,
 * then attempts parsing with progressively-applied closing tokens (`}`, `]`, `"`) so
 * generative-UI renderers can read a best-effort [JsonObject] after every tick and recompose.
 *
 * Not thread-safe — each tool-call id should own one instance.
 */
public class StreamingArgsMerger(
    private val json: Json = defaultJson,
) {
    private val buffer = StringBuilder()
    private var lastGood: JsonObject = JsonObject(emptyMap())

    /** Append [delta] and return the best-effort parsed object after this tick. */
    public fun append(delta: String): JsonObject {
        buffer.append(delta)
        return tryParse(buffer.toString()) ?: lastGood
    }

    /** Final parse attempt when the stream closes. Returns the last successful parse on failure. */
    public fun finalParse(): JsonObject = tryParse(buffer.toString()) ?: lastGood

    /** The full buffered string so far (useful for typed decoding via `Json.decodeFromString<T>`). */
    public fun buffered(): String = buffer.toString()

    private fun tryParse(s: String): JsonObject? {
        if (s.isBlank()) return null
        decodeObject(s)?.let { lastGood = it; return it }
        val closers = computeClosers(s)
        if (closers.isNotEmpty()) {
            decodeObject(s + closers)?.let { lastGood = it; return it }
        }
        return null
    }

    private fun decodeObject(s: String): JsonObject? = try {
        json.parseToJsonElement(s) as? JsonObject
    } catch (_: SerializationException) { null }
      catch (_: IllegalArgumentException) { null }

    /**
     * Compute closing tokens that balance the partial [s]. Tracks the open-token stack so
     * closers are emitted in LIFO order (essential for mixed nesting like arrays-of-objects).
     * String quotes are closed first if the buffer ends mid-string.
     */
    private fun computeClosers(s: String): String {
        val stack = mutableListOf<Char>()
        var inString = false
        var escape = false
        for (c in s) {
            if (escape) { escape = false; continue }
            if (c == '\\') { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '{', '[' -> stack.add(c)
                '}' -> if (stack.lastOrNull() == '{') stack.removeAt(stack.lastIndex)
                ']' -> if (stack.lastOrNull() == '[') stack.removeAt(stack.lastIndex)
            }
        }
        val sb = StringBuilder()
        if (inString) sb.append('"')
        for (i in stack.indices.reversed()) {
            sb.append(if (stack[i] == '{') '}' else ']')
        }
        return sb.toString()
    }

    public companion object {
        internal val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
