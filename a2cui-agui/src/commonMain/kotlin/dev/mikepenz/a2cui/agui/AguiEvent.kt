package dev.mikepenz.a2cui.agui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * One event from the AG-UI protocol stream. Discriminated by `type` at the top level,
 * matching the wire format from [docs.ag-ui.com](https://docs.ag-ui.com/sdk/js/core/events).
 *
 * This is a curated subset — 16 events covering Lifecycle, Text, Tool, State, and Special
 * categories. Reasoning events and `*_CHUNK` convenience duplicates are not modeled yet;
 * implementations that encounter them will drop to [Raw] via the `ignoreUnknownKeys` flag.
 */
@Serializable
public sealed interface AguiEvent {
    public val timestamp: Long?

    // ---- Lifecycle ----

    @Serializable @SerialName("RUN_STARTED")
    public data class RunStarted(
        val threadId: String,
        val runId: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("RUN_FINISHED")
    public data class RunFinished(
        val threadId: String,
        val runId: String,
        /** One of `"success"` / `"interrupt"` per the draft interrupt spec; optional for legacy runs. */
        val outcome: String? = null,
        val interrupts: List<Interrupt> = emptyList(),
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("RUN_ERROR")
    public data class RunError(
        val message: String,
        val code: String? = null,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("STEP_STARTED")
    public data class StepStarted(
        val stepName: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("STEP_FINISHED")
    public data class StepFinished(
        val stepName: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    // ---- Text ----

    @Serializable @SerialName("TEXT_MESSAGE_START")
    public data class TextMessageStart(
        val messageId: String,
        val role: String = "assistant",
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("TEXT_MESSAGE_CONTENT")
    public data class TextMessageContent(
        val messageId: String,
        val delta: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("TEXT_MESSAGE_END")
    public data class TextMessageEnd(
        val messageId: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    // ---- Tool calls ----

    @Serializable @SerialName("TOOL_CALL_START")
    public data class ToolCallStart(
        val toolCallId: String,
        val toolCallName: String,
        val parentMessageId: String? = null,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("TOOL_CALL_ARGS")
    public data class ToolCallArgs(
        val toolCallId: String,
        val delta: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("TOOL_CALL_END")
    public data class ToolCallEnd(
        val toolCallId: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("TOOL_CALL_RESULT")
    public data class ToolCallResult(
        val toolCallId: String,
        val messageId: String? = null,
        val content: String,
        override val timestamp: Long? = null,
    ) : AguiEvent

    // ---- State ----

    @Serializable @SerialName("STATE_SNAPSHOT")
    public data class StateSnapshot(
        val snapshot: JsonElement,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("STATE_DELTA")
    public data class StateDelta(
        /** RFC 6902 JSON Patch operations. */
        val delta: JsonArray,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("MESSAGES_SNAPSHOT")
    public data class MessagesSnapshot(
        val messages: JsonArray,
        override val timestamp: Long? = null,
    ) : AguiEvent

    // ---- Special ----

    @Serializable @SerialName("RAW")
    public data class Raw(
        val event: JsonElement,
        override val timestamp: Long? = null,
    ) : AguiEvent

    @Serializable @SerialName("CUSTOM")
    public data class Custom(
        val name: String,
        val value: JsonElement,
        override val timestamp: Long? = null,
    ) : AguiEvent
}

/** One pending approval request inside a `RUN_FINISHED { outcome = "interrupt" }`. */
@Serializable
public data class Interrupt(
    val id: String,
    val reason: String,
    val message: String? = null,
    val toolCallId: String? = null,
    val responseSchema: JsonObject? = null,
    val expiresAt: Long? = null,
)
