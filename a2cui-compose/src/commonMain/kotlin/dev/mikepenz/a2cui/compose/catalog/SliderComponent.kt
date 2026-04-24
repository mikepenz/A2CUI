package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

/**
 * Slider with optional two-way binding. Props: `min`, `max`, `step` (int; 0 = continuous),
 * `value` (literal or {"path":"/ptr"}), `label`, `showValue` (bool, default true — renders the
 * current value next to the label).
 */
internal val SliderFactory: ComponentFactory = @Composable { node, scope ->
    val min = scope.resolveDouble(node, "min", default = 0.0).toFloat()
    val max = scope.resolveDouble(node, "max", default = 1.0).toFloat()
    val step = scope.resolveInt(node, "step", default = 0)
    val label = scope.resolveString(node, "label")
    val showValue = scope.resolveBool(node, "showValue", default = true)
    val path = remember(node.id) { pathOf(node.properties, "value") }

    val current: Float = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content?.toFloatOrNull() ?: min
    } else {
        scope.resolveDouble(node, "value", default = min.toDouble()).toFloat()
    }

    val clamped = current.coerceIn(min, max)
    // `steps` in Material3's Slider is the count BETWEEN endpoints, not the number of stops.
    // For `step=1` across min..max we need (max-min-1) steps; for `step=0` we pass 0 (continuous).
    val sliderSteps = if (step > 0) ((max - min) / step - 1).toInt().coerceAtLeast(0) else 0
    val display = if (step > 0 || clamped == clamped.toInt().toFloat()) {
        clamped.roundToInt().toString()
    } else {
        ((clamped * 100).roundToInt() / 100.0).toString()
    }

    Column {
        if (label.isNotEmpty() || showValue) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.bodyMedium)
                if (showValue) Text(display, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Slider(
            value = clamped,
            onValueChange = { next ->
                if (path != null) {
                    // When `step` is a positive integer the slider is discrete: write integer
                    // primitives so paths bound to int-typed fields don't silently get `3.0`.
                    // For step == 0 (continuous) keep float precision via Double.
                    val v: JsonPrimitive = if (step > 0) {
                        JsonPrimitive(next.roundToInt())
                    } else {
                        JsonPrimitive(next.toDouble())
                    }
                    scope.dataModel.write(path, v)
                }
            },
            valueRange = min..max,
            steps = sliderSteps,
        )
    }
}
