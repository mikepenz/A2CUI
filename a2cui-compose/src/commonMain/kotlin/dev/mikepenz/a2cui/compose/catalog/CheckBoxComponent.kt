package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

internal val CheckBoxFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val path = remember(node.id) { pathOf(node.properties, "value") }
    val checked: Boolean = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
    } else {
        scope.resolveBool(node, "value")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = { next ->
                if (path != null) scope.dataModel.write(path, JsonPrimitive(next))
            },
        )
        if (label.isNotEmpty()) Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}
