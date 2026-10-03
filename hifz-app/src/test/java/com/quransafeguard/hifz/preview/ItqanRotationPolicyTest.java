package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class ItqanRotationPolicyTest {
    @Test public void startsOnTailWithInitialTailNotYetCompleted() {
        ItqanRotationPolicy.State start = ItqanRotationPolicy.State.startOfTail();
        assertEquals(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, start.leg);
        assertEquals(ItqanRotationPolicy.TAIL_START, start.cursor);
        assertFalse(start.initialTailCompleted);
    }

    @Test public void findsTheFirstAcquiredVerseAtOrAfterTheCursorWithinTheLeg() {
        ItqanRotationPolicy.State state = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(49, 5), false);
        List<VerseRef> acquired = Arrays.asList(new VerseRef(49, 1), new VerseRef(49, 8), new VerseRef(50, 1));
        assertEquals(new VerseRef(49, 8), ItqanRotationPolicy.nextInLeg(state, acquired));
    }

    @Test public void ignoresAcquiredMaterialOutsideTheCurrentLegsBounds() {
        ItqanRotationPolicy.State state = ItqanRotationPolicy.State.startOfTail();
        List<VerseRef> acquired = Arrays.asList(new VerseRef(2, 1), new VerseRef(2, 76));
        assertNull("2:1-2:76 is FRONT material, never eligible while on TAIL",
            ItqanRotationPolicy.nextInLeg(state, acquired));
    }

    @Test public void returnsNullWhenNothingRemainsInThisLegForThisPass() {
        ItqanRotationPolicy.State state = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(114, 1), false);
        List<VerseRef> acquired = Arrays.asList(new VerseRef(49, 1));
        assertNull(ItqanRotationPolicy.nextInLeg(state, acquired));
    }

    @Test public void neverJumpsOrRewindsForANewlyAcquiredVerseBehindTheCursor() {
        ItqanRotationPolicy.State state = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(60, 1), false);
        List<VerseRef> acquired = Arrays.asList(new VerseRef(49, 1), new VerseRef(70, 1));
        assertEquals("a verse behind the cursor waits for the leg's next pass, never jumps back",
            new VerseRef(70, 1), ItqanRotationPolicy.nextInLeg(state, acquired));
    }

    @Test public void firstTailToFrontTransitionSetsInitialTailCompletedPermanently() {
        ItqanRotationPolicy.State tail = ItqanRotationPolicy.State.startOfTail();
        ItqanRotationPolicy.State front = ItqanRotationPolicy.onLegExhausted(tail);
        assertEquals(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, front.leg);
        assertEquals(ItqanRotationPolicy.FRONT_START, front.cursor);
        assertTrue(front.initialTailCompleted);

        ItqanRotationPolicy.State tailAgain = ItqanRotationPolicy.onLegExhausted(front);
        assertEquals(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, tailAgain.leg);
        assertEquals(ItqanRotationPolicy.TAIL_START, tailAgain.cursor);
        assertTrue("initialTailCompleted must never reset back to false", tailAgain.initialTailCompleted);
    }

    @Test public void frontLegStopsExactlyAtFortyEightTwentyNineNeverCrossingIntoHujurat() {
        assertEquals(new VerseRef(48, 29), ItqanRotationPolicy.FRONT_END);
    }

    @Test public void advancedPastMovesTheCursorToTheCanonicalNextVerse() {
        ItqanRotationPolicy.State state = ItqanRotationPolicy.State.startOfTail();
        ItqanRotationPolicy.State after = ItqanRotationPolicy.advancedPast(state, new VerseRef(49, 1));
        assertEquals(new VerseRef(49, 2), after.cursor);
        assertEquals(state.leg, after.leg);
    }
}
