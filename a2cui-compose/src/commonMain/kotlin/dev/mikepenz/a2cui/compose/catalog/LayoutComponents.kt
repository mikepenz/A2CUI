package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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

internal val CardFactory: ComponentFactory = @Composable { node, scope ->
    Card(modifier = Modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.children.forEach { scope.Child(it) }
        }
    }
}
