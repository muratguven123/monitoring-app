plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

android {
    namespace = "com.monitoring.dashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.monitoring.dashboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.monitoring.dashboard.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Release signing config – override via environment variables in CI:
        //   KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
        // Or create a local keystore.properties file (do NOT commit it to VCS).
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
                ?: project.findProperty("keystorePath") as String?
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: project.findProperty("keystorePassword") as String?
            val keyAlias = System.getenv("KEY_ALIAS")
                ?: project.findProperty("keyAlias") as String?
            val keyPassword = System.getenv("KEY_PASSWORD")
                ?: project.findProperty("keyPassword") as String?

            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Debug URLs point to local emulator (http://10.0.2.2 = host machine)
            buildConfigField("String", "GRAFANA_BASE_URL", "\"http://10.0.2.2:3000\"")
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

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
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
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kapt {
    // Room schema export for migration testing
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
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
