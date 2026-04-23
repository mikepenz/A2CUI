# A2CUI — Architecture & Project Structure

**Prepared**: 2026-04-22.
**Scope**: Kotlin Multiplatform library rendering A2UI v0.9 to Compose Multiplatform, with AG-UI transport and CopilotKit-style generative-UI actions.
**Gradle convention**: mirrors `mikepenz/compose-buddy` — `baseLibs` version catalog from `com.mikepenz:version-catalog`, local `libs` catalog in `gradle/libs.versions.toml`, convention plugins `com.mikepenz.convention.kotlin-multiplatform` + `com.mikepenz.convention.publishing`, type-safe project accessors, `dev.mikepenz.a2cui.*` package namespace.

---

## 1. Module graph

```
                               ┌────────────────────────┐
                               │  :a2cui-core           │  pure Kotlin MPP
                               │  (A2UI v0.9 types,     │  commonMain only
                               │   parser, data model,  │  no Compose, no Ktor
                               │   binding resolver)    │
                               └─────────┬──────────────┘
                                         │
        ┌────────────────────────────────┼────────────────────────────┐
        │                                │                            │
        ▼                                ▼                            ▼
┌───────────────────┐       ┌────────────────────────┐     ┌──────────────────────┐
│ :a2cui-transport  │       │ :a2cui-compose         │     │ :a2cui-codegen       │
│ (Ktor WS/SSE      │       │ (Material3 basic       │     │ (KSP processor       │
│  transport,       │       │  catalog, SurfaceCtrl, │     │  @A2uiComponent →    │
│  A2UI-over-JSON   │       │  ComponentRegistry,    │     │  factories + schema) │
│  framing)         │       │  BindingAdapters)      │     └──────────────────────┘
└─────────┬─────────┘       └─────────┬──────────────┘
          │                           │
          ▼                           │
┌──────────────────────┐              │
│ :a2cui-agui          │              │
│ (depends on          │              │
│  com.agui:           │              │
│  kotlin-client;      │              │
│  AG-UI events →      │              │
│  A2UI messages)      │              │
└─────────┬────────────┘              │
          │                           │
          └───────────┬───────────────┘
                      ▼
          ┌──────────────────────────────┐
          │ :a2cui-actions               │
          │ (rememberAction,             │
          │  rememberReadable,           │
          │  rememberCoAgentState;       │
          │  CopilotKit-style gen-UI)    │
          └──────────────┬───────────────┘
                         │
                         ▼
          ┌──────────────────────────────┐
          │ :a2cui-adaptive-cards        │  optional interop
          │ (Adaptive Cards v1.6 →       │
          │  A2UI message mapper)        │
          └──────────────────────────────┘

          ┌──────────────────────────────┐
          │ :a2cui-sample                │  Android + Desktop + iOS
          │ (wires everything against    │
          │  a local AG-UI dev server)   │
          └──────────────────────────────┘
```

Dependency rule: arrows point **down** to deps. `:a2cui-core` has zero third-party runtime deps beyond kotlinx-serialization + kotlinx-coroutines + kermit. `:a2cui-compose` never talks to transport directly. `:a2cui-actions` composes `:a2cui-core` + `:a2cui-compose` + `:a2cui-agui`.

## 2. Module catalog

Each row: module name, purpose, KMP targets, key artifacts, upstream runtime deps.

