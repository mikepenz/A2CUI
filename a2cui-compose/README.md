# :a2cui-compose

Compose Multiplatform renderer for A2UI surfaces. Ships the `SurfaceController`, a pluggable `ComponentRegistry` / `Catalog` model, the `Material3BasicCatalog` (15 v0.9 components), and the `CatalogHelpers` toolkit for writing your own component factories.

## When to use / when not to

- **Use** for Compose UI rendering of A2UI frames — Android, Desktop, iOS, Web (JS/Wasm).
- **Use** when you want a turnkey Material3 catalog — `Image` uses Coil 3 out of the box; override via `registry.register("Image") { ... }` to plug your own loader.
- **Don't use** in headless or non-Compose contexts — use `:a2cui-core` directly.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-compose:0.1.0-a02")
    implementation("dev.mikepenz.a2cui:a2cui-transport:0.1.0-a02")
}
```

```toml
[libraries]
a2cui-compose = { module = "dev.mikepenz.a2cui:a2cui-compose", version.ref = "a2cui" }
a2cui-transport = { module = "dev.mikepenz.a2cui:a2cui-transport", version.ref = "a2cui" }
```

```kotlin
dependencies {
    implementation(libs.a2cui.compose)
    implementation(libs.a2cui.transport)
}
```

## Targets supported

`android`, `jvm` (Desktop), `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`. Requires Compose Multiplatform 1.10+ and Kotlin 2.3+.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `SurfaceController` | Ingests `A2uiFrame`s from a transport, manages `SurfaceState`, exposes `surfaces: StateFlow<Map<String, SurfaceState>>`, `events: SharedFlow<A2uiClientMessage>`, `scrollRequests`. | [`SurfaceController.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/SurfaceController.kt) |
| `A2cuiSurface` | `@Composable` entry point that renders one surface by id. | [`A2cuiSurface.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/A2cuiSurface.kt) |
| `rememberSurfaceController` | Composable factory binding a controller to the current composition scope. | [`A2cuiSurface.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/A2cuiSurface.kt) |
| `ComponentRegistry` | Name-to-factory map; `register`, `registerAll(catalog)`, `contains`, `names`. | [`ComponentRegistry.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/ComponentRegistry.kt) |
| `Catalog` | Interface: `id`, `install(registry)`, optional `toJsonSchema()`. | [`Catalog.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/Catalog.kt) |
| `RenderScope` | Per-node scope: `dataModel`, `resolver`, `theme`, `registry`, `nodesById`, `surfaceId`, `emit(EventSpec)`, `Child(id)`, `withScope(iterationPointer)`. | [`RenderScope.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/RenderScope.kt) |
| `EventSpec` | Outbound event payload (name, sourceComponentId, context). | [`RenderScope.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/RenderScope.kt) |
| `Material3BasicCatalog` | Object catalog of 15 v0.9 components (see below). | [`catalog/Material3BasicCatalog.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/catalog/Material3BasicCatalog.kt) |
| `CatalogHelpers` (package fns) | `resolveString`, `resolveInt`, `resolveBool`, `resolveDouble`, `resolveStringList`, `resolve`, `emitAction`, `parseAction`, `pathOf`, `displayString`. | [`catalog/CatalogHelpers.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/catalog/CatalogHelpers.kt) |
| `FallbackPlaceholder` | Muted placeholder for unknown components. | [`FallbackPlaceholder.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/compose/FallbackPlaceholder.kt) |

### Material3BasicCatalog components

`Text`, `Column`, `Row`, `Card`, `Button`, `TextField`, `CheckBox`, `Slider`, `ChoicePicker`, `List`, `Tabs`, `Modal`, `DateTimeInput`, `Image`, `Icon`.

`List` supports both flat-children mode and iteration-scope mode: `items: {path: "/users"}` plus the first child as a template — `BindingResolver.withScope(...)` rewrites per-item `{path}` refs relative to the iteration element. `Image` uses Coil 3 by default; override via `registry.register("Image") { ... }` to plug your own loader. `Icon` renders a labelled placeholder by default.

## Minimal example

```kotlin
@Composable
fun App(transport: A2uiTransport) {
    val controller = rememberSurfaceController(
        catalogs = listOf(Material3BasicCatalog),
        transport = transport,
    )
    MaterialTheme { A2cuiSurface(surfaceId = "main", controller = controller) }
}
```

Adding a custom component with `CatalogHelpers`:

```kotlin
val RatingFactory: @Composable (ComponentNode, RenderScope) -> Unit = { node, scope ->
    val stars = scope.resolveInt(node, "value", default = 0)
    val action = parseAction(node.properties["action"])
    RatingBar(stars, onClick = {
        if (action != null) scope.emitAction(node, overrides = buildJsonObject { put("value", JsonPrimitive(it)) })
    })
}

registry.register("Rating", RatingFactory)
```

## Integration

- Takes an `A2uiTransport` from `:a2cui-transport` (see [minimum viable wiring in STATUS.md](../STATUS.md#73-minimum-viable-wiring)).
- Pair with `:a2cui-agui` when the upstream is AG-UI — the bridge converts `CUSTOM(a2ui)` events into `A2uiFrame`s and fans `STATE_*` / `TOOL_*` out separately.
- `:a2cui-actions` layers generative-UI actions on top of the registry via `LocalActionRegistry`.
- `:a2cui-codegen` can produce a `Catalog` object from `@A2uiComponent`-annotated factory properties.

## Known limitations

- `Material3BasicCatalog` uses `TabRow` (deprecated in favour of `PrimaryTabRow` — warning only).
- `Image` uses Coil 3 by default; override via `registry.register("Image") { ... }` to plug your own loader. `Icon` renders a placeholder until the host overrides.
- Paparazzi / Android snapshot tests are outstanding; JVM `compose-ui-test` harnesses are wired.
- Compose MP deprecation warnings for `compose.runtime` / `compose.foundation` / `compose.material3` are non-blocking.

## Links

- Root: [../README.md](../README.md)
- Spec: https://a2ui.org/specification/v0.9-a2ui/
- Core: [../a2cui-core/README.md](../a2cui-core/README.md)
- Codegen example: [../a2cui-sample-custom-catalog](../a2cui-sample-custom-catalog)
