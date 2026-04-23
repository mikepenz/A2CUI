package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

internal val ChoicePickerFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val choices = scope.resolveStringList(node, "choices")
    val path = remember(node.id) { pathOf(node.properties, "value") }

    val current: String = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content ?: ""
    } else {
        scope.resolveString(node, "value")
    }

    var expanded by remember { mutableStateOf(false) }
    Column {
        Box {
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                label = if (label.isNotEmpty()) { { Text(label) } } else null,
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = {
                            if (path != null) scope.dataModel.write(path, JsonPrimitive(choice))
                            expanded = false
                        },
                    )
                }
            }
        }
        if (choices.isEmpty()) Text(
            "No choices",
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
