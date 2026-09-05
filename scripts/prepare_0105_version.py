#!/usr/bin/env python3
from pathlib import Path
import re

p = Path(__file__).resolve().parents[1] / "app/build.gradle.kts"
s = p.read_text(encoding="utf-8")
old = s

# 0.10.4 private APK used versionCode 23. 0.10.5 must therefore be strictly
# higher so it can update both 0.10.3 (22) and 0.10.4 (23) in place.
s = s.replace("versionCode = 22", "versionCode = 24")
s = s.replace("versionCode = 23", "versionCode = 24")
s = s.replace('versionName = "0.10.3"', 'versionName = "0.10.5"')
s = s.replace('versionName = "0.10.4"', 'versionName = "0.10.5"')

# Update source-string guards embedded in the release migration audit.
for previous in ("0.10.3", "0.10.4"):
    s = s.replace(
        f'versionName = \\"{previous}\\"',
        'versionName = \\"0.10.5\\"',
    )
s = s.replace(
    "0.10.3 must use versionCode 22 for an in-place update over 0.10.2.",
    "0.10.5 must use versionCode 24 for an in-place update over 0.10.4.",
)
s = s.replace(
    "0.10.4 must use versionCode 23 for an in-place update over 0.10.3.",
    "0.10.5 must use versionCode 24 for an in-place update over 0.10.4.",
)
s = s.replace(
    "Expected audited personal Plus update 0.10.3.",
    "Expected audited Quran Safeguard update 0.10.5.",
)
s = s.replace(
    "Expected audited Quran Safeguard update 0.10.4.",
    "Expected audited Quran Safeguard update 0.10.5.",
)

