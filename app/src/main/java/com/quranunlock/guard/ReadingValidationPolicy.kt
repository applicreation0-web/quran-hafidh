package com.applicreation0.quransafeguard

object ReadingValidationPolicy {
    const val MIN_ACTIVE_READING_MS = 60_000L

    fun canValidate(activeReadingMs: Long, bottomReached: Boolean): Boolean =
        bottomReached && activeReadingMs >= MIN_ACTIVE_READING_MS

    fun remainingMs(activeReadingMs: Long): Long =
        (MIN_ACTIVE_READING_MS - activeReadingMs.coerceAtLeast(0L)).coerceAtLeast(0L)
}
