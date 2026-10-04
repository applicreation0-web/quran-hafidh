package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAudioSourceTest {
    @Test
    fun canonicalAyahFileNamesAreStable() {
        assertEquals("001001.mp3", QuranAudioSource.fileName(1, 1))
        assertEquals("002255.mp3", QuranAudioSource.fileName(2, 255))
        assertEquals("114006.mp3", QuranAudioSource.fileName(114, 6))
    }

    @Test
    fun generatedUrlUsesOnlyThePinnedHusaryMuallimDirectory() {
        assertEquals(
            "https://everyayah.com/data/Husary_Muallim_128kbps/001001.mp3",
            QuranAudioSource.url(1, 1)
        )
        assertTrue(
            QuranAudioSource.url(114, 6)
                .startsWith("https://everyayah.com/data/Husary_Muallim_128kbps/")
        )
    }

    @Test
    fun sourceHostPolicyIsHttpsAndNarrow() {
        assertTrue(QuranAudioSource.isAllowedHttpsUrl("https", "everyayah.com"))
        assertTrue(QuranAudioSource.isAllowedHttpsUrl("HTTPS", "www.everyayah.com"))
        assertFalse(QuranAudioSource.isAllowedHttpsUrl("http", "everyayah.com"))
        assertFalse(QuranAudioSource.isAllowedHttpsUrl("https", "example.com"))
        assertFalse(QuranAudioSource.isAllowedHttpsUrl("https", null))
    }

    @Test
    fun canonicalSurahSpecificAyahLimitsAreEnforced() {
        assertEquals(7, QuranAudioSource.ayahCount(1))
        assertEquals(286, QuranAudioSource.ayahCount(2))
        assertEquals(6, QuranAudioSource.ayahCount(114))

        assertFalse(QuranAudioSource.isValidReference(0, 1))
        assertFalse(QuranAudioSource.isValidReference(115, 1))
        assertFalse(QuranAudioSource.isValidReference(1, 0))
        assertFalse(QuranAudioSource.isValidReference(1, 8))
        assertFalse(QuranAudioSource.isValidReference(2, 287))
        assertFalse(QuranAudioSource.isValidReference(114, 7))
        assertTrue(QuranAudioSource.isValidReference(1, 7))
        assertTrue(QuranAudioSource.isValidReference(2, 286))
        assertTrue(QuranAudioSource.isValidReference(114, 6))
    }
}