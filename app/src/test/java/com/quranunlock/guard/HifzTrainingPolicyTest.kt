package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzTrainingPolicyTest {

    @Test
    fun sabqiStartsWithAudioAndEndsFullyMasked() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI)

        assertEquals(HifzTrainingKind.AUDIO_PASSIVE, steps.first().kind)
        assertTrue(steps.first().requiresAudio)
        assertEquals(HifzTrainingKind.AUDIO_ACTIVE, steps[1].kind)
        assertTrue(steps[1].requiresAudio)
        assertEquals(listOf(25, 50, 75, 100), steps.filter { it.kind == HifzTrainingKind.MASKED }.map { it.maskPercent })
        assertEquals(HifzTrainingKind.FINAL_TEST, steps.last().kind)
        assertEquals(100, steps.last().maskPercent)
    }

    @Test
    fun sabqiCanExplicitlyFallBackToNonAudioStepsWhenAudioIsUnavailable() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI, audioAvailable = false)

        assertFalse(steps.any { it.requiresAudio })
        assertEquals("sabqi-visible", steps.first().id)
        assertEquals(HifzTrainingKind.FINAL_TEST, steps.last().kind)
    }

    @Test
    fun itqanUsesThirtyVisibleRepetitionsThenProgressiveMasking() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN)
        val masked = steps.filter { it.kind == HifzTrainingKind.MASKED }

        assertEquals(30, steps.first().repetitions)
        assertEquals(HifzTrainingKind.VISIBLE, steps.first().kind)
        assertEquals(listOf(25, 50, 75, 100), masked.map { it.maskPercent })
        assertTrue(masked.all { it.requiresConsecutiveSuccesses == 1 })
        assertEquals(HifzTrainingKind.FINAL_TEST, steps.last().kind)
        assertEquals(100, steps.last().maskPercent)
        assertEquals(1, steps.last().requiresConsecutiveSuccesses)
        assertFalse(steps.any { it.requiresAudio })
    }

    @Test
    fun murajaahUsesOneExplicitFullyMaskedRecallStep() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.MURAJAAH)

        assertEquals(1, steps.size)
        assertEquals("murajaah-recall", steps.single().id)
        assertEquals(HifzTrainingKind.FINAL_TEST, steps.single().kind)
        assertEquals(100, steps.single().maskPercent)
        assertEquals(MurajaahPolicy.RECITATIONS_PER_PORTION, steps.single().repetitions)
        assertEquals(1, steps.single().requiresConsecutiveSuccesses)
        assertFalse(steps.single().requiresAudio)
    }
}