| Module | Purpose | Targets | Key public types | Runtime deps (beyond stdlib) |
|---|---|---|---|---|
| `:a2cui-core` | A2UI v0.9 wire types + parser + data model + binding resolver | jvm, android, iosArm64, iosX64, iosSimulatorArm64, desktop (jvm) | `A2uiMessage`, `ComponentNode`, `PropertyValue`, `DataModel`, `JsonPointer`, `BindingResolver`, `A2uiParser`, `A2uiFrame` | `kotlinx-serialization-json`, `kotlinx-coroutines-core`, `kermit` |
| `:a2cui-transport` | Ktor SSE/WS transport emitting `Flow<String>` JSON frames | same as core | `A2uiTransport`, `SseTransport`, `WebSocketTransport`, `FakeTransport` (testing) | `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-okhttp` (android), `ktor-client-darwin` (ios), `ktor-client-java` (desktop) |
| `:a2cui-compose` | Material3 catalog + renderer + `SurfaceController` + registry | android, ios, jvm (Compose MP) | `SurfaceController`, `A2cuiSurface`, `ComponentRegistry`, `Material3BasicCatalog`, `WebResource`, `A2cuiTheme` | `compose-multiplatform`, `:a2cui-core` |
| `:a2cui-agui` | AG-UI event → A2UI message adapter; HITL `awaitApproval()` | same as core | `AguiA2cuiBridge`, `AguiTransport`, `AwaitApproval`, `AguiStateReducer` | `com.agui:kotlin-client:0.2.6`, `:a2cui-core`, `:a2cui-transport` |
| `:a2cui-actions` | Generative-UI hooks (CopilotKit-style) | android, ios, jvm (Compose MP) | `rememberAction`, `rememberReadable`, `rememberCoAgentState`, `ActionRegistry`, `ToolCallStatus`, `LocalActionRegistry` | `compose-multiplatform`, `:a2cui-core`, `:a2cui-agui` |
| `:a2cui-codegen` | KSP annotations + processor emitting catalog factories + JSON Schema | jvm (KSP) | `@A2uiComponent`, `@A2uiProp`, `@A2uiEvent`, `@A2uiSlot`, `A2uiComponentProcessor` | `com.google.devtools.ksp:symbol-processing-api`, `kotlinx-serialization-json` |
| `:a2cui-adaptive-cards` *(optional)* | Adaptive Cards v1.6 JSON → A2UI message translator | same as core | `AdaptiveCardsMapper`, `AdaptiveCardHostConfig` | `:a2cui-core`, `kotlinx-serialization-json` |
| `:a2cui-sample` | KMP sample app | android app, desktop app, iosApp | n/a (apps) | everything |

Publishing: every module except `:a2cui-sample` publishes to Maven Central under `dev.mikepenz.a2cui:<module>`.

## 3. Directory layout

