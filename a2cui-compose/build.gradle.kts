plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.compose")
    id("com.mikepenz.convention.publishing")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    android {
        namespace = "dev.mikepenz.a2cui.compose"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            api(projects.a2cuiTransport)
            api(libs.kotlinx.serialization.json)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}

// Coil 3's JS artifact references `window` at module init, so loading the compiled
// test bundle under Node (mocha requires it eagerly) throws before any test runs.
tasks.matching { it.name == "jsNodeTest" || it.name == "wasmJsNodeTest" }.configureEach { enabled = false }
