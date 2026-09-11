package com.quransafeguard.safeguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeguardCoreTest {

    private val twentyMinutes = 20 * 60_000L
    private val engine = PerAppBudgetEngine(initialBudgetMillis = { twentyMinutes })

    @Test
    fun approvedTargetScopeContainsOnlyThe14FrozenTargets() {
        assertEquals(14, AllowedTargetPackages.all.size)
        assertTrue(AllowedTargetPackages.isSelectable("com.whatsapp"))
        assertTrue(AllowedTargetPackages.isSelectable("com.android.chrome"))
        assertFalse(AllowedTargetPackages.isSelectable("com.example.bank"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun bankPackageCannotEnterBudgetEngine() {
        engine.onForegroundSignal(
            BudgetLedgerState(),
            ForegroundSignal.Target("com.example.bank"),
            nowElapsedMillis = 1_000L
        )
    }

    @Test
    fun whatsappFiveMinutesOutsideOneHourThenFourMinutesLeavesElevenMinutes() {
        var state = BudgetLedgerState()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            5 * 60_000L
        ).state

        assertEquals(15 * 60_000L, engine.remainingMillis(state, "com.whatsapp"))

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            65 * 60_000L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            69 * 60_000L
        ).state

        assertEquals(11 * 60_000L, engine.remainingMillis(state, "com.whatsapp"))
    }

    @Test
    fun budgetsAreIndependentPerApplication() {
        var state = BudgetLedgerState()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.android.chrome"),
            5 * 60_000L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            8 * 60_000L
        ).state

        assertEquals(15 * 60_000L, engine.remainingMillis(state, "com.whatsapp"))
        assertEquals(17 * 60_000L, engine.remainingMillis(state, "com.android.chrome"))
    }

    @Test
    fun longOutsideGapNeverConsumesTargetBudget() {
        var state = BudgetLedgerState()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.instagram.android"),
            1000L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            61_000L
        ).state

        val afterOneMinute = engine.remainingMillis(state, "com.instagram.android")

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            3_661_000L
        ).state

        assertEquals(afterOneMinute, engine.remainingMillis(state, "com.instagram.android"))
    }

    @Test
    fun transientSystemUiDoesNotPauseTargetOwnership() {
        var state = BudgetLedgerState()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.TransientSystemUi,
            2 * 60_000L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            5 * 60_000L
        ).state

        assertEquals(15 * 60_000L, engine.remainingMillis(state, "com.whatsapp"))
    }

    @Test
    fun frequentCheckpointsNeverExtendCredit() {
        var state = BudgetLedgerState()
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.google.android.youtube"),
            0L
        ).state

        for (second in 1..300) {
            state = engine.checkpoint(state, second * 1000L).state
        }

        assertEquals(15 * 60_000L, engine.remainingMillis(state, "com.google.android.youtube"))
    }

    @Test
    fun crossingTenFiveOneMinuteThresholdsEmitsEachWarningOnlyOnce() {
        var state = BudgetLedgerState()
        val warnings = mutableListOf<BudgetWarning>()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state

        listOf(11, 15, 19, 20).forEach { minute ->
            val transition = engine.checkpoint(state, minute * 60_000L)
            state = transition.state
            warnings += transition.warnings
        }

        assertEquals(
            listOf(10 * 60_000L, 5 * 60_000L, 1 * 60_000L),
            warnings.map { it.thresholdMillis }
        )
        assertEquals(3, warnings.map { it.thresholdMillis }.distinct().size)
    }

    @Test
    fun freshGrantResetsWarningOnceStateForThatPackageOnly() {
        var state = BudgetLedgerState()
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state
        var transition = engine.checkpoint(state, 11 * 60_000L)
        state = transition.state
        assertEquals(listOf(10 * 60_000L), transition.warnings.map { it.thresholdMillis })

        transition = engine.replaceGrant(
            state = state,
            packageName = "com.whatsapp",
            grantedMillis = twentyMinutes,
            nowElapsedMillis = 11 * 60_000L
        )
        state = transition.state

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            11 * 60_000L
        ).state
        transition = engine.checkpoint(state, 22 * 60_000L)

        assertEquals(listOf(10 * 60_000L), transition.warnings.map { it.thresholdMillis })
    }

    @Test
    fun exhaustingOneTargetDoesNotExhaustAnother() {
        var state = BudgetLedgerState()

        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.Target("com.whatsapp"),
            0L
        ).state
        state = engine.onForegroundSignal(
            state,
            ForegroundSignal.OutsideTarget,
            twentyMinutes
        ).state

        assertEquals(0L, engine.remainingMillis(state, "com.whatsapp"))
        assertEquals(twentyMinutes, engine.remainingMillis(state, "com.android.chrome"))
    }

    @Test
    fun fiftyNineActiveReadingSecondsCannotValidate() {
        val started = ReadingGatePolicy.start(ReadingGateState(), 1_000L)

        assertFalse(ReadingGatePolicy.canValidate(started, 60_000L))
    }

    @Test
    fun sixtyActiveReadingSecondsCanValidate() {
        val started = ReadingGatePolicy.start(ReadingGateState(), 1_000L)

        assertTrue(ReadingGatePolicy.canValidate(started, 61_000L))
    }

    @Test
    fun pausedReadingTimeDoesNotCount() {
        var state = ReadingGatePolicy.start(ReadingGateState(), 0L)
        state = ReadingGatePolicy.pause(state, 30_000L)

        assertFalse(ReadingGatePolicy.canValidate(state, 3_600_000L))

        state = ReadingGatePolicy.start(state, 3_600_000L)
        assertTrue(ReadingGatePolicy.canValidate(state, 3_630_000L))
    }

    @Test
    fun dailyJokerCounterIsCappedAtThree() {
        var counter = DailyJokerCounter()
        repeat(3) { counter = counter.consume() }

        assertEquals(0, counter.remaining)
        assertEquals(3, counter.consumed)
    }
}
