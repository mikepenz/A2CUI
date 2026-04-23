package dev.mikepenz.a2cui.agui.mockserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Round-trips a `POST /events` with an `X-Thread-Id` header and asserts that:
 *  - the server records the action body
 *  - the thread id is echoed back as a response header
 *  - a subsequent POST on the same thread id reuses the same session id
 *  - the response body is a valid SSE stream with RUN_STARTED and RUN_FINISHED
 */
class MockAguiServerPostEventsTest {

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
    fun `post events round trips action with thread id`() = runBlocking {
        val client = HttpClient(CIO)
        val threadId = "thread-abc"
        val actionBody = """
            {"version":"v0.9","action":{"surfaceId":"demo","name":"submit",
             "sourceComponentId":"btn","context":{"email":"me@example.com"}}}
        """.trimIndent()

        val response = client.post("http://127.0.0.1:$port/events") {
            header("X-Thread-Id", threadId)
            contentType(ContentType.Application.Json)
            setBody(actionBody)
        }

        assertEquals(threadId, response.headers["X-Thread-Id"], "server must echo X-Thread-Id")
        val body = response.bodyAsText()
        assertTrue(body.contains("RUN_STARTED"), "expected RUN_STARTED in SSE body; got: $body")
        assertTrue(body.contains("RUN_FINISHED"), "expected RUN_FINISHED in SSE body; got: $body")

        assertEquals(1, server.receivedActions.size)
        val (recordedThread, recordedBody) = server.receivedActions[0]
        assertEquals(threadId, recordedThread)
        assertTrue(recordedBody.contains("submit"), "expected action name forwarded; got $recordedBody")

        val sessionId = server.threadSessions[threadId]
        assertNotNull(sessionId, "thread id must map to a session id")

        // Second POST on same thread reuses the same session.
        client.post("http://127.0.0.1:$port/events") {
            header("X-Thread-Id", threadId)
            contentType(ContentType.Application.Json)
            setBody(actionBody)
        }
        assertEquals(sessionId, server.threadSessions[threadId], "session id must persist across turns")
        assertEquals(2, server.receivedActions.size)

        client.close()
    }
}
