# Monitoring Dashboard — Solution Summary

## 1. Proje Özeti

**Monitoring Dashboard**, Grafana ve New Relic gibi izleme platformlarının verilerini tek bir Android
uygulamasında birleştiren mobil bir dashboard çözümüdür. Geliştiriciler ve DevOps ekipleri servis
sağlığını, uygulama performansını ve açık uyarıları (alert violation) sahadan — herhangi bir Android
cihazdan — anlık olarak takip edebilir.

Uygulama, modern Android geliştirme standartlarına uygun olarak tasarlanmıştır: tamamen **Kotlin**
ile yazılmış, UI katmanında **Jetpack Compose**, DI için **Hilt**, ağ katmanında **Retrofit + OkHttp**,
yerel önbellek için **Room**, arka plan görevlerinde **WorkManager** kullanılmaktadır.

---

## 2. Teknoloji Stack

| Katman            | Teknoloji / Kütüphane                          |
|-------------------|-------------------------------------------------|
| Dil               | Kotlin 2.0                                      |
| UI Framework      | Jetpack Compose + Material 3                    |
| Mimari            | MVVM + Clean Architecture                       |
| DI                | Hilt (Dagger)                                   |
| Ağ (Network)      | Retrofit 2 + OkHttp 4 + Moshi                   |
| Yerel Veritabanı  | Room (SQLite)                                   |
| Arka Plan Görevi  | WorkManager (15 dk periyodik)                   |
| Güvenli Depolama  | EncryptedSharedPreferences (AndroidX Security)  |
| Navigasyon        | Navigation Compose                              |
| Loglama           | Timber                                          |
| Test              | JUnit 4, MockK, Turbine, Coroutines-Test        |
| Build             | Gradle (Kotlin DSL), Version Catalogs            |

---

## 3. Mimari (Architecture)

```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│  Compose Screens  ←  ViewModels (StateFlow) │
├─────────────────────────────────────────────┤
│               Domain Layer                   │
│        Use Cases  ←  Domain Models           │
├─────────────────────────────────────────────┤
│                Data Layer                    │
│  Repository (interface + impl)               │
│    ├── Remote: Retrofit API Services         │
│    └── Local:  Room DAOs + Entities          │
├─────────────────────────────────────────────┤
│                 DI Layer                     │
│  Hilt Modules (Network, Database, Worker)    │
└─────────────────────────────────────────────┘
```

### Veri Akışı (NetworkBoundResource Pattern)

1. Repository, önce Room'dan eski/stale kayıtları siler (`deleteOlderThan(cacheTtl)`).
2. Paralel olarak ağ isteği gönderilir (Retrofit).
3. Ağ başarılıysa → sonuç Room'a yazılır, ardından `NetworkResult.Success` döner.
4. Ağ başarısızsa → Room'dan önbellekteki veriler okunur.
   - Önbellek doluysa → `NetworkResult.Success` (cache'den).
   - Önbellek boşsa → `NetworkResult.Error`.

Bu desen sayesinde uygulama, internet olmadan bile son verileri gösterebilir.

---

## 4. Ana Modüller

### 4.1 `data/remote`
- **GrafanaApiService** — Grafana REST API: dashboard arama, detay, datasource, sağlık kontrolü.
- **NewRelicApiService** — New Relic REST API v2: uygulamalar, alert violations, metrikler.
- **Interceptor'lar** — `AuthInterceptor` (Grafana), `NewRelicAuthInterceptor` (New Relic),
  `DynamicBaseUrlInterceptor` (runtime URL değişikliği).
- **DTO Sınıfları** — Grafana ve New Relic JSON yanıtlarını temsil eden data class'lar.
- **NetworkResult** — `Success<T>`, `Error`, `Loading` durumlarını saran sealed class.

### 4.2 `data/local`
- **MonitoringDatabase** — Room veritabanı (version 1, 3 entity).
- **Entity'ler:**
  - `GrafanaDashboardEntity` — Dashboard id, uid, title, tags, url, folderTitle, cachedAt.
  - `NewRelicAppEntity` — Application id, name, language, healthStatus, reporting, cachedAt.
  - `AlertViolationEntity` — Violation id, label, policyName, openedAt, severity, cachedAt.
- **DAO'lar** — `GrafanaDao`, `NewRelicDao`, `AlertDao` — CRUD + `deleteOlderThan()` ile
  otomatik TTL temizliği (5 dakika).

### 4.3 `data/repository`
- **GrafanaRepositoryImpl** — Grafana dashboard listesi ve detay; NetworkBoundResource destekli.
- **NewRelicRepositoryImpl** — New Relic uygulamalar ve alert violation'lar;
  NetworkBoundResource destekli.

### 4.4 `domain`
- **Model'ler** — `Dashboard`, `DashboardDetail`, `GrafanaHealth` (UI'a dönüştürülmüş domain
  nesneleri).
