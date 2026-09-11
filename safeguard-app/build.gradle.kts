import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.zip.GZIPInputStream

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

val verify0108ReaderContract by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_0108_reader_contract.py")
}

val verifyEditionIsolation by tasks.registering {
    doLast {
        val mainAssets = file("src/main/assets")
        val forbiddenMainCorpus = fileTree(mainAssets) {
            include("**/*jalalayn*")
            include("**/*tafsir*")
            include("**/*.sqlite")
            include("**/*.db")
            include("**/*.pdf")
        }.files
        check(forbiddenMainCorpus.isEmpty()) {
            "The shared/Light source set must never contain the private tafsir corpus."
        }
        check(file("src/light/java/com/quranunlock/guard/TafsirEdition.kt").readText()
            .contains("isEnabled: Boolean = false"))
        check(file("src/plus/java/com/quranunlock/guard/TafsirEdition.kt").readText()
            .contains("isEnabled: Boolean = true"))
        check(file("src/plus/res/xml/accessibility_service_config.xml").readText()
            .contains("com.applicreation0.quransafeguard.plus"))
        check(file("src/plus/res/values/strings.xml").readText()
            .contains("Quran Safeguard Plus"))
    }
}

val verifyPlusTafsirCorpus by tasks.registering {
    doLast {
        val archiveParts = (0..3).map { index ->
            file("src/plus/assets/tafsir/al_jalalayn_en.sqlite.gz.part%02d".format(index))
        }
        check(archiveParts.all { it.isFile && it.length() > 0L }) {
            "Generate the private Plus tafsir database before building Plus."
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val streams = archiveParts.map { it.inputStream() }
        val archive = SequenceInputStream(
            Collections.enumeration(streams)
        )
        GZIPInputStream(archive).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actualSha256 = digest.digest().joinToString("") {
            "%02x".format(it)
        }
        check(actualSha256 ==
            "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
        ) {
            "The private Plus tafsir database checksum is not approved."
        }
    }
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
            reader.contains("MushafPageWebView(") &&
                reader.contains("onSwipePrevious") &&
                reader.contains("onSwipeNext") &&
                reader.contains("AnimatedContent(") &&
                reader.contains("SafeguardCyclePrefs.currentPlan")
        ) {
            "The reading challenge must support calm swipe navigation across its in-memory plan."
        }
        check(
            reader.contains("index > activeIndex") &&
                reader.contains("displayedIndex < activeIndex") &&
                reader.contains("ReadingValidationPolicy.canValidate")
        ) {
            "Only validated pages may be revisited and forward progress must remain gated."
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
            "Persistence must enforce the shared 60-second active-reading policy."
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
            "sixtyActiveSecondsCanValidate",
            "missingScrollSignalCannotKeepSixtySecondPageLocked",
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
        val presenceScope = file(
            "src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt"
        ).readText()
        val presenceScopeTests = file(
            "src/test/java/com/quranunlock/guard/TargetPresenceScopePolicyTest.kt"
        ).readText()
        val migrationSource = file(
            "src/main/java/com/quranunlock/guard/AppMigrations.kt"
        ).readText()

        check(manifest.contains("android:name=\".QuranSafeguardApp\""))
        check(migrationSource.contains("class QuranSafeguardApp"))
        check(migrationSource.contains("AppMigrations.run(this)"))
        check(manifest.contains("android.permission.INTERNET")) {
            "Private local Al-Husary downloads require Android's normal INTERNET permission."
        }
        val audioController = file(
            "src/main/java/com/quranunlock/guard/QuranAudioController.kt"
        ).readText()
        val audioSource = file(
            "src/main/java/com/quranunlock/guard/QuranAudioSource.kt"
        ).readText()
        check(
            audioController.contains("QuranAudioSource.url") &&
                audioController.contains("downloadSurah(") &&
                audioController.contains("looksLikeMp3") &&
                audioSource.contains("Husary_Muallim_128kbps")
        ) {
            "INTERNET may only support the explicit private local Quran-audio path."
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
        check(applicationsUi.contains("Une application ajoutée est protégée immédiatement."))
        check(applicationsUi.contains("retrait est confirmé puis appliqué le lendemain"))
        check(applicationsUi.contains("Retrait prévu demain"))

        check(service.contains("TargetPresenceScopePolicy.requiresAnonymousExitSentinel"))
        check(service.contains("if (anonymousExitSentinel)"))
        check(service.contains("info.packageNames = if (anonymousExitSentinel)")) {
            "Broad event delivery must remain guarded by the privacy-first sentinel policy."
        }
        check(service.contains("ProtectedApps.eventScopePackages(this).toTypedArray()")) {
            "Runtime accessibility scope must remain on the explicit target list."
        }
        check(presenceScope.contains("): Boolean = false")) {
            "0.10.5 must make broad Accessibility scope impossible in policy."
        }
        check(presenceScopeTests.contains("fun runningSelectedTargetNeverEnablesAnonymousExitSentinel(")) {
            "Missing 0.10.5 privacy-first sentinel regression test."
        }
        check(service.contains("handleOutsideScopeForeground()"))
        val outsideHandler = service
            .substringAfter("private fun handleOutsideScopeForeground()")
            .substringBefore("private fun applyEventPackageScope")
        check(outsideHandler.indexOf("pauseForegroundBudget(clearForeground = true)") in
            0 until outsideHandler.indexOf("GuardRuntime.resetForeground()")
        ) {
            "Any in-scope transition that proves exit must pause target presence before state is cleared."
        }
        check(service.contains("applyEventPackageScope(broad = false)"))
        check(protectedApps.contains("transitionSignalPackages"))
        check(protectedApps.contains("SYSTEM_UI_PACKAGE"))
        check(protectedApps.contains("launcherPackage(context)"))
        check(protectedApps.contains("GuardPrefs.protectedPackages(context)"))
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
        val gestureTests = file(
            "src/test/java/com/quranunlock/guard/ReaderGestureClassifierTest.kt"
        ).readText()
        val targetReturn = file(
            "src/main/java/com/quranunlock/guard/TargetReturnCoordinator.kt"
        ).readText()
        val targetReturnPolicy = file(
            "src/main/java/com/quranunlock/guard/TargetReturnPolicy.kt"
        ).readText()
        val targetReturnTests = file(
            "src/test/java/com/quranunlock/guard/TargetReturnPolicyTest.kt"
        ).readText()
        val mainUi = file(
            "src/main/java/com/quranunlock/guard/MainActivity.kt"
        ).readText()
        val tests = file(
            "src/test/java/com/quranunlock/guard/UsageCyclePolicyTest.kt"
        ).readText()
        val budgetTests = file(
            "src/test/java/com/quranunlock/guard/UnlockBudgetIntegrityTest.kt"
        ).readText()
        val stressTests = file(
            "src/test/java/com/quranunlock/guard/TargetPresenceStressTest.kt"
        ).readText()
        val structure = file(
            "src/main/java/com/quranunlock/guard/QuranStructureMetadata.kt"
        ).readText()
        val structureTests = file(
            "src/test/java/com/quranunlock/guard/QuranStructureMetadataTest.kt"
        ).readText()
        val selectionUi = file(
            "src/main/java/com/quranunlock/guard/ReadingSelectionActivity.kt"
        ).readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(!service.contains("750L"))
        check(service.contains("Settings.Secure.DEFAULT_INPUT_METHOD"))
        check(service.contains("addOnModeChangedListener"))
        check(service.contains("isKnownWhatsAppCallActivity"))
        check(service.contains("handleAudioModeChanged"))
        check(service.contains("pauseForegroundBudget(clearForeground = true)"))
        check(!service.contains("resumeCurrentProtectedPackageIfEligible"))
        check(service.contains("TARGET_SELECTION_EXPIRED"))
        check(service.contains("next real event from a selected target"))
        check(engine.contains("shouldFreezeForAudioMode"))
        check(engine.contains("MODE_IN_COMMUNICATION"))
        check(!manifest.contains("android.permission.READ_PHONE_STATE"))
        check(!manifest.contains("NotificationListenerService"))

        check(cycle.contains("INTERVAL_MINUTES = 15"))
        check(cycle.contains("CUMULATIVE_MINUTES = 90"))
        check(cycle.contains("CUMULATIVE_MS = CUMULATIVE_MINUTES * 60_000L"))
        check(cycle.contains("INTERVALS_PER_NINETY_MINUTE_CYCLE ="))
        check(cycle.contains("CUMULATIVE_MINUTES / INTERVAL_MINUTES"))
        check(cycle.contains("MORNING_PAGE_COUNT = 20"))
        check(cycle.contains("NINETY_MINUTE_PAGE_COUNT = 10"))
        check(cycle.contains("ChallengeLevel.HIZB"))
        check(prefs.contains("GLOBAL_USAGE_KEY = \"__all_protected_targets__\""))
        check(prefs.contains("val grantedMs = UsageCyclePolicy.INTERVAL_MS"))
        check(prefs.contains("currentIntervalTargetPresenceMs"))
        check(prefs.contains("currentCycleTargetPresenceMs"))
        check(!prefs.contains("getInt(UNLOCK_MINUTES"))
        check(cyclePrefs.contains("SafeguardCyclePrefs"))
        check(cyclePrefs.contains("val mode = GuardPrefs.selectionMode(context)"))
        check(cyclePrefs.contains("GuardPrefs.selectedJuz(context)"))
        check(cyclePrefs.contains("GuardPrefs.selectedHizb(context)"))
        check(cyclePrefs.contains("QuranPageSelector.sequentialCanonicalQuotaPages"))
        check(cyclePrefs.contains("plan.pages.distinct().size == plan.pages.size"))
        check(cyclePrefs.contains("plan.pages.all { it in canonicalPool }"))
        check(gate.contains("Filtre matinal • 20 pages"))
        check(gate.contains("Palier de 90 minutes • 10 pages"))
        check(reader.contains("Valider et avancer"))
        check(
            reader.contains("onSwipeNext = {") &&
                reader.contains("ReaderSwipe.NEXT -> currentOnSwipeNext.value()") &&
                reader.contains("ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()")
        ) {
            "Quran reader must retain wired RTL page gestures after helper prose removal."
        }
        check(
            gestureTests.contains("fun swipeRightAdvancesArabicBook(") &&
                gestureTests.contains("fun swipeLeftReturnsToPreviousPage(") &&
                gestureTests.contains("fun verticalScrollIsNotMisclassifiedAsPageTurn(")
        ) {
            "Arabic-book RTL navigation requires right-next/left-previous regression tests."
        }
        check(reader.contains("READING_QUOTA_REACHED"))
        check(
            reader.contains("quotaReached") &&
                reader.contains("Quota atteint • sortie libre") &&
                reader.contains("continueFreely")
        ) {
            "Quota completion must still enter optional free-reading state."
        }
        check(reader.contains("Ouvrir l’application cible"))
        check(reader.contains("continueFreely"))
        check(reader.contains("Débloquer et ouvrir"))
        check(reader.contains("validateAndAdvance(continueAfterQuota = true)"))
        check(reader.contains("TargetReturnCoordinator.returnImmediately"))
        check(gate.contains("TargetReturnCoordinator.returnImmediately"))
        check(targetReturnPolicy.contains("REVEAL_EXISTING_TASK"))
        check(targetReturn.contains("FLAG_ACTIVITY_RESET_TASK_IF_NEEDED"))
        listOf(
            "normalUnlockRevealsTheExactTriggerTaskWithoutRelaunch",
            "missingTriggerTaskUsesLauncherFallback",
            "unavailableTargetOnlyClosesSafeguard",
            "blankTargetCanNeverBeLaunched"
        ).forEach { scenario ->
            check(targetReturnTests.contains("fun " + scenario + "(")) {
                "Missing target-return regression test: " + scenario
            }
        }
        check(structure.contains("Tanzil Quran Metadata 1.0"))
        check(structure.contains("startsInsidePage"))
        check(structure.contains("endsInsidePage"))
        check(selectionUi.contains("limites réelles des versets"))
        check(selectionUi.contains("QuranStructureMetadata.selectionSubtitle"))
        listOf(
            "allThirtyJuzStartsAndPagesMatchCanonicalMetadata",
            "allSixtyHizbStartsAndPagesMatchCanonicalMetadata",
            "everyCanonicalDivisionIsGaplessAndNonOverlappingByVerse",
            "sharedBoundaryPagesRemainVisibleToBothCanonicalSections",
            "canonicalSelectionIncludesSharedBoundaryPageButNeverOutsideRange",
            "shortHizbQuotaNeverBorrowsFromNextHizb",
            "longHizbQuotaMayBeTenPagesButNeverCrossesCanonicalBoundary",
            "fixedQuotaHonoursJuzSelectionAsCanonicalPool",
            "quotaNeverRepeatsPagesWhenSelectedPoolIsSmallerThanRequest"
        ).forEach { scenario ->
            check(structureTests.contains("fun " + scenario + "(")) {
                "Missing Quran structure regression test: " + scenario
            }
        }
        listOf(
            "thousandsOfTargetBurstsAndOutsideGapsDebitExactlyFifteenMinutes",
            "twoHundredFiftyNinetyMinuteCyclesRemainExactUnderRapidSwitching",
            "oneHundredThousandScopeDecisionsNeverGiveOutsideAppsBudgetOwnership",
            "timerAndReadingBoundaryStayStableAcrossOneMillionChecks"
        ).forEach { scenario ->
            check(stressTests.contains("fun " + scenario + "(")) {
                "Missing release-blocking target-presence stress test: " + scenario
            }
        }
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
            "canonicalQuotaDoesNotRepeatShortSelectedPool",
            "canonicalQuotaAdvancesSequentiallyAcrossSelectedUnits",
            "ninetyMinuteQuotaNeverCrossesSelectedCanonicalPool",
            "fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds",
            "livePresenceJoinsCompletedIntervalsWithoutWallClockTime",
            "ninetyMinutePendingHizbCannotOverflowTheCurrentCycle"
        )
        requiredScenarios.forEach { scenario ->
            check(tests.contains("fun " + scenario + "(")) {
                "Missing release-blocking 15/90 cycle test: " + scenario
            }
        }
        listOf(
            "chromeThenYoutubeShareOneHardFifteenMinuteLimit",
            "threeProtectedAppsCannotExceedFifteenMinutesTogether",
            "frequentCheckpointsNeverExtendTheSharedInterval",
            "outsideApplicationsAndLongGapsNeverConsumeTargetPresence",
            "targetSwitchesAndOutsideGapsExpireAtExactlyFifteenPresenceMinutes"
        ).forEach { scenario ->
            check(budgetTests.contains("fun " + scenario + "(")) {
                "Missing multi-target 15-minute hard-limit test: " + scenario
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
        val selectionTests = file(
            "src/test/java/com/quranunlock/guard/ProtectedSelectionPolicyTest.kt"
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
        check(service.contains("info.packageNames = if (anonymousExitSentinel)"))
        check(protectedApps.contains("eventScopePackages"))
        check(protectedApps.contains("transitionSignalPackages"))
        check(appCatalog.contains("ProtectedApps.selectableTargets.mapNotNull"))
        check(appCatalog.contains("cachedLaunchableTargets"))
        check(applicationsUi.contains("AppCatalog.refresh()"))
        check(applicationsUi.contains("Confirmer pour demain"))
        check(applicationsUi.contains("cochez pour annuler"))
        check(prefs.contains("ProtectedSelectionPolicy.reconcile"))
        check(prefs.contains("installedTargets = installedTargets"))
        check(!prefs.contains("UNINSTALL_CHALLENGE_KEY"))
        check(dashboard.contains("joker(s) utilisé(s) aujourd’hui"))
        listOf(
            "addition_is_immediate",
            "removal_stays_active_until_next_day",
            "reselecting_cancels_pending_removal",
            "pending_removal_is_applied_on_next_day",
            "uninstall_is_immediate_and_clears_pending_entry",
            "missing_legacy_effective_day_is_repaired_to_tomorrow"
        ).forEach { scenario ->
            check(selectionTests.contains("fun " + scenario + "(")) {
                "Missing next-day selection regression test: " + scenario
            }
        }

        check(manifest.contains("android:name=\".ProtectionSetupActivity\"")) {
            "Guided accessibility activation must be packaged."
        }
        check(setupUi.contains("Activation guidée")) {
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
        val releaseContract = rootProject.file(
            "docs/SAFEGUARD_CONTRACT_0.10.2.md"
        ).readText()

        check(buildFile.contains("applicationId = \"com.applicreation0.quransafeguard\"")) {
            "Application ID must remain unchanged for in-place update."
        }
        val auditedBaselineMetadata =
            buildFile.contains("versionCode = 22") &&
                buildFile.contains("versionName = \"0.10.3\"")
        val preparedReleaseMetadata =
            (buildFile.contains("versionCode = 28") &&
                buildFile.contains("versionName = \"0.10.9\"")) ||
            (buildFile.contains("versionCode = 29") &&
                buildFile.contains("versionName = \"0.10.10\""))
        check(auditedBaselineMetadata || preparedReleaseMetadata) {
            "Expected the audited baseline or an explicitly prepared 0.10.9/0.10.10 release metadata set."
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
        check(releaseContract.contains(
            "Les 15 minutes sont exactement la somme des durées de présence"
        ))
        check(releaseContract.contains(
            "Les 90 minutes sont six cumuls successifs de 15 minutes"
        ))
        check(releaseContract.contains(
            "L’absence d’un signal de défilement"
        ))
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
        check(!spiritualLibraryUi.contains("commentaire", ignoreCase = true)) {
            "The Hikam library must not mention or expose a commentary layer."
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
                adhkarUi.contains("FilterChip(") &&
                adhkarUi.contains("✓ Matin") &&
                adhkarUi.contains("✓ Soir")
        ) {
            "Morning and evening Adhkar must both be visible and selectable in-app."
        }
        val safeguardDesign = file(
            "src/main/java/com/quranunlock/guard/SafeguardDesign.kt"
        ).readText()
        check(
            !safeguardDesign.contains("sahelianButtonOrnament") &&
                !safeguardDesign.contains("drawDiamond") &&
                !safeguardDesign.contains("chevron") &&
                safeguardDesign.contains("SafeguardReadingSurface = Color(0xFFF7F2E8)") &&
                !safeguardDesign.contains("#F4F0E6") &&
                !safeguardDesign.contains("0xFFF4F0E6")
        ) {
            "0.10.7 requires calm cream controls without ornamental button drawing."
        }
        val launcherIcon = file(
            "src/main/res/drawable/ic_launcher_foreground.xml"
        ).readText()
        listOf("#2C5D49", "#D8BA73", "#7A5337", "#FFFDF5").forEach { baselineColor ->
            check(launcherIcon.contains(baselineColor)) {
                "Launcher icon no longer matches the exact 0.10.8 green/gold/brown/ivory baseline: " + baselineColor
            }
        }
        listOf(
            "HikmaCommentary",
            "additionalCommentaries",
            "commentaryByNumber",
            "Approfondir — commentaire classique",
            "Commentaire classique",
            "Ibn ʿAjība"
        ).forEach { removedCommentarySignal ->
            check(!hikam.contains(removedCommentarySignal) &&
                !hikamUi.contains(removedCommentarySignal)
            ) {
                "Hikam must expose only the Hikma, never a commentary: " +
                    removedCommentarySignal
            }
        }
        check(hikam.contains("arabic_vocalized"))
        check(hikam.contains("vocalization_status"))
        check(hikam.contains("source_aligned_no_automatic_generation"))
        check(hikam.contains("vocalizationSourceUrl"))
        val hikamAsset = file("src/main/assets/hikam/al_hikam_verified.json").readText()
        check(hikamAsset.contains("\"arabic_vocalized\""))
        check(hikamAsset.contains("\"vocalization_status\": \"source_aligned_no_automatic_generation\""))
        check(!hikamAsset.contains("\"commentary\"")) {
            "Bundled Hikam JSON must contain no commentary field."
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
        check(hikamTests.contains("fun verifiedVocalizedHikmaCanDisplay(")) {
            "A verified, vocalized Hikma must remain displayable."
        }
        check(!hikamTests.contains("Commentary") &&
            !hikamTests.contains("commentator", ignoreCase = true)
        ) {
            "Hikam tests must no longer encode a commentary subsystem."
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

val verifyExperienceBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val theme = file("src/main/res/values/themes.xml").readText()
        val dashboard = file(
            "src/main/java/com/quranunlock/guard/DashboardActivity.kt"
        ).readText()
        val design = file(
            "src/main/java/com/quranunlock/guard/SafeguardDesign.kt"
        ).readText()
        val setup = file(
            "src/main/java/com/quranunlock/guard/ProtectionSetupActivity.kt"
        ).readText()
        val reader = file(
            "src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
        ).readText()

        check(manifest.contains("android:theme=\"@style/Theme.QuranSafeguard\""))
        check(theme.contains("android:windowAnimationStyle"))
        listOf(
            "safeguard_open_enter",
            "safeguard_open_exit",
            "safeguard_close_enter",
            "safeguard_close_exit"
        ).forEach { animation ->
            check(file("src/main/res/anim/" + animation + ".xml").isFile) {
                "Missing calm application transition: " + animation
            }
        }

        check(design.contains("fun SafeguardProgressBar("))
        check(dashboard.contains("currentIntervalTargetPresenceMs"))
        check(dashboard.contains("currentCycleTargetPresenceMs"))
        check(dashboard.contains("/ 15:00"))
        check(dashboard.contains("/ 90:00"))
        check(dashboard.contains("Le temps hors cible et les appels ne comptent pas."))
        check(dashboard.indexOf("thought.arabicText") < dashboard.indexOf("thought.frenchText")) {
            "The dashboard thought must present Arabic before French."
        }
        listOf(
            "ic_nav_home",
            "ic_nav_quran",
            "ic_nav_library",
            "ic_nav_settings"
        ).forEach { icon ->
            check(dashboard.contains("R.drawable." + icon))
            check(file("src/main/res/drawable/" + icon + ".xml").isFile)
        }
        check(setup.contains("Build.VERSION_CODES.TIRAMISU"))
        check(setup.contains("Préparer l’autorisation Android"))
        check(setup.contains("Autoriser les paramètres restreints"))
        check(reader.contains("AnimatedContent("))
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
    dependsOn(verifyExperienceBoundary)
    dependsOn(verifyEditionIsolation)
    dependsOn(verify0108ReaderContract)
}

android {
    namespace = "com.applicreation0.quransafeguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.applicreation0.quransafeguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "0.10.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("light") {
            dimension = "edition"
        }
        create("plus") {
            dimension = "edition"
            applicationIdSuffix = ".plus"
            versionNameSuffix = "-plus.1"
        }
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
    dependsOn(verifyExperienceBoundary)
    dependsOn(verifyEditionIsolation)
    dependsOn(verify0108ReaderContract)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    add("plusImplementation", "androidx.webkit:webkit:1.16.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.batoulapps.adhan:adhan2:0.0.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
}


tasks.matching { it.name == "assembleLightRelease" }.configureEach {
    dependsOn("testLightDebugUnitTest")
    dependsOn(verifyReleaseAudit)
}

tasks.matching {
    it.name == "assemblePlusDebug" || it.name == "assemblePlusRelease"
}.configureEach {
    dependsOn(verifyPlusTafsirCorpus)
}
