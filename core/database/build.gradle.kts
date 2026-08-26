plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "ru.avito.notesandtasks.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    ksp(libs.room.compiler)
}
