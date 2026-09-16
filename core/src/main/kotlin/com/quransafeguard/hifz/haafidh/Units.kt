package com.quransafeguard.hifz.haafidh

/**
 * Quran Haafidh core rules.
 * Sabqi: 5 lines. Itqan: 7-8 lines.
 * No unit crosses a surah boundary.
 */

data class MushafLine(
    val lineId: Int,
    val page: Int,
    val surah: Int,
    val endsVerse: Boolean,
)

enum class Track { SABQI, ITQAN }

data class HaafidhUnit(
    val id: String,
    val track: Track,
    val surah: Int,
    val lineIds: List<Int>,
) { val size: Int get() = lineIds.size }

object Segmenter {
    const val SABQI_LINES = 5
    const val ITQAN_MIN = 7
    const val ITQAN_MAX = 8

    fun validate(unit: HaafidhUnit): Boolean {
        return when (unit.track) {
            Track.SABQI -> unit.size == SABQI_LINES
            Track.ITQAN -> unit.size in ITQAN_MIN..ITQAN_MAX
        }
    }
}
