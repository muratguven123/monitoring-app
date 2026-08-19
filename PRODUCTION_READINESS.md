# Production Readiness — Değerlendirme ve Plan

**Proje:** MonitoringDashboard (`com.monitoring.dashboard`)
**Dağıtım hedefi:** Şirket içi / kurumsal (APK veya MDM), Play Store değil
**Değerlendirme tarihi:** 2026-08-16
**Kod tabanı:** 111 Kotlin dosyası, ~10.250 satır

---

## Özet Karar

**Şu an production-ready DEĞİL.** Mimari ve altyapı beklenenin üzerinde olgun — temiz katmanlı yapı (data/domain/ui), Hilt DI, Room, WorkManager, EncryptedSharedPreferences, R8 + ProGuard kuralları, CI'da release smoke build ve i18n mevcut. Ancak **canlıya çıkmayı engelleyen 5 kritik (P0) sorun** var. Bunların en önemlisi Crashlytics'in sahte bir Firebase yapılandırmasıyla çalışıyor olması — yani üretimde hiçbir crash raporu gelmeyecek.

Tahmini süre: **P0 için 2–3 gün**, P1 dahil **1,5–2 hafta**.

| Alan | İlk denetim | Şimdi |
|---|---|---|
| Mimari & kod yapısı | Hazır | Hazır |
| Güvenlik (uygulama içi) | Büyük ölçüde hazır | Hazır |
| Debug build | — | **Derleniyor, emülatörde çalışıyor** |
| Release build (R8) | Hiç denenmedi | **Derleniyor, imzalı APK üretiyor** |
| Lint | Yapılandırılmamış | **Temiz** (`No issues found`) |
| Crash / gözlemlenebilirlik | **Hazır değil** | Gerçek Firebase projesi bağlandı, guard çalışıyor |
| Unit testler | 8 ViewModel'in testi yok | **146 test, hepsi yeşil** |
| UI / instrumented test | Yok | CI job'ı var, test yok |
| İmzalama | Parola repoda düz metin | Çalışıyor — **keystore hâlâ proje klasöründe** |
| Dağıtım süreci | Yok | Tag → imzalı APK + mapping + release |
| Grafana yapılandırma | Sessizce başarısız | Doğrulanıyor, alt yol destekli |
| Cihazda regresyon turu | Yapılmadı | **Yapılmadı** |

---

## DURUM (18.08.2026, 19:50)

**Instrumented testler ilk kez koştu ve geçti. İki engel daha kapandı.**

| Paket | Sonuç | Zaman |
|---|---|---|
| Unit | **146 test, 0 hata** | 19:48 — DI refaktöründen *sonra*, yani refaktör doğrulandı |
| Instrumented | **24 test, 0 hata** | 18:56 — **ilk kez** (Pixel_10_Pro emülatör) |

Instrumented dağılımı: `AlertDaoTest` 6, `GrafanaDaoTest` 6, `NewRelicDaoTest` 6
(üçü de daha önce hiç çalışmamıştı) + `CriticalFlowsTest` 6 (yeni UI testleri).

Cursor bu turda döngüyü kapattı: kod yazdı, koşturdu, geçirdi.

### Ama önemli bir nüans

Bu testler **debug** varyantında koştu (`androidTest-results/connected/debug/`).
Debug'da R8 uygulanmaz. Yani asıl endişemizi — R8'in yanlış şeyi elemesi —
doğrulamıyorlar. APK'nın %60'ı elendi ve bu risk hâlâ test edilmedi.

`CURSOR_PROMPT_release.md` Görev 3 tam bu yüzden vardı: minify açık varyantta
test koşabilmek. O kısım henüz yapılmadı.

### Kalan

| Engel | Durum |
|---|---|
| ~~İmzalama doğrulanmadı~~ | Kapandı |
| ~~Instrumented testler~~ | **Kapandı** — 24/24 |
| Cihazda regresyon (release/R8) | **Açık** — UI testleri debug'da koştu |
| Crashlytics teslimatı | Açık — insan gerektiriyor |
| Etiket yok | Açık |
| Keystore parolaları repoda | Açık — insan gerektiriyor |
| ProGuard daraltma | Durdu (17 toptan kural kaldı) |
| Release APK | `clean` edilmiş, yeniden derlenmeli |

---

## DURUM (18.08.2026, 18:19)

**Henüz production ready değil — ama iki engel daha kapandı.**

### Bu turda değişenler

ProGuard daraltma işi başlamış: `-keep class androidx.compose.** { *; }` kaldırılmış.
Tek bir kural, iki büyük kazanç:

| Ölçüm | Önce | Sonra |
|---|---|---|
| `mapping.txt` | 209 MB | **50.4 MB** (−76%) |
| **APK boyutu** | 11.6 MB | **4.7 MB** (−60%) |
| `seeds.txt` | 19 MB | 4.8 MB |

APK'nın yarıdan fazla küçülmesi, Compose'un gerçekten hiç küçültülmediğini
doğruluyor. Kalan 17 toptan kural daraltılırsa daha da inecek.

**Ayrıca engel #1 kendiliğinden kapandı:** bu build (18.08 18:19) benim
`keystore.properties` değişikliğimden (16.08 19:26) sonra çalıştı ve imzalı APK
üretti. Yani mutlak keystore yolu **doğrulandı**.

### Kanıtlanmış olanlar

| Kontrol | Kanıt |
|---|---|
| Unit testler | 146 test, 0 hata |
| Release derleme + R8 | `app-release.apk` üretildi, `mapping.txt` var |
| İmzalama zinciri | APK Signing Block v2 mevcut |
| Firebase yapılandırması | `monitoring-app-8a8e0`, placeholder izi yok |
| Release guard'ları | `verifyReleaseSigning` çalıştı ve uyarı bastı |
| Lint | `No issues found` |
| Çeviri | 183/183, format belirteçleri tutarlı |
| Sır sızıntısı | Repoda `.jks` yok, git'e sızan sır: 0 dosya |

### Kalan engeller

