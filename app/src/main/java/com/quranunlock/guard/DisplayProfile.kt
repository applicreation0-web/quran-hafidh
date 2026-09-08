package com.applicreation0.quransafeguard

import android.content.Context
import android.os.Build

enum class DisplayProfilePreference {
    AUTOMATIC,
    STANDARD,
    EINK
}

enum class DisplayProfile {
    STANDARD,
    EINK
}

/**
 * The only place in the application that detects an E-Ink device.
 *
 * Automatic detection is deliberately conservative. A user override always wins,
 * including forcing STANDARD on a device that matches the heuristic.
 */
object DisplayProfileManager {
    private const val PREFS = "display_profile"
    private const val KEY = "preference"

    fun preference(context: Context): DisplayProfilePreference {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, DisplayProfilePreference.AUTOMATIC.name)
        return runCatching { DisplayProfilePreference.valueOf(stored.orEmpty()) }
            .getOrDefault(DisplayProfilePreference.AUTOMATIC)
    }

    fun setPreference(context: Context, value: DisplayProfilePreference) {
        check(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, value.name)
                .commit()
        )
    }

    fun resolve(context: Context): DisplayProfile = resolvePreference(
        preference(context),
        looksLikeEInkDevice(context, Build.MANUFACTURER, Build.BRAND, Build.MODEL)
    )

    internal fun resolvePreference(
        preference: DisplayProfilePreference,
        automaticDetection: Boolean
    ): DisplayProfile = when (preference) {
        DisplayProfilePreference.STANDARD -> DisplayProfile.STANDARD
        DisplayProfilePreference.EINK -> DisplayProfile.EINK
        DisplayProfilePreference.AUTOMATIC ->
            if (automaticDetection) DisplayProfile.EINK else DisplayProfile.STANDARD
    }

    internal fun looksLikeEInkDevice(
        context: Context?,
        manufacturer: String?,
        brand: String?,
        model: String?
    ): Boolean {
        val signature = listOf(manufacturer, brand, model)
            .joinToString(" ")
            .lowercase()
        val known = listOf(
            "onyx", "boox", "boyue", "likebook", "meebook",
            "remarkable", "inkbook", "pocketbook", "bigme"
        )
        if (known.any(signature::contains)) return true
        val features = listOf(
            "com.onyx.android.feature.ONYX",
            "android.hardware.eink",
            "android.software.eink"
        )
        return context != null && features.any(context.packageManager::hasSystemFeature)
    }

    fun motionDurationMillis(profile: DisplayProfile): Int =
        if (profile == DisplayProfile.EINK) 0 else 200
}

