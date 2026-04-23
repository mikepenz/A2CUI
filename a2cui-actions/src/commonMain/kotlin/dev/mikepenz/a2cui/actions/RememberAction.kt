package dev.mikepenz.a2cui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Register one generative-UI action scoped to this composition's lifetime.
 *
 * The [render] callback fires on every [ToolCallStatus] transition — incremental partial args
 * during streaming, then typed args once parseable, then a [ToolCallStatus.Complete] with the
 * tool result. The [schema] serializer both validates partial args and produces the decoded
 * typed value for [ToolCallStatus.Executing] / [ToolCallStatus.Complete].
 *
 * On disposal the action is unregistered from [LocalActionRegistry], so the agent's outbound
 * tool list reflects only currently-mounted renderers.
 */
@Composable
public fun <T : Any> rememberAction(
    name: String,
    description: String,
    schema: KSerializer<T>,
    parameters: JsonObject = JsonObject(emptyMap()),
    json: Json = defaultActionJson,
    render: @Composable (ToolCallStatus<T>) -> Unit,
) {
    val registry = LocalActionRegistry.current
    DisposableEffect(name, registry) {
        val wrapped: @Composable (ToolCallStatus<*>) -> Unit = { status ->
            @Suppress("UNCHECKED_CAST")
            render(status as ToolCallStatus<T>)
        }
        registry.register(
            ActionRegistration(
                descriptor = ActionDescriptor(
                    name = name,
                    description = description,
                    parameters = parameters,
                ),
                render = wrapped,
            ),
        )
        onDispose { registry.unregister(name) }
    }
}

/** Decode a fully-buffered args string into [T] using [schema]. Surface failures through [ToolCallStatus.Failed]. */
public fun <T : Any> decodeArgs(
    buffered: String,
    schema: KSerializer<T>,
    json: Json = defaultActionJson,
): Result<T> = runCatching {
    json.decodeFromString(schema, buffered)
}.recoverCatching { cause ->
    if (cause is SerializationException || cause is IllegalArgumentException) {
        throw cause
    } else {
        throw cause
    }
}

internal val defaultActionJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
