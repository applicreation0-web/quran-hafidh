package com.applicreation0.quransafeguard

/**
 * A commentary remains a documentary object of one author and one work.
 * Never merge two commentators into one generated explanation.
 */
enum class HikamCommentator(
    val stableId: String,
    val shortLabel: String,
    val fullName: String
) {
    SHARNUBI(
        stableId = "sharnubi",
        shortLabel = "al-Sharnūbī",
        fullName = "ʿAbd al-Majīd al-Sharnūbī al-Azharī"
    ),
    IBN_ABBAD(
        stableId = "ibn_abbad",
        shortLabel = "Ibn ʿAbbād",
        fullName = "Ibn ʿAbbād al-Rundī"
    )
}

enum class HikamRichRole {
    TECHNICAL_TERM,
    AUTHOR_EMPHASIS,
    COMMENTATOR_EMPHASIS,
    QURAN_QUOTE,
    HADITH_QUOTE,
    EDITORIAL_BRACKET,
    SOURCE_NOTE
}

data class HikamRichSpan(
    val start: Int,
    val endExclusive: Int,
    val role: HikamRichRole,
    val lexiconKey: String? = null
)

data class HikamSharhEntry(
    val hikmaNumber: Int,
    val commentator: HikamCommentator,
    val workTitle: String,
    val arabicText: String,
    val frenchText: String,
    val sourceUrl: String,
    val printLocator: String,
    val verified: Boolean,
    val richSpansArabic: List<HikamRichSpan> = emptyList(),
    val richSpansFrench: List<HikamRichSpan> = emptyList(),
    val technicalTerms: Set<String> = emptySet()
) {
    val displayEligible: Boolean
        get() =
            hikmaNumber in 1..264 &&
                workTitle.isNotBlank() &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                sourceUrl.isNotBlank() &&
                printLocator.isNotBlank() &&
                verified
}

data class HikamSharhAvailability(
    val commentator: HikamCommentator,
    val entry: HikamSharhEntry?
) {
    val available: Boolean
        get() = entry?.displayEligible == true
}
