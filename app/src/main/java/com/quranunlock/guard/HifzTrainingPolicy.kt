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
 * Pedagogical contract for the structured Hifz journey.
 *
 * This does not replace reader109's free Memorisation protocol. It describes what a
 * structured Hifz task expects so the reader can later execute the task without owning
 * the Hifz schedule or persistence.
 */
object HifzTrainingPolicy {

    fun stepsFor(track: HifzTrack): List<HifzTrainingStep> = when (track) {
        HifzTrack.SABQI -> sabqiSteps()
        HifzTrack.ITQAN -> itqanSteps()
        HifzTrack.MURAJAAH -> emptyList()
    }

    /**
     * Sabqi keeps the 0.10.9 memorisation principles: audio preparation followed by
     * progressive masking. The final step is fully masked and requires successful recall.
     */
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
            label = "Test final entièrement masqué",
            kind = HifzTrainingKind.FINAL_TEST,
            repetitions = 3,
            maskPercent = 100,
            requiresConsecutiveSuccesses = 3
        )
    )

    /**
     * Itqan is consolidation, not new learning. One already learnt page is repeated
     * thirty times before progressive masking. No audio step is required by this contract.
     */
    private fun itqanSteps(): List<HifzTrainingStep> = listOf(
        HifzTrainingStep(
            id = "itqan-visible-30",
            label = "Consolidation texte visible",
            kind = HifzTrainingKind.VISIBLE,
            repetitions = 30,
            maskPercent = 0
        ),
        HifzTrainingStep("itqan-mask-25", "Masquage 25 %", HifzTrainingKind.MASKED, 1, 25),
        HifzTrainingStep("itqan-mask-50", "Masquage 50 %", HifzTrainingKind.MASKED, 1, 50),
        HifzTrainingStep("itqan-mask-75", "Masquage 75 %", HifzTrainingKind.MASKED, 1, 75),
        HifzTrainingStep("itqan-mask-100", "Masquage 100 %", HifzTrainingKind.MASKED, 1, 100),
        HifzTrainingStep(
            id = "itqan-final",
            label = "Test final entièrement masqué",
            kind = HifzTrainingKind.FINAL_TEST,
            repetitions = 1,
            maskPercent = 100,
            requiresConsecutiveSuccesses = 1
        )
    )
}
