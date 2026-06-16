pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CoolPerseus"

include(":keyhandler")

include(":coolperseus")

include(":coolperseus:shared:design")
include(":coolperseus:shared:data")

include(":coolperseus:feat:overlay")
include(":coolperseus:feat:sound_selection")
include(":coolperseus:feat:action_selection")
include(":coolperseus:feat:widgets")

