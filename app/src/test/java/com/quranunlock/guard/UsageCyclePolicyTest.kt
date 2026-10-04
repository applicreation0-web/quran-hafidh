package com.applicreation0.quransafeguard

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
        assertEquals(UsageCyclePolicy.INTERVALS_PER_HIZB, state.completedIntervals)
        assertEquals(ChallengeLevel.HIZB, state.pendingLevel)
    }

    @Test
    fun cumulativeHizbAbsorbsTheCoincidentSixthMicroBlock() {
        var state = UsageCycleState(
            morningCompleted = true,
            completedIntervals = 5
        )
        state = UsageCyclePolicy.onIntervalExpired(state)

        assertEquals(ChallengeLevel.HIZB, state.pendingLevel)
        assertTrue(state.pendingLevel != ChallengeLevel.MICRO)
    }

    @Test
    fun completingHizbResetsEntireNinetyMinuteCycle() {
        val completed = UsageCyclePolicy.completeChallenge(
            UsageCycleState(
                morningCompleted = true,
                completedIntervals = 6,
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
                completedIntervals = 6,
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
    fun canonicalQuotaDoesNotRepeatShortSelectedPool() {
        val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(15),
            cursor = 0,
            pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
        )

        assertEquals((142..150).toList(), plan.pages)
        assertEquals(9, plan.pages.size)
        assertEquals(plan.pages.size, plan.pages.distinct().size)
        assertFalse(151 in plan.pages)
    }

    @Test
    fun canonicalQuotaAdvancesSequentiallyAcrossSelectedUnits() {
        val pool = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(2, 5, 9)
        )
        val first = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(9, 2, 5),
            cursor = 0,
            pageCount = UsageCyclePolicy.NINETY_MINUTE_PAGE_COUNT
        )
        val second = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(9, 2, 5),
            cursor = first.nextCursor,
            pageCount = UsageCyclePolicy.NINETY_MINUTE_PAGE_COUNT
        )

        assertEquals(pool.take(10), first.pages)
        assertEquals(pool.drop(10).take(10), second.pages)
        assertTrue(first.pages.zipWithNext().all { (a, b) -> a < b })
        assertTrue(second.pages.zipWithNext().all { (a, b) -> a < b })
        assertTrue((first.pages + second.pages).all { it in pool })
    }

    @Test
    fun ninetyMinuteQuotaNeverCrossesSelectedCanonicalPool() {
        val selected = setOf(15)
        val canonical = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            selected
        ).toSet()
        val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = selected,
            cursor = 0,
            pageCount = UsageCyclePolicy.NINETY_MINUTE_PAGE_COUNT
        )

        assertTrue(plan.pages.all { it in canonical })
        assertEquals((142..150).toList(), plan.pages)
        assertFalse(151 in plan.pages)
    }

    @Test
    fun fifteenAndNinetyMinutesAreLiteralTargetPresenceThresholds() {
        assertEquals(15L * 60_000L, UsageCyclePolicy.INTERVAL_MS)
        assertEquals(90, UsageCyclePolicy.CUMULATIVE_MINUTES)
        assertEquals(90L * 60_000L, UsageCyclePolicy.CUMULATIVE_MS)
        assertEquals(6, UsageCyclePolicy.INTERVALS_PER_NINETY_MINUTE_CYCLE)
        assertEquals(10, UsageCyclePolicy.NINETY_MINUTE_PAGE_COUNT)
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
            completedIntervals = 6,
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
