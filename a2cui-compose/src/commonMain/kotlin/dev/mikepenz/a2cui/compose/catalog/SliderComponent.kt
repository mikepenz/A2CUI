package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

internal val SliderFactory: ComponentFactory = @Composable { node, scope ->
    val min = scope.resolveDouble(node, "min", default = 0.0).toFloat()
    val max = scope.resolveDouble(node, "max", default = 1.0).toFloat()
    val label = scope.resolveString(node, "label")
    val path = remember(node.id) { pathOf(node.properties, "value") }

    val current: Float = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content?.toFloatOrNull() ?: min
    } else {
        scope.resolveDouble(node, "value", default = min.toDouble()).toFloat()
    }

    Column {
        if (label.isNotEmpty()) Text(label)
        Slider(
            value = current.coerceIn(min, max),
            onValueChange = { next ->
                if (path != null) scope.dataModel.write(path, JsonPrimitive(next))
            },
            valueRange = min..max,
        )
    }
}
