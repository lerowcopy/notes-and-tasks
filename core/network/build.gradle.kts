plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.avito.notesandtasks.core.network"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.kotlinx.serialization.json)

    implementation(libs.okhttp.logging.interceptor)
}
