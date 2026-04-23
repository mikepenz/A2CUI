package dev.mikepenz.a2cui.agui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Compose-level round-trip of [rememberCoAgentState]: upstream `STATE_DELTA` must recompose
 * the bound UI; a local `state.value =` write must flush a JSON Patch on
 * [AguiStateReducer.outbound].
 */
@OptIn(ExperimentalTestApi::class)
class RememberCoAgentStateComposableTest {

    @Serializable
    data class Counter(val n: Int = 0)

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test fun upstream_delta_recomposes_and_click_flushes_patch() = runComposeUiTest {
        val reducer = AguiStateReducer()
        val outboundCaptured = java.util.concurrent.CopyOnWriteArrayList<kotlinx.serialization.json.JsonArray>()
        val collectorScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default,
        )
        collectorScope.launch { reducer.outbound.collect { outboundCaptured.add(it) } }

        setContent {
            var counter by rememberCoAgentState(
                name = "counter",
                initial = Counter(0),
                serializer = Counter.serializer(),
                reducer = reducer,
                json = json,
            )
            Text("n=${counter.n}")
            Button(onClick = { counter = Counter(counter.n + 1) }) { Text("Bump") }
        }

        waitForIdle()
        onNodeWithText("n=0").assertIsDisplayed()

        // Upstream agent pushes a snapshot: the UI must recompose to show n=5.
        reducer.apply(
            AguiEvent.StateSnapshot(
                snapshot = buildJsonObject {
                    put("state", buildJsonObject {
                        put("counter", buildJsonObject { put("n", JsonPrimitive(5)) })
                    })
                },
            ),
        )
        waitUntil(timeoutMillis = 5_000) {
            runCatching { onNodeWithText("n=5").assertIsDisplayed() }.isSuccess
        }

        // User taps the button: downstream `replace` JSON patch should flush on outbound with
        // the new (Counter.n=6) value.
        onNodeWithText("Bump").performClick()
        waitUntil(timeoutMillis = 5_000) {
            outboundCaptured.any { patch ->
                patch.any { op ->
                    val o = op.jsonObject
                    if (o["path"]?.jsonPrimitive?.content != "/state/counter") return@any false
                    val v = o["value"] ?: return@any false
                    runCatching { json.decodeFromJsonElement(Counter.serializer(), v) }
                        .getOrNull() == Counter(6)
                }
            }
        }

        val match = outboundCaptured.flatten().first { op ->
            val o = op.jsonObject
            o["path"]?.jsonPrimitive?.content == "/state/counter" &&
                runCatching { json.decodeFromJsonElement(Counter.serializer(), o["value"]!!) }.getOrNull() == Counter(6)
        }.jsonObject
        assertEquals("replace", match["op"]!!.jsonPrimitive.content)

        collectorScope.cancel()
    }
}
