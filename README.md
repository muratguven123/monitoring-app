# 📊 Monitoring Dashboard

Android uygulaması — **Grafana** ve **New Relic** verilerini tek ekranda birleştiren, gerçek zamanlı alert bildirimleri ve otomatik yenileme destekli monitoring dashboard'u.

---

## 📱 Ekranlar

### 🏠 Home Screen
```
┌─────────────────────────────────────┐
│  Monitoring Dashboard          29s 🔄│
├─────────────────────────────────────┤
│  ⚠ 2 Open Alert Violations          │
│  payment-service · HighErrorRate    │
├─────────────────────────────────────┤
│  Service Status                     │
│  ┌────────────┐  ┌────────────┐     │
│  │  Grafana   │  │ New Relic  │     │
│  │ ● Connected│  │ ● 5 Apps   │     │
│  │  v10.2.1   │  │            │     │
│  └────────────┘  └────────────┘     │
├─────────────────────────────────────┤
│  Grafana Dashboards      [See All]  │
│  🫀 API Overview · General          │
│  🫀 Infrastructure · Ops            │
├─────────────────────────────────────┤
│  New Relic Applications  [See All]  │
│  📊 payment-service · Java | 145ms  │
│       | 2.3% errors        ● Red    │
│  📊 auth-service · Go | 23ms        │
│       | 0.1% errors        ● Green  │
└─────────────────────────────────────┘
│ 🏠 Home │ 📊 Grafana │ 🔍 NR │ ⚙️ │
```

> **Sağ üstteki sayaç** (29s, 28s…) bir sonraki otomatik yenilemeye kalan süreyi gösterir.

---

### 🟠 Grafana — Dashboard Listesi
```
┌─────────────────────────────────────┐
│  Grafana Dashboards                 │
│  🔍 Search dashboards...            │
├─────────────────────────────────────┤
│  🫀 API Overview                    │
│     General                         │
│  🫀 Infrastructure Health           │
│     Ops                             │
│  🫀 Database Performance            │
│     DB Team                         │
│  🫀 Kubernetes Cluster              │
│     Platform                        │
└─────────────────────────────────────┘
```

### 🟠 Grafana — Dashboard Detay
```
┌─────────────────────────────────────┐
│  ← API Overview                     │
├─────────────────────────────────────┤
│  Panel: Request Rate                │
│  ┌─────────────────────────────┐   │
│  │  ████                       │   │
│  │  ████ ████                  │   │
│  │  ████ ████ ███              │   │
│  └─────────────────────────────┘   │
│  Panel: Error Rate                  │
│  ┌─────────────────────────────┐   │
│  │  Type: graph                │   │
│  │  Datasource: Prometheus     │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

### 🟢 New Relic — Uygulama Listesi
```
┌─────────────────────────────────────┐
│  New Relic Applications             │
│  🔍 Search applications...          │
├─────────────────────────────────────┤
│  📊 payment-service                 │
│     Java | 145ms | 2.3 rpm          │
│     | 2.3% err             ● Red    │
│  📊 auth-service                    │
│     Go | 23ms | 450.2 rpm           │
│     | 0.1% err             ● Green  │
│  📊 notification-worker             │
│     Java | 67ms | 120.0 rpm         │
│     | 0.0% err             ● Green  │
│  📊 legacy-monolith                 │
│     Ruby | 890ms | 88.5 rpm         │
│     | 5.1% err             ● Orange │
└─────────────────────────────────────┘
```

### 🟢 New Relic — Uygulama Detay
```
┌─────────────────────────────────────┐
│  ← payment-service                  │
├─────────────────────────────────────┤
│  payment-service    ● Critical      │
│  Java               Not Reporting   │
├─────────────────────────────────────┤
│  Performance Summary                │
│  ┌───────────┬──────────┬────────┐  │
│  │ Response  │Throughput│  Err   │  │
│  │  145 ms   │ 23.4 rpm │ 2.30% │  │
│  ├───────────┼──────────┼────────┤  │
│  │   Apdex   │  Hosts   │  Inst. │  │
│  │   0.85    │    3     │   12   │  │
│  └───────────┴──────────┴────────┘  │
├─────────────────────────────────────┤
│  Open Violations (2)                │
│  🔴 HighErrorRate                   │
│     payment-alerts                  │
│  ⚠️  SlowResponseTime               │
│     sla-policy                      │
└─────────────────────────────────────┘
```

---

### ⚙️ Settings
```
┌─────────────────────────────────────┐
│  Settings                           │
├─────────────────────────────────────┤
│  Grafana Configuration              │
│  Base URL                           │
│  https://grafana.yourcompany.com    │
│  API Key                            │
│  ••••••••••••••••••••••             │
├─────────────────────────────────────┤
│  New Relic Configuration            │
│  API Key                            │
│  ••••••••••••••••••••••             │
│  Account ID                         │
│  1234567                            │
├─────────────────────────────────────┤
│          [  Save Settings  ]        │
└─────────────────────────────────────┘
```

---

## 🔔 Bildirimler

Uygulama kapalıyken dahi arka planda New Relic alert violation'larını izler. Yeni bir alert geldiğinde sistem bildirimi gösterilir:

```
┌────────────────────────────────────┐
│ ⚠ Monitoring Dashboard             │
│                                    │
│ ⚠ 1 New Alert Violation            │
│ payment-alerts · HighErrorRate     │
└────────────────────────────────────┘
```

Bildirime tıklamak uygulamayı açar ve Home ekranına yönlendirir.

### Bildirim Kanalları

| Kanal | Önem | Ne Zaman |
|---|---|---|
| Critical Alert Violations | Yüksek (titreşim + ışık) | `priority: critical` violation'larda |
| Warning Alert Violations | Normal | `priority: warning` violation'larda |

---

## ⏱ Otomatik Yenileme

Home ekranı verileri **30 saniyede bir** otomatik olarak yeniler. Yenileme butonu yanındaki sayaç (`29s`, `28s`…) bir sonraki yenilemeye kalan süreyi gösterir. Manuel yenilemede sayaç sıfırlanır.

---

## 🏗 Mimari

```
com.monitoring.dashboard
├── data/
│   ├── local/
│   │   └── SecurePreferencesManager    # API key'leri EncryptedSharedPreferences'ta saklar
│   ├── remote/
│   │   ├── GrafanaApiService           # Retrofit — Grafana REST API
│   │   ├── NewRelicApiService          # Retrofit — New Relic REST API v2
│   │   ├── dto/                        # API response DTO'ları
│   │   └── interceptor/                # Auth header interceptor'ları
│   └── repository/
│       ├── GrafanaRepository           # Grafana veri katmanı
│       └── NewRelicRepository          # New Relic veri katmanı
├── di/
│   ├── NetworkModule                   # Hilt: Retrofit, OkHttp, Repository
│   └── WorkManagerModule               # Hilt: WorkManager
├── domain/
│   ├── model/                          # Temiz domain modelleri
│   └── usecase/                        # Use case'ler
├── notification/
│   └── AlertNotificationHelper         # Bildirim kanalları + gösterimi
├── ui/
│   ├── MainScreen                      # Scaffold + alt navigasyon çubuğu
│   ├── components/                     # Paylaşılan Compose component'leri
│   ├── navigation/
│   │   ├── AppNavGraph                 # Tam navigasyon grafiği
│   │   └── Screen                     # Sealed class — rota tanımları
│   ├── screens/
│   │   ├── home/                       # Ana ekran + auto-refresh ViewModel
│   │   ├── grafana/                    # Dashboard listesi + detay
│   │   ├── newrelic/                   # Uygulama listesi + detay
│   │   └── settings/                  # Ayarlar ekranı
│   └── theme/                          # Grafana-dark renk paleti
└── worker/
    └── AlertMonitorWorker              # WorkManager — arka plan alert kontrolü
