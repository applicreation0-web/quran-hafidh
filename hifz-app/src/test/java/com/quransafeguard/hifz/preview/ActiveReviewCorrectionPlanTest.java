package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import static org.junit.Assert.*;

/** Page reveal, not weak-verse flag, is the only selection criterion. */
public final class ActiveReviewCorrectionPlanTest {
    @Test public void everyRevealedPageReplaysInFirstRevealOrder() {
        LinkedHashSet<Integer> revealed=new LinkedHashSet<>(Arrays.asList(408,407,408,410));
        List<Integer> pages=ActiveReviewCorrectionPlan.select(revealed);
        assertEquals(Arrays.asList(408,407,410),pages);
        try{pages.add(411);fail("Immutable correction plan expected");}
        catch(UnsupportedOperationException expected){}
    }
    @Test public void noRevealRequiresNoCorrection() {
        assertTrue(ActiveReviewCorrectionPlan.select(Collections.emptySet()).isEmpty());
        assertTrue(ActiveReviewCorrectionPlan.select(null).isEmpty());
    }
    @Test public void invalidPageNeverEarnsCorrectionOrCredit() {
        for(int bad:new int[]{0,-1,605}) {
            try{
                ActiveReviewCorrectionPlan.select(new LinkedHashSet<>(Arrays.asList(30,bad)));
                fail("Unexpected accepted page "+bad);
            }catch(IllegalArgumentException expected){}
        }
    }
}
