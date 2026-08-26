plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.avito.notesandtasks"

    defaultConfig {
        applicationId = "ru.avito.notesandtasks"
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:gigachat"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:permissions"))
    implementation(project(":core:ui"))
    implementation(project(":core:voice"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:tasks"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.room.runtime)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlin.metadata.jvm)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
