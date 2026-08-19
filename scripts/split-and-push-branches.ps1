# Split unpushed work into stacked topic branches and push to origin.
# Run from repo root: .\scripts\split-and-push-branches.ps1
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

$base = "origin/main"
$workBranch = "release/work-split"

function Step-Commit {
    param(
        [string]$Branch,
        [string]$Message,
        [string[]]$Add,
        [string[]]$Remove = @()
    )
    foreach ($r in $Remove) {
        if (Test-Path $r) { git rm -r --quiet -- $r 2>$null }
        elseif (git ls-files --error-unmatch -- $r 2>$null) { git rm --quiet -- $r }
    }
    if ($Add.Count -gt 0) { git add -- @Add }
    $pending = git diff --cached --name-only
    if (-not $pending) {
        Write-Host "SKIP (nothing staged): $Branch"
        return
    }
    git commit -m $Message
    git branch -f $Branch
    Write-Host "OK $Branch -> $(git rev-parse --short HEAD)"
}

# Start from main tip (all 6 commits), flatten to working tree
git checkout -B $workBranch main | Out-Null
git reset --soft $base | Out-Null
git reset HEAD | Out-Null

Write-Host "Working tree reset to $base - building stacked branches..."

Step-Commit "release/01-ci" "ci: add Dependabot and instrumented test job on emulator" @(
    ".github/dependabot.yml",
    ".github/workflows/ci.yml"
)

Step-Commit "release/02-release-workflow" "ci: add release workflow with signed APK and mapping artifact" @(
    ".github/workflows/release.yml"
)

Step-Commit "release/03-build-guards-signing" "build: add Firebase/signing guards and external credential lookup" @(
    "settings.gradle.kts",
    "app/build.gradle.kts",
    "keystore.properties.example",
    "gradle.properties",
    "gradle/gradle-daemon-jvm.properties",
    "gradle/wrapper/gradle-wrapper.properties"
)

Step-Commit "release/04-docs" "docs: add production readiness and release signing guides" @(
    "PRODUCTION_READINESS.md",
    "RELEASE_SIGNING.md",
    "README.md"
)

Step-Commit "release/05-proguard" "build: narrow Compose ProGuard keep rule" @(
    "app/proguard-rules.pro",
    ".gitignore"
)

Step-Commit "release/06-room-schema-v1" "refactor: reset Room schema to version 1" @(
    "app/src/main/java/com/monitoring/dashboard/data/local/MonitoringDatabase.kt",
    "app/src/main/java/com/monitoring/dashboard/di/DatabaseModule.kt",
    "app/schemas/com.monitoring.dashboard.data.local.MonitoringDatabase/1.json"
) -Remove @(
    "app/src/main/java/com/monitoring/dashboard/data/local/MonitoringMigrations.kt",
    "app/schemas/com.monitoring.dashboard.data.local.MonitoringDatabase/2.json",
    "app/src/test/java/com/monitoring/dashboard/data/local/MonitoringMigrationsTest.kt"
)

Step-Commit "release/07-grafana-url-model" "feat: add GrafanaServerUrl parsing and validation" @(
    "app/src/main/java/com/monitoring/dashboard/domain/model/GrafanaServerUrl.kt",
    "app/src/test/java/com/monitoring/dashboard/domain/model/GrafanaServerUrlTest.kt"
)

Step-Commit "release/08-grafana-interceptor" "feat: rewrite DynamicBaseUrlInterceptor for path prefix support" @(
    "app/src/main/java/com/monitoring/dashboard/data/remote/GrafanaBaseUrlProvider.kt",
    "app/src/main/java/com/monitoring/dashboard/data/remote/interceptor/DynamicBaseUrlInterceptor.kt",
    "app/src/main/java/com/monitoring/dashboard/data/repository/GrafanaRepositoryImpl.kt",
    "app/src/main/java/com/monitoring/dashboard/di/NetworkModule.kt",
    "app/src/test/java/com/monitoring/dashboard/data/remote/interceptor/DynamicBaseUrlInterceptorTest.kt"
)

Step-Commit "release/09-grafana-ui" "feat: Grafana unconfigured state and Settings URL validation" @(
    "app/src/main/java/com/monitoring/dashboard/ui/screens/home/HomeScreen.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/home/HomeViewModel.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/grafana/GrafanaDashboardDetailViewModel.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/grafana/GrafanaPanelDetailViewModel.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/newrelic/NewRelicAppDetailScreen.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/settings/SettingsScreen.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/settings/SettingsViewModel.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/onboarding/OnboardingScreen.kt"
)

