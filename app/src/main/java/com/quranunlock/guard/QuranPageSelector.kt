package com.quranunlock.guard

import kotlin.random.Random

object QuranPageSelector {
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

    fun randomPage(selectedJuz: Set<Int>): Int {
        val validJuz = selectedJuz.filter { it in 1..30 }.ifEmpty { (1..30).toList() }
        val candidatePages = validJuz
            .flatMap { pagesByJuz.getValue(it).toList() }
            .distinct()
        return candidatePages[Random.nextInt(candidatePages.size)]
    }

    fun juzForPage(page: Int): List<Int> =
        pagesByJuz.filterValues { page in it }.keys.sorted()
}
