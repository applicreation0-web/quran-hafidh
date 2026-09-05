package com.applicreation0.quransafeguard

import java.security.SecureRandom

enum class QuranSelectionMode {
    JUZ,
    HIZB
}

/**
 * Historical name kept for source compatibility. The plan now represents a
 * page quota drawn strictly from the user's canonical Juz/Hizb selection.
 * `hizbNumbers` therefore contains the selected unit numbers for the active
 * mode (Juz or Hizb) and must not be interpreted as proof that 10 pages = 1 Hizb.
 */
data class HizbPagePlan(
    val pages: List<Int>,
    val hizbNumbers: List<Int>,
    val nextCursor: Int
)

object QuranPageSelector {
    private val secureRandom = SecureRandom()

    fun canonicalPages(
        mode: QuranSelectionMode,
        unit: Int
    ): List<Int> {
        val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
        require(unit in 1..maxUnit) {
            "${mode.name} must be between 1 and $maxUnit."
        }
        return QuranStructureMetadata.division(mode, unit).pageRange.toList()
    }

    fun availablePages(
        mode: QuranSelectionMode,
        selectedUnits: Set<Int>
    ): List<Int> {
        val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
        val validUnits = selectedUnits
            .filter { it in 1..maxUnit }
            .distinct()
            .sorted()
            .ifEmpty { (1..maxUnit).toList() }

        return validUnits
            .flatMap { canonicalPages(mode, it) }
            .distinct()
            .sorted()
    }

    /**
     * Builds a Safeguard reading quota without ever escaping the exact
     * canonical pool selected by the user.
     *
     * A challenge never wraps from the end of the selected pool back to its
     * beginning. If fewer pages remain than the requested quota, the current
     * plan ends at the canonical pool boundary and the following challenge
     * restarts from the beginning. This preserves normal Mushaf reading order.
     */
    fun sequentialCanonicalQuotaPages(
        mode: QuranSelectionMode,
        selectedUnits: Set<Int>,
        cursor: Int,
        pageCount: Int
    ): HizbPagePlan {
        require(pageCount > 0) { "Page quota must be positive." }
        val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
        val validUnits = selectedUnits
            .filter { it in 1..maxUnit }
            .distinct()
            .sorted()
            .ifEmpty { (1..maxUnit).toList() }
        val pool = availablePages(mode, validUnits.toSet())
        require(pool.isNotEmpty()) { "No Quran pages available." }

        val startIndex = Math.floorMod(cursor, pool.size)
        val remaining = pool.size - startIndex
        val actualCount = minOf(pageCount, remaining)
        val pages = pool.subList(startIndex, startIndex + actualCount).toList()
        val selectedSet = validUnits.toSet()
        val touchedUnits = pages
            .flatMap { page ->
                QuranStructureMetadata.divisionsForPage(mode, page)
                    .map(QuranDivision::number)
            }
            .filter { it in selectedSet }
            .distinct()

        val endIndex = startIndex + actualCount
        return HizbPagePlan(
            pages = pages,
            hizbNumbers = touchedUnits,
            nextCursor = if (endIndex >= pool.size) 0 else endIndex
        )
    }

    /**
     * Compatibility helper for older call sites. It now means "up to the
     * 10-page Safeguard quota, strictly inside this canonical Hizb".
     */
    fun tenPageQuotaFromHizb(hizb: Int): List<Int> =
        sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(hizb),
            cursor = 0,
            pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT
        ).pages

    /**
     * Compatibility helper. `hizbCount` controls the requested number of
     * 10-page Safeguard quota blocks; pages remain strictly inside the selected
     * canonical Hizb pool.
     */
    fun sequentialHizbPages(
        selectedHizb: Set<Int>,
        cursor: Int,
        hizbCount: Int
    ): HizbPagePlan {
        require(hizbCount > 0)
        return sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = selectedHizb,
            cursor = cursor,
            pageCount = hizbCount * UsageCyclePolicy.HIZB_PAGE_COUNT
        )
    }

    fun randomPage(
        mode: QuranSelectionMode,
        selectedUnits: Set<Int>,
        recentPagesNewestFirst: List<Int>,
        maxRecentExclusions: Int = 30
    ): Int {
        val candidates = availablePages(mode, selectedUnits)
        require(candidates.isNotEmpty()) { "No Quran pages available." }

        val candidateSet = candidates.toSet()
        val maxExclusions = minOf(
            maxRecentExclusions,
            (candidates.size - 1).coerceAtLeast(0)
        )

        val excluded = recentPagesNewestFirst
            .asSequence()
            .filter { it in candidateSet }
            .distinct()
            .take(maxExclusions)
            .toSet()

        val eligible = candidates.filterNot { it in excluded }
            .ifEmpty { candidates }

        return eligible[secureRandom.nextInt(eligible.size)]
    }

    fun juzForPage(page: Int): List<Int> =
        QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.JUZ,
            page
        ).map(QuranDivision::number)

    fun hizbForPage(page: Int): List<Int> =
        QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.HIZB,
            page
        ).map(QuranDivision::number)
}
