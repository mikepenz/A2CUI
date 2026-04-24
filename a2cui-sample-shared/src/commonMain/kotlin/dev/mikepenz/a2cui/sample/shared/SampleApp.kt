package dev.mikepenz.a2cui.sample.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.A2cuiSurface
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.compose.rememberSurfaceController
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.delay

/**
 * Shared Compose composable used by every sample host (desktop, android, ios). The composable
 * drives a `FakeTransport` with a scripted replay so the demo is deterministic and runs without
 * a live agent. Host modules wrap this in their platform-appropriate entry point
 * (Window / Activity / ComposeUIViewController). A toolbar selector lets the user flip between
 * scripted scenarios at runtime; outbound events live in a bottom sheet to keep the surface
 * vertical space uncluttered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun A2cuiSampleApp() {
    var scenario by remember { mutableStateOf(SampleScenario.Booking) }
    var logsOpen by remember { mutableStateOf(false) }
    val transport = remember(scenario) { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = transport,
    )
    val outbound = remember(scenario) { mutableStateListOf<String>() }

    LaunchedEffect(controller) {
        controller.events.collect { msg -> outbound.add(msg.toString()) }
    }

    LaunchedEffect(transport) {
        when (scenario) {
            SampleScenario.Booking -> {
                transport.emit(SampleFrames.createSurface("demo"))
                delay(250)
                transport.emit(SampleFrames.bookingComponents("demo"))
                delay(500)
                transport.emit(SampleFrames.seedEmail("demo"))
            }
            SampleScenario.Theater -> {
                transport.emit(TheaterFrames.createSurface("demo"))
                delay(200)
                transport.emit(TheaterFrames.seedCatalog("demo"))
                delay(300)
                transport.emit(TheaterFrames.theaterComponents("demo"))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A2CUI Sample") },
                actions = {
                    TextButton(onClick = { logsOpen = true }) {
                        Text("Events (${outbound.size})")
                    }
                    ScenarioSelector(
                        current = scenario,
                        onSelect = { scenario = it },
                    )
                },
            )
        },
    ) { padding ->
        // `key(scenario)` forces the surface card to remount on scenario change, so the prior
        // tree (which may contain a `List` → LazyColumn) is torn down before the new
        // verticalScroll wrapper measures it — otherwise switching Theater → Booking briefly
        // wraps the still-mounted LazyColumn in a vertically scrollable parent and crashes.
        key(scenario) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
            ) {
                Card(Modifier.fillMaxSize().fillMaxWidth()) {
                    val surfaceModifier = if (scenario.surfaceScrolls) {
                        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxSize().padding(16.dp)
                    }
                    Column(
                        surfaceModifier,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Rendered surface", style = MaterialTheme.typography.labelLarge)
                        A2cuiSurface(surfaceId = "demo", controller = controller)
                    }
                }
            }
        }
    }

    if (logsOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { logsOpen = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Outbound events", style = MaterialTheme.typography.titleMedium)
                if (outbound.isEmpty()) {
                    Text(
                        "No events yet — interact with the surface to emit one.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
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

@Composable
private fun ScenarioSelector(
    current: SampleScenario,
    onSelect: (SampleScenario) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(end = 8.dp)) {
        Row(
            Modifier
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Scenario:", style = MaterialTheme.typography.labelMedium)
            Text(current.label, style = MaterialTheme.typography.labelLarge)
            Text("▾", style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SampleScenario.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    onClick = {
                        onSelect(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}

internal enum class SampleScenario(val label: String, val surfaceScrolls: Boolean) {
    Booking("Booking", surfaceScrolls = true),
    // Theater's only `List` is `lazy: false` (renders as Column), so the whole surface can
    // safely live inside a `verticalScroll`.
    Theater("Theater", surfaceScrolls = true),
}
