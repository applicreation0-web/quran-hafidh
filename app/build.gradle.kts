plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val verifyFrozenReminderSnapshot by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_frozen_reminders.py")
}

val verifyMushafPages by tasks.registering {
    doLast {
        val mushafDir = file("src/main/assets/mushaf/hafs/kfqc/svg-br")
        val missing = (1..604).filter { page ->
            !mushafDir.resolve("%03d.svg.br".format(page)).isFile
        }

        check(missing.isEmpty()) {
            "Missing Medina Mushaf Brotli pages: " +
                missing.take(10).joinToString() +
                ". Run scripts/fetch_mushaf_pages.sh before building."
        }

        check(mushafDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{3}\\.svg\\.br"))
        }?.size == 604) {
            "The embedded Hafs/KFQC Mushaf must contain exactly 604 canonical pages."
        }
    }
}

val verifyPrivacyBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val accessibility = file("src/main/res/xml/accessibility_service_config.xml").readText()

        check(!manifest.contains("android.permission.INTERNET")) {
            "Quran Safeguard must remain offline: INTERNET permission is forbidden."
        }
        check(!manifest.contains("android.permission.QUERY_ALL_PACKAGES")) {
            "Broad package visibility is forbidden; keep launcher-scoped queries only."
        }
        check(accessibility.contains("android:canRetrieveWindowContent=\"false\"")) {
            "Accessibility window content retrieval must remain disabled."
        }
        check(!accessibility.contains("typeViewFocused")) {
            "Focused-view accessibility events are outside Safeguard scope."
        }
        check(
            accessibility.contains(
                "android:accessibilityEventTypes=\"typeWindowStateChanged|typeWindowsChanged\""
            )
        ) {
            "Accessibility events must remain limited to window changes."
        }
    }
}

android {
    namespace = "com.applicreation0.quransafeguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.applicreation0.quransafeguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "0.8.3"
    }

    buildFeatures {
        compose = true
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyFrozenReminderSnapshot)
    dependsOn(verifyMushafPages)
    dependsOn(verifyPrivacyBoundary)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.batoulapps.adhan:adhan2:0.0.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
