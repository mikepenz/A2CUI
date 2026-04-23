package dev.mikepenz.a2cui.transport

import dev.mikepenz.a2cui.core.A2uiClientMessage
import dev.mikepenz.a2cui.core.A2uiParser
import kotlinx.coroutines.flow.Flow

/**
 * Bidirectional channel carrying A2UI frames between client and agent backend.
 *
 * Implementations are transport-agnostic to [A2uiClientMessage] — they accept outbound messages
 * and expose inbound raw JSON strings. Parsing is the responsibility of the caller (typically
 * a [A2uiParser]) so transports can be reused for protocols layered on top of A2UI such as
 * AG-UI, where frames arrive inside a `CUSTOM` event envelope.
 */
public interface A2uiTransport : AutoCloseable {

    /**
     * Cold [Flow] of inbound JSON frame strings. Collection starts the underlying connection
     * (SSE / WebSocket / etc.); cancellation tears it down. Each emitted element is expected
     * to be one complete JSON document — framing is the transport's responsibility.
     */
    public fun incoming(): Flow<String>

    /**
     * Send an outbound client message. The default implementation encodes the message via
     * [A2uiParser.encode] and delegates to [sendRaw]. SSE-only transports that do not support
     * a return channel may throw [UnsupportedOperationException].
     */
    public suspend fun send(message: A2uiClientMessage) {
        sendRaw(defaultParser.encode(message))
    }

    /**
     * Send a pre-encoded JSON frame. Lower-level escape hatch used by [send] and by tests.
     */
    public suspend fun sendRaw(raw: String)

    /** Release resources. Safe to call multiple times. */
    override fun close()

    public companion object {
        internal val defaultParser: A2uiParser = A2uiParser()
    }
}
