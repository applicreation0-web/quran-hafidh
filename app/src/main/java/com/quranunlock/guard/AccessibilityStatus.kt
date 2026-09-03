package com.applicreation0.quransafeguard

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

    fun isOtherEditionEnabled(context: Context): Boolean {
        val otherPackage = if (context.packageName.endsWith(".plus")) {
            "com.applicreation0.quransafeguard"
        } else {
            "com.applicreation0.quransafeguard.plus"
        }
        val otherComponent = "$otherPackage/" +
            "com.applicreation0.quransafeguard.QuranAccessibilityService"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any {
            it.equals(otherComponent, ignoreCase = true)
        }
    }
}
