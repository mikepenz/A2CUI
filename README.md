# A2CUI — Agent-to-Compose-UI

<!-- TODO: hero screenshot of :a2cui-sample running -->

Render [Google A2UI v0.9](https://a2ui.org/specification/v0.9-a2ui/) surfaces natively on Compose Multiplatform, driven over [AG-UI](https://docs.ag-ui.com/) streaming transport, with CopilotKit-style generative-UI actions layered on top.

Kotlin Multiplatform. Material3 catalog. Host-owned theming. No WebView.

<!-- TODO: badges — CI / Maven Central / License -->
<!-- TODO: ![CI](…) ![Maven Central](…) ![License: Apache-2.0](…) -->

## Why A2CUI

**Closed component vocabulary.** The catalog is the trust boundary. Agents cannot invent widgets the client has not registered — unknown components render a muted `FallbackPlaceholder`, never arbitrary code. Every rendered pixel comes from a factory you shipped in the binary.

**Local-first input binding.** Inputs mutate the local `DataModel` synchronously via JSON Pointer paths — `TextField`, `Slider`, `CheckBox`, `ChoicePicker`, `DateTimeInput` all two-way bind without a server round-trip. Only explicit `event` declarations flush to the agent. IME latency stays native.

**Host-owned theming.** Agents supply semantic hints (`primary`, `error`, variants). Compose resolves them through `MaterialTheme`. Off-brand colors and inaccessible contrast are physically not expressible in the wire format.

Design rationale in [SYNTHESIS.md](SYNTHESIS.md); module graph in [ARCHITECTURE.md](ARCHITECTURE.md).

## Quickstart

Add the dependency (planned coords — not yet published to Maven Central):

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-compose:0.1.0-a02")
    // optional:
    // implementation("dev.mikepenz.a2cui:a2cui-agui:0.1.0-a02")
    // implementation("dev.mikepenz.a2cui:a2cui-actions:0.1.0-a02")
}
```

Minimum viable surface:

```kotlin
@Composable
fun Demo() {
    val transport = remember { FakeTransport() }
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = transport,
    )
    LaunchedEffect(transport) {
        transport.emit(CREATE_SURFACE)
        transport.emit(UPDATE_COMPONENTS)
    }
    A2cuiSurface(surfaceId = "main", controller = controller)
}
```

First frames to paste in:

```json
{ "version": "v0.9",
  "createSurface": { "surfaceId": "main",
    "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json" } }
```

```json
{ "version": "v0.9",
  "updateComponents": { "surfaceId": "main", "components": [
    { "id": "root",  "component": "Column", "children": ["hello", "go"] },
    { "id": "hello", "component": "Text",   "text": "Hello from an agent", "variant": "h2" },
    { "id": "go",    "component": "Button", "label": "Continue",
      "action": { "event": { "name": "submit" } } }
  ] } }
