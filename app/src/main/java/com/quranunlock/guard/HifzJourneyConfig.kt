package com.applicreation0.quransafeguard

/** User-declared active time available for each structured Hifz track. */
data class HifzAvailableMinutes(
    val sabqi: Int? = null,
    val itqan: Int? = null,
    val murajaah: Int? = null
) {
    init {
        listOfNotNull(sabqi, itqan, murajaah).forEach {
            require(it > 0) { "Configured Hifz time must be positive." }
        }
    }

    fun forTrack(track: HifzTrack): Int? = when (track) {
        HifzTrack.SABQI -> sabqi
        HifzTrack.ITQAN -> itqan
        HifzTrack.MURAJAAH -> murajaah
    }
}

/** Persistent rare setup, available time and measured pace for the Hifz journey. */
data class HifzJourneyConfig(
    val bounds: HifzJourneyBounds? = null,
    val pace: HifzPaceProfile = HifzPaceProfile(),
    val availableMinutes: HifzAvailableMinutes = HifzAvailableMinutes()
)
