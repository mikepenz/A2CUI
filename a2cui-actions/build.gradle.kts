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
    }
}
