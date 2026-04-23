# :a2cui-core

Pure-Kotlin types, parser, data model, and binding resolver for [A2UI v0.9](https://a2ui.org/specification/v0.9-a2ui/). Zero Compose / Ktor dependencies — this is the shared wire layer every other A2CUI module builds on.

## When to use / when not to

- **Use** when you need A2UI frame types, streaming JSON parsing, or a `DataModel` + `BindingResolver` detached from rendering.
- **Use** if you are implementing a non-Compose renderer or a custom transport and still want spec-accurate types.
- **Don't use directly** if you just want to render agent-authored UI in Compose — depend on `:a2cui-compose` instead, which re-exports `:a2cui-core` as `api`.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-core:0.1.0-a01")
}
```

With version catalog:

```toml
# gradle/libs.versions.toml
[versions]
a2cui = "0.1.0-a01"

[libraries]
a2cui-core = { module = "dev.mikepenz.a2cui:a2cui-core", version.ref = "a2cui" }
```

```kotlin
// build.gradle.kts
dependencies { implementation(libs.a2cui.core) }
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`. No platform-specific source sets — everything lives in `commonMain`.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `A2uiFrame` | Sealed, key-discriminated server frame hierarchy (`CreateSurface`, `UpdateComponents`, `UpdateDataModel`, `DeleteSurface`, experimental `DataModelPatch` + `ScrollTo`). | [`A2uiFrame.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/A2uiFrame.kt) |
| `ComponentNode` | Flat adjacency-list node: `id`, `component`, `children`, verbatim `properties: JsonObject`. | [`ComponentNode.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/ComponentNode.kt) |
| `PropertyValue` | Sealed: `Literal` / `Path` (JSON Pointer) / `Call` / experimental `Conditional`. | [`PropertyValue.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/PropertyValue.kt) |
| `DataModel` | `StateFlow<JsonElement>`-backed document with `read`/`write`/`observe`/`snapshot`. | [`DataModel.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/DataModel.kt) |
| `BindingResolver` | Resolves `PropertyValue` against a `DataModel`; supports **scoped** resolution via `withScope(pointer)` for `List` iteration. | [`BindingResolver.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/BindingResolver.kt) |
| `JsonPointer` | RFC 6901 `parse` / `encode` / `read` / immutable `write`. | [`JsonPointer.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/JsonPointer.kt) |
| `A2uiParser` | `parseOne(String) → A2uiStreamEvent` and streaming `parse(Flow<String>)`; materialises `ParseError` instead of throwing. | [`A2uiParser.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/A2uiParser.kt) |
| `A2uiClientMessage` | Outbound sealed envelope: `Action`, `Error`, experimental `Viewport`. | [`A2uiClientMessage.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/A2uiClientMessage.kt) |
| `A2uiJson` | Pre-configured `Json` instance with the A2UI serializers module. | [`A2uiJson.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/A2uiJson.kt) |
| `ErrorCode` | Spec constants for `A2uiClientMessage.Error`. | [`A2uiParser.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/A2uiParser.kt) |

### Experimental opt-in

`@ExperimentalA2uiDraft` (see [`ExperimentalA2uiDraft.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/core/ExperimentalA2uiDraft.kt)) gates four A2CUI-proposed extensions beyond the v0.9 spec: `A2uiFrame.DataModelPatch`, `A2uiFrame.ScrollTo`, `A2uiClientMessage.Viewport`, and `PropertyValue.Conditional`. Wire shapes may change until A2CUI 1.0 or upstream acceptance.

## Minimal example

```kotlin
import dev.mikepenz.a2cui.core.*
import kotlinx.serialization.json.*

val parser = A2uiParser()

val frameJson = """
  {"version":"v0.9","updateComponents":{"surfaceId":"main","components":[
    {"id":"root","component":"Text","properties":{"text":{"path":"/greeting"}}}
  ]}}
""".trimIndent()

when (val ev = parser.parseOne(frameJson)) {
    is A2uiStreamEvent.Event -> {
        val frame = ev.frame as A2uiFrame.UpdateComponents
        val model = DataModel().apply { write("/greeting", JsonPrimitive("Hello")) }
        val resolver = BindingResolver(model)
        val textNode = frame.updateComponents.components.single()
        val resolved = resolver.resolveProperty(textNode.properties["text"]!!)
        println(resolved) // "Hello"
    }
    is A2uiStreamEvent.ParseError -> error(ev.message)
}
```

## Integration

- `:a2cui-transport` owns delivery; it emits `Flow<String>` frames that `A2uiParser` turns into `A2uiStreamEvent`s.
- `:a2cui-compose` wraps `SurfaceController` over a transport + parser, fanning `DataModel` into Compose state.
- `:a2cui-agui` decodes AG-UI `CUSTOM(name="a2ui")` events and forwards their `value` payload through `A2uiParser`.
- `:a2cui-adaptive-cards` produces `A2uiFrame.UpdateComponents` instances from Adaptive Cards JSON.

## Known limitations

- `DataModel` uses immutable-snapshot `StateFlow`; structural sharing is a later optimisation.
- v0.10 A2UI primitives are **not** implemented — see `@ExperimentalA2uiDraft` for the forward-looking subset under opt-in.
- `Headers.applyHeaders` / WebSocket cancel shape lives in `:a2cui-transport` (KMP `runBlocking` + `Map.forEach` inference caveats).

## Links

- Root: [../README.md](../README.md)
- Spec: https://a2ui.org/specification/v0.9-a2ui/
- Architecture: [../ARCHITECTURE.md](../ARCHITECTURE.md)
- Research: [../research/a2ui.md](../research/a2ui.md)
