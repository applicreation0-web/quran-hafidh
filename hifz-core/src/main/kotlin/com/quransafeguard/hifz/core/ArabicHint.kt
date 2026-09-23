package com.quransafeguard.hifz.core

/**
 * Builds the writing exercise's hidden-by-default hint: the first few letters of a verse, WITH
 * their tashkil (short vowels) — unlike ArabicTextComparison's normalize(), which strips tashkil
 * for a fair ML Kit content comparison, the hint is shown to a human and must keep the diacritics
 * or it would be misleading about which vowel to write.
 */
object ArabicHint {
    private val COMBINING_MARKS = ('ً'..'ٟ') + 'ـ' + 'ٰ' + ('ۖ'..'ۭ')

    /**
     * The verse text's first [letterCount] base letters, each with any tashkil marks that follow
     * it kept attached (a base letter's diacritics are separate combining characters immediately
     * after it in the string, so a plain substring could otherwise cut one off mid-letter).
     */
    @JvmStatic
    fun firstLettersWithTashkil(text: String, letterCount: Int): String {
        if (letterCount <= 0 || text.isEmpty()) return ""
        var lettersSeen = 0
        var end = 0
        while (end < text.length) {
            val ch = text[end]
            if (!COMBINING_MARKS.contains(ch) && !ch.isWhitespace()) {
                if (lettersSeen == letterCount) break
                lettersSeen++
            }
            end++
        }
        return text.substring(0, end).trimEnd()
    }
}
