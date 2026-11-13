# Staj Defteri

Uçak bakım teknisyenliği stajı için günlük takip uygulaması (Android, Kotlin + Jetpack Compose).

- **Günlük:** Her staj günü için yapılan işler, öğrenilenler, çalışma alanı, uçak tipi/tescil, ATA bölümü, referans doküman, süre ve sorumlu teknisyen. Kayıtlarda arama, aylara göre gruplama, gün sayacı.
- **Notlar:** Serbest notlar; arama ve başa sabitleme.
- **Özet:** Hedef gün ilerlemesi, toplam saat, alan / uçak tipi / ATA dağılımı. Defteri metin olarak paylaşma, JSON yedek alma ve geri yükleme.

Veriler yalnızca cihazda (Room veritabanı) saklanır; uygulama internet izni istemez.

## Derleme

Android SDK (platform 36) ve JDK 17+ gerekir.

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
./gradlew testDebugUnitTest
```

Testler uygulamayı Robolectric ile baştan sona çalıştırır ve ekran görüntülerini `app/build/screenshots/` altına yazar.
