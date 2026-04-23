package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.mikepenz.a2cui.compose.ComponentFactory

internal val ButtonFactory: ComponentFactory = @Composable { node, scope ->
    val text = scope.resolveString(node, "text")
    val enabled = scope.resolveBool(node, "enabled", default = true)
    val onClick = { scope.emitAction(node) }
    when (scope.resolveString(node, "variant")) {
        "outlined" -> OutlinedButton(onClick = onClick, enabled = enabled) { Text(text) }
        "text" -> TextButton(onClick = onClick, enabled = enabled) { Text(text) }
        else -> Button(onClick = onClick, enabled = enabled) { Text(text) }
    }
}
