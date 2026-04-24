plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.compose")
    id("com.mikepenz.convention.publishing")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    android {
        namespace = "dev.mikepenz.a2cui.actions"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            api(projects.a2cuiCompose)
            api(projects.a2cuiAgui)
            implementation(compose.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(baseLibs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// Transitively pulls Coil 3, whose JS artifact touches `window` at module init —
// that throws inside mocha's eager require, so the Node test runner can't load the bundle.
tasks.matching { it.name == "jsNodeTest" }.configureEach { enabled = false }