- **Use Case'ler** — `GetDashboardsUseCase`, `GetDashboardDetailUseCase`,
  `CheckGrafanaHealthUseCase`.

### 4.5 `ui` (Compose Screens)
- **HomeScreen** — Tüm servislerin özet durumu, açık violation sayısı, auto-refresh countdown.
- **GrafanaDashboardsScreen** — Dashboard listesi, arama, folder bilgisi.
- **GrafanaDashboardDetailScreen** — Panel listesi (tip ikonu, PromQL sorguları).
- **NewRelicAppsScreen** — Uygulama listesi, sağlık durumu, arama.
- **NewRelicAppDetailScreen** — Renk kodlu performans metrikleri (Apdex, response time,
  error rate, throughput, host/instance sayıları), End User özeti, açık violation'lar.
- **SettingsScreen** — Grafana ve New Relic API ayarları (URL, API key, Account ID).
- **DemoPreviewScreen** — Sahte verilerle tam canlı önizleme (sunum modu).

### 4.6 `ui/components`
- **MonitoringCard** — Genel amaçlı kart (ikon, başlık, alt başlık, trailing content).
- **MetricItem / ColoredMetricItem** — Metrik gösterimi, renk kodlu eşiklerle.
- **ServiceStatusCard** — Servis sağlık durumu kartı (HEALTHY / WARNING / CRITICAL / UNKNOWN).
- **LoadingIndicator / ErrorMessage / EmptyState** — Ortak durum bileşenleri.
- **metricStatusColor()** — Değer + eşik → renk dönüşüm yardımcısı.

### 4.7 `notification`
- **AlertNotificationHelper** — İki kanal (Critical / Warning), violation sayısına göre
  bildirim, POST_NOTIFICATIONS izin kontrolü.

### 4.8 `worker`
- **AlertMonitorWorker** — 15 dakikada bir New Relic alert violation'ları kontrol eder,
  Room DB ile dedup yaparak yalnızca yeni ihlaller için bildirim gönderir.

### 4.9 `di`
- **NetworkModule** — OkHttp, Retrofit, Repository provider'ları.
- **DatabaseModule** — Room DB, DAO'lar, cache TTL sabiti.
- **WorkManagerModule** — WorkManager + HiltWorkerFactory.

---

## 5. Özellikler

| Özellik                              | Durum |
|--------------------------------------|-------|
| Grafana dashboard listeleme/detay    | ✅    |
| New Relic uygulama listeleme/detay   | ✅    |
| Renk kodlu performans metrikleri     | ✅    |
| Servis sağlık durum kartları         | ✅    |
| Room offline cache (5 dk TTL)        | ✅    |
| Arka plan alert kontrolü (WorkManager)| ✅   |
| Push bildirim (Critical/Warning)     | ✅    |
| API key güvenli depolama             | ✅    |
| String externalization (90+ kaynak)  | ✅    |
| Arama / filtreleme                   | ✅    |
| Demo/sunum modu                      | ✅    |
| Material 3 & Dark Mode desteği       | ✅    |

---

## 6. Kurulum & Çalıştırma

### Gereksinimler
- **Java 17+** (Android Studio ile birlikte gelir)
- **Android Studio Ladybug** veya üzeri
- **Android SDK 34+**

### Adımlar

