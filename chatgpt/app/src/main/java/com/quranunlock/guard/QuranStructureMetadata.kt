package com.applicreation0.quransafeguard

/**
 * Exact Juz/Hizb verse boundaries derived from Tanzil Quran Metadata 1.0
 * (CC-BY, © 2008–2009 Tanzil.info):
 * https://tanzil.net/docs/quran_metadata
 *
 * Page numbers target the 604-page Medina Mushaf. A structural boundary may
 * occur inside a printed page, so boundary pages legitimately belong to two
 * adjacent divisions.
 */
data class QuranVerseRef(
    val surah: Int,
    val ayah: Int
) {
    val label: String
        get() = "$surah:$ayah"
}

data class QuranDivision(
    val number: Int,
    val start: QuranVerseRef,
    val end: QuranVerseRef,
    val startPage: Int,
    val endPage: Int,
    val startsInsidePage: Boolean,
    val endsInsidePage: Boolean
) {
    val pageRange: IntRange
        get() = startPage..endPage

    val verseRangeLabel: String
        get() = "${start.label} → ${end.label}"

    val pageRangeLabel: String
        get() = if (startPage == endPage) {
            "page $startPage"
        } else {
            "pages $startPage–$endPage"
        }
}

object QuranStructureMetadata {
    const val SOURCE_LABEL = "Tanzil Quran Metadata 1.0"
    const val SOURCE_URL = "https://tanzil.net/docs/quran_metadata"

    private val juzDivisions = listOf(
        QuranDivision(1, QuranVerseRef(1, 1), QuranVerseRef(2, 141), 1, 21, false, false),
        QuranDivision(2, QuranVerseRef(2, 142), QuranVerseRef(2, 252), 22, 41, false, false),
        QuranDivision(3, QuranVerseRef(2, 253), QuranVerseRef(3, 92), 42, 62, false, true),
        QuranDivision(4, QuranVerseRef(3, 93), QuranVerseRef(4, 23), 62, 81, true, false),
        QuranDivision(5, QuranVerseRef(4, 24), QuranVerseRef(4, 147), 82, 101, false, false),
        QuranDivision(6, QuranVerseRef(4, 148), QuranVerseRef(5, 81), 102, 121, false, true),
        QuranDivision(7, QuranVerseRef(5, 82), QuranVerseRef(6, 110), 121, 141, true, false),
        QuranDivision(8, QuranVerseRef(6, 111), QuranVerseRef(7, 87), 142, 161, false, false),
        QuranDivision(9, QuranVerseRef(7, 88), QuranVerseRef(8, 40), 162, 181, false, false),
        QuranDivision(10, QuranVerseRef(8, 41), QuranVerseRef(9, 92), 182, 201, false, true),
        QuranDivision(11, QuranVerseRef(9, 93), QuranVerseRef(11, 5), 201, 221, true, false),
        QuranDivision(12, QuranVerseRef(11, 6), QuranVerseRef(12, 52), 222, 241, false, false),
        QuranDivision(13, QuranVerseRef(12, 53), QuranVerseRef(14, 52), 242, 261, false, false),
        QuranDivision(14, QuranVerseRef(15, 1), QuranVerseRef(16, 128), 262, 281, false, false),
        QuranDivision(15, QuranVerseRef(17, 1), QuranVerseRef(18, 74), 282, 301, false, false),
        QuranDivision(16, QuranVerseRef(18, 75), QuranVerseRef(20, 135), 302, 321, false, false),
        QuranDivision(17, QuranVerseRef(21, 1), QuranVerseRef(22, 78), 322, 341, false, false),
        QuranDivision(18, QuranVerseRef(23, 1), QuranVerseRef(25, 20), 342, 361, false, false),
        QuranDivision(19, QuranVerseRef(25, 21), QuranVerseRef(27, 55), 362, 381, false, false),
        QuranDivision(20, QuranVerseRef(27, 56), QuranVerseRef(29, 45), 382, 401, false, false),
        QuranDivision(21, QuranVerseRef(29, 46), QuranVerseRef(33, 30), 402, 421, false, false),
        QuranDivision(22, QuranVerseRef(33, 31), QuranVerseRef(36, 27), 422, 441, false, false),
        QuranDivision(23, QuranVerseRef(36, 28), QuranVerseRef(39, 31), 442, 461, false, false),
        QuranDivision(24, QuranVerseRef(39, 32), QuranVerseRef(41, 46), 462, 481, false, false),
        QuranDivision(25, QuranVerseRef(41, 47), QuranVerseRef(45, 37), 482, 502, false, true),
        QuranDivision(26, QuranVerseRef(46, 1), QuranVerseRef(51, 30), 502, 521, true, false),
        QuranDivision(27, QuranVerseRef(51, 31), QuranVerseRef(57, 29), 522, 541, false, false),
        QuranDivision(28, QuranVerseRef(58, 1), QuranVerseRef(66, 12), 542, 561, false, false),
        QuranDivision(29, QuranVerseRef(67, 1), QuranVerseRef(77, 50), 562, 581, false, false),
        QuranDivision(30, QuranVerseRef(78, 1), QuranVerseRef(114, 6), 582, 604, false, false)
    )

