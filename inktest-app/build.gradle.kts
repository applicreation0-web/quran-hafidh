// Throwaway, standalone test app — NOT part of Quran Hifz or Quran Safeguard.
// Its only purpose: answer one factual question before committing to any real
// architecture change — does ML Kit Digital Ink Recognition's Arabic model
// output tashkil (short vowel diacritics), or only the bare consonant skeleton?
// Deliberately isolated in its own module so it never touches hifz-app's
// offline-first boundary contract (verifyHifzProductBoundary).
plugins {
    id("com.android.application")
}

android {
    namespace = "com.quransafeguard.inktest"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.inktest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-inktest"
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
    }
}

dependencies {
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")
}
