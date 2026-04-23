package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory
import dev.mikepenz.a2cui.core.JsonPointer
import kotlinx.serialization.json.JsonArray

/**
 * Scrollable list with two rendering modes:
 *
 *  1. **Flat children** (pre-v1 default): when no `items` binding is declared, children are
 *     rendered as-is — one lazy item per child id. Matches the v0.9-era catalog behaviour.
 *
 *  2. **Iteration-scope binding**: when the node declares `items: {"path": "/users"}`
 *     pointing to a data-model array, the **first child** is treated as the item template
 *     (one subtree per array element). Subsequent children in `children` are ignored. Inside
 *     each iteration, the [RenderScope]'s resolver is scoped to `/users/<index>`, so
 *     `{path:"/name"}` inside the template resolves against the current item while
 *     `{path:"../..."}` walks up toward the surface root.
 *
 *  Design trade-off — *first-child template* vs explicit `template: "<node-id>"` property:
 *  we picked first-child because (a) it keeps the v0.9 wire format verbatim — no new
 *  top-level property — and (b) agent authors already express "one subtree per item" by
 *  shipping exactly one child, so the implicit convention matches the natural mental model.
 *  If the v0.10 draft later standardises `template`, we can honour it additively without
 *  breaking existing surfaces.
 */
internal val ListFactory: ComponentFactory = @Composable { node, scope ->
    val spacing = scope.resolveInt(node, "spacing", default = 4)
    val itemsPath = pathOf(node.properties, "items")

    val root by scope.dataModel.root.collectAsState()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        if (itemsPath == null || node.children.isEmpty()) {
            items(node.children) { childId -> scope.Child(childId) }
        } else {
            val templateId = node.children.first()
            val resolvedItemsPath = scope.resolver.scopedPointer(itemsPath)
            val arr = JsonPointer.read(root, resolvedItemsPath) as? JsonArray
            val size = arr?.size ?: 0
            itemsIndexed(List(size) { it }) { index, _ ->
                val itemRoot = "$resolvedItemsPath/$index"
                scope.withScope(itemRoot).Child(templateId)
            }
        }
    }
}
