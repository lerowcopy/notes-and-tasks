import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localBuildConfigValue(name: String): String = localProperties
    .getProperty(name)
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "ru.avito.notesandtasks.core.voice"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "SALUTE_SPEECH_AUTH_KEY",
            "\"${localBuildConfigValue("SALUTE_SPEECH_AUTH_KEY")}\"",
        )
        buildConfigField(
            "String",
            "SALUTE_SPEECH_SCOPE",
            "\"${localBuildConfigValue("SALUTE_SPEECH_SCOPE")}\"",
        )
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
