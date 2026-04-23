package dev.mikepenz.a2cui.agui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import dev.mikepenz.a2cui.core.JsonPointer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Bidirectionally bind a Compose [MutableState] to a slot inside an AG-UI [AguiStateReducer].
 *
 * The slot is addressed at RFC 6901 pointer `/state/$name` inside the reducer's store:
 * - Upstream: when the reducer applies a `STATE_SNAPSHOT` / `STATE_DELTA` that touches the
 *   slot, the new value is decoded via [serializer] and pushed into the returned state.
 * - Downstream: when the caller mutates `state.value`, the new value is encoded and written
 *   back via [AguiStateReducer.writeLocal], which emits a `replace` patch on the reducer's
 *   outbound flow for the host to forward to the agent.
 *
 * If the slot is empty at first read (or decoding fails) the state seeds from [initial].
 *
 * CopilotKit parity for LangGraph-style shared state.
 */
@Composable
public fun <T> rememberCoAgentState(
    name: String,
    initial: T,
    serializer: KSerializer<T>,
    reducer: AguiStateReducer,
    json: Json = defaultCoAgentJson,
): MutableState<T> {
    val pointer = "/state/$name"

    val state: MutableState<T> = remember(name, reducer) {
        val seed: T = reducer.store.read(pointer)?.let { el ->
            runCatching { json.decodeFromJsonElement(serializer, el) }.getOrNull()
        } ?: initial
        mutableStateOf(seed)
    }

    // Upstream: agent -> UI.
    LaunchedEffect(name, reducer, serializer) {
        reducer.state
            .map { root -> JsonPointer.read(root, pointer) }
            .distinctUntilChanged()
            .collect { el ->
                if (el == null) return@collect
                val decoded = try {
                    json.decodeFromJsonElement(serializer, el)
                } catch (_: SerializationException) {
                    return@collect
                } catch (_: IllegalArgumentException) {
                    return@collect
                }
                if (decoded != state.value) state.value = decoded
            }
    }

    // Downstream: UI -> agent.
    LaunchedEffect(name, reducer, serializer) {
        snapshotFlow { state.value }
            .collect { value ->
                val encoded: JsonElement = json.encodeToJsonElement(serializer, value)
                val existing = reducer.store.read(pointer)
                if (existing == encoded) return@collect
                reducer.writeLocal(pointer, encoded)
            }
    }

    return state
}

internal val defaultCoAgentJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
