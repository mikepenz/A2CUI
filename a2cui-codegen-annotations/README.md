# :a2cui-codegen-annotations

Annotations consumed by the `:a2cui-codegen` KSP processor to declare custom A2UI catalogs. Pure-metadata module — no runtime deps.

## When to use / when not to

- **Use** on `ComponentFactory` properties when you want KSP to generate a `Catalog`, JSON Schema, and tool-prompt fragment for you.
- **Don't use** if you're hand-writing `registry.register(...)` calls — the annotations have no runtime effect on their own.

## Install

```kotlin
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-codegen-annotations:0.1.0-a01")
}
// plus the KSP processor in your build (see :a2cui-codegen)
```

```toml
[libraries]
a2cui-codegen-annotations = { module = "dev.mikepenz.a2cui:a2cui-codegen-annotations", version.ref = "a2cui" }
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `@A2uiComponent(name, description, props, events, slots)` | Marks a `ComponentFactory` property for catalog inclusion. | [`Annotations.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/codegen/annotations/Annotations.kt) |
| `@A2uiProp(name, type, description, required, defaultValue, enumValues)` | Declares a renderable / bindable property. | same |
| `@A2uiEvent(name, description, context)` | Declares an outbound event plus its context schema. | same |
| `@A2uiSlot(name, description, multiple)` | Declares a child-slot position. | same |
| `A2uiPropType` | Enum: `STRING`, `NUMBER`, `INTEGER`, `BOOLEAN`, `ARRAY`, `OBJECT`. | same |

## Minimal example

```kotlin
import dev.mikepenz.a2cui.codegen.annotations.*

@A2uiComponent(
    name = "Rating",
    description = "A 1-5 star rating selector",
    props = [
        A2uiProp("value", A2uiPropType.INTEGER, "Current star value", required = true),
        A2uiProp("max", A2uiPropType.INTEGER, "Maximum stars", defaultValue = "5"),
        A2uiProp("size", A2uiPropType.STRING, "Visual size", enumValues = ["small", "medium", "large"]),
    ],
    events = [
        A2uiEvent(
            name = "onChange",
            description = "Fired when the user picks a rating",
            context = [A2uiProp("value", A2uiPropType.INTEGER, "The picked rating", required = true)],
        ),
    ],
)
public val RatingFactory: @Composable (ComponentNode, RenderScope) -> Unit = { node, scope ->
    val value = scope.resolveInt(node, "value", 0)
    val max = scope.resolveInt(node, "max", 5)
    RatingRow(value, max, onClick = {
        scope.emitAction(node, overrides = buildJsonObject { put("value", JsonPrimitive(it)) })
    })
}
```

The `:a2cui-codegen` processor then emits a `Catalog` object whose `install(registry)` registers `"Rating"` against `RatingFactory`, whose `toJsonSchema()` serialises the declared props/events, and whose `ToolPromptFragment` documents the catalog for the agent prompt.

## Integration

- Consumed by `:a2cui-codegen`. Apply the KSP plugin in your module's `build.gradle.kts` and add `:a2cui-codegen` as a `ksp(...)` dependency.
- Generated `Catalog` implements `dev.mikepenz.a2cui.compose.Catalog` so it plugs into `ComponentRegistry.registerAll(...)`.

## Known limitations

- Annotations only apply to top-level properties of type `ComponentFactory`; class-level and function-level support is not yet wired (the processor logs a warning).
- `defaultValue` is a plain `String`; multi-typed default expressions are out of scope for v0.1.

## Links

- Root: [../README.md](../README.md)
- KSP processor: [../a2cui-codegen/README.md](../a2cui-codegen/README.md)
- Working example: [../a2cui-sample-custom-catalog](../a2cui-sample-custom-catalog)
