package com.quransafeguard.hifz.domain

import com.quransafeguard.hifz.core.ItqanMaskPlan
import com.quransafeguard.hifz.core.ItqanProgress

class ItqanEngine(
    val maskPlan: ItqanMaskPlan = ItqanMaskPlan.WORKING_DEFAULT
) {
    fun nextMask(progress: ItqanProgress): MaskStage? = progress.nextStage(maskPlan)

    fun completeRepetition(progress: ItqanProgress, assisted: Boolean = false): ItqanProgress =
        progress.completeRepetition(assisted)
}
