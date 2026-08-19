import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    kotlin("kapt")
}

// =============================================================================
// Release signing credentials
// =============================================================================
// Resolution order (first hit wins):
//   1. KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD env vars   ← CI
//   2. A properties file pointed at by KEYSTORE_PROPERTIES_FILE                ← flexible
//   3. ~/.gradle/gradle.properties (Gradle user home, outside the repo)        ← recommended for devs
//   4. <repo>/keystore.properties                                             ← legacy, discouraged
//
// (4) is still honoured so existing setups keep working, but it puts a signing
// password in plaintext inside the project directory where it can leak through
// backups, cloud sync or a mis-scoped archive. The build warns when it is used.
// See RELEASE_SIGNING.md.
// Note: do not use java.util.* via `java.` — in Gradle Kotlin DSL `java` is the Java extension.

val legacyKeystoreFile = rootProject.file("keystore.properties")
val externalKeystoreFile = System.getenv("KEYSTORE_PROPERTIES_FILE")
    ?.takeIf { it.isNotBlank() }
    ?.let { File(it) }
// OS user home, not GRADLE_USER_HOME — daemons/CI can redirect the latter.
val userHomeGradlePropsFile =
    File(System.getProperty("user.home"), ".gradle/gradle.properties")

val keystoreProps = Properties().apply {
    // Lowest priority first so a later file overwrites.
    sequenceOf(legacyKeystoreFile, userHomeGradlePropsFile, externalKeystoreFile)
        .mapNotNull { it }
        .filter { it.exists() }
        .forEach { file -> file.inputStream().use { load(it) } }
}

fun File.hasSigningStoreFile(): Boolean {
    if (!exists()) return false
    val props = Properties().apply { inputStream().use { load(it) } }
    return sequenceOf("storeFile", "keystorePath")
        .any { !props.getProperty(it).isNullOrBlank() }
}

val usingLegacyKeystoreFile =
    legacyKeystoreFile.exists() &&
        externalKeystoreFile?.hasSigningStoreFile() != true &&
        !userHomeGradlePropsFile.hasSigningStoreFile()

