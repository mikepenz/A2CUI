package dev.mikepenz.a2cui.sample.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.ktor.http.encodeURLQueryComponent

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2CUI Live Sample — prompt-driven agent UI",
        state = rememberWindowState(width = 960.dp, height = 880.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            A2cuiLiveApp()
        }
    }
}

/**
 * Resolve the AG-UI server URL from (in order):
 *   1. `AGUI_URL` environment variable
 *   2. `-Dagui.url=...` JVM system property
 *   3. `null` — caller spins up an in-process [MockAguiServer]
 */
private fun resolveAguiUrl(): String? =
    System.getenv("AGUI_URL") ?: System.getProperty("agui.url")

/**
 * Append or replace the `prompt=` query parameter on [baseUrl]. Handles both
 * "http://host/events" and "http://host/events?existing=1" cleanly.
 */
private fun withPrompt(baseUrl: String, prompt: String?): String {
    if (prompt.isNullOrBlank()) return baseUrl
    val encoded = prompt.encodeURLQueryComponent(spaceToPlus = true)
    val separator = if ('?' in baseUrl) '&' else '?'
    return "$baseUrl${separator}prompt=$encoded"
}

@Composable
private fun A2cuiLiveApp() {
    val a2uiConduit = remember { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = a2uiConduit,
    )
    val eventLog = remember { mutableStateListOf<String>() }
    val outbound = remember { mutableStateListOf<String>() }

    val externalUrl = remember { resolveAguiUrl() }
    val server = remember {
        if (externalUrl == null) MockAguiServer(port = 0, surfaceId = "demo", frameDelayMillis = 200L) else null
    }
    val httpClient = remember { HttpClient(CIO) { install(SSE) } }

    // Prompt state + a monotonic generation counter. Incrementing the counter
    // is the signal to (re-)subscribe to /events with the current prompt.
    var prompt by remember { mutableStateOf("") }
    var generation by remember { mutableStateOf(0) }

    DisposableEffect(server, externalUrl) {
        if (server != null) {
            val port = server.start(wait = false)
            eventLog.add("[mock-server] listening on http://127.0.0.1:$port/events")
        } else {
            eventLog.add("[external] AGUI_URL=$externalUrl")
        }
        onDispose {
            server?.stop()
            httpClient.close()
        }
    }

    LaunchedEffect(controller) {
        controller.events.collect { msg -> outbound.add(msg.toString()) }
    }

    // Re-subscribe to SSE every time `generation` increments. Each new subscription
    // uses the current prompt string in the query parameter so the agent regenerates
    // the UI from scratch. `generation == 0` kicks off the initial default run.
    LaunchedEffect(server, httpClient, externalUrl, generation) {
        val baseUrl = externalUrl ?: run {
            val port = server!!.resolvedPort()
            "http://127.0.0.1:$port/events"
        }
        val url = withPrompt(baseUrl, prompt.takeIf { generation > 0 })
        eventLog.add("[request] $url")

        val transport = SseTransport(httpClient = httpClient, receiveUrl = url)
        val parser = AguiEventParser()

        transport.incoming().collect { raw ->
            eventLog.add("[agui-raw] $raw")
            when (val parsed = parser.parseOne(raw)) {
                is AguiStreamEvent.Event -> {
                    val event = parsed.event
                    if (event is AguiEvent.Custom && event.name == "a2ui") {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Prompt bar ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Describe the UI you want the agent to generate",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g. login form with email, password, sign-in button") },
                            singleLine = true,
                            keyboardActions = KeyboardActions(onDone = { generation += 1 }),
                        )
                        Button(
                            onClick = { generation += 1 },
                            enabled = prompt.isNotBlank(),
                        ) { Text("Generate") }
                    }
                    Text(
                        "Tip: try \"vertical stack with a title, a slider from 0-100, and a submit button\".",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // --- Rendered surface ---
            Card(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Rendered surface (from live AG-UI stream)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    A2cuiSurface(surfaceId = "demo", controller = controller)
                }
            }

            // --- Event log ---
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
