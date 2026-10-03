package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * HizbBoundaries is a thin view over QuranRubBoundaries' already-verified table (every
 * Hizb's own start is that table's position-0 row), so these tests mostly check the view
 * itself lines up with rows QuranRubBoundariesTest already anchors independently.
 */
public final class HizbBoundariesTest {
    @Test public void hizbOneIsAlFatihaItself() {
        assertArrayEquals(new int[]{1, 1, 1, 1}, HizbBoundaries.rowForHizb(1));
    }

    @Test public void hizbTwoMatchesTheStartAlreadyReliedOnElsewhereInThisProject() {
        // HifzPrefs.sabqiStart()'s default is 2:75 — the real start of Hizb 2 — page 11.
        assertArrayEquals(new int[]{2, 2, 75, 11}, HizbBoundaries.rowForHizb(2));
    }

    @Test public void hizbSixtyIsTheLastOneOnPage591() {
        assertArrayEquals(new int[]{60, 87, 1, 591}, HizbBoundaries.rowForHizb(60));
    }

    @Test public void rejectsOutOfRangeHizb() {
        try {
            HizbBoundaries.rowForHizb(0);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            HizbBoundaries.rowForHizb(61);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test public void boundaryOnPageFindsExactStartsOnlyAndNeverAWrongMatch() {
        assertEquals(1, HizbBoundaries.boundaryOnPage(1));
        assertEquals(2, HizbBoundaries.boundaryOnPage(11));
        assertEquals(-1, HizbBoundaries.boundaryOnPage(2));
        assertEquals(-1, HizbBoundaries.boundaryOnPage(604));
    }

    @Test public void currentAtFindsTheLastBoundaryAtOrBeforeThePage() {
        assertEquals(1, HizbBoundaries.currentAt(1));
        assertEquals(1, HizbBoundaries.currentAt(10));
        assertEquals(2, HizbBoundaries.currentAt(11));
        assertEquals(60, HizbBoundaries.currentAt(604));
    }
}
