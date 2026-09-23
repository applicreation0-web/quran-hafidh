package com.applicreation0.quransafeguard

enum class HifzTrainingKind {
    AUDIO_PASSIVE,
    AUDIO_ACTIVE,
    VISIBLE,
    MASKED,
    FINAL_TEST
}

data class HifzTrainingStep(
    val id: String,
    val label: String,
    val kind: HifzTrainingKind,
    val repetitions: Int,
    val maskPercent: Int,
    val requiresAudio: Boolean = false,
    val requiresConsecutiveSuccesses: Int = 0
) {
    init {
        require(id.isNotBlank())
        require(repetitions >= 0)
        require(maskPercent in 0..100)
        require(requiresConsecutiveSuccesses >= 0)
    }
}

/**
 * Structured-Hifz repetition protocol.
 *
 * Frozen Sabqi rule: one five-real-line block, 37 recitations total:
 * 15 visible + 5 at 25% + 5 at 50% + 5 at 75% + 7 at 100%.
 * Audio is deliberately NOT a training step: listening never increments repetitions,
 * never advances the task cursor and is controlled independently by the Hifz player.
 *
 * Itqan is frozen at x30 with mandatory masking, but the exact distribution between
 * masking stages is not yet a final product decision. PREVIEW_* constants therefore
 * provide one centralized, replaceable test-build policy rather than scattered magic
 * numbers.
 */
object HifzTrainingPolicy {
    const val SABQI_VISIBLE_REPETITIONS = 15
    const val SABQI_MASK_25_REPETITIONS = 5
    const val SABQI_MASK_50_REPETITIONS = 5
    const val SABQI_MASK_75_REPETITIONS = 5
    const val SABQI_MASK_100_REPETITIONS = 7
    const val SABQI_TOTAL_REPETITIONS = 37

    // Preview-only distribution. Total x30 and mandatory masking are the frozen rules.
    const val PREVIEW_ITQAN_VISIBLE_REPETITIONS = 10
    const val PREVIEW_ITQAN_MASK_25_REPETITIONS = 5
    const val PREVIEW_ITQAN_MASK_50_REPETITIONS = 5
    const val PREVIEW_ITQAN_MASK_75_REPETITIONS = 5
    const val PREVIEW_ITQAN_MASK_100_REPETITIONS = 5
    const val ITQAN_TOTAL_REPETITIONS = 30

    init {
        check(
            SABQI_VISIBLE_REPETITIONS +
                SABQI_MASK_25_REPETITIONS +
                SABQI_MASK_50_REPETITIONS +
                SABQI_MASK_75_REPETITIONS +
                SABQI_MASK_100_REPETITIONS == SABQI_TOTAL_REPETITIONS
        )
        check(
            PREVIEW_ITQAN_VISIBLE_REPETITIONS +
                PREVIEW_ITQAN_MASK_25_REPETITIONS +
                PREVIEW_ITQAN_MASK_50_REPETITIONS +
                PREVIEW_ITQAN_MASK_75_REPETITIONS +
                PREVIEW_ITQAN_MASK_100_REPETITIONS == ITQAN_TOTAL_REPETITIONS
        )
    }

    /** Kept source-compatible; audio availability must not alter the repetition protocol. */
    fun stepsFor(track: HifzTrack, @Suppress("UNUSED_PARAMETER") audioAvailable: Boolean = true): List<HifzTrainingStep> =
        when (track) {
            HifzTrack.SABQI -> sabqiSteps()
            HifzTrack.ITQAN -> itqanSteps()
            HifzTrack.MURAJAAH -> murajaahSteps()
        }

    /** Exact five-line Sabqi blocks need no extra repetition phase after the 37 recitations. */
    fun assemblyStepsFor(@Suppress("UNUSED_PARAMETER") track: HifzTrack): List<HifzTrainingStep> = emptyList()

    private fun sabqiSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            "sabqi-visible",
            "Texte visible",
            HifzTrainingKind.VISIBLE,
            SABQI_VISIBLE_REPETITIONS,
            0
        ),
        HifzTrainingStep(
            "sabqi-mask-25",
            "Masquage 25 %",
            HifzTrainingKind.MASKED,
            SABQI_MASK_25_REPETITIONS,
            25
        ),
        HifzTrainingStep(
            "sabqi-mask-50",
            "Masquage 50 %",
            HifzTrainingKind.MASKED,
            SABQI_MASK_50_REPETITIONS,
            50
        ),
        HifzTrainingStep(
            "sabqi-mask-75",
            "Masquage 75 %",
            HifzTrainingKind.MASKED,
            SABQI_MASK_75_REPETITIONS,
            75
        ),
        HifzTrainingStep(
            "sabqi-mask-100",
            "Masquage 100 %",
            HifzTrainingKind.MASKED,
            SABQI_MASK_100_REPETITIONS,
            100
        )
    )

    private fun itqanSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            "itqan-visible",
            "Consolidation texte visible",
            HifzTrainingKind.VISIBLE,
            PREVIEW_ITQAN_VISIBLE_REPETITIONS,
            0
        ),
        HifzTrainingStep(
            "itqan-mask-25",
            "Masquage 25 %",
            HifzTrainingKind.MASKED,
            PREVIEW_ITQAN_MASK_25_REPETITIONS,
            25
        ),
        HifzTrainingStep(
            "itqan-mask-50",
            "Masquage 50 %",
            HifzTrainingKind.MASKED,
            PREVIEW_ITQAN_MASK_50_REPETITIONS,
            50
        ),
        HifzTrainingStep(
            "itqan-mask-75",
            "Masquage 75 %",
            HifzTrainingKind.MASKED,
            PREVIEW_ITQAN_MASK_75_REPETITIONS,
            75
        ),
        HifzTrainingStep(
            "itqan-mask-100",
            "Masquage 100 %",
            HifzTrainingKind.MASKED,
            PREVIEW_ITQAN_MASK_100_REPETITIONS,
            100
        )
    )

    private fun murajaahSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            id = "murajaah-recall",
            label = "Récitation de révision",
            kind = HifzTrainingKind.VISIBLE,
            repetitions = MurajaahPolicy.RECITATIONS_PER_PORTION,
            maskPercent = 0,
            requiresConsecutiveSuccesses = 1
        )
    )
}
