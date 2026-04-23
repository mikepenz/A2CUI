package dev.mikepenz.a2cui.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.A2cuiSurface
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.compose.rememberSurfaceController
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.delay

/**
 * Shared Compose composable used by every sample host (desktop, android, ios). The composable
 * drives a `FakeTransport` with a scripted booking-form replay so the demo is deterministic and
 * runs without a live agent. Host modules wrap this in their platform-appropriate entry point
 * (Window / Activity / ComposeUIViewController).
 */
@Composable
public fun A2cuiSampleApp() {
    val transport = remember { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = transport,
    )
    val outbound = remember { mutableStateListOf<String>() }

    LaunchedEffect(controller) {
        controller.events.collect { msg -> outbound.add(msg.toString()) }
    }

    LaunchedEffect(transport) {
        transport.emit(SampleFrames.createSurface("demo"))
        delay(250)
        transport.emit(SampleFrames.bookingComponents("demo"))
        delay(500)
        transport.emit(SampleFrames.seedEmail("demo"))
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Rendered surface", style = MaterialTheme.typography.labelLarge)
                    A2cuiSurface(surfaceId = "demo", controller = controller)
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
