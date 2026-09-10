package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzTrainingPolicyTest {

    @Test
    fun sabqiDefaultsToAudioThenTenVisibleAndTwentyTwoMaskedReadings() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI)

        assertEquals(HifzTrainingKind.AUDIO_PASSIVE, steps[0].kind)
        assertEquals(HifzTrainingKind.AUDIO_ACTIVE, steps[1].kind)
        assertTrue(steps.take(2).all { it.requiresAudio })

        val readings = steps.filter { it.kind != HifzTrainingKind.AUDIO_PASSIVE && it.kind != HifzTrainingKind.AUDIO_ACTIVE }
        assertEquals(10, readings.filter { it.maskPercent == 0 }.sumOf { it.repetitions })
        assertEquals(22, readings.filter { it.maskPercent > 0 }.sumOf { it.repetitions })
        assertEquals(listOf(25, 50, 75, 100), readings.filter { it.maskPercent > 0 }.map { it.maskPercent })
        assertEquals(listOf(5, 5, 5, 7), readings.filter { it.maskPercent > 0 }.map { it.repetitions })
    }

    @Test
    fun sabqiCanFailSafeWithoutAudioWithoutChangingReadingProtocol() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI, audioAvailable = false)

        assertFalse(steps.any { it.requiresAudio })
        assertEquals("sabqi-visible", steps.first().id)
        assertEquals(10, steps.filter { it.maskPercent == 0 }.sumOf { it.repetitions })
        assertEquals(22, steps.filter { it.maskPercent > 0 }.sumOf { it.repetitions })
    }

    @Test
    fun itqanIsExactlyTwentyVisiblePlusTenMaskedRepetitions() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN)
        val visible = steps.filter { it.maskPercent == 0 }
        val masked = steps.filter { it.maskPercent > 0 }

        assertEquals(20, visible.sumOf { it.repetitions })
        assertEquals(10, masked.sumOf { it.repetitions })
        assertEquals(30, steps.sumOf { it.repetitions })
        assertEquals(listOf(25, 50, 75, 100), masked.map { it.maskPercent })
        assertEquals(listOf(2, 2, 2, 4), masked.map { it.repetitions })
        assertTrue(masked.all { it.requiresConsecutiveSuccesses == 1 })
        assertFalse(steps.any { it.requiresAudio })
    }

    @Test
    fun murajaahNeverImposesMasking() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.MURAJAAH)

        assertEquals(1, steps.size)
        assertEquals("murajaah-recall", steps.single().id)
        assertEquals(HifzTrainingKind.VISIBLE, steps.single().kind)
        assertEquals(0, steps.single().maskPercent)
        assertEquals(MurajaahPolicy.RECITATIONS_PER_PORTION, steps.single().repetitions)
        assertEquals(1, steps.single().requiresConsecutiveSuccesses)
    }
}
