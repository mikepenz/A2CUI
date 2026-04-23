package dev.mikepenz.a2cui.agui

import app.cash.turbine.test
import dev.mikepenz.a2cui.core.A2uiFrame
import dev.mikepenz.a2cui.core.A2uiStreamEvent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AguiA2cuiBridgeTest {

    private val bridge = AguiA2cuiBridge()

    @Test fun extracts_a2ui_frames_from_custom_events() = runTest {
        val a2uiPayload = buildJsonObject {
            put("version", JsonPrimitive("v0.9"))
            put("deleteSurface", buildJsonObject { put("surfaceId", JsonPrimitive("s")) })
        }
        val events = flowOf<AguiEvent>(
            AguiEvent.RunStarted(threadId = "t", runId = "r"),
            AguiEvent.Custom(name = "a2ui", value = a2uiPayload),
            AguiEvent.RunFinished(threadId = "t", runId = "r", outcome = "success"),
        )
        bridge.a2uiFrames(events).test {
            val first = assertIs<A2uiStreamEvent.Frame>(awaitItem())
            assertIs<A2uiFrame.DeleteSurface>(first.frame)
            awaitComplete()
        }
    }

    @Test fun ignores_custom_events_with_other_names() = runTest {
        val events = flowOf<AguiEvent>(
            AguiEvent.Custom(name = "metrics", value = JsonPrimitive("ok")),
        )
        bridge.a2uiFrames(events).test { awaitComplete() }
    }

    @Test fun tool_calls_streaming_reducer_accumulates_args() = runTest {
        val events = flowOf<AguiEvent>(
            AguiEvent.ToolCallStart(toolCallId = "tc1", toolCallName = "fetch"),
            AguiEvent.ToolCallArgs(toolCallId = "tc1", delta = """{"city":"San"""),
            AguiEvent.ToolCallArgs(toolCallId = "tc1", delta = """ Francisco"}"""),
            AguiEvent.ToolCallEnd(toolCallId = "tc1"),
            AguiEvent.ToolCallResult(
                toolCallId = "tc1",
                content = """{"tempF":62}""",
            ),
        )
        bridge.toolCalls(events).test {
            assertIs<ToolCallEvent.Started>(awaitItem())
            val a1 = assertIs<ToolCallEvent.Args>(awaitItem())
            assertTrue(a1.args.endsWith("San"))
            val a2 = assertIs<ToolCallEvent.Args>(awaitItem())
            assertEquals("""{"city":"San Francisco"}""", a2.args)
            val end = assertIs<ToolCallEvent.ArgsComplete>(awaitItem())
            assertEquals("""{"city":"San Francisco"}""", end.args)
            val r = assertIs<ToolCallEvent.Result>(awaitItem())
            assertEquals("""{"tempF":62}""", r.content)
            awaitComplete()
        }
    }

    @Test fun interrupts_surfaces_pending_approvals() = runTest {
        val events = flowOf<AguiEvent>(
            AguiEvent.RunFinished(
                threadId = "t",
                runId = "r",
                outcome = "interrupt",
                interrupts = listOf(
                    Interrupt(id = "i1", reason = "tool_call", toolCallId = "tc"),
                ),
            ),
        )
        bridge.interrupts(events).test {
            val ints = awaitItem()
            assertEquals(1, ints.size)
            assertEquals("i1", ints[0].id)
            awaitComplete()
        }
    }
}
