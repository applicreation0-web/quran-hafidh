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
    /** Historical single-number field kept for source/test compatibility. */
    val hikmaNumber: Int,
    val commentator: HikamCommentator,
    val workId: String,
    val workTitle: String,
    val arabicText: String,
    val frenchText: String,
    val sourceUrl: String,
    val printLocator: String,
    val verified: Boolean,
    /**
     * One classical source block may genuinely explain several canonical Hikam. Store the
     * block once and map it to every applicable canonical number instead of duplicating or
     * inventing a split.
     */
    val canonicalHikmaNumbers: Set<Int> = setOf(hikmaNumber),
    val sourceHikmaLocator: String = hikmaNumber.toString(),
    val commentaryGroupId: String = "${commentator.stableId}_$hikmaNumber",
    val richSpansArabic: List<HikamRichSpan> = emptyList(),
    val richSpansFrench: List<HikamRichSpan> = emptyList(),
    val technicalTerms: Set<String> = emptySet()
) {
    val attributionMatchesSource: Boolean
        get() = workId == commentator.expectedWorkId

    val displayEligible: Boolean
        get() =
            hikmaNumber in 1..264 &&
                canonicalHikmaNumbers.isNotEmpty() &&
                canonicalHikmaNumbers.all { it in 1..264 } &&
                hikmaNumber in canonicalHikmaNumbers &&
                sourceHikmaLocator.isNotBlank() &&
                commentaryGroupId.isNotBlank() &&
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
    /** Fail closed instead of silently choosing or duplicating an attribution. */
    fun requireUnique(entries: List<HikamSharhEntry>) {
        require(entries.all(HikamSharhEntry::attributionMatchesSource)) {
            "Hikam sharh work/commentator attribution mismatch"
        }
        require(entries.all { entry ->
            entry.canonicalHikmaNumbers.isNotEmpty() &&
                entry.canonicalHikmaNumbers.all { it in 1..264 } &&
                entry.hikmaNumber in entry.canonicalHikmaNumbers &&
                entry.commentaryGroupId.isNotBlank() &&
                entry.sourceHikmaLocator.isNotBlank()
        }) {
            "Invalid Hikam sharh source mapping metadata"
        }

        val mappedKeys = entries.flatMap { entry ->
            entry.canonicalHikmaNumbers.map { number -> number to entry.commentator }
        }
        require(mappedKeys.size == mappedKeys.toSet().size) {
            "Duplicate Hikam sharh mapping for the same canonical Hikma/commentator"
        }

        val groupIds = entries.map(HikamSharhEntry::commentaryGroupId)
        require(groupIds.size == groupIds.toSet().size) {
            "Duplicate commentary_group_id: one classical block must be stored once"
        }
    }
}
