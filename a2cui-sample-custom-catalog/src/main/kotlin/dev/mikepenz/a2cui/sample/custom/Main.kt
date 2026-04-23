package dev.mikepenz.a2cui.sample.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
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
            "action": { "event": { "name": "rated", "context": { "value": 0 } } } },
          { "id": "hint",  "component": "Text", "text": "Tap a star — event fires with tapped index.", "variant": "caption" }
        ]
      } }
""".trimIndent()

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2CUI — Custom Catalog Sample",
        state = rememberWindowState(width = 640.dp, height = 640.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            val transport = remember { FakeTransport() }
            val controller = rememberSurfaceController(
                catalogs = listOf(Material3BasicCatalog, CustomDemoCatalog),
                transport = transport,
            )
            val outbound = remember { mutableStateListOf<String>() }

            LaunchedEffect(controller) {
                controller.events.collect { msg -> outbound.add(msg.toString()) }
            }
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
                    Card(Modifier.weight(1f).fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            A2cuiSurface(surfaceId = DEMO_SURFACE, controller = controller)
                        }
                    }
                    Card(Modifier.weight(1f).fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Outbound events", style = MaterialTheme.typography.labelLarge)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(outbound) { line ->
                                    Text(line, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
