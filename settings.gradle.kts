pluginManagement {
    includeBuild("build-logic")
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Offhand"
include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:device")
include(":core:audio")
include(":core:ai-api")
include(":core:ai-local")
include(":core:security")
include(":core:data")
include(":feature:onboarding")
include(":feature:recording")
include(":feature:notes")
include(":feature:settings")
