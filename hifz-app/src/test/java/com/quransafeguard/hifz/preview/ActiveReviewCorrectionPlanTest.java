package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

public final class ActiveReviewCorrectionPlanTest {
    private static VerseRef v(int surah, int ayah) { return new VerseRef(surah, ayah); }
    private static int page(VerseRef verse) {
        if (verse.getSurah() == 2) return verse.getAyah() < 3 ? 3 : 4;
        return 604;
    }

    @Test public void revealedAndMarkedIntersectionOnly() {
        List<ActiveReviewCorrectionPlan.Page> result = ActiveReviewCorrectionPlan.select(
            Arrays.asList(v(2,1),v(2,2),v(2,3),v(2,4),v(114,1)),
            new HashSet<>(Arrays.asList(3,4)),
            Arrays.asList(v(2,1),v(2,4),v(114,1)),
            ActiveReviewCorrectionPlanTest::page);
        assertEquals(2,result.size());
        assertEquals(3,result.get(0).page);
        assertEquals(Collections.singletonList(v(2,1)),result.get(0).flagged);
        assertEquals(4,result.get(1).page);
        assertEquals(Collections.singletonList(v(2,4)),result.get(1).flagged);
    }

    @Test public void samePageWithMultipleFlagsAppearsOnceInTraversalOrder() {
        List<ActiveReviewCorrectionPlan.Page> result = ActiveReviewCorrectionPlan.select(
            Arrays.asList(v(114,1),v(2,3),v(2,1),v(2,2),v(2,1)),
            new HashSet<>(Arrays.asList(3,604)),
            Arrays.asList(v(2,1),v(2,2),v(114,1)),
            ActiveReviewCorrectionPlanTest::page);
        assertEquals(2,result.size());
        assertEquals(604,result.get(0).page);
        assertEquals(3,result.get(1).page);
        assertEquals(Arrays.asList(v(2,1),v(2,2)),result.get(1).flagged);
    }

    @Test public void noRevealNoFlagOrOutsideCoverageNeverQueuesAnything() {
        List<VerseRef> traversed = Arrays.asList(v(2,1),v(2,2));
        assertTrue(ActiveReviewCorrectionPlan.select(traversed,Collections.emptySet(),
            Arrays.asList(v(2,1)),ActiveReviewCorrectionPlanTest::page).isEmpty());
        assertTrue(ActiveReviewCorrectionPlan.select(traversed,Collections.singleton(3),
            Arrays.asList(v(114,1)),ActiveReviewCorrectionPlanTest::page).isEmpty());
        assertTrue(ActiveReviewCorrectionPlan.select(traversed,Collections.singleton(4),
            Arrays.asList(v(2,1)),ActiveReviewCorrectionPlanTest::page).isEmpty());
    }
}
