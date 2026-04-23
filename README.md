# A2CUI — Agent-to-Compose-UI

Kotlin Multiplatform library rendering [Google A2UI v0.9](https://a2ui.org/specification/v0.9-a2ui/) surfaces natively on Compose Multiplatform, with [AG-UI](https://docs.ag-ui.com/) transport and CopilotKit-style generative-UI actions.

Status: **pre-alpha**. The wire format, catalog, and public API are likely to change as A2UI stabilises and real-world agents are wired against A2CUI. See [`SYNTHESIS.md`](SYNTHESIS.md) for the design rationale and [`ARCHITECTURE.md`](ARCHITECTURE.md) for the module plan. Research notes live under [`research/`](research/).

## Why

Agents today talk to chat UIs. We want them to talk to **native** UIs — the same Compose tree a human developer would write, but structured at runtime by an LLM. A2UI gives us a closed, declarative JSON vocabulary; AG-UI gives us a streaming event transport; A2CUI makes both render as real Material3 composables on Android, Desktop, iOS, and the web.

## Modules

| Module | Targets | Purpose |
|---|---|---|
| `:a2cui-core` | android, jvm, iOS, macOS, JS, WASM | A2UI v0.9 wire types, parser, data model, binding resolver |
| `:a2cui-transport` | android, jvm, iOS, macOS, JS, WASM | Ktor SSE + WebSocket + `FakeTransport` for tests |
| `:a2cui-compose` | android, jvm, iOS, macOS, JS, WASM | Material3 basic catalog (15 components), `SurfaceController`, `A2cuiSurface` |
| `:a2cui-agui` | android, jvm, iOS, macOS, JS, WASM | AG-UI event types, `AguiA2cuiBridge`, `AguiStateReducer`, JSON Patch, interrupts |
| `:a2cui-actions` | android, jvm, iOS, macOS, JS, WASM | CopilotKit-style `rememberAction` / `rememberReadable` + streaming-args merger |
| `:a2cui-codegen` | jvm | KSP `@A2uiComponent` processor (planned) |
| `:a2cui-adaptive-cards` | android, jvm, iOS, macOS, JS, WASM | Adaptive Cards v1.6 → A2UI mapper (planned) |
| `:a2cui-sample` | desktop jvm | Runnable Compose Desktop demo with `FakeTransport` replay |

## Quick start

```kotlin
@Composable
fun MyScreen() {
    val transport = remember { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = transport,
    )
    LaunchedEffect(Unit) {
        transport.emit(
            """
            { "version": "v0.9",
              "createSurface": { "surfaceId": "main",
                "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json" } }
            """.trimIndent()
        )
        transport.emit(
            """
            { "version": "v0.9",
              "updateComponents": { "surfaceId": "main", "components": [
                { "id": "root", "component": "Column", "children": ["title", "email"] },
                { "id": "title", "component": "Text", "text": "Hello", "variant": "h1" },
                { "id": "email", "component": "TextField",
                  "value": { "path": "/form/email" }, "label": "Email" } ] } }
            """.trimIndent()
        )
    }
    A2cuiSurface(surfaceId = "main", controller = controller)
}
```

## Run the sample

```bash
./gradlew :a2cui-sample:run
```

Opens a Compose Desktop window driving a scripted booking form through `FakeTransport`.

## Protocol overview

A2UI ships four server→client message types over any JSON-framed channel:
- `createSurface` — register a new surface with a catalog + optional theme.
- `updateComponents` — add / replace components by id (flat adjacency list).
- `updateDataModel` — write a JSON Pointer path.
- `deleteSurface` — tear down a surface.

Inputs two-way bind locally via `{ "path": "/pointer" }` on a property; server-bound events fire via `{ "action": { "event": { "name": "submit", "context": { ... } } } }`.

A2CUI parses the stream, keeps one `SurfaceState` per live surface, and looks up registered `@Composable` factories by component name. Unknown components render a muted `FallbackPlaceholder`; the registry is the **closed trust boundary** — the agent cannot invent components the client has not registered.

## Repository layout

```
A2CUI/
├── build.gradle.kts          # root — convention plugin + apply-false aliases
├── settings.gradle.kts       # module includes + baseLibs version catalog
├── gradle.properties         # GROUP, VERSION, KMP + Compose target flags
├── gradle/libs.versions.toml # local version catalog
├── ARCHITECTURE.md           # module architecture + types + data flow
├── SYNTHESIS.md              # design rationale comparing 10+ agent-UI systems
├── research/                 # 7 deep-dive reports (AG-UI, A2UI, CopilotKit, …)
├── a2cui-core/               # v0.9 wire types, parser, DataModel, JsonPointer, BindingResolver
├── a2cui-transport/          # A2uiTransport + SSE + WS + FakeTransport
├── a2cui-compose/            # SurfaceController, A2cuiSurface, Material3BasicCatalog
├── a2cui-agui/               # AG-UI events, bridge, JSON Patch, state reducer, interrupts
├── a2cui-actions/            # rememberAction / rememberReadable / StreamingArgsMerger
├── a2cui-codegen/            # KSP processor (planned)
├── a2cui-adaptive-cards/     # Adaptive Cards interop (planned)
└── a2cui-sample/             # runnable Compose Desktop demo
```

## Tests

```bash
./gradlew :a2cui-core:jvmTest :a2cui-transport:jvmTest :a2cui-compose:jvmTest \
          :a2cui-agui:jvmTest :a2cui-actions:jvmTest
```

Current: ~76 tests green across 13 suites. UI tests for composables are deferred behind `compose-ui-test`; the state layer is covered by pure-Kotlin unit tests.

## Dependencies

- Kotlin `2.3.20`, Compose Multiplatform `1.10.0`, Ktor `3.4.2`, kotlinx-serialization `1.11.0`, kotlinx-coroutines `latest`, kermit, Turbine (tests).
- No AG-UI Kotlin SDK dependency — AG-UI events are decoded in-house via kotlinx-serialization.

## License

Apache-2.0.