```

---

## 🛠 Teknoloji Stack

| Kategori | Kütüphane |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navigasyon | Navigation Compose |
| Dependency Injection | Hilt |
| Ağ | Retrofit 2 + OkHttp 4 + Gson |
| Arka Plan | WorkManager 2.9 |
| Güvenli Depolama | EncryptedSharedPreferences |
| Tercihler | DataStore |
| Grafikler | Vico (Compose) |
| Resim Yükleme | Coil |
| Logging | Timber |

---

## 🚀 Kurulum

### Gereksinimler

- Android Studio Hedgehog (2023.1.1) veya üstü
- Android SDK API 26+ (minSdk)
- Aktif bir Grafana sunucusu
- New Relic API key'i

### Build

```bash
# Debug APK
./gradlew assembleDebug

# Cihaza yükle
./gradlew installDebug
```

### API Yapılandırması

Uygulamayı ilk açışta **Settings** sekmesine gidin ve aşağıdakileri girin:

```
Grafana Base URL  : https://grafana.sirketiniz.com
Grafana API Key   : glsa_xxxxxxxxxxxxxxxxxxxx
New Relic API Key : NRAK-xxxxxxxxxxxxxxxxxxxx
New Relic Account : 1234567
```

API key'ler cihazda **AES-256-GCM ile şifreli** olarak saklanır.

---

## 🔧 Geliştirici Notları

### WorkManager Testi

Alert worker'ı hemen çalıştırmak için:
```bash
adb shell am broadcast \
  -a "androidx.work.impl.background.systemalarm.RescheduleReceiver" \
  -p "com.monitoring.dashboard"
```

### Bildirim Testi

Test cihazında bildirim iznini kontrol edin:
```bash
adb shell dumpsys notification --noredact | grep monitoring
```

### Hilt + WorkManager Entegrasyonu

`MonitoringApp`, `Configuration.Provider` implement eder. Bu sayede `@HiltWorker` ile işaretlenmiş worker sınıfları Hilt üzerinden dependency injection alır. WorkManager'ın default auto-init'i `AndroidManifest.xml`'de devre dışı bırakılmıştır.

---

## 📋 Özellik Durumu



<img width="1080" height="2400" alt="Screenshot_20260317_220031" src="https://github.com/user-attachments/assets/6f89c716-1738-4bb9-bba5-c33e071ac333" />


<img width="1080" height="2400" alt="Screenshot_20260317_220018" src="https://github.com/user-attachments/assets/12a0ecb7-6214-49de-bf55-e2f08b54f940" />

<img width="1080" height="2400" alt="Screenshot_20260317_220452" src="https://github.com/user-attachments/assets/6f412cff-acda-49a1-abcf-ef84e63d108c" />

| Özellik | Durum |
|---|---|
| Grafana dashboard listesi | ✅ |
| Grafana dashboard detay (panel listesi) | ✅ |
| New Relic uygulama listesi + arama | ✅ |
| New Relic uygulama detay + metrikler | ✅ |
| New Relic open violations | ✅ |
| Home ekranı — servis durumu özeti | ✅ |
| Otomatik yenileme (30 sn) + geri sayım | ✅ |
| Arka plan alert bildirimleri | ✅ |
| API key güvenli depolama | ✅ |
| Alt navigasyon çubuğu | ✅ |
