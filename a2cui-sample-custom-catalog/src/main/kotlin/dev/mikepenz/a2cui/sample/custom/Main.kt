package dev.mikepenz.a2cui.sample.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mikepenz.a2cui.compose.A2cuiSurface
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.compose.rememberSurfaceController
import dev.mikepenz.a2cui.transport.FakeTransport

private const val DEMO_SURFACE = "demo"

private val CREATE_SURFACE = """
    { "version": "v0.9",
      "createSurface": { "surfaceId": "$DEMO_SURFACE", "catalogId": "custom-demo" } }
""".trimIndent()

private val CUSTOM_COMPONENTS = """
    { "version": "v0.9",
      "updateComponents": {
        "surfaceId": "$DEMO_SURFACE",
        "components": [
          { "id": "root",  "component": "Column", "spacing": 12, "children": ["title","badge","rating","hint"] },
          { "id": "title", "component": "Text", "text": "Custom catalog demo", "variant": "h2" },
          { "id": "badge", "component": "Badge", "text": "Beta", "tone": "info", "emphasised": true },
          { "id": "rating","component": "Rating", "value": 4, "max": 5, "size": "medium",
            "action": { "event": { "name": "rated", "context": { "value": 4 } } } },
          { "id": "hint",  "component": "Text", "text": "Rating + Badge emitted by :a2cui-codegen.", "variant": "caption" }
        ]
      } }
""".trimIndent()

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2CUI — Custom Catalog Sample",
        state = rememberWindowState(width = 640.dp, height = 520.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            val transport = remember { FakeTransport() }
            // Install both the stock Material3 catalog (for Column / Text) and the generated
            // CustomDemoCatalog (Rating, Badge). The generated object is produced by KSP under
            // build/generated/ksp/main/kotlin/dev/mikepenz/a2cui/sample/custom/CustomDemoCatalog.kt.
            val controller = rememberSurfaceController(
                catalogs = listOf(Material3BasicCatalog, CustomDemoCatalog),
                transport = transport,
            )
            LaunchedEffect(transport) {
                transport.emit(CREATE_SURFACE)
                transport.emit(CUSTOM_COMPONENTS)
            }
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Generated catalog id: ${CustomDemoCatalog.id}",
                        style = MaterialTheme.typography.labelLarge)
                    Card(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp)) {
                            A2cuiSurface(surfaceId = DEMO_SURFACE, controller = controller)
                        }
                    }
                }
            }
        }
    }
}
