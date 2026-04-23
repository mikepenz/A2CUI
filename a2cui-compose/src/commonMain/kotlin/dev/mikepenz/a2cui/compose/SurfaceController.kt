package dev.mikepenz.a2cui.compose

import androidx.compose.runtime.Stable
import dev.mikepenz.a2cui.core.A2UI_PROTOCOL_VERSION
import dev.mikepenz.a2cui.core.A2uiClientMessage
import dev.mikepenz.a2cui.core.A2uiFrame
import dev.mikepenz.a2cui.core.A2uiParser
import dev.mikepenz.a2cui.core.ExperimentalA2uiDraft
import dev.mikepenz.a2cui.core.A2uiStreamEvent
import dev.mikepenz.a2cui.core.DataModel
import dev.mikepenz.a2cui.transport.A2uiTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Orchestrates A2UI frames for a running agent session: consumes [A2uiTransport.incoming],
 * applies structural mutations to [surfaces], routes data-model writes, and dispatches user
 * actions back through the transport.
 *
 * Typical lifecycle:
 *  1. Construct on a [CoroutineScope] (usually `rememberCoroutineScope()`).
 *  2. Call [start] — ingestion begins.
 *  3. Bind `A2cuiSurface(surfaceId, controller)` into the composition tree.
 *  4. On cleanup, [close] cancels ingestion and releases the transport.
 */
@Stable
public class SurfaceController(
    public val registry: ComponentRegistry,
    private val transport: A2uiTransport,
    private val scope: CoroutineScope,
    private val parser: A2uiParser = A2uiParser(),
) {
    private val _surfaces = MutableStateFlow<Map<String, SurfaceState>>(emptyMap())
    public val surfaces: StateFlow<Map<String, SurfaceState>> = _surfaces.asStateFlow()

    private val _events = MutableSharedFlow<A2uiClientMessage>(extraBufferCapacity = 64)
    /** Outbound messages (actions, errors) — useful for observability/logging. */
    public val events: SharedFlow<A2uiClientMessage> = _events.asSharedFlow()

    @OptIn(ExperimentalA2uiDraft::class)
    private val _scrollRequests = MutableSharedFlow<A2uiFrame.ScrollTo.Body>(extraBufferCapacity = 16)

    /**
     * A2CUI experimental extension — server-initiated scroll/focus hints (see [A2uiFrame.ScrollTo]).
     * Host catalogs observe this flow to bring the referenced component into view.
     */
    @ExperimentalA2uiDraft
    public val scrollRequests: SharedFlow<A2uiFrame.ScrollTo.Body> get() = _scrollRequests.asSharedFlow()

    private var ingestionJob: Job? = null
    private var closed: Boolean = false

    /** Start ingesting [transport.incoming]. Idempotent — repeated calls are no-ops. */
    public fun start() {
        if (ingestionJob?.isActive == true) return
        check(!closed) { "SurfaceController is closed" }
        ingestionJob = scope.launch {
            parser.parse(transport.incoming()).collect { event ->
                when (event) {
                    is A2uiStreamEvent.Frame -> apply(event.frame)
                    is A2uiStreamEvent.ParseError -> {
                        val err = parser.wrapError(event.error)
                        _events.emit(err)
                        runCatching { transport.send(err) }
                    }
                }
            }
        }
    }

    /** Dispatch a user-triggered action back to the agent. */
    public fun dispatch(
        surfaceId: String,
        name: String,
        sourceComponentId: String,
        context: JsonObject,
    ) {
        val msg = A2uiClientMessage.Action(
            version = A2UI_PROTOCOL_VERSION,
            action = A2uiClientMessage.Action.Body(
                surfaceId = surfaceId,
                name = name,
                sourceComponentId = sourceComponentId,
                context = context,
            ),
        )
        scope.launch {
            _events.emit(msg)
            runCatching { transport.send(msg) }
        }
    }

    /** Release resources. After [close], [start] throws. */
    public fun close() {
        ingestionJob?.cancel()
        ingestionJob = null
        closed = true
        runCatching { transport.close() }
    }

    internal fun applyForTest(frame: A2uiFrame) { apply(frame) }

    @OptIn(ExperimentalA2uiDraft::class)
    private fun apply(frame: A2uiFrame) {
        when (frame) {
            is A2uiFrame.CreateSurface -> {
                val body = frame.createSurface
                _surfaces.update { prev ->
                    prev + (body.surfaceId to SurfaceState(
                        surfaceId = body.surfaceId,
                        catalogId = body.catalogId,
                        theme = body.theme,
                        rootId = SurfaceState.DEFAULT_ROOT_ID,
                        nodesById = emptyMap(),
                        dataModel = DataModel(),
                    ))
                }
            }
            is A2uiFrame.UpdateComponents -> {
                val body = frame.updateComponents
                _surfaces.update { prev ->
                    val existing = prev[body.surfaceId] ?: return@update prev
                    val merged = existing.nodesById.toMutableMap()
                    for (n in body.components) merged[n.id] = n
                    prev + (body.surfaceId to existing.withNodes(merged))
                }
            }
            is A2uiFrame.UpdateDataModel -> {
                val body = frame.updateDataModel
                _surfaces.value[body.surfaceId]?.dataModel?.write(body.path, body.value)
            }
            is A2uiFrame.DeleteSurface -> {
                _surfaces.update { it - frame.deleteSurface.surfaceId }
            }
            is A2uiFrame.DataModelPatch -> {
                // A2CUI experimental extension — apply JSON-Patch style ops against the surface's data model.
                // Only `add`/`replace` (value) and `remove` (no value) are handled here; any
                // unknown op is silently ignored until the shape stabilises.
                val body = frame.dataModelPatch
                val dm = _surfaces.value[body.surfaceId]?.dataModel ?: return
                for (op in body.operations) {
                    when (op.op) {
                        "add", "replace" -> op.value?.let { dm.write(op.path, it) }
                        "remove" -> {
                            // Minimal semantics — write JsonNull to mark removal; structural
                            // removal awaits the finalised shape.
                            dm.write(op.path, kotlinx.serialization.json.JsonNull)
                        }
                    }
                }
            }
            is A2uiFrame.ScrollTo -> {
                // A2CUI experimental extension — hosts observe `scrollRequests` to honour focus
                // / scroll hints in catalog components. A noop when the surface doesn't exist.
                val body = frame.scrollTo
                if (_surfaces.value[body.surfaceId] != null) {
                    _scrollRequests.tryEmit(body)
                }
            }
        }
    }
}