# Replace the brittle 0.10.4 unlock audit as one coherent unit. The old task
# encoded obsolete source strings such as “10 pages = one Hizb” and legacy test
# names. 0.10.5 keeps the same task name so all release workflows remain wired,
# but validates the actual current invariants instead of historical wording.
unlock_gate = r'''val verifyUnlockBudgetIntegrity by tasks.registering {
    doLast {
        fun text(path: String): String = file(path).readText()
        fun requireMarker(source: String, marker: String, message: String) {
            check(source.contains(marker)) { message + ": " + marker }
        }

        val service = text("src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt")
        val prefs = text("src/main/java/com/quranunlock/guard/GuardPrefs.kt")
        val engine = text("src/main/java/com/quranunlock/guard/UnlockBudgetIntegrity.kt")
        val cycle = text("src/main/java/com/quranunlock/guard/UsageCyclePolicy.kt")
        val cyclePrefs = text("src/main/java/com/quranunlock/guard/SafeguardCyclePrefs.kt")
        val gate = text("src/main/java/com/quranunlock/guard/GateActivity.kt")
        val reader = text("src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
        val targetReturn = text("src/main/java/com/quranunlock/guard/TargetReturnCoordinator.kt")
        val targetReturnPolicy = text("src/main/java/com/quranunlock/guard/TargetReturnPolicy.kt")
        val targetReturnTests = text("src/test/java/com/quranunlock/guard/TargetReturnPolicyTest.kt")
        val mainUi = text("src/main/java/com/quranunlock/guard/MainActivity.kt")
        val usageTests = text("src/test/java/com/quranunlock/guard/UsageCyclePolicyTest.kt")
        val budgetTests = text("src/test/java/com/quranunlock/guard/UnlockBudgetIntegrityTest.kt")
        val stressTests = text("src/test/java/com/quranunlock/guard/TargetPresenceStressTest.kt")
        val structure = text("src/main/java/com/quranunlock/guard/QuranStructureMetadata.kt")
        val structureTests = text("src/test/java/com/quranunlock/guard/QuranStructureMetadataTest.kt")
        val selectionUi = text("src/main/java/com/quranunlock/guard/ReadingSelectionActivity.kt")
        val presenceScope = text("src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt")
        val manifest = text("src/main/AndroidManifest.xml")

        // Exact target-presence accounting and call/IME safety.
        check(!service.contains("750L")) { "Legacy timing heuristic returned" }
        listOf(
            "Settings.Secure.DEFAULT_INPUT_METHOD",
            "addOnModeChangedListener",
            "isKnownWhatsAppCallActivity",
            "handleAudioModeChanged",
            "pauseForegroundBudget(clearForeground = true)",
            "TARGET_SELECTION_EXPIRED"
        ).forEach { requireMarker(service, it, "Missing accessibility accounting invariant") }
        check(!service.contains("resumeCurrentProtectedPackageIfEligible")) {
            "A call end must not revive a stale protected target"
        }
        listOf(
            "shouldFreezeForAudioMode",
            "MODE_IN_COMMUNICATION",
            "MAX_UNCERTAIN_RECOVERY_CHARGE_MS = 1_000L",
            "boundedRecoveryChargeMs",
            "shouldGateOnExpiration"
        ).forEach { requireMarker(engine, it, "Missing deterministic budget invariant") }
        check(!manifest.contains("android.permission.READ_PHONE_STATE"))
        check(!manifest.contains("NotificationListenerService"))
        check(!manifest.contains("android.permission.QUERY_ALL_PACKAGES"))
        check(!manifest.contains("android.permission.PACKAGE_USAGE_STATS"))

        // 0.10.5 privacy boundary: the historical broad-scope API is retained
        // only for source compatibility and is compile-time disabled.
        requireMarker(
            presenceScope,
            "): Boolean = false",
            "Unfiltered Accessibility sentinel must remain compile-time disabled"
        )

        // 15/90 minutes are target-presence thresholds. 20/10 are Safeguard
        // page quotas only; they never define canonical Juz/Hizb boundaries.
        listOf(
            "INTERVAL_MINUTES = 15",
            "CUMULATIVE_MINUTES = 90",
            "INTERVALS_PER_NINETY_MINUTE_CYCLE",
            "MORNING_PAGE_COUNT = 20",
            "NINETY_MINUTE_PAGE_COUNT = 10",
            "HIZB_PAGE_COUNT = NINETY_MINUTE_PAGE_COUNT"
        ).forEach { requireMarker(cycle, it, "Missing 15/90 usage-cycle contract") }
        requireMarker(prefs, "GLOBAL_USAGE_KEY = \"__all_protected_targets__\"", "Shared target budget missing")
        requireMarker(prefs, "val grantedMs = UsageCyclePolicy.INTERVAL_MS", "Unlock must grant exactly one 15-minute interval")
        requireMarker(prefs, "currentIntervalTargetPresenceMs", "15-minute presence telemetry missing")
        requireMarker(prefs, "currentCycleTargetPresenceMs", "90-minute presence telemetry missing")
        check(!prefs.contains("getInt(UNLOCK_MINUTES")) { "Selectable unlock duration must not return" }

        // Canonical Juz/Hizb pool controls every reading plan.
        listOf(
            "sequentialCanonicalQuotaPages",
            "QuranSelectionMode.JUZ -> GuardPrefs.selectedJuz",
            "QuranSelectionMode.HIZB -> GuardPrefs.selectedHizb",
            "plan.pages.all { it in canonicalPool }",
            "plan.pages.distinct().size == plan.pages.size"
        ).forEach { requireMarker(cyclePrefs, it, "Canonical Quran planner invariant missing") }
        requireMarker(structure, "Tanzil Quran Metadata 1.0", "Canonical Quran metadata authority missing")
        requireMarker(structure, "startsInsidePage", "Mid-page start boundary support missing")
        requireMarker(structure, "endsInsidePage", "Mid-page end boundary support missing")
        requireMarker(selectionUi, "limites réelles des versets", "Canonical-boundary UI wording missing")
        requireMarker(selectionUi, "QuranStructureMetadata.selectionSubtitle", "Selection UI must use canonical metadata")

        listOf(
            "allThirtyJuzStartsAndPagesMatchCanonicalMetadata",
            "allSixtyHizbStartsAndPagesMatchCanonicalMetadata",
            "everyCanonicalDivisionIsGaplessAndNonOverlappingByVerse",
            "sharedBoundaryPagesRemainVisibleToBothCanonicalSections",
            "canonicalSelectionIncludesSharedBoundaryPageButNeverOutsideRange",
            "shortHizbQuotaNeverBorrowsFromNextHizb",
            "fixedQuotaHonoursJuzSelectionAsCanonicalPool",
            "quotaNeverRepeatsPagesWhenSelectedPoolIsSmallerThanRequest"
        ).forEach { scenario ->
            requireMarker(structureTests, "fun " + scenario + "(", "Missing canonical Quran regression test")
        }

        listOf(
            "canonicalQuotaDoesNotRepeatShortSelectedPool",
            "canonicalQuotaAdvancesSequentiallyAcrossSelectedUnits",
            "ninetyMinuteQuotaNeverCrossesSelectedCanonicalPool",
            "fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds",
            "livePresenceJoinsCompletedIntervalsWithoutWallClockTime",
            "ninetyMinutePendingHizbCannotOverflowTheCurrentCycle"
        ).forEach { scenario ->
            requireMarker(usageTests, "fun " + scenario + "(", "Missing 0.10.5 usage-cycle regression test")
        }

        // High-volume counter and privacy regressions.
        listOf(
            "thousandsOfTargetBurstsAndOutsideGapsDebitExactlyFifteenMinutes",
            "twoHundredFiftyNinetyMinuteCyclesRemainExactUnderRapidSwitching",
            "oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility",
            "timerAndReadingBoundaryStayStableAcrossOneMillionChecks"
        ).forEach { scenario ->
            requireMarker(stressTests, "fun " + scenario + "(", "Missing target-presence stress test")
        }
        listOf(
            "chromeThenYoutubeShareOneHardFifteenMinuteLimit",
            "threeProtectedAppsCannotExceedFifteenMinutesTogether",
            "frequentCheckpointsNeverExtendTheSharedInterval",
            "outsideApplicationsAndLongGapsNeverConsumeTargetPresence",
            "targetSwitchesAndOutsideGapsExpireAtExactlyFifteenPresenceMinutes"
        ).forEach { scenario ->
            requireMarker(budgetTests, "fun " + scenario + "(", "Missing hard-limit budget regression test")
        }

        // Reading validation and immediate return to the exact target remain intact.
        requireMarker(gate, "Filtre matinal • 20 pages", "Morning gate label missing")
        requireMarker(gate, "Palier de 90 minutes • 10 pages", "90-minute gate label missing")
        listOf(
            "Valider et avancer",
            "Balayez vers la droite pour avancer",
            "READING_QUOTA_REACHED",
            "Quota atteint • sortie libre • lecture facultative",
            "Débloquer et ouvrir",
            "TargetReturnCoordinator.returnImmediately"
        ).forEach { requireMarker(reader, it, "Reader/unlock invariant missing") }
        requireMarker(targetReturnPolicy, "REVEAL_EXISTING_TASK", "Target-return policy changed")
        requireMarker(targetReturn, "FLAG_ACTIVITY_RESET_TASK_IF_NEEDED", "Target task reveal fallback missing")
        listOf(
            "normalUnlockRevealsTheExactTriggerTaskWithoutRelaunch",
            "missingTriggerTaskUsesLauncherFallback",
            "unavailableTargetOnlyClosesSafeguard",
            "blankTargetCanNeverBeLaunched"
        ).forEach { scenario ->
            requireMarker(targetReturnTests, "fun " + scenario + "(", "Missing target-return regression test")
        }

        requireMarker(mainUi, "Intervalle fixe : 15 minutes", "Fixed 15-minute UI contract missing")
        check(!mainUi.contains("durationChoices")) { "Selectable duration UI must remain removed" }
        requireMarker(prefs, "const val DAILY_JOKERS = 3", "Three daily jokers contract missing")
        requireMarker(prefs, "consumeJokerAndUnlock", "Joker unlock path missing")
        requireMarker(prefs, "reconcileOrphanedUnlockForeground", "Foreground recovery missing")

        val readingHistory = text("src/main/java/com/quranunlock/guard/ReadingHistoryActivity.kt")
        listOf(
            "fun dailyReadingSummary(",
            "fun completedTargetUsageMs(",
            "fun averageReadingMsForWindow("
        ).forEach { requireMarker(prefs, it, "Reading/usage summary invariant missing") }
        requireMarker(readingHistory, "Moyenne 7 jours", "Seven-day reading average UI missing")
    }
}
'''

