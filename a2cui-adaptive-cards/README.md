# :a2cui-adaptive-cards

Maps [Adaptive Cards v1.6](https://adaptivecards.io/) JSON to an `A2uiFrame.UpdateComponents`, so Copilot Studio / Microsoft 365 Agent SDK cards render through the same A2CUI pipeline as native A2UI.

## When to use / when not to

- **Use** to render Adaptive Cards inside a Compose app via `:a2cui-compose`'s `Material3BasicCatalog`.
- **Use** as an interop bridge when your backend only emits Adaptive Cards.
- **Don't use** if the upstream already speaks A2UI — skip the translation overhead.

## Install

```kotlin
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-adaptive-cards:0.1.0-a01")
    implementation("dev.mikepenz.a2cui:a2cui-compose:0.1.0-a01")
}
```

```toml
[libraries]
a2cui-adaptive-cards = { module = "dev.mikepenz.a2cui:a2cui-adaptive-cards", version.ref = "a2cui" }
```

```kotlin
dependencies { implementation(libs.a2cui.adaptive.cards) }
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `AdaptiveCardsMapper` | Object with `map(card: JsonObject, surfaceId: String = "main"): Result`. | [`AdaptiveCardsMapper.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/adaptivecards/AdaptiveCardsMapper.kt) |
| `AdaptiveCardsMapper.Result` | `frame: A2uiFrame.UpdateComponents?` + `warnings: List<String>`. | [`AdaptiveCardsMapper.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/adaptivecards/AdaptiveCardsMapper.kt) |

### Mapping table

| Adaptive Cards | A2UI v0.9 (Material3BasicCatalog) |
|---|---|
| `AdaptiveCard` (root) | `Column` container |
| `Container` | `Column` |
| `ColumnSet` / `Column` | `Row` / child `Column` |
| `TextBlock` | `Text` |
| `Image` | `Image` |
| `Input.Text` | `TextField` (`{path}` two-way binding) |
| `Input.Number` | `TextField` with numeric hint |
| `Input.Toggle` | `CheckBox` |
| `Input.ChoiceSet` | `ChoicePicker` |
| `Input.Date` / `Input.Time` | `DateTimeInput` |
| `FactSet` | `Column` of labelled `Row`s |
| `ActionSet` | `Row` of `Button`s |
| `Action.Submit` | `Button` with `event` emitting inputs |
| `Action.OpenUrl` | `Button` with `call: openUrl` |

Unmapped node types (e.g. `Action.ShowCard`, `RichTextBlock.inlines`) are dropped with a string appended to `warnings`.

## Minimal example

```kotlin
import dev.mikepenz.a2cui.adaptivecards.AdaptiveCardsMapper
import kotlinx.serialization.json.Json

val card = Json.parseToJsonElement("""
  {
    "type":"AdaptiveCard","version":"1.6","body":[
      {"type":"TextBlock","text":"Hello","weight":"Bolder"},
      {"type":"Input.Text","id":"name","placeholder":"Name"}
    ],
    "actions":[{"type":"Action.Submit","title":"Send","id":"submit"}]
  }
""").jsonObject

val result = AdaptiveCardsMapper.map(card, surfaceId = "chat")
result.warnings.forEach(::println)
val frame = result.frame ?: error("card was empty")

// Feed it through the usual surface controller:
val controller = SurfaceController(registry, transport, scope)
controller.ingest(frame) // or via FakeTransport.emit(A2uiJson.encodeToString(frame))
```

## Integration

- Output is a vanilla `A2uiFrame.UpdateComponents` — route it through any `SurfaceController` + `Material3BasicCatalog` pipeline from `:a2cui-compose`.
- If the card has `Input.*`, the generated components already use `{path: "/<id>"}` bindings so submits emit an `Action` carrying the inputs.

## Known limitations

- Mapping covers v1.6 core; v1.7+ features and host-config theming overrides are out of scope for v0.1.
- `Action.ShowCard`, inline rich text, and `Media` are currently unmapped.
- Two-way binding is single-surface; multi-card stacks need one surface id per card.

## Links

- Root: [../README.md](../README.md)
- Adaptive Cards: https://adaptivecards.io/
- Research: [../research/mcp-ui-and-others.md](../research/mcp-ui-and-others.md)
- Compose renderer: [../a2cui-compose/README.md](../a2cui-compose/README.md)