Step-Commit "release/10-grafana-vm-tests" "test: add Grafana ViewModel unit tests" @(
    "app/src/test/java/com/monitoring/dashboard/ui/screens/grafana/GrafanaDashboardDetailViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/ui/screens/grafana/GrafanaDashboardsViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/ui/screens/grafana/GrafanaPanelDetailViewModelTest.kt"
)

Step-Commit "release/11-newrelic-vm-tests" "test: add New Relic ViewModel unit tests" @(
    "app/src/test/java/com/monitoring/dashboard/ui/screens/newrelic/NewRelicAppDetailViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/ui/screens/newrelic/NewRelicAppsViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/ui/screens/newrelic/NewRelicMetricDetailViewModelTest.kt"
)

Step-Commit "release/12-alerts-settings-tests" "test: add Alerts and Settings ViewModel tests" @(
    "app/src/test/java/com/monitoring/dashboard/ui/screens/alerts/AlertsViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/ui/screens/settings/SettingsViewModelTest.kt"
)

Step-Commit "release/13-test-fixes" "test: fix HomeViewModel hang and ShouldNotifyViolation mock" @(
    "app/src/test/java/com/monitoring/dashboard/ui/screens/home/HomeViewModelTest.kt",
    "app/src/test/java/com/monitoring/dashboard/domain/usecase/ShouldNotifyViolationUseCaseTest.kt"
)

Step-Commit "release/14-i18n" "i18n: complete Turkish translations" @(
    "app/src/main/res/values/strings.xml",
    "app/src/main/res/values-tr/strings.xml"
)

Step-Commit "release/15-repo-fakes" "test: extract RepositoryModule and Hilt test fakes" @(
    "app/src/main/java/com/monitoring/dashboard/di/RepositoryModule.kt",
    "app/src/androidTest/java/com/monitoring/dashboard/di/FakeRepositoryModule.kt",
    "app/src/androidTest/java/com/monitoring/dashboard/fake/FakeGrafanaRepository.kt",
    "app/src/androidTest/java/com/monitoring/dashboard/fake/FakeNewRelicRepository.kt",
    "app/src/main/java/com/monitoring/dashboard/data/local/SecurePreferencesManager.kt"
)

Step-Commit "release/16-ui-flow-tests" "test: add CriticalFlows Compose UI tests" @(
    "app/src/androidTest/java/com/monitoring/dashboard/ui/BaseHiltComposeTest.kt",
    "app/src/androidTest/java/com/monitoring/dashboard/ui/CriticalFlowsTest.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/TestTags.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/MainScreen.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/navigation/AppNavGraph.kt",
    "app/src/main/java/com/monitoring/dashboard/ui/screens/lock/AppLockScreen.kt",
    "app/src/main/java/com/monitoring/dashboard/notification/AlertNotificationHelper.kt"
)

Step-Commit "release/17-instrumented-infra" "test: wire instrumented test infra and release smoke script" @(
    "app/src/androidTest/java/com/monitoring/dashboard/HiltTestRunner.kt",
    "app/proguard-androidTest-rules.pro",
    "app/src/androidTest/AndroidManifest.xml",
    "gradle/libs.versions.toml",
    "scripts/release-smoke.ps1",
    "scripts/connected-minified-release-test.ps1"
)

Step-Commit "release/18-newrelic-region" "feat: add New Relic EU/US/JP region selection in Settings" @(
    "app/src/main/java/com/monitoring/dashboard/domain/model/NewRelicRegion.kt",
    "app/src/main/java/com/monitoring/dashboard/data/remote/interceptor/NewRelicRegionInterceptor.kt",
    "app/src/main/java/com/monitoring/dashboard/data/repository/NerdGraphRepository.kt",
    "app/src/test/java/com/monitoring/dashboard/data/remote/interceptor/NewRelicRegionInterceptorTest.kt"
)

# Anything left?
$left = git status --porcelain
if ($left) {
    Write-Host "WARNING: uncommitted files remain:"
    Write-Host $left
    git add -A
    git commit -m "chore: include remaining split files"
    git branch -f release/19-remaining
}

Write-Host "`nPushing branches to origin..."
$branches = git branch --list "release/*" | ForEach-Object { $_.Trim().TrimStart("* ").Trim() }
foreach ($b in $branches) {
    Write-Host "git push -u origin $b"
    git push -u origin $b
}

Write-Host "`nDone. Branch tips:"
foreach ($b in $branches) {
    $sha = git rev-parse --short $b
    Write-Host ("  {0} -> {1}" -f $b, $sha)
}
