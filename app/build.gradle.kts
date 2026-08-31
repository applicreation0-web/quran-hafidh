plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val verifyMushafPages by tasks.registering {
    doLast {
        val mushafDir = file("src/main/assets/mushaf/hafs/kfqc/svg")
        val missing = (1..604).filter { page ->
            !mushafDir.resolve("%03d.svg".format(page)).isFile
        }

        check(missing.isEmpty()) {
            "Missing Medina Mushaf SVG pages: " +
                missing.take(10).joinToString() +
                ". Run scripts/fetch_mushaf_pages.sh before building."
        }

        check(mushafDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{3}\\.svg"))
        }?.size == 604) {
            "The embedded Hafs/KFQC Mushaf must contain exactly 604 canonical pages."
        }
    }
}

android {
    namespace = "com.quranunlock.guard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quranunlock.guard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
    }

    buildFeatures {
        compose = true
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyMushafPages)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
