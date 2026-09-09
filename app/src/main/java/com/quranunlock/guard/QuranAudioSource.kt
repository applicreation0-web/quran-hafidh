package com.applicreation0.quransafeguard

/**
 * Private local-download source for Al-Husary Muʿallim (Hafṣ).
 *
 * Audio is never bundled in the APK. Every downloaded file is one complete
 * ayah, which keeps playback/repetition and visual highlighting aligned to the
 * whole verse without word-level timing data.
 */
object QuranAudioSource {
    const val RECITER_NAME = "Mahmoud Khalil Al-Husary — Muʿallim (Hafṣ)"
    const val SOURCE_LABEL = "EveryAyah — Husary_Muallim_128kbps"
    const val BASE_URL = "https://everyayah.com/data/Husary_Muallim_128kbps"
    const val STORAGE_VERSION = "husary-muallim-everyayah-v1"

    private val allowedHosts = setOf("everyayah.com", "www.everyayah.com")

    /** Canonical ayah count for each surah, indexed by surah - 1. */
    private val ayahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99,
        128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34,
        30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29,
        18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19,
        36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    fun ayahCount(surah: Int): Int {
        require(surah in 1..ayahCounts.size) { "Sourate invalide: $surah" }
        return ayahCounts[surah - 1]
    }

    fun isValidReference(surah: Int, ayah: Int): Boolean =
        surah in 1..ayahCounts.size && ayah in 1..ayahCounts[surah - 1]

    fun fileName(surah: Int, ayah: Int): String {
        require(isValidReference(surah, ayah))
        return "%03d%03d.mp3".format(surah, ayah)
    }

    fun url(surah: Int, ayah: Int): String =
        "$BASE_URL/${fileName(surah, ayah)}"

    fun isAllowedHttpsUrl(protocol: String?, host: String?): Boolean =
        protocol.equals("https", ignoreCase = true) &&
            host?.lowercase() in allowedHosts
}