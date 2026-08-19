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

// ---------------------------------------------------------------------------
// google-services.json resolution
// ---------------------------------------------------------------------------
// The google-services plugin needs app/google-services.json to exist before the
// :app project is configured, so this has to happen here in settings.
//
// The CI placeholder (app/ci/google-services.json) points at a fake Firebase
// project and MUST NOT end up in a build that is distributed to users —
// Crashlytics would silently report to nowhere. It is therefore only copied
// when running on CI. Locally, a missing file fails fast with instructions.
//
// See app/build.gradle.kts :verifyFirebaseConfig for the release-time guard.
val googleServicesJson = file("app/google-services.json")
if (!googleServicesJson.exists()) {
    val isCi = System.getenv("CI")?.isNotBlank() == true
    val placeholder = file("app/ci/google-services.json")

    if (isCi && placeholder.exists()) {
        placeholder.copyTo(googleServicesJson)
        println("[CI] Copied app/ci/google-services.json → app/google-services.json (placeholder — debug/smoke builds only)")
    } else {
        throw GradleException(
            """
            |
            |Missing app/google-services.json
            |
            |Download it from the Firebase console for the Android app with
            |package name "com.monitoring.dashboard" and place it at:
            |
            |    app/google-services.json
            |
            |This file is gitignored and must never be committed.
            |See app/google-services.json.example for the expected shape.
            |
            |(CI builds fall back to the placeholder in app/ci/ automatically.
            | If you are on CI, set the CI environment variable.)
            |
            """.trimMargin()
        )
    }
}
