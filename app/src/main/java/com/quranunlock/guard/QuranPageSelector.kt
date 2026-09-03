package com.applicreation0.quransafeguard

import java.security.SecureRandom

enum class QuranSelectionMode {
    JUZ,
    HIZB
}

data class HizbPagePlan(
    val pages: List<Int>,
    val hizbNumbers: List<Int>,
    val nextCursor: Int
)

object QuranPageSelector {
    private val secureRandom = SecureRandom()

    /**
     * The protection quota remains ten timed pages, while the UI separately
     * exposes the exact verse boundary and allows voluntary continuation.
     */
    fun tenPageQuotaFromHizb(hizb: Int): List<Int> {
        require(hizb in 1..60) { "Hizb must be between 1 and 60." }
        val division = QuranStructureMetadata.division(
            QuranSelectionMode.HIZB,
            hizb
        )
        return (division.startPage until
            (division.startPage + UsageCyclePolicy.HIZB_PAGE_COUNT))
            .filter { it in 1..604 }
            .take(UsageCyclePolicy.HIZB_PAGE_COUNT)
    }

    fun sequentialHizbPages(
        selectedHizb: Set<Int>,
        cursor: Int,
        hizbCount: Int
    ): HizbPagePlan {
        require(hizbCount > 0)
        val pool = selectedHizb.filter { it in 1..60 }
            .distinct()
            .sorted()
            .ifEmpty { (1..60).toList() }
        val startIndex = Math.floorMod(cursor, pool.size)
        val selected = (0 until hizbCount).map { offset ->
            pool[(startIndex + offset) % pool.size]
        }
        val pages = selected.flatMap(::tenPageQuotaFromHizb)
        return HizbPagePlan(
            pages = pages,
            hizbNumbers = selected,
            nextCursor = if (pool.size == 1) 0 else (startIndex + hizbCount) % pool.size
        )
    }

    fun availablePages(
        mode: QuranSelectionMode,
        selectedUnits: Set<Int>
    ): List<Int> {
        val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
        val ranges = selectedUnits
            .filter { it in 1..maxUnit }
            .ifEmpty { (1..maxUnit).toList() }
            .map { QuranStructureMetadata.division(mode, it).pageRange }

        return ranges
            .flatMap { it.toList() }
            .distinct()
            .sorted()
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
