# Release Signing & Firebase Configuration

Bu doküman, imzalama anahtarlarının ve Firebase yapılandırmasının nasıl
yönetileceğini anlatır. **Hiçbir sır bu repoya girmemelidir.**

---

## 1. Neden değişti

Önceden imzalama parolası `<repo>/keystore.properties` içinde düz metin
duruyordu. Dosya `.gitignore`'da olduğu için commit edilmemişti, ancak:

- proje klasörünün yedeği/cloud senkronu parolayı da taşır,
- repo bir arşive alındığında (`zip`, `tar`) parola arşive girer,
- store ve key aynı parolayı paylaşıyordu.

Build artık kimlik bilgilerini repo dışından okumayı tercih eder ve
`<repo>/keystore.properties` kullanıldığında uyarı basar.

> **Önemli:** Eski parola (`keystore.properties` içindeki) yanmış kabul
> edilmelidir. Aşağıdaki adımlarla yeni bir keystore üretin.

---

## 2. Yeni keystore üretimi

Store ve key için **farklı** parolalar kullanın. Geçerlilik süresi en az 25 yıl
olmalı — süresi dolan bir sertifikayla güncelleme yayınlanamaz.

```bash
keytool -genkeypair -v \
  -keystore monitoring-release.jks \
  -alias monitoring \
  -keyalg RSA -keysize 4096 \
  -validity 10950 \
  -dname "CN=Monitoring Dashboard, OU=IT, O=<Şirket>, L=<Şehir>, ST=<İl>, C=TR"
```

Keystore dosyasını **repo dışına** koyun, örneğin:

- Windows: `C:\Users\<kullanıcı>\.android-keys\monitoring-release.jks`
- macOS/Linux: `~/.android-keys/monitoring-release.jks`

### Yedekleme — atlanmamalı

Keystore kaybedilirse aynı `applicationId` ile **hiçbir güncelleme
yayınlanamaz**; kullanıcılar uygulamayı kaldırıp yeniden kurmak zorunda kalır.

- Keystore dosyasının şifreli bir kopyasını şirket parola yöneticisinde /
  secret store'da saklayın.
- Parolaları aynı yerde, keystore'dan ayrı kayıtta tutun.
- En az iki kişinin erişimi olsun.

---

## 3. Kimlik bilgilerinin verilmesi

Build şu sırayla arar, ilk bulduğunu kullanır:

| Öncelik | Kaynak | Kullanım |
|---|---|---|
| 1 | `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` ortam değişkenleri | CI |
| 2 | `KEYSTORE_PROPERTIES_FILE` ile gösterilen properties dosyası | Esnek |
| 3 | `~/.gradle/gradle.properties` | **Geliştirici için önerilen** |
| 4 | `<repo>/keystore.properties` | Eski yöntem, uyarı basar |

Properties dosyalarında her iki anahtar isimlendirmesi de kabul edilir:

| Amaç | Android Studio adı | Alternatif |
|---|---|---|
| Keystore yolu | `storeFile` | `keystorePath` |
| Store parolası | `storePassword` | `keystorePassword` |
| Key alias | `keyAlias` | — |
| Key parolası | `keyPassword` | — |

`storeFile` göreli verilirse `app/` modülüne göre çözülür; mutlak yol da olabilir.

### Önerilen: Gradle user home

`~/.gradle/gradle.properties` (Windows: `C:\Users\<kullanıcı>\.gradle\gradle.properties`):

```properties
storeFile=C:\\Users\\<kullanıcı>\\.android-keys\\monitoring-release.jks
storePassword=<store parolası>
keyAlias=monitoring
keyPassword=<key parolası>
```

Bu dosya repo dışındadır, hiçbir zaman commit edilmez.

### Keystore dosyası nerede durmalı

`.gitignore` `*.jks` ve `keystore.properties`'i dışlar, yani bunlar commit
edilmez. Ancak **gitignore sızıntıyı engellemez** — dosya hâlâ proje
klasöründedir ve o klasör yedeklenirse, buluta senkronlanırsa veya arşivlenirse
anahtar ve parola birlikte gider.

Keystore'u proje klasörünün dışına alın:

```
C:\Users\<kullanıcı>\.android-keys\monitoring-release.jks
```

ve yolu `~/.gradle/gradle.properties` üzerinden verin. Proje içinde birden fazla
`.jks` kopyası varsa hepsini silin — hangisinin kullanıldığı belirsizleşir ve
yanlış anahtarla imzalanmış bir sürüm, güncellemeyi kalıcı olarak bozar.

