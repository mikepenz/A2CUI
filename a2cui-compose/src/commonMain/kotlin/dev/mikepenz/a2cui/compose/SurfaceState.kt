package dev.mikepenz.a2cui.compose

import dev.mikepenz.a2cui.core.ComponentNode
import dev.mikepenz.a2cui.core.DataModel
import kotlinx.serialization.json.JsonObject

/**
 * Current structural snapshot of one A2UI surface: which nodes exist and their layout tree.
 *
 * The [dataModel] is held by reference — it is independently mutable, with its own
 * change-observation surface via [DataModel.observe]. Structural changes (nodes added,
 * removed, or reparented) flow through new [SurfaceState] instances on the controller's
 * `StateFlow`; value changes flow through the data model.
 */
public class SurfaceState internal constructor(
    public val surfaceId: String,
    public val catalogId: String,
    public val theme: JsonObject?,
    public val rootId: String,
    public val nodesById: Map<String, ComponentNode>,
    public val dataModel: DataModel,
) {
    internal fun withNodes(nodes: Map<String, ComponentNode>): SurfaceState =
        SurfaceState(surfaceId, catalogId, theme, rootId, nodes, dataModel)

    public companion object {
        public const val DEFAULT_ROOT_ID: String = "root"
    }
}
