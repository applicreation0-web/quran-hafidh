package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class FixedActiveReviewPolicyTest {
    @Test public void bandsAreExactAndTheTenDayReferenceFitsAllCorpusSizes() {
        for (int total : new int[]{8820,9060}) {
            for (int count=0; count<=total; count++) {
                int active=ActiveReviewDurationPolicy.activeMinutes(count,total);
                assertEquals(count*30L<=total*15L?40:count*30L<=total*25L?60:70,active);
                assertEquals(active==40?5:active==60?10:15,ActiveReviewDurationPolicy.fragilityMinutes(active));
                assertTrue(count*8L<=18L*active*60L);
            }
        }
    }

    @Test public void historicalTargetsAndNewTargetsAreReadable() {
        for(int minutes:new int[]{30,40,45,60,70,75,90,105})
            assertTrue(ActiveReviewDurationPolicy.validPersistedMinutes(minutes));
        for(int minutes:new int[]{0,15,39,41,50,69,71})
            assertFalse(ActiveReviewDurationPolicy.validPersistedMinutes(minutes));
    }

    @Test public void queueDeduplicatesAndRotatesTheUnresolvedPage() {
        assertEquals(Arrays.asList(5,7,9),FragilityQueuePolicy.merge(
            Arrays.asList(5,7),Arrays.asList(7,9,9)));
        assertEquals(Arrays.asList(9,7),FragilityQueuePolicy.remaining(
            Arrays.asList(5,7),Arrays.asList(5,7,9),1,false));
        assertEquals(Collections.emptyList(),FragilityQueuePolicy.remaining(
            Arrays.asList(5,7),Arrays.asList(5,7,9),2,true));
        assertEquals(Arrays.asList(2,3,1),FragilityQueuePolicy.remaining(
            Arrays.asList(1,2,3),Arrays.asList(1,2,3),0,false));
    }
}
