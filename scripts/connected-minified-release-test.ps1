# Run instrumented tests against the R8-minified release app (testBuildType = release).
# Requires: emulator/device, release signing (keystore.properties or env vars).
# Usage (repo root):  .\scripts\connected-minified-release-test.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "Building signed release APK (R8)..."
if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleRelease --max-workers=1
} else {
    & ./gradlew assembleRelease --max-workers=1
}
if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed" }

Write-Host "Running connectedReleaseAndroidTest (24 tests on minified app)..."
if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat connectedReleaseAndroidTest --max-workers=1
} else {
    & ./gradlew connectedReleaseAndroidTest --max-workers=1
}
if ($LASTEXITCODE -ne 0) { throw "connectedReleaseAndroidTest failed" }

Write-Host "Minified release instrumented tests passed."
