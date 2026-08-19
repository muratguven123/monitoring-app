# Monitoring Dashboard

Monitoring Dashboard, **Grafana** ve **New Relic** verilerini tek bir Android uygulamasında birleştirir.  
Bu sayede servis durumunu, uygulama performansını ve alarmları telefondan hızlıca takip edebilirsiniz.

## Ne İşe Yarar?

- İlk açılışta onboarding + bağlantı doğrulama
- Grafana dashboard / panel ve New Relic APM metriklerini tek uygulamada izleme
- Alerts inbox, arka plan poll (15/30/60 dk), sessiz saatler ve mute
- NRQL Explorer, GitHub Actions durumu, Glance widget
- Ortam profilleri (Default / Staging / Prod) — profil değişince cache temizlenir
- Offline cache + Home’da “cached data” uyarısı
- Biyometrik / PIN app lock (arka plana gidince yeniden kilit)
- TR / EN string kaynakları

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

> Not: Bu JAR dosyası yoksa projeyi Android Studio ile açarak otomatik indirebilirsiniz. Alternatif olarak, bilgisayarınızda yerel Gradle kuruluysa proje kökünde `gradle wrapper` çalıştırabilirsiniz.

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

1. Onboarding akışında Grafana ve/veya New Relic kimlik bilgilerini girin; **Bağlan ve Kaydet** ile doğrulayın.
2. İsteğe bağlı: New Relic Account ID (NRQL), GitHub token + `owner/repo`, app lock.
3. Daha sonra **Settings** üzerinden profilleri, poll aralığını ve metrik eşiklerini yönetin.

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

## Offline Support

Uygulama, Room veritabanı ile offline destek sunar. Grafana dashboard listesi, New Relic uygulama bilgileri ve alert violation verileri her başarılı API çağrısından sonra önbelleğe alınır (cache TTL: 5 dakika). Ağ hatasında cache’den dönülür; Home’da “Showing cached data” bandı görünür. Ortam profili değişince Room cache temizlenir. `AlertMonitorWorker` ihlal karşılaştırmasını Room üzerinden yapar (dedup + geçmiş).

## Production (iç kullanım)

Bu uygulama **Google Play dışı / sideload** iç dağıtım için sertleştirilmiştir.

### Firebase Crashlytics

1. [Firebase Console](https://console.firebase.google.com/) → Android app ekleyin (`applicationId`: `com.monitoring.dashboard`).
2. İndirilen `google-services.json` dosyasını `app/google-services.json` olarak koyun (git’e commit edilmez).
3. Debug build’lerde Crashlytics collection kapalıdır; release’te `CrashReporting.CrashlyticsSink` aktiftir.
4. Placeholder (`app/ci/google-services.json`) **yalnızca CI'da** otomatik kopyalanır. Yerelde dosya yoksa build açıklayıcı bir hatayla durur.
5. Release build'i `verifyFirebaseConfig` görevi ile korunur: placeholder tespit edilirse build durur, çünkü o APK hiç crash raporu göndermez.
6. Şablon: `app/google-services.json.example`. Ayrıntı: [`RELEASE_SIGNING.md`](RELEASE_SIGNING.md).

### Release APK (iç dağıtım)

Ayrıntılı kurulum ve secret yönetimi: [`RELEASE_SIGNING.md`](RELEASE_SIGNING.md).

1. Keystore'u repo **dışında** oluşturun; kimlik bilgilerini `~/.gradle/gradle.properties` veya `KEYSTORE_*` env değişkenleriyle verin. `<repo>/keystore.properties` hâlâ okunur ama uyarı basar ve önerilmez.
2. `./gradlew assembleRelease`
3. APK: `app/build/outputs/apk/release/`
4. `versionCode` / `versionName` **elle düzenlenmez** — `VERSION_CODE` / `VERSION_NAME` ortam değişkenlerinden gelir. Yayın için git tag'i atın (`git tag v1.1.0 && git push origin v1.1.0`); `.github/workflows/release.yml` imzalı APK üretir, `mapping.txt`'i Crashlytics'e yükler ve GitHub release'i oluşturur.
5. Yüklü sürüm uygulama içinde Ayarlar ekranının en altında görünür.

### Release smoke checklist

1. Cold start → onboarding / credentials  
2. Home + Alerts  
3. Settings: ortam profili değiştir (cache clear snackbar)  
4. Poll aralığı 30 dk  
5. App lock: arka plana al → dön → biometric/PIN  
6. Widget özeti (alert + health)  
7. Firebase Console’da bir non-fatal / test crash görünür mü (gerçek `google-services.json` ile)

### Güvenlik notları

- API key’ler EncryptedSharedPreferences; encryption açılamazsa **plaintext disk fallback yok** (in-memory + yeniden giriş).
- Release’te cleartext HTTP kapalı; emulator HTTP yalnızca debug.
- Loglar sanitize edilir (`Bearer`, `NRAK-`, `ghp_`, …).

## Gelecek (bilinçli olarak dışarıda)

- PromQL editörü, NerdGraph alert acknowledge/close, ekran görüntüsü paylaşımı
- Google Play Data Safety / store listing (kapsam: iç kullanım)

## Mimari (Özet)

```
UI (Compose + ViewModel)
        ↓
Domain (Use Case + Model)
        ↓
Data (Repository + Remote/Local)
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



## Uygulama Sonuçları (Örnek)

Aşağıdaki sonuçlar, ekran görüntülerindeki örnek akıştan elde edilen uygulama çıktısını özetler:

- **Servis durumları** tek ekranda listelenip hızlıca kontrol edilebiliyor.
- **New Relic metrikleri** (yanıt süresi, throughput, hata oranı, Apdex) mobilde renk kodlu eşiklerle (yeşil/sarı/kırmızı) görüntüleniyor.
- **Grafana panel verileri** uygulama içinden takip edilebiliyor.
- **Ayarlar ekranı** üzerinden API/base URL değerleri güncellenerek veri kaynakları yönetilebiliyor.

## License

This project is provided as-is for educational and internal use.
