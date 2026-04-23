package dev.mikepenz.a2cui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.mikepenz.a2cui.core.BindingResolver
import dev.mikepenz.a2cui.core.ComponentNode
import dev.mikepenz.a2cui.core.DataModel

/**
 * Context handed to every [ComponentFactory]. Provides data-model access, binding resolution,
 * child recursion, event emission, and the current theme hints.
 */
@Immutable
public class RenderScope internal constructor(
    public val surfaceId: String,
    public val dataModel: DataModel,
    public val resolver: BindingResolver,
    public val registry: ComponentRegistry,
    public val nodesById: Map<String, ComponentNode>,
    public val theme: A2cuiTheme,
    private val eventEmitter: (EventSpec) -> Unit,
) {
    /** Recurse into a child node by id. Missing nodes / unregistered components render a placeholder. */
    @Composable
    public fun Child(nodeId: String) {
        val node = nodesById[nodeId]
        if (node == null) {
            FallbackPlaceholder(reason = "missing node", identifier = nodeId)
            return
        }
        val factory = registry[node.component]
        if (factory == null) {
            FallbackPlaceholder(reason = "unknown component", identifier = "${node.component}#${node.id}")
            return
        }
        factory(node, this)
    }

    /** Fire an outbound action event. */
    public fun emit(event: EventSpec) { eventEmitter(event) }

    /**
     * Return a child [RenderScope] whose [resolver] is scoped under [rootPointer]. Used by
     * iterating components (e.g. `List`) to bind a subtree template against a single array
     * element: inside the scope, `{path:"/name"}` resolves to `rootPointer + "/name"`, and
     * `{path:"../..."}` walks up toward the surface root.
     *
     * All other scope fields (surfaceId, dataModel, registry, nodesById, theme, emitter) are
     * inherited unchanged — structural navigation still uses the full node map.
     */
    public fun withScope(rootPointer: String): RenderScope = RenderScope(
        surfaceId = surfaceId,
        dataModel = dataModel,
        resolver = resolver.withScope(rootPointer),
        registry = registry,
        nodesById = nodesById,
        theme = theme,
        eventEmitter = eventEmitter,
    )
}

/** An outbound user-triggered action derived from a component interaction. */
@Immutable
public data class EventSpec(
    val name: String,
    val sourceComponentId: String,
    val context: kotlinx.serialization.json.JsonObject =
        kotlinx.serialization.json.JsonObject(emptyMap()),
)
