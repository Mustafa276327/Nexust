plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nexus.browser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nexus.browser"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    // KRİTİK: GitHub Actions her derlemede RASTGELE yeni bir debug anahtarı
    // üretir — bu yüzden eski APK üzerine yenisi "imza uyuşmuyor" hatasıyla
    // KURULAMAZDI. Artık repo'daki sabit anahtarla imzalanıyor; güncellemeler
    // doğrudan üstüne kurulur.
    signingConfigs {
        create("nexus") {
            storeFile = rootProject.file("keystore/nexus.jks")
            storePassword = "nexus1234"
            keyAlias = "nexus"
            keyPassword = "nexus1234"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("nexus")
        }
        debug {
            signingConfig = signingConfigs.getByName("nexus")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
