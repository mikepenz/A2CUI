package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory

internal val TabsFactory: ComponentFactory = @Composable { node, scope ->
    val titles = scope.resolveStringList(node, "titles")
    var selected by remember { mutableStateOf(0) }
    val selectedIndex = selected.coerceIn(0, (node.children.size - 1).coerceAtLeast(0))
    Column {
        PrimaryTabRow(selectedTabIndex = selectedIndex) {
            node.children.forEachIndexed { i, _ ->
                Tab(
                    selected = i == selectedIndex,
                    onClick = { selected = i },
                    text = { Text(titles.getOrNull(i) ?: "Tab ${i + 1}") },
                )
            }
        }
        Box(Modifier.padding(top = 8.dp)) {
            node.children.getOrNull(selectedIndex)?.let { scope.Child(it) }
        }
    }
}
