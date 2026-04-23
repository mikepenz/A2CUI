package dev.mikepenz.a2cui.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mikepenz.a2cui.sample.shared.A2cuiSampleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "A2CUI Sample — Agent-driven Compose UI",
        state = rememberWindowState(width = 720.dp, height = 720.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            A2cuiSampleApp()
        }
    }
}
