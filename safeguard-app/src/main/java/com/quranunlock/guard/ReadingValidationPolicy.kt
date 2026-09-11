package com.applicreation0.quransafeguard

object ReadingValidationPolicy {
    const val MIN_ACTIVE_READING_MS = 60_000L
    const val SCROLL_TOLERANCE_PX = 24

    fun requiresScroll(
        contentHeightPx: Int,
        viewportHeightPx: Int,
        tolerancePx: Int = SCROLL_TOLERANCE_PX
    ): Boolean =
        contentHeightPx > viewportHeightPx + tolerancePx.coerceAtLeast(0)

    fun canValidate(activeReadingMs: Long): Boolean =
        activeReadingMs >= MIN_ACTIVE_READING_MS

    fun remainingMs(activeReadingMs: Long): Long =
        (MIN_ACTIVE_READING_MS - activeReadingMs.coerceAtLeast(0L)).coerceAtLeast(0L)
}
