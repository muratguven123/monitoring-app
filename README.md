# Monitoring Dashboard

A native Android application built with **Jetpack Compose** that provides a unified monitoring dashboard. It aggregates data from **Grafana** and **New Relic** into a single mobile interface, allowing you to monitor infrastructure health, application performance, and alerts on the go.

## Features

- **Grafana Integration** — Browse dashboards, view panel details, and check datasource health
- **New Relic Integration** — Monitor application performance, view metrics (response time, throughput, error rate), and track alerts
- **Home Dashboard** — Unified overview combining status from all connected services
- **Settings** — Configure API keys, base URLs, theme preferences, and refresh intervals
- **Offline Support** — Local caching with Room database for offline access
- **Secure Storage** — API keys stored via `EncryptedSharedPreferences`
- **Background Sync** — WorkManager-based background data synchronization and alert notifications
- **Material 3 UI** — Modern Compose UI with dynamic theming, pull-to-refresh, and native charts (Vico)

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Local DB | Room |
| Async | Kotlin Coroutines + Flow |
| Charts | Vico |
| Images | Coil |
| Navigation | Jetpack Navigation Compose |
| Background Work | WorkManager |
| Security | AndroidX Security Crypto |
| Preferences | DataStore |
| Logging | Timber |
| Build System | Gradle 8.5 (Kotlin DSL) + Version Catalog |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Project Structure

```
app/src/main/java/com/monitoring/dashboard/
├── MonitoringApp.kt                  # Application class (Hilt entry point)
├── MainActivity.kt                   # Single Activity host
├── HiltTestRunner.kt                 # Custom test runner for Hilt
├── data/
│   ├── local/
│   │   └── SecurePreferencesManager.kt
│   ├── remote/
│   │   ├── GrafanaApiService.kt      # Grafana REST API
│   │   ├── NewRelicApiService.kt     # New Relic REST API
│   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── GrafanaDashboardDetailDto.kt
│   │   │   ├── GrafanaDashboardSearchDto.kt
│   │   │   ├── GrafanaDatasourceDto.kt
│   │   │   ├── GrafanaHealthDto.kt
│   │   │   ├── GrafanaPanelDto.kt
│   │   │   └── newrelic/
│   │   │       ├── NewRelicAlertDto.kt
│   │   │       ├── NewRelicApplicationDto.kt
│   │   │       └── NewRelicMetricDto.kt
│   │   ├── interceptor/
│   │   │   ├── AuthInterceptor.kt
│   │   │   ├── DynamicBaseUrlInterceptor.kt
│   │   │   └── NewRelicAuthInterceptor.kt
│   │   └── util/
│   │       └── NetworkResult.kt
│   └── repository/
│       ├── GrafanaRepository.kt
│       ├── GrafanaRepositoryImpl.kt
│       ├── NewRelicRepository.kt
│       └── NewRelicRepositoryImpl.kt
├── di/
│   └── NetworkModule.kt              # Hilt network dependency graph
├── domain/
│   ├── model/
│   │   ├── Dashboard.kt
│   │   ├── DashboardDetail.kt
│   │   └── GrafanaHealth.kt
│   └── usecase/
│       ├── CheckGrafanaHealthUseCase.kt
│       ├── GetDashboardDetailUseCase.kt
│       └── GetDashboardsUseCase.kt
└── ui/
    ├── components/
    │   ├── LoadingIndicator.kt
    │   ├── MonitoringCard.kt
    │   └── ServiceStatusCard.kt
    ├── navigation/
    │   ├── NavGraph.kt
    │   └── Screen.kt
    ├── screens/
    │   ├── grafana/
    │   │   ├── GrafanaDashboardDetailScreen.kt
    │   │   ├── GrafanaDashboardDetailViewModel.kt
    │   │   ├── GrafanaDashboardsScreen.kt
    │   │   └── GrafanaDashboardsViewModel.kt
    │   ├── home/
    │   │   ├── HomeScreen.kt
    │   │   └── HomeViewModel.kt
    │   ├── newrelic/
    │   │   ├── NewRelicAppDetailScreen.kt
    │   │   ├── NewRelicAppDetailViewModel.kt
    │   │   ├── NewRelicAppsScreen.kt
    │   │   └── NewRelicAppsViewModel.kt
    │   └── settings/
    │       ├── SettingsScreen.kt
    │       └── SettingsViewModel.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Prerequisites

- **Java 17** or newer
- **Android Studio Hedgehog** (2023.1.1) or newer recommended
- Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) — see [Setup](#setup)

## Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/muratguven123/monitoring-app.git
   cd monitoring-app
   ```

