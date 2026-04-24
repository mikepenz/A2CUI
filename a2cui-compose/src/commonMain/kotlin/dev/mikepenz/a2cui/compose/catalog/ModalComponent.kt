package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

/**
 * Agent-driven modal dialog.
 *
 * Visibility binding modes:
 *  - `open: {path: "/state/showModal"}` (recommended) — two-way bound. The agent can open or
 *    close the modal by writing to the data-model pointer; user dismissal writes `false` back.
 *  - `open: true|false` literal — initial visibility only; subsequent dismissal is local.
 *  - omitted — defaults to visible (so a just-received Modal frame appears immediately).
 *
 * `title` renders as the dialog heading; children render as the body content. `action` (if
 * present) fires when the user taps the confirm button before closing.
 */
internal val ModalFactory: ComponentFactory = @Composable { node, scope ->
    val title = scope.resolveString(node, "title")
    val dismissText = scope.resolveString(node, "dismissText", default = "Close")
    val openPath = remember(node.id) { pathOf(node.properties, "open") }

    val isOpen: Boolean
    val close: () -> Unit
    if (openPath != null) {
        val el by scope.dataModel.observe(openPath).collectAsState(initial = scope.dataModel.read(openPath))
        isOpen = (el as? JsonPrimitive)?.booleanOrNull() ?: true
        close = { scope.dataModel.write(openPath, JsonPrimitive(false)) }
    } else {
        val initialOpen = scope.resolveBool(node, "open", default = true)
        var localOpen by remember(node.id) { mutableStateOf(initialOpen) }
        isOpen = localOpen
        close = { localOpen = false }
    }

    if (isOpen) {
        AlertDialog(
            onDismissRequest = close,
            title = if (title.isNotEmpty()) { { Text(title) } } else null,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    node.children.forEach { scope.Child(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.emitAction(node)
                    close()
                }) { Text(dismissText) }
            },
        )
    }
}

private fun JsonPrimitive.booleanOrNull(): Boolean? =
    when (content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
