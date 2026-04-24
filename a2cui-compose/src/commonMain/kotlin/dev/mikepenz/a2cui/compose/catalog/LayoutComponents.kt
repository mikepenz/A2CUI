package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory

internal val ColumnFactory: ComponentFactory = @Composable { node, scope ->
    val spacing = scope.resolveInt(node, "spacing", default = 8)
    val align = when (scope.resolveString(node, "alignment")) {
        "center" -> Alignment.CenterHorizontally
        "end" -> Alignment.End
        else -> Alignment.Start
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        horizontalAlignment = align,
    ) {
        node.children.forEach { scope.Child(it) }
    }
}

internal val RowFactory: ComponentFactory = @Composable { node, scope ->
    val spacing = scope.resolveInt(node, "spacing", default = 8)
    val align = when (scope.resolveString(node, "alignment")) {
        "center" -> Alignment.CenterVertically
        "bottom" -> Alignment.Bottom
        else -> Alignment.Top
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = align,
    ) {
        node.children.forEach { scope.Child(it) }
    }
}

/**
 * Material 3 Card.
 *
 * Props:
 *  - `elevation` (int dp, default 1) — sets the resting tonal/shadow elevation.
 *  - `padding` (int dp, default 16) — inner content padding.
 *  - `spacing` (int dp, default 8) — vertical spacing between children.
 *  - `action` — when present, renders the clickable Card overload and emits the action's
 *    event on tap. Without `action`, the card is non-interactive.
 */
internal val CardFactory: ComponentFactory = @Composable { node, scope ->
    val elevation = scope.resolveInt(node, "elevation", default = 1)
    val padding = scope.resolveInt(node, "padding", default = 16)
    val spacing = scope.resolveInt(node, "spacing", default = 8)
    val hasAction = node.properties["action"] != null
    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)

    val content: @Composable () -> Unit = {
        Column(
            Modifier.padding(padding.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
        ) {
            node.children.forEach { scope.Child(it) }
        }
    }

    if (hasAction) {
        Card(onClick = { scope.emitAction(node) }, elevation = cardElevation) { content() }
    } else {
        Card(elevation = cardElevation) { content() }
    }
}
