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
        // AI FreeGrip（SmartGrip Kit）SDK 的官方 Maven 仓。
        // 因为 FAIL_ON_PROJECT_REPOS，仓库只能声明在这里。
        maven { url = uri("https://developer.honor.com/repo") }
    }
}

rootProject.name = "AI-Freegrip"
include(":app")