    private val hizbDivisions = listOf(
        QuranDivision(1, QuranVerseRef(1, 1), QuranVerseRef(2, 74), 1, 11, false, true),
        QuranDivision(2, QuranVerseRef(2, 75), QuranVerseRef(2, 141), 11, 21, true, false),
        QuranDivision(3, QuranVerseRef(2, 142), QuranVerseRef(2, 202), 22, 31, false, false),
        QuranDivision(4, QuranVerseRef(2, 203), QuranVerseRef(2, 252), 32, 41, false, false),
        QuranDivision(5, QuranVerseRef(2, 253), QuranVerseRef(3, 14), 42, 51, false, true),
        QuranDivision(6, QuranVerseRef(3, 15), QuranVerseRef(3, 92), 51, 62, true, true),
        QuranDivision(7, QuranVerseRef(3, 93), QuranVerseRef(3, 170), 62, 72, true, true),
        QuranDivision(8, QuranVerseRef(3, 171), QuranVerseRef(4, 23), 72, 81, true, false),
        QuranDivision(9, QuranVerseRef(4, 24), QuranVerseRef(4, 87), 82, 92, false, true),
        QuranDivision(10, QuranVerseRef(4, 88), QuranVerseRef(4, 147), 92, 101, true, false),
        QuranDivision(11, QuranVerseRef(4, 148), QuranVerseRef(5, 26), 102, 112, false, true),
        QuranDivision(12, QuranVerseRef(5, 27), QuranVerseRef(5, 81), 112, 121, true, true),
        QuranDivision(13, QuranVerseRef(5, 82), QuranVerseRef(6, 35), 121, 131, true, false),
        QuranDivision(14, QuranVerseRef(6, 36), QuranVerseRef(6, 110), 132, 141, false, false),
        QuranDivision(15, QuranVerseRef(6, 111), QuranVerseRef(6, 165), 142, 150, false, false),
        QuranDivision(16, QuranVerseRef(7, 1), QuranVerseRef(7, 87), 151, 161, false, false),
        QuranDivision(17, QuranVerseRef(7, 88), QuranVerseRef(7, 170), 162, 172, false, false),
        QuranDivision(18, QuranVerseRef(7, 171), QuranVerseRef(8, 40), 173, 181, false, false),
        QuranDivision(19, QuranVerseRef(8, 41), QuranVerseRef(9, 33), 182, 192, false, true),
        QuranDivision(20, QuranVerseRef(9, 34), QuranVerseRef(9, 92), 192, 201, true, true),
        QuranDivision(21, QuranVerseRef(9, 93), QuranVerseRef(10, 25), 201, 211, true, false),
        QuranDivision(22, QuranVerseRef(10, 26), QuranVerseRef(11, 5), 212, 221, false, false),
        QuranDivision(23, QuranVerseRef(11, 6), QuranVerseRef(11, 83), 222, 231, false, true),
        QuranDivision(24, QuranVerseRef(11, 84), QuranVerseRef(12, 52), 231, 241, true, false),
        QuranDivision(25, QuranVerseRef(12, 53), QuranVerseRef(13, 18), 242, 251, false, false),
        QuranDivision(26, QuranVerseRef(13, 19), QuranVerseRef(14, 52), 252, 261, false, false),
        QuranDivision(27, QuranVerseRef(15, 1), QuranVerseRef(16, 50), 262, 272, false, true),
        QuranDivision(28, QuranVerseRef(16, 51), QuranVerseRef(16, 128), 272, 281, true, false),
        QuranDivision(29, QuranVerseRef(17, 1), QuranVerseRef(17, 98), 282, 292, false, true),
        QuranDivision(30, QuranVerseRef(17, 99), QuranVerseRef(18, 74), 292, 301, true, false),
        QuranDivision(31, QuranVerseRef(18, 75), QuranVerseRef(19, 98), 302, 312, false, true),
        QuranDivision(32, QuranVerseRef(20, 1), QuranVerseRef(20, 135), 312, 321, true, false),
        QuranDivision(33, QuranVerseRef(21, 1), QuranVerseRef(21, 112), 322, 331, false, false),
        QuranDivision(34, QuranVerseRef(22, 1), QuranVerseRef(22, 78), 332, 341, false, false),
        QuranDivision(35, QuranVerseRef(23, 1), QuranVerseRef(24, 20), 342, 351, false, false),
        QuranDivision(36, QuranVerseRef(24, 21), QuranVerseRef(25, 20), 352, 361, false, false),
        QuranDivision(37, QuranVerseRef(25, 21), QuranVerseRef(26, 110), 362, 371, false, true),
        QuranDivision(38, QuranVerseRef(26, 111), QuranVerseRef(27, 55), 371, 381, true, false),
        QuranDivision(39, QuranVerseRef(27, 56), QuranVerseRef(28, 50), 382, 391, false, false),
        QuranDivision(40, QuranVerseRef(28, 51), QuranVerseRef(29, 45), 392, 401, false, false),
        QuranDivision(41, QuranVerseRef(29, 46), QuranVerseRef(31, 21), 402, 413, false, true),
        QuranDivision(42, QuranVerseRef(31, 22), QuranVerseRef(33, 30), 413, 421, true, false),
        QuranDivision(43, QuranVerseRef(33, 31), QuranVerseRef(34, 23), 422, 431, false, true),
        QuranDivision(44, QuranVerseRef(34, 24), QuranVerseRef(36, 27), 431, 441, true, false),
        QuranDivision(45, QuranVerseRef(36, 28), QuranVerseRef(37, 144), 442, 451, false, true),
        QuranDivision(46, QuranVerseRef(37, 145), QuranVerseRef(39, 31), 451, 461, true, false),
        QuranDivision(47, QuranVerseRef(39, 32), QuranVerseRef(40, 40), 462, 471, false, false),
        QuranDivision(48, QuranVerseRef(40, 41), QuranVerseRef(41, 46), 472, 481, false, false),
        QuranDivision(49, QuranVerseRef(41, 47), QuranVerseRef(43, 23), 482, 491, false, true),
        QuranDivision(50, QuranVerseRef(43, 24), QuranVerseRef(45, 37), 491, 502, true, true),
        QuranDivision(51, QuranVerseRef(46, 1), QuranVerseRef(48, 17), 502, 513, true, true),
        QuranDivision(52, QuranVerseRef(48, 18), QuranVerseRef(51, 30), 513, 521, true, false),
        QuranDivision(53, QuranVerseRef(51, 31), QuranVerseRef(54, 55), 522, 531, false, true),
        QuranDivision(54, QuranVerseRef(55, 1), QuranVerseRef(57, 29), 531, 541, true, false),
        QuranDivision(55, QuranVerseRef(58, 1), QuranVerseRef(61, 14), 542, 552, false, false),
        QuranDivision(56, QuranVerseRef(62, 1), QuranVerseRef(66, 12), 553, 561, false, false),
        QuranDivision(57, QuranVerseRef(67, 1), QuranVerseRef(71, 28), 562, 571, false, false),
        QuranDivision(58, QuranVerseRef(72, 1), QuranVerseRef(77, 50), 572, 581, false, false),
        QuranDivision(59, QuranVerseRef(78, 1), QuranVerseRef(86, 17), 582, 591, false, true),
        QuranDivision(60, QuranVerseRef(87, 1), QuranVerseRef(114, 6), 591, 604, true, false)
    )

