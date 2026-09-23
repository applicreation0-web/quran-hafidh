package com.applicreation0.quransafeguard

/**
 * Private local-download source for Al-Husary Muʿallim (Hafṣ).
 *
 * Audio is never bundled in the APK. Every downloaded file is one complete ayah,
 * keeping playback/repetition and visual highlighting aligned to the whole verse.
 * Canonical verse validation is shared with Hifz/Tafsir through QuranCanonicalBounds.
 */
object QuranAudioSource {
    const val RECITER_NAME = "Mahmoud Khalil Al-Husary — Muʿallim (Hafṣ)"
    const val SOURCE_LABEL = "EveryAyah — Husary_Muallim_128kbps"
    const val BASE_URL = "https://everyayah.com/data/Husary_Muallim_128kbps"
    const val STORAGE_VERSION = "husary-muallim-everyayah-v1"

    private val allowedHosts = setOf("everyayah.com", "www.everyayah.com")

    fun ayahCount(surah: Int): Int {
        require(surah in 1..QuranCanonicalBounds.SURAH_COUNT) { "Sourate invalide: $surah" }
        return requireNotNull(QuranCanonicalBounds.ayahCount(surah))
    }

    fun isValidReference(surah: Int, ayah: Int): Boolean =
        QuranCanonicalBounds.isValid(QuranVerseRef(surah, ayah))

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
