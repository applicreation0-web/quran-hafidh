package com.quransafeguard.hifz.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Real candidates observed from three on-device ML Kit tests during this feature's design. */
class ArabicTextComparisonTest {
    @Test fun stripsTashkilForComparison() {
        assertTrue(ArabicTextComparison.matches("الحمد", "اَلْحَمْدُ"))
    }

    @Test fun collapsesAlefSeatVariantsBeforeComparing() {
        // The real word is "إِيَّاكَ" (hamza-under-alif); ML Kit's own candidates varied the seat.
        assertTrue(ArabicTextComparison.matches("اياك", "إِيَّاكَ"))
        assertTrue(ArabicTextComparison.matches("أياك", "إِيَّاكَ"))
    }

    @Test fun rejectsAGenuinelyDifferentWord() {
        assertFalse(ArabicTextComparison.matches("العمد", "اَلْحَمْدُ"))
        assertFalse(ArabicTextComparison.matches("اباك", "إِيَّاكَ"))
    }

    @Test fun anyMatchesChecksEveryRankedCandidate() {
        // Real 10-candidate output for "إِيَّاكَ": the correct word ranked first, but this must not
        // depend on rank — a correct recognition buried lower in the list is still correct content.
        val candidates = listOf("اياك", "ايا ك", "ا ياك", "ا يا ك", "الاك", "أياك", "ايك", "أيا ك", "اباك")
        assertTrue(ArabicTextComparison.anyMatches(candidates, "إِيَّاكَ"))
        assertFalse(ArabicTextComparison.anyMatches(candidates, "نَعْبُدُ"))
    }

    @Test fun collapsesWhitespaceDifferencesFromWordSegmentationGuesses() {
        assertTrue(ArabicTextComparison.matches("ا يا ك", "اياك"))
    }
}
