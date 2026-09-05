#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_exact(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Required old block not found in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


build = ROOT / "app/build.gradle.kts"

replace_exact(
    build,
    '''        check(cycle.contains(\n            "INTERVALS_PER_HIZB = CUMULATIVE_MINUTES / INTERVAL_MINUTES"\n        ))''',
    '''        check(cycle.contains("INTERVALS_PER_NINETY_MINUTE_CYCLE"))\n        check(cycle.contains(\n            "INTERVALS_PER_HIZB = INTERVALS_PER_NINETY_MINUTE_CYCLE"\n        ))''',
)

replace_exact(
    build,
    '''        check(cyclePrefs.contains("SafeguardCyclePrefs"))\n        check(cyclePrefs.contains("sequentialHizbPages"))\n        check(cyclePrefs.contains("hizbCount = 2"))\n        check(gate.contains("Filtre matinal • 20 pages"))\n        check(gate.contains("Palier de 90 minutes • 10 pages"))''',
    '''        check(cyclePrefs.contains("SafeguardCyclePrefs"))\n        check(cyclePrefs.contains("sequentialCanonicalQuotaPages"))\n        check(cyclePrefs.contains("pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT"))\n        check(cyclePrefs.contains("pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT"))\n        check(cyclePrefs.contains("plan.pages.distinct().size == plan.pages.size"))\n        check(cyclePrefs.contains("plan.pages.all { it in canonicalPool }"))\n        check(gate.contains("Filtre matinal • jusqu’à 20 pages"))\n        check(gate.contains("Palier de 90 minutes • jusqu’à 10 pages"))''',
)

replace_exact(
    build,
    '''        listOf(\n            "juzSixUsesItsExactVerseBoundary",\n            "pageElevenBelongsToBothAdjacentHizb",\n            "selectionIncludesSharedBoundaryPages",\n            "protectionQuotaRemainsTenPagesAndContinuationCanFollow",\n            "shortHizbKeepsTenPageQuotaWithoutHidingRealBoundary",\n            "everyJuzAndHizbHasOrderedValidBounds"\n        ).forEach { scenario ->''',
    '''        listOf(\n            "allThirtyJuzStartsAndPagesMatchCanonicalMetadata",\n            "allSixtyHizbStartsAndPagesMatchCanonicalMetadata",\n            "everyCanonicalDivisionIsGaplessAndNonOverlappingByVerse",\n            "sharedBoundaryPagesRemainVisibleToBothCanonicalSections",\n            "canonicalSelectionIncludesSharedBoundaryPageButNeverOutsideRange",\n            "shortHizbQuotaNeverBorrowsFromNextHizb",\n            "longHizbQuotaMayBeTenPagesButNeverCrossesCanonicalBoundary",\n            "fixedQuotaHonoursJuzSelectionAsCanonicalPool",\n            "quotaNeverRepeatsPagesWhenSelectedPoolIsSmallerThanRequest"\n        ).forEach { scenario ->''',
)

replace_exact(
    build,
    '"oneHundredThousandScopeDecisionsNeverGiveOutsideAppsBudgetOwnership"',
    '"oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility"',
)

replace_exact(
    build,
    '''            "singleHizbPoolRepeatsToReachTwentyMorningPages",\n            "multiHizbPoolAdvancesSequentiallyFromSmallest",\n            "everyHizbChallengeUsesExactlyTenPages",''',
    '''            "singleHizbPoolStopsAtCanonicalBoundaryWithoutRepeatingPages",\n            "multiHizbPoolAdvancesSequentiallyWithoutLeavingSelectedPool",\n            "ninetyMinuteChallengeUsesUpToTenCanonicalPagesWithoutCrossingBoundary",''',
)