fun keystoreProp(env: String, vararg keys: String): String? {
    System.getenv(env)?.takeIf { it.isNotBlank() }?.let { return it }
    for (key in keys) {
        keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
        (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}

val releaseStoreFileProp = keystoreProp("KEYSTORE_PATH", "storeFile", "keystorePath")
val releaseSigningConfigured = releaseStoreFileProp != null

// =============================================================================
// Versioning
// =============================================================================
// CI supplies VERSION_CODE (monotonic build number) and VERSION_NAME (git tag).
// Local builds fall back to the baseline so a developer build is always possible.
// A versionCode that does not increase means devices silently refuse the update,
// so this must never be hand-edited for a real release.
val appVersionCode = System.getenv("VERSION_CODE")?.trim()?.toIntOrNull() ?: 1
val appVersionName = System.getenv("VERSION_NAME")?.trim()
    ?.removePrefix("v")
    ?.takeIf { it.isNotEmpty() }
    ?: "1.0.0"

android {
    namespace = "com.monitoring.dashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.monitoring.dashboard"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "com.monitoring.dashboard.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storeFileProp = keystoreProp("KEYSTORE_PATH", "storeFile", "keystorePath")
            val storePasswordValue = keystoreProp("KEYSTORE_PASSWORD", "storePassword", "keystorePassword")
            val keyAliasValue = keystoreProp("KEY_ALIAS", "keyAlias")
            val keyPasswordValue = keystoreProp("KEY_PASSWORD", "keyPassword")

            if (storeFileProp != null) {
                // Absolute paths as-is; relative paths resolve against the app/ module.
                val resolved = file(storeFileProp)
                storeFile = if (resolved.isAbsolute) resolved else file(storeFileProp)
                storePassword = storePasswordValue
                this.keyAlias = keyAliasValue
                this.keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Grafana has no compile-time default: the user must set the server
            // in Settings, same as release. A baked-in emulator URL made the
            // "not configured" Home state unreachable on debug builds.
            buildConfigField("String", "GRAFANA_BASE_URL", "\"\"")
            buildConfigField("String", "NEWRELIC_BASE_URL", "\"http://10.0.2.2:5000\"")
            buildConfigField("String", "NEWRELIC_NERDGRAPH_URL", "\"http://10.0.2.2:5000/graphql\"")
            buildConfigField("String", "GITHUB_BASE_URL", "\"https://api.github.com\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production URLs — Grafana base URL is overridable at runtime via Settings.
            // New Relic always uses the official API endpoint.
            buildConfigField("String", "GRAFANA_BASE_URL", "\"\"")   // empty → user sets via Settings
            buildConfigField("String", "NEWRELIC_BASE_URL", "\"https://api.newrelic.com\"")
            buildConfigField("String", "NEWRELIC_NERDGRAPH_URL", "\"https://api.newrelic.com/graphql\"")
            buildConfigField("String", "GITHUB_BASE_URL", "\"https://api.github.com\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Release builds must not ship with lint errors.
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        checkDependencies = true

        // Missing translations are a release blocker for a bilingual app.
        fatal += listOf("MissingTranslation")

        // Accessibility and correctness issues we want surfaced but not yet
        // build-breaking. Promote these to `error` once the reports are clean.
        warning += listOf(
            "ContentDescription",
            "HardcodedText",
            "UnusedResources",
        )

        // Only apply a baseline once one has been generated, otherwise the first
        // lint run fails just to write the file.
        val lintBaseline = file("lint-baseline.xml")
        if (lintBaseline.exists()) {
            baseline = lintBaseline
        }

        htmlReport = true
        xmlReport = true
        sarifReport = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}

kapt {
    // Required by Hilt; without this, kapt can emit incomplete factories
    // (e.g. AssistedFactory_Impl without the matching *_Factory class).
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// =============================================================================
// Release guards
// =============================================================================
// These run before any release build and turn two silent, expensive failures
// into loud build errors:
//   1. Shipping the CI placeholder Firebase config → zero crash reports.
//   2. Shipping an unsigned release APK → cannot be installed or updated.

// Escape hatch for the CI "does R8 still work" smoke build, which has no access
// to the real Firebase project. Must never be set for a build that is handed to
// a user — the artifact would have no crash reporting.
val allowPlaceholderFirebase =
    (project.findProperty("allowPlaceholderFirebase") as String?)?.toBoolean() == true

val verifyFirebaseConfig = tasks.register("verifyFirebaseConfig") {
    group = "verification"
    description = "Fails the build if app/google-services.json is the CI placeholder."

    // Captured as locals so the task action holds no reference to the build
    // script object — required for the configuration cache, which is enabled.
    val configFile = layout.projectDirectory.file("google-services.json").asFile
    val allowPlaceholder = allowPlaceholderFirebase
    val placeholderMarkers = listOf("PLACEHOLDER", "monitoring-dashboard-ci", "000000000000")
    outputs.upToDateWhen { false }

    doLast {
        if (allowPlaceholder) {
            logger.warn(
                "WARNING: -PallowPlaceholderFirebase=true — this build has NO crash reporting " +
                    "and must not be distributed."
            )
            return@doLast
        }

        if (!configFile.exists()) {
            throw GradleException(
                "Missing app/google-services.json — download it from the Firebase console."
            )
        }

        val text = configFile.readText()
        val marker = placeholderMarkers.firstOrNull { text.contains(it) }
        if (marker != null) {
            throw GradleException(
                """
                |
                |Refusing to build a release with a placeholder Firebase configuration.
                |
                |app/google-services.json contains "$marker", which means it is the CI
                |placeholder (app/ci/google-services.json), not a real Firebase project.
                |Crashlytics would report to nowhere and production crashes would be invisible.
                |
                |Fix: download google-services.json from the Firebase console for the
                |Android app with package name "com.monitoring.dashboard" and place it at
                |app/google-services.json.
                |
                """.trimMargin()
            )
        }
    }
}

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Fails the build if release signing credentials are not configured."

    val configured = releaseSigningConfigured
    val legacyFileInUse = usingLegacyKeystoreFile
    outputs.upToDateWhen { false }

    doLast {
        if (!configured) {
            throw GradleException(
                """
                |
                |Release signing is not configured — the build would produce an unsigned APK.
                |
                |Provide credentials in one of these ways (see RELEASE_SIGNING.md):
                |  * KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD env vars
                |  * KEYSTORE_PROPERTIES_FILE pointing at a properties file outside the repo
                |  * keystorePath / keystorePassword / keyAlias / keyPassword in
                |    ~/.gradle/gradle.properties
                |
                """.trimMargin()
            )
        }

        if (legacyFileInUse) {
            logger.warn(
                """
                |
                |WARNING: signing credentials were read from <repo>/keystore.properties.
                |This keeps a signing password in plaintext inside the project directory,
                |where it can leak via backups, cloud sync or an archive of the repo.
                |Move it outside the repo — see RELEASE_SIGNING.md.
                |
                """.trimMargin()
            )
        }
    }
}

// Attach the guards to release builds.
//
// Hooking a single lifecycle task is not safe: AGP renames and removes these
// between major versions, and a guard that silently stops running is worse than
// no guard at all — it looks protected while shipping a broken artifact. This
// happened here: an earlier version hooked only `preReleaseBuild`, which no
// longer runs on this AGP, and a release APK was produced with the placeholder
// Firebase config.
//
// So: hook every task that must not run against a bad configuration, and fail
// loudly at configuration time if none of them exist.
val releaseGuardTaskNames = setOf(
    "preReleaseBuild",
    "processReleaseGoogleServices", // reads google-services.json
    "minifyReleaseWithR8",
    "packageRelease", // produces the APK
    "bundleRelease", // produces the AAB
)

tasks.matching { it.name in releaseGuardTaskNames }.configureEach {
    dependsOn(verifyFirebaseConfig, verifyReleaseSigning)
}

// Self-check: if AGP renames these tasks again, say so at build time instead of
// letting the guards quietly stop protecting anything.
// (Deliberately not gradle.taskGraph.whenReady — that is not compatible with the
// configuration cache, which this project enables.)
afterEvaluate {
    val matched = tasks.names.filter { it in releaseGuardTaskNames }
    if (matched.none { it.contains("Release") }) {
        logger.warn(
            "WARNING: no release task matched releaseGuardTaskNames in app/build.gradle.kts. " +
                "The Firebase/signing guards are NOT protecting release builds. " +
                "Found instead: " + tasks.names.filter { it.contains("Release") }.take(20)
        )
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.splash.screen)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // Network (Retrofit + OkHttp) — Grafana, New Relic, GitHub API
    implementation(libs.bundles.network)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Room (Offline Cache)
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)

    // WorkManager (Background Sync & Alert Notifications)
    implementation(libs.work.runtime)

    // Security (EncryptedSharedPreferences — API Key Storage)
    implementation(libs.security.crypto)

    // DataStore (User Preferences — Theme, Refresh Interval)
    implementation(libs.datastore.preferences)

    // Biometric lock + FragmentActivity host
    implementation(libs.biometric)
    implementation(libs.fragment.ktx)

    // Home screen widget
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Charts (Vico — Compose Native Dashboard Charts)
    implementation(libs.bundles.vico)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // Timber (Logging)
    implementation(libs.timber)

    // Firebase Crashlytics (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Accompanist (Pull-to-Refresh, System UI)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.accompanist.systemuicontroller)

    // Unit Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.work.testing)

    // Instrumented Testing
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.coroutines.test)
    kaptAndroidTest(libs.hilt.compiler)
}
