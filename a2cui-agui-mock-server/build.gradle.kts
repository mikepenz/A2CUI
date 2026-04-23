plugins {
    alias(baseLibs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(baseLibs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit5)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(projects.a2cuiTransport)
    testImplementation(projects.a2cuiAgui)
    testImplementation(projects.a2cuiCore)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
