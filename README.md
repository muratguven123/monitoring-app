# Monitoring Dashboard

Monitoring Dashboard, **Grafana** ve **New Relic** verilerini tek bir Android uygulamasında birleştirir.  
Bu sayede servis durumunu, uygulama performansını ve alarmları telefondan hızlıca takip edebilirsiniz.

## Ne İşe Yarar?

- Grafana dashboard ve panel bilgilerini görüntüleme
- New Relic metriklerini (yanıt süresi, throughput, hata oranı) izleme
- Tüm servisleri tek ekrandan özet olarak görme
- API anahtarlarını güvenli şekilde saklama
- İnternet yokken son verileri önbellekten gösterme

## Kullanılan Teknolojiler (Kısa)

- **Kotlin + Jetpack Compose**
- **MVVM + Clean Architecture**
- **Hilt** (Dependency Injection)
- **Retrofit + OkHttp** (API)
- **Room** (Local cache)
- **WorkManager** (Arka plan senkronizasyonu)

## Hızlı Kurulum

### 1) Gereksinimler

- Java 17+
- Android Studio (önerilen)
- `gradle/wrapper/gradle-wrapper.jar` dosyası

> Not: Bu JAR dosyası yoksa projeyi Android Studio ile açarak otomatik indirebilirsiniz veya proje kökünde `gradle wrapper` çalıştırabilirsiniz.

### 2) Projeyi klonlayın

```bash
git clone https://github.com/muratguven123/monitoring-app.git
cd monitoring-app
```

### 3) Uygulamayı derleyin

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

### 4) Cihaza kurup çalıştırın

```bash
./gradlew installDebug
```

İsterseniz Android Studio içindeki **"Run"** düğmesi ile de çalıştırabilirsiniz.

## İlk Açılışta Yapılacaklar

1. Uygulamada **Settings** ekranına gidin.
2. Gerekli API key bilgilerini girin.
3. Gerekirse base URL ayarlarını güncelleyin.

Base URL alanları `app/build.gradle.kts` dosyasındaki `BuildConfig` alanlarından gelir:

```kotlin
buildConfigField("String", "GRAFANA_BASE_URL", "\"https://your-grafana-instance.com\"")
buildConfigField("String", "NEWRELIC_BASE_URL", "\"https://api.newrelic.com\"")
buildConfigField("String", "NEWRELIC_NERDGRAPH_URL", "\"https://api.newrelic.com/graphql\"")
buildConfigField("String", "GITHUB_BASE_URL", "\"https://api.github.com\"")
```

API key bilgileri cihazda `EncryptedSharedPreferences` ile güvenli şekilde saklanır.

## Sık Kullanılan Komutlar

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit test
./gradlew test

# Instrumented test (emulator/cihaz gerekir)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

## Mimari (Özet)

```
UI (Compose + ViewModel)
        ↓
Domain (Use Case + Model)
        ↓
Data (Repository + Remote/Local)
```

## Ekran Görüntüleri

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
