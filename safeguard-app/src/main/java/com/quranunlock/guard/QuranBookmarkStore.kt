package com.applicreation0.quransafeguard

import android.content.Context

/** Persistent user bookmarks for the voluntary Qur'an reader. */
internal object QuranBookmarkStore {
    private const val PREFS = "free_quran_reader"
    private const val KEY_BOOKMARK_PAGES = "bookmark_pages"
    private const val LEGACY_SINGLE_BOOKMARK = "bookmark_page"
    private const val FIRST_PAGE = 1
    private const val LAST_PAGE = 604

    fun load(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_BOOKMARK_PAGES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in FIRST_PAGE..LAST_PAGE }
            .toSet()

        if (stored.isNotEmpty()) return stored

        val legacy = prefs.getInt(LEGACY_SINGLE_BOOKMARK, 0)
            .takeIf { it in FIRST_PAGE..LAST_PAGE }
            ?: return emptySet()

        val migrated = setOf(legacy)
        persist(context, migrated)
        return migrated
    }

    fun toggle(context: Context, page: Int): Set<Int> {
        if (page !in FIRST_PAGE..LAST_PAGE) return load(context)
        val updated = load(context).toMutableSet().apply {
            if (!add(page)) remove(page)
        }.toSet()
        persist(context, updated)
        return updated
    }

    private fun persist(context: Context, pages: Set<Int>) {
        val encoded = pages
            .filter { it in FIRST_PAGE..LAST_PAGE }
            .map(Int::toString)
            .toSet()
        check(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_BOOKMARK_PAGES, encoded)
                .remove(LEGACY_SINGLE_BOOKMARK)
                .commit()
        ) { "Unable to persist Qur'an bookmarks" }
    }
}
