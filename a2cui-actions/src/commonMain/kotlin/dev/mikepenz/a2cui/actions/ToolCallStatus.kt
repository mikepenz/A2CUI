package dev.mikepenz.a2cui.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * State of one streaming tool call from the perspective of a generative-UI renderer.
 *
 * [T] is the decoded, typed-args payload. [InProgress] carries only the partial [JsonObject]
 * accumulated from `TOOL_CALL_ARGS` deltas — renderers that want to show a live view (e.g. a
 * spreadsheet that fills row-by-row) read the partial. [Executing] and later stages carry a
 * fully decoded [T]; if decoding fails the status transitions to [Failed].
 */
public sealed interface ToolCallStatus<out T> {

    /** Tool call started, or args still streaming; [partial] holds a best-effort parse so far. */
    public data class InProgress<T>(val partial: JsonObject) : ToolCallStatus<T>

    /** Args complete, tool is running (no result yet). */
    public data class Executing<T>(val args: T) : ToolCallStatus<T>

    /** Tool has executed successfully. [result] is the raw JSON content from the AG-UI event. */
    public data class Complete<T>(val args: T, val result: JsonElement) : ToolCallStatus<T>

    /** Decoding / execution failed — the render function should surface a fallback UI. */
    public data class Failed<T>(val error: Throwable, val partial: JsonObject) : ToolCallStatus<T>
}
