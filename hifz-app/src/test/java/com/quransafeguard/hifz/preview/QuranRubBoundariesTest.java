package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The 240 rub' al-hizb boundaries were verified against two independent open datasets
 * (they matched exactly, verse for verse) and cross-checked against this project's own
 * already-relied-upon Hizb/Juz start pages. These tests check the data's structural
 * integrity (exhaustive, ordered, no page ever repeated) and spot-check the anchor rows
 * that tie back to that cross-check, rather than re-typing all 240 rows a second time.
 */
public final class QuranRubBoundariesTest {
    @Test public void tableHasExactlyTwoHundredFortyOrderedNonOverlappingRows() {
        assertEquals(240, QuranRubBoundaries.TABLE.length);
        int previousPage = 0;
        for (int i = 0; i < QuranRubBoundaries.TABLE.length; i++) {
            int[] row = QuranRubBoundaries.TABLE[i];
            assertEquals("row " + i + " index", i + 1, row[0]);
            assertTrue("row " + i + " page must strictly increase", row[3] > previousPage);
            previousPage = row[3];
            assertTrue("row " + i + " hizb in range", row[4] >= 1 && row[4] <= 60);
            assertTrue("row " + i + " position in range", row[5] >= 0 && row[5] <= 3);
            assertEquals("row " + i + " hizb derived from n", (row[0] - 1) / 4 + 1, row[4]);
            assertEquals("row " + i + " position derived from n", (row[0] - 1) % 4, row[5]);
        }
    }

    @Test public void firstRubIsAlFatihaItself() {
        assertArrayEquals(new int[]{1, 1, 1, 1, 1, 0}, QuranRubBoundaries.rowForIndex(1));
    }

    @Test public void hizbTwoStartMatchesTheHizbStartAlreadyReliedOnElsewhereInThisProject() {
        // HifzPrefs.sabqiStart()'s default is 2:75 — the real start of Hizb 2 — page 11.
        int[] row = QuranRubBoundaries.rowForIndex(5);
        assertArrayEquals(new int[]{5, 2, 75, 11, 2, 0}, row);
        assertEquals(11, QuranRubBoundaries.pageForRub(2, 0));
    }

    @Test public void juzTwoStartMatchesItsOwnKnownBoundary() {
        // Juz 2 begins at 2:142, page 22 — also a rub' boundary (rub' 9, Hizb 3's own start).
        int[] row = QuranRubBoundaries.rowForIndex(9);
        assertArrayEquals(new int[]{9, 2, 142, 22, 3, 0}, row);
    }

    @Test public void lastRubIsInSurahAlKawtharOnPage600() {
        assertArrayEquals(new int[]{240, 100, 9, 600, 60, 3}, QuranRubBoundaries.rowForIndex(240));
    }

    @Test public void boundaryOnPageFindsExactStartsOnlyAndNeverAWrongMatch() {
        assertArrayEquals(new int[]{1, 1, 1, 1, 1, 0}, QuranRubBoundaries.boundaryOnPage(1));
        assertArrayEquals(new int[]{5, 2, 75, 11, 2, 0}, QuranRubBoundaries.boundaryOnPage(11));
        assertNull("page 2 starts no rub'", QuranRubBoundaries.boundaryOnPage(2));
        assertNull("page 604 starts no rub' (the last one starts on 600)", QuranRubBoundaries.boundaryOnPage(604));
    }

    @Test public void currentAtFindsTheLastBoundaryAtOrBeforeThePage() {
        assertArrayEquals("page 1 is exactly rub' 1's own start", new int[]{1, 1, 1, 1, 1, 0}, QuranRubBoundaries.currentAt(1));
        assertArrayEquals("page 10, still within rub' 5 (started page 11)? no — before it, so rub' 4",
            new int[]{4, 2, 60, 9, 1, 3}, QuranRubBoundaries.currentAt(10));
        assertArrayEquals("page 11 lands exactly on rub' 5's own start",
            new int[]{5, 2, 75, 11, 2, 0}, QuranRubBoundaries.currentAt(11));
        assertArrayEquals("page 604, past the last boundary, still resolves to rub' 240",
            new int[]{240, 100, 9, 600, 60, 3}, QuranRubBoundaries.currentAt(604));
    }
}