2. **Gradle wrapper JAR**

   If `gradle/wrapper/gradle-wrapper.jar` is not present, either:
   - Open the project in **Android Studio** — it downloads the JAR automatically, or
   - Run a local Gradle installation: `gradle wrapper` in the project root.

3. **Build the project**

   ```bash
   # Linux / macOS
   ./gradlew build

   # Windows
   gradlew.bat build
   ```

4. **Run on a device or emulator**

   ```bash
   ./gradlew installDebug
   ```

   Or use the **Run** button in Android Studio.

## Configuration

API base URLs are configured as `BuildConfig` fields in `app/build.gradle.kts`. Update the values for your environment:

```kotlin
buildConfigField("String", "GRAFANA_BASE_URL", "\"https://your-grafana-instance.com\"")
buildConfigField("String", "NEWRELIC_BASE_URL", "\"https://api.newrelic.com\"")
buildConfigField("String", "NEWRELIC_NERDGRAPH_URL", "\"https://api.newrelic.com/graphql\"")
buildConfigField("String", "GITHUB_BASE_URL", "\"https://api.github.com\"")
```

API keys are entered at runtime through the **Settings** screen and stored securely via `EncryptedSharedPreferences`.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (with ProGuard/R8)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

## Architecture

The app follows **MVVM + Clean Architecture**:

```
UI (Compose Screens + ViewModels)
        ↓
   Domain (Use Cases + Models)
        ↓
   Data (Repositories → Remote API + Local Cache)
```

- **UI Layer** — Jetpack Compose screens observe `StateFlow` from ViewModels.
- **Domain Layer** — Use cases encapsulate business logic and are injected via Hilt.
- **Data Layer** — Repository pattern with remote (Retrofit) and local (Room) data sources. `NetworkResult` wrapper for consistent error handling.



<img width="1080" height="2400" alt="Screenshot_20260317_220031" src="https://github.com/user-attachments/assets/6f89c716-1738-4bb9-bba5-c33e071ac333" />

----------------------------------------------------------------------------------

<img width="1080" height="2400" alt="Screenshot_20260317_220018" src="https://github.com/user-attachments/assets/12a0ecb7-6214-49de-bf55-e2f08b54f940" />

----------------------------------------------------------------------------------

<img width="1080" height="2400" alt="Screenshot_20260317_220452" src="https://github.com/user-attachments/assets/6f412cff-acda-49a1-abcf-ef84e63d108c" />

----------------------------------------------------------------------------------

<img width="1080" height="2400" alt="Screenshot_20260317_224419" src="https://github.com/user-attachments/assets/6f73b061-62aa-4832-a8f2-dfaa6d4b9d9c" />

----------------------------------------------------------------------------------

<img width="1080" height="2400" alt="Screenshot_20260318_152022" src="https://github.com/user-attachments/assets/d10698bb-c045-4602-bf09-9ac508efaa06" />


----------------------------------------------------------------------------------

<img width="1080" height="2400" alt="Screenshot_20260318_151810" src="https://github.com/user-attachments/assets/866b8c09-6db1-4ad9-b9b0-5988020a0e56" />



## License

This project is provided as-is for educational and internal use.
