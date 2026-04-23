package dev.mikepenz.a2cui.actions

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * One registered action — its [descriptor] (metadata) plus the [render] callback invoked by
 * the generative-UI layer with the current [ToolCallStatus].
 */
public class ActionRegistration(
    public val descriptor: ActionDescriptor,
    public val render: @Composable (ToolCallStatus<*>) -> Unit,
    /**
     * Optional factory for a fresh [TypedArgsAccumulator] scoped to one tool-call id.
     * Non-null when the action was registered with a typed [kotlinx.serialization.KSerializer]
     * via [dev.mikepenz.a2cui.actions.rememberAction]. Dispatchers use this to drive
     * [ToolCallStatus.Executing] emissions mid-stream as partial JSON becomes decodable.
     */
    public val accumulatorFactory: (() -> TypedArgsAccumulator<*>)? = null,
)

/**
 * Mutable store of currently-registered generative-UI actions. Exposed to the composition
 * via [dev.mikepenz.a2cui.actions.LocalActionRegistry]; the closed set of names is the trust
 * boundary against hallucinated tool calls — incoming `TOOL_CALL_START` events whose name is
 * not present are rejected before composition runs.
 */
public class ActionRegistry {
    private val _entries = MutableStateFlow<Map<String, ActionRegistration>>(emptyMap())
    public val entries: StateFlow<Map<String, ActionRegistration>> = _entries.asStateFlow()

    public fun register(registration: ActionRegistration) {
        _entries.update { it + (registration.descriptor.name to registration) }
    }

    public fun unregister(name: String) {
        _entries.update { it - name }
    }

    public operator fun get(name: String): ActionRegistration? = _entries.value[name]

    /** Descriptors only — what the agent sees in its tool list. */
    public fun descriptors(): List<ActionDescriptor> =
        _entries.value.values.map { it.descriptor }
}
