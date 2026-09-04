package com.applicreation0.quransafeguard

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

/**
 * Taddabur is intentionally unavailable in the Light edition.
 * Keep this stub free of storage, alarms, UI strings and blocking behavior.
 */
object TaddaburEdition {
    const val isEnabled: Boolean = false

    @Composable
    fun DashboardCard() = Unit

    fun shouldBlockNow(context: Context): Boolean = false

    fun scheduleReminder(context: Context) = Unit

    fun handlesReminder(action: String?): Boolean = false

    fun handleReminder(context: Context, action: String?) = Unit

    fun renderBlockingGate(
        activity: ComponentActivity,
        targetPackage: String
    ): Boolean = false
}
