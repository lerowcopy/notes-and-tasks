pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NotesAndTasks"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:navigation")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:permissions")
include(":core:media")
include(":core:voice")
include(":core:gigachat")
include(":feature:notes")
include(":feature:tasks")
include(":feature:settings")
