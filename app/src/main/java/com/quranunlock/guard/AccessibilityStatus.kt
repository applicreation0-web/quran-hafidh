package com.quranunlock.guard

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessibilityStatus {
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, QuranAccessibilityService::class.java)
            .flattenToString()

        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        return enabled
            .split(':')
            .any { it.equals(expected, ignoreCase = true) }
    }
}
