#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
p = root / "app/build.gradle.kts"
s = p.read_text(encoding="utf-8")
old = s

# 0.10.5 must update every previously distributed private build in place.
s = s.replace("versionCode = 22", "versionCode = 24")
s = s.replace("versionCode = 23", "versionCode = 24")
s = s.replace('versionName = "0.10.3"', 'versionName = "0.10.5"')
s = s.replace('versionName = "0.10.4"', 'versionName = "0.10.5"')
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

# Keep the historical task name because every existing build/release workflow is
# wired to it, but replace its 0.10.4 source-string assumptions with the actual
# 0.10.5 invariants. Behaviour is then exercised by the real Light/Plus unit
# tests immediately after this audit.
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
        val structure = text("src/main/java/com/quranunlock/guard/QuranStructureMetadata.kt")
        val selectionUi = text("src/main/java/com/quranunlock/guard/ReadingSelectionActivity.kt")
        val presenceScope = text("src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt")
        val structureTests = text("src/test/java/com/quranunlock/guard/QuranStructureMetadataTest.kt")
        val usageTests = text("src/test/java/com/quranunlock/guard/UsageCyclePolicyTest.kt")
        val budgetTests = text("src/test/java/com/quranunlock/guard/UnlockBudgetIntegrityTest.kt")
        val stressTests = text("src/test/java/com/quranunlock/guard/TargetPresenceStressTest.kt")
        val reader = text("src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
        val gate = text("src/main/java/com/quranunlock/guard/GateActivity.kt")
        val manifest = text("src/main/AndroidManifest.xml")

        // Privacy and foreground accounting: no broad package/window/usage
        // fallback, and the historical anonymous sentinel is compile-time off.
        listOf(
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.READ_PHONE_STATE"
        ).forEach { forbidden -> check(!manifest.contains(forbidden)) }
        check(!manifest.contains("NotificationListenerService"))
        requireMarker(
            presenceScope,
            "): Boolean = false",
            "Unfiltered Accessibility sentinel must remain compile-time disabled"
        )
        listOf(
            "pauseForegroundBudget(clearForeground = true)",
            "Settings.Secure.DEFAULT_INPUT_METHOD",
            "handleAudioModeChanged"
        ).forEach { requireMarker(service, it, "Missing target-presence safety invariant") }
        listOf(
            "MAX_UNCERTAIN_RECOVERY_CHARGE_MS = 1_000L",
            "boundedRecoveryChargeMs",
            "shouldGateOnExpiration"
        ).forEach { requireMarker(engine, it, "Missing deterministic budget invariant") }

        // 15/90 are literal protected-target presence thresholds. 20/10/1 are
        // Safeguard page quotas only and never define a canonical Juz/Hizb.
        listOf(
            "INTERVAL_MINUTES = 15",
            "CUMULATIVE_MINUTES = 90",
            "INTERVALS_PER_NINETY_MINUTE_CYCLE",
            "MORNING_PAGE_COUNT = 20",
            "NINETY_MINUTE_PAGE_COUNT = 10",
            "HIZB_PAGE_COUNT = NINETY_MINUTE_PAGE_COUNT"
        ).forEach { requireMarker(cycle, it, "Missing 0.10.5 15/90 contract") }
        requireMarker(prefs, "GLOBAL_USAGE_KEY = \"__all_protected_targets__\"", "Shared target budget missing")
        requireMarker(prefs, "currentIntervalTargetPresenceMs", "15-minute presence telemetry missing")
        requireMarker(prefs, "currentCycleTargetPresenceMs", "90-minute presence telemetry missing")
        check(!prefs.contains("getInt(UNLOCK_MINUTES"))

        // Canonical structure is data-driven. Do not gate on a particular
        // French sentence: gate the actual metadata calls used by the UI and
        // planner, then rely on exhaustive tests for all 30/60 boundaries.
        requireMarker(structure, "Tanzil Quran Metadata 1.0", "Canonical Quran metadata authority missing")
        requireMarker(structure, "startsInsidePage", "Mid-page starts must be represented")
        requireMarker(structure, "endsInsidePage", "Mid-page ends must be represented")
        listOf(
            "QuranSelectionMode.JUZ -> GuardPrefs.selectedJuz",
            "QuranSelectionMode.HIZB -> GuardPrefs.selectedHizb",
            "sequentialCanonicalQuotaPages",
            "plan.pages.all { it in canonicalPool }",
            "plan.pages.distinct().size == plan.pages.size"
        ).forEach { requireMarker(cyclePrefs, it, "Canonical planner invariant missing") }
        listOf(
            "QuranStructureMetadata.division(",
            "QuranStructureMetadata.selectionSubtitle(",
            "QuranStructureMetadata.SOURCE_LABEL",
            "QuranSelectionMode.JUZ",
            "QuranSelectionMode.HIZB"
        ).forEach { requireMarker(selectionUi, it, "Selection UI is not bound to canonical metadata") }

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
            requireMarker(structureTests, "fun " + scenario + "(", "Missing canonical structure regression")
        }
        listOf(
            "canonicalQuotaDoesNotRepeatShortSelectedPool",
            "canonicalQuotaAdvancesSequentiallyAcrossSelectedUnits",
            "ninetyMinuteQuotaNeverCrossesSelectedCanonicalPool",
            "fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds"
        ).forEach { scenario ->
            requireMarker(usageTests, "fun " + scenario + "(", "Missing usage-cycle regression")
        }

        // High-volume target-only counter regressions remain release-blocking.
        listOf(
            "thousandsOfTargetBurstsAndOutsideGapsDebitExactlyFifteenMinutes",
            "twoHundredFiftyNinetyMinuteCyclesRemainExactUnderRapidSwitching",
            "oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility",
            "timerAndReadingBoundaryStayStableAcrossOneMillionChecks"
        ).forEach { scenario ->
            requireMarker(stressTests, "fun " + scenario + "(", "Missing target-presence stress regression")
        }
        listOf(
            "chromeThenYoutubeShareOneHardFifteenMinuteLimit",
            "threeProtectedAppsCannotExceedFifteenMinutesTogether",
            "frequentCheckpointsNeverExtendTheSharedInterval",
            "outsideApplicationsAndLongGapsNeverConsumeTargetPresence",
            "targetSwitchesAndOutsideGapsExpireAtExactlyFifteenPresenceMinutes"
        ).forEach { scenario ->
            requireMarker(budgetTests, "fun " + scenario + "(", "Missing hard-limit budget regression")
        }

        // Reading and post-unlock UX must remain connected to the corrected
        // quota engine. Exact behavior is exercised by Light/Plus tests.
        requireMarker(gate, "Filtre matinal • 20 pages", "Morning gate contract missing")
        requireMarker(gate, "Palier de 90 minutes • 10 pages", "90-minute gate contract missing")
        requireMarker(reader, "READING_QUOTA_REACHED", "Reader quota completion signal missing")
        requireMarker(reader, "TargetReturnCoordinator.returnImmediately", "Immediate target return missing")
        requireMarker(prefs, "const val DAILY_JOKERS = 3", "Three daily jokers contract missing")
        requireMarker(prefs, "reconcileOrphanedUnlockForeground", "Foreground recovery missing")
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
    raise SystemExit("0.10.5 could not replace verifyUnlockBudgetIntegrity")

for marker in (
    "versionCode = 24",
    'versionName = "0.10.5"',
    "INTERVALS_PER_NINETY_MINUTE_CYCLE",
    "canonicalQuotaDoesNotRepeatShortSelectedPool",
    "oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility",
    "Unfiltered Accessibility sentinel must remain compile-time disabled",
):
    if marker not in s:
        raise SystemExit(f"0.10.5 preparation missing marker: {marker}")

if s != old:
    p.write_text(s, encoding="utf-8")
    print("Prepared 0.10.5 metadata and functional release audit")
else:
    print("0.10.5 metadata and functional release audit already prepared")