```
A2CUI/
├── build.gradle.kts                        # root: convention plugin + apply-false aliases
├── settings.gradle.kts                     # module includes, baseLibs version catalog
├── gradle.properties                       # GROUP=dev.mikepenz.a2cui, target flags, POM metadata
├── gradle/
│   └── libs.versions.toml                  # local catalog (ktor, agui, serialization, ksp)
├── gradle/wrapper/
├── gradlew, gradlew.bat
├── renovate.json
├── PUBLICATION.md                          # mirrors compose-buddy
├── README.md
├── research/                               # already exists — 7 reports
├── SYNTHESIS.md                            # already exists
├── ARCHITECTURE.md                         # this file
│
├── a2cui-core/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/dev/mikepenz/a2cui/core/
│       │   ├── A2uiFrame.kt                # @Serializable sealed interface top-level messages
│       │   ├── ComponentNode.kt            # flat-adjacency component node
│       │   ├── PropertyValue.kt            # Literal | Path | Call
│       │   ├── DataModel.kt                # MutableMap-backed JSON document + observer
│       │   ├── JsonPointer.kt              # RFC 6901 resolver (read/write/observe)
│       │   ├── BindingResolver.kt          # PropertyValue × DataModel → resolved value
│       │   ├── A2uiParser.kt               # Flow<String> → Flow<A2uiFrame>
│       │   ├── Surface.kt                  # surfaceId → components + data model projection
│       │   ├── SurfaceState.kt             # immutable snapshot the renderer consumes
│       │   ├── Action.kt                   # outbound action + error envelopes
│       │   └── SerializersModule.kt
│       └── commonTest/kotlin/dev/mikepenz/a2cui/core/
│           ├── A2uiParserTest.kt           # round-trip fixtures from google/A2UI samples
│           ├── DataModelTest.kt            # pointer reads, updates, observer fan-out
│           ├── BindingResolverTest.kt      # literal/path/call resolution
│           └── fixtures/                   # sample JSON from A2UI spec
│
├── a2cui-transport/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/dev/mikepenz/a2cui/transport/
│       │   ├── A2uiTransport.kt            # sealed interface { open(): Flow<String>; send(A2uiClientMessage) }
│       │   ├── SseTransport.kt             # Ktor client SSE
│       │   ├── WebSocketTransport.kt       # Ktor client WS
│       │   ├── FakeTransport.kt            # test helper
│       │   └── transport.Framing.kt
│       └── commonTest/…
│
├── a2cui-compose/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/dev/mikepenz/a2cui/compose/
│       │   ├── SurfaceController.kt        # ingests A2uiFrame, holds SurfaceState, emits actions
│       │   ├── A2cuiSurface.kt             # @Composable entry point
│       │   ├── ComponentRegistry.kt        # name → ComponentFactory
│       │   ├── ComponentFactory.kt         # typealias (ComponentNode, RenderScope) -> @Composable () -> Unit
│       │   ├── RenderScope.kt              # exposes data-model bindings, event emitter, theme
│       │   ├── A2cuiTheme.kt               # Material3 HostConfig mapping
│       │   ├── Bindings.kt                 # Compose adapters over BindingResolver
│       │   ├── catalog/Material3BasicCatalog.kt     # Text, Image, Icon, Row, Column, List, Card, Button, CheckBox, TextField, DateTimeInput, ChoicePicker, Slider, Modal, Tabs
│       │   ├── catalog/TextComponent.kt
│       │   ├── catalog/ButtonComponent.kt
│       │   ├── catalog/TextFieldComponent.kt
│       │   ├── catalog/…                   # one file per component
│       │   └── escape/WebResource.kt       # expect/actual (Android WebView / WKWebView / JCEF)
│       ├── androidMain/kotlin/…/escape/WebResource.android.kt
│       ├── iosMain/kotlin/…/escape/WebResource.ios.kt
│       ├── jvmMain/kotlin/…/escape/WebResource.jvm.kt
│       └── commonTest/kotlin/…             # snapshot-less tests (state transitions, event emission)
│
├── a2cui-agui/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/dev/mikepenz/a2cui/agui/
│       │   ├── AguiTransport.kt            # wraps com.agui.client.AgUiAgent as A2uiTransport
│       │   ├── AguiA2cuiBridge.kt          # BaseEvent → A2uiFrame mapper (CUSTOM + STATE_* + TOOL_* fan-out)
│       │   ├── AguiStateReducer.kt         # STATE_SNAPSHOT + JSON Patch → DataModel writes
│       │   ├── Interrupts.kt               # RUN_FINISHED(interrupt) → awaitApproval suspend
│       │   └── AguiConfig.kt
│       └── commonTest/…
│
├── a2cui-actions/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/dev/mikepenz/a2cui/actions/
│       │   ├── ActionRegistry.kt           # name → (ToolCallStatus) -> @Composable
│       │   ├── LocalActionRegistry.kt      # CompositionLocal
│       │   ├── RememberAction.kt           # rememberAction(name, schema) { state -> ... }
│       │   ├── RememberReadable.kt         # rememberReadable(description, value)
│       │   ├── RememberCoAgentState.kt     # STATE_SNAPSHOT/DELTA ↔ MutableState<T>
│       │   ├── ToolCallStatus.kt           # sealed interface
│       │   └── StreamingArgsMerger.kt      # JSON-Patch-like deep merge over partial ARGS deltas
│       └── commonTest/…
│
├── a2cui-codegen/
│   ├── build.gradle.kts                    # KSP jvm-only
│   └── src/
│       ├── main/kotlin/dev/mikepenz/a2cui/codegen/
│       │   ├── annotations/                # @A2uiComponent, @A2uiProp, @A2uiEvent, @A2uiSlot
│       │   ├── A2uiComponentProcessor.kt
│       │   ├── SchemaEmitter.kt            # JSON Schema per component
│       │   ├── FactoryEmitter.kt           # Kotlin registration helper
│       │   └── PromptFragmentEmitter.kt    # system-prompt tool fragment
│       └── test/…
│
├── a2cui-adaptive-cards/
│   └── src/commonMain/kotlin/dev/mikepenz/a2cui/adaptivecards/
│       ├── AdaptiveCard.kt                 # schema types
│       ├── AdaptiveCardsMapper.kt          # AdaptiveCard → List<A2uiFrame>
│       └── HostConfig.kt
│
└── a2cui-sample/
    ├── settings.gradle.kts                 # composite build hook if kept separate
    ├── shared/                             # Compose Multiplatform sample UI
    ├── androidApp/
    ├── desktopApp/
    └── iosApp/
```