```

Swap `FakeTransport()` for `SseTransport(...)` or `WebSocketTransport(...)` when you wire a real agent.

## Platforms

Per [STATUS.md §2.2](STATUS.md#22-module-matrix):

| Target | `:a2cui-core` / `-transport` / `-compose` / `-agui` / `-actions` / `-adaptive-cards` | `:a2cui-codegen` |
|---|---|---|
| android | yes | — |
| jvm (desktop) | yes | yes |
| iosArm64 / iosX64 / iosSimulatorArm64 | yes | — |
| macosArm64 / macosX64 | yes | — |
| js | yes | — |
| wasmJs | yes | — |

## Module map

One-line summary per publishable module. Each links to its own README (authored separately).

- [`:a2cui-core`](a2cui-core/) — A2UI v0.9 wire types, parser, `DataModel`, `JsonPointer`, `BindingResolver`. Pure Kotlin, zero Compose.
- [`:a2cui-transport`](a2cui-transport/) — `A2uiTransport` interface plus Ktor `SseTransport`, `WebSocketTransport`, and `FakeTransport` for tests.
- [`:a2cui-compose`](a2cui-compose/) — `SurfaceController`, `A2cuiSurface`, `ComponentRegistry`, and the 15-component `Material3BasicCatalog`.
- [`:a2cui-agui`](a2cui-agui/) — AG-UI event decoder, `AguiA2cuiBridge`, `AguiStateReducer` (JSON Patch), interrupt awaiter, `rememberCoAgentState`.
- [`:a2cui-actions`](a2cui-actions/) — CopilotKit-style `rememberAction` / `rememberReadable` with typed streaming-args decoding.
- [`:a2cui-adaptive-cards`](a2cui-adaptive-cards/) — Adaptive Cards v1.6 → A2UI v0.9 mapper for Copilot Studio interop.
- [`:a2cui-codegen`](a2cui-codegen/) — KSP processor for `@A2uiComponent` emitting catalog factories, JSON Schema, and tool-prompt fragments.
- [`:a2cui-codegen-annotations`](a2cui-codegen-annotations/) — multiplatform annotations consumed by `:a2cui-codegen`.

## Samples

- [`a2cui-sample`](a2cui-sample/) — Compose Desktop app wrapping the shared sample in a `Window`. Run: `./gradlew :a2cui-sample:run`.
- [`a2cui-sample-shared`](a2cui-sample-shared/) — `A2cuiSampleApp` composable + `SampleFrames`, shared across desktop / android / ios hosts.
- [`a2cui-sample-android`](a2cui-sample-android/) — `MainActivity` hosting the shared sample inside an `Activity`.
- [`a2cui-sample-ios`](a2cui-sample-ios/) — `SampleViewController` exposing the shared sample as a `ComposeUIViewController`.
- [`a2cui-sample-live`](a2cui-sample-live/) — desktop host spinning up `MockAguiServer` in-process and wiring `SseTransport` → `AguiA2cuiBridge` → `A2cuiSurface`.
- [`a2cui-sample-custom-catalog`](a2cui-sample-custom-catalog/) — dogfoods `:a2cui-codegen`: annotated `RatingFactory` + `BadgeFactory` compile into a `CustomDemoCatalog`.

## Live agent backends

Runnable backends that emit AG-UI events wrapping A2UI frames — point `:a2cui-sample-live` (or your own host) at them:

- [`integrations/pydantic-ai/`](integrations/pydantic-ai/) — Python + Pydantic AI scaffold.
- [`integrations/mastra/`](integrations/mastra/) — TypeScript + Mastra scaffold.

Both speak AG-UI over SSE with `CUSTOM { name: "a2ui" }` frames carrying A2UI v0.9 JSON.

## Status & roadmap

- **Stable (v0.1)** — wire types, parser, transports, `Material3BasicCatalog`, `AguiA2cuiBridge`, `rememberAction`, `:a2cui-adaptive-cards`, `:a2cui-codegen`. 125 tests green on JVM; Android / iOS / macOS / JS / WASM compile.
- **Experimental** — `@ExperimentalA2uiDraft` primitives (`DataModelPatch`, `ScrollTo`, `Viewport`, `Conditional`) ahead of upstream A2UI spec changes. AG-UI interrupt resume semantics still draft.
- **Next** — real Pydantic AI / Mastra / ADK integration, Paparazzi snapshot tests on Android, Maven Central publish, Compose MP 1.11 deprecation migration, optional `WebResource` escape-hatch for MCP Apps.

Full execution log, test matrix, and deferrals: [STATUS.md](STATUS.md).

## Contributing

CONTRIBUTING.md is **WIP** — see [`CONTRIBUTING.md`](CONTRIBUTING.md) once it lands. In the meantime, issues and PRs are welcome; run `./gradlew assemble check` before opening one.

## License

Apache-2.0. See [`gradle.properties`](gradle.properties) for POM metadata.
