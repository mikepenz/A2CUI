package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * List with two rendering modes:
 *
 *  1. **Flat children** (pre-v1 default): when no `items` binding is declared, children are
 *     rendered as-is — one item per child id. Matches the v0.9-era catalog behaviour.
 *
 *  2. **Iteration-scope binding**: when the node declares `items: {"path": "/users"}`
 *     pointing to a data-model array, the **first child** is treated as the item template
 *     (one subtree per array element). Subsequent children in `children` are ignored. Inside
 *     each iteration, the [RenderScope]'s resolver is scoped to `/users/<index>`, so
 *     `{path:"/name"}` inside the template resolves against the current item while
 *     `{path:"../..."}` walks up toward the surface root.
 *
 *  ### `lazy` property
 *
 *  Defaults to `true` → renders as a `LazyColumn` (virtualised, supports very long lists, but
 *  requires a bounded-height parent — a `LazyColumn` cannot live inside a `Column` with
 *  `verticalScroll`).
 *
 *  Set `lazy: false` to render as a regular `Column` (every item materialised eagerly). Use
 *  this when the list is one section among others on a scrollable surface — e.g. a "Now playing"
 *  block above a booking form. The whole surface can then be wrapped in a single
 *  `verticalScroll` without nested-scroll-container conflicts. The trade-off is no
 *  virtualisation, so prefer `lazy: false` only when item counts are bounded (typically <50).
 *
 *  Design trade-off — *first-child template* vs explicit `template: "<node-id>"` property:
 *  we picked first-child because (a) it keeps the v0.9 wire format verbatim — no new
 *  top-level property — and (b) agent authors already express "one subtree per item" by
 *  shipping exactly one child, so the implicit convention matches the natural mental model.
 *  If a future A2UI spec standardises an explicit `template` property, we can honour it
 *  additively without breaking existing surfaces.
 */
internal val ListFactory: ComponentFactory = @Composable { node, scope ->
    val spacing = scope.resolveInt(node, "spacing", default = 4)
    val lazy = scope.resolveBool(node, "lazy", default = true)
    val itemsPath = pathOf(node.properties, "items")

    val root by scope.dataModel.root.collectAsState()
    val templateId = node.children.firstOrNull()
    val resolvedItemsPath = itemsPath?.let { scope.resolver.scopedPointer(it) }
    val iterationSize: Int = if (resolvedItemsPath != null && templateId != null) {
        (JsonPointer.read(root, resolvedItemsPath) as? JsonArray)?.size ?: 0
    } else 0
    val useIteration = resolvedItemsPath != null && templateId != null

    if (lazy) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
            if (!useIteration) {
                items(node.children) { childId -> scope.Child(childId) }
            } else {
                itemsIndexed(List(iterationSize) { it }) { index, _ ->
                    val itemRoot = "$resolvedItemsPath/$index"
                    scope.withScope(itemRoot).Child(templateId)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
            if (!useIteration) {
                node.children.forEach { childId -> scope.Child(childId) }
            } else {
                for (index in 0 until iterationSize) {
                    val itemRoot = "$resolvedItemsPath/$index"
                    scope.withScope(itemRoot).Child(templateId)
                }
            }
        }
    }
}
