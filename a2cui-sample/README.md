# `:a2cui-sample` — Compose Desktop demo

Scripted Compose Desktop host that replays a booking-form A2UI stream through
`FakeTransport`. No network, no agent — a deterministic offline demo of
`A2cuiSurface` + the Material3 basic catalog.

## How to run

```bash
./gradlew :a2cui-sample:run
```

Opens a 720x720 dark-theme window titled *"A2CUI Sample — Agent-driven Compose UI"*.

## What you'll see

Two stacked cards:

- **Top — Rendered surface.** The scripted A2UI booking form: title, subtitle,
  two `TextField`s (Email, Name), a `CheckBox` (Email me the receipt), and a
  *Submit booking* `Button`. Email is pre-seeded with `hello@example.com` by the
  third scripted frame.
- **Bottom — Outbound events.** Every `A2uiClientMessage` emitted back to the
  agent lands here. Pressing *Submit booking* appends a `submit_booking` event
  carrying the current email / name / subscribe values resolved from the data
  model.

## Key files

| Concept | Path |
|---|---|
| Desktop `Window` entry point | [`src/main/kotlin/.../sample/Main.kt`](src/main/kotlin/dev/mikepenz/a2cui/sample/Main.kt) |
| Shared Compose body | [`:a2cui-sample-shared` — `SampleApp.kt`](../a2cui-sample-shared/src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleApp.kt) |
| Scripted A2UI frames | [`:a2cui-sample-shared` — `SampleFrames.kt`](../a2cui-sample-shared/src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleFrames.kt) |
| Gradle wiring | [`build.gradle.kts`](build.gradle.kts) |

## How to adapt

This host is a thin `Window { MaterialTheme { A2cuiSampleApp() } }` wrapper —
everything interesting lives in `:a2cui-sample-shared`. To drive it differently:

- **Swap the transport.** Replace `FakeTransport()` in `A2cuiSampleApp` with
  `SseTransport(...)` or `WebSocketTransport(...)` to connect to a real A2UI
  backend. See [`:a2cui-sample-live`](../a2cui-sample-live) for a full AG-UI
  wiring example.
- **Add components.** Pass your own catalog into `rememberSurfaceController`:
  `catalogs = listOf(Material3BasicCatalog, MyCatalog)`. See
  [`:a2cui-sample-custom-catalog`](../a2cui-sample-custom-catalog) for a KSP-
  generated catalog.
- **Change the script.** Edit `SampleFrames.kt` — the JSON is raw so you can
  paste anything your agent would emit.

## See also

- Root: [`../README.md`](../README.md)
- Live AG-UI host: [`:a2cui-sample-live`](../a2cui-sample-live/README.md)
- Android host: [`:a2cui-sample-android`](../a2cui-sample-android/README.md)
- iOS host: [`:a2cui-sample-ios`](../a2cui-sample-ios/README.md)
- Custom catalog: [`:a2cui-sample-custom-catalog`](../a2cui-sample-custom-catalog/README.md)