### CI (GitHub Actions)

Keystore'u base64 olarak secret'a koyun ve iş sırasında geri açın:

```bash
base64 -w0 monitoring-release.jks > keystore.b64   # bu içeriği RELEASE_KEYSTORE_BASE64 secret'ına yapıştırın
```

Gereken secret'lar:

| Secret | İçerik |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Keystore dosyasının base64 hâli |
| `RELEASE_KEYSTORE_PASSWORD` | Store parolası |
| `RELEASE_KEY_ALIAS` | Key alias (`monitoring`) |
| `RELEASE_KEY_PASSWORD` | Key parolası |
| `GOOGLE_SERVICES_JSON` | Gerçek `google-services.json` içeriği |

`.github/workflows/release.yml` bunları kullanacak şekilde hazırlandı.

---

## 4. Bu projedeki mevcut durum

| Adım | Durum |
|---|---|
| Yeni keystore üretildi | Tamam |
| Repodaki artık kopya silindi | Tamam |
| Keystore repo dışına alındı (`C:\Users\murat\.android-keys\release-key.jks`) | Tamam |
| `storeFile` mutlak yola çevrildi | Tamam |
| **Parolalar repo dışına** | **Kalan** |
| Şifreli yedek alındı | **Kalan** |

Kalan iki adım:

1. `keystore.properties` içindeki dört satırı
   `C:\Users\murat\.gradle\gradle.properties` dosyasına taşı, sonra:
   ```powershell
   del C:\Users\murat\Desktop\monitoring-app\keystore.properties
   ```
   `./gradlew assembleRelease` çalıştır — `verifyReleaseSigning` uyarısı artık
   basılmamalı. Uyarının kaybolması işin bittiğinin göstergesidir.

2. Keystore'un şifreli bir kopyasını parola yöneticisine / şirket secret
   store'una koy. Parolaları ayrı bir kayıtta tut, en az iki kişide erişim olsun.

> `C:\Users\murat\keystore\` altında eski kurulumdan kalma bir klasör var.
> Orada artık kullanılmayan bir keystore duruyorsa imha et — hangi anahtarın
> geçerli olduğu belirsiz kalmamalı.

---

## 5. Firebase / `google-services.json`

`app/ci/google-services.json` **sahte** bir Firebase projesine işaret eder
(`monitoring-dashboard-ci`, `project_number` sıfırlar, API key `PLACEHOLDER`).
Yalnızca CI'ın derleyebilmesi için vardır.

Bu dosya bir release build'e girerse **Crashlytics hiçbir rapor göndermez** ve
üretimdeki çökmeler görünmez olur. Build artık buna karşı iki katmanlı korumaya
sahiptir:

- `settings.gradle.kts` placeholder'ı **yalnızca `CI` ortam değişkeni set
  edilmişken** kopyalar. Yerelde dosya yoksa build açıklayıcı bir hatayla durur.
- `verifyFirebaseConfig` görevi her release build öncesi çalışır; dosyada
  placeholder izi bulursa build'i durdurur.

### Kurulum

1. Firebase konsolunda proje oluştur (veya mevcut şirket projesini kullan).
2. Android uygulaması ekle, paket adı: `com.monitoring.dashboard`.
3. `google-services.json` indir, `app/google-services.json` olarak kaydet.
4. CI için aynı dosyanın içeriğini `GOOGLE_SERVICES_JSON` secret'ına koy.

### Doğrulama

Release build'i cihaza kurduktan sonra bilerek bir çökme tetikle ve Firebase
konsolunda göründüğünü doğrula. Rapor gelmiyorsa Crashlytics yapılandırılmamış
demektir.

`mapping.txt` yüklemesi olmadan stack trace'ler okunamaz:

```bash
./gradlew assembleRelease firebaseCrashlyticsUploadMappingRelease
```

---

## 6. Sürümleme

`versionCode` ve `versionName` artık elle düzenlenmez:

| Değişken | Kaynak | Yerel fallback |
|---|---|---|
| `VERSION_CODE` | CI build numarası (`github.run_number`) | `1` |
| `VERSION_NAME` | Git tag (`v1.2.3` → `1.2.3`) | `1.0.0` |

Yeni sürüm yayınlamak için etiket at:

```bash
git tag v1.1.0
git push origin v1.1.0
```

`versionCode`'un **her yayında artması zorunludur**. Artmazsa cihazlar
güncellemeyi sessizce reddeder.

Yüklü sürüm, uygulama içinde Ayarlar ekranının en altında görünür — destek
taleplerinde bu değeri isteyin.
