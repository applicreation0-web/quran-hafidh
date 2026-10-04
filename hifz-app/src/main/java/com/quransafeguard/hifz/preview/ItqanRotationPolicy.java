package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * P4's perpetual Itqān macro-cycle: TAIL (Al-Hujurāt → An-Nās, 49:1..114:6) then FRONT (Al-Baqara
 * → Al-Fatḥ, 2:1..48:29) then TAIL again, forever. Pure and Android-free — it only ever advances
 * the cursor within whichever leg is current. Every physical unit in a leg's bounds is met on its
 * turn regardless of its Stabilisation/Acquis status: status only decides what HifzPrefs does once
 * a unit is reached (build it, or give it another full reinforcement pass), never whether or when
 * it is reached — a unit already ACQUIRED long ago is never skipped, and the cycle never stops.
 *
 * A unit is never jumped to or rewound for: the cursor only ever advances past whatever was just
 * presented, so the leg's own physical order is walked exactly once per lap, every lap, forever.
 * The only state carried between calls is the leg and the cursor itself.
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
     *  the leg never changes here, only nextInLeg's own scan start moves forward. Completing the
     *  unit that ends at An-Nās 114:6 on the TAIL leg is the first-arrival event of the latest
     *  user decision: it latches initialTailCompleted (post-An-Nās maintenance) right here, in the
     *  same commit as the cursor move, instead of waiting for a later leg flip that is only
     *  persisted when the next leg happens to have a unit. */
    static State advancedPast(State state, VerseRef presented) {
        VerseRef next = com.quransafeguard.hifz.core.QuranCanon.INSTANCE.next(presented);
        VerseRef cursor = next != null ? next : legEnd(state.leg);
        boolean reachedNas = state.leg == Leg.TAIL_HUJURAT_NAS
            && GeometryRepository.ordinal(presented) >= GeometryRepository.ordinal(TAIL_END);
        return new State(state.leg, cursor, state.initialTailCompleted || reachedNas);
    }

    /** One-way latch: once post-An-Nās maintenance was ever persisted, no later state write can
     *  turn it back off (a stale in-memory state, a legacy key, a corrupt cursor). */
    static State latched(State state, boolean everCompleted) {
        if (state == null) throw new IllegalArgumentException("state required");
        if (state.initialTailCompleted || !everCompleted) return state;
        return new State(state.leg, state.cursor, true);
    }

    /**
     * Last verse the FRONT leg may visit: strictly before the first verse of Sabqi's current,
     * not-yet-validated block, capped at FRONT_END. Null when Sabqi has not yet put anything
     * behind it inside FRONT. Itqān reads this frontier, never writes it.
     */
    static VerseRef frontLegEnd(VerseRef sabqiCurrentBlockStart) {
        if (sabqiCurrentBlockStart == null) return null;
        int frontier = GeometryRepository.ordinal(sabqiCurrentBlockStart);
        if (frontier <= GeometryRepository.ordinal(FRONT_START)) return null;
        if (frontier > GeometryRepository.ordinal(FRONT_END)) return FRONT_END;
        return com.quransafeguard.hifz.core.QuranCanon.INSTANCE.fromOrdinal(frontier - 1);
    }

    private static VerseRef legStart(Leg leg) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_START : FRONT_START;
    }

    private static VerseRef legEnd(Leg leg) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_END : FRONT_END;
    }

    /** The unit the rotation lands on, and the (possibly flipped) state it was found in. */
    static final class Pick {
        final AnchoringQueue.Entry unit;
        final State state;

        Pick(AnchoringQueue.Entry unit, State state) {
            this.unit = unit;
            this.state = state;
        }
    }

    /**
     * Picks the first unit starting at or after the cursor in the current leg. Three looks: the
     * rest of this leg, the other leg from its start, then this leg again from its start — so
     * reaching the Sabqi frontier on FRONT (or FRONT still being empty) always restarts the next
     * tour at Al-Hujurāt 49:1 instead of returning nothing. Read-only on Sabqi: the frontier is
     * baked into unitsInLeg by the caller and never written here.
     */
    static Pick pick(State original, java.util.function.Function<Leg, List<AnchoringQueue.Entry>> unitsInLeg) {
        State state = original;
        for (int attempt = 0; attempt < 3; attempt++) {
            for (AnchoringQueue.Entry unit : unitsInLeg.apply(state.leg)) {
                if (GeometryRepository.ordinal(GeometryRepository.parseVerse(unit.start))
                        >= GeometryRepository.ordinal(state.cursor)) {
                    return new Pick(unit, state);
                }
            }
            state = onLegExhausted(state);
        }
        return new Pick(null, original);
    }

    /** Display-only preview (WeeklyDashboardPlanner's 7-day projection): sorts candidates into
     *  (state.leg first, other leg second) order, each ordinal-ascending — never persists or
     *  decides anything, and never itself filters by Stabilisation/Acquis status. */
    static List<AnchoringQueue.Entry> projectedOrder(State state, List<AnchoringQueue.Entry> candidates) {
        ArrayList<AnchoringQueue.Entry> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
            .comparingInt((AnchoringQueue.Entry e) -> legRank(state.leg, e))
            .thenComparingInt(e -> GeometryRepository.ordinal(GeometryRepository.parseVerse(e.start))));
        return sorted;
    }

    private static int legRank(Leg startingLeg, AnchoringQueue.Entry entry) {
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        int o = GeometryRepository.ordinal(start);
        boolean inStartingLeg = o >= GeometryRepository.ordinal(legStart(startingLeg))
            && o <= GeometryRepository.ordinal(legEnd(startingLeg));
        return inStartingLeg ? 0 : 1;
    }
}
