package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranStructureMetadataTest {
    private val juzStarts = listOf(
        QuranVerseRef(1, 1), QuranVerseRef(2, 142), QuranVerseRef(2, 253),
        QuranVerseRef(3, 93), QuranVerseRef(4, 24), QuranVerseRef(4, 148),
        QuranVerseRef(5, 82), QuranVerseRef(6, 111), QuranVerseRef(7, 88),
        QuranVerseRef(8, 41), QuranVerseRef(9, 93), QuranVerseRef(11, 6),
        QuranVerseRef(12, 53), QuranVerseRef(15, 1), QuranVerseRef(17, 1),
        QuranVerseRef(18, 75), QuranVerseRef(21, 1), QuranVerseRef(23, 1),
        QuranVerseRef(25, 21), QuranVerseRef(27, 56), QuranVerseRef(29, 46),
        QuranVerseRef(33, 31), QuranVerseRef(36, 28), QuranVerseRef(39, 32),
        QuranVerseRef(41, 47), QuranVerseRef(46, 1), QuranVerseRef(51, 31),
        QuranVerseRef(58, 1), QuranVerseRef(67, 1), QuranVerseRef(78, 1)
    )

    private val juzStartPages = listOf(
        1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
        201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582
    )

    private val hizbStarts = listOf(
        QuranVerseRef(1, 1), QuranVerseRef(2, 75), QuranVerseRef(2, 142),
        QuranVerseRef(2, 203), QuranVerseRef(2, 253), QuranVerseRef(3, 15),
        QuranVerseRef(3, 93), QuranVerseRef(3, 171), QuranVerseRef(4, 24),
        QuranVerseRef(4, 88), QuranVerseRef(4, 148), QuranVerseRef(5, 27),
        QuranVerseRef(5, 82), QuranVerseRef(6, 36), QuranVerseRef(6, 111),
        QuranVerseRef(7, 1), QuranVerseRef(7, 88), QuranVerseRef(7, 171),
        QuranVerseRef(8, 41), QuranVerseRef(9, 34), QuranVerseRef(9, 93),
        QuranVerseRef(10, 26), QuranVerseRef(11, 6), QuranVerseRef(11, 84),
        QuranVerseRef(12, 53), QuranVerseRef(13, 19), QuranVerseRef(15, 1),
        QuranVerseRef(16, 51), QuranVerseRef(17, 1), QuranVerseRef(17, 99),
        QuranVerseRef(18, 75), QuranVerseRef(20, 1), QuranVerseRef(21, 1),
        QuranVerseRef(22, 1), QuranVerseRef(23, 1), QuranVerseRef(24, 21),
        QuranVerseRef(25, 21), QuranVerseRef(26, 111), QuranVerseRef(27, 56),
        QuranVerseRef(28, 51), QuranVerseRef(29, 46), QuranVerseRef(31, 22),
        QuranVerseRef(33, 31), QuranVerseRef(34, 24), QuranVerseRef(36, 28),
        QuranVerseRef(37, 145), QuranVerseRef(39, 32), QuranVerseRef(40, 41),
        QuranVerseRef(41, 47), QuranVerseRef(43, 24), QuranVerseRef(46, 1),
        QuranVerseRef(48, 18), QuranVerseRef(51, 31), QuranVerseRef(55, 1),
        QuranVerseRef(58, 1), QuranVerseRef(62, 1), QuranVerseRef(67, 1),
        QuranVerseRef(72, 1), QuranVerseRef(78, 1), QuranVerseRef(87, 1)
    )

    private val hizbStartPages = listOf(
        1, 11, 22, 32, 42, 51, 62, 72, 82, 92,
        102, 112, 121, 132, 142, 151, 162, 173, 182, 192,
        201, 212, 222, 231, 242, 252, 262, 272, 282, 292,
        302, 312, 322, 332, 342, 352, 362, 371, 382, 392,
        402, 413, 422, 431, 442, 451, 462, 472, 482, 491,
        502, 513, 522, 531, 542, 553, 562, 572, 582, 591
    )

    private val surahAyahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    @Test
    fun allThirtyJuzStartsAndPagesMatchCanonicalMetadata() {
        assertEquals(30, juzStarts.size)
        assertEquals(30, juzStartPages.size)
        juzStarts.indices.forEach { index ->
            val division = QuranStructureMetadata.division(
                QuranSelectionMode.JUZ,
                index + 1
            )
            assertEquals("Juz ${index + 1} start", juzStarts[index], division.start)
            assertEquals("Juz ${index + 1} page", juzStartPages[index], division.startPage)
        }
    }

    @Test
    fun allSixtyHizbStartsAndPagesMatchCanonicalMetadata() {
        assertEquals(60, hizbStarts.size)
        assertEquals(60, hizbStartPages.size)
        hizbStarts.indices.forEach { index ->
            val division = QuranStructureMetadata.division(
                QuranSelectionMode.HIZB,
                index + 1
            )
            assertEquals("Hizb ${index + 1} start", hizbStarts[index], division.start)
            assertEquals("Hizb ${index + 1} page", hizbStartPages[index], division.startPage)
        }
    }

    @Test
    fun everyCanonicalDivisionIsGaplessAndNonOverlappingByVerse() {
        listOf(
            QuranSelectionMode.JUZ to 30,
            QuranSelectionMode.HIZB to 60
        ).forEach { (mode, count) ->
            val divisions = (1..count).map { QuranStructureMetadata.division(mode, it) }
            assertEquals(1, ordinal(divisions.first().start))
            assertEquals(6236, ordinal(divisions.last().end))
            divisions.zipWithNext().forEach { (current, next) ->
                assertEquals(
                    "${mode.name} boundary ${current.number}/${next.number}",
                    ordinal(current.end) + 1,
                    ordinal(next.start)
                )
            }
        }
    }

    @Test
    fun sharedBoundaryPagesRemainVisibleToBothCanonicalSections() {
        val pageEleven = QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.HIZB,
            11
        ).map(QuranDivision::number)
        assertEquals(listOf(1, 2), pageEleven)

        val pageSixtyTwoJuz = QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.JUZ,
            62
        ).map(QuranDivision::number)
        assertEquals(listOf(3, 4), pageSixtyTwoJuz)

        val pageFiveHundredTwo = QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.JUZ,
            502
        ).map(QuranDivision::number)
        assertEquals(listOf(25, 26), pageFiveHundredTwo)
    }

    @Test
    fun canonicalSelectionIncludesSharedBoundaryPageButNeverOutsideRange() {
        (1..60).forEach { hizb ->
            assertEquals(
                QuranStructureMetadata.division(
                    QuranSelectionMode.HIZB,
                    hizb
                ).pageRange.toList(),
                QuranPageSelector.canonicalPages(QuranSelectionMode.HIZB, hizb)
            )
        }
        (1..30).forEach { juz ->
            assertEquals(
                QuranStructureMetadata.division(
                    QuranSelectionMode.JUZ,
                    juz
                ).pageRange.toList(),
                QuranPageSelector.canonicalPages(QuranSelectionMode.JUZ, juz)
            )
        }
    }

    @Test
    fun shortHizbQuotaNeverBorrowsFromNextHizb() {
        val hizb15 = QuranStructureMetadata.division(QuranSelectionMode.HIZB, 15)
        val quota = QuranPageSelector.tenPageQuotaFromHizb(15)

        assertEquals(142, hizb15.startPage)
        assertEquals(150, hizb15.endPage)
        assertEquals((142..150).toList(), quota)
        assertEquals(9, quota.size)
        assertTrue(quota.all { it in hizb15.pageRange })
        assertFalse(151 in quota)
    }

    @Test
    fun longHizbQuotaMayBeTenPagesButNeverCrossesCanonicalBoundary() {
        val hizb1 = QuranStructureMetadata.division(QuranSelectionMode.HIZB, 1)
        val quota = QuranPageSelector.tenPageQuotaFromHizb(1)

        assertEquals(UsageCyclePolicy.HIZB_PAGE_COUNT, quota.size)
        assertEquals((1..10).toList(), quota)
        assertTrue(quota.all { it in hizb1.pageRange })
    }

    @Test
    fun fixedQuotaHonoursJuzSelectionAsCanonicalPool() {
        val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.JUZ,
            selectedUnits = setOf(4),
            cursor = 0,
            pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT
        )
        val canonical = QuranPageSelector.canonicalPages(QuranSelectionMode.JUZ, 4).toSet()

        assertEquals(10, plan.pages.size)
        assertTrue(plan.pages.all { it in canonical })
        assertFalse(plan.pages.any { it < 62 || it > 81 })
    }

    @Test
    fun quotaNeverRepeatsPagesWhenSelectedPoolIsSmallerThanRequest() {
        val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(15),
            cursor = 0,
            pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
        )

        assertEquals((142..150).toList(), plan.pages)
        assertEquals(plan.pages.size, plan.pages.distinct().size)
        assertEquals(9, plan.pages.size)
    }

    private fun ordinal(ref: QuranVerseRef): Int {
        require(ref.surah in 1..surahAyahCounts.size)
        require(ref.ayah in 1..surahAyahCounts[ref.surah - 1])
        return surahAyahCounts.take(ref.surah - 1).sum() + ref.ayah
    }
}
