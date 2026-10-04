package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class HifzReadingCountPolicyTest {

    @Test
    fun audioNeverCountsAsReading() {
        val audio = HifzTrainingStep(
            id = "audio",
            label = "Audio",
            kind = HifzTrainingKind.AUDIO_ACTIVE,
            repetitions = 1,
            maskPercent = 0,
            requiresAudio = true
        )
        assertEquals(HifzReadingCounts(), HifzReadingCountPolicy.add(HifzReadingCounts(), audio))
    }

    @Test
    fun visibleAndMaskedAttemptsRemainSeparated() {
        val visible = HifzTrainingStep("visible", "Visible", HifzTrainingKind.VISIBLE, 1, 0)
        val masked = HifzTrainingStep("masked", "Masqué", HifzTrainingKind.MASKED, 1, 75)

        var counts = HifzReadingCounts()
        repeat(20) { counts = HifzReadingCountPolicy.add(counts, visible) }
        repeat(10) { counts = HifzReadingCountPolicy.add(counts, masked) }

        assertEquals(20, counts.visible)
        assertEquals(10, counts.masked)
    }
}
