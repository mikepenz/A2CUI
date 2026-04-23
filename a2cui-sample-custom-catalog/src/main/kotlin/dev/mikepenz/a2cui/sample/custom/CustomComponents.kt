package dev.mikepenz.a2cui.sample.custom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.codegen.annotations.A2uiComponent
import dev.mikepenz.a2cui.codegen.annotations.A2uiEvent
import dev.mikepenz.a2cui.codegen.annotations.A2uiProp
import dev.mikepenz.a2cui.codegen.annotations.A2uiPropType
import dev.mikepenz.a2cui.compose.ComponentFactory
import dev.mikepenz.a2cui.compose.catalog.emitAction
import dev.mikepenz.a2cui.compose.catalog.resolveBool
import dev.mikepenz.a2cui.compose.catalog.resolveInt
import dev.mikepenz.a2cui.compose.catalog.resolveString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 5-star rating component. Tapping a star fires the authored `rated` event with the tapped
 * index overriding the authored `value` context field so the event payload reflects the user's
 * choice rather than the initial rendered value.
 */
@A2uiComponent(
    name = "Rating",
    description = "A 0..max star rating display with a tap event.",
    props = [
        A2uiProp(name = "value", type = A2uiPropType.INTEGER, description = "Current rating", required = true, defaultValue = "0"),
        A2uiProp(name = "max", type = A2uiPropType.INTEGER, description = "Max rating", defaultValue = "5"),
        A2uiProp(name = "size", type = A2uiPropType.STRING, description = "Size variant", enumValues = ["small", "medium", "large"], defaultValue = "medium"),
    ],
    events = [
        A2uiEvent(name = "rated", description = "User tapped a star", context = [
            A2uiProp(name = "value", type = A2uiPropType.INTEGER, description = "New rating value"),
        ]),
    ],
)
public val RatingFactory: ComponentFactory = @Composable { node, scope ->
    val value = scope.resolveInt(node, "value", default = 0)
    val max = scope.resolveInt(node, "max", default = 5)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i ->
            val tapped = i + 1
            val filled = i < value
            TextButton(onClick = {
                scope.emitAction(node, overrides = JsonObject(mapOf("value" to JsonPrimitive(tapped))))
            }) {
                Text(if (filled) "*" else "-", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/**
 * Badge component — a small pill-shaped label with semantic tone. No events; no children.
 */
@A2uiComponent(
    name = "Badge",
    description = "Small pill-shaped label with semantic tone.",
    props = [
        A2uiProp(name = "text", type = A2uiPropType.STRING, description = "Badge label", required = true),
        A2uiProp(name = "tone", type = A2uiPropType.STRING, description = "Visual tone", enumValues = ["neutral", "info", "success", "warning", "error"], defaultValue = "neutral"),
        A2uiProp(name = "emphasised", type = A2uiPropType.BOOLEAN, description = "Render filled vs outlined", defaultValue = "false"),
    ],
)
public val BadgeFactory: ComponentFactory = @Composable { node, scope ->
    val text = scope.resolveString(node, "text")
    val tone = scope.resolveString(node, "tone", default = "neutral")
    val emphasised = scope.resolveBool(node, "emphasised", default = false)
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        "info" -> colors.primaryContainer to colors.onPrimaryContainer
        "success" -> colors.tertiaryContainer to colors.onTertiaryContainer
        "warning" -> colors.secondaryContainer to colors.onSecondaryContainer
        "error" -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(
        color = if (emphasised) bg else colors.surface,
        contentColor = fg,
        shape = RoundedCornerShape(50),
        border = if (emphasised) null else BorderStroke(1.dp, bg),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
