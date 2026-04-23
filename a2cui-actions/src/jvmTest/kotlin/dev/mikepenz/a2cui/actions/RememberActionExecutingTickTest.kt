package dev.mikepenz.a2cui.actions

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.mikepenz.a2cui.agui.AguiEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

/**
 * Full composable path for `rememberAction` + `TypedArgsAccumulator`: drives a fake
 * `TOOL_CALL_ARGS` / `TOOL_CALL_END` sequence through the registered action's
 * `accumulatorFactory` and asserts the render callback observes `Executing(typed)` once the
 * partial JSON becomes decodable, and `Complete` on `TOOL_CALL_END`. Complements
 * `TypedArgsAccumulatorTest` by validating the `rememberAction` + `LocalActionRegistry` seam.
 */
@OptIn(ExperimentalTestApi::class)
class RememberActionExecutingTickTest {

    @Serializable
    data class Destination(val city: String, val nights: Int = 0)

    @Test fun rememberAction_surfaces_executing_then_complete() = runComposeUiTest {
        val registry = ActionRegistry()
        // Externally-driven status — feeds the registration's render composable.
        val externalStatus = mutableStateOf<ToolCallStatus<*>>(
            ToolCallStatus.InProgress<Destination>(JsonObject(emptyMap())),
        )

        setContent {
            CompositionLocalProvider(LocalActionRegistry provides registry) {
                rememberAction(
                    name = "plan_trip",
                    description = "Plan a trip",
                    schema = Destination.serializer(),
                ) { status ->
                    when (status) {
                        is ToolCallStatus.InProgress -> Text("status=inprogress")
                        is ToolCallStatus.Executing -> Text("status=executing city=${status.args.city} nights=${status.args.nights}")
                        is ToolCallStatus.Complete -> Text("status=complete city=${status.args.city}")
                        is ToolCallStatus.Failed -> Text("status=failed")
                    }
                }
                // Dispatcher stand-in: read the live registration and invoke its render with
                // the externally-driven status, forcing recomposition on each status change.
                val entries by registry.entries.collectAsState()
                val registration = entries["plan_trip"]
                val s by externalStatus
                registration?.render?.invoke(s)
            }
        }

        // Wait for the action to register and the initial InProgress render to appear.
        waitUntil(timeoutMillis = 5_000) {
            runCatching { onNodeWithText("status=inprogress").assertIsDisplayed() }.isSuccess
        }

        // Now drive the accumulator outside the composition, mirroring an AG-UI dispatcher.
        val registration = registry["plan_trip"]!!
        val accumulator = registration.accumulatorFactory!!.invoke()

        // Partial JSON delta — stays InProgress.
        runOnUiThread { externalStatus.value = accumulator.onDelta("{\"city\":\"Pa") }
        waitForIdle()
        onNodeWithText("status=inprogress").assertIsDisplayed()

        // Completing delta — becomes Executing(typed).
        val afterSecondDelta = accumulator.onDelta("ris\",\"nights\":3}")
        runOnUiThread { externalStatus.value = afterSecondDelta }
        waitUntil(timeoutMillis = 5_000) {
            runCatching { onNodeWithText("status=executing city=Paris nights=3").assertIsDisplayed() }.isSuccess
        }

        // End event — transitions to Complete with the final typed args.
        runOnUiThread { externalStatus.value = accumulator.onEnd(JsonNull) }
        waitUntil(timeoutMillis = 5_000) {
            runCatching { onNodeWithText("status=complete city=Paris").assertIsDisplayed() }.isSuccess
        }
        // AG-UI semantic mapping:
        //   TOOL_CALL_ARGS(delta=…)     → accumulator.onDelta(delta)
        //   TOOL_CALL_END               → accumulator.onEnd(JsonNull)
        //   TOOL_CALL_RESULT(content=…) → accumulator.onEnd(JsonPrimitive(content))
        @Suppress("UNUSED_VARIABLE")
        val _wireShape: List<AguiEvent> = listOf(
            AguiEvent.ToolCallStart(toolCallId = "tc1", toolCallName = "plan_trip"),
            AguiEvent.ToolCallArgs(toolCallId = "tc1", delta = ""),
            AguiEvent.ToolCallEnd(toolCallId = "tc1"),
            AguiEvent.ToolCallResult(toolCallId = "tc1", content = "ok"),
        )
        @Suppress("UNUSED_VARIABLE")
        val _resultShape: JsonPrimitive = JsonPrimitive("ok")
    }
}
