package com.applicreation0.quransafeguard

import android.content.Context

/** Light edition never bundles the personal Hikam commentary corpus. */
object HikamSharhEdition {
    const val isEnabled: Boolean = false

    fun forHikma(
        context: Context,
        hikmaNumber: Int
    ): List<HikamSharhAvailability> = emptyList()
}