pattern = re.compile(
    r"val verifyUnlockBudgetIntegrity by tasks\.registering \{.*?\n\}\n\nval verifyProtectedOnlyBoundary",
    flags=re.S,
)
replacement = unlock_gate + "\nval verifyProtectedOnlyBoundary"
if pattern.search(s):
    s = pattern.sub(lambda _: replacement, s, count=1)
elif unlock_gate not in s:
    raise SystemExit("0.10.5 could not replace legacy verifyUnlockBudgetIntegrity block")

required = (
    "versionCode = 24",
    'versionName = "0.10.5"',
    'versionName = \\"0.10.5\\"',
    "INTERVALS_PER_NINETY_MINUTE_CYCLE",
    "canonicalQuotaDoesNotRepeatShortSelectedPool",
    "shortHizbQuotaNeverBorrowsFromNextHizb",
    "oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility",
    "Unfiltered Accessibility sentinel must remain compile-time disabled",
)
for marker in required:
    if marker not in s:
        raise SystemExit(f"0.10.5 preparation missing required marker: {marker}")

for stale in (
    'versionName = \\"0.10.3\\"',
    'versionName = \\"0.10.4\\"',
    '"singleHizbPoolRepeatsToReachTwentyMorningPages"',
    '"everyHizbChallengeUsesExactlyTenPages"',
    '"shortHizbKeepsTenPageQuotaWithoutHidingRealBoundary"',
):
    if stale in s:
        raise SystemExit(f"0.10.5 preparation left stale release guard: {stale}")

if s != old:
    p.write_text(s, encoding="utf-8")
    print("Prepared 0.10.5 metadata and replaced legacy unlock audit with 0.10.5 contract")
else:
    print("0.10.5 metadata and release gates already prepared")
