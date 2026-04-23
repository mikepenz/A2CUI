rootProject.name = "a2cui"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("baseLibs") {
            from("com.mikepenz:version-catalog:0.14.4")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":a2cui-core")
include(":a2cui-transport")
include(":a2cui-compose")
include(":a2cui-agui")
include(":a2cui-actions")
include(":a2cui-codegen-annotations")
include(":a2cui-codegen")
include(":a2cui-adaptive-cards")
include(":a2cui-sample")
include(":a2cui-sample-shared")
include(":a2cui-sample-android")
include(":a2cui-sample-ios")