usage_test = ROOT / "app/src/test/java/com/quranunlock/guard/UsageCyclePolicyTest.kt"
usage_test.write_text('''package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCyclePolicyTest {
    @Test
    fun morningFilterIsAlwaysTheFirstDailyRequirement() {
        val state = UsageCycleState()
        assertEquals(ChallengeLevel.MORNING, UsageCyclePolicy.requiredLevel(state))
    }

    @Test
    fun firstFiveIntervalsRequireOnePageAndSixthRequiresHizb() {
        var state = UsageCyclePolicy.completeChallenge(
            UsageCycleState(),
            ChallengeLevel.MORNING
        )

        repeat(5) { index ->
            state = UsageCyclePolicy.onIntervalExpired(state)
            assertEquals(index + 1, state.completedIntervals)
            assertEquals(ChallengeLevel.MICRO, state.pendingLevel)
            state = UsageCyclePolicy.completeChallenge(state, ChallengeLevel.MICRO)
        }

        state = UsageCyclePolicy.onIntervalExpired(state)
        assertEquals(UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE, state.completedIntervals)
        assertEquals(ChallengeLevel.HIZB, state.pendingLevel)
    }

    @Test
    fun cumulativeHizbAbsorbsTheCoincidentSixthMicroBlock() {
        var state = UsageCycleState(morningCompleted = true, completedIntervals = 5)
        state = UsageCyclePolicy.onIntervalExpired(state)
        assertEquals(ChallengeLevel.HIZB, state.pendingLevel)
        assertTrue(state.pendingLevel != ChallengeLevel.MICRO)
    }

    @Test
    fun completingHizbResetsEntireNinetyMinuteCycle() {
        val completed = UsageCyclePolicy.completeChallenge(
            UsageCycleState(
                morningCompleted = true,
                completedIntervals = UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE,
                pendingLevel = ChallengeLevel.HIZB
            ),
            ChallengeLevel.HIZB
        )
        assertEquals(0, completed.completedIntervals)
        assertEquals(1, completed.completedNinetyMinuteCycles)
        assertNull(completed.pendingLevel)
        assertTrue(completed.morningCompleted)
    }

    @Test
    fun jokerCanSkipMorningMicroAndHizbLevels() {
        val morning = UsageCyclePolicy.skipWithJoker(UsageCycleState())
        assertTrue(morning.morningCompleted)

        val micro = UsageCyclePolicy.skipWithJoker(
            UsageCycleState(
                morningCompleted = true,
                completedIntervals = 1,
                pendingLevel = ChallengeLevel.MICRO
            )
        )
        assertEquals(1, micro.completedIntervals)
        assertNull(micro.pendingLevel)

        val hizb = UsageCyclePolicy.skipWithJoker(
            UsageCycleState(
                morningCompleted = true,
                completedIntervals = UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE,
                pendingLevel = ChallengeLevel.HIZB
            )
        )
        assertEquals(0, hizb.completedIntervals)
        assertEquals(1, hizb.completedNinetyMinuteCycles)
        assertNull(hizb.pendingLevel)
    }

    @Test
    fun onlyCompletedEffectiveIntervalsCountTowardUsage() {
        val state = UsageCycleState(
            morningCompleted = true,
            completedIntervals = 3,
            completedNinetyMinuteCycles = 2
        )
        assertEquals(
            (2L * 90L + 3L * 15L) * 60_000L,
            UsageCyclePolicy.completedUsageMs(state)
        )
    }

    @Test
    fun singleHizbPoolStopsAtCanonicalBoundaryWithoutRepeatingPages() {
        val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(15),
            cursor = 0,
            pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
        )
        assertEquals((142..150).toList(), plan.pages)
        assertEquals(9, plan.pages.size)
        assertEquals(plan.pages.size, plan.pages.distinct().size)
        assertEquals(0, plan.nextCursor)
        assertFalse(151 in plan.pages)
    }

    @Test
    fun multiHizbPoolAdvancesSequentiallyWithoutLeavingSelectedPool() {
        val selected = setOf(2, 5, 9)
        val allowed = QuranPageSelector.availablePages(QuranSelectionMode.HIZB, selected).toSet()
        val first = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = selected,
            cursor = 0,
            pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
        )
        val second = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = selected,
            cursor = first.nextCursor,
            pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
        )
        assertTrue(first.pages.isNotEmpty())
        assertTrue(second.pages.isNotEmpty())
        assertTrue(first.pages.size <= UsageCyclePolicy.MORNING_PAGE_COUNT)
        assertTrue(second.pages.size <= UsageCyclePolicy.MORNING_PAGE_COUNT)
        assertTrue(first.pages.all { it in allowed })
        assertTrue(second.pages.all { it in allowed })
        assertEquals(first.pages.size, first.pages.distinct().size)
        assertEquals(second.pages.size, second.pages.distinct().size)
        if (first.nextCursor != 0) {
            assertTrue(second.pages.first() > first.pages.last())
        }
    }

    @Test
    fun ninetyMinuteChallengeUsesUpToTenCanonicalPagesWithoutCrossingBoundary() {
        (1..60).forEach { hizb ->
            val canonical = QuranPageSelector.canonicalPages(QuranSelectionMode.HIZB, hizb).toSet()
            val quota = QuranPageSelector.tenPageQuotaFromHizb(hizb)
            assertTrue("Hizb $hizb", quota.isNotEmpty())
            assertTrue("Hizb $hizb", quota.size <= UsageCyclePolicy.NINETY_MINUTE_PAGE_COUNT)
            assertTrue("Hizb $hizb", quota.all { it in canonical })
            assertEquals("Hizb $hizb", quota.size, quota.distinct().size)
        }
    }

    @Test
    fun fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds() {
        assertEquals(15L * 60_000L, UsageCyclePolicy.INTERVAL_MS)
        assertEquals(90, UsageCyclePolicy.CUMULATIVE_MINUTES)
        assertEquals(90L * 60_000L, UsageCyclePolicy.CUMULATIVE_MS)
        assertEquals(6, UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE)
    }

    @Test
    fun livePresenceJoinsCompletedIntervalsWithoutWallClockTime() {
        val state = UsageCycleState(
            morningCompleted = true,
            completedIntervals = 4,
            completedNinetyMinuteCycles = 3
        )
        assertEquals(
            67L * 60_000L,
            UsageCyclePolicy.currentCyclePresenceMs(
                state,
                currentIntervalPresenceMs = 7L * 60_000L
            )
        )
    }

    @Test
    fun ninetyMinutePendingHizbCannotOverflowTheCurrentCycle() {
        val state = UsageCycleState(
            morningCompleted = true,
            completedIntervals = UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE,
            completedNinetyMinuteCycles = 2,
            pendingLevel = ChallengeLevel.HIZB
        )
        assertEquals(
            UsageCyclePolicy.CUMULATIVE_MS,
            UsageCyclePolicy.currentCyclePresenceMs(
                state,
                currentIntervalPresenceMs = UsageCyclePolicy.INTERVAL_MS
            )
        )
    }
}
''', encoding="utf-8")

