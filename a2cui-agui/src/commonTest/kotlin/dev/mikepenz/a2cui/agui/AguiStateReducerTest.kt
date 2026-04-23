package dev.mikepenz.a2cui.agui

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AguiStateReducerTest {

    @Test fun snapshot_replaces_entire_store() {
        val reducer = AguiStateReducer()
        val snap = buildJsonObject { put("user", JsonPrimitive("Ada")) }
        reducer.apply(AguiEvent.StateSnapshot(snapshot = snap))
        assertEquals("Ada", reducer.store.read("/user")?.jsonPrimitive?.content)
    }

    @Test fun delta_patches_store_incrementally() {
        val reducer = AguiStateReducer()
        reducer.apply(
            AguiEvent.StateSnapshot(
                snapshot = buildJsonObject { put("count", JsonPrimitive(0)) },
            ),
        )
        reducer.apply(
            AguiEvent.StateDelta(
                delta = buildJsonArray {
                    add(buildJsonObject {
                        put("op", JsonPrimitive("replace"))
                        put("path", JsonPrimitive("/count"))
                        put("value", JsonPrimitive(1))
                    })
                },
            ),
        )
        assertEquals("1", reducer.store.read("/count")?.jsonPrimitive?.content)
    }

    @Test fun non_state_events_are_ignored() {
        val reducer = AguiStateReducer()
        reducer.apply(AguiEvent.RunStarted(threadId = "t", runId = "r"))
        assertEquals(null, reducer.store.read("/anything"))
    }
}
