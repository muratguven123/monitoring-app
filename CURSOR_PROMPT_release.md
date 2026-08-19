# Cursor Prompt — Release'e hazırlık

> Tek seferlik görev tarifi. İş bitince silinebilir.
> `CURSOR_PROMPT_proguard.md` ile birlikte yürür; sıralama aşağıda.
> Aşağıdaki bloğu olduğu gibi Cursor'a yapıştır.

---

## Bağlam

Uygulama production'a hazırlanıyor. Şu an durum:

- 146 unit test geçiyor
- Release build çalışıyor, imzalı APK üretiyor, lint temiz
- Gerçek Firebase yapılandırması bağlı
- ProGuard daraltma işi devam ediyor: bir kural kaldırıldı, APK 11.6 → 4.7 MB,
  `mapping.txt` 209 → 50 MB düştü

Kalan dört engel var. Bu görev bunlardan **üçünü** kapatıyor. Dördüncüsü
(Crashlytics teslimat doğrulaması) insan gerektiriyor — aşağıda açıklandı.

---

## Görev 0 — ÖNCE COMMIT ET (her şeyden önce)

172 dosya commit edilmemiş durumda ve sürüm etiketi yok. ProGuard işi devam
ederken bir şey kırılırsa geri dönecek nokta yok.

```bash
git add -A
git commit -m "feat: production hardening, test suite, ProGuard narrowing"
```

**Etiket atma henüz.** Etiket, doğrulama bittikten sonra.

Bundan sonra **her mantıksal adım için ayrı commit** at. Özellikle ProGuard
kuralı kaldırırken: commit mesajına yeni `mapping.txt` ve APK boyutunu yaz.
Bir şey kırılırsa hangi adımın kırdığı git geçmişinden okunmalı.

---

## Görev 1 — Instrumented testleri ilk kez çalıştır

`app/src/androidTest` altında üç DAO testi var (`AlertDaoTest`,
`GrafanaDaoTest`, `NewRelicDaoTest`) ve bunlar **hiç çalıştırılmadı.**

Bir emülatör başlat, sonra:

```bash
./gradlew connectedDebugAndroidTest
```

Bekle: bu testler muhtemelen ilk denemede geçmez. Unit test paketinde de aynısı
oldu — üç ayrı sorun çıktı, üçü de testler hiç çalıştırılmadığı içindi.

Dikkat edilecek noktalar:

- Veritabanı şeması yakın zamanda **version 1'e sıfırlandı**, migration'lar
  kaldırıldı. Testler eski şemaya veya migration'a atıf yapıyorsa güncelle.
- `app/schemas/` altındaki JSON şema dosyaları silindi; Room ilk build'de
  yeniden üretir. Üretilen şema commit edilmeli.
- Hata çıkarsa **testi değil, önce üretim kodunu şüphelen.** Unit test turunda
  hatalardan biri gerçekten üretim kodundaydı (`WorkManager.getInstance` statik
  çağrısı) ve doğru çözüm testi susturmak değil, bağımlılığı enjekte etmekti.

---

## Görev 2 — Kritik akışlar için Compose UI testi yaz

Bu görevin asıl değeri burada. Şu an sıfır UI testi var, ve ProGuard kuralları
daraltıldıkça **her turda elle regresyon turu atmak** gerekiyor. Bunu
otomatikleştir.

`app/src/androidTest` altına, Hilt ile (`@HiltAndroidTest`,
`createAndroidComposeRule`) şu akışlar için test yaz:

| Akış | Doğrulanacak |
|---|---|
| Onboarding → Settings | Grafana adresi ve API anahtarı girilebiliyor, kaydediliyor |
| Grafana adresi doğrulama | Geçersiz adres hata gösteriyor, geçerli adres normalize edilmiş hâlini gösteriyor |
| Home — yapılandırılmamış durum | "Grafana yapılandırılmamış" kartı görünüyor ve Ayarlar'a götürüyor |
| Alerts listesi + filtreler | OPEN / CRITICAL / RESOLVED filtreleri doğru süzüyor |
| App Lock | Kilit açıkken arka plandan dönüşte kilit ekranı geliyor |
| Navigasyon | Tüm ana ekranlar açılıyor, geri tuşu doğru çalışıyor |

Ağ katmanını sahtele (`GrafanaRepository` / `NewRelicRepository` için Hilt test
modülü veya MockWebServer). Testler gerçek sunucuya bağlanmamalı.

Bu testler geçtiğinde, ProGuard kuralı kaldırdıktan sonra `connectedAndroidTest`
çalıştırmak elle turun büyük kısmının yerini tutar.

---

## Görev 3 — Release APK'yı otomatik duman testinden geçir

ProGuard değişiklikleri kodu çalışma zamanında kırar, derlemede değil. Bu yüzden
UI testlerini **release varyantında da** çalıştırabilmek değerli.

`app/build.gradle.kts` içinde release varyantı için test edilebilir bir
yapılandırma kur (`testBuildType` veya minify açık ayrı bir `benchmark`/`staging`
build type). Amaç: R8 uygulanmış APK üzerinde UI testleri koşabilmek.

Bu kurulum zorlayıcıysa, en azından şunu otomatikleştir:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.monitoring.dashboard/.MainActivity
# birkaç saniye bekle, çökme var mı diye logcat'e bak
adb logcat -d -s AndroidRuntime:E | tail -50
```

ve bunu bir script'e (`scripts/release-smoke.sh` veya `.ps1`) al.

---

## Yapamayacakların — bunları "yaptım" deme

Aşağıdakiler insan gerektiriyor. Bunları tamamlanmış gibi raporlama, sadece
kullanıcıya hatırlat:

1. **Crashlytics teslimat doğrulaması.** Firebase konsoluna girip raporun
   ulaştığını görmek gerekiyor. Adımlar `PRODUCTION_READINESS.md` içinde
   "Kalan 3 Adım" §2 başlığı altında.
2. **Keystore parolalarının taşınması.** `keystore.properties` hâlâ proje
   klasöründe ve parola içeriyor. Parola taşımak insan işi.
3. **Gerçek cihazda his/performans kontrolü.** Emülatör her şeyi göstermez.

---

## Başarı ölçütü

- [ ] Çalışan ağaç temiz, her adım ayrı commit
- [ ] `./gradlew testDebugUnitTest` → 146+ test, 0 hata
- [ ] `./gradlew connectedDebugAndroidTest` → geçiyor (ilk kez)
- [ ] Yukarıdaki 6 akış için UI testi var ve geçiyor
- [ ] `./gradlew lintRelease` temiz
- [ ] Release APK kurulup açılıyor, logcat'te `AndroidRuntime:E` yok
- [ ] `app/schemas/` altındaki üretilmiş Room şeması commit edilmiş

Bunlar tamamlandığında sürüm etiketi atılabilir:

```bash
git tag v1.0.0
```
