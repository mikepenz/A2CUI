package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

/**
 * Read-only dropdown picker built on M3's [ExposedDropdownMenuBox]. We deliberately use that
 * helper rather than a hand-rolled `Box(Modifier.clickable) { OutlinedTextField(readOnly=true) }`
 * because a read-only `OutlinedTextField` still consumes pointer events for focus/cursor
 * handling — so a `clickable` attached to the same node (or a sibling) never fires and the
 * menu cannot open. `ExposedDropdownMenuBox` wires the anchor through `menuAnchor()` and
 * routes the tap through M3's own focus pipeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = current,
                onValueChange = {},
                readOnly = true,
                label = if (label.isNotEmpty()) { { Text(label) } } else null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
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
