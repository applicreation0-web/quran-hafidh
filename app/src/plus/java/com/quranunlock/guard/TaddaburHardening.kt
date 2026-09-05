package com.applicreation0.quransafeguard

import android.content.Context

/** Pure rules used by the Taddabur reader and its regression tests. */
internal object TaddaburPageSessionPolicy {
    const val CHECKPOINT_MS = 5_000L

    fun mushafAssetPath(page: Int): String {
        require(page in 1..604) { "Mushaf page must be in 1..604" }
        return "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
    }

    fun acceptsPageCallback(currentPage: Int, callbackPage: Int): Boolean =
        currentPage == callbackPage

    fun shouldCheckpoint(pendingMs: Long, displayedTotalMs: Long): Boolean =
        pendingMs >= CHECKPOINT_MS || displayedTotalMs >= TaddaburPolicy.MIN_PAGE_MS
}

/**
 * Canonical boundary-page rule.
 *
 * Adjacent Hizb can meet in the middle of one printed Madinah page. When the
 * previous day's Hizb was completed, that physical page has already received
 * its full 90-second validation and must not be demanded again the next day.
 * The verse boundaries themselves are never changed.
 */
internal object TaddaburBoundaryPolicy {
    fun carriedBoundaryPage(
        previousHizb: Int,
        currentHizb: Int,
        previousComplete: Boolean,
    ): Int? {
        if (!previousComplete) return null
        if (TaddaburPolicy.nextHizb(previousHizb) != currentHizb) return null
        val previous = QuranStructureMetadata.division(QuranSelectionMode.HIZB, previousHizb)
        val current = QuranStructureMetadata.division(QuranSelectionMode.HIZB, currentHizb)
        return previous.endPage.takeIf { it == current.startPage }
    }
}

/** Applies the boundary carry once after a day rollover, using only verified history. */
internal object TaddaburBoundaryCarry {
    fun applyIfEligible(context: Context): Int? {
        val state = TaddaburPrefs.progress(context)
        if (state.startPage in state.completedPages) return null

        val yesterday = state.epochDay - 1L
        val previous = TaddaburPrefs.history(context, 2)
            .firstOrNull { it.epochDay == yesterday && it.complete }
            ?: return null

        val page = TaddaburBoundaryPolicy.carriedBoundaryPage(
            previousHizb = previous.hizb,
            currentHizb = state.hizb,
            previousComplete = previous.complete,
        ) ?: return null

        if (page != state.startPage) return null
        TaddaburPrefs.recordActiveMs(context, page, TaddaburPolicy.MIN_PAGE_MS)
        return page
    }
}
