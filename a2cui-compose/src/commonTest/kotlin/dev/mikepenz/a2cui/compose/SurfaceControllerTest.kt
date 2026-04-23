package dev.mikepenz.a2cui.compose

import app.cash.turbine.test
import dev.mikepenz.a2cui.core.A2uiClientMessage
import dev.mikepenz.a2cui.core.ErrorCode
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SurfaceControllerTest {

    private fun newController(
        transport: FakeTransport,
        scope: TestScope,
    ): SurfaceController {
        val registry = ComponentRegistry().register("Text") { _, _ -> }.register("Column") { _, _ -> }
        return SurfaceController(registry, transport, scope).also { it.start() }
    }

    @Test fun createSurface_adds_empty_surface() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        advanceUntilIdle()

        val state = assertNotNull(controller.surfaces.value["s"])
        assertEquals("s", state.surfaceId)
        assertEquals("c", state.catalogId)
        assertTrue(state.nodesById.isEmpty())
        controller.close()
    }

    @Test fun updateComponents_merges_into_existing_surface() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        transport.emit(
            """
            {"version":"v0.9","updateComponents":{"surfaceId":"s","components":[
              {"id":"root","component":"Column","children":["t"]},
              {"id":"t","component":"Text","text":"Hello"}
            ]}}
            """.trimIndent()
        )
        advanceUntilIdle()

        val state = controller.surfaces.value["s"]!!
        assertEquals(2, state.nodesById.size)
        assertEquals("Column", state.nodesById["root"]?.component)
        assertEquals("Text", state.nodesById["t"]?.component)
        controller.close()
    }

    @Test fun updateComponents_preserves_prior_nodes() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        transport.emit(
            """{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[
               {"id":"a","component":"Text","text":"A"}]}}"""
        )
        transport.emit(
            """{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[
               {"id":"b","component":"Text","text":"B"}]}}"""
        )
        advanceUntilIdle()

        val nodes = controller.surfaces.value["s"]!!.nodesById
        assertEquals(setOf("a", "b"), nodes.keys)
        controller.close()
    }

    @Test fun updateDataModel_writes_through_to_surface_data_model() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        transport.emit(
            """{"version":"v0.9","updateDataModel":{"surfaceId":"s","path":"/form/email","value":"u@x"}}"""
        )
        advanceUntilIdle()

        val dm = controller.surfaces.value["s"]!!.dataModel
        assertEquals("u@x", (dm.read("/form/email") as JsonPrimitive).content)
        controller.close()
    }

    @Test fun deleteSurface_removes_it() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        transport.emit("""{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}""")
        advanceUntilIdle()

        assertNull(controller.surfaces.value["s"])
        controller.close()
    }

    @Test fun parse_error_forwards_error_envelope_and_continues() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        transport.emit("not json")
        transport.emit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
        advanceUntilIdle()

        assertNotNull(controller.surfaces.value["s"])
        val errs = transport.sent.map { it }
        assertTrue(errs.any { ErrorCode.PARSE_ERROR in it }, "expected parse error in sent: $errs")
        controller.close()
    }

    @Test fun dispatch_sends_action_envelope() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        controller.dispatch(
            surfaceId = "s",
            name = "submit",
            sourceComponentId = "btn",
            context = buildJsonObject { put("email", JsonPrimitive("u@x")) },
        )
        advanceUntilIdle()

        val sent = transport.sent.single()
        assertTrue("submit" in sent)
        assertTrue("u@x" in sent)
        controller.close()
    }

    @Test fun close_releases_transport_and_rejects_restart() = runTest {
        val transport = FakeTransport()
        val controller = newController(transport, this)
        controller.close()
        // Transport was closed as part of controller.close(); further emits throw.
        val ex = kotlin.runCatching { transport.tryEmit("""{}""") }
        assertTrue(ex.isFailure)
        assertIs<IllegalStateException>(ex.exceptionOrNull())
        // Second close is a no-op (idempotent).
        controller.close()
    }
}
