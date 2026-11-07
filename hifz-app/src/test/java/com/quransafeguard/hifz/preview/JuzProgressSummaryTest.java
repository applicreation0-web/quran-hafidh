package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** A Juz gauge must never display a false 100% before all verses are acquired. */
public final class JuzProgressSummaryTest {
    @Test public void percentUsesOnlyRealFullyAcquiredCount() {
        assertEquals(0,JuzProgressSummary.percent(0,20));
        assertEquals(5,JuzProgressSummary.percent(1,20));
        assertEquals(99,JuzProgressSummary.percent(199,200));
        assertEquals(100,JuzProgressSummary.percent(200,200));
    }
    @Test public void impossibleReceiptCountsAreRejected() {
        for(int[] invalid:new int[][]{{0,0},{2,1},{-1,20},{21,20}}){
            try {
                JuzProgressSummary.percent(invalid[0],invalid[1]);
                fail("Impossible Juz acquisition must fail closed");
            } catch(IllegalArgumentException expected) { }
        }
    }
}
