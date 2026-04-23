package dev.mikepenz.a2cui.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Mutable JSON document addressed by RFC 6901 pointers.
 *
 * Backed by a [MutableStateFlow] so Compose collectors can subscribe to the root; [observe]
 * derives a pointer-scoped flow with `distinctUntilChanged` to avoid spurious recomposition.
 *
 * All writes produce fresh immutable snapshots (structural sharing is not yet implemented —
 * good enough for human-sized forms; can be optimised later if profiling warrants it).
 */
public class DataModel(initial: JsonElement = JsonObject(emptyMap())) {

    private val _root = MutableStateFlow(initial)

    /** Observe the entire document root. */
    public val root: StateFlow<JsonElement> = _root.asStateFlow()

    /** Return the current root snapshot. */
    public fun snapshot(): JsonElement = _root.value

    /** Read [pointer]; returns `null` if any segment is missing. */
    public fun read(pointer: String): JsonElement? = JsonPointer.read(_root.value, pointer)

    /** Write [value] at [pointer]. Creates intermediate objects as needed. */
    public fun write(pointer: String, value: JsonElement) {
        _root.value = JsonPointer.write(_root.value, pointer, value)
    }

    /** Observe [pointer]: emits the resolved element on every write that changes it. */
    public fun observe(pointer: String): Flow<JsonElement?> =
        _root.map { JsonPointer.read(it, pointer) }.distinctUntilChanged()
}
