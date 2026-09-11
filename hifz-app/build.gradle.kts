plugins {
    id("com.android.application")
}

android {
    namespace = "com.quransafeguard.hifz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-local-first"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    implementation("com.caverock:androidsvg-aar:1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
