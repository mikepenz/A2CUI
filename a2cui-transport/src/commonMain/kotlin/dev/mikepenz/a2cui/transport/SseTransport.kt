package dev.mikepenz.a2cui.transport

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Server-Sent Events transport.
 *
 * SSE is inherently unidirectional (server→client), so outbound messages are delivered via a
 * separate HTTP `POST` to [sendUrl]. If [sendUrl] is `null`, the transport is receive-only and
 * [sendRaw] throws [UnsupportedOperationException].
 *
 * Each inbound SSE event's `data` field is emitted as one [String] frame; multi-line data
 * blocks are concatenated with `\n` per the SSE specification.
 */
public class SseTransport(
    private val httpClient: HttpClient,
    private val receiveUrl: String,
    private val sendUrl: String? = null,
    private val headers: Map<String, String> = emptyMap(),
) : A2uiTransport {

    override fun incoming(): Flow<String> = channelFlow {
        httpClient.sse(
            urlString = receiveUrl,
            request = { applyHeaders(this@SseTransport.headers) },
        ) {
            incoming.collect { event ->
                val data = event.data ?: return@collect
                if (data.isNotEmpty()) send(data)
            }
        }
    }

    override suspend fun sendRaw(raw: String) {
        val url = sendUrl
            ?: throw UnsupportedOperationException(
                "SseTransport has no sendUrl configured; use WebSocketTransport or supply sendUrl",
            )
        httpClient.post(url) {
            applyHeaders(this@SseTransport.headers)
            contentType(ContentType.Application.Json)
            setBody(raw)
        }
    }

    override fun close() {
        httpClient.close()
    }
}
