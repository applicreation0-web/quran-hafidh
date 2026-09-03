package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
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
    fun singleHizbPoolRepeatsToReachTwentyMorningPages() {
        val plan = QuranPageSelector.sequentialHizbPages(
            selectedHizb = setOf(7),
            cursor = 0,
            hizbCount = 2
        )

        assertEquals(20, plan.pages.size)
        assertEquals(listOf(7, 7), plan.hizbNumbers)
        assertEquals(plan.pages.take(10), plan.pages.drop(10))
        assertEquals(0, plan.nextCursor)
    }

    @Test
    fun multiHizbPoolAdvancesSequentiallyFromSmallest() {
        val dayOne = QuranPageSelector.sequentialHizbPages(
            selectedHizb = setOf(9, 2, 5),
            cursor = 0,
            hizbCount = 2
        )
        val dayTwo = QuranPageSelector.sequentialHizbPages(
            selectedHizb = setOf(9, 2, 5),
            cursor = dayOne.nextCursor,
            hizbCount = 2
        )

        assertEquals(listOf(2, 5), dayOne.hizbNumbers)
        assertEquals(listOf(9, 2), dayTwo.hizbNumbers)
        assertEquals(20, dayOne.pages.size)
        assertEquals(20, dayTwo.pages.size)
    }

    @Test
    fun everyHizbChallengeUsesExactlyTenPages() {
        (1..60).forEach { hizb ->
            assertEquals(10, QuranPageSelector.tenPageQuotaFromHizb(hizb).size)
        }
    }
}
