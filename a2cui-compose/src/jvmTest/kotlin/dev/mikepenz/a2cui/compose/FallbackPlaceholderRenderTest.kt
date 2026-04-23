package dev.mikepenz.a2cui.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test

/**
 * An unknown component type must render the muted [FallbackPlaceholder] (catalog is the trust
 * boundary) — agents cannot inject arbitrary widget types.
 */
@OptIn(ExperimentalTestApi::class)
class FallbackPlaceholderRenderTest {

    @Test fun unknown_component_renders_placeholder() = runComposeUiTest {
        val transport = FakeTransport()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        // Empty registry — every component type is "unknown".
        val controller = SurfaceController(ComponentRegistry(), transport, scope).also { it.start() }
        try {
            transport.tryEmit("""{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""")
            transport.tryEmit(
                """{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[
                   {"id":"root","component":"Hologram","label":"ghost"}]}}"""
            )

            setContent {
                A2cuiSurface(surfaceId = "s", controller = controller)
            }
            waitForIdle()

            // Placeholder copy is "⚠ unknown component: Hologram#root".
            onNode(hasText("Hologram", substring = true)).assertIsDisplayed()
            onNode(hasText("unknown component", substring = true)).assertIsDisplayed()
        } finally {
            controller.close()
        }
    }
}
