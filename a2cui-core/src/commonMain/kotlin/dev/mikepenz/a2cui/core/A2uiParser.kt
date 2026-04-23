package dev.mikepenz.a2cui.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Streaming parser for A2UI server→client frames.
 *
 * Accepts a [Flow] of JSON strings (one frame per element — the transport layer handles SSE
 * event framing, WebSocket message boundaries, or newline-delimited JSON) and emits a
 * [Flow] of [A2uiStreamEvent] values. Parse failures are materialised as
 * [A2uiStreamEvent.ParseError] so the consumer can forward an [A2uiClientMessage.Error]
 * response upstream without tearing down the whole stream.
 */
public class A2uiParser(private val json: Json = A2uiJson) {

    /** Parse each incoming JSON string into a [A2uiStreamEvent]. Never throws on bad input. */
    public fun parse(frames: Flow<String>): Flow<A2uiStreamEvent> = frames.map(::parseOne)

    /** Parse a single JSON frame. Exposed for direct testing and one-shot flows. */
    public fun parseOne(raw: String): A2uiStreamEvent = try {
        A2uiStreamEvent.Frame(json.decodeFromString<A2uiFrame>(raw))
    } catch (e: SerializationException) {
        A2uiStreamEvent.ParseError(
            raw = raw,
            error = A2uiClientMessage.Error.Body(
                code = ErrorCode.PARSE_ERROR,
                message = e.message ?: "Invalid A2UI frame",
            ),
        )
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization throws IAE for some malformed JSON via JsonContentPolymorphicSerializer.
        A2uiStreamEvent.ParseError(
            raw = raw,
            error = A2uiClientMessage.Error.Body(
                code = ErrorCode.PARSE_ERROR,
                message = e.message ?: "Invalid A2UI frame",
            ),
        )
    } catch (e: IllegalStateException) {
        // Defensive: custom serializers or user-supplied call dispatchers may `error(...)` on
        // malformed payloads. We surface these the same way to keep the transport layer
        // resilient — a broken frame must not tear down the whole stream or the UI.
        A2uiStreamEvent.ParseError(
            raw = raw,
            error = A2uiClientMessage.Error.Body(
                code = ErrorCode.PARSE_ERROR,
                message = e.message ?: "Invalid A2UI frame",
            ),
        )
    }

    /** Encode an outbound client message to JSON. */
    public fun encode(message: A2uiClientMessage): String = json.encodeToString(message)

    /** Encode a server frame — used by tests and server-side A2UI emitters. */
    public fun encodeFrame(frame: A2uiFrame): String = json.encodeToString(frame)

    /**
     * Convenience: wrap a [A2uiClientMessage.Error.Body] into a full outbound message at the
     * current protocol version.
     */
    public fun wrapError(body: A2uiClientMessage.Error.Body): A2uiClientMessage.Error =
        A2uiClientMessage.Error(version = A2UI_PROTOCOL_VERSION, error = body)
}

/** Standardised error codes emitted by [A2uiParser] and catalog-side validators. */
public object ErrorCode {
    public const val PARSE_ERROR: String = "PARSE_ERROR"
    public const val UNKNOWN_COMPONENT: String = "UNKNOWN_COMPONENT"
    public const val VALIDATION_FAILED: String = "VALIDATION_FAILED"
    public const val UNKNOWN_ACTION: String = "UNKNOWN_ACTION"
    public const val UNKNOWN_SURFACE: String = "UNKNOWN_SURFACE"
}
