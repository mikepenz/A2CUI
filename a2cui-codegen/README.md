# :a2cui-codegen

KSP processor that scans your module for `@A2uiComponent`-annotated `ComponentFactory` properties and generates:

1. A `Catalog` object (extends `dev.mikepenz.a2cui.compose.Catalog`) whose `install(registry)` wires every factory.
2. A JSON Schema (`toJsonSchema(): JsonObject`) describing props / events / slots for the agent's tool list.
3. A markdown prompt fragment describing the catalog so an LLM knows how to emit frames.

## When to use / when not to

- **Use** when you author a custom catalog and want the schema + prompt fragment generated for free.
- **Use** to keep the agent's system prompt in sync with your Compose factories without hand-maintaining JSON.
- **Don't use** if you're only consuming `Material3BasicCatalog` — you don't need KSP at all.

## Install

This is a **JVM-only** KSP processor. Apply it in your catalog module's `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-codegen-annotations:0.1.0-a02")
    implementation("dev.mikepenz.a2cui:a2cui-compose:0.1.0-a02")
    ksp("dev.mikepenz.a2cui:a2cui-codegen:0.1.0-a02")
}

ksp {
    arg("a2cui.catalogId", "com.example.custom")
    arg("a2cui.catalogPackage", "com.example.generated")
    arg("a2cui.catalogClassName", "CustomDemoCatalog")
}
```

Catalog-style:

```toml
[versions]
a2cui = "0.1.0-a02"
ksp = "2.3.6"

[libraries]
a2cui-codegen-annotations = { module = "dev.mikepenz.a2cui:a2cui-codegen-annotations", version.ref = "a2cui" }
a2cui-codegen = { module = "dev.mikepenz.a2cui:a2cui-codegen", version.ref = "a2cui" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
plugins { alias(libs.plugins.ksp) }
dependencies {
    implementation(libs.a2cui.codegen.annotations)
    ksp(libs.a2cui.codegen)
}
```

## Targets supported

**JVM only.** KSP runs on JVM even if your downstream catalog module is multiplatform — declare the `ksp(...)` dep on the platform-specific configuration (`kspCommonMainMetadata`, `kspAndroid`, `kspJvm`, `kspIosArm64`, …) that matches your catalog's source sets.

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `A2uiSymbolProcessorProvider` | KSP entry point (registered via `META-INF/services`). Instantiates `A2uiSymbolProcessor`. | [`A2uiSymbolProcessor.kt`](src/main/kotlin/dev/mikepenz/a2cui/codegen/A2uiSymbolProcessor.kt) |

Internal (drives code generation, not part of the published ABI):

| Symbol | Purpose | Source |
|---|---|---|
| `A2uiSymbolProcessor` | Collects `@A2uiComponent` properties, delegates to the three emitters. | [`A2uiSymbolProcessor.kt`](src/main/kotlin/dev/mikepenz/a2cui/codegen/A2uiSymbolProcessor.kt) |
| `SpecExtractor` | Normalises KSP annotation arguments into `ComponentSpec`. | same |
| `SchemaGenerator` | Produces the catalog-level JSON Schema. | [`SchemaGenerator.kt`](src/main/kotlin/dev/mikepenz/a2cui/codegen/SchemaGenerator.kt) |
| `PromptGenerator` | Produces the markdown tool-prompt fragment. | [`PromptGenerator.kt`](src/main/kotlin/dev/mikepenz/a2cui/codegen/PromptGenerator.kt) |
| `CatalogGenerator` | Emits the `Catalog` Kotlin source file. | [`CatalogGenerator.kt`](src/main/kotlin/dev/mikepenz/a2cui/codegen/CatalogGenerator.kt) |

### KSP options

| Option | Default | Purpose |
|---|---|---|
| `a2cui.catalogId` | `custom` | Value of the generated `Catalog.id` (matches A2UI `catalogId`). |
| `a2cui.catalogPackage` | `a2cui.generated` | Package of the generated object. |
| `a2cui.catalogClassName` | `GeneratedA2cuiCatalog` | Simple name of the generated object. |

## Generated artefacts

For N `@A2uiComponent`-annotated properties, the processor emits **one** Kotlin file at `<catalogPackage>.<catalogClassName>`:

```kotlin
public object CustomDemoCatalog : Catalog {
    override val id: String = "com.example.custom"
    override fun install(registry: ComponentRegistry) {
        registry.register("Rating", com.example.RatingFactory)
        registry.register("Badge", com.example.BadgeFactory)
    }
    override fun toJsonSchema(): JsonObject = /* pre-baked schema */
    public val ToolPromptFragment: String = /* markdown */
}
```

## Minimal example

See [`:a2cui-sample-custom-catalog`](../a2cui-sample-custom-catalog) — `RatingFactory` + `BadgeFactory` are annotated with `@A2uiComponent`, KSP emits `CustomDemoCatalog`, and the catalog is plugged into a Compose Desktop app:

```kotlin
val registry = ComponentRegistry()
    .registerAll(Material3BasicCatalog)
    .registerAll(CustomDemoCatalog)
```

## Integration

- Paired with `:a2cui-codegen-annotations` — consumers put the annotations on their `ComponentFactory` properties and add this module as a `ksp(...)` dep.
- Generated `Catalog` plugs into `:a2cui-compose`'s `ComponentRegistry` exactly like `Material3BasicCatalog`.

## Known limitations

- Only top-level `ComponentFactory` properties are supported today; class- and function-level annotations emit a warning and are skipped.
- One catalog per KSP invocation — you can't partition factories across multiple generated catalogs without splitting modules.
- `aggregating = true` dependencies: any change to any annotated property triggers a full catalog regeneration (small cost for v0.1).

## Links

- Root: [../README.md](../README.md)
- Annotations: [../a2cui-codegen-annotations/README.md](../a2cui-codegen-annotations/README.md)
- Working example: [../a2cui-sample-custom-catalog](../a2cui-sample-custom-catalog)
- Compose renderer: [../a2cui-compose/README.md](../a2cui-compose/README.md)
