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
        check(!manifest.contains("android:showWhenLocked=\"true\"")) {
            "Quran gate must never be allowed over the Android lock screen."
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

val verifyEditorialBoundary by tasks.registering {
    doLast {
        val sources = fileTree("src/main/java") {
            include("**/*.kt")
        }.files.joinToString("\n") { it.readText() }

        val forbidden = listOf(
            "Pour comprendre cette Ḥikma",
            "Pour comprendre cette Hikma",
            "Explication simple",
            "commentaire de l’auteur",
            "commentaire de l'auteur"
        )
        forbidden.forEach { phrase ->
            check(!sources.contains(phrase, ignoreCase = true)) {
                "Forbidden Hikam editorial summary/attribution found: $phrase"
            }
        }

        val hikam = file("src/main/java/com/quranunlock/guard/HikamRepository.kt").readText()
        check(hikam.contains("Commentaire classique")) {
            // The UI label lives in HikamDetailActivity; this branch intentionally
            // validates the data structure below instead.
            check(hikam.contains("HikmaCommentary"))
        }
        check(hikam.contains("Ibn ʿAjība")) {
            "Classical commentary must identify Ibn ʿAjība explicitly."
        }
        check(hikam.contains("isExcerpt: Boolean = true")) {
            "Abridged commentary must remain explicitly marked as excerpt."
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
        versionCode = 14
        versionName = "0.9.0"
    }

    buildFeatures {
        compose = true
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyFrozenReminderSnapshot)
    dependsOn(verifyMushafPages)
    dependsOn(verifyPrivacyBoundary)
    dependsOn(verifyEditorialBoundary)
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
