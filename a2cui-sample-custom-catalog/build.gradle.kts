plugins {
    alias(baseLibs.plugins.kotlinJvm)
    alias(baseLibs.plugins.composeMultiplatform)
    alias(baseLibs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    application
}

application {
    mainClass = "dev.mikepenz.a2cui.sample.custom.MainKt"
}

dependencies {
    implementation(projects.a2cuiCompose)
    implementation(project(":a2cui-codegen-annotations"))
    ksp(project(":a2cui-codegen"))

    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    implementation(baseLibs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit5)
}

ksp {
    arg("a2cui.catalogId", "custom-demo")
    arg("a2cui.catalogPackage", "dev.mikepenz.a2cui.sample.custom")
    arg("a2cui.catalogClassName", "CustomDemoCatalog")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
