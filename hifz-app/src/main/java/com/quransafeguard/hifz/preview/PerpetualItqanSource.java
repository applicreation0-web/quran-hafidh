package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * P4: migrates the Ancrage/Stabilisation queue's own selection order away from the historical
 * cyclic list-index scan (HifzPrefs.currentAnchoringEntry), to the perpetual macro-order the
 * user actually walks the Mushaf in for Stabilisation and Consolidation: every not-yet-done unit
 * in Al-Hujurāt→An-Nās (TAIL) first, then every not-yet-done unit in Al-Baqara→[wherever Sabqi has
 * actually reached] (FRONT) — forever. Consolidation needs no separate wiring of its own: it only
 * ever processes material that has already cleared Stabilisation, so it inherits this same order
 * for free.
 *
 * Pure and Android-free — HifzPrefs owns persisting the State this returns and handing this class
 * whichever AnchoringQueue entries are currently not fully Stabilisé/Acquis; this class only ever
 * picks among entries it's given, it never builds or mutates the queue itself.
 */
final class PerpetualItqanSource {
    private PerpetualItqanSource() {}

    enum Leg { TAIL_HUJURAT_NAS, FRONT_BAQARA_HUJURAT }

    static final VerseRef TAIL_START = new VerseRef(49, 1);
    static final VerseRef TAIL_END = new VerseRef(114, 6);
    static final VerseRef FRONT_START = new VerseRef(2, 1);

    static final class State {
        final Leg leg;
        /** Sticky: becomes true the first time TAIL's own material ever runs dry (nothing pending
         *  there), and is never reset back to false afterwards. */
        final boolean initialTailCompleted;

        State(Leg leg, boolean initialTailCompleted) {
            this.leg = leg;
            this.initialTailCompleted = initialTailCompleted;
        }

        static State startOfTail() {
            return new State(Leg.TAIL_HUJURAT_NAS, false);
        }

        private State flipped() {
            Leg next = leg == Leg.TAIL_HUJURAT_NAS ? Leg.FRONT_BAQARA_HUJURAT : Leg.TAIL_HUJURAT_NAS;
            boolean tailDone = initialTailCompleted || leg == Leg.TAIL_HUJURAT_NAS;
            return new State(next, tailDone);
        }
    }

    static final class Selection {
        final State state;
        final AnchoringQueue.Entry entry;

        Selection(State state, AnchoringQueue.Entry entry) {
            this.state = state;
            this.entry = entry;
        }
    }

    /**
     * The earliest not-done entry (by start ordinal) within state.leg's own bounds — FRONT's own
     * upper bound is sabqiFrontier itself, never further than what's actually been learned, so
     * FRONT is always "devancé par le Sabqi" — or, if that leg currently has nothing pending,
     * flips to the other leg and retries there (bounded to the one possible flip; there are only
     * two legs). Null only when neither leg currently has anything pending at all — the whole
     * queue is caught up for now.
     */
    static Selection selectNext(State state, List<AnchoringQueue.Entry> notDoneEntries, VerseRef sabqiFrontier) {
        AnchoringQueue.Entry entry = earliestInLeg(state.leg, notDoneEntries, sabqiFrontier);
        if (entry != null) return new Selection(state, entry);
        State flipped = state.flipped();
        AnchoringQueue.Entry flippedEntry = earliestInLeg(flipped.leg, notDoneEntries, sabqiFrontier);
        return flippedEntry != null ? new Selection(flipped, flippedEntry) : null;
    }

    private static AnchoringQueue.Entry earliestInLeg(
            Leg leg, List<AnchoringQueue.Entry> notDoneEntries, VerseRef sabqiFrontier) {
        int legStartOrdinal = GeometryRepository.ordinal(legStart(leg));
        int legEndOrdinal = GeometryRepository.ordinal(legEnd(leg, sabqiFrontier));
        AnchoringQueue.Entry best = null;
        int bestOrdinal = Integer.MAX_VALUE;
        for (AnchoringQueue.Entry entry : notDoneEntries) {
            int o = GeometryRepository.ordinal(GeometryRepository.parseVerse(entry.start));
            if (o < legStartOrdinal || o > legEndOrdinal) continue;
            if (o < bestOrdinal) {
                bestOrdinal = o;
                best = entry;
            }
        }
        return best;
    }

    /**
     * Display-only convenience for WeeklyDashboardPlanner's 7-day preview: the same candidate
     * entries, sorted into the same TAIL-then-FRONT macro order selectNext would actually walk
     * starting from startingState's own current leg — entries in that leg first (by ordinal),
     * then every entry in the other leg (by ordinal). This never persists or decides anything —
     * real day-to-day selection must always go through selectNext/HifzPrefs's own persisted
     * state, never this.
     */
    static List<AnchoringQueue.Entry> projectedOrder(
            State startingState, List<AnchoringQueue.Entry> candidateEntries, VerseRef sabqiFrontier) {
        List<AnchoringQueue.Entry> sorted = new ArrayList<>(candidateEntries);
        sorted.sort(Comparator
            .comparingInt((AnchoringQueue.Entry e) -> legRank(startingState.leg, e, sabqiFrontier))
            .thenComparingInt(e -> GeometryRepository.ordinal(GeometryRepository.parseVerse(e.start))));
        return sorted;
    }

    private static int legRank(Leg startingLeg, AnchoringQueue.Entry entry, VerseRef sabqiFrontier) {
        int o = GeometryRepository.ordinal(GeometryRepository.parseVerse(entry.start));
        boolean inStartingLeg = o >= GeometryRepository.ordinal(legStart(startingLeg))
            && o <= GeometryRepository.ordinal(legEnd(startingLeg, sabqiFrontier));
        return inStartingLeg ? 0 : 1;
    }

    private static VerseRef legStart(Leg leg) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_START : FRONT_START;
    }

    private static VerseRef legEnd(Leg leg, VerseRef sabqiFrontier) {
        return leg == Leg.TAIL_HUJURAT_NAS ? TAIL_END : sabqiFrontier;
    }
}
