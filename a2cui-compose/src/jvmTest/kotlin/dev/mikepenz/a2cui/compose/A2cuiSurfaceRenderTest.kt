package dev.mikepenz.a2cui.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test

/**
 * End-to-end render test: drives a [SurfaceController] through `createSurface` +
 * `updateComponents` + `updateDataModel` frames and asserts the resulting Compose tree shows
 * the expected Material3 nodes.
 */
@OptIn(ExperimentalTestApi::class)
class A2cuiSurfaceRenderTest {

    private fun newScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private fun bookingComponents(id: String) = """
        {
          "version": "v0.9",
          "updateComponents": {
            "surfaceId": "$id",
            "components": [
              { "id": "root", "component": "Column", "spacing": 12, "children": ["title","email","submit"] },
              { "id": "title", "component": "Text", "text": "Book your table" },
              { "id": "email", "component": "TextField", "label": "Email", "value": { "path": "/form/email" } },
              { "id": "submit", "component": "Button", "text": "Submit",
                "action": { "event": { "name": "submit_booking" } } }
            ]
          }
        }
    """.trimIndent()

    @Test fun surface_renders_text_button_and_bound_textfield() = runComposeUiTest {
        val transport = FakeTransport()
        val scope = newScope()
        val registry = ComponentRegistry().also { it.registerAll(Material3BasicCatalog) }
        val controller = SurfaceController(registry, transport, scope).also { it.start() }
        try {
            transport.tryEmit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
            transport.tryEmit(bookingComponents("s"))
            transport.tryEmit(
                """{"version":"v0.9","updateDataModel":{"surfaceId":"s","path":"/form/email","value":"hello@example.com"}}"""
            )

            setContent {
                A2cuiSurface(surfaceId = "s", controller = controller)
            }
            waitForIdle()

            onNodeWithText("Book your table").assertIsDisplayed()
            onNodeWithText("Submit").assertIsDisplayed()
            onNode(hasText("hello@example.com")).assertIsDisplayed()
        } finally {
            controller.close()
        }
    }
}
