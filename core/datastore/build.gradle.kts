plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.avito.notesandtasks.core.datastore"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore.preferences)
}