```bash
# 1. Projeyi klonlayın
git clone https://github.com/muratguven123/monitoring-app.git
cd monitoring-app

# 2. Build
./gradlew assembleDebug

# 3. Cihaza kur
./gradlew installDebug

# 4. İlk açılışta Settings ekranından API anahtarlarınızı girin
```

### Test Ortamı (Opsiyonel)

`test-env/` klasörü Docker Compose ile Grafana + Prometheus + mock New Relic sunucusu sağlar:

```bash
cd test-env
docker-compose up -d
```

Bu ortamda Grafana `http://localhost:3000`, mock New Relic `http://localhost:5000` adresinde çalışır.

---

## 7. Test

### Unit Tests (4 dosya)

| Test Sınıfı                   | Test Sayısı | Kapsam                                     |
|-------------------------------|-------------|---------------------------------------------|
| `GrafanaRepositoryImplTest`   | 3           | Ağ başarı, ağ hata → cache, boş cache + hata|
| `NewRelicRepositoryImplTest`  | 3           | Aynı 3 senaryo                              |
| `HomeViewModelTest`           | 3           | Initial state, başarılı yükleme, countdown  |
| `AlertMonitorWorkerTest`      | 2           | Yeni violation → bildirim, dedup → sessiz   |

### Instrumented (DAO) Tests (3 dosya)

| Test Sınıfı       | Test Sayısı | Kapsam                                       |
|--------------------|-------------|-----------------------------------------------|
| `GrafanaDaoTest`   | 6           | insert, getAll, getById, deleteAll, TTL, upsert|
| `NewRelicDaoTest`  | 6           | Aynı 6 senaryo                                |
| `AlertDaoTest`     | 6           | Aynı 6 senaryo                                |

### Testleri Çalıştırma

```bash
# Unit testler
./gradlew test

# Instrumented testler (emulator veya fiziksel cihaz gerekli)
./gradlew connectedAndroidTest
```

---

## 8. Renk Kodlu Metrik Eşikleri

| Metrik          | Yeşil (Sağlıklı) | Sarı (Uyarı)    | Kırmızı (Kritik) |
|-----------------|-------------------|------------------|-------------------|
| Apdex Score     | ≥ 0.90            | 0.70 – 0.89      | < 0.70            |
| Response Time   | < 500 ms          | 500 – 1999 ms    | ≥ 2000 ms         |
| Error Rate      | < 1%              | 1% – 4.99%       | ≥ 5%              |
| Page Load       | < 3000 ms         | 3000 – 6999 ms   | ≥ 7000 ms         |

---

## 9. Gelecek İyileştirmeler (Future Improvements)

- **Grafik/Chart desteği** — Sparkline veya MPAndroidChart ile metrik trend grafiklerini
  doğrudan uygulama içinde görselleştirme.
- **Widget** — Ana ekran widget'ı ile servis durumunu bir bakışta görme.
- **Webhook / SSE** — WorkManager polling yerine gerçek zamanlı push mekanizması.
- **Çoklu dil desteği** — Türkçe `strings.xml` eklenmesi (şu an yalnızca İngilizce).
- **Biometric koruma** — Uygulama girişinde parmak izi/yüz tanıma ile güvenlik.
- **Pagination** — Büyük dashboard/uygulama listeleri için sayfalama.
- **NRQL / PromQL sorgu editörü** — Uygulama içinden özel sorgu çalıştırma.
- **Export / paylaşım** — Metrik raporlarını PDF veya ekran görüntüsü olarak paylaşma.
- **CI/CD entegrasyonu** — GitHub Actions ile otomatik build, test, lint pipeline.
- **Compose Preview / Screenshot testing** — UI regression testleri.

---

## 10. Proje İstatistikleri

- **Kotlin kaynak dosyaları:** 65+
- **Unit test:** 11
- **Instrumented test:** 18
- **String kaynağı:** 90+ (externalize edilmiş)
- **Room entity:** 3
- **Notification kanalı:** 2
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

---

*Bu doküman, projenin mevcut durumunu özetlemek amacıyla hazırlanmıştır.*
