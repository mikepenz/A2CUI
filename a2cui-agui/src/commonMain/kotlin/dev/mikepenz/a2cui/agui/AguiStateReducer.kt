package dev.mikepenz.a2cui.agui

import dev.mikepenz.a2cui.core.DataModel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Applies AG-UI `STATE_SNAPSHOT` and `STATE_DELTA` events to a [DataModel]. This is the
 * agent-level state channel, separate from A2UI's per-surface data model — apps that use
 * CoAgent-style shared state should instantiate one reducer per logical state store.
 */
public class AguiStateReducer(public val store: DataModel = DataModel()) {

    public fun apply(event: AguiEvent) {
        when (event) {
            is AguiEvent.StateSnapshot -> {
                val next = event.snapshot
                store.write(pointer = "", value = next)
            }
            is AguiEvent.StateDelta -> {
                val current = store.snapshot()
                val updated = JsonPatch.apply(current, event.delta)
                store.write(pointer = "", value = updated)
            }
            else -> Unit
        }
    }
}
