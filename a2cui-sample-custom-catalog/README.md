# `:a2cui-sample-custom-catalog` — dogfoods `:a2cui-codegen`

End-to-end demo of the A2CUI KSP pipeline:

1. Two `@A2uiComponent`-annotated `ComponentFactory` fields (`RatingFactory`,
   `BadgeFactory`) declare their props + events in Kotlin.
2. [`:a2cui-codegen`](../a2cui-codegen) runs as a KSP processor and emits a
   single aggregating `Catalog` object — `CustomDemoCatalog`.
3. `Main.kt` composes it side-by-side with `Material3BasicCatalog` and replays
   a scripted A2UI stream that mixes standard and custom components.

## How to run

```bash
./gradlew :a2cui-sample-custom-catalog:run
```

Opens a 640x640 window titled *"A2CUI — Custom Catalog Sample"*.

The first build will first run KSP, producing the generated catalog file.

## What you'll see

Two stacked cards:

- **Top — Rendered surface.** Header (`Text`), an emphasised `info` `Badge`
  saying *Beta*, a 5-star `Rating` (value = 4), and a caption hint.
- **Bottom — Outbound events.** Tap any star to fire a `rated` event with
  `context.value` overridden to the tapped index (1..5).

The label above the cards reads `Generated catalog id: custom-demo` — proof
that `CustomDemoCatalog.id` was populated from KSP args.

## Key files

| Concept | Path |
|---|---|
| Desktop entry point | [`src/main/kotlin/.../custom/Main.kt`](src/main/kotlin/dev/mikepenz/a2cui/sample/custom/Main.kt) |
| Annotated factories | [`src/main/kotlin/.../custom/CustomComponents.kt`](src/main/kotlin/dev/mikepenz/a2cui/sample/custom/CustomComponents.kt) |
| Generated catalog (after build) | `build/generated/ksp/main/kotlin/dev/mikepenz/a2cui/sample/custom/CustomDemoCatalog.kt` |
| KSP options | [`build.gradle.kts`](build.gradle.kts) (`ksp { arg(...) }`) |

## KSP options wired in `build.gradle.kts`

```kotlin
ksp {
    arg("a2cui.catalogId", "custom-demo")
    arg("a2cui.catalogPackage", "dev.mikepenz.a2cui.sample.custom")
    arg("a2cui.catalogClassName", "CustomDemoCatalog")
}
```

These drive the generator: the catalog object's package, class name, and the
`id` embedded in `createSurface` `catalogId` lookups.

## Inspect the generated file

After any build that runs the `kspKotlin` task:

```bash
./gradlew :a2cui-sample-custom-catalog:kspKotlin
cat a2cui-sample-custom-catalog/build/generated/ksp/main/kotlin/dev/mikepenz/a2cui/sample/custom/CustomDemoCatalog.kt
```

You'll see a `public object CustomDemoCatalog : Catalog` with `id = "custom-demo"`
and a `register(registry)` that wires each annotated `ComponentFactory` under
the `name` declared in the annotation.

The same processor also emits the JSON schema + a prompt fragment for LLM
consumption — check neighbouring files in the same `generated/ksp` tree.

## How to adapt

- **Add a component.** Declare another `val MyFactory: ComponentFactory =
  @Composable { node, scope -> ... }` with `@A2uiComponent(...)` and rerun the
  build. It appears in `CustomDemoCatalog.register` automatically.
- **Change the catalog id / class.** Update the three `ksp.arg(...)` entries.
- **Use the catalog from another module.** Depend on this module (or copy the
  factories), then pass `CustomDemoCatalog` into
  `rememberSurfaceController(catalogs = listOf(Material3BasicCatalog, CustomDemoCatalog))`.

## See also

- Root: [`../README.md`](../README.md)
- Codegen: [`:a2cui-codegen`](../a2cui-codegen),
  [`:a2cui-codegen-annotations`](../a2cui-codegen-annotations)
- Sibling sample: [`:a2cui-sample`](../a2cui-sample/README.md)
