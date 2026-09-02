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
        check(
            reader.contains("override fun onTopResumedActivityChanged") &&
                reader.contains("mayCountActiveReading()")
        ) {
            "Android 10+ multi-window must pause reading unless the reader is top-resumed."
        }
        check(policy.contains("MIN_ACTIVE_READING_MS = 60_000L")) {
            "A page requires at least 60 active seconds."
        }
        check(prefs.contains("ReadingValidationPolicy.canValidate")) {
            "Persistence must enforce the shared 60-second plus progress policy."
        }
        val selectionUi = file(
            "src/main/java/com/quranunlock/guard/ReadingSelectionActivity.kt"
        ).readText()
        check(
            selectionUi.contains("bottomBar =") &&
                selectionUi.contains("Text(\"OK\")") &&
                selectionUi.contains("onClick = { finish() }")
        ) {
            "Juz/Hizb selection must keep a persistent, explicit OK exit."
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
        val protectedApps = file(
            "src/main/java/com/quranunlock/guard/ProtectedApps.kt"
        ).readText()
        val service = file(
            "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
        ).readText()
        val applicationsUi = file(
            "src/main/java/com/quranunlock/guard/ApplicationsActivity.kt"
        ).readText()
        val migrationSource = file(
            "src/main/java/com/quranunlock/guard/AppMigrations.kt"
        ).readText()

        check(manifest.contains("android:name=\".QuranSafeguardApp\""))
        check(migrationSource.contains("class QuranSafeguardApp"))
        check(migrationSource.contains("AppMigrations.run(this)"))
        check(!manifest.contains("android.permission.INTERNET")) {
            "Quran Safeguard must remain offline."
        }
        check(!manifest.contains("android.permission.QUERY_ALL_PACKAGES")) {
            "Broad package visibility is forbidden."
        }
        check(!manifest.contains("android.permission.READ_PHONE_STATE")) {
            "Call exclusion must not require sensitive phone-state access."
        }
        check(!manifest.contains("NotificationListenerService")) {
            "Notification access would add avoidable Android-settings friction."
        }
        check(accessibility.contains("android:canRetrieveWindowContent=\"false\""))
        check(accessibility.contains(
            "android:accessibilityEventTypes=\"typeWindowStateChanged|typeWindowsChanged|typeViewClicked|typeViewScrolled\""
        ))
        check(accessibility.contains("android:notificationTimeout=\"0\""))
        check(!accessibility.contains("typeViewTextChanged"))
        check(!accessibility.contains("typeViewFocused"))

        val browsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser"
        )
        val social = listOf(
            "com.whatsapp",
            "com.twitter.android",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically"
        )
        val targets = browsers + social
        check(targets.size == 14 && targets.toSet().size == 14)
        targets.forEach { packageName ->
            check(accessibility.contains(packageName)) {
                "Missing target from static Accessibility scope: " + packageName
            }
            check(protectedApps.contains(packageName)) {
                "Missing target from fixed product scope: " + packageName
            }
            check(manifest.contains("<package android:name=\"" + packageName + "\" />")) {
                "Missing narrow package visibility declaration: " + packageName
            }
        }

        val removedTargets = listOf(
            "org.telegram.messenger",
            "com.discord",
            "com.reddit.frontpage",
            "com.snapchat.android"
        )
        removedTargets.forEach { packageName ->
            check(!accessibility.contains(packageName))
            check(!protectedApps.contains(packageName))
        }

        val queryBlock = manifest.substringAfter("<queries>").substringBefore("</queries>")
        check(!queryBlock.contains("android.intent.action.MAIN")) {
            "Launcher-wide package discovery is forbidden."
        }
        check(!manifest.contains("android:name=\".SensitiveAppsActivity\""))
        check(!file("src/main/java/com/quranunlock/guard/SensitiveAppsActivity.kt").exists())
        check(!file("src/main/java/com/quranunlock/guard/SensitiveHandoffPolicy.kt").exists())
        check(!protectedApps.contains("looksSensitive"))
        check(!protectedApps.contains("sensitivePackage"))
        check(!protectedApps.contains("launchableAppCache"))
        check(!service.contains("SensitiveHandoffPolicy"))
        check(!service.contains("isSensitiveFlowOrigin"))
        check(!applicationsUi.contains("Vérifier les exclusions"))
        check(applicationsUi.contains("sans liste d’exclusion à maintenir"))

        check(service.contains("info.packageNames = if (broad)"))
        check(service.contains("null"))
        check(service.contains("handleOutsideScopeForeground()"))
        check(service.contains("applyEventPackageScope(broad = false)"))
        check(protectedApps.contains("GuardPrefs.protectedPackages(context) + context.packageName"))
        check(protectedApps.contains("!isSelectableTarget(packageName)"))
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
        val cycle = file(
            "src/main/java/com/quranunlock/guard/UsageCyclePolicy.kt"
        ).readText()
        val cyclePrefs = file(
            "src/main/java/com/quranunlock/guard/SafeguardCyclePrefs.kt"
        ).readText()
        val gate = file(
            "src/main/java/com/quranunlock/guard/GateActivity.kt"
        ).readText()
        val reader = file(
            "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
        ).readText()
        val mainUi = file(
            "src/main/java/com/quranunlock/guard/MainActivity.kt"
        ).readText()
        val tests = file(
            "src/test/java/com/quranunlock/guard/UsageCyclePolicyTest.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(!service.contains("750L"))
        check(service.contains("Settings.Secure.DEFAULT_INPUT_METHOD"))
        check(service.contains("addOnModeChangedListener"))
        check(service.contains("isKnownWhatsAppCallActivity"))
        check(service.contains("handleAudioModeChanged"))
        check(service.contains("pauseForegroundBudget(clearForeground = true)"))
        check(engine.contains("shouldFreezeForAudioMode"))
        check(engine.contains("MODE_IN_COMMUNICATION"))
        check(!manifest.contains("android.permission.READ_PHONE_STATE"))
        check(!manifest.contains("NotificationListenerService"))

        check(cycle.contains("INTERVAL_MINUTES = 15"))
        check(cycle.contains("INTERVALS_PER_HIZB = 6"))
        check(cycle.contains("MORNING_PAGE_COUNT = 20"))
        check(cycle.contains("HIZB_PAGE_COUNT = 10"))
        check(cycle.contains("ChallengeLevel.HIZB"))
        check(prefs.contains("GLOBAL_USAGE_KEY = \"__all_protected_targets__\""))
        check(prefs.contains("val grantedMs = UsageCyclePolicy.INTERVAL_MS"))
        check(!prefs.contains("getInt(UNLOCK_MINUTES"))
        check(cyclePrefs.contains("SafeguardCyclePrefs"))
        check(cyclePrefs.contains("sequentialHizbPages"))
        check(cyclePrefs.contains("hizbCount = 2"))
        check(gate.contains("Filtre matinal • 20 pages"))
        check(gate.contains("Palier de 90 minutes • 10 pages"))
        check(reader.contains("Valider et passer à la page suivante"))
        check(mainUi.contains("Intervalle fixe : 15 minutes"))
        check(!mainUi.contains("durationChoices"))

        check(prefs.contains("const val DAILY_JOKERS = 3"))
        check(prefs.contains("consumeJokerAndUnlock"))
        check(prefs.contains("appendUsageIntervalGrant(editor, packageName)"))
        check(cyclePrefs.contains("onChallengeCompleted(editor)"))
        check(gate.contains("prochain intervalle 15 min"))
        check(
            service.contains("10 to \"Il vous reste 10 min") &&
                service.contains("5 to \"Encore 5 min") &&
                service.contains("1 to \"Dernière minute")
        )

        check(prefs.contains("UNLOCK_FOREGROUND_BOOT_PREFIX"))
        check(prefs.contains("UNLOCK_FOREGROUND_CHECKPOINT_PREFIX"))
        check(prefs.contains("reconcileOrphanedUnlockForeground"))
        check(engine.contains("MAX_UNCERTAIN_RECOVERY_CHARGE_MS = 1_000L"))
        check(prefs.contains("boundedRecoveryChargeMs"))
        check(engine.contains("shouldGateOnExpiration"))

        val requiredScenarios = listOf(
            "morningFilterIsAlwaysTheFirstDailyRequirement",
            "firstFiveIntervalsRequireOnePageAndSixthRequiresHizb",
            "cumulativeHizbAbsorbsTheCoincidentSixthMicroBlock",
            "completingHizbResetsEntireNinetyMinuteCycle",
            "jokerCanSkipMorningMicroAndHizbLevels",
            "onlyCompletedEffectiveIntervalsCountTowardUsage",
            "singleHizbPoolRepeatsToReachTwentyMorningPages",
            "multiHizbPoolAdvancesSequentiallyFromSmallest",
            "everyHizbChallengeUsesExactlyTenPages"
        )
        requiredScenarios.forEach { scenario ->
            check(tests.contains("fun " + scenario + "(")) {
                "Missing release-blocking 15/90 cycle test: " + scenario
            }
        }

        val readingHistory = file(
            "src/main/java/com/quranunlock/guard/ReadingHistoryActivity.kt"
        ).readText()
        check(
            prefs.contains("fun dailyReadingSummary(") &&
                prefs.contains("fun completedTargetUsageMs(") &&
                prefs.contains("fun averageReadingMsForWindow(") &&
                readingHistory.contains("Moyenne 7 jours")
        )
    }
}


