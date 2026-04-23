# `:a2cui-sample-ios` — iOS framework host

Kotlin Multiplatform module that produces a static iOS framework
(`A2cuiSampleIos.framework`) exposing a single entry point:
`SampleViewController()` — a `ComposeUIViewController` that renders
`A2cuiSampleApp` from [`:a2cui-sample-shared`](../a2cui-sample-shared).

**This module does not ship a runnable iOS app.** It emits a framework; you
consume it from a host Xcode project (SwiftUI or UIKit).

## How to build the framework

Pick the target that matches your simulator / device:

```bash
# Apple-silicon simulator (most common on M-series Macs)
./gradlew :a2cui-sample-ios:linkDebugFrameworkIosSimulatorArm64

# Intel simulator
./gradlew :a2cui-sample-ios:linkDebugFrameworkIosX64

# Physical device
./gradlew :a2cui-sample-ios:linkDebugFrameworkIosArm64
```

Output:

```
a2cui-sample-ios/build/bin/iosSimulatorArm64/debugFramework/A2cuiSampleIos.framework
```

For release builds swap `linkDebugFramework...` → `linkReleaseFramework...`.

## Consuming the framework from Xcode

1. In your Xcode project: **Target → General → Frameworks, Libraries, and
   Embedded Content → +** → *Add Other… → Add Files* → pick
   `A2cuiSampleIos.framework` from the path above. Set *Embed & Sign*.
2. Add the parent directory to **Build Settings → Framework Search Paths**.
3. In Swift, import and wrap the controller:

```swift
import SwiftUI
import A2cuiSampleIos

struct A2cuiSampleView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Kotlin top-level `fun SampleViewController()` is exposed as the
        // static accessor on the generated `SampleViewControllerKt` class.
        SampleViewControllerKt.sampleViewController()
    }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

@main
struct DemoApp: App {
    var body: some Scene {
        WindowGroup { A2cuiSampleView().ignoresSafeArea() }
    }
}
```

From UIKit, just present the controller:

```swift
let vc = SampleViewControllerKt.sampleViewController()
window?.rootViewController = vc
```

For a production setup prefer **CocoaPods / SPM / XCFrameworks** rather than a
raw framework path — see the Compose Multiplatform iOS integration guide:
<https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-ios-integration.html>

## What you'll see

The same booking-form demo as the other hosts: a rendered A2UI surface driven
by `FakeTransport` + the scripted frames in `SampleFrames.kt`, with an outbound
events log underneath. Dark Material theme.

## Key files

| Concept | Path |
|---|---|
| iOS entry point | [`src/iosMain/kotlin/.../ios/SampleViewController.kt`](src/iosMain/kotlin/dev/mikepenz/a2cui/sample/ios/SampleViewController.kt) |
| Shared Compose body | [`:a2cui-sample-shared` — `SampleApp.kt`](../a2cui-sample-shared/src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleApp.kt) |
| Scripted A2UI frames | [`:a2cui-sample-shared` — `SampleFrames.kt`](../a2cui-sample-shared/src/commonMain/kotlin/dev/mikepenz/a2cui/sample/shared/SampleFrames.kt) |
| Framework declaration | [`build.gradle.kts`](build.gradle.kts) (`baseName = "A2cuiSampleIos"`, `isStatic = true`) |

## How to adapt

- **Replace the root composable.** Fork `SampleViewController` and call your
  own `@Composable` in place of `A2cuiSampleApp()`.
- **Expose additional entry points.** Add more `ComposeUIViewController { ... }`
  top-level functions — each appears in the Obj-C header under
  `<FileNameKt>.<functionName>()`.
- **Wire a live agent.** Swap `FakeTransport` for `SseTransport` in the shared
  module — mirrors exactly what [`:a2cui-sample-live`](../a2cui-sample-live)
  does on Desktop.

## See also

- Root: [`../README.md`](../README.md)
- Desktop host: [`:a2cui-sample`](../a2cui-sample/README.md)
- Android host: [`:a2cui-sample-android`](../a2cui-sample-android/README.md)
- Shared composable: [`:a2cui-sample-shared`](../a2cui-sample-shared/README.md)
- Compose Multiplatform iOS integration:
  <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-ios-integration.html>
