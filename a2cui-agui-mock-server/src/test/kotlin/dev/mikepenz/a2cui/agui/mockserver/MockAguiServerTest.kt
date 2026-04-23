package dev.mikepenz.a2cui.agui.mockserver

import dev.mikepenz.a2cui.agui.AguiEvent
import dev.mikepenz.a2cui.agui.AguiEventParser
import dev.mikepenz.a2cui.agui.AguiStreamEvent
import dev.mikepenz.a2cui.transport.SseTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockAguiServerTest {

    private lateinit var server: MockAguiServer
    private var port: Int = 0

    @BeforeTest
    fun setUp() {
        server = MockAguiServer(port = 0, frameDelayMillis = 5L)
        port = server.start(wait = false)
    }

    @AfterTest
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `server streams the scripted AG-UI sequence`() = runBlocking {
        val client = HttpClient(CIO) { install(SSE) }
        val transport = SseTransport(
            httpClient = client,
            receiveUrl = "http://127.0.0.1:$port/events",
        )
        val parser = AguiEventParser()

        val events = withTimeout(10_000) {
            transport.incoming().take(8).toList()
        }
        transport.close()

        val decoded = events.map { parser.parseOne(it) }
        val typed = decoded.filterIsInstance<AguiStreamEvent.Event>().map { it.event }

        assertEquals(8, typed.size, "expected 8 AG-UI events, got ${decoded.size} (${decoded})")
        assertTrue(typed[0] is AguiEvent.RunStarted, "expected RunStarted, got ${typed[0]}; raw=${events[0]}")
        assertTrue(typed[1] is AguiEvent.TextMessageStart)
        assertTrue(typed[2] is AguiEvent.TextMessageContent)
        assertTrue(typed[3] is AguiEvent.TextMessageEnd)

        val customs = typed.filterIsInstance<AguiEvent.Custom>()
        assertEquals(3, customs.size, "expected 3 CUSTOM a2ui events")
        customs.forEach { assertEquals("a2ui", it.name) }

        val last = typed.last()
        assertTrue(last is AguiEvent.RunFinished)
        assertEquals("success", last.outcome)
    }
}
