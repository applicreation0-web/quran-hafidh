plugins {
    id("com.android.application")
}

android {
    namespace = "com.applicreation0.quransafeguard.migrationfixture"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "target"
    productFlavors {
        create("whatsapp") {
            dimension = "target"
            applicationId = "com.whatsapp"
        }
        create("chrome") {
            dimension = "target"
            applicationId = "com.android.chrome"
        }
        create("bank") {
            dimension = "target"
            applicationId = "com.quransafeguard.fixture.bank"
        }
    }
}