Package root throughout: `dev.mikepenz.a2cui.<module>`. Matches compose-buddy's `dev.mikepenz.composebuddy.<module>` convention.

## 4. Core types — signatures

### 4.1 Wire layer (`:a2cui-core`)

```kotlin
package dev.mikepenz.a2cui.core

@Serializable
sealed interface A2uiFrame {
    val version: String        // "v0.9"

    @Serializable @SerialName("createSurface")
    data class CreateSurface(
        override val version: String,
        val surfaceId: String,
        val catalogId: String,
        val theme: JsonObject? = null,
        val sendDataModel: Boolean = false,
    ) : A2uiFrame

    @Serializable @SerialName("updateComponents")
    data class UpdateComponents(
        override val version: String,
        val surfaceId: String,
        val components: List<ComponentNode>,
    ) : A2uiFrame

    @Serializable @SerialName("updateDataModel")
    data class UpdateDataModel(
        override val version: String,
        val surfaceId: String,
        val path: String,       // JSON Pointer
        val value: JsonElement,
    ) : A2uiFrame

    @Serializable @SerialName("deleteSurface")
    data class DeleteSurface(
        override val version: String,
        val surfaceId: String,
    ) : A2uiFrame
}

@Serializable
data class ComponentNode(
    val id: String,
    val component: String,                     // catalog type name
    val children: List<String> = emptyList(),
    @Contextual val properties: JsonObject = JsonObject(emptyMap()),
)

@Serializable
sealed interface PropertyValue {
    @Serializable @SerialName("literal")
    data class Literal(val value: JsonElement) : PropertyValue
    @Serializable @SerialName("path")
    data class Path(val path: String) : PropertyValue     // JSON Pointer
    @Serializable @SerialName("call")
    data class Call(val call: String, val args: JsonObject = JsonObject(emptyMap())) : PropertyValue
}

class DataModel {
    fun read(pointer: String): JsonElement?
    fun write(pointer: String, value: JsonElement)
    fun observe(pointer: String): Flow<JsonElement?>   // SharedFlow fanned out per pointer
    fun snapshot(): JsonObject
}

interface BindingResolver {
    fun resolve(prop: PropertyValue, scope: BindingScope): JsonElement
}

class A2uiParser(json: Json = A2uiJson) {
    fun parse(frames: Flow<String>): Flow<A2uiFrame>
    fun encode(frame: A2uiClientMessage): String
}

@Serializable
sealed interface A2uiClientMessage {
    @Serializable @SerialName("action")
    data class Action(
        val surfaceId: String,
        val name: String,
        val context: JsonObject,
        val sourceComponentId: String,
        val timestamp: Long,
    ) : A2uiClientMessage

    @Serializable @SerialName("error")
    data class Error(val code: String, val path: String? = null, val message: String) : A2uiClientMessage
}
```

### 4.2 Transport (`:a2cui-transport`)

```kotlin
package dev.mikepenz.a2cui.transport

interface A2uiTransport : AutoCloseable {
    fun incoming(): Flow<String>
    suspend fun send(message: A2uiClientMessage)
}

class SseTransport(
    private val httpClient: HttpClient,
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
) : A2uiTransport

class WebSocketTransport(
    private val httpClient: HttpClient,
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
) : A2uiTransport

class FakeTransport(incoming: Flow<String>) : A2uiTransport  // tests
```