    fun division(mode: QuranSelectionMode, number: Int): QuranDivision =
        when (mode) {
            QuranSelectionMode.JUZ -> juzDivisions
            QuranSelectionMode.HIZB -> hizbDivisions
        }.first { it.number == number }

    fun divisionsForPage(
        mode: QuranSelectionMode,
        page: Int
    ): List<QuranDivision> =
        when (mode) {
            QuranSelectionMode.JUZ -> juzDivisions
            QuranSelectionMode.HIZB -> hizbDivisions
        }.filter { page in it.pageRange }

    fun unitLabel(mode: QuranSelectionMode, number: Int): String =
        when (mode) {
            QuranSelectionMode.JUZ -> "Juz $number"
            QuranSelectionMode.HIZB -> "Hizb $number"
        }

    fun selectionSubtitle(mode: QuranSelectionMode, number: Int): String {
        val division = division(mode, number)
        return "${division.verseRangeLabel} • ${division.pageRangeLabel}"
    }

    fun boundaryNotice(
        mode: QuranSelectionMode,
        number: Int,
        page: Int
    ): String? {
        val division = division(mode, number)
        val label = unitLabel(mode, number)
        val notices = buildList {
            if (page == division.startPage && division.startsInsidePage) {
                add("$label commence à ${division.start.label} au milieu de cette page.")
            }
            if (page == division.endPage && division.endsInsidePage) {
                add("$label se termine à ${division.end.label} au milieu de cette page.")
            }
        }
        return notices.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}
