@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
REM ════════════════════════════════════════════════════════════════════════
REM  setup.bat — Windows için test ortamı kurulum scripti
REM  Gereksinim: Docker Desktop (çalışıyor olmalı)
REM  Kullanım  : test-env klasöründe çift tıkla veya cmd'den çalıştır
REM ════════════════════════════════════════════════════════════════════════

echo.
echo  ================================================================
echo    Monitoring Dashboard - Test Ortami Kurulumu
echo  ================================================================
echo.

REM ── Docker kurulu mu kontrol et ────────────────────────────────────────
docker info > nul 2>&1
if errorlevel 1 (
    echo  [HATA] Docker Desktop calismıyor!
    echo  Lutfen Docker Desktop'ı baslatin ve tekrar deneyin.
    echo  Indirme: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)
echo  [OK] Docker Desktop calisiyor

REM ── Servisleri baslat ──────────────────────────────────────────────────
echo.
echo  Docker servisleri baslatiliyor...
docker compose up -d --build
if errorlevel 1 (
    echo  [HATA] docker compose basarisiz oldu.
    pause
    exit /b 1
)

REM ── Grafana hazır olana kadar bekle (max 60 saniye) ─────────────────────
echo.
echo  Grafana hazir olana kadar bekleniyor...
set /a COUNTER=0
:wait_grafana
set /a COUNTER+=1
if !COUNTER! GTR 60 (
    echo  [HATA] Grafana 60 saniyede hazir olmadi.
    echo  Loglara bakin: docker compose logs grafana
    pause
    exit /b 1
)
curl -s -o nul -w "%%{http_code}" http://localhost:3000/api/health 2>nul | findstr "200" > nul
if errorlevel 1 (
    timeout /t 1 /nobreak > nul
    goto :wait_grafana
)
echo  [OK] Grafana hazir ^(deneme: !COUNTER!^)

REM ── Mock New Relic hazır olana kadar bekle ─────────────────────────────
echo  Mock New Relic hazir olana kadar bekleniyor...
set /a COUNTER2=0
:wait_newrelic
set /a COUNTER2+=1
if !COUNTER2! GTR 20 (
    echo  [UYARI] Mock New Relic yavas basliyor, devam ediliyor...
    goto :create_token
)
curl -s -o nul -w "%%{http_code}" http://localhost:5000/v2/applications.json 2>nul | findstr "200" > nul
if errorlevel 1 (
    timeout /t 1 /nobreak > nul
    goto :wait_newrelic
)
echo  [OK] Mock New Relic hazir

REM ── Grafana Service Account + Token oluştur ────────────────────────────
:create_token
echo.
echo  Grafana API token olusturuluyor...

REM Service account oluştur (zaten varsa hata yok sayılır)
curl -s -X POST "http://localhost:3000/api/serviceaccounts" ^
     -H "Content-Type: application/json" ^
     -u "admin:monitoring123" ^
     -d "{\"name\":\"monitoring-app\",\"role\":\"Viewer\"}" > nul 2>&1

REM Token oluştur ve key değerini çıkar
curl -s -X POST "http://localhost:3000/api/serviceaccounts/1/tokens" ^
     -H "Content-Type: application/json" ^
     -u "admin:monitoring123" ^
     -d "{\"name\":\"app-token-%RANDOM%\"}" > "%TEMP%\grafana_token.json" 2>&1

REM Python ile JSON'dan key'i oku (Python yoksa manuel talimat ver)
set GRAFANA_TOKEN=
python -c "import json,sys; d=json.load(open(r'%TEMP%\grafana_token.json')); print(d['key'],end='')" > "%TEMP%\token.txt" 2>nul
set /p GRAFANA_TOKEN=< "%TEMP%\token.txt"

if "!GRAFANA_TOKEN!"=="" (
    set GRAFANA_TOKEN=MANUEL_OLUSTURUN
    echo  [UYARI] Token otomatik alinamadi. Asagidaki adimlarla manuel olusturun:
    echo          1. http://localhost:3000 adresine gidin
    echo          2. Administration - Service Accounts - monitoring-app
    echo          3. "Add service account token" butonuna tiklayin
)

REM ── LAN IP bul ─────────────────────────────────────────────────────────
set LAN_IP=YOUR_PC_IP
for /f "tokens=2 delims=:" %%A in ('ipconfig ^| findstr /c:"IPv4 Adresi" /c:"IPv4 Address"') do (
    set RAW_IP=%%A
    REM Baştaki boşluğu temizle
    for /f "tokens=1" %%B in ("!RAW_IP!") do (
        set CANDIDATE=%%B
        REM 192.168 veya 10. ile başlayan ilk adresi al
        echo !CANDIDATE! | findstr /b "192.168 10\." > nul
        if not errorlevel 1 (
            set LAN_IP=!CANDIDATE!
            goto :ip_found
        )
    )
)
:ip_found

REM ── Sonuç ekranı ───────────────────────────────────────────────────────
echo.
echo  ================================================================
echo    Test ortami HAZIR!
echo  ================================================================
echo.
echo   [GRAFANA]
echo   Tarayici     : http://localhost:3000
echo   Kullanici    : admin
echo   Sifre        : monitoring123
echo.
echo   >>> Android Uygulamasi - Grafana Ayarlari <<<
echo   Base URL     : http://!LAN_IP!:3000
echo   API Key      : !GRAFANA_TOKEN!
echo.
echo   [NEW RELIC - MOCK]
echo   >>> Android Uygulamasi - New Relic Ayarlari <<<
echo   API Key      : MOCK-TEST-API-KEY-12345
echo   Account ID   : 9999999
echo.
echo   NOT: Telefon/emulator ayarlarinda localhost degil
echo        !LAN_IP! kullanin!
echo.
echo   Durdurmak icin: docker compose down
echo  ================================================================
echo.

REM ── Ayarları dosyaya kaydet ────────────────────────────────────────────
(
    echo Grafana Base URL : http://!LAN_IP!:3000
    echo Grafana API Key  : !GRAFANA_TOKEN!
    echo NewRelic API Key : MOCK-TEST-API-KEY-12345
    echo NewRelic Account : 9999999
) > connection-info.txt
echo  Baglanti bilgileri "connection-info.txt" dosyasina kaydedildi.
echo.
pause
