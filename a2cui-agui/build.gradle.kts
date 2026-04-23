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
        namespace = "dev.mikepenz.a2cui.agui"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            api(projects.a2cuiTransport)
            // AG-UI protocol is implemented in-house on top of :a2cui-transport (Ktor SSE/WS).
            // No external com.agui:kotlin-client dependency — the event schema is a sealed
            // interface mirrored from docs.ag-ui.com, decoded via kotlinx-serialization.
            implementation(libs.kotlinx.serialization.json)
            implementation(baseLibs.kotlinx.coroutines.core)
            implementation(compose.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
