package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory

/**
 * Scrollable list. For v0.9 we render the declared children as lazy items. Item-scope path
 * resolution (`{"path": "0/name"}` relative to a data-model array) is not yet supported and
 * is tracked as a v1.0 enhancement — agents currently ship a flat child list.
 */
internal val ListFactory: ComponentFactory = @Composable { node, scope ->
    val spacing = scope.resolveInt(node, "spacing", default = 4)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        items(node.children) { childId -> scope.Child(childId) }
    }
}
