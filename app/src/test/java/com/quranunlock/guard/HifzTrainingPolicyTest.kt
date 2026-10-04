package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzTrainingPolicyTest {

    @Test
    fun sabqiIsExactlyThirtySevenRecitationsAndAudioIsNotAProgressStep() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI)

        assertFalse(steps.any { it.requiresAudio })
        assertFalse(steps.any { it.kind == HifzTrainingKind.AUDIO_PASSIVE || it.kind == HifzTrainingKind.AUDIO_ACTIVE })
        assertEquals(15, steps.filter { it.maskPercent == 0 }.sumOf { it.repetitions })
        assertEquals(22, steps.filter { it.maskPercent > 0 }.sumOf { it.repetitions })
        assertEquals(37, steps.sumOf { it.repetitions })
        assertEquals(listOf(25, 50, 75, 100), steps.filter { it.maskPercent > 0 }.map { it.maskPercent })
        assertEquals(listOf(5, 5, 5, 7), steps.filter { it.maskPercent > 0 }.map { it.repetitions })
        assertTrue(HifzTrainingPolicy.assemblyStepsFor(HifzTrack.SABQI).isEmpty())
    }

    @Test
    fun audioAvailabilityNeverChangesSabqiRepetitionProtocol() {
        val withAudio = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI, audioAvailable = true)
        val withoutAudio = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI, audioAvailable = false)
        assertEquals(withAudio, withoutAudio)
        assertEquals(37, withoutAudio.sumOf { it.repetitions })
    }

    @Test
    fun itqanPreviewDistributionRemainsExactlyThirtyWithMandatoryMasking() {
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN)
        val visible = steps.filter { it.maskPercent == 0 }
        val masked = steps.filter { it.maskPercent > 0 }

        assertEquals(HifzTrainingPolicy.PREVIEW_ITQAN_VISIBLE_REPETITIONS, visible.sumOf { it.repetitions })
        assertEquals(20, masked.sumOf { it.repetitions })
        assertEquals(30, steps.sumOf { it.repetitions })
        assertEquals(listOf(25, 50, 75, 100), masked.map { it.maskPercent })
        assertEquals(listOf(5, 5, 5, 5), masked.map { it.repetitions })
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
