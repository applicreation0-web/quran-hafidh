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

        val appBootstrap = file(
            "src/main/java/com/quranunlock/guard/QuranSafeguardApp.kt"
        )
        check(appBootstrap.isFile) {
            "Manifest declares QuranSafeguardApp but its Application class is missing."
        }
        val appBootstrapText = appBootstrap.readText()
        check(manifest.contains("android:name=\".QuranSafeguardApp\"")) {
            "QuranSafeguardApp must remain the process bootstrap."
        }
        check(appBootstrapText.contains("AppMigrations.run(this)")) {
            "Data migrations must run before activities/services use persisted state."
        }

        val requiredManifestComponents = mapOf(
            ".QuranSafeguardApp" to "src/main/java/com/quranunlock/guard/QuranSafeguardApp.kt",
            ".QuranAccessibilityService" to "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt",
            ".DashboardActivity" to "src/main/java/com/quranunlock/guard/DashboardActivity.kt",
            ".MainActivity" to "src/main/java/com/quranunlock/guard/MainActivity.kt",
            ".GateActivity" to "src/main/java/com/quranunlock/guard/GateActivity.kt",
            ".MushafReaderActivity" to "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt",
            ".ReadingCompleteActivity" to "src/main/java/com/quranunlock/guard/ReadingCompleteActivity.kt"
        )
        requiredManifestComponents.forEach { (component, source) ->
            check(manifest.contains("android:name=\"" + component + "\"")) {
                "Required Android component missing from manifest: " + component
            }
            check(file(source).isFile) {
                "Manifest component has no source file: " + component
            }
        }
        val reminderSource = file(
            "src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt"
        ).readText()
        listOf("MindfulReminderReceiver", "ReminderRescheduleReceiver").forEach { receiver ->
            check(manifest.contains("android:name=\"." + receiver + "\"")) {
                "Reminder receiver missing from manifest: " + receiver
            }
            check(reminderSource.contains("class " + receiver + " ")) {
                "Manifest receiver class missing from source: " + receiver
            }
        }

        check(!manifest.contains("android.permission.INTERNET")) {
            "Quran Safeguard must remain offline: INTERNET permission is forbidden."
        }
        check(!manifest.contains("android.permission.QUERY_ALL_PACKAGES")) {
            "Broad package visibility is forbidden; keep launcher-scoped queries only."
        }
        check(!manifest.contains("android.permission.READ_PHONE_STATE")) {
            "Unlock-call freeze must use AudioManager mode, not sensitive phone-state access."
        }
        check(accessibility.contains("android:canRetrieveWindowContent=\"false\"")) {
            "Accessibility window content retrieval must remain disabled."
        }
        check(!accessibility.contains("typeViewFocused")) {
            "Focused-view accessibility events are outside Safeguard scope."
        }
        check(accessibility.contains("android:packageNames=")) {
            "Accessibility must start from an explicit social/browser package scope."
        }
        val excludedStaticPackages = listOf(
            "com.barclays",
            "com.revolut",
            "bitwarden",
            "authenticator2",
            "com.android.phone",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.incallui",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.google.android.apps.wallet",
            "com.google.android.gms"
        )
        excludedStaticPackages.forEach { excluded ->
            check(!accessibility.contains(excluded)) {
                "Excluded package must never appear in Accessibility static scope: " + excluded
            }
        }
        check(!manifest.contains("android:showWhenLocked=\"true\"")) {
            "Quran gate must never be allowed over the Android lock screen."
        }
        check(
            accessibility.contains(
                "android:accessibilityEventTypes=\"typeWindowStateChanged|typeWindowsChanged|typeViewClicked|typeViewScrolled\""
            )
        ) {
            "Accessibility events must remain limited to window ownership plus click/scroll interaction."
        }
        check(accessibility.contains("android:notificationTimeout=\"0\"")) {
            "Unlock accounting requires immediate accessibility transitions; debounce is forbidden."
        }
        check(!accessibility.contains("typeViewTextChanged")) {
            "Text-change accessibility events are forbidden."
        }
    }
}


