GITHUB BUILD KULLANIMI
=====================

1) Bu ZIP'i bilgisayarda/telefonda açın.
2) ZIP'in içindeki TÜM dosya ve klasörleri GitHub reposunun KÖKÜNE yükleyin.
   Örnek kök yapı:
     .github/workflows/build.yml
     app/
     build.gradle.kts
     settings.gradle.kts
     gradle.properties
     README.md
3) GitHub'da Actions > Build APK bölümüne girin.
4) Run workflow > Run workflow seçin.
5) İşlem yeşil olduğunda çalışmanın Summary sayfasının altındaki Artifacts bölümünden
   LockscreenNotificationTouchBlocker-v1.0.1-debug dosyasını indirin.
6) Artifact ZIP'ini açın; içindeki app-debug.apk dosyasını kurun.
7) LSPosed > Modules bölümünde modülü etkinleştirin ve scope olarak yalnızca
   System UI (com.android.systemui) seçin.
8) SystemUI'yi veya telefonu yeniden başlatın.

Not: Bu paket gradlew kullanmaz. GitHub Actions Gradle 8.11.1'i kendisi kurar.
