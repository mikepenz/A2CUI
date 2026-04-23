package dev.mikepenz.a2cui.agui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the reducer-level contract that `rememberCoAgentState` relies on:
 * - seeding from `/state/$name` or falling back to the initial
 * - reflecting upstream patches (STATE_DELTA) into the typed slot
 * - emitting downstream replace patches when local writes happen
 *
 * The composable itself is a thin Compose wrapper over this contract.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RememberCoAgentStateTest {

    @Serializable
    data class Counter(val n: Int = 0)

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test fun seeds_from_initial_when_store_is_empty() {
        val reducer = AguiStateReducer()
        val pointer = "/state/counter"
        assertEquals(null, reducer.store.read(pointer))
        // Simulating rememberCoAgentState seed path:
        val seed = reducer.store.read(pointer)?.let {
            runCatching { json.decodeFromJsonElement(Counter.serializer(), it) }.getOrNull()
        } ?: Counter(0)
        assertEquals(Counter(0), seed)
    }

    @Test fun upstream_patch_reflects_into_typed_slot() = runTest {
        val reducer = AguiStateReducer()
        reducer.apply(
            AguiEvent.StateSnapshot(
                snapshot = buildJsonObject {
                    put("state", buildJsonObject {
                        put("counter", buildJsonObject { put("n", JsonPrimitive(0)) })
                    })
                },
            ),
        )

        reducer.state.test {
            val first = awaitItem()
            assertEquals(
                0,
                first.jsonObject["state"]!!.jsonObject["counter"]!!.jsonObject["n"]!!.jsonPrimitive.content.toInt(),
            )
            // Upstream delta flips the counter.
            reducer.apply(
                AguiEvent.StateDelta(
                    delta = buildJsonArray {
                        add(buildJsonObject {
                            put("op", JsonPrimitive("replace"))
                            put("path", JsonPrimitive("/state/counter/n"))
                            put("value", JsonPrimitive(5))
                        })
                    },
                ),
            )
            val second = awaitItem()
            val decoded = json.decodeFromJsonElement(
                Counter.serializer(),
                second.jsonObject["state"]!!.jsonObject["counter"]!!,
            )
            assertEquals(Counter(5), decoded)
        }
    }

    @Test fun downstream_local_write_emits_replace_patch() = runTest {
        val reducer = AguiStateReducer()
        reducer.outbound.test {
            val encoded = json.encodeToJsonElement(Counter.serializer(), Counter(7))
            reducer.writeLocal("/state/counter", encoded)
            val patch = awaitItem()
            assertEquals(1, patch.size)
            val op = patch[0].jsonObject
            assertEquals("replace", op["op"]!!.jsonPrimitive.content)
            assertEquals("/state/counter", op["path"]!!.jsonPrimitive.content)
            assertEquals(
                Counter(7),
                json.decodeFromJsonElement(Counter.serializer(), op["value"]!!),
            )
        }
        // And the store was updated.
        val stored = reducer.store.read("/state/counter")!!
        assertEquals(Counter(7), json.decodeFromJsonElement(Counter.serializer(), stored))
    }
}
