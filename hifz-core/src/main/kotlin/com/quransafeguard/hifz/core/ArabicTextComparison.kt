package com.quransafeguard.hifz.core

/**
 * Compares recognized handwriting text against the real verse text for content verification
 * (InkContentVerifier in hifz-app). Confirmed empirically (three separate on-device tests) that
 * ML Kit's Arabic digital-ink model never outputs tashkil (short vowels) — it only recognizes the
 * consonant skeleton — and that it is genuinely uncertain about which alef/hamza seat was drawn
 * (اياك / إياك / أياك all showed up as candidates for the same handwritten word). Comparison must
 * therefore be tashkil-insensitive and alef-seat-insensitive, or it would reject correct content
 * for limitations of the recognizer, not real memorization errors.
 */
object ArabicTextComparison {
    private val TASHKIL_AND_TATWEEL = ('ً'..'ٟ') + 'ـ' + 'ٰ' + ('ۖ'..'ۭ')
    private val ALEF_VARIANTS = charArrayOf('أ', 'إ', 'آ', 'ٱ') // أ إ آ ٱ

    /**
     * Strips tashkil/tatweel, collapses alef variants, and removes all whitespace, for a fair
     * recognized-vs-real comparison. Whitespace is dropped rather than merely collapsed because
     * this compares one word (or one short known phrase) at a time: a space in a candidate is
     * always the recognizer guessing a sub-word break, never a meaningful content difference.
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (TASHKIL_AND_TATWEEL.contains(ch) || ch.isWhitespace()) continue
            sb.append(if (ALEF_VARIANTS.contains(ch)) 'ا' else ch)
        }
        return sb.toString()
    }

    /** True if the recognized text matches the expected text once both are normalized. */
    fun matches(recognized: String, expected: String): Boolean =
        normalize(recognized) == normalize(expected)

    /** True if any of ML Kit's ranked candidates matches — recognition of the right word can rank below #1. */
    fun anyMatches(candidates: List<String>, expected: String): Boolean {
        val normalizedExpected = normalize(expected)
        return candidates.any { normalize(it) == normalizedExpected }
    }
}
