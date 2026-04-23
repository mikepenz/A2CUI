package dev.mikepenz.a2cui.agui

import dev.mikepenz.a2cui.core.A2uiParser
import dev.mikepenz.a2cui.core.A2uiStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Translation layer sitting between [AguiEvent]s and A2CUI's own abstractions.
 *
 * CopilotKit carries A2UI frames as AG-UI `CUSTOM` events named `"a2ui"`, with the frame
 * embedded in the `value` field. This bridge extracts those payloads and re-emits them
 * through the standard [A2uiParser] path. Tool calls and state events are exposed as
 * separate flows so apps can wire per-concern consumers.
 */
public class AguiA2cuiBridge(
    private val parser: A2uiParser = A2uiParser(),
    /** Name matched against `CUSTOM.name` to detect A2UI-carrying events. */
    public val a2uiCustomEventName: String = "a2ui",
) {

    /** Extract A2UI frames from `CUSTOM` events whose name matches [a2uiCustomEventName]. */
    public fun a2uiFrames(events: Flow<AguiEvent>): Flow<A2uiStreamEvent> =
        events.mapNotNull { event ->
            if (event !is AguiEvent.Custom || event.name != a2uiCustomEventName) return@mapNotNull null
            parser.parseOne(event.value.toString())
        }

    /** Streaming tool-call reducer: builds [ToolCallEvent]s out of `TOOL_CALL_*` events. */
    public fun toolCalls(events: Flow<AguiEvent>): Flow<ToolCallEvent> {
        val argBuffers = mutableMapOf<String, StringBuilder>()
        val nameById = mutableMapOf<String, String>()
        val parentById = mutableMapOf<String, String?>()
        return events.mapNotNull { event ->
            when (event) {
                is AguiEvent.ToolCallStart -> {
                    nameById[event.toolCallId] = event.toolCallName
                    parentById[event.toolCallId] = event.parentMessageId
                    argBuffers[event.toolCallId] = StringBuilder()
                    ToolCallEvent.Started(
                        toolCallId = event.toolCallId,
                        toolCallName = event.toolCallName,
                        parentMessageId = event.parentMessageId,
                    )
                }
                is AguiEvent.ToolCallArgs -> {
                    val name = nameById[event.toolCallId] ?: return@mapNotNull null
                    val buf = argBuffers.getOrPut(event.toolCallId) { StringBuilder() }
                    buf.append(event.delta)
                    ToolCallEvent.Args(
                        toolCallId = event.toolCallId,
                        toolCallName = name,
                        args = buf.toString(),
                    )
                }
                is AguiEvent.ToolCallEnd -> {
                    val name = nameById[event.toolCallId] ?: return@mapNotNull null
                    ToolCallEvent.ArgsComplete(
                        toolCallId = event.toolCallId,
                        toolCallName = name,
                        args = (argBuffers[event.toolCallId] ?: StringBuilder()).toString(),
                    )
                }
                is AguiEvent.ToolCallResult -> {
                    val name = nameById[event.toolCallId] ?: return@mapNotNull null
                    val result = ToolCallEvent.Result(
                        toolCallId = event.toolCallId,
                        toolCallName = name,
                        content = event.content,
                        messageId = event.messageId,
                    )
                    // Tool call is closed; free buffers.
                    argBuffers.remove(event.toolCallId)
                    nameById.remove(event.toolCallId)
                    parentById.remove(event.toolCallId)
                    result
                }
                else -> null
            }
        }
    }

    /** Filter only `RUN_FINISHED` events whose outcome is `"interrupt"` — ready to present as HITL prompts. */
    public fun interrupts(events: Flow<AguiEvent>): Flow<List<Interrupt>> =
        events.mapNotNull { event ->
            if (event is AguiEvent.RunFinished && event.outcome == "interrupt") event.interrupts else null
        }
}
