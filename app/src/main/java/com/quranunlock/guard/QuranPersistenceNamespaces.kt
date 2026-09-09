package com.applicreation0.quransafeguard

/**
 * Persistence namespaces that must never collide. The free reader/memorisation mode and
 * the structured Hifz journey are intentionally separate products of state.
 */
object QuranPersistenceNamespaces {
    const val FREE_READER_MEMORIZATION = "reader109"
    const val FREE_READER_LAST_PAGE = "free_quran_reader"
    const val HIFZ = "hifz_01010"

    init {
        check(HIFZ != FREE_READER_MEMORIZATION)
        check(HIFZ != FREE_READER_LAST_PAGE)
    }
}
