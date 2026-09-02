plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val verifyFrozenReminderSnapshot by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_frozen_reminders.py")
}

val verifyHikam264 by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_hikam_264.py")
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

        val reader = file(
            "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
        ).readText()
        val prefs = file(
            "src/main/java/com/quranunlock/guard/GuardPrefs.kt"
        ).readText()
        val policy = file(
            "src/main/java/com/quranunlock/guard/ReadingValidationPolicy.kt"
        ).readText()
        val policyTests = file(
            "src/test/java/com/quranunlock/guard/ReadingValidationPolicyTest.kt"
        ).readText()

        check(!reader.contains("0.80f") && !reader.contains("0.78f")) {
            "Legacy 80% Mushaf viewport logic must never return."
        }
        check(reader.contains(".weight(1f)")) {
            "The canonical Mushaf page must occupy the full remaining reader viewport."
        }
        check(policy.contains("MIN_ACTIVE_READING_MS = 60_000L")) {
            "A page requires at least 60 active seconds."
        }
        check(prefs.contains("ReadingValidationPolicy.canValidate")) {
            "Persistence must enforce the shared 60-second plus progress policy."
        }
        listOf(
            "fiftyNineSecondsCannotValidateEvenAtBottom",
            "sixtyActiveSecondsAndBottomCanValidate",
            "sixtySecondsWithoutPageProgressCannotValidate",
            "pausedTimeCannotBeInventedByValidationPolicy",
            "fullyVisiblePageDoesNotRequireScroll",
            "minorWebViewRoundingDoesNotCreateFakeScrollRequirement",
            "overflowingPageRequiresNaturalScroll"
        ).forEach { scenario ->
            check(policyTests.contains("fun " + scenario + "(")) {
                "Missing release-blocking reading validation test: " + scenario
            }
        }
    }
}

val verifyPrivacyBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val accessibility = file("src/main/res/xml/accessibility_service_config.xml").readText()

        val migrationSource = file(
            "src/main/java/com/quranunlock/guard/AppMigrations.kt"
        ).readText()
        check(manifest.contains("android:name=\".QuranSafeguardApp\"")) {
            "QuranSafeguardApp must remain the process bootstrap."
        }
        check(migrationSource.contains("class QuranSafeguardApp")) {
            "Manifest declares QuranSafeguardApp but the Application class is missing."
        }
        check(migrationSource.contains("AppMigrations.run(this)")) {
            "Data migrations must run before activities/services use persisted state."
        }

        val requiredManifestComponents = mapOf(
            ".QuranAccessibilityService" to "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt",
            ".DashboardActivity" to "src/main/java/com/quranunlock/guard/DashboardActivity.kt",
            ".MainActivity" to "src/main/java/com/quranunlock/guard/MainActivity.kt",
            ".GateActivity" to "src/main/java/com/quranunlock/guard/GateActivity.kt",
            ".MushafReaderActivity" to "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
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

        val browserDetector = file(
            "src/main/java/com/quranunlock/guard/BrowserDetector.kt"
        ).readText()
        val protectedAppsSource = file(
            "src/main/java/com/quranunlock/guard/ProtectedApps.kt"
        ).readText()
        val requiredBrowsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser"
        )
        check(requiredBrowsers.size == 8 && requiredBrowsers.toSet().size == 8) {
            "Safeguard must keep exactly eight declared browsers."
        }
        requiredBrowsers.forEach { browser ->
            check(
                accessibility.contains(browser) &&
                    browserDetector.contains(browser) &&
                    protectedAppsSource.contains(browser)
            ) {
                "Required browser missing from fixed Safeguard scope: " + browser
            }
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

        val settingsUi = file(
            "src/main/java/com/quranunlock/guard/MainActivity.kt"
        ).readText()
        val consentAction = settingsUi
            .substringAfter("onAccept = {")
            .substringBefore("onLater =")
        check(!consentAction.contains("openAccessibilitySettings()")) {
            "Accepting the disclosure must open Safeguard settings, not Android Accessibility."
        }
        check(settingsUi.contains("Activer la protection via Android")) {
            "Android Accessibility must remain a separate, explicit activation action."
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
        check(service.contains("isKnownWhatsAppCallActivity")) {
            "WhatsApp call UI fallback must complement AudioManager VoIP detection."
        }
        check(service.contains("packageName != \"com.whatsapp\" && whatsappCallUiActive")) {
            "WhatsApp call UI fallback must clear when another package owns the window."
        }
        check(service.contains("handleAudioModeChanged")) {
            "Call/VoIP freeze handling is required."
        }
        check(service.contains("if (packageName == this.packageName)")) {
            "Safeguard UI must explicitly narrow broad accessibility observation."
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
        check(engine.contains("MAX_UNCERTAIN_RECOVERY_CHARGE_MS = 1_000L")) {
            "Unknown service-death downtime must be bounded to one checkpoint interval."
        }
        check(prefs.contains("boundedRecoveryChargeMs")) {
            "Persistence layer must use bounded orphan recovery accounting."
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
        check(!prefs.contains("legacyRemaining")) {
            "Boot-unsafe legacy elapsedRealtime windows must never be converted into fresh budget."
        }
        check(prefs.contains("putLong(key, 0L)")) {
            "Legacy unlock state must fail closed and require a fresh Quran reading."
        }

        val requiredScenarios = listOf(
            "twentyMinutesFiveUsedLeavesFifteen",
            "twoHoursOutsideAppConsumesNothing",
            "intermittentThreePlusTwoPlusFourConsumesExactlyNine",
            "twoApplicationsHaveIndependentBudgets",
            "keyboardDoesNotCountAsExit",
            "screenOffFreezesBudget",
            "normalPhoneCallFreezesBudget",
            "whatsappCallUiFreezesBeforeAudioMode",
            "whatsappVoipCallFreezesBudget",
            "legacy09ForegroundMarkerIsInvalidatedWithoutReusingTimestamp",
            "rebootPreservesBudgetAndInvalidatesForegroundSession",
            "unknownBootCountCanNeverBeTreatedAsSameBootRecovery",
            "serviceKillRestartLeavesNoPhantomForeground",
            "longServiceDeathGapCanChargeAtMostOneCheckpointInterval",
            "shortServiceDeathGapChargesOnlyTheObservedTail",
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


val verifySensitiveAppBoundary by tasks.registering {
    doLast {
        val protectedApps = file(
            "src/main/java/com/quranunlock/guard/ProtectedApps.kt"
        ).readText()
        val service = file(
            "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
        ).readText()
        val policy = file(
            "src/main/java/com/quranunlock/guard/SensitiveHandoffPolicy.kt"
        ).readText()
        val sensitiveUi = file(
            "src/main/java/com/quranunlock/guard/SensitiveAppsActivity.kt"
        ).readText()
        val setupUi = file(
            "src/main/java/com/quranunlock/guard/ProtectionSetupActivity.kt"
        ).readText()
        val mainUi = file(
            "src/main/java/com/quranunlock/guard/MainActivity.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val tests = file(
            "src/test/java/com/quranunlock/guard/SensitiveHandoffPolicyTest.kt"
        ).readText()

        check(protectedApps.contains("if (isAlwaysAllowed(context, packageName)) return false")) {
            "Always-accessible banking/identity apps must fail open before any protection decision."
        }
        check(protectedApps.contains("userAlwaysAllowedPackages(context)")) {
            "Unknown local banks require an explicit always-accessible fallback."
        }
        check(service.contains("ProtectedApps.isSensitiveFlowOrigin")) {
            "Sensitive app origins must be recognized before normal gate handling."
        }
        check(service.contains("SensitiveHandoffPolicy.shouldAllowHandoff")) {
            "Bank authentication/settings handoff policy is not wired into accessibility events."
        }
        check(policy.contains("HANDOFF_WINDOW_MS = 120_000L")) {
            "Sensitive handoff must remain time-bounded."
        }
        check(policy.contains("customtab") && policy.contains("webauthn")) {
            "Browser authentication windows must be recognized without exempting ordinary browsing."
        }
        check(policy.contains("isTrustedSensitiveSettingsWindow") &&
            policy.contains("normalized.contains(\"accessibility\")")
        ) {
            "Banking handoff must never exempt Android accessibility settings."
        }
        check(sensitiveUi.contains("Banques, paiements et identité")) {
            "The visible banking exclusion control is missing."
        }
        check(manifest.contains("android:name=\".SensitiveAppsActivity\"")) {
            "SensitiveAppsActivity must be packaged."
        }
        check(manifest.contains("<queries>") && manifest.contains("android.intent.category.LAUNCHER")) {
            "Package visibility is required to show installed banks for manual exclusion."
        }
        check(manifest.contains("android:name=\".ProtectionSetupActivity\"")) {
            "Guided accessibility activation must be packaged."
        }
        check(setupUi.contains("Activer en trois étapes")) {
            "Accessibility activation must remain didactic and lightweight."
        }
        check(
            setupUi.contains("\"android.settings.ACCESSIBILITY_DETAILS_SETTINGS\"") &&
                setupUi.contains("Intent.EXTRA_COMPONENT_NAME")
        ) {
            "Guided activation must open Safeguard's Accessibility detail page directly."
        }
        check(!mainUi.contains("Settings.ACTION_ACCESSIBILITY_SETTINGS")) {
            "Main settings must not jump directly into Android accessibility settings."
        }
        listOf(
            "bankingOriginAndAndroidSettingsAreAllowedDuringLease",
            "accessibilitySettingsRemainProtectedDuringBankingLease",
            "chromeAuthenticationCustomTabIsAllowedDuringLease",
            "ordinaryBrowserWindowIsNeverExempted",
            "expiredLeaseCannotExemptSettingsOrAuthentication",
            "unrelatedAppCannotCreateOrReuseTheException"
        ).forEach { scenario ->
            check(tests.contains("fun " + scenario + "(")) {
                "Missing release-blocking sensitive handoff test: " + scenario
            }
        }
    }
}

val verifyUpdateMigrationIntegrity by tasks.registering {
    doLast {
        val buildFile = file("build.gradle.kts").readText()
        val migrations = file(
            "src/main/java/com/quranunlock/guard/AppMigrations.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(buildFile.contains("applicationId = \"com.applicreation0.quransafeguard\"")) {
            "Application ID must remain unchanged for in-place update."
        }
        check(buildFile.contains("versionCode = 18")) {
            "0.9.4 must keep versionCode 18, above the installed 0.9.3 candidate."
        }
        check(buildFile.contains("versionName = \"0.9.4\"")) {
            "Expected banking/UX candidate versionName 0.9.4."
        }
        check(migrations.contains("CURRENT_SCHEMA = 7")) {
            "0.9.1 must migrate installed schema 6 to schema 7."
        }
        check(migrations.contains("migrateToSchema7(context)")) {
            "Schema 7 migration must be wired into AppMigrations.run."
        }
        check(migrations.contains("migrateToSchema6(context)")) {
            "Schema 7 must reapply the fixed-scope purge to legacy state."
        }
        check(migrations.contains("\"hikam_prefs\"")) {
            "Obsolete Hikam transliteration preference must be purged."
        }
        check(migrations.contains("reconcileOrphanedUnlockForeground(context)")) {
            "Schema 7 must invalidate/reconcile stale foreground unlock state."
        }
        check(manifest.contains("android:allowBackup=\"false\"")) {
            "Private app state must not be restored from Android backup into stale budget state."
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

        val spiritualLibraryUi = file(
            "src/main/java/com/quranunlock/guard/SpiritualLibraryActivity.kt"
        ).readText()
        val dailyReminderSource = file(
            "src/main/java/com/quranunlock/guard/DailyReminder.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(
            !spiritualLibraryUi.contains("LibrarySection.GHAZALI") &&
                !spiritualLibraryUi.contains("onOpenGhazali")
        ) {
            "The incomplete Ghazali section must remain hidden until a sufficient corpus is verified."
        }
        check(!dailyReminderSource.contains("GhazaliRepository.asDailyReminders()")) {
            "Incomplete Ghazali content must not enter daily reminders."
        }
        check(!manifest.contains("android:name=\".GhazaliDetailActivity\"")) {
            "The incomplete Ghazali detail route must not be packaged."
        }

        val adhkarUi = file(
            "src/main/java/com/quranunlock/guard/AdhkarActivity.kt"
        ).readText()
        check(
            adhkarUi.contains("AdhkarPeriod.MORNING") &&
                adhkarUi.contains("AdhkarPeriod.EVENING") &&
                adhkarUi.contains("Text(\"Matin\")") &&
                adhkarUi.contains("Text(\"Soir\")")
        ) {
            "Morning and evening Adhkar must both be selectable in-app."
        }
        check(hikam.contains("HikmaCommentary")) {
            "Canonical Hikam data must retain classical commentary metadata."
        }
        check(hikam.contains("additionalCommentaries") && hikam.contains("val commentaries: List<HikmaCommentary>")) {
            "Hikam must support multiple commentators as separate source units."
        }
        check(hikamUi.contains("Approfondir — commentaire classique")) {
            "Hikma UI must expose the classical commentary explicitly."
        }
        check(hikamUi.contains("Texte du commentateur présenté sans reformulation.")) {
            "Hikma UI must state that the commentator text is not reformulated."
        }
        check(hikamUi.contains("commentary.source.author") && hikamUi.contains("commentary.source.workTitle")) {
            "Each commentary card must display its own author and work."
        }
        check(hikamUi.contains("Aucune synthèse entre commentateurs.")) {
            "Cross-commentator synthesis must be explicitly forbidden in the UI contract."
        }
        check(hikam.contains("Ibn ʿAjība")) {
            "Existing verified Ibn ʿAjība commentary must remain identified explicitly."
        }
        (1..13).forEach { sourceNumber ->
            check(hikam.contains(sourceNumber.toString() + " to ajibaCommentary(")) {
                "Verified Ibn ʿAjība commentary missing for initial Hikma " + sourceNumber
            }
        }
        check(!hikam.contains("[…]") && !hikam.contains("hasInternalOmissions = true")) {
            "Production Hikam commentaries must be continuous and free of internal cuts."
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
        check(authenticity.contains("translationAvailable")) {
            "Classical content must record that a French translation is available."
        }
        check(authenticity.contains("humanVerified")) {
            "Classical content must keep external human-review status distinct."
        }
        check(authenticity.contains("rightsStatus")) {
            "Classical content must track translation rights."
        }

        val ghazali = file("src/main/java/com/quranunlock/guard/GhazaliRepository.kt").readText()
        check(ghazali.contains("Ayyuhā al-Walad")) {
            "Issue #31 limits short al-Ghazali reminders to Ayyuha al-Walad."
        }
        check(!ghazali.contains("Bidāyat al-Hidāya") && !ghazali.contains("Iḥyāʾ ʿUlūm al-Dīn")) {
            "Legacy Bidaya/Ihya short-reminder scope must not return before explicit review."
        }
        check(ghazali.contains("GhazaliContextControl")) {
            "Ayyuha excerpts require immediate before/after context-control metadata."
        }
        check(ghazali.contains("continuityChecked = true") &&
            ghazali.contains("nuanceRiskChecked = true")
        ) {
            "Ayyuha excerpts must be checked for continuity and contextual nuance risk."
        }
        check(ghazali.contains("Approfondir — contexte dans l’œuvre").not()) {
            "UI wording belongs in GhazaliDetailActivity, not classical source data."
        }
        check(authenticity.contains("reconstructedOrAssembled")) {
            "Classical text integrity must reject reconstructed/assembled passages."
        }
        check(authenticity.contains("hasInternalOmissions")) {
            "Classical excerpts must track internal omissions explicitly."
        }

        val classicalIntegrityTests = file(
            "src/test/java/com/quranunlock/guard/ClassicalCorpusIntegrityTest.kt"
        ).readText()
        listOf(
            "classicalTextCannotDisplayWithoutArabic",
            "classicalTextCannotDisplayWithoutLocator",
            "classicalSourceRequiresAuthorWorkAndSourceUrl",
            "classicalAttributionIsMandatoryEvenWithTranslation",
            "internalTranslationCanDisplayWithoutHumanVerification",
            "unmarkedInternalOmissionIsRejected",
            "reconstructedClassicalPassageIsRejected"
        ).forEach { scenario ->
            check(classicalIntegrityTests.contains("fun " + scenario + "(")) {
                "Missing release-blocking classical integrity test: " + scenario
            }
        }

        val hikamTests = file(
            "src/test/java/com/quranunlock/guard/HikamRepositoryTest.kt"
        ).readText()
        check(hikamTests.contains("fun sourcedHikmaCanDisplayWithoutUnverifiedCommentary(")) {
            "A sourced Hikma must remain displayable without an unverified commentary."
        }
        check(hikamTests.contains("fun multipleCommentatorsRemainSeparateSourceUnits(")) {
            "Different commentators must remain separate, independently sourced units."
        }

        val classicalFiles = listOf(hikam, ghazali)
        listOf("simpleExplanation", "aiSummary", "meaning").forEach { forbiddenField ->
            classicalFiles.forEach { source ->
                check(!source.contains(forbiddenField, ignoreCase = true)) {
                    "Forbidden AI interpretation field in classical corpus: " + forbiddenField
                }
            }
        }
    }
}


val verifyThoughtOfDayBoundary by tasks.registering {
    doLast {
        val scheduler = file(
            "src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt"
        ).readText()
        val daily = file(
            "src/main/java/com/quranunlock/guard/DailyReminder.kt"
        ).readText()
        val reader = file(
            "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
        ).readText()
        val dashboard = file(
            "src/main/java/com/quranunlock/guard/DashboardActivity.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val thoughtScreen = file(
            "src/main/java/com/quranunlock/guard/ThoughtOfDayActivity.kt"
        ).readText()
        val tests = file(
            "src/test/java/com/quranunlock/guard/ThoughtOfDayPolicyTest.kt"
        ).readText()

        check(daily.contains("REMINDER_HOUR = 20")) {
            "Thought of the day must be scheduled at 20:00 local."
        }
        check(scheduler.contains("markThoughtNotificationIfNeeded")) {
            "Thought notification must be deduplicated per epoch day."
        }
        check(scheduler.contains("ThoughtOfDayActivity::class.java")) {
            "Thought notification must open its dedicated full card."
        }
        check(
            scheduler.contains(
                "putExtra(ThoughtOfDayActivity.EXTRA_REMINDER_ID, thought.id)"
            )
        ) {
            "Thought notification must carry the exact selected reminder ID."
        }
        check(thoughtScreen.contains("ReminderLibrary.byId")) {
            "Thought full card must resolve the reminder ID carried by notification."
        }
        check(daily.contains("FULL_CARD_ACTIVITY_SIMPLE_NAME = \"ThoughtOfDayActivity\"")) {
            "Thought-of-day full-card destination contract is missing."
        }
        check(manifest.contains("android:name=\".ThoughtOfDayActivity\"")) {
            "ThoughtOfDayActivity must be registered."
        }
        check(thoughtScreen.contains("DailyReminderManager.today")) {
            "Thought screen must display the persisted thought selected for the day."
        }
        check(dashboard.contains("DailyReminderManager.today")) {
            "Dashboard must show the same thought all day."
        }
        check(dashboard.contains("ThoughtOfDayActivity::class.java")) {
            "Dashboard thought must open the full card."
        }
        check(!reader.contains("DailyReminderCard") &&
            !reader.contains("DailyReminderManager.today")
        ) {
            "Quran unlock must remain independent of spiritual reminder selection."
        }
        check(reader.contains("completeReadingAndUnlock")) {
            "Validated Quran reading must grant credit directly from the reader."
        }
        check(!reader.contains("ReadingCompleteActivity")) {
            "The obsolete post-reading summary screen must never return."
        }

        listOf(
            "thoughtIsStableForSameDateAndSameCorpus",
            "onlyOneThoughtNotificationIsAllowedPerEpochDay",
            "notificationOpensTheDedicatedFullThoughtCard",
            "thoughtNotificationIsScheduledAtTwentyLocal"
        ).forEach { scenario ->
            check(tests.contains("fun " + scenario + "(")) {
                "Missing release-blocking thought-of-day test: " + scenario
            }
        }
    }
}

val verifyReleaseAudit by tasks.registering {
    dependsOn(verifyFrozenReminderSnapshot)
    dependsOn(verifyHikam264)
    dependsOn(verifyMushafPages)
    dependsOn(verifyPrivacyBoundary)
    dependsOn(verifyEditorialBoundary)
    dependsOn(verifyUnlockBudgetIntegrity)
    dependsOn(verifyUpdateMigrationIntegrity)
    dependsOn(verifySensitiveAppBoundary)
    dependsOn(verifyThoughtOfDayBoundary)
}

android {
    namespace = "com.applicreation0.quransafeguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.applicreation0.quransafeguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 18
        versionName = "0.9.4"
    }

    buildFeatures {
        compose = true
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyFrozenReminderSnapshot)
    dependsOn(verifyHikam264)
    dependsOn(verifyMushafPages)
    dependsOn(verifyPrivacyBoundary)
    dependsOn(verifyEditorialBoundary)
    dependsOn(verifyUnlockBudgetIntegrity)
    dependsOn(verifyUpdateMigrationIntegrity)
    dependsOn(verifySensitiveAppBoundary)
    dependsOn(verifyThoughtOfDayBoundary)
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
    dependsOn(verifyReleaseAudit)
}
