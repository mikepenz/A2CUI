# :a2cui-actions

CopilotKit-style generative-UI actions for Compose: `rememberAction` registers a typed tool, streams partial args as they arrive, and renders UI keyed on four `ToolCallStatus<T>` phases. Also ships `rememberReadable` for exposing local state to the agent.

## When to use / when not to

- **Use** to render UI per streamed tool call (chat-message widgets, approval cards, live tables).
- **Use** `rememberReadable` to publish page-local context that the agent's system prompt should reference.
- **Don't use** for surface rendering — that's `:a2cui-compose`. This module is strictly the gen-UI action layer on top.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-actions:0.1.0-a01")
    implementation("dev.mikepenz.a2cui:a2cui-agui:0.1.0-a01") // typical pairing
}
```

```toml
[libraries]
a2cui-actions = { module = "dev.mikepenz.a2cui:a2cui-actions", version.ref = "a2cui" }
```

```kotlin
dependencies { implementation(libs.a2cui.actions) }
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `ToolCallStatus<T>` | Sealed: `InProgress(partial)` → `Executing(args: T)` → `Complete(args, result)` (or `Failed`). | [`ToolCallStatus.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/ToolCallStatus.kt) |
| `ActionDescriptor` | `name` + `description` + JSON-Schema `parameters` (sent to the agent). | [`ActionDescriptor.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/ActionDescriptor.kt) |
| `ActionRegistry` / `ActionRegistration` | Live per-composition set of actions; `entries: StateFlow<...>`, `descriptors()`. | [`ActionRegistry.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/ActionRegistry.kt) |
| `LocalActionRegistry` | Compose `CompositionLocal` carrying the active registry. | [`LocalActionRegistry.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/LocalActionRegistry.kt) |
| `rememberAction<T>(name, description, schema, parameters, render)` | `@Composable` — registers an action for the current composition's lifetime. | [`RememberAction.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/RememberAction.kt) |
| `rememberReadable(id, description, value)` | `@Composable` — publishes a readable entry to the agent context. | [`RememberReadable.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/RememberReadable.kt) |
| `TypedArgsAccumulator<T>` | Emits `Executing<T>` as soon as partial JSON decodes; drives the executing-tick. | [`TypedArgsAccumulator.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/TypedArgsAccumulator.kt) |
| `StreamingArgsMerger` | LIFO closer-stack that makes mid-stream tool args parseable. | [`StreamingArgsMerger.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/StreamingArgsMerger.kt) |
| `ReadableRegistry` / `LocalReadableRegistry` | Compose-local registry; `asPromptObject()` for embedding in the agent prompt. | [`ActionRegistry.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/ActionRegistry.kt) |
| `decodeArgs<T>(buffered, schema, json)` | Final-parse helper returning `Result<T>`. | [`RememberAction.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/actions/RememberAction.kt) |

## Minimal example

```kotlin
@Serializable data class BookFlight(val from: String, val to: String, val date: String)

@Composable
fun ChatMessage() {
    rememberAction(
        name = "book_flight",
        description = "Book a flight for the user",
        schema = BookFlight.serializer(),
    ) { status ->
        when (status) {
            is ToolCallStatus.InProgress -> CircularProgressIndicator()
            is ToolCallStatus.Executing -> FlightCard(status.args, enabled = false)
            is ToolCallStatus.Complete   -> FlightCard(status.args, enabled = true, result = status.result)
            is ToolCallStatus.Failed     -> ErrorBanner(status.error)
        }
    }

    rememberReadable(id = "user.tier", description = "Loyalty tier", value = "Platinum")
}
```

Wire the registry at the root:

```kotlin
val actions = remember { ActionRegistry() }
CompositionLocalProvider(LocalActionRegistry provides actions) {
    // Pass actions.descriptors() to the agent in your system prompt; route
    // AG-UI TOOL_CALL_* into actions via ToolCallEvent -> registration.accumulatorFactory.
    App()
}
```

## Integration

- Consume `ToolCallEvent` from `:a2cui-agui`'s `AguiA2cuiBridge.toolCalls(...)`. For each `ToolCallEvent.Args` delta, look up the `ActionRegistration` by name, feed the delta into its `accumulatorFactory()` instance, and surface the resulting `ToolCallStatus` to the `render` callback.
- `:a2cui-compose` renders surface UI; `:a2cui-actions` composes on top of it for streamed chat / tool UI.

## Known limitations

- `StreamingArgsMerger` closes partial JSON structurally (LIFO closer stack); pathological unclosed strings still emit `InProgress` until `onEnd`.
- No built-in persistence of `ToolCallStatus.Complete` beyond composition lifetime — store results in your own state holder if the message needs to survive recomposition of a list.
- `ReadableRegistry.asPromptObject()` is a snapshot — call it at prompt-build time, not once at app start.

## Links

- Root: [../README.md](../README.md)
- CopilotKit reference: [../research/copilotkit.md](../research/copilotkit.md)
- AG-UI bridge: [../a2cui-agui/README.md](../a2cui-agui/README.md)
- Compose renderer: [../a2cui-compose/README.md](../a2cui-compose/README.md)
