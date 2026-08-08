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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MonitoringDashboard"
include(":app")

// google-services plugin requires app/google-services.json before :app configures.
val googleServicesJson = file("app/google-services.json")
if (!googleServicesJson.exists()) {
    val placeholder = file("app/ci/google-services.json")
    if (placeholder.exists()) {
        placeholder.copyTo(googleServicesJson)
        println("Copied app/ci/google-services.json → app/google-services.json (placeholder)")
    }
}
