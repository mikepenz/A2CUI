# `:a2cui-sample-android` — Android host

Android application that embeds the shared Compose sample
(`A2cuiSampleApp` from [`:a2cui-sample-shared`](../a2cui-sample-shared)) in a
single `MainActivity`. Demonstrates that the same A2UI surface renders
unmodified on Android.

## How to run

Build the debug APK from the command line:

```bash
./gradlew :a2cui-sample-android:assembleDebug
```

Output: `a2cui-sample-android/build/outputs/apk/debug/a2cui-sample-android-debug.apk`.

Install on a connected device / emulator:

```bash
./gradlew :a2cui-sample-android:installDebug
adb shell am start -n dev.mikepenz.a2cui.sample.android/.MainActivity
```

Or open the project root in Android Studio, select the
**a2cui-sample-android** run configuration, and press Run.

- `minSdk` = 23
- `applicationId` = `dev.mikepenz.a2cui.sample.android`

## What you'll see

Same UI as [`:a2cui-sample`](../a2cui-sample), scaled to the device screen:

- A rendered booking form (title, email + name `TextField`s, subscribe
  `CheckBox`, submit `Button`) with email seeded to `hello@example.com`.
- Below it, an outbound-events log. Tapping *Submit booking* appends a
  `submit_booking` event.

## Key files

| Concept | Path |
|---|---|
| `Activity` entry point | [`src/main/kotlin/.../android/MainActivity.kt`](src/main/kotlin/dev/mikepenz/a2cui/sample/android/MainActivity.kt) |
| Manifest | [`src/main/AndroidManifest.xml`](src/main/AndroidManifest.xml) |
| Shared Compose body | [`:a2cui-sample-shared` — `SampleApp.kt`](../a2cui-sample-shared/src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleApp.kt) |
| Gradle wiring | [`build.gradle.kts`](build.gradle.kts) |

## How to adapt

- **Custom theme.** Replace `MaterialTheme(colorScheme = darkColorScheme())` in
  `MainActivity` with your app theme. The A2UI surface resolves semantic hints
  against whatever `MaterialTheme` is in scope.
- **Real backend.** Fork `A2cuiSampleApp` in `:a2cui-sample-shared` and swap
  `FakeTransport` for `SseTransport` / `WebSocketTransport`, or for an in-
  process AG-UI bridge — see [`:a2cui-sample-live`](../a2cui-sample-live).
- **Embed inside an existing app.** Call `A2cuiSampleApp()` directly from any
  `setContent { ... }` block — it's a plain `@Composable`.

## See also

- Root: [`../README.md`](../README.md)
- Desktop host: [`:a2cui-sample`](../a2cui-sample/README.md)
- iOS host: [`:a2cui-sample-ios`](../a2cui-sample-ios/README.md)
- Shared composable: [`:a2cui-sample-shared`](../a2cui-sample-shared/README.md)
