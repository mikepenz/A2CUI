package dev.mikepenz.a2cui.agui.mockserver

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Minimal AG-UI SSE server for local integration testing.
 *
 * Streams a scripted AG-UI sequence:
 *  1. `RUN_STARTED`
 *  2. `TEXT_MESSAGE_START` / `TEXT_MESSAGE_CONTENT` / `TEXT_MESSAGE_END` — an assistant greeting
 *  3. `CUSTOM` events (name = `"a2ui"`) carrying A2UI frames: `createSurface`, `updateComponents`,
 *     `updateDataModel`
 *  4. `RUN_FINISHED`
 *
 * Endpoint: `GET /events` (SSE stream). Each SSE `data` line is one JSON-encoded AG-UI event.
 */
public class MockAguiServer(
    public val port: Int = 0,
    public val surfaceId: String = "demo",
    private val frameDelayMillis: Long = 100L,
) {
    private var engine: EmbeddedServer<*, *>? = null
    private val json = Json { encodeDefaults = false }

    /** Start the server on the configured port (0 = ephemeral). Returns the bound port. */
    public fun start(wait: Boolean = false): Int {
        val server = embeddedServer(CIO, port = port) {
            install(SSE)
            routing {
                sse("/events") {
                    val threadId = "thread-${kotlin.random.Random.nextInt()}"
                    val runId = "run-${kotlin.random.Random.nextInt()}"
                    val messageId = "msg-1"

                    send(data = encodeEvent("RUN_STARTED") {
                        put("threadId", threadId); put("runId", runId)
                    })
                    delay(frameDelayMillis)

                    send(data = encodeEvent("TEXT_MESSAGE_START") {
                        put("messageId", messageId); put("role", "assistant")
                    })
                    send(data = encodeEvent("TEXT_MESSAGE_CONTENT") {
                        put("messageId", messageId); put("delta", "Rendering booking form…")
                    })
                    send(data = encodeEvent("TEXT_MESSAGE_END") {
                        put("messageId", messageId)
                    })
                    delay(frameDelayMillis)

                    for (frame in listOf(
                        SampleA2uiFrames.createSurface(surfaceId),
                        SampleA2uiFrames.bookingComponents(surfaceId),
                        SampleA2uiFrames.seedEmail(surfaceId),
                    )) {
                        val value = Json.parseToJsonElement(frame)
                        send(data = encodeEvent("CUSTOM") {
                            put("name", "a2ui")
                            put("value", value)
                        })
                        delay(frameDelayMillis)
                    }

                    send(data = encodeEvent("RUN_FINISHED") {
                        put("threadId", threadId); put("runId", runId)
                        put("outcome", "success")
                    })
                }
            }
        }
        engine = server
        server.start(wait = wait)
        return resolvedPort()
    }

    /** Resolve the actual bound port after start() — useful when port = 0. */
    public fun resolvedPort(): Int {
        val eng = engine ?: return port
        return runCatching {
            kotlinx.coroutines.runBlocking { eng.engine.resolvedConnectors().first().port }
        }.getOrDefault(port)
    }

    public fun stop(gracePeriodMillis: Long = 0L, timeoutMillis: Long = 1_000L) {
        engine?.stop(gracePeriodMillis, timeoutMillis)
        engine = null
    }

    private inline fun encodeEvent(type: String, builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): String {
        val obj: JsonObject = buildJsonObject {
            put("type", type)
            builder()
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }
}

/** Convenience entry-point for `java -jar` style local runs. */
public fun main() {
    val server = MockAguiServer(port = 8765)
    println("Mock AG-UI server starting on http://localhost:${server.port}/events")
    server.start(wait = true)
}
