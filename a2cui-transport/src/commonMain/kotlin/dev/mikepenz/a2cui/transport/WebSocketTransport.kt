package dev.mikepenz.a2cui.transport

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bidirectional WebSocket transport. Each inbound text [Frame] is emitted as one JSON string;
 * binary frames are ignored. Outbound [sendRaw] writes a single text frame.
 *
 * The session is established lazily on the first [incoming] collection and is reused by
 * subsequent [sendRaw] calls within the same collector lifetime. [close] terminates the
 * underlying [HttpClient] — cancelling the collector coroutine is the preferred shutdown path.
 */
public class WebSocketTransport(
    private val httpClient: HttpClient,
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
) : A2uiTransport {

    private val sessionMutex = Mutex()
    private var session: WebSocketSession? = null

    override fun incoming(): Flow<String> = channelFlow {
        val s = httpClient.webSocketSession(urlString = url) {
            applyHeaders(this@WebSocketTransport.headers)
        }
        sessionMutex.withLock { session = s }
        try {
            for (frame in s.incoming) {
                if (frame is Frame.Text) send(frame.readText())
            }
        } finally {
            // Structured concurrency: cancelling the surrounding scope cancels the session.
            sessionMutex.withLock { if (session === s) session = null }
        }
    }

    override suspend fun sendRaw(raw: String) {
        val s = sessionMutex.withLock { session }
            ?: throw IllegalStateException(
                "WebSocket session not established — call incoming() first",
            )
        s.send(Frame.Text(raw))
    }

    override fun close() {
        httpClient.close()
    }
}
