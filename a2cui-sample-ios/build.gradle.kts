plugins {
    id("com.mikepenz.convention.kotlin-multiplatform")
    id("com.mikepenz.convention.compose")
}

kotlin {
    android {
        namespace = "dev.mikepenz.a2cui.sample.ios"
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "A2cuiSampleIos"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.a2cuiSampleShared)
            implementation(compose.runtime)
            implementation(compose.ui)
        }
    }
}
