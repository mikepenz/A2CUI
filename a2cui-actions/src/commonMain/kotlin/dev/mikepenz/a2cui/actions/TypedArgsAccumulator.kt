package dev.mikepenz.a2cui.actions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Stateful adapter that turns a [StreamingArgsMerger] delta stream into a sequence of
 * [ToolCallStatus] transitions for a specific typed payload [T].
 *
 * Semantics (per STATUS.md §5.1, partial-args typed decoding):
 * - Each [onDelta] appends to the merger, then tries `decodeArgs(buffered, serializer)`.
 *   - On decode success, emits `Executing(typed)` (but only when the decoded value changes,
 *     to avoid churn).
 *   - On decode failure, stays in `InProgress(partial)` — mid-stream failures are expected.
 * - [onEnd] emits `Complete(typed, result)` on a successful final decode, or `Failed` if the
 *   final buffer still won't parse.
 *
 * Not thread-safe. One instance per tool-call id.
 */
public class TypedArgsAccumulator<T : Any>(
    private val serializer: KSerializer<T>,
    private val json: Json = defaultActionJson,
    private val merger: StreamingArgsMerger = StreamingArgsMerger(),
) {
    private var lastTyped: T? = null

    /** Current best-effort status; initial state is `InProgress` with the empty partial object. */
    public var status: ToolCallStatus<T> = ToolCallStatus.InProgress(merger.append(""))
        private set

    /** Append [delta] from a `TOOL_CALL_ARGS` event; returns the resulting status. */
    public fun onDelta(delta: String): ToolCallStatus<T> {
        val partial = merger.append(delta)
        val buffered = merger.buffered()
        val decoded = try {
            json.decodeFromString(serializer, buffered)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        status = if (decoded != null) {
            if (decoded != lastTyped) {
                lastTyped = decoded
                ToolCallStatus.Executing(decoded)
            } else {
                status  // No change — keep prior Executing.
            }
        } else {
            ToolCallStatus.InProgress(partial)
        }
        return status
    }

    /**
     * Finalise on `TOOL_CALL_END`. If [result] is provided (from a later `TOOL_CALL_RESULT`),
     * the `Complete` carries it; otherwise it carries [kotlinx.serialization.json.JsonNull].
     */
    public fun onEnd(
        result: kotlinx.serialization.json.JsonElement = kotlinx.serialization.json.JsonNull,
    ): ToolCallStatus<T> {
        val buffered = merger.buffered()
        val decoded = try {
            json.decodeFromString(serializer, buffered)
        } catch (cause: SerializationException) {
            status = ToolCallStatus.Failed(cause, merger.finalParse())
            return status
        } catch (cause: IllegalArgumentException) {
            status = ToolCallStatus.Failed(cause, merger.finalParse())
            return status
        }
        lastTyped = decoded
        status = ToolCallStatus.Complete(decoded, result)
        return status
    }
}