replace_exact(
    ROOT / "app/src/main/java/com/quranunlock/guard/GateActivity.kt",
    'ChallengeLevel.MORNING -> "Filtre matinal • 20 pages"',
    'ChallengeLevel.MORNING -> "Filtre matinal • jusqu’à 20 pages"',
)
replace_exact(
    ROOT / "app/src/main/java/com/quranunlock/guard/GateActivity.kt",
    'ChallengeLevel.HIZB -> "Palier de 90 minutes • 10 pages"',
    'ChallengeLevel.HIZB -> "Palier de 90 minutes • jusqu’à 10 pages"',
)

# Self-check: the repaired gate must reject the obsolete semantics rather than
# merely hiding them behind comments or dead markers.
build_text = build.read_text(encoding="utf-8")nusage_text = usage_test.read_text(encoding="utf-8")
for forbidden in (
    "singleHizbPoolRepeatsToReachTwentyMorningPages",
    "everyHizbChallengeUsesExactlyTenPages",
    "shortHizbKeepsTenPageQuotaWithoutHidingRealBoundary",
):
    if forbidden in build_text or forbidden in usage_text:
        raise SystemExit(f"Obsolete 0.10.4 semantic gate remains: {forbidden}")

print("0.10.5 release gate repaired for canonical Juz/Hizb semantics")
