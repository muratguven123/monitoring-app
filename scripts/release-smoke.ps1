# Release APK smoke: assemble, install, launch, fail on AndroidRuntime crashes.
# Usage (from repo root):  .\scripts\release-smoke.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "adb not found at $adb. Set ANDROID_HOME or install platform-tools."
}

$devices = & $adb devices | Select-String "\tdevice$"
if (-not $devices) {
    throw "No device/emulator in 'device' state. Start an emulator or plug in a phone."
}

Write-Host "Assembling release APK..."
if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleRelease --stacktrace
} else {
    & ./gradlew assembleRelease --stacktrace
}
if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed" }

$apk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

$pkg = "com.monitoring.dashboard"
Write-Host "Installing $apk"
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }

Write-Host "Clearing logcat and launching MainActivity"
& $adb logcat -c
& $adb shell am start -n "$pkg/.MainActivity"
Start-Sleep -Seconds 8

$crashLog = & $adb logcat -d -s AndroidRuntime:E *:F | Out-String
if ($crashLog -match "FATAL EXCEPTION|AndroidRuntime") {
    Write-Host $crashLog
    throw "Release smoke failed: AndroidRuntime crash in logcat"
}

Write-Host "Release smoke passed (no AndroidRuntime:E in the first 8s)."
Write-Host ("APK size: {0:N2} MB" -f ((Get-Item $apk).Length / 1MB))
$mapping = Join-Path $repoRoot "app\build\outputs\mapping\release\mapping.txt"
if (Test-Path $mapping) {
    Write-Host ("mapping.txt: {0:N2} MB" -f ((Get-Item $mapping).Length / 1MB))
}
