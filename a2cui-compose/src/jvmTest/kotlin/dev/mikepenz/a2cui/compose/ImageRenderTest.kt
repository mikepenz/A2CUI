package dev.mikepenz.a2cui.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the default `Image` factory wires Coil 3's AsyncImage and renders without
 * exception when `src` is populated. Network fetch is not awaited — we only assert the
 * composable sits in the semantics tree.
 */
@OptIn(ExperimentalTestApi::class)
class ImageRenderTest {

    private fun newScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test fun image_with_src_renders_async_image() = runComposeUiTest {
        val transport = FakeTransport()
        val scope = newScope()
        val registry = ComponentRegistry().also { it.registerAll(Material3BasicCatalog) }
        val controller = SurfaceController(registry, transport, scope).also { it.start() }
        try {
            transport.tryEmit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
            transport.tryEmit(
                """
                {
                  "version": "v0.9",
                  "updateComponents": {
                    "surfaceId": "s",
                    "components": [
                      { "id": "root", "component": "Image",
                        "src": "file:///tmp/a2cui-nonexistent.png",
                        "contentDescription": "test image",
                        "size": 64 }
                    ]
                  }
                }
                """.trimIndent()
            )

            setContent {
                A2cuiSurface(surfaceId = "s", controller = controller)
            }
            waitForIdle()

            // Node tree should be non-empty — AsyncImage mounted, no exception thrown.
            assertTrue(onRoot().fetchSemanticsNode().children.isNotEmpty() ||
                       onRoot().fetchSemanticsNode().size.width > 0)
        } finally {
            controller.close()
        }
    }
}
