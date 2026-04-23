package dev.mikepenz.a2cui.sample.custom

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
import dev.mikepenz.a2cui.compose.RenderScope
import dev.mikepenz.a2cui.core.ComponentNode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

// Local copies of the `:a2cui-compose` internal helpers — the sample module cannot reach them
// directly. Kept minimal: read a property, treat it as a primitive, fall back to a default.
private fun RenderScope.prop(node: ComponentNode, key: String): JsonElement =
    resolver.resolveProperty(node.properties, key)

private fun RenderScope.propString(node: ComponentNode, key: String, default: String = ""): String =
    (prop(node, key) as? JsonPrimitive)?.contentOrNull ?: default

private fun RenderScope.propInt(node: ComponentNode, key: String, default: Int = 0): Int =
    (prop(node, key) as? JsonPrimitive)?.intOrNull ?: default

private fun RenderScope.propBool(node: ComponentNode, key: String, default: Boolean = false): Boolean =
    (prop(node, key) as? JsonPrimitive)?.booleanOrNull ?: default

private fun RenderScope.emitActionLike(node: ComponentNode, eventName: String) {
    val action = node.properties["action"] as? JsonObject ?: return
    val ev = action["event"] as? JsonObject ?: return
    val name = (ev["name"] as? JsonPrimitive)?.contentOrNull ?: eventName
    val ctx = ev["context"] as? JsonObject ?: JsonObject(emptyMap())
    val resolved = JsonObject(ctx.mapValues { resolver.resolve(it.value) })
    emit(dev.mikepenz.a2cui.compose.EventSpec(name = name, sourceComponentId = node.id, context = resolved))
}

/**
 * 5-star rating component. Values are clamped to 0..max; a bare row of `*`/`-` glyphs keeps this
 * demo portable without relying on icon assets. A `tap` event is emitted whenever the user
 * clicks on a star.
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
    val value = scope.propInt(node, "value", default = 0)
    val max = scope.propInt(node, "max", default = 5)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i ->
            val filled = i < value
            TextButton(onClick = { scope.emitActionLike(node, "rated") }) {
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
    val text = scope.propString(node, "text")
    val tone = scope.propString(node, "tone", default = "neutral")
    val emphasised = scope.propBool(node, "emphasised", default = false)
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
        border = if (emphasised) null else androidx.compose.foundation.BorderStroke(1.dp, bg),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
