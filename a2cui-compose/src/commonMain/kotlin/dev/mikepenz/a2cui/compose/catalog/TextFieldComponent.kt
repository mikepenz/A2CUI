package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

internal val TextFieldFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val placeholder = scope.resolveString(node, "placeholder")
    val bindingPath = remember(node.id) { pathOf(node.properties, "value") }

    if (bindingPath != null) {
        // Local-first two-way binding: writes update the data model synchronously; outbound
        // messages to the agent fire only on explicit `event` actions on other components.
        val valueElement by scope.dataModel
            .observe(bindingPath)
            .collectAsState(initial = scope.dataModel.read(bindingPath))
        val current = valueElement.displayString()
        OutlinedTextField(
            value = current,
            onValueChange = { scope.dataModel.write(bindingPath, JsonPrimitive(it)) },
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        )
    } else {
        // No binding — read-only literal.
        val literal = scope.resolveString(node, "value")
        OutlinedTextField(
            value = literal,
            onValueChange = {},
            readOnly = true,
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
        )
    }
}
