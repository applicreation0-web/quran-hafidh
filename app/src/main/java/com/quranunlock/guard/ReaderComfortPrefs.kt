package com.applicreation0.quransafeguard

import android.content.Context
import android.view.WindowManager

enum class ReaderVisualMode {
    COMFORT,
    LIGHT,
    DARK
}

/**
 * Local-only visual preferences for voluntary Qur'an reading.
 *
 * These settings never touch GuardPrefs, SafeguardCyclePrefs or challenge state.
 * They therefore cannot change Juz/Hizb progression, unlock credit or protected-app timing.
 */
object ReaderComfortPrefs {
    private const val PREFS = "reader_comfort"
    private const val KEY_VISUAL_MODE = "visual_mode"
    private const val KEY_BRIGHTNESS = "brightness"
    private const val SYSTEM_BRIGHTNESS = -1f

    fun visualMode(context: Context): ReaderVisualMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VISUAL_MODE, ReaderVisualMode.COMFORT.name)
        return runCatching { ReaderVisualMode.valueOf(raw ?: ReaderVisualMode.COMFORT.name) }
            .getOrDefault(ReaderVisualMode.COMFORT)
    }

    fun setVisualMode(context: Context, mode: ReaderVisualMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VISUAL_MODE, mode.name)
            .apply()
    }

    /** -1f means follow Android/system brightness. */
    fun brightness(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_BRIGHTNESS, SYSTEM_BRIGHTNESS)
            .let { value ->
                if (value == SYSTEM_BRIGHTNESS) SYSTEM_BRIGHTNESS else value.coerceIn(0.12f, 1f)
            }

    fun setBrightness(context: Context, value: Float?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BRIGHTNESS, value?.coerceIn(0.12f, 1f) ?: SYSTEM_BRIGHTNESS)
            .apply()
    }

    fun applyBrightness(window: android.view.Window, value: Float) {
        val params = window.attributes
        params.screenBrightness = if (value < 0f) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            value.coerceIn(0.12f, 1f)
        }
        window.attributes = params
    }

    fun pageBackground(mode: ReaderVisualMode): String = when (mode) {
        ReaderVisualMode.COMFORT -> "#F4F0E6"
        ReaderVisualMode.LIGHT -> "#FCFBF7"
        ReaderVisualMode.DARK -> "#E7DFD1"
    }
}
