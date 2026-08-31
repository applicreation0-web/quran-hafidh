package com.applicreation0.quransafeguard

import java.security.SecureRandom

enum class QuranSelectionMode {
    JUZ,
    HIZB
}

object QuranPageSelector {
    private val secureRandom = SecureRandom()

    private val pagesByJuz: Map<Int, IntRange> = mapOf(
        1 to (1..21),
        2 to (22..41),
        3 to (42..61),
        4 to (62..81),
        5 to (82..101),
        6 to (102..121),
        7 to (121..141),
        8 to (142..161),
        9 to (162..181),
        10 to (182..201),
        11 to (202..221),
        12 to (222..241),
        13 to (242..261),
        14 to (262..281),
        15 to (282..301),
        16 to (302..321),
        17 to (322..341),
        18 to (342..361),
        19 to (362..381),
        20 to (382..401),
        21 to (402..421),
        22 to (422..441),
        23 to (442..461),
        24 to (462..481),
        25 to (482..502),
        26 to (502..521),
        27 to (522..541),
        28 to (542..561),
        29 to (562..581),
        30 to (582..604)
    )

    // First Madani Mushaf page containing the start of each of the 60 Hizb.
    private val hizbStartPages = listOf(
        1, 11, 22, 32, 42, 51, 62, 72, 82, 92,
        102, 112, 121, 132, 142, 151, 162, 173, 182, 192,
        201, 212, 222, 231, 242, 252, 262, 272, 282, 292,
        302, 312, 322, 332, 342, 352, 362, 371, 382, 392,
        402, 413, 422, 431, 442, 451, 462, 472, 482, 491,
        502, 513, 522, 531, 542, 553, 562, 572, 582, 591
    )

    private val pagesByHizb: Map<Int, IntRange> =
        (1..60).associateWith { hizb ->
            val start = hizbStartPages[hizb - 1]
            val end = if (hizb == 60) 604 else hizbStartPages[hizb]
            start..end
        }

    fun availablePages(
        mode: QuranSelectionMode,
        selectedUnits: Set<Int>
    ): List<Int> {
        val ranges = when (mode) {
            QuranSelectionMode.JUZ -> {
                val valid = selectedUnits.filter { it in 1..30 }
                    .ifEmpty { (1..30).toList() }
                valid.map { pagesByJuz.getValue(it) }
            }

            QuranSelectionMode.HIZB -> {
                val valid = selectedUnits.filter { it in 1..60 }
                    .ifEmpty { (1..60).toList() }
                valid.map { pagesByHizb.getValue(it) }
            }
        }

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
        pagesByJuz.filterValues { page in it }.keys.sorted()

    fun hizbForPage(page: Int): List<Int> =
        pagesByHizb.filterValues { page in it }.keys.sorted()
}
