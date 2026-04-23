# `:a2cui-sample-shared` — shared Compose sample body

Kotlin Multiplatform module (Android + JVM + iOS) that holds the single
`@Composable` driving every sample host in this repo:

- [`:a2cui-sample`](../a2cui-sample) — Compose Desktop window
- [`:a2cui-sample-android`](../a2cui-sample-android) — `ComponentActivity`
- [`:a2cui-sample-ios`](../a2cui-sample-ios) — `ComposeUIViewController`

## Why it exists

The three host modules are thin platform shells (a `Window`, an `Activity`, a
`UIViewController`). Everything agent-facing — the catalog registration, the
`SurfaceController`, the outbound-events pane, and the scripted A2UI replay —
lives here so it stays in lock-step across platforms. If you fix a bug in the
demo, you fix it once.

## How to run

`:a2cui-sample-shared` is a library — it has no `main`. Run one of the host
modules instead:

```bash
./gradlew :a2cui-sample:run                      # Desktop
./gradlew :a2cui-sample-android:installDebug     # Android (device/emulator)
./gradlew :a2cui-sample-ios:linkDebugFrameworkIosSimulatorArm64   # iOS framework
```

To compile just this module:

```bash
./gradlew :a2cui-sample-shared:assemble
```

## What's in `SampleFrames.kt`

[`SampleFrames.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleFrames.kt)
holds the scripted A2UI stream as raw JSON strings (on purpose — the demo shows
exactly the bytes an agent would send):

| Function | Emits |
|---|---|
| `createSurface(id)` | `createSurface` frame binding to the Material3 basic catalog |
| `bookingComponents(id)` | `updateComponents` with a `Column` root containing title, subtitle, `TextField` x2, `CheckBox`, and a submit `Button` whose `action.event` is `submit_booking` with context refs |
| `seedEmail(id)` | `updateDataModel` writing `hello@example.com` to `/form/email` |

`A2cuiSampleApp` emits these in order, with small delays between, to mimic a
live streaming agent.

## What's in `SampleApp.kt`

[`SampleApp.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleApp.kt)
exposes `A2cuiSampleApp()`:

1. Creates a `FakeTransport` and a `SurfaceController` bound to
   `Material3BasicCatalog`.
2. Collects `controller.events` into a `mutableStateListOf<String>` so outbound
   messages appear live in the UI.
3. Renders a two-pane layout: the `A2cuiSurface` on top, the outbound-events
   log underneath.

## Key files

| Concept | Path |
|---|---|
| Shared composable | [`src/commonMain/kotlin/.../shared/SampleApp.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleApp.kt) |
| Scripted A2UI frames | [`src/commonMain/kotlin/.../shared/SampleFrames.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleFrames.kt) |
| Gradle wiring | [`build.gradle.kts`](build.gradle.kts) |

## How to adapt

- **Different transport.** Replace `FakeTransport()` with `SseTransport(url)`
  or `WebSocketTransport(url)` — the rest of the composable is transport-
  agnostic. For the AG-UI → A2UI bridging pattern see
  [`:a2cui-sample-live`](../a2cui-sample-live).
- **Different frames.** Edit `SampleFrames.kt` or build your own
  `updateComponents` payload. The JSON must match the A2UI v0.9 wire format;
  see [`:a2cui-core`](../a2cui-core) for the types.
- **Extra components.** Append catalogs in `rememberSurfaceController`:
  `catalogs = listOf(Material3BasicCatalog, MyCatalog)`. See
  [`:a2cui-sample-custom-catalog`](../a2cui-sample-custom-catalog) for
  KSP-generated examples.

## See also

- Root: [`../README.md`](../README.md)
- Hosts: [`:a2cui-sample`](../a2cui-sample/README.md),
  [`:a2cui-sample-android`](../a2cui-sample-android/README.md),
  [`:a2cui-sample-ios`](../a2cui-sample-ios/README.md)
