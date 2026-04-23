# :a2cui-agui

[AG-UI](https://docs.ag-ui.com/) protocol adapter: decodes 16 event types, reduces `STATE_SNAPSHOT` / `STATE_DELTA` into a `DataModel`, reassembles streamed tool calls, and bridges `CUSTOM(name="a2ui")` payloads through `A2uiParser` into the normal A2UI pipeline. Includes the CopilotKit-parity `rememberCoAgentState<T>` composable.

## When to use / when not to

- **Use** when your backend speaks AG-UI (e.g. Pydantic AI, Mastra, LangGraph, ADK adapters, a CopilotKit runtime).
- **Use** when you need JSON-Patch-based shared state between agent and UI.
- **Don't use** if your transport already emits A2UI frames directly — go straight to `:a2cui-transport` + `:a2cui-compose`.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-agui:0.1.0-a01")
    implementation("dev.mikepenz.a2cui:a2cui-compose:0.1.0-a01")
    implementation("dev.mikepenz.a2cui:a2cui-transport:0.1.0-a01")
}
```

```toml
[libraries]
a2cui-agui = { module = "dev.mikepenz.a2cui:a2cui-agui", version.ref = "a2cui" }
```

```kotlin
dependencies { implementation(libs.a2cui.agui) }
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `AguiEvent` | Sealed hierarchy of 16 event types: `RunStarted`, `RunFinished`, `RunError`, `StepStarted`, `StepFinished`, `TextMessageStart`, `TextMessageContent`, `TextMessageEnd`, `ToolCallStart`, `ToolCallArgs`, `ToolCallEnd`, `ToolCallResult`, `StateSnapshot`, `StateDelta`, `MessagesSnapshot`, `Custom`, `Raw`. | [`AguiEvent.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/AguiEvent.kt) |
| `AguiEventParser` | Decodes AG-UI JSON lines (SSE / WS) into `AguiStreamEvent` (Event / ParseError). | [`AguiEventParser.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/AguiEventParser.kt) |
| `AguiA2cuiBridge` | Splits an `AguiEvent` flow into three concerns: `a2uiFrames(...)`, `toolCalls(...)`, `interrupts(...)`. | [`AguiA2cuiBridge.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/AguiA2cuiBridge.kt) |
| `AguiStateReducer` | Applies `StateSnapshot`/`StateDelta` to a `DataModel`; exposes `state: StateFlow<JsonElement>`, `outbound: SharedFlow<JsonArray>` (RFC 6902 patches), and `writeLocal(pointer, value)`. | [`AguiStateReducer.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/AguiStateReducer.kt) |
| `JsonPatch` | Standalone RFC 6902 apply (`add`, `remove`, `replace`, `move`, `copy`, `test`). | [`JsonPatch.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/JsonPatch.kt) |
| `ToolCallEvent` | Reassembled tool-call views: `Started`, `Args`, `ArgsComplete`, `Result`. | [`ToolCallEvent.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/ToolCallEvent.kt) |
| `Interrupt` + `InterruptAwaiter` | HITL: presents `RunFinished(outcome="interrupt")` interrupts and awaits an approval payload to resume the next run. | [`InterruptAwaiter.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/InterruptAwaiter.kt) |
| `rememberCoAgentState<T>` | `@Composable` returning a `MutableState<T>` wired to an `AguiStateReducer` at a JSON Pointer — CopilotKit parity. | [`RememberCoAgentState.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/agui/RememberCoAgentState.kt) |

## Minimal example

```kotlin
val transport: A2uiTransport = SseTransport(httpClient, "https://agent.example/events")
val parser = AguiEventParser()
val reducer = AguiStateReducer()
val bridge = AguiA2cuiBridge()

val rawEvents: Flow<AguiEvent> = transport.incoming()
    .mapNotNull { (parser.parseOne(it) as? AguiStreamEvent.Event)?.event }
    .onEach(reducer::apply)

// A2UI frames travel through the normal SurfaceController path:
val a2uiStream: Flow<A2uiStreamEvent> = bridge.a2uiFrames(rawEvents)

// Tool calls go to :a2cui-actions:
val toolEvents: Flow<ToolCallEvent> = bridge.toolCalls(rawEvents)

@Composable
fun CounterScreen() {
    var count by rememberCoAgentState(pointer = "/count", initial = 0, serializer = Int.serializer(), reducer = reducer)
    Button(onClick = { count += 1 }) { Text("clicks: $count") }
}
```

## Integration

- Sits between `:a2cui-transport` (raw `Flow<String>`) and `:a2cui-compose` (A2UI frames) — the bridge hands `A2uiStreamEvent` to a `SurfaceController` while state + tool-call flows go to `AguiStateReducer` and `:a2cui-actions`.
- `rememberCoAgentState<T>` is the CopilotKit-style entry point; writes call `reducer.writeLocal(...)` which emits an RFC 6902 patch on `outbound` for the app to forward to the agent.

## Known limitations

- No dependency on `com.agui:kotlin-client` — events are decoded in-house via kotlinx-serialization. Revisit if an official KMP SDK lands on Maven Central.
- `InterruptAwaiter` surfaces approval payloads but final `resume[]`-array wiring waits on the AG-UI draft interrupt spec stabilising.
- JSON Patch supports the RFC 6902 verbs; complex path-array `test` edge cases may warrant scrutiny.

## Links

- Root: [../README.md](../README.md)
- AG-UI docs: https://docs.ag-ui.com/
- Research: [../research/ag-ui.md](../research/ag-ui.md), [../research/copilotkit.md](../research/copilotkit.md)
- Actions layer: [../a2cui-actions/README.md](../a2cui-actions/README.md)
