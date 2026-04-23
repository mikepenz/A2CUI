package dev.mikepenz.a2cui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One piece of app state the agent should see in its prompt. `useCopilotReadable` equivalent.
 */
public data class ReadableEntry(val id: String, val description: String, val value: JsonElement)

public class ReadableRegistry {
    private val _entries = MutableStateFlow<Map<String, ReadableEntry>>(emptyMap())
    public val entries: StateFlow<Map<String, ReadableEntry>> = _entries.asStateFlow()

    public fun put(entry: ReadableEntry) {
        _entries.update { it + (entry.id to entry) }
    }

    public fun remove(id: String) {
        _entries.update { it - id }
    }

    /** Flatten current readables into a single JSON object for prompt injection. */
    public fun asPromptObject(): JsonObject {
        val current = _entries.value
        return JsonObject(current.mapValues { it.value.value })
    }
}

public val LocalReadableRegistry = staticCompositionLocalOf<ReadableRegistry> {
    error("LocalReadableRegistry not provided. Wrap your tree with CompositionLocalProvider.")
}

/**
 * Expose [value] to the agent as a named readable. Re-registers on every [value] change so
 * the agent always sees the fresh snapshot without the caller having to plumb updates.
 */
@Composable
public fun rememberReadable(
    id: String,
    description: String,
    value: JsonElement,
) {
    val registry = LocalReadableRegistry.current
    DisposableEffect(id, description, value, registry) {
        registry.put(ReadableEntry(id = id, description = description, value = value))
        onDispose { registry.remove(id) }
    }
}

/** Convenience overload: wrap a primitive in [JsonPrimitive]. */
@Composable
public fun rememberReadable(id: String, description: String, value: String) {
    rememberReadable(id, description, JsonPrimitive(value))
}
