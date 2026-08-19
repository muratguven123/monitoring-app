# Cursor Prompt — ProGuard/R8 kurallarını daralt

> Bu dosya tek seferlik bir görev tarifi. İş bitince silinebilir.
> Aşağıdaki bloğu olduğu gibi Cursor'a yapıştır.

---

## Görev

`app/proguard-rules.pro` dosyasındaki aşırı geniş `-keep` kurallarını daralt.

### Neden

Şu an release build'de R8 pratikte devre dışı. Kanıt:

- `app/build/outputs/mapping/release/mapping.txt` → **209 MB, 1.4 milyon satır**
- `seeds.txt` → **19 MB**. En çok korunan paketler:
  - `androidx.compose.material` → 25.455 üye
  - `androidx.compose.material3` → 9.044
  - `androidx.compose.foundation` → 8.431
  - `androidx.compose.ui` → 7.732
  - `com.google.firebase` → 2.419

Sebep: `-keep class <paket>.** { *; }` biçimindeki 18 adet toptan kural. Bunlar
ilgili paketleri hem küçültmeden hem karartmadan bırakıyor.

İki somut zarar:

1. **Crashlytics deobfuscation riski.** 209 MB'lık mapping dosyası yükleme
   sınırlarını zorlar; yüklenemezse üretimdeki stack trace'ler okunamaz.
2. **Optimizasyon kaybı.** Ölü kod eleme ve inline'lama bu paketlerde çalışmıyor.

### Kritik bağlam — bu görev neden tehlikeli

`-keep` kuralı kaldırmak **derleme hatası üretmez**. Kırılma yalnızca çalışma
zamanında, genellikle reflection kullanan yerlerde ortaya çıkar:
Gson deserialization, Retrofit arayüzleri, Room, WorkManager worker'ları,
Hilt üretilen sınıflar.

Bu yüzden "derlendi, tamam" yeterli değil. Her adımda release APK'yı çalıştırıp
doğrulaman gerekiyor.

---

## Yöntem

Kuralları **tek tek** kaldır. Toplu değişiklik yapma — bir şey kırılırsa hangi
kuralın kırdığını bilemezsin.

Her adım için döngü:

```powershell
.\gradlew assembleRelease
# mapping.txt boyutunu not et:
(Get-Item app\build\outputs\mapping\release\mapping.txt).Length / 1MB
# APK'yı kur ve ilgili ekranı test et:
adb install -r app\build\outputs\apk\release\app-release.apk
```

Bir adım kırılırsa kuralı geri koy ama **daraltarak** — toptan
`-keep class x.** { *; }` yerine yalnızca gereken sınıf/üyeyi koru. Neden
gerektiğini kuralın üstüne yorum olarak yaz.

### Kaldırma sırası (düşük riskten yükseğe)

Bu kütüphanelerin çoğu AAR'ı içinde kendi `consumer-rules.pro` dosyasını
getiriyor; R8 onları zaten otomatik uyguluyor. Yani elle yazılmış toptan kurallar
çoğunlukla gereksiz.

| Sıra | Kural (satır) | Kaldırınca test edilecek |
|---|---|---|
| 1 | `androidx.compose.**` (126) | Tüm ekranlar açılıyor mu, tema/dark mode |
| 2 | `com.google.android.gms.**` (147) | Uygulama açılışı |
| 3 | `androidx.navigation.**` (130) | Ekranlar arası geçiş, deep link `monitoring://alerts` |
| 4 | `com.google.accompanist.**` (122) | Aşağı çekip yenileme |
| 5 | `coil.**` (104) | Grafana panel görselleri |
| 6 | `com.patrykandpatrick.vico.**` (108) | New Relic grafikleri |
| 7 | `androidx.datastore.**` (100) | Ayarlar kalıcılığı (kapat/aç) |
| 8 | `androidx.work.**` (92) | Alarm bildirimi, cihaz yeniden başlatma |
| 9 | `androidx.security.crypto.**` (96) | API anahtarı kaydet/oku |
| 10 | `okhttp3.**`, `okio.**` (40,41,44) | Tüm ağ çağrıları |
| 11 | `retrofit2.**` (30) | Tüm ağ çağrıları |
| 12 | `com.google.gson.**` (47) | JSON ayrıştırma — **en riskli** |
| 13 | `com.google.firebase.**` (146) | Crashlytics raporu ulaşıyor mu |

### Dokunma

Bunlar gerçekten gerekli, kaldırma:

- **Satır 61** `-keep class com.monitoring.dashboard.data.remote.dto.** { *; }`
  DTO alan adları Gson tarafından reflection ile okunuyor; karartılırsa
  deserialization sessizce boş nesne üretir.
- **Satır 31-36** Retrofit `@retrofit2.http.*` anotasyonlu metot kuralları.
- **Satır 64-66** Room `@Entity` / `@Dao` / `RoomDatabase` kuralları.
- **Satır 88-91** `ListenableWorker` constructor kuralı.
- **Satır 135-136** `-keepattributes SourceFile,LineNumberTable` ve
  `-renamesourcefileattribute` — Crashlytics stack trace okunabilirliği buna bağlı.
- **Satır 152-156** Application/Activity/Service/Receiver/Provider giriş noktaları.

### Ayrıca gözden geçir

- **Satır 25-27:**
  ```
  -keepclassmembers class * { ** INSTANCE; }
  ```
  Bu, uygulamadaki *ve tüm kütüphanelerdeki* her sınıfa uygulanıyor. Yalnızca
  gerçekten gereken yere daralt (muhtemelen sadece kendi `object`'lerimiz).

- **`-dontwarn` kuralları:** Toptan `-dontwarn` gerçek uyumsuzluk uyarılarını
  gizler. Kaldırıp hangi uyarıların gerçekten çıktığına bak, sadece onları sustur.

- **`gradle.properties`:** `android.r8.strictFullModeForKeepRules=false` ve
  `android.r8.optimizedResourceShrinking=false` geçici uyumluluk bayrakları.
  Kurallar daraldıktan sonra bunları tek tek kaldırmayı dene.

---

## Başarı ölçütü

- [ ] `mapping.txt` 209 MB'tan **20 MB'ın altına** indi (hedef: birkaç MB)
- [ ] `.\gradlew assembleRelease` geçiyor
- [ ] `.\gradlew testDebugUnitTest` hâlâ 146/146 geçiyor
- [ ] `.\gradlew lintRelease` temiz
- [ ] Release APK'da tüm ekranlar geziliyor: Home, Grafana liste/detay/panel,
      New Relic liste/detay/metrik, Alerts, NRQL, Datasources, GitHub, Settings,
      Onboarding, App Lock
- [ ] Widget ana ekrana eklenip veri gösteriyor
- [ ] Bildirim geliyor, deep link `monitoring://alerts` açılıyor
- [ ] Cihaz yeniden başlatıldıktan sonra arka plan senkronu devam ediyor
- [ ] API anahtarı kaydedilip uygulama yeniden açıldığında korunuyor
- [ ] Firebase konsolunda bir test raporu **okunabilir** stack trace ile görünüyor
      (obfuscated satır adları değil)

Son maddeyi atlama — bu görevin asıl amacı zaten deobfuscation'ı kurtarmak.

## Teslim

Her kaldırılan kural için tek bir commit at, mesajında mapping.txt'in yeni
boyutunu yaz. Böylece bir şey kırılırsa hangi adımın kırdığı git geçmişinden
görülür.