val verifyUnlockBudgetIntegrity by tasks.registering {
    doLast {
        val service = file(
            "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
        ).readText()
        val prefs = file(
            "src/main/java/com/quranunlock/guard/GuardPrefs.kt"
        ).readText()
        val engine = file(
            "src/main/java/com/quranunlock/guard/UnlockBudgetIntegrity.kt"
        ).readText()
        val tests = file(
            "src/test/java/com/quranunlock/guard/UnlockBudgetIntegrityTest.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(!service.contains("750L")) {
            "The old 750 ms foreground-exit grace period must never return."
        }
        check(service.contains("Settings.Secure.DEFAULT_INPUT_METHOD")) {
            "The active IME must be identified explicitly so typing does not pause the app budget."
        }
        check(service.contains("addOnModeChangedListener")) {
            "Android 12+ must react immediately to call/VoIP audio-mode changes."
        }
        check(service.contains("handleAudioModeChanged")) {
            "Call/VoIP freeze handling is required."
        }
        check(service.contains("pauseForegroundBudget(clearForeground = true)")) {
            "Real app exits and screen-off must pause immediately."
        }
        check(prefs.contains("getInt(UNLOCK_MINUTES, 20)")) {
            "The default per-app unlock budget must remain 20 minutes."
        }
        check(prefs.contains("UNLOCK_FOREGROUND_BOOT_PREFIX")) {
            "Foreground unlock state must be tied to a boot identity."
        }
        check(prefs.contains("UNLOCK_FOREGROUND_CHECKPOINT_PREFIX")) {
            "Foreground unlock state needs a persisted proof-of-life checkpoint."
        }
        check(prefs.contains("reconcileOrphanedUnlockForeground")) {
            "Service/reboot orphan cleanup is mandatory."
        }
        check(engine.contains("shouldFreezeForAudioMode")) {
            "Telephony and VoIP call freeze policy is missing."
        }
        check(engine.contains("shouldGateOnExpiration")) {
            "Exact-zero expiration gate policy is missing."
        }
        check(!manifest.contains("android.permission.READ_PHONE_STATE")) {
            "READ_PHONE_STATE is forbidden for this counter implementation."
        }

        val requiredScenarios = listOf(
            "twentyMinutesFiveUsedLeavesFifteen",
            "twoHoursOutsideAppConsumesNothing",
            "intermittentThreePlusTwoPlusFourConsumesExactlyNine",
            "twoApplicationsHaveIndependentBudgets",
            "keyboardDoesNotCountAsExit",
            "screenOffFreezesBudget",
            "normalPhoneCallFreezesBudget",
            "whatsappVoipCallFreezesBudget",
            "rebootPreservesBudgetAndInvalidatesForegroundSession",
            "serviceKillRestartLeavesNoPhantomForeground",
            "oneHundredRapidTransitionsDoNotDrift",
            "jokerUsesTheSameForegroundAccounting",
            "deselectionReselectionStartsWithoutOldBudget",
            "returnFromGateStartsBudgetEvenWhenForegroundPackageAlreadyMatches",
            "expirationIsExactZeroAndRequiresGateWhenStillForeground",
            "pipAndSplitScreenNeverRequireTwoConcurrentBudgets"
        )
        requiredScenarios.forEach { scenario ->
            check(tests.contains("fun " + scenario + "(")) {
                "Missing release-blocking unlock-budget test: " + scenario
            }
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
            "commentaire de l'auteur",
            "Ibn ʿAṭāʾ Allāh veut dire",
            "Ibn 'Ata' Allah veut dire",
            "Al-Ghazâlî nous enseigne ici",
            "Al-Ghazali nous enseigne ici",
            "ce que l’auteur veut dire",
            "ce que l'auteur veut dire",
            "en d’autres termes",
            "en d'autres termes",
            "Explication de la pensée",
            "Résumé IA",
            "Toutes les Ḥikam",
            "Toutes les Hikam",
            "264 Ḥikam",
            "264 Hikam"
        )
        forbidden.forEach { phrase ->
            check(!sources.contains(phrase, ignoreCase = true)) {
                "Forbidden Hikam editorial summary/attribution found: $phrase"
            }
        }

        val hikam = file("src/main/java/com/quranunlock/guard/HikamRepository.kt").readText()
        val hikamUi = file("src/main/java/com/quranunlock/guard/HikamDetailActivity.kt").readText()
        check(!hikam.contains("transliteration", ignoreCase = true)) {
            "Transliteration is reserved for Adhkar and must not exist in Hikam data."
        }
        check(
            !hikamUi.contains("transliteration", ignoreCase = true) &&
                !hikamUi.contains("translittération", ignoreCase = true)
        ) {
            "Transliteration is reserved for Adhkar and must not appear in Hikam UI."
        }
        val ghazaliUi = file(
            "src/main/java/com/quranunlock/guard/GhazaliDetailActivity.kt"
        ).readText()
        check(
            !ghazaliUi.contains("transliteration", ignoreCase = true) &&
                !ghazaliUi.contains("translittération", ignoreCase = true)
        ) {
            "Transliteration is reserved for Adhkar and must not appear in Ghazali UI."
        }
        check(hikam.contains("HikmaCommentary")) {
            "Canonical Hikam data must retain classical commentary metadata."
        }
        check(hikamUi.contains("Approfondir — commentaire classique")) {
            "Hikma UI must expose the classical commentary explicitly."
        }
        check(hikamUi.contains("Texte du commentateur présenté sans reformulation.")) {
            "Hikma UI must state that the commentator text is not reformulated."
        }
        check(hikam.contains("Ibn ʿAjība")) {
            "Classical commentary must identify Ibn ʿAjība explicitly."
        }
        check(hikam.contains("isExcerpt: Boolean")) {
            "Abridged commentary must retain an explicit excerpt flag."
        }

        val authenticity = file(
            "src/main/java/com/quranunlock/guard/ClassicalAuthenticity.kt"
        ).readText()
        check(authenticity.contains("sourceVerified")) {
            "Classical content must track source verification separately."
        }
        check(authenticity.contains("attributionVerified")) {
            "Classical content must track attribution verification separately."
        }
        check(authenticity.contains("translationVerified")) {
            "Classical content must track translation verification separately."
        }
        check(authenticity.contains("humanVerified")) {
            "Classical content must keep external human-review status distinct."
        }
        check(authenticity.contains("rightsStatus")) {
            "Classical content must track translation rights."
        }

        val ghazali = file("src/main/java/com/quranunlock/guard/GhazaliRepository.kt").readText()
        check(ghazali.contains("اعلم أن للدين شطرين")) {
            "The corrected exact Bidayat al-Hidaya wording must remain locked."
        }
        check(ghazali.contains("Approfondir — contexte dans l’œuvre").not()) {
            "UI wording belongs in GhazaliDetailActivity, not classical source data."
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
        versionCode = 15
        versionName = "0.9.1"
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
    dependsOn(verifyUnlockBudgetIntegrity)
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


tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn("testDebugUnitTest")
    dependsOn(verifyUnlockBudgetIntegrity)
}
