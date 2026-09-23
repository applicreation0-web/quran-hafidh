package com.quransafeguard.hifz.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ArabicHintTest {
    // The real Tanzil-Uthmani text for 1:1, exactly as generated into verses_text.json.
    private val BISMILLAH = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ"

    @Test fun keepsTashkilAttachedToEachOfTheFirstThreeLetters() {
        val hint = ArabicHint.firstLettersWithTashkil(BISMILLAH, 3)
        // ب + ِ, س + ۡ, م + ِ — three base letters, each keeping its own diacritic.
        assertEquals("بِسۡمِ", hint)
    }

    @Test fun oneLetterHintKeepsOnlyThatLettersOwnDiacritics() {
        assertEquals("بِ", ArabicHint.firstLettersWithTashkil(BISMILLAH, 1))
    }

    @Test fun zeroOrNegativeCountYieldsNothing() {
        assertEquals("", ArabicHint.firstLettersWithTashkil(BISMILLAH, 0))
        assertEquals("", ArabicHint.firstLettersWithTashkil(BISMILLAH, -1))
    }

    @Test fun emptyTextYieldsNothing() {
        assertEquals("", ArabicHint.firstLettersWithTashkil("", 3))
    }

    @Test fun countLargerThanTheTextReturnsTheWholeText() {
        assertEquals(BISMILLAH, ArabicHint.firstLettersWithTashkil(BISMILLAH, 999))
    }
}
