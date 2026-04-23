package dev.mikepenz.a2cui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.mikepenz.a2cui.core.BindingResolver
import dev.mikepenz.a2cui.transport.A2uiTransport

/**
 * Top-level composable rendering one A2UI surface. Looks up the current [SurfaceState] on
 * [controller], walks from the root node, and dispatches user events back through the
 * controller's transport.
 */
@Composable
public fun A2cuiSurface(
    surfaceId: String,
    controller: SurfaceController,
    modifier: Modifier = Modifier,
) {
    val surfaces by controller.surfaces.collectAsState()
    val state = surfaces[surfaceId] ?: return

    val resolver = remember(state.dataModel) { BindingResolver(state.dataModel) }
    val scope = remember(state, resolver) {
        RenderScope(
            surfaceId = state.surfaceId,
            dataModel = state.dataModel,
            resolver = resolver,
            registry = controller.registry,
            nodesById = state.nodesById,
            theme = A2cuiTheme(hints = state.theme),
            eventEmitter = { ev ->
                controller.dispatch(
                    surfaceId = state.surfaceId,
                    name = ev.name,
                    sourceComponentId = ev.sourceComponentId,
                    context = ev.context,
                )
            },
        )
    }

    Box(modifier) { scope.Child(state.rootId) }
}

/**
 * Convenience builder: creates a registry from [catalogs], wires a [SurfaceController] to
 * [transport] on a `rememberCoroutineScope`, starts ingestion, and closes on disposal.
 */
@Composable
public fun rememberSurfaceController(
    catalogs: List<Catalog>,
    transport: A2uiTransport,
): SurfaceController {
    val scope = rememberCoroutineScope()
    val controller = remember(catalogs, transport) {
        val registry = ComponentRegistry().also { r -> catalogs.forEach { r.registerAll(it) } }
        SurfaceController(registry = registry, transport = transport, scope = scope).also { it.start() }
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    return controller
}
