package com.applicreation0.quransafeguard

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

data class HifzReadingCounts(
    val visible: Int = 0,
    val masked: Int = 0
) {
    init {
        require(visible >= 0)
        require(masked >= 0)
    }
}

/**
 * Separate, persistent counters required by the 0.10.10 product contract.
 * Audio playback is never a reading. Every manual Hifz attempt is classified from the
 * actual active step: maskPercent == 0 => visible, otherwise masked.
 */
object HifzReadingCountPolicy {
    fun isReading(step: HifzTrainingStep): Boolean =
        step.kind != HifzTrainingKind.AUDIO_PASSIVE && step.kind != HifzTrainingKind.AUDIO_ACTIVE

    fun add(counts: HifzReadingCounts, step: HifzTrainingStep): HifzReadingCounts {
        if (!isReading(step)) return counts
        return if (step.maskPercent > 0) {
            counts.copy(masked = Math.addExact(counts.masked, 1))
        } else {
            counts.copy(visible = Math.addExact(counts.visible, 1))
        }
    }
}

object HifzReadingStatsStore {
    private const val FILE = "quran_safeguard_hifz_reading_stats"

    private fun key(taskId: String, suffix: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(taskId.toByteArray(StandardCharsets.UTF_8))
        return "$encoded:$suffix"
    }

    fun load(context: Context, taskId: String): HifzReadingCounts {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return HifzReadingCounts(
            visible = prefs.getInt(key(taskId, "visible"), 0).coerceAtLeast(0),
            masked = prefs.getInt(key(taskId, "masked"), 0).coerceAtLeast(0)
        )
    }

    @Synchronized
    fun record(context: Context, taskId: String, step: HifzTrainingStep): Boolean {
        if (!HifzReadingCountPolicy.isReading(step)) return true
        val next = HifzReadingCountPolicy.add(load(context, taskId), step)
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(key(taskId, "visible"), next.visible)
            .putInt(key(taskId, "masked"), next.masked)
            .commit()
    }
}
