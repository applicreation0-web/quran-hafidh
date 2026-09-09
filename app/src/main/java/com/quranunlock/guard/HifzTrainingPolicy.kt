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
 * Pedagogical contract for the structured Hifz journey. Audio steps are part of the
 * complete Sabqi protocol but are removed from the effective protocol when the release
 * audio gate is closed. No fake/manual repetition can stand in for unavailable audio.
 */
object HifzTrainingPolicy {

    fun stepsFor(track: HifzTrack, audioAvailable: Boolean = false): List<HifzTrainingStep> =
        rawStepsFor(track).filter { !it.requiresAudio || audioAvailable }

    private fun rawStepsFor(track: HifzTrack): List<HifzTrainingStep> = when (track) {
        HifzTrack.SABQI -> sabqiSteps()
        HifzTrack.ITQAN -> itqanSteps()
        HifzTrack.MURAJAAH -> murajaahSteps()
    }

    /**
     * When a Sabqi canonical passage had to be split into real Mushaf-line segments,
     * the segments are only preparation. A final whole-passage recall is mandatory
     * before the canonical task can be completed.
     */
    fun assemblyStepsFor(track: HifzTrack): List<HifzTrainingStep> = when (track) {
        HifzTrack.SABQI -> listOf(
            HifzTrainingStep(
                id = "sabqi-assembly-final",
                label = "Assemblage final du passage",
                kind = HifzTrainingKind.FINAL_TEST,
                repetitions = 3,
                maskPercent = 100,
                requiresConsecutiveSuccesses = 3
            )
        )
        HifzTrack.ITQAN,
        HifzTrack.MURAJAAH -> emptyList()
    }

    private fun sabqiSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            id = "sabqi-audio-passive",
            label = "Écoute passive",
            kind = HifzTrainingKind.AUDIO_PASSIVE,
            repetitions = 2,
            maskPercent = 0,
            requiresAudio = true
        ),
        HifzTrainingStep(
            id = "sabqi-audio-active",
            label = "Écoute active",
            kind = HifzTrainingKind.AUDIO_ACTIVE,
            repetitions = 3,
            maskPercent = 0,
            requiresAudio = true
        ),
        HifzTrainingStep("sabqi-visible", "Texte visible", HifzTrainingKind.VISIBLE, 10, 0),
        HifzTrainingStep("sabqi-mask-25", "Masquage 25 %", HifzTrainingKind.MASKED, 5, 25),
        HifzTrainingStep("sabqi-mask-50", "Masquage 50 %", HifzTrainingKind.MASKED, 5, 50),
        HifzTrainingStep("sabqi-mask-75", "Masquage 75 %", HifzTrainingKind.MASKED, 5, 75),
        HifzTrainingStep("sabqi-mask-100", "Masquage 100 %", HifzTrainingKind.MASKED, 7, 100),
        HifzTrainingStep(
            id = "sabqi-final",
            label = "Test final du segment entièrement masqué",
            kind = HifzTrainingKind.FINAL_TEST,
            repetitions = 3,
            maskPercent = 100,
            requiresConsecutiveSuccesses = 3
        )
    )

    private fun itqanSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            id = "itqan-visible-30",
            label = "Consolidation texte visible",
            kind = HifzTrainingKind.VISIBLE,
            repetitions = 30,
            maskPercent = 0
        ),
        HifzTrainingStep("itqan-mask-25", "Masquage 25 %", HifzTrainingKind.MASKED, 1, 25, requiresConsecutiveSuccesses = 1),
        HifzTrainingStep("itqan-mask-50", "Masquage 50 %", HifzTrainingKind.MASKED, 1, 50, requiresConsecutiveSuccesses = 1),
        HifzTrainingStep("itqan-mask-75", "Masquage 75 %", HifzTrainingKind.MASKED, 1, 75, requiresConsecutiveSuccesses = 1),
        HifzTrainingStep("itqan-mask-100", "Masquage 100 %", HifzTrainingKind.MASKED, 1, 100, requiresConsecutiveSuccesses = 1),
        HifzTrainingStep(
            id = "itqan-final",
            label = "Test final entièrement masqué",
            kind = HifzTrainingKind.FINAL_TEST,
            repetitions = 1,
            maskPercent = 100,
            requiresConsecutiveSuccesses = 1
        )
    )

    private fun murajaahSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            id = "murajaah-recall",
            label = "Récitation de révision",
            kind = HifzTrainingKind.FINAL_TEST,
            repetitions = MurajaahPolicy.RECITATIONS_PER_PORTION,
            maskPercent = 100,
            requiresConsecutiveSuccesses = 1
        )
    )
}
