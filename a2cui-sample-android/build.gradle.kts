plugins {
    id("com.mikepenz.convention.android-application")
    id("com.mikepenz.convention.compose")
}

android {
    namespace = "dev.mikepenz.a2cui.sample.android"

    defaultConfig {
        applicationId = "dev.mikepenz.a2cui.sample.android"
        minSdk = 23
        versionCode = 1
        versionName = "0.1.0-a02"
    }
}

dependencies {
    implementation(projects.a2cuiSampleShared)

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
}
