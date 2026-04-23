package dev.mikepenz.a2cui.agui

import kotlinx.serialization.json.JsonElement

/**
 * State of one streaming tool call, assembled from `TOOL_CALL_START` / `TOOL_CALL_ARGS` /
 * `TOOL_CALL_END` / `TOOL_CALL_RESULT` events.
 *
 * The `args` string is the concatenation of every `delta` received so far — it may be
 * mid-JSON and not parse cleanly. Consumers that render generative UI from partial args
 * should use a lenient JSON merger (see `:a2cui-actions` layer).
 */
public sealed interface ToolCallEvent {
    public val toolCallId: String
    public val toolCallName: String

    /** A new tool call started; no args yet. */
    public data class Started(
        override val toolCallId: String,
        override val toolCallName: String,
        val parentMessageId: String? = null,
    ) : ToolCallEvent

    /** Tool call args are being streamed; [args] holds everything accumulated so far. */
    public data class Args(
        override val toolCallId: String,
        override val toolCallName: String,
        val args: String,
    ) : ToolCallEvent

    /** Tool call args are complete; no further deltas will arrive. */
    public data class ArgsComplete(
        override val toolCallId: String,
        override val toolCallName: String,
        val args: String,
    ) : ToolCallEvent

    /** Tool has executed; [content] is the raw result payload (typically JSON). */
    public data class Result(
        override val toolCallId: String,
        override val toolCallName: String,
        val content: String,
        val messageId: String? = null,
    ) : ToolCallEvent
}
