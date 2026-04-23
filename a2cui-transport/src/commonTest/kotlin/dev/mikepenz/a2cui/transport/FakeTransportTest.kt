package dev.mikepenz.a2cui.transport

import app.cash.turbine.test
import dev.mikepenz.a2cui.core.A2UI_PROTOCOL_VERSION
import dev.mikepenz.a2cui.core.A2uiClientMessage
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FakeTransportTest {

    @Test fun replays_seed_frames_to_new_collectors() = runTest {
        val seed = listOf(
            """{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""",
            """{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}""",
        )
        val transport = FakeTransport(seed)
        val collected = transport.incoming().take(2).toList()
        assertEquals(seed, collected)
    }

    @Test fun emits_delivered_to_live_collector() = runTest {
        val transport = FakeTransport()
        transport.incoming().test {
            transport.emit("""{"version":"v0.9","deleteSurface":{"surfaceId":"main"}}""")
            val first = awaitItem()
            assertTrue(first.contains("deleteSurface"))
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun captures_outbound_client_messages() = runTest {
        val transport = FakeTransport()
        val msg: A2uiClientMessage = A2uiClientMessage.Action(
            version = A2UI_PROTOCOL_VERSION,
            action = A2uiClientMessage.Action.Body(
                surfaceId = "s", name = "tap", sourceComponentId = "btn",
            ),
        )
        transport.send(msg)
        transport.sendRaw("""{"raw":"passthrough"}""")
        assertEquals(2, transport.sent.size)
        assertTrue(transport.sent[0].contains("tap"))
        assertEquals("""{"raw":"passthrough"}""", transport.sent[1])
    }

    @Test fun close_rejects_further_emits() = runTest {
        val transport = FakeTransport()
        transport.close()
        assertFailsWith<IllegalStateException> { transport.emit("x") }
        assertFailsWith<IllegalStateException> { transport.sendRaw("y") }
    }
}
