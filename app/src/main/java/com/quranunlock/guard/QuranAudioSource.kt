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

    fun isValidReference(surah: Int, ayah: Int): Boolean =
        surah in 1..114 && ayah in 1..286

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
