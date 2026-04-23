plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.publishing")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    android {
        namespace = "dev.mikepenz.a2cui.adaptivecards"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiCore)
            implementation(libs.kotlinx.serialization.json)
            implementation(baseLibs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
