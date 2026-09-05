#!/usr/bin/env python3
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "app/build.gradle.kts"
s = p.read_text(encoding="utf-8")
old = s


def replace_gate(old_text: str, new_text: str, label: str) -> None:
    global s
    if old_text in s:
        s = s.replace(old_text, new_text, 1)
    elif new_text not in s:
        raise SystemExit(f"0.10.5 gate migration could not locate {label}")


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

# The 0.10.4 build gate encoded the old approximation "10 pages = one Hizb".
# 0.10.5 keeps 10/20 pages only as Safeguard quotas while canonical divisions
# live exclusively in QuranStructureMetadata. Migrate the gate itself rather
# than weakening the corrected planner to satisfy obsolete assertions.
replace_gate(
    '''        check(cycle.contains(\n            "INTERVALS_PER_HIZB = CUMULATIVE_MINUTES / INTERVAL_MINUTES"\n        ))\n        check(cycle.contains("MORNING_PAGE_COUNT = 20"))\n        check(cycle.contains("HIZB_PAGE_COUNT = 10"))''',
    '''        check(cycle.contains("INTERVALS_PER_NINETY_MINUTE_CYCLE"))\n        check(cycle.contains("NINETY_MINUTE_PAGE_COUNT = 10"))\n        check(cycle.contains("MORNING_PAGE_COUNT = 20"))\n        check(cycle.contains("HIZB_PAGE_COUNT = NINETY_MINUTE_PAGE_COUNT"))''',
    "90-minute quota constants",
)
replace_gate(
    '''        check(cyclePrefs.contains("sequentialHizbPages"))\n        check(cyclePrefs.contains("hizbCount = 2"))''',
    '''        check(cyclePrefs.contains("sequentialCanonicalQuotaPages"))\n        check(cyclePrefs.contains("QuranSelectionMode.JUZ -> GuardPrefs.selectedJuz"))\n        check(cyclePrefs.contains("QuranSelectionMode.HIZB -> GuardPrefs.selectedHizb"))\n        check(cyclePrefs.contains("plan.pages.all { it in canonicalPool }"))\n        check(cyclePrefs.contains("plan.pages.distinct().size == plan.pages.size"))''',
    "canonical Juz/Hizb planner checks",
)
replace_gate(
    '''        listOf(\n            "juzSixUsesItsExactVerseBoundary",\n            "pageElevenBelongsToBothAdjacentHizb",\n            "selectionIncludesSharedBoundaryPages",\n            "protectionQuotaRemainsTenPagesAndContinuationCanFollow",\n            "shortHizbKeepsTenPageQuotaWithoutHidingRealBoundary",\n            "everyJuzAndHizbHasOrderedValidBounds"\n        ).forEach { scenario ->''',
    '''        listOf(\n            "allThirtyJuzStartsAndPagesMatchCanonicalMetadata",\n            "allSixtyHizbStartsAndPagesMatchCanonicalMetadata",\n            "everyCanonicalDivisionIsGaplessAndNonOverlappingByVerse",\n            "sharedBoundaryPagesRemainVisibleToBothCanonicalSections",\n            "canonicalSelectionIncludesSharedBoundaryPageButNeverOutsideRange",\n            "shortHizbQuotaNeverBorrowsFromNextHizb",\n            "quotaNeverRepeatsPagesWhenSelectedPoolIsSmallerThanRequest"\n        ).forEach { scenario ->''',
    "canonical structure regression names",
)
replace_gate(
    '"oneHundredThousandScopeDecisionsNeverGiveOutsideAppsBudgetOwnership"',
    '"oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility"',
    "privacy stress regression name",
)
replace_gate(
    '''            "onlyCompletedEffectiveIntervalsCountTowardUsage",\n            "singleHizbPoolRepeatsToReachTwentyMorningPages",\n            "multiHizbPoolAdvancesSequentiallyFromSmallest",\n            "everyHizbChallengeUsesExactlyTenPages",\n            "fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds",''',
    '''            "onlyCompletedEffectiveIntervalsCountTowardUsage",\n            "canonicalQuotaDoesNotRepeatShortSelectedPool",\n            "canonicalQuotaAdvancesSequentiallyAcrossSelectedUnits",\n            "ninetyMinuteQuotaNeverCrossesSelectedCanonicalPool",\n            "fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds",''',
    "usage-cycle canonical quota regressions",
)

required = (
    "versionCode = 24",
    'versionName = "0.10.5"',
    'versionName = \\"0.10.5\\"',
    "INTERVALS_PER_NINETY_MINUTE_CYCLE",
    "canonicalQuotaDoesNotRepeatShortSelectedPool",
    "shortHizbQuotaNeverBorrowsFromNextHizb",
    "oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility",
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
    print("Prepared 0.10.5 metadata and canonical release gates")
else:
    print("0.10.5 metadata and canonical release gates already prepared")
