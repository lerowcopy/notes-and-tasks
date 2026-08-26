plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "ru.avito.notesandtasks.core.voice"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
