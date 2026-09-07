package com.applicreation0.quransafeguard

import android.content.Context
import android.view.WindowManager

/** Legacy persisted values from <=0.10.7. They no longer change Quran reader colors. */
enum class ReaderVisualMode {
    COMFORT,
    LIGHT,
    DARK
}

/**
 * Shared local comfort settings for every Quran reader.
 *
 * 0.10.8 freezes the reader surface to cream. The sun control adjusts only window
 * brightness and never changes theme, challenge state, Juz/Hizb progression, unlock
 * credit, or protected-app timing.
 */
object ReaderComfortPrefs {
    internal const val READER_CREAM_HEX = "#F7F2E8"
    private const val PREFS = "reader_comfort"
    private const val KEY_VISUAL_MODE = "visual_mode"
    private const val KEY_BRIGHTNESS = "brightness"
    private const val SYSTEM_BRIGHTNESS = -1f

    /** Compatibility read: all historical modes now resolve to the single cream mode. */
    fun visualMode(context: Context): ReaderVisualMode = ReaderVisualMode.COMFORT

    /** Compatibility write: never persist a non-cream visual mode again. */
    fun setVisualMode(context: Context, mode: ReaderVisualMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VISUAL_MODE, ReaderVisualMode.COMFORT.name)
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

    fun pageBackground(): String = READER_CREAM_HEX

    /** Compatibility overload for <=0.10.7 callers; every mode is cream in 0.10.8. */
    fun pageBackground(mode: ReaderVisualMode): String = READER_CREAM_HEX
}
