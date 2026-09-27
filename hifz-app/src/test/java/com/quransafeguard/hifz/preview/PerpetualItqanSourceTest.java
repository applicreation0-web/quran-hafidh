package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class PerpetualItqanSourceTest {
    private static AnchoringQueue.Entry entry(String start, String end) {
        return new AnchoringQueue.Entry(start, end, AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0);
    }

    @Test public void picksTheEarliestTailEntryFirst() {
        PerpetualItqanSource.State state = PerpetualItqanSource.State.startOfTail();
        // Al-Hujurat (49) has only 18 ayahs — both entries must stay within that.
        List<AnchoringQueue.Entry> notDone = Arrays.asList(entry("49:14", "49:16"), entry("49:9", "49:12"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertEquals("49:9", selected.entry.start);
        assertEquals(PerpetualItqanSource.Leg.TAIL_HUJURAT_NAS, selected.state.leg);
        assertTrue(!selected.state.initialTailCompleted);
    }

    @Test public void ignoresFrontEntriesWhileTailStillHasSomethingPending() {
        PerpetualItqanSource.State state = PerpetualItqanSource.State.startOfTail();
        List<AnchoringQueue.Entry> notDone = Arrays.asList(entry("2:1", "2:5"), entry("49:9", "49:12"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertEquals("49:9", selected.entry.start);
    }

    @Test public void flipsToFrontOnceTailHasNothingPendingAndMarksInitialTailCompleted() {
        PerpetualItqanSource.State state = PerpetualItqanSource.State.startOfTail();
        List<AnchoringQueue.Entry> notDone = Collections.singletonList(entry("2:1", "2:5"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertEquals("2:1", selected.entry.start);
        assertEquals(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, selected.state.leg);
        assertTrue(selected.state.initialTailCompleted);
    }

    @Test public void frontNeverOffersMaterialBeyondTheSabqiFrontier() {
        PerpetualItqanSource.State state = new PerpetualItqanSource.State(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, true);
        // 3:10 is beyond the Sabqi frontier of 2:77 — must never be selected, even though it's the
        // only entry in the queue: FRONT is "devancé par le Sabqi", so nothing there means null.
        List<AnchoringQueue.Entry> notDone = Collections.singletonList(entry("3:10", "3:12"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertNull(selected);
    }

    @Test public void frontOffersMaterialUpToAndIncludingTheSabqiFrontier() {
        PerpetualItqanSource.State state = new PerpetualItqanSource.State(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, true);
        List<AnchoringQueue.Entry> notDone = Collections.singletonList(entry("2:70", "2:77"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertEquals("2:70", selected.entry.start);
    }

    @Test public void staysNullWhenNeitherLegHasAnythingPending() {
        // TAIL is permanently dry (initialTailCompleted already true) and FRONT momentarily has
        // nothing pending either (everything caught up) — must return null, not crash or bounce
        // back into a leg with nothing real to offer.
        PerpetualItqanSource.State state = new PerpetualItqanSource.State(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, true);
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(
            state, Collections.emptyList(), new VerseRef(2, 77));
        assertNull(selected);
    }

    @Test public void flipsBackToTailOnceFrontIsMomentarilyCaughtUp() {
        PerpetualItqanSource.State state = new PerpetualItqanSource.State(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, true);
        // Al-Hujurat (49) has only 18 ayahs.
        List<AnchoringQueue.Entry> notDone = Collections.singletonList(entry("49:16", "49:18"));
        PerpetualItqanSource.Selection selected = PerpetualItqanSource.selectNext(state, notDone, new VerseRef(2, 77));
        assertEquals("49:16", selected.entry.start);
        assertEquals(PerpetualItqanSource.Leg.TAIL_HUJURAT_NAS, selected.state.leg);
        assertTrue("flipping back to TAIL must not un-stick initialTailCompleted",
            selected.state.initialTailCompleted);
    }

    @Test public void projectedOrderPutsTheStartingLegFirstThenTheOtherLegByOrdinal() {
        PerpetualItqanSource.State state = PerpetualItqanSource.State.startOfTail();
        // Al-Hujurat (49) has only 18 ayahs.
        List<AnchoringQueue.Entry> candidates = Arrays.asList(
            entry("2:10", "2:12"), entry("49:14", "49:16"), entry("49:9", "49:11"));
        List<AnchoringQueue.Entry> ordered = PerpetualItqanSource.projectedOrder(state, candidates, new VerseRef(2, 77));
        assertEquals("49:9", ordered.get(0).start);
        assertEquals("49:14", ordered.get(1).start);
        assertEquals("2:10", ordered.get(2).start);
    }

    @Test public void projectedOrderStartsFromFrontWhenThatIsTheCurrentLeg() {
        PerpetualItqanSource.State state = new PerpetualItqanSource.State(PerpetualItqanSource.Leg.FRONT_BAQARA_HUJURAT, true);
        List<AnchoringQueue.Entry> candidates = Arrays.asList(entry("49:9", "49:11"), entry("2:10", "2:12"));
        List<AnchoringQueue.Entry> ordered = PerpetualItqanSource.projectedOrder(state, candidates, new VerseRef(2, 77));
        assertEquals("2:10", ordered.get(0).start);
        assertEquals("49:9", ordered.get(1).start);
    }
}