### 4.3 Compose layer (`:a2cui-compose`)

```kotlin
package dev.mikepenz.a2cui.compose

@Stable
class SurfaceController(
    private val registry: ComponentRegistry,
    private val transport: A2uiTransport,
    private val scope: CoroutineScope,
) {
    val surfaces: StateFlow<Map<String, SurfaceState>>
    val events: SharedFlow<A2uiClientMessage>

    suspend fun start()
    fun dispatch(surfaceId: String, action: A2uiClientMessage.Action)
    fun close()
}

@Composable
fun rememberSurfaceController(
    catalogs: List<Catalog> = listOf(Material3BasicCatalog),
    transport: A2uiTransport,
): SurfaceController

@Composable
fun A2cuiSurface(
    surfaceId: String,
    controller: SurfaceController,
    modifier: Modifier = Modifier,
)

class ComponentRegistry {
    fun register(name: String, factory: ComponentFactory)
    fun registerAll(catalog: Catalog)
    operator fun get(name: String): ComponentFactory?
}

typealias ComponentFactory = @Composable (ComponentNode, RenderScope) -> Unit

interface Catalog {
    val id: String                         // matches A2UI catalogId URI
    fun install(registry: ComponentRegistry)
    fun toJsonSchema(): JsonObject         // for the agent system prompt
}

object Material3BasicCatalog : Catalog

class RenderScope internal constructor(
    val dataModel: DataModel,
    val theme: A2cuiTheme,
    val onEvent: (EventSpec) -> Unit,
    val children: List<ComponentNode>,
    val resolver: BindingResolver,
)
```

### 4.4 AG-UI bridge (`:a2cui-agui`)

```kotlin
package dev.mikepenz.a2cui.agui

class AguiTransport(
    private val agent: AgUiAgent,
) : A2uiTransport                    // converts Flow<BaseEvent> → Flow<A2uiFrame-as-string>

class AguiA2cuiBridge(
    private val parser: A2uiParser,
) {
    fun map(events: Flow<BaseEvent>): Flow<A2uiFrame>
    //  CUSTOM{ name="a2ui", value=<frame json> } → parser.parse(single)
    //  STATE_SNAPSHOT / STATE_DELTA              → synthesized UpdateDataModel frames
    //  TOOL_CALL_START / _ARGS / _RESULT         → passed through to :a2cui-actions
}

class InterruptAwaiter(private val bridge: AguiA2cuiBridge) {
    suspend fun awaitApproval(): ApprovalRequest
    fun resolve(id: String, payload: JsonObject?)
}
```

### 4.5 Generative-UI actions (`:a2cui-actions`)

```kotlin
package dev.mikepenz.a2cui.actions

val LocalActionRegistry = staticCompositionLocalOf<ActionRegistry> { ActionRegistry() }

@Composable
fun <T : Any> rememberAction(
    name: String,
    description: String,
    schema: KSerializer<T>,
    render: @Composable (ToolCallStatus<T>) -> Unit,
)

@Composable
fun rememberReadable(description: String, value: Any?)

@Composable
fun <T : Any> rememberCoAgentState(name: String, initial: T, serializer: KSerializer<T>): MutableState<T>

sealed interface ToolCallStatus<out T> {
    data class InProgress<T>(val partial: JsonObject) : ToolCallStatus<T>
    data class Executing<T>(val args: T) : ToolCallStatus<T>
    data class Complete<T>(val args: T, val result: JsonElement) : ToolCallStatus<T>
    data class Failed<T>(val error: Throwable) : ToolCallStatus<T>
}
```

## 5. Data flow

