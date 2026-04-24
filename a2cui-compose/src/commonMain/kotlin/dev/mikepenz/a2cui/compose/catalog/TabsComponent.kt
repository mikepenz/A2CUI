package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
 * Tabs with optional two-way data-model binding for the active index.
 *
 * Props:
 *  - `titles` — list of tab labels (literal or resolvable).
 *  - `selectedIndex` — optional `value`-style binding: literal int, or `{path: "/state/tab"}`.
 *    When bound, server-driven writes to the data model swap tabs reactively, and tab clicks
 *    write back through the same pointer. When omitted, falls back to local-only state.
 */
internal val TabsFactory: ComponentFactory = @Composable { node, scope ->
    val titles = scope.resolveStringList(node, "titles")
    val path = remember(node.id) { pathOf(node.properties, "selectedIndex") }

    val bound: Int? = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content?.toIntOrNull()
    } else null
    val initialIndex = scope.resolveInt(node, "selectedIndex", default = 0)
    var local by remember { mutableStateOf(initialIndex) }
    val active = (bound ?: local).coerceIn(0, (node.children.size - 1).coerceAtLeast(0))

    Column {
        PrimaryTabRow(selectedTabIndex = active) {
            node.children.forEachIndexed { i, _ ->
                Tab(
                    selected = i == active,
                    onClick = {
                        if (path != null) scope.dataModel.write(path, JsonPrimitive(i)) else local = i
                    },
                    text = { Text(titles.getOrNull(i) ?: "Tab ${i + 1}") },
                )
            }
        }
        Box(Modifier.padding(top = 8.dp)) {
            node.children.getOrNull(active)?.let { scope.Child(it) }
        }
    }
}
