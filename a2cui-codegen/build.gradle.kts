plugins {
    alias(baseLibs.plugins.kotlinJvm)
    alias(baseLibs.plugins.mavenPublish)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinx.serialization.json)
    // Annotations live in a KMP sibling; processor pulls the JVM artifact so KSClassDeclaration
    // comparisons against the annotation FQN resolve against real runtime types.
    compileOnly(project(":a2cui-codegen-annotations"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit5)
    testImplementation(project(":a2cui-codegen-annotations"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xopt-in=kotlin.RequiresOptIn", "-Xallow-experimental-api")
    }
}
