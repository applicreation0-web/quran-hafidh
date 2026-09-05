package com.applicreation0.quransafeguard

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

/**
 * Taddabur is intentionally deferred from the published 0.10.5 Plus runtime.
 *
 * Its core rules and regression material remain preserved in the repository for
 * a later release, but 0.10.5 must not schedule Taddabur alarms, show a Taddabur
 * dashboard card, or block protected applications. This keeps Tafsir, Qur'an
 * reading and the normal Safeguard budget path independent from the optional gate.
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

    fun open(context: Context) = Unit
}