| # | Engel | Neden önemli |
|---|---|---|
| ~~1~~ | ~~İmzalama ayarı doğrulanmadı~~ | **Kapandı** — 18.08 build'i imzalı APK üretti |
| 2 | **172 dosya commit edilmemiş, etiket yok** | Tüm işin tamamı çalışma ağacında duruyor. Tek bir kaza her şeyi götürür |
| 3 | **Crashlytics teslimatı doğrulanmadı** | Yapılandırma doğru ama raporun ulaştığı hiç görülmedi |
| 4 | **Cihazda regresyon turu yapılmadı** | **Artık daha kritik:** APK 11.6 → 4.7 MB düştü, yani ciddi miktarda kod elendi. Neyin elendiği ancak çalıştırılınca anlaşılır |
| 5 | **Instrumented testler hiç çalışmadı** | DAO testleri emülatör gerektiriyor |

### Engel olmayan ama not edilmeli

- **Parolalar hâlâ proje klasöründe** (`keystore.properties`). Anahtar dışarı
  taşındı, parolalar kaldı.
- **`mapping.txt` 209 MB / 1.4M satır.** Bu boyut bir uygulama için olağandışı
  büyük ve Crashlytics yükleme sınırlarını zorlar. Adım 3'te stack trace'ler
  okunamaz (obfuscated) geliyorsa sebebi büyük olasılıkla budur. Kök neden,
  ProGuard kurallarının fazla geniş olması (`-keep class okhttp3.** { *; }` gibi
  toptan kurallar; `seeds.txt` 19 MB). Daraltılması ayrı bir iş.

### Sıradaki adım

**Önce commit et.** ProGuard işi devam ederken bir şey kırılırsa geri dönecek
bir nokta yok:

```powershell
git add -A
git commit -m "feat: production hardening + narrow Compose keep rule"
```

Sonra yeni APK'yı cihaza kur ve tam turu at — bu sefer gerçekten şart, çünkü
APK'nın %60'ı elendi:

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

Home / Grafana liste-detay-panel / New Relic liste-detay-metrik / Alerts / NRQL /
Datasources / GitHub / Settings / Onboarding / App Lock + widget + bildirim +
deep link + cihaz yeniden başlatma.

Bu tur temizse Crashlytics doğrulaması (bkz. "Kalan 3 Adım" §2) ve ardından
`git tag v1.0.0`.

---

## Uygulama Durumu (2026-08-16 güncellemesi)

Aşağıdaki maddeler kodda uygulandı. **Hiçbiri derlenerek doğrulanmadı** — bu
ortamda Android SDK ve Maven erişimi yok. İlk yapılacak iş Android Studio'da
`./gradlew testDebugUnitTest lintRelease assembleDebug` çalıştırmak.