```
Agent backend (LangGraph, ADK, Mastra, bare LLM, …)
        │  AG-UI SSE/WS stream
        ▼
[AguiTransport] ─ Flow<BaseEvent> ─┐
                                   │
                                   ├─ CUSTOM(a2ui) ─┐
                                   ├─ STATE_*  ─────┤
                                   └─ TOOL_*   ─────┘
                                                    ▼
                                          [AguiA2cuiBridge]
                                                    │
                                                    │ Flow<A2uiFrame> + Flow<ToolCallEvent>
                                                    ▼
                               ┌── [A2uiParser] ──────[SurfaceController]
                               │                            │
                               │        DataModel writes    │
                               │        SurfaceState updates│
                               │                            ▼
                               │                     Compose recomposition
                               │                            │
                               │                            ▼
                               │                 ┌───────────────────────┐
                               │                 │ A2cuiSurface(id)      │
                               │                 │  ComponentRegistry    │
                               │                 │  Material3 factories  │
                               │                 │  BindingResolver      │
                               │                 │  @Composable tree     │
                               │                 └───────────┬───────────┘
                               │                             │
                               │                             │ user tap / input
                               │                             ▼
                               │                 [ActionEvent emitter]
                               │                             │
                               └─ action / error ────────────┘
                                              ▼
                                      [AguiTransport.send]
                                              │
                                              ▼
                                         Agent backend
```

Key properties:
- **Ingress is a pipeline of `Flow`s**, pure suspend functions, no UI thread coupling.
- **DataModel mutations invalidate Compose state** via `SnapshotStateMap` writes; `A2cuiSurface` recomposes only the components whose bindings touch changed pointers.
- **Input components are local-first**: TextField `onValueChange` writes to DataModel synchronously; an outgoing action only fires on an explicit `event` declaration. Matches A2UI spec §5.
- **Every boundary is testable**: `FakeTransport` drives `SurfaceController` in `commonTest`; snapshot JSON fixtures round-trip through `A2uiParser`.
- **Unknown component types** resolve to `FallbackPlaceholder` emitted by `ComponentRegistry[get]`; logged via kermit.

## 6. Security invariants enforced in code

- `ComponentRegistry[name]` returns `null` for unregistered components → renderer emits `FallbackPlaceholder`, never crashes, never allows agent-named code paths.
- `A2uiParser.parse` validates against `@Serializable` schema; unknown frame types emit `A2uiClientMessage.Error("UNKNOWN_FRAME", …)` back to the agent.
- `ActionRegistry` is a closed set populated only by `rememberAction`. Incoming `TOOL_CALL_START` for unregistered names → rejected before composition, error bubbled to chat transcript.
- `A2cuiTheme` clamps agent-supplied `theme.primaryColor` etc. through `MaterialTheme.colorScheme` — agents cannot force off-brand or inaccessible colors.
- `WebResource` escape-hatch is opt-in: an app must explicitly register it in the `ComponentRegistry` to accept `WebResource` frames. Default catalog does not.
- `BindingResolver` resolves paths against the local `DataModel` at fire time, so the agent never sees inlined PII in `context` — only the pointer.

## 7. Testing strategy

