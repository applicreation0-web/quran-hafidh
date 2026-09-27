package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.List;

/**
 * P4's perpetual Itqān macro-cycle: TAIL (Al-Hujurāt → An-Nās, 49:1..114:6) then FRONT (Al-Baqara
 * → Al-Fatḥ, 2:1..48:29) then TAIL again, forever. Pure and Android-free — it only ever advances
 * the cursor within whichever leg is current, restricted to material the caller already knows is
 * ACQUIRED; it never decides ACQUIRED-ness itself and never reaches into HifzPrefs/AnchoringQueue.
 *
 * A new ACQUIRED verse never causes a jump or rewind: if it falls ahead of the cursor in the
 * current leg it is met naturally when the cursor reaches it; if it falls behind, it waits for the
 * leg's next pass. The only state carried between calls is the leg and the cursor itself.
 */
final class ItqanRotationPolicy {
    enum Leg { TAIL_HUJURAT_NAS, FRONT_BAQARA_HUJURAT }

    static final VerseRef TAIL_START = new VerseRef(49, 1);
    static final VerseRef TAIL_END = new VerseRef(114, 6);

    static final VerseRef FRONT_START = new VerseRef(2, 1);
    /** 48:29, immediately before Al-Hujurāt (49:1) — see SabqiRoute.ROUTE_END, the same boundary
     *  Sabqi's own route is capped at, so FRONT never re-treats Al-Hujurāt as its own material. */
    static final VerseRef FRONT_END = SabqiRoute.ROUTE_END;

    static final class State {
        final Leg leg;
        final VerseRef cursor;
        final boolean initialTailCompleted;

        State(Leg leg, VerseRef cursor, boolean initialTailCompleted) {
            this.leg = leg;
            this.cursor = cursor;
            this.initialTailCompleted = initialTailCompleted;
        }

        static State startOfTail() {
            return new State(Leg.TAIL_HUJURAT_NAS, TAIL_START, false);
        }
    }

    private ItqanRotationPolicy() {}

    /** The first eligible (ACQUIRED) verse at or after state.cursor within state.leg's own bounds,
     *  or null if none remains in this leg for this pass — the caller should then call
     *  onLegExhausted to flip to the other leg and try again from its start. */
    static VerseRef nextInLeg(State state, List<VerseRef> acquiredSorted) {
        VerseRef legStart = legStart(state.leg);
        VerseRef legEnd = legEnd(state.leg);
        int cursorOrdinal = GeometryRepository.ordinal(state.cursor);
        int startOrdinal = GeometryRepository.ordinal(legStart);
        int endOrdinal = GeometryRepository.ordinal(legEnd);
        int from = Math.max(cursorOrdinal, startOrdinal);
        for (VerseRef ref : acquiredSorted) {
            int o = GeometryRepository.ordinal(ref);
            if (o < from) continue;
            if (o > endOrdinal) break;
            return ref;
        }
        return null;
    }

    /** Flips to the other leg, restarting its cursor at that leg's own start. The first ever
     *  TAIL -> FRONT transition is the one moment initialTailCompleted becomes true; it is never
     *  reset back to false afterwards. */
    static State onLegExhausted(State state) {
        Leg nextLeg = state.leg == Leg.TAIL_HUJURAT_NAS ? Leg.FRONT_BAQARA_HUJURAT : Leg.TAIL_HUJURAT_NAS;
        boolean initialTailCompleted = state.initialTailCompleted
            || state.leg == Leg.TAIL_HUJURAT_NAS;
        return new State(nextLeg, legStart(nextLeg), initialTailCompleted);
    }

    /** Advances the cursor to just past the verse that was just presented to the Itqān engine —
     *  the leg never changes here, only nextInLeg's own scan start moves forward. */
    static State advancedPast(State state, VerseRef presented) {
        VerseRef next = com.quransafeguard.hifz.core.QuranCanon.INSTANCE.next(presented);
        VerseRef cursor = next != null ? next : legEnd(state.leg);
        return new State(state.leg, cursor, state.initialTailCompleted);
    }

    private static VerseRef legStart(Leg leg) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_START : FRONT_START;
    }

    private static VerseRef legEnd(Leg leg) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_END : FRONT_END;
    }
}
