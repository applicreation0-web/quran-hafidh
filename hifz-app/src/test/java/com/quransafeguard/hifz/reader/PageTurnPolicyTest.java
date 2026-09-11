package com.quransafeguard.hifz.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PageTurnPolicyTest {
    @Test public void rightSwipeAdvancesArabicMushaf() {
        assertEquals(1, PageTurnPolicy.deltaForHorizontalSwipe(120f));
    }

    @Test public void leftSwipeReturnsToPreviousPage() {
        assertEquals(-1, PageTurnPolicy.deltaForHorizontalSwipe(-120f));
    }

    @Test public void zeroMovementDoesNotTurnPage() {
        assertEquals(0, PageTurnPolicy.deltaForHorizontalSwipe(0f));
    }
}
