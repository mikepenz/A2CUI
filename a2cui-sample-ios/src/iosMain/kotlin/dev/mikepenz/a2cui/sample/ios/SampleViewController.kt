package dev.mikepenz.a2cui.sample.ios

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.window.ComposeUIViewController
import dev.mikepenz.a2cui.sample.shared.A2cuiSampleApp
import platform.UIKit.UIViewController

/**
 * Entry point for the iOS sample host. A Swift UIKit app calls [SampleViewController] from its
 * `AppDelegate` (or a SwiftUI `UIViewControllerRepresentable`) to embed the shared Compose
 * surface. The function name is exposed through the generated Objective-C header as
 * `SampleViewControllerKt.sampleViewController()`.
 */
public fun SampleViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme(colorScheme = darkColorScheme()) {
        A2cuiSampleApp()
    }
}
