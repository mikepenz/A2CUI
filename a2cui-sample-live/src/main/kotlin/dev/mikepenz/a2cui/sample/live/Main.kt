package dev.mikepenz.a2cui.sample.live

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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mikepenz.a2cui.agui.AguiEvent
import dev.mikepenz.a2cui.agui.AguiEventParser
import dev.mikepenz.a2cui.agui.AguiStreamEvent
import dev.mikepenz.a2cui.agui.mockserver.MockAguiServer
import dev.mikepenz.a2cui.compose.A2cuiSurface
import dev.mikepenz.a2cui.compose.catalog.Material3BasicCatalog
import dev.mikepenz.a2cui.compose.rememberSurfaceController
import dev.mikepenz.a2cui.transport.FakeTransport
import dev.mikepenz.a2cui.transport.SseTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2CUI Live Sample — SSE → AG-UI → A2UI",
        state = rememberWindowState(width = 900.dp, height = 800.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            A2cuiLiveApp()
        }
    }
}

/**
 * Live-agent wiring:
 *   MockAguiServer (in-process) → SseTransport → AguiEventParser → AguiA2cuiBridge
 *     → FakeTransport (as A2UI frame conduit) → SurfaceController → A2cuiSurface
 *
 * Two panes: rendered A2UI surface on top, raw AG-UI event log on the bottom.
 */
@Composable
private fun A2cuiLiveApp() {
    val a2uiConduit = remember { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = a2uiConduit,
    )
    val eventLog = remember { mutableStateListOf<String>() }
    val outbound = remember { mutableStateListOf<String>() }

    val server = remember { MockAguiServer(port = 0, surfaceId = "demo", frameDelayMillis = 300L) }
    val httpClient = remember { HttpClient(CIO) { install(SSE) } }

    DisposableEffect(server) {
        val port = server.start(wait = false)
        eventLog.add("[mock-server] listening on http://127.0.0.1:$port/events")
        onDispose {
            server.stop()
            httpClient.close()
        }
    }

    LaunchedEffect(controller) {
        controller.events.collect { msg -> outbound.add(msg.toString()) }
    }

    LaunchedEffect(server, httpClient) {
        val port = server.resolvedPort()
        val transport = SseTransport(
            httpClient = httpClient,
            receiveUrl = "http://127.0.0.1:$port/events",
        )
        val parser = AguiEventParser()

        transport.incoming().collect { raw ->
            eventLog.add("[agui-raw] $raw")
            when (val parsed = parser.parseOne(raw)) {
                is AguiStreamEvent.Event -> {
                    val event = parsed.event
                    if (event is AguiEvent.Custom && event.name == "a2ui") {
                        // Bridge A2UI payload into the SurfaceController conduit.
                        a2uiConduit.emit(event.value.toString())
                    }
                }
                is AguiStreamEvent.ParseError -> eventLog.add("[parse-error] ${parsed.message}")
            }
        }
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
                    Text("Rendered surface (from live AG-UI stream)", style = MaterialTheme.typography.labelLarge)
                    A2cuiSurface(surfaceId = "demo", controller = controller)
                }
            }
            Card(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("AG-UI event log", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(eventLog) { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall)
                        }
                        items(outbound) { line ->
                            Text("[outbound] $line", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
