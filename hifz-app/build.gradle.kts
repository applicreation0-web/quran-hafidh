plugins {
    id("com.android.application")
}

android {
    namespace = "com.quransafeguard.hifz.test"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz.test"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1-parallel-test"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":hifz-core"))
}
