package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory

/**
 * Agent-driven modal dialog. `open` controls visibility (defaults to `true` so a
 * just-received Modal frame is visible). `title` renders as the dialog heading; children
 * render as the body content.
 */
internal val ModalFactory: ComponentFactory = @Composable { node, scope ->
    val title = scope.resolveString(node, "title")
    val dismissText = scope.resolveString(node, "dismissText", default = "Close")
    var open by remember(node.id) { mutableStateOf(scope.resolveBool(node, "open", default = true)) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = if (title.isNotEmpty()) { { Text(title) } } else null,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    node.children.forEach { scope.Child(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.emitAction(node)
                    open = false
                }) { Text(dismissText) }
            },
        )
    }
}
