package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ItqanPlanSnapshotTest {
    private final VerseRef start = new VerseRef(2, 70);
    private final VerseRef end = new VerseRef(2, 90);

    @Test public void undecidedMatchesOnlyItsOwnBlock() {
        ItqanPlanSnapshot snapshot = ItqanPlanSnapshot.undecided(start, end, 1);
        assertTrue(snapshot.matches(start, end, 1));
        assertFalse("a different block index within the same parent is not a match",
            snapshot.matches(start, end, 2));
        assertFalse("a different parent unit is not a match",
            snapshot.matches(new VerseRef(2, 71), end, 1));
        assertEquals(ItqanPlanSnapshot.Decision.UNDECIDED, snapshot.decision);
        assertTrue(snapshot.bonusLineIds.isEmpty());
    }

    @Test public void keepCarriesNoBonusLines() {
        ItqanPlanSnapshot snapshot = ItqanPlanSnapshot.undecided(start, end, 0).keep();
        assertEquals(ItqanPlanSnapshot.Decision.KEEP, snapshot.decision);
        assertTrue(snapshot.bonusLineIds.isEmpty());
    }

    @Test public void extendCarriesExactlyItsBonusLines() {
        ItqanPlanSnapshot snapshot = ItqanPlanSnapshot.undecided(start, end, 0)
            .extend(Arrays.asList("l-9", "l-10"));
        assertEquals(ItqanPlanSnapshot.Decision.EXTEND, snapshot.decision);
        assertEquals(Arrays.asList("l-9", "l-10"), snapshot.bonusLineIds);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extendRejectsMoreThanTwoBonusLines() {
        ItqanPlanSnapshot.undecided(start, end, 0).extend(Arrays.asList("a", "b", "c"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extendRejectsZeroBonusLines() {
        ItqanPlanSnapshot.undecided(start, end, 0).extend(Collections.emptyList());
    }

    @Test public void roundTripsThroughJsonForEveryDecision() throws JSONException {
        ItqanPlanSnapshot undecided = ItqanPlanSnapshot.undecided(start, end, 2);
        ItqanPlanSnapshot keep = ItqanPlanSnapshot.undecided(start, end, 2).keep();
        ItqanPlanSnapshot extend = ItqanPlanSnapshot.undecided(start, end, 2).extend(Arrays.asList("l-a", "l-b"));

        for (ItqanPlanSnapshot original : new ItqanPlanSnapshot[]{undecided, keep, extend}) {
            JSONObject json = original.toJson();
            ItqanPlanSnapshot restored = ItqanPlanSnapshot.fromJson(new JSONObject(json.toString()));
            assertEquals(original.decision, restored.decision);
            assertEquals(original.unitStart, restored.unitStart);
            assertEquals(original.unitEnd, restored.unitEnd);
            assertEquals(original.blockIndex, restored.blockIndex);
            assertEquals(original.bonusLineIds, restored.bonusLineIds);
            assertTrue(restored.matches(start, end, 2));
        }
    }
}