| Madde | Durum | Ne yapıldı |
|---|---|---|
| P0-1 Firebase koruması | Kodda tamam | `settings.gradle.kts` placeholder'ı yalnızca CI'da kopyalar; `verifyFirebaseConfig` görevi release öncesi placeholder tespit ederse build'i durdurur |
| P0-1 Gerçek Firebase projesi | **Sende** | Konsoldan `google-services.json` indirip `app/` altına koyman gerekiyor |
| P0-2 İmzalama sırları | Kodda tamam | env → `KEYSTORE_PROPERTIES_FILE` → `~/.gradle/gradle.properties` → repo (uyarılı) sırası; `verifyReleaseSigning` imzasız release'i engeller; `RELEASE_SIGNING.md` yazıldı |
| P0-2 Yeni keystore | **Sende** | Mevcut parola yanmış kabul edilmeli, `RELEASE_SIGNING.md` §2'deki adımlar |
| P0-3 Release regresyonu | **Sende** | Cihazda tam tur gerektiriyor, otomatikleştirilemez |
| P0-4 Room şeması | Tamam | `version = 1`'e sıfırlandı, `MonitoringMigrations` ve elle yazılmış şema JSON'ları silindi (Room ilk build'de yeniden üretecek) |
| P0-5 Sürümleme | Kodda tamam | `versionCode`/`versionName` `VERSION_CODE`/`VERSION_NAME` env'lerinden; `release.yml` git tag'inden türetiyor; sürüm Ayarlar ekranında görünüyor |
| P1-1 ViewModel testleri | Tamam | 8 ViewModel'in tamamı + `GrafanaServerUrl` için ~70 yeni test |
| P1-2 Lint & statik analiz | Tamam | `lint` bloğu (`MissingTranslation` fatal), CI'da `lintRelease` + rapor artifact, Dependabot |
| P1-2 Instrumented testler | Tamam | CI'ya emulator job'ı eklendi |
| P1-3 Bağımlılık güncellemeleri | **Ertelendi** | Derleyip doğrulayabildiğin bir ortamda yapılacak (aşağıda) |
| P1-4 Türkçe çeviri | Tamam | 96 eksik string çevrildi, 183/183 eşleşiyor, format belirteçleri doğrulandı |
| P1-5 Grafana URL altyapısı | Tamam | Aşağıda ayrıntılı |

### Grafana URL altyapısı — ne değişti

Amaç: herhangi bir Grafana kurulumuna işaret edebilmek. Yeni
`GrafanaServerUrl` tipi kullanıcı girdisini ayrıştırıp normalleştiriyor:

```
grafana.sirket.com              → https://grafana.sirket.com/
grafana.sirket.com:3000         → https://grafana.sirket.com:3000/
https://sirket.com/grafana      → https://sirket.com/grafana/
HTTPS://Sirket.COM/grafana?x=1  → https://sirket.com/grafana/
```

Bulunan ve düzeltilen gerçek bir hata: eski `DynamicBaseUrlInterceptor` yalnızca
şema/host/port'u yeniden yazıyordu, **yol önekini yok sayıyordu**. Ters proxy
arkasında alt yolda çalışan bir Grafana'da (`sirket.com/grafana`) `/api/health`
isteği proxy köküne gidip 404 dönüyordu — yani sağlıklı bir sunucu bozuk
görünüyordu. Kurumsal kurulumlarda bu yaygın bir yerleşim.

Diğer değişiklikler:

- `https://localhost/` fallback'i kaldırıldı. Yapılandırılmamış durum artık
  `GrafanaNotConfiguredException` ile tip düzeyinde modelleniyor; istek cihazdan
  hiç çıkmıyor.
- Home ekranında "Grafana yapılandırılmamış" kartı + Ayarlar'a yönlendirme.
  Ağ hatası olarak gösterilmiyor.
- Ayarlar'da yazarken canlı doğrulama: normalleştirilmiş adres gösteriliyor,
  geçersiz şema/adres hata olarak işaretleniyor, `http://` için uyarı veriliyor
  (release'de cleartext zaten engelli).
- Kaydederken normalleştirilmiş biçim saklanıyor.

### Senin doğrulaman gerekenler

Derleyemediğim için bu üçü öncelikli:

1. `./gradlew testDebugUnitTest` — yeni testler mockk imza eşleşmelerine
   dayanıyor; bir repository metodunun parametre sayısı beklediğimden farklıysa
   orada patlar.
2. `./gradlew lintRelease` — `lint-baseline.xml` henüz yok. İlk çalıştırmada
   çıkan uyarılar temizlenmeli veya baseline üretilmeli.
3. `./gradlew assembleDebug` — özellikle `SettingsScreen`'deki
   `KeyboardOptions`/`supportingText` kullanımı ve Compose BOM 2024.02 uyumu.

---

## Dördüncü Denetim — testler ilk kez çalıştı

`./gradlew testDebugUnitTest` çalıştırıldı. İki önemli sonuç:

### 1. Test kaynakları derlendi

146 testin tamamı derlendi — mockk imza uyuşmazlığı, eksik import, yanlış
parametre sayısı yok. Bu, önceki üç raporda "doğrulanamadı" diye işaretlediğim
en büyük belirsizliği kapatıyor.

Öncesinde statik taramayla bulup düzelttiğim `CacheInvalidator.clearAll()`
suspend/`verify` hatası olmasaydı burada takılırdı.

### 2. Bulunan 4 hata bana ait değil — ve CI'ı kırık bırakmış

Dört hatanın tamamı `ShouldNotifyViolationUseCaseTest` içinde. Bu dosyaya hiç
dokunmadım; `4444b42` commit'inden beri repoda duruyor ve
`UserPreferencesRepository` / `ShouldNotifyViolationUseCase` kaynakları HEAD'den
farksız. Yani bu testler **yazıldıkları günden beri kırık**.

CI workflow'u `testDebugUnitTest` çalıştırdığına göre, pipeline o commit'ten bu
yana kırmızı olmalı.

**Kök neden.** `clearExpiredMutes` varsayılan parametreli:

```kotlin
suspend fun clearExpiredMutes(now: Long = System.currentTimeMillis())
```

Kotlin varsayılan argümanı **çağrı yerinde** hesaplar. Test onu parametresiz
stub'lamıştı:

```kotlin
coEvery { userPreferencesRepository.clearExpiredMutes() } just runs
```

Bu, stub satırının çalıştığı andaki zaman damgasını kaydeder. Use case daha
sonra fonksiyonu çağırdığında farklı bir zaman damgası üretir, strict mock
eşleşme bulamaz ve `MockKException` atar. `setup()` ortak olduğu için dört test
birden düşer.

**Düzeltme:** `clearExpiredMutes(any())`.

Aynı hata sınıfını (varsayılan değeri zamana bağlı fonksiyonlar) tüm kod
tabanında taradım — başka örneği yok.

### 3. Test paketi donuyordu — bu yüzden hiç bitmemiş

İkinci koşuda hata kalmadı ama paket `HomeViewModelTest`'te asılı kaldı
(3.5 dakika, %97). Bu yavaşlık değil, kilitlenme.

`HomeViewModel.init` sonsuz bir geri sayım başlatıyor:

```kotlin
while (isActive) { ...; delay(1_000L) }
```

`advanceUntilIdle()` sanal zamanı scheduler boşalana kadar ilerletir. Sınırsız
bir `delay` döngüsüne karşı **asla boşalmaz** — test başarısız olmaz, sonsuza
kadar asılı kalır.

Bu da bana ait değil: hem döngü hem `advanceUntilIdle` kullanımı benden önce
vardı. Yani bu test dosyası yazıldığından beri hiç tamamlanmamış. Yukarıdaki
mockk hatasıyla birlikte, CI'ın uzun süredir ya kırmızı ya da zaman aşımına
uğruyor olması gerekiyor.

**Düzeltme — iki aşamalı oldu.** İlk denemem eksikti ve donma devam etti:

1. `advanceUntilIdle()` → `runCurrent()`. Gerekliydi ama yetmedi.
2. Asıl sebep: `runTest`, test gövdesi bittikten sonra **kendisi de** scheduler'ı
   boşaltıyor. `viewModelScope` test scope'unun çocuğu olmadığı için otomatik
   iptal edilmiyor ve bu boşaltma sonsuz geri sayımda dönüyor. Yani test
   gövdesinde ne yaparsam yapayım fark etmiyordu.

Her test artık ViewModel'in scope'unu `finally` içinde iptal ediyor
(`withViewModel` yardımcısı) — assertion patlasa bile, aksi halde başarısız test
raporlanmak yerine asılı kalırdı.

Ayrıca her teste `timeout = 15.seconds` eklendi: ileride biri sınırsız bir
coroutine daha eklerse test saniyeler içinde başarısız olur, CPU'yu job
zaman aşımına kadar meşgul etmez. Dosyanın başına ikisinin de nedenini yazdım.

Diğer test dosyaları `advanceUntilIdle` kullanmaya devam ediyor; onların test
ettiği ViewModel'lerde sonsuz döngü yok (tüm kod tabanında tarandı, tek örnek
`HomeViewModel`).

**Ayrıca:** CI job'larına `timeout-minutes` eklendi. Donan bir test aksi halde
GitHub'ın 6 saatlik sınırına kadar runner'ı tutar.

### 4. Son hata: ViewModel'in WorkManager'a statik erişimi

Donma çözülünce paket tamamlandı: **146 test, 1 hata.**

`SettingsViewModelTest > changing the poll interval persists it` →
`IllegalStateException`. Sebep test değil, üretim kodu:

```kotlin
AlertMonitorWorker.schedule(WorkManager.getInstance(context), ...)
```

`WorkManager.getInstance()` WorkManager başlatılmamışsa hata fırlatır ve bu bir
JVM birim testinde asla başlatılmaz. ViewModel bu yüzden statik mock olmadan test
edilemiyordu.

**Düzeltme — test yerine tasarımı düzelttim.** `WorkManagerModule` zaten aynı
singleton'ı Hilt'e sağlıyordu, dolayısıyla ViewModel artık `WorkManager`'ı
enjekte ediyor. Yan etkisi: `@ApplicationContext Context` bağımlılığı tamamen
kalktı — sadece bu tek statik çağrı için duruyordu.

Test de güçlendi: artık yalnızca değerin kaydedildiğini değil, arka plan
işinin **gerçekten yeniden zamanlandığını** da doğruluyor. Aksi halde ayar
sessizce bir sonraki uygulama açılışına kadar etkisiz kalırdı.

### Sonuç: paket yeşil

```
146 test | hata: 0 | atlanan: 0
```

| Sınıf | Test |
|---|---|
| GrafanaServerUrlTest | 20 |
| SettingsViewModelTest | 17 |
| AlertsViewModelTest | 11 |
| GrafanaDashboardsViewModelTest | 11 |
| NewRelicAppDetailViewModelTest | 10 |
| NewRelicAppsViewModelTest | 10 |
| DynamicBaseUrlInterceptorTest | 9 |
| GrafanaPanelDetailViewModelTest | 9 |
| NewRelicMetricDetailViewModelTest | 9 |
| GrafanaDashboardDetailViewModelTest | 8 |
| HomeViewModelTest | 6 |
| AppLockControllerTest | 5 |
| ShouldNotifyViolationUseCaseTest | 4 |
| GrafanaRepositoryImplTest | 3 |
| NewRelicRepositoryImplTest | 3 |
| LogSanitizerTest | 3 |
| Diğer (worker, cache, crash, prefs) | 8 |

Üç koşuda üç ayrı sorun çıktı ve **üçü de bu oturumdan önce vardı**:
`clearExpiredMutes` mock uyuşmazlığı, `HomeViewModel`'in sonsuz sayacı,
`WorkManager`'a statik erişim. Test paketinin gerçekten hiç çalışmadığının
kanıtı — CI geçmişine bakmaya değer.

---

## Beşinci Denetim — release build doğrulandı

`./gradlew assembleRelease` → **BUILD SUCCESSFUL**.

### Guard'lar çalışıyor

Çıktıda `> Task :app:verifyReleaseSigning` göründü ve uyarısını bastı. Bu,
üçüncü denetimde düzelttiğim `dependsOn` bağlamasının **gerçekten devrede**
olduğunu kanıtlıyor — ilk versiyonu sessizce hiç çalışmıyordu.

`verifyFirebaseConfig` build'i durdurmadı çünkü durduracak bir şey kalmamış:
`app/google-services.json` artık gerçek projeye işaret ediyor
(`monitoring-app-8a8e0`, `project_number 264318505856`). Üç placeholder
işaretinin hiçbiri yok.

**P0-1 kapandı.** Geriye tek doğrulama kalıyor: release APK'yı cihaza kurup
bilerek bir çökme tetiklemek ve Firebase konsolunda göründüğünü görmek.
Yapılandırmanın doğru olması, raporun ulaştığını kanıtlamaz.

### aapt'ın yakaladığı gerçek hata

```
Multiple substitutions specified in non-positional format of string resource
string/demo_alert_summary
```

`%d Critical · %d Warning` — iki argüman, pozisyon belirteci yok. Çeviri
argümanları farklı sıraya koyarsa kritik ve uyarı sayıları sessizce yer
değiştirir. İngilizcesi baştan böyleymiş; Türkçe çeviriyi yazarken ben de aynı
kalıbı kopyalamışım.

`%1$d` / `%2$d` yapıldı, iki dilde de. Tüm string kaynakları aynı kalıp için
tarandı — başka örneği yok.

**Yan bulgu:** `demo_*` stringlerinin hiçbiri kodda kullanılmıyor (7 adet, ölü
kaynak). Silmedim — bir önizleme ekranından kalmış olabilir, kararı sana ait.
Lint'in `UnusedResources` kuralı bunları zaten uyarı olarak listeleyecek.

### İmzalama uyarısı beklenen davranış

```
WARNING: signing credentials were read from <repo>/keystore.properties.
```

Bu guard'ın doğru çalıştığının işareti; keystore hâlâ proje klasöründe.
Taşıma adımları "Kalan 3 Adım" §3'te.

---

## Kalan 3 Adım — tam komutlar

### 1. Unit testleri çalıştır

```powershell
cd C:\Users\murat\Desktop\monitoring-app
.\gradlew testDebugUnitTest
```

Rapor: `app/build/reports/tests/testDebugUnitTest/index.html`

Aynı turda release guard'ın gerçekten çalıştığını da doğrula — bu komut artık
placeholder Firebase config yüzünden **hata vermeli**:

```powershell
.\gradlew assembleRelease
```

Hata vermiyorsa guard hâlâ bağlanmamıştır; build çıktısındaki
`WARNING: no release task matched releaseGuardTaskNames` satırını ara.

### 2. Crashlytics'in gerçekten rapor gönderdiğini doğrula

Yapılandırma tamam (`monitoring-app-8a8e0`), ama **doğru yapılandırma raporun
ulaştığını kanıtlamaz.** Bunu bir kez görmeden Crashlytics'e güvenilmemeli.

Kod değişikliği gerekmiyor. Release build'de `MonitoringApp.ProductionTree` her
`Timber.e(throwable, …)` çağrısını `FirebaseCrashlytics.recordException()`'a
yönlendiriyor, ve `GrafanaRepositoryImpl.safeApiCall` ağ hatalarında tam olarak
bunu yapıyor. Yani çözülemeyen bir sunucu adresi bir non-fatal rapor üretir:

1. Release APK'yı cihaza/emülatöre kur (debug **olmaz** — debug'da Crashlytics
   toplama kapalı, `MonitoringApp.onCreate` içinde).
