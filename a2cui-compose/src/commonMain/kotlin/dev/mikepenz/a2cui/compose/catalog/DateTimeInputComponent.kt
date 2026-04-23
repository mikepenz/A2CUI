package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

/**
 * Minimal v0.9 DateTimeInput: renders as a text field that accepts an ISO-8601 string. A
 * platform-native calendar/time picker is deferred — Compose Multiplatform's DatePicker /
 * TimePicker can be layered on top in a later pass without changing the wire contract.
 */
internal val DateTimeInputFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val path = remember(node.id) { pathOf(node.properties, "value") }
    val current: String = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content ?: ""
    } else {
        scope.resolveString(node, "value")
    }
    OutlinedTextField(
        value = current,
        onValueChange = { next ->
            if (path != null) scope.dataModel.write(path, JsonPrimitive(next))
        },
        label = if (label.isNotEmpty()) { { Text(label) } } else null,
        placeholder = { Text("YYYY-MM-DD[THH:MM]") },
        readOnly = path == null,
    )
}
