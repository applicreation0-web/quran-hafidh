package com.applicreation0.quransafeguard

/**
 * A commentary remains a documentary object of one author and one work.
 * Never merge two commentators into one generated explanation.
 */
enum class HikamCommentator(
    val stableId: String,
    val shortLabel: String,
    val fullName: String,
    val expectedWorkId: String
) {
    SHARNUBI(
        stableId = "sharnubi",
        shortLabel = "al-Sharnūbī",
        fullName = "ʿAbd al-Majīd al-Sharnūbī al-Azharī",
        expectedWorkId = "sharnubi_sharh_al_hikam"
    ),
    IBN_ABBAD(
        stableId = "ibn_abbad",
        shortLabel = "Ibn ʿAbbād",
        fullName = "Ibn ʿAbbād al-Rundī",
        expectedWorkId = "ibn_abbad_ghayth_al_mawahib"
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
    val workId: String,
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
    val attributionMatchesSource: Boolean
        get() = workId == commentator.expectedWorkId

    val displayEligible: Boolean
        get() =
            hikmaNumber in 1..264 &&
                attributionMatchesSource &&
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

object HikamSharhIntegrity {
    /** Fail closed instead of silently choosing the first duplicated attribution. */
    fun requireUnique(entries: List<HikamSharhEntry>) {
        val keys = entries.map { it.hikmaNumber to it.commentator }
        require(keys.size == keys.toSet().size) {
            "Duplicate Hikam sharh entry for the same Hikma/commentator"
        }
        require(entries.all(HikamSharhEntry::attributionMatchesSource)) {
            "Hikam sharh work/commentator attribution mismatch"
        }
    }
}
