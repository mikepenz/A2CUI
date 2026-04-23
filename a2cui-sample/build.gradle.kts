plugins {
    alias(baseLibs.plugins.kotlinJvm)
    alias(baseLibs.plugins.composeMultiplatform)
    alias(baseLibs.plugins.composeCompiler)
    application
}

application {
    mainClass = "dev.mikepenz.a2cui.sample.MainKt"
}

dependencies {
    implementation(projects.a2cuiSampleShared)
    implementation(projects.a2cuiAgui)
    implementation(projects.a2cuiActions)

    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    implementation(baseLibs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)
}
