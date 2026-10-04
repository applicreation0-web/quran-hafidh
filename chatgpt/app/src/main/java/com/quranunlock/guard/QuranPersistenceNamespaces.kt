package com.applicreation0.quransafeguard

/**
 * Persistence namespaces that must never collide. Free reading/memorisation and the
 * structured Hifz journey deliberately keep separate durable state.
 */
object QuranPersistenceNamespaces {
    const val FREE_READER_MEMORIZATION = "reader109"
    const val FREE_READER_LAST_PAGE = "free_quran_reader"
    const val HIFZ = "hifz_01010"
    const val HIFZ_READER = "hifz_reader_01010"

    init {
        val namespaces = setOf(
            FREE_READER_MEMORIZATION,
            FREE_READER_LAST_PAGE,
            HIFZ,
            HIFZ_READER
        )
        check(namespaces.size == 4) { "Quran persistence namespaces must be isolated." }
    }
}