2. Ayarlar → Grafana adresini `https://bozuk-adres.invalid` yap, kaydet.
3. Grafana ekranında aşağı çekip yenile → DNS hatası → non-fatal kaydedilir.
4. Uygulamayı tamamen kapat ve yeniden aç (non-fatal'lar sonraki açılışta yüklenir).
5. Firebase konsolu → Crashlytics → **Non-fatals** sekmesi.

Not: bu `recordException` yolunu doğrular. Gerçek bir fatal çökme aynı SDK'dan
geçtiği için bu yeterli bir kanıttır; ayrıca stack trace okunabilir çıkıyorsa
`mapping.txt` yüklemesi de çalışıyor demektir.

Bittiğinde Grafana adresini gerçek sunucuya geri almayı unutma.

### 3. Keystore'u proje klasörünün dışına al — YARISI YAPILDI

**Yapıldı:**

- Kökteki artık kopya silindi (iki dosya byte-byte aynıydı, sha256 `ee0e9226…`;
  build `app/` altındakini kullanıyordu).
- `app/release-key.jks` → `C:\Users\murat\.android-keys\release-key.jks` taşındı.
- `keystore.properties` içindeki `storeFile` mutlak yola çevrildi.

Artık repoda hiçbir `.jks` yok — anahtar materyali proje klasörünün dışında.

**Kalan (sende):** iki parola hâlâ `keystore.properties` içinde, yani proje
klasöründe. Bu satırları ben taşıyamam — parola yazmak güvenlik sınırım.

Dört satırı `C:\Users\murat\.gradle\gradle.properties` dosyasına taşı:

```properties
storeFile=C:\\Users\\murat\\.android-keys\\release-key.jks
storePassword=<store parolası>
keyAlias=my-key-alias
keyPassword=<key parolası>
```

Sonra repodakini sil:

```powershell
del C:\Users\murat\Desktop\monitoring-app\keystore.properties
```

`verifyReleaseSigning` uyarısının kaybolması, işin bittiğinin göstergesi olur.

**Son olarak:** keystore'un şifreli bir yedeğini parola yöneticisine koy.
Kaybolursa aynı `applicationId` ile bir daha güncelleme yayınlanamaz — kullanıcılar
uygulamayı silip yeniden kurmak zorunda kalır.

---

## Üçüncü Denetim — build kanıtlarıyla

Bu tur öncekilerden farklı: artık tahmin değil, **gerçek build çıktıları** var.
`app/build/` içindeki zaman damgaları ve ürünler incelendi.

### Kanıtlananlar

Tüm kaynak değişikliklerim 16:31'de bitmiş; release APK 17:08'de, debug APK
17:58'de üretilmiş. Yani **her iki varyant da değişikliklerimle derlendi.**

| Kanıt | Anlamı |
|---|---|
| `app-debug.apk` (17:58, 24 MB) | Debug derleniyor; emülatörde çalışan build bu |
| `app-release.apk` (17:08, 11.6 MB) | **R8/minify çalışıyor** — P0-3'ün derleme kısmı geçti |
| `mapping.txt` üretilmiş | Obfuscation aktif, stack trace çözümlemesi mümkün |
| APK Signing Block v2 mevcut | **Release APK imzalı** — imzalama zinciri çalışıyor |
| `lint-results-release.txt` → `No issues found` | Lint temiz; `MissingTranslation` fatal kuralı geçti, yani çeviriler gerçekten tam |
| `versionCode 1 / versionName 1.0.0` | Sürümleme kodu doğru çalışıyor (env yokken baseline'a düşüyor) |
| Baseline profile üretilmiş | Soğuk başlatma optimizasyonu zaten devrede (P2 maddesi gereksizmiş) |

Bu, ilk iki raporda "derlenmedi, doğrulanmadı" diye işaretlediğim risklerin
büyük kısmını kapatıyor. ProGuard kuralları R8'i geçiyor, Compose/Hilt/Room
kod üretimi çalışıyor, yeni sınıflarım (`GrafanaServerUrl`,
`GrafanaBaseUrlProvider`, yeniden yazılan interceptor) sorunsuz derleniyor.

### Bu turda bulunan kusur: release guard hiç çalışmamış

`app/google-services.json` hâlâ placeholder olmasına rağmen release build
17:08'de **başarıyla tamamlanmış**. `verifyFirebaseConfig` çalışsaydı build
durmalıydı.

Sebep: guard'ı yalnızca `preReleaseBuild` görevine bağlamıştım ve bu görev
kullanılan AGP sürümünde artık çalışmıyor. Sessizce hiçbir şeyi korumayan bir
guard, guard olmamasından daha kötü — korunuyormuş izlenimi veriyor.

**Düzeltildi:** guard artık beş göreve birden bağlı
(`processReleaseGoogleServices`, `minifyReleaseWithR8`, `packageRelease`,
`bundleRelease`, `preReleaseBuild`) ve hiçbiri eşleşmezse build uyarı basıyor.
İlk denemede `gradle.taskGraph.whenReady` kullanmıştım; bu projede
configuration cache açık olduğu için onunla uyumsuzdu, `afterEvaluate`'e
çevirdim.

**Doğrulaması sende:** `./gradlew assembleRelease` şimdi placeholder Firebase
config yüzünden **durmalı**. Durmuyorsa guard hâlâ bağlanmamış demektir.

### Hâlâ çalıştırılmamış olan

`app/build/reports/` altında test sonucu yok — **146 unit test bir kez bile
çalışmadı.** Emülatörde uygulamanın açılması bunu kanıtlamaz; testler ayrı
derlenir ve mockk imza uyuşmazlıkları yalnızca orada ortaya çıkar.

```
./gradlew testDebugUnitTest
```

### İmzalama: ilerleme var, bir risk kaldı

`keystore.properties` yeniden yazılmış, standart `storeFile`/`storePassword`
isimlendirmesine geçilmiş ve build script her iki adlandırmayı da kabul edecek
şekilde genişletilmiş. Release APK imzalı üretiliyor.

Kalan risk: keystore dosyası proje klasörünün **içinde**, üstelik iki kopya
hâlinde (`release-key.jks` ve `app/release-key.jks`). İkisi de gitignore'lu, yani
commit edilmiyor — ama gitignore sızıntıyı engellemez. Klasör Masaüstü'nde;
yedeklenir, senkronlanır veya zip'lenirse anahtar ve parola birlikte gider.
İki kopya olması ayrıca hangisinin kullanıldığını belirsizleştiriyor; yanlış
anahtarla imzalanmış bir sürüm güncelleme zincirini kalıcı olarak bozar.

Öneri: tek kopya, `C:\Users\murat\.android-keys\` altında, yol
`~/.gradle/gradle.properties` üzerinden verilsin. Ayrıntı `RELEASE_SIGNING.md` §3.

---

## İkinci Denetim (aynı gün) — bulunan ve düzeltilen regresyon

İlk turda eklediğim yol öneki desteği **iki kez uygulanıyordu**.

`provideGrafanaRetrofit` yapılandırılmış adresi Retrofit'e base URL olarak
veriyordu (`https://sirket.com/grafana/`), Retrofit `api/search`'ü buna ekleyip
`/grafana/api/search` üretiyordu, sonra interceptor öneki bir kez daha
ekleyerek `/grafana/grafana/api/search` yapıyordu. Aynı sorun Coil'in panel
render URL'lerinde de vardı: onlar zaten mutlak ve önekli geliyordu.

Sonuç: alt yolda çalışan her Grafana kurulumunda tüm istekler 404 olurdu — yani
düzeltmeye çalıştığım hatanın aynısı, ters yönden.

**Düzeltme.** Retrofit artık *her zaman* placeholder host'a
(`grafana-not-configured.invalid`) işaret ediyor, gerçek adres yalnızca tek bir
yerde — interceptor'da — uygulanıyor. Bu, interceptor'a kesin ve idempotent bir
kural veriyor: *placeholder'a giden istekleri yeniden yaz, diğerlerine dokunma.*

Yan faydası: Retrofit singleton olduğu için, eskiden kullanıcı Ayarlar'dan
sunucuyu değiştirdiğinde uygulama yeniden başlatılana kadar eski adrese istek
atmaya devam ediyordu. Artık her istek güncel adresi kullanıyor.

Ayrıca panel render URL'lerini üreten iki ViewModel ham tercihi okumayı bırakıp
normalleştirilmiş adrese geçti — şemasız kaydedilmiş eski bir değer artık
bozuk URL üretmiyor.

Bu davranışı kilitleyen 10 test eklendi (`DynamicBaseUrlInterceptorTest`):
öneksiz/önekli/iç içe önekli sunucular, port ve query korunumu, mutlak render
URL'lerinin değiştirilmemesi, alakasız host'a müdahale edilmemesi, ve
yapılandırılmamış durumda isteğin cihazdan hiç çıkmaması.

### İkinci denetimde düzeltilen diğer şeyler

- `README.md` hâlâ "`versionCode`'u elle artırın" ve "`keystore.properties`
  kopyalayın" diyordu — ikisi de artık yanlış. Güncellendi.
- `.gitignore`'a lint rapor çıktıları eklendi.

### İkinci denetimde bir düzeltme (ilk rapordaki hatam)

İlk raporda "111 dosyanın sadece 14'ünde `contentDescription` var" demiştim.
Bu yanıltıcıydı: 14 *dosya* sayısıydı, kullanım sayısı değil. Gerçek oran
28 `Icon`/`Image` çağrısına karşılık 30 `contentDescription` — yani erişilebilirlik
etiketlemesi büyük ölçüde yapılmış. P1-4'teki erişilebilirlik maddesi bu yüzden
tahmin ettiğimden küçük; geriye TalkBack ile bir gezinme turu ve dinamik yazı
boyutu testi kalıyor.

---

## P0 — Canlıya Çıkmayı Engelleyenler

### P0-1. Crashlytics sahte yapılandırmayla çalışıyor
`app/google-services.json` dosyası, CI için hazırlanmış placeholder'ın (`app/ci/google-services.json`) birebir kopyası: `project_id: monitoring-dashboard-ci`, `project_number: 000000000000`, API key `AIzaSyCI-PLACEHOLDER-NOT-A-REAL-KEY000`.

`settings.gradle.kts` gerçek dosya yoksa placeholder'ı otomatik kopyalıyor — bu davranış CI için doğru ama **release build'de sessizce sahte config'i gömüyor**. Sonuç: `MonitoringApp.onCreate()` release'de `isCrashlyticsCollectionEnabled = true` yapıyor ama raporlar hiçbir yere ulaşmıyor. Üretimdeki hataları göremezsin.

**Yapılacaklar:**
- Gerçek bir Firebase projesi oluştur, `com.monitoring.dashboard` paketi için `google-services.json` indir ve makineye/CI secret'ına koy.
- `settings.gradle.kts`'teki otomatik kopyalamayı **sadece CI ortamıyla sınırla** (`System.getenv("CI") != null` koşulu).
- `app/build.gradle.kts` release bloğuna bir doğrulama görevi ekle: `google-services.json` içinde `PLACEHOLDER` geçiyorsa build'i durdur.
- Release derlemesinde `mapping.txt`'in Crashlytics'e yüklendiğini doğrula (`firebaseCrashlyticsUploadMappingRelease` görevi).

### P0-2. Keystore parolası düz metin olarak repo kökünde
`keystore.properties` dosyası gerçek imzalama parolasını (`Guven1905`) içeriyor. `.gitignore`'da olduğu için commit edilmemiş — ama dosya diskte açık duruyor, yedeklere/senkronizasyona sızabilir ve aynı parola hem store hem key için kullanılmış.

**Yapılacaklar:**
- Bu parolayı **yanmış kabul et**: yeni bir keystore üret, store ve key için ayrı ve güçlü parolalar kullan.
- Keystore dosyasını repo dışında güvenli bir yerde sakla (parola yöneticisi / şirket secret store).
- Yerel build'lerde `keystore.properties` yerine ortam değişkenlerini kullan — build script zaten `KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD` env'lerini önceliklendiriyor.
- **Keystore'un şifreli bir yedeğini al.** Kaybedilirse aynı `applicationId` ile güncelleme yayınlanamaz.
- Sertifika geçerliliğini en az 25 yıl olarak ayarla.

### P0-3. Release build hiç doğrulanmadı
CI `assembleRelease` çalıştırıyor (R8 smoke testi olarak, iyi), ama **R8 ile küçültülmüş APK hiçbir cihazda çalıştırılmadı**. ProGuard kuralları kapsamlı görünüyor; yine de Room, Hilt, Glance widget, Vico chart ve Gson DTO'ları minify sonrası çoğu projede sorun çıkarır. Ayrıca `android.r8.strictFullModeForKeepRules=false` ve `optimizedResourceShrinking=false` — bunlar geçici uyumluluk bayrakları, kalıcı olmamalı.

**Yapılacaklar:**
- Release APK'yı gerçek bir cihaza kur ve **her ekranı** tek tek gez: Home, Grafana listesi/detay/panel, New Relic listesi/detay/metrik, Alerts, NRQL, Datasources, GitHub, Settings, Onboarding, App Lock.
- Widget'ı ana ekrana ekle, AlertMonitorWorker'ı tetikle, bildirim gelişini ve deep link'i (`monitoring://alerts`) doğrula.
- Uygulamayı kapat/aç, cihazı yeniden başlat — WorkManager'ın yeniden planlandığını gör.
- Doğrulama bitince `strictFullModeForKeepRules` bayrağını kaldırmayı dene; kırılan yer varsa keep kuralı ekle.

### P0-4. Room migration 1→2 gerçek veriyle test edilmedi
Veritabanı `version = 2` ve `MonitoringMigrations` + şema JSON'ları mevcut, `MonitoringMigrationsTest` de var. Ancak uygulama henüz `versionCode = 1` ile hiç yayınlanmadığından, sahada v1 şeması taşıyan cihaz yok. İlk sürümü **version 2 ile temiz başlatmak**, ileride gereksiz migration yükünden kurtarır.

**Yapılacaklar:**
- Karar ver: ilk yayın v2 şemasıyla temiz mi çıkacak, yoksa migration yolu korunacak mı? (Öneri: kimse v1 kullanmadığı için migration'ı sil, şemayı 1'e sabitle ve `schemas/1.json`'ı yeni şema olarak yeniden üret.)
- Hangi yolu seçersen seç, `fallbackToDestructiveMigration` **kullanılmadığını** doğrula.
- `MonitoringDatabase` üzerinde bir migration testinin CI'da instrumented olarak koştuğundan emin ol.

### P0-5. Sürümleme ve dağıtım süreci tanımsız
`versionCode = 1`, `versionName = "1.0.0"` sabit. Bir güncelleme yayınlandığında `versionCode` elle artırılmazsa cihazlar güncellemeyi kabul etmez. Şirket içi dağıtımda otomatik güncelleme mekanizması da yok.

**Yapılacaklar:**
- `versionCode`'u CI'da otomatik üret (örn. build numarası veya git commit sayısı), `versionName`'i git tag'inden al.
- Dağıtım kanalını netleştir: MDM (Intune/Workspace ONE), Firebase App Distribution veya kurumsal indirme sayfası.
- Kullanıcıya sürüm gösteren bir alan ekle (Settings ekranında `versionName (versionCode)`).
- Rollback prosedürünü yaz: hangi APK arşivde, kim geri alır, ne kadar sürede.

---

## P1 — Yayından Önce Yapılmalı

### P1-1. Test kapsamı ciddi biçimde eksik
12 unit test dosyası var ve doğru yerlerde (repository, worker, migration, sanitizer, app lock). Ama:

- **8 ViewModel'in hiç testi yok:** Alerts, GrafanaDashboards, GrafanaDashboardDetail, GrafanaPanelDetail, NewRelicApps, NewRelicAppDetail, NewRelicMetricDetail, Settings.
- **Compose UI testi sıfır** — `androidTest` altında sadece 3 DAO testi var.
- Instrumented testler CI'da **hiç çalışmıyor** (workflow'da emulator adımı yok).

**Yapılacaklar:**
- Her ViewModel için yükleme / başarı / hata / boş durum akışlarını Turbine ile test et (mevcut `HomeViewModelTest` şablon olarak kullanılabilir).
- En kritik 3 akış için Compose UI testi yaz: Onboarding → Settings'te kimlik bilgisi girme, Alerts listesi + detay, App Lock kilit/aç.
- CI'ya `reactivecircus/android-emulator-runner` ile instrumented test job'ı ekle.
- Hedef: domain + data katmanında %70+ satır kapsamı.

### P1-2. CI'da statik analiz yok
`app/build.gradle.kts` içinde `lint` bloğu yok, workflow'da lint/detekt adımı yok. Android Lint, kullanılmayan izinlerden erişilebilirlik eksiklerine kadar çok şey yakalar.

**Yapılacaklar:**
- `lint { abortOnError = true; warningsAsErrors = false; checkReleaseBuilds = true }` ekle, `lint-baseline.xml` üret.
- CI'ya `./gradlew lintRelease` adımı ve rapor artifact'ı ekle.
- detekt + ktlint ekle (opsiyonel ama tavsiye edilir).
- Bağımlılık güvenlik taraması ekle (OWASP dependency-check veya GitHub Dependabot).

### P1-3. Bağımlılık sürümlerinde tutarsızlıklar
- `compose-bom = 2024.02.00` — Kotlin 2.2.10 / AGP 9.1.0 ile birlikte oldukça eski. 2025 BOM'una güncelle.
- `security-crypto = 1.1.0-alpha06` — **alpha sürüm, üretimde kritik veri saklıyor.** Stabil sürüme veya modern alternatifine geç.
- `vico = 2.0.0-alpha.28` — alpha; stabil 2.x'e geç.
- `kapt` kullanılıyor; Hilt ve Room artık **KSP** destekliyor, derleme süresini belirgin kısaltır.

### P1-4. Erişilebilirlik ve i18n boşlukları
- Türkçe çeviri eksik: `values/strings.xml` 174 string, `values-tr/strings.xml` 87 string — **87 string çevrilmemiş**, Türkçe cihazda İngilizce görünecek.
- 111 dosyanın sadece 14'ünde `contentDescription` geçiyor; ikonlar ve grafikler ekran okuyucuya kapalı olabilir.

**Yapılacaklar:**
- `values-tr/strings.xml`'i tamamla; eksik çeviriyi CI'da kontrol eden bir script ekle.
- Tüm anlamlı `Icon`/`Image` için `contentDescription`, dekoratif olanlar için `null` ver.
- TalkBack ile bir tam gezinme turu yap, dinamik yazı boyutunu (200%) test et.

### P1-5. Grafana base URL'i yapılandırılmadan uygulama sessizce başarısız oluyor
Release'de `GRAFANA_BASE_URL` boş; `NetworkModule` fallback olarak `https://localhost/` kullanıyor. Kullanıcı Settings'ten URL girmezse tüm istekler anlaşılmaz hatalarla düşer.

**Yapılacaklar:**
- Kurumsal dağıtımda şirketin Grafana URL'ini `buildConfigField` olarak varsayılan yap (kullanıcı yine değiştirebilsin).
- Yapılandırılmamış durumda Home ekranında net bir uyarı ve "Ayarlara git" aksiyonu göster.
- `https://localhost/` fallback'ini kaldır; yapılandırılmamış durumu tip düzeyinde ayrı bir hal olarak modelle.

---

## P2 — İyileştirmeler

- **Baseline Profile** ekle — soğuk başlatma ve liste kaydırma performansını %20–30 iyileştirir.
- **StrictMode**'u debug build'de aç, ana thread disk/ağ erişimlerini yakala.
- **Sertifika sabitleme (certificate pinning)** — kurumsal iç Grafana sunucusu için değerlendirilebilir; sertifika rotasyonu riski nedeniyle dikkatli planla.
- **App Lock zorunluluğu** — kurumsal politikaya göre biyometrik kilidi varsayılan açık yap.
- **Performans izleme** — Firebase Performance Monitoring veya ağ isteği süresi metrikleri.
- **Repo temizliği** — `test.html`, `test-env/`, `build/`, `.gradle/` ve 6 ayrı markdown dokümanı gözden geçir; `BUILD_CHECKLIST.md`, `QUICK_REFERENCE.md`, `README_DOCUMENTATION.md`, `SOLUTION_SUMMARY.md` ciddi ölçüde örtüşüyor, tek README'de birleştir.
- **Commit edilmemiş değişiklikler** — `git status` 20+ değiştirilmiş dosya gösteriyor; canlıya çıkmadan önce commit'lenip etiketlenmeli.
- `gradle.properties`'teki geçici uyumluluk bayraklarını (`android.newDsl=false`, `android.builtInKotlin=false`, `android.uniquePackageNames=false` vb.) AGP 9 ile teker teker kaldırmayı dene.

---

## Uygulama Sırası

**1. Hafta — Engelleyiciler**

| Gün | İş |
|---|---|
| 1 | Gerçek Firebase projesi + google-services.json, CI koruması (P0-1) |
| 1 | Yeni keystore, ayrı parolalar, güvenli yedek, env tabanlı imzalama (P0-2) |
| 2 | Room migration kararı ve şema sabitleme (P0-4) |
| 2–3 | Release APK ile cihazda tam regresyon turu (P0-3) |
| 3 | Sürümleme otomasyonu + dağıtım/rollback prosedürü (P0-5) |

**2. Hafta — Kalite kapısı**

| Gün | İş |
|---|---|
| 4–5 | 8 ViewModel testi (P1-1) |
| 5 | Lint + detekt + CI entegrasyonu (P1-2) |
| 6 | Bağımlılık güncellemeleri, alpha'lardan çıkış, KSP geçişi (P1-3) |
| 7 | Türkçe çeviri tamamlama + erişilebilirlik turu (P1-4) |
| 8 | Grafana yapılandırma akışı düzeltmesi (P1-5) |
| 9 | Compose UI testleri + CI emulator job |
| 10 | Release adayı ile pilot grup dağıtımı |

---

## Yayın Öncesi Kontrol Listesi

- [ ] `google-services.json` gerçek Firebase projesine ait, placeholder değil
- [ ] Test crash gönderildi ve Firebase konsolunda görüldü
- [ ] `mapping.txt` Crashlytics'e yüklendi, stack trace okunabilir
- [ ] Keystore yenilendi, parolalar ayrı, şifreli yedek alındı
- [ ] Release APK cihazda kuruldu, tüm ekranlar gezildi
- [ ] Widget çalışıyor, bildirim geliyor, deep link açılıyor
- [ ] Cihaz yeniden başlatıldı, arka plan senkronu devam ediyor
- [ ] `versionCode` otomatik artıyor
- [ ] `./gradlew lintRelease` temiz
- [ ] Unit + instrumented testler CI'da yeşil
- [ ] Türkçe cihazda çevrilmemiş string yok
- [ ] Grafana URL'i yapılandırılmadığında anlaşılır bir yönlendirme var
- [ ] Rollback prosedürü yazılı ve bir kez denendi
- [ ] Tüm değişiklikler commit'lendi ve sürüm tag'i atıldı

---

## Kayda Değer Güçlü Yanlar

Bu bölümü, neyin **yeniden yapılmasına gerek olmadığını** netleştirmek için ekliyorum:

- Katmanlı mimari (data → domain → ui) tutarlı uygulanmış, use-case'ler ayrı.
- `SecurePreferencesManager` fail-closed tasarlanmış: şifreleme açılamazsa düz metin diske yazmak yerine bellek içi depoya düşüyor ve kullanıcıdan kimlik bilgisi yenilemesi istiyor. Bu, çoğu projede yanlış yapılan bir detay.
- `LogSanitizer` + `ProductionTree` ile release loglarında API anahtarı sızıntısı engellenmiş, seviye WARN'a çekilmiş.
- Release'de cleartext trafiği kapalı, debug cleartext'i ayrı source set'te izole edilmiş — doğru yaklaşım.
- HTTP body logging sadece debug'da açık.
- CI'da R8'li release derlemesi ve mapping artifact'ı zaten var.
- ProGuard kuralları elle yazılmış ve kapsamlı; DTO'lar korunmuş.
- Room şema export'u ve migration testi altyapısı kurulu.
- WorkManager auto-init'i doğru şekilde devre dışı bırakılıp Hilt worker factory'ye bağlanmış.
