plugins {
    id("com.android.application")
}

android {
    namespace = "com.sedat.locknotificationtouchblocker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sedat.locknotificationtouchblocker"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