val verifyProtectedOnlyBoundary by tasks.registering {
    doLast {
        val protectedApps = file(
            "src/main/java/com/quranunlock/guard/ProtectedApps.kt"
        ).readText()
        val service = file(
            "src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
        ).readText()
        val setupUi = file(
            "src/main/java/com/quranunlock/guard/ProtectionSetupActivity.kt"
        ).readText()
        val mainUi = file(
            "src/main/java/com/quranunlock/guard/MainActivity.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val appCatalog = file(
            "src/main/java/com/quranunlock/guard/AppCatalog.kt"
        ).readText()
        val prefs = file(
            "src/main/java/com/quranunlock/guard/GuardPrefs.kt"
        ).readText()
        val applicationsUi = file(
            "src/main/java/com/quranunlock/guard/ApplicationsActivity.kt"
        ).readText()
        val dashboard = file(
            "src/main/java/com/quranunlock/guard/DashboardActivity.kt"
        ).readText()

        check(!file("src/main/java/com/quranunlock/guard/SensitiveAppsActivity.kt").exists())
        check(!file("src/main/java/com/quranunlock/guard/SensitiveHandoffPolicy.kt").exists())
        check(!file("src/test/java/com/quranunlock/guard/SensitiveHandoffPolicyTest.kt").exists())
        check(!protectedApps.contains("isAlwaysAllowed"))
        check(!protectedApps.contains("looksSensitive"))
        check(!service.contains("SensitiveHandoffPolicy"))
        check(!service.contains("sensitiveFlow"))
        check(!manifest.contains("SensitiveAppsActivity"))
        check(protectedApps.contains("fun isProtected(context: Context, packageName: String): Boolean"))
        check(protectedApps.contains("!isSelectableTarget(packageName)"))
        check(service.contains("applyEventPackageScope(broad = true)"))
        check(service.contains("handleOutsideScopeForeground()"))
        check(service.contains("applyEventPackageScope(broad = false)"))
        check(appCatalog.contains("ProtectedApps.selectableTargets.mapNotNull"))
        check(appCatalog.contains("cachedLaunchableTargets"))
        check(applicationsUi.contains("AppCatalog.refresh()"))
        check(prefs.contains("it in installedTargets"))
        check(!prefs.contains("UNINSTALL_CHALLENGE_KEY"))
        check(dashboard.contains("joker(s) utilisé(s) aujourd’hui"))

        check(manifest.contains("android:name=\".ProtectionSetupActivity\"")) {
            "Guided accessibility activation must be packaged."
        }
        check(setupUi.contains("Activer en trois étapes")) {
            "Accessibility activation must remain didactic and lightweight."
        }
        val accessibilityServiceDeclaration = Regex(
            """(?s)<service\b[^>]*android:name="\.QuranAccessibilityService"[^>]*>"""
        ).find(manifest)?.value.orEmpty()
        check(
            accessibilityServiceDeclaration.contains("android:exported=\"true\"") &&
                accessibilityServiceDeclaration.contains(
                    "android.permission.BIND_ACCESSIBILITY_SERVICE"
                )
        ) {
            "Android must be able to discover and bind the protected Accessibility service."
        }
        check(
            setupUi.contains("Settings.ACTION_ACCESSIBILITY_SETTINGS") &&
                setupUi.contains("Settings.ACTION_APPLICATION_DETAILS_SETTINGS") &&
                setupUi.contains("Paramètre restreint") &&
                !setupUi.contains("android.settings.ACCESSIBILITY_DETAILS_SETTINGS") &&
                !setupUi.contains("Intent.EXTRA_COMPONENT_NAME")
        ) {
            "Guided activation must use portable Android Settings routes and explain restricted settings."
        }
        check(!mainUi.contains("Settings.ACTION_ACCESSIBILITY_SETTINGS"))
        check(!mainUi.contains("Settings.ACTION_APPLICATION_DETAILS_SETTINGS"))
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
        check(buildFile.contains("versionCode = 19")) {
            "0.10.0 must keep versionCode 19, above the 0.9.4 candidate."
        }
        check(buildFile.contains("versionName = \"0.10.0\"")) {
            "Expected point-1/point-2 candidate versionName 0.10.0."
        }
        check(migrations.contains("CURRENT_SCHEMA = 8")) {
            "The protected-only shared-cycle model requires schema 8."
        }
        check(migrations.contains("migrateToSchema8(context)")) {
            "Schema 8 migration must be wired into AppMigrations.run."
        }
        check(migrations.contains("user_always_allowed_packages")) {
            "Schema 8 must purge legacy application-exclusion state."
        }
        check(migrations.contains("unlock_minutes")) {
            "Schema 8 must purge the obsolete selectable-duration preference."
        }
        check(migrations.contains("reconcileOrphanedUnlockForeground(context)")) {
            "Migration must reconcile stale foreground state."
        }
        check(manifest.contains("android:allowBackup=\"false\"")) {
            "Private app state must not be restored from Android backup."
        }
        check(!manifest.contains("android.permission.BIND_DEVICE_ADMIN")) {
            "Quran Safeguard must remain freely uninstallable."
        }
        val signingAudit = rootProject.file(
            "docs/RELEASE_SIGNING_CONTINUITY.md"
        ).readText()
        check(
            signingAudit.contains("6C:70:6F:4E") &&
                signingAudit.contains("Quran-Safeguard-release.p12") &&
                signingAudit.contains("Never commit")
        ) {
            "The retained release signing lineage and key-handling rule must remain explicit."
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
        check(adhkarUi.contains("Crossfade(")) {
            "Morning/evening changes require a calm in-app transition."
        }
        check(hikamUi.contains("AnimatedVisibility(")) {
            "Classical commentary expansion requires a calm in-app transition."
        }
        val safeguardDesign = file(
            "src/main/java/com/quranunlock/guard/SafeguardDesign.kt"
        ).readText()
        check(
            safeguardDesign.contains("sahelianButtonOrnament") &&
                safeguardDesign.contains("drawDiamond") &&
                safeguardDesign.contains("chevron")
        ) {
            "Safeguard buttons must retain their Sahelian/oriental contour."
        }
        val launcherIcon = file(
            "src/main/res/drawable/ic_launcher_foreground.xml"
        ).readText()
        listOf("#214B3B", "#B9873E", "#FFFDF5").forEach { brandColor ->
            check(launcherIcon.contains(brandColor)) {
                "Launcher icon lost a required green/gold/cream brand color: " + brandColor
            }
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
        ((1..15) + (17..20)).forEach { sourceNumber ->
            check(hikam.contains(sourceNumber.toString() + " to ajibaCommentary(")) {
                "Verified Ibn ʿAjība commentary missing for Hikma " + sourceNumber
            }
        }
        check(!hikam.contains("16 to ajibaCommentary(")) {
            "Hikma 16 commentary must remain withheld until its source boundary is resolved."
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
    dependsOn(verifyProtectedOnlyBoundary)
    dependsOn(verifyThoughtOfDayBoundary)
}

android {
    namespace = "com.applicreation0.quransafeguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.applicreation0.quransafeguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "0.10.0"
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
    dependsOn(verifyProtectedOnlyBoundary)
    dependsOn(verifyThoughtOfDayBoundary)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
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