| Layer | Style | Tooling |
|---|---|---|
| `:a2cui-core` | Pure unit | kotlin-test + JUnit5, JSON fixtures from [github.com/google/A2UI](https://github.com/google/A2UI) samples |
| `:a2cui-transport` | Integration | MockEngine in Ktor client; `FakeTransport` for downstream tests |
| `:a2cui-compose` | Compose UI test | `androidx.compose.ui:ui-test` on jvm/desktop, Paparazzi on Android (matches compose-buddy's approach) |
| `:a2cui-agui` | Event sequencing | Fake `AgUiAgent` delivering canned `BaseEvent` sequences |
| `:a2cui-actions` | Compose UI test + coroutine turbine | `app.cash.turbine` for Flows |
| `:a2cui-codegen` | Source-gen assertion | Golden-file comparison of generated Kotlin + JSON Schema |
| End-to-end | Fixture-driven | A2UI "Restaurant Finder" and "Contact Lookup" sample scripts replayed through `FakeTransport` |

## 8. Gradle scaffolding (compose-buddy convention)

`settings.gradle.kts`:
```kotlin
rootProject.name = "a2cui"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
    versionCatalogs {
        create("baseLibs") { from("com.mikepenz:version-catalog:0.14.4") }
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

include(":a2cui-core")
include(":a2cui-transport")
include(":a2cui-compose")
include(":a2cui-agui")
include(":a2cui-actions")
include(":a2cui-codegen")
include(":a2cui-adaptive-cards")
// :a2cui-sample kept as a separate composite build
```

Root `build.gradle.kts` mirrors compose-buddy:
```kotlin
plugins {
    alias(baseLibs.plugins.conventionPlugin)

    alias(baseLibs.plugins.kotlinMultiplatform) apply false
    alias(baseLibs.plugins.kotlinJvm) apply false
    alias(baseLibs.plugins.kotlinAndroid) apply false
    alias(baseLibs.plugins.androidLibrary) apply false
    alias(baseLibs.plugins.composeMultiplatform) apply false
    alias(baseLibs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false

    alias(baseLibs.plugins.mavenPublish) apply false
    alias(baseLibs.plugins.binaryCompatiblityValidator) apply false
    alias(baseLibs.plugins.dokka) apply false
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}
```

`gradle.properties` (derived from compose-buddy):
```properties
app.version=0.1.0-a01

GROUP=dev.mikepenz.a2cui
VERSION_NAME=0.1.0-a01
POM_URL=https://github.com/mikepenz/A2CUI
POM_SCM_URL=https://github.com/mikepenz/A2CUI
POM_SCM_CONNECTION=scm:git@github.com:mikepenz/A2CUI.git
POM_SCM_DEV_CONNECTION=scm:git@github.com:mikepenz/A2CUI.git
POM_GITHUB_REPO=mikepenz/A2CUI
POM_LICENCE_NAME=Apache-2.0
POM_LICENCE_URL=https://www.apache.org/licenses/LICENSE-2.0.txt
POM_DEVELOPER_ID=mikepenz
POM_DEVELOPER_NAME=Mike Penz

com.mikepenz.binary-compatibility-validator.enabled=true

# Convention targets — multiplatform: android + jvm + ios
com.mikepenz.targets.enabled=true
com.mikepenz.android.enabled=true
com.mikepenz.jvm.enabled=true
com.mikepenz.js.enabled=false
com.mikepenz.wasm.enabled=false
com.mikepenz.native.enabled=true
com.mikepenz.composeNative.enabled=true
com.mikepenz.tapmoc.enabled=false
com.mikepenz.compatPatrouille.enabled=false
com.mikepenz.kotlin.version=2.3
com.mikepenz.java.version=21
com.mikepenz.kotlin.warningsAsErrors.enabled=false

kotlin.code.style=official
kotlin.daemon.jvmargs=-Xmx3072M
android.useAndroidX=true

org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8
org.gradle.caching=true
```

`gradle/libs.versions.toml` (delta beyond compose-buddy — just what A2CUI adds):
```toml
[versions]
kotlin = "2.3.20"
kotlinx-serialization = "1.11.0"
ktor = "3.4.2"
agui-kotlin = "0.2.6"
ksp = "2.3.6"
kermit = "2.1.0"
turbine = "1.3.0"
junit5 = "5.11.4"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-websockets = { module = "io.ktor:ktor-client-websockets", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
agui-kotlin-core = { module = "com.agui:kotlin-core", version.ref = "agui-kotlin" }
agui-kotlin-client = { module = "com.agui:kotlin-client", version.ref = "agui-kotlin" }
agui-kotlin-tools = { module = "com.agui:kotlin-tools", version.ref = "agui-kotlin" }
ksp-api = { module = "com.google.devtools.ksp:symbol-processing-api", version.ref = "ksp" }
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit5" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Example module `build.gradle.kts` — `:a2cui-core`:
```kotlin
plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.publishing")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(baseLibs.kotlinx.coroutines.core)
            api(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
        }
    }
}
```

Example module `build.gradle.kts` — `:a2cui-compose`:
```kotlin
plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.publishing")
    alias(baseLibs.plugins.composeMultiplatform)
    alias(baseLibs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
        }
    }
}
```

Example module `build.gradle.kts` — `:a2cui-agui`:
```kotlin
plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.publishing")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            api(projects.a2cuiTransport)
            implementation(libs.agui.kotlin.core)
            implementation(libs.agui.kotlin.client)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
        }
    }
}
```

`:a2cui-codegen` stays JVM-only, matching the `compose-buddy-device-ksp` pattern — `kotlinMultiplatform` with `jvm()` target only, `libs.ksp.api` as the sole runtime dep.

## 9. KMP target matrix

| Module | android | jvm | iosArm64 | iosX64 | iosSimulatorArm64 | js | wasm |
|---|---|---|---|---|---|---|---|
| `:a2cui-core` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| `:a2cui-transport` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| `:a2cui-compose` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| `:a2cui-agui` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| `:a2cui-actions` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| `:a2cui-codegen` | — | ✓ | — | — | — | — | — |
| `:a2cui-adaptive-cards` | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |

`com.mikepenz.native.enabled=true` + `com.mikepenz.composeNative.enabled=true` in `gradle.properties` enables iOS targets via the convention plugin. js/wasm disabled for v1 to match the transport dependency footprint (Ktor client has JS but `com.agui:kotlin-client` is jvm/android/native only).

## 10. Versioning & binary compatibility

- Every module opts into `com.mikepenz.binary-compatibility-validator.enabled=true` so API surface is tracked.
- Semantic versioning: `0.1.0-a01` → `0.1.0` → `0.2.0` as A2UI spec moves.
- A2UI protocol version supported is surfaced as a constant in `:a2cui-core` (`A2uiProtocol.VERSION = "v0.9"`), generated via the same `VersionTask` pattern compose-buddy uses.
- v0.10 primitives live behind `@ExperimentalA2uiV010` opt-ins.

## 11. Implementation order (first 14 days)

1. **Day 1** — scaffold repo: root build, settings, gradle.properties, libs.versions.toml, empty modules.
2. **Day 2** — `:a2cui-core` `A2uiFrame` + `ComponentNode` + `PropertyValue` with round-trip tests on A2UI spec fixtures.
3. **Day 3** — `DataModel` + `JsonPointer` + `BindingResolver` + tests.
4. **Day 4** — `A2uiParser` streaming; `A2uiClientMessage` envelope; error paths.
5. **Day 5** — `:a2cui-transport` Ktor SSE + WS + `FakeTransport`; MockEngine tests.
6. **Day 6–7** — `:a2cui-compose` `SurfaceController` + `ComponentRegistry` + `A2cuiSurface` wiring against `FakeTransport`.
7. **Day 8–9** — Material3 basic catalog: Text, Column, Row, Card, Button, TextField.
8. **Day 10** — remaining basic-catalog components (CheckBox, Slider, ChoicePicker, List, Tabs, Modal, DateTimeInput, Image, Icon).
9. **Day 11** — `:a2cui-agui` bridge: CUSTOM-carried A2UI frames + STATE_* reducer + HITL `awaitApproval`.
10. **Day 12** — `:a2cui-actions` `rememberAction` + `ToolCallStatus` + streaming args merger.
11. **Day 13** — `:a2cui-sample` Android + Desktop apps against a local AG-UI dev server (e.g. Pydantic AI quickstart).
12. **Day 14** — README, PUBLICATION.md, CI, snapshot publish to Maven Central staging.

`:a2cui-codegen` and `:a2cui-adaptive-cards` are deferred past day 14.

## 12. Cross-references

- `research/a2ui.md` — v0.9 wire format authoritative
- `research/ag-ui.md` — event shapes used by `:a2cui-agui`
- `research/copilotkit.md` — inspires `:a2cui-actions` API
- `research/compose-sdui-ecosystem.md` — `com.agui:kotlin-client:0.2.6` dependency, `@A2uiComponent` DSL from Nativeblocks
- `research/mcp-ui-and-others.md` — Adaptive Cards mapping, `WebResource` escape-hatch
- `SYNTHESIS.md` — why this architecture, why not others
