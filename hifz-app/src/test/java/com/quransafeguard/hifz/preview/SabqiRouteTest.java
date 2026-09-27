package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SabqiRouteTest {
    @Test public void notCompleteWhileStillWithinAlBaqara() {
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(2, 77)));
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(2, 286)));
    }

    @Test public void notCompleteInAnySurahBetweenBaqaraAndTheRouteEnd() {
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(3, 1)));
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(18, 10)));
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(48, 29)));
    }

    @Test public void completeOnceThePastAlFathBoundaryIsCrossed() {
        assertTrue("49:1 (Al-Hujurat) belongs to the Itqan TAIL leg, never Sabqi",
            SabqiRoute.allNewSabqiComplete(new VerseRef(49, 1)));
        assertTrue(SabqiRoute.allNewSabqiComplete(new VerseRef(114, 6)));
    }

    @Test public void clampKeepsAnEndAlreadyInsideTheRouteUnchanged() {
        VerseRef end = new VerseRef(2, 286);
        assertEquals(end, SabqiRoute.clampToRouteEnd(end));
    }

    @Test public void clampCapsAnEndThatWouldOverrunTheRoute() {
        assertEquals(SabqiRoute.ROUTE_END, SabqiRoute.clampToRouteEnd(new VerseRef(49, 1)));
        assertEquals(SabqiRoute.ROUTE_END, SabqiRoute.clampToRouteEnd(new VerseRef(114, 6)));
    }

    @Test public void baqaraNotCompleteWhileStillWithinIt() {
        assertFalse(SabqiRoute.baqaraComplete(new VerseRef(2, 77)));
        assertFalse(SabqiRoute.baqaraComplete(new VerseRef(2, 286)));
    }

    @Test public void baqaraCompleteOnceAlImranIsReached() {
        assertTrue(SabqiRoute.baqaraComplete(new VerseRef(3, 1)));
        assertTrue(SabqiRoute.baqaraComplete(new VerseRef(48, 29)));
    }
}
