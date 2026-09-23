package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Persistent-domain model for one page in the anchoring queue. */
public final class AnchoringQueue {
    private AnchoringQueue() {}

    public enum Origin { RECONSTRUCTION, PROMOTED, FORCED_PROMOTION }
    /** Named ItqanProtocol (not just Protocol) to stay distinct from ConsolidationCycleEngine.Protocol. */
    public enum ItqanProtocol { LIGHT, FULL }

    public static final class Entry {
        public final String start;
        public final String end;
        public final Origin origin;
        public final ItqanProtocol protocol;
        public final int failures;

        public Entry(String start, String end, Origin origin, ItqanProtocol protocol, int failures) {
            if (start == null || start.isEmpty() || end == null || end.isEmpty()) {
                throw new IllegalArgumentException("anchoring range required");
            }
            if (origin == null || protocol == null) throw new IllegalArgumentException("anchoring state required");
            this.start = start;
            this.end = end;
            this.origin = origin;
            this.protocol = protocol;
            this.failures = Math.max(0, failures);
        }
    }

    public static final class Deferral {
        public final List<Entry> entries;
        public final int nextIndex;

        private Deferral(List<Entry> entries, int nextIndex) {
            this.entries = Collections.unmodifiableList(entries);
            this.nextIndex = nextIndex;
        }
    }

    public static Origin originFor(boolean reconstruction, boolean forcedPromotion) {
        if (reconstruction) return Origin.RECONSTRUCTION;
        return forcedPromotion ? Origin.FORCED_PROMOTION : Origin.PROMOTED;
    }

    /** Return the cyclic visit order beginning at the persisted current index. */
    public static List<Entry> visitOrder(List<Entry> source, int currentIndex) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        int start = Math.max(0, Math.min(currentIndex, source.size() - 1));
        ArrayList<Entry> out = new ArrayList<>(source.size());
        for (int step = 0; step < source.size(); step++) {
            out.add(source.get((start + step) % source.size()));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Reconcile visit order so acquired promotions are handled before pending reconstruction.
     * An already-started fractionated unit stays first and is never interrupted mid-page.
     */
    public static List<Entry> mergeWithPromotionPriority(List<Entry> existing, int currentIndex,
                                                         boolean keepCurrent, List<Entry> additions) {
        ArrayList<Entry> visit = new ArrayList<>();
        if (existing != null && !existing.isEmpty()) visit.addAll(visitOrder(existing, currentIndex));
        if (additions != null) visit.addAll(additions);

        LinkedHashMap<String, Entry> unique = new LinkedHashMap<>();
        for (Entry entry : visit) {
            if (entry != null) unique.put(entry.start + "→" + entry.end, entry);
        }
        ArrayList<Entry> ordered = new ArrayList<>(unique.values());
        Entry pinned = keepCurrent && !visit.isEmpty() ? visit.get(0) : null;

        ArrayList<Entry> out = new ArrayList<>(ordered.size());
        if (pinned != null) {
            Entry canonicalPinned = unique.get(pinned.start + "→" + pinned.end);
            if (canonicalPinned != null) out.add(canonicalPinned);
        }
        for (Entry entry : ordered) {
            if (pinned != null && sameRange(entry, pinned)) continue;
            if (entry.origin != Origin.RECONSTRUCTION) out.add(entry);
        }
        for (Entry entry : ordered) {
            if (pinned != null && sameRange(entry, pinned)) continue;
            if (entry.origin == Origin.RECONSTRUCTION) out.add(entry);
        }
        return Collections.unmodifiableList(out);
    }

    private static boolean sameRange(Entry a, Entry b) {
        return a != null && b != null && a.start.equals(b.start) && a.end.equals(b.end);
    }

    public static Entry findByRange(List<Entry> source, String start, String end) {
        if (source == null || start == null || end == null) return null;
        for (Entry entry : source) {
            if (entry != null && start.equals(entry.start) && end.equals(entry.end)) return entry;
        }
        return null;
    }

    /**
     * Move the page actually displayed behind {@code places} following distinct entries in cyclic
     * visit order. With only one page, +3 cannot be represented without an immediate replay, so
     * {@code nextIndex == -1} explicitly means retry on a later Ancrage session.
     */
    public static Deferral defer(List<Entry> source, int displayedIndex, int places) {
        if (source == null || source.isEmpty()) throw new IllegalArgumentException("anchoring queue required");
        if (displayedIndex < 0 || displayedIndex >= source.size()) {
            throw new IllegalArgumentException("displayed anchoring index outside queue");
        }
        Entry displayed = source.get(displayedIndex);
        if (source.size() == 1) {
            return new Deferral(Collections.singletonList(displayed), -1);
        }
        ArrayList<Entry> otherVisits = new ArrayList<>(source.size() - 1);
        for (int step = 1; step < source.size(); step++) {
            otherVisits.add(source.get((displayedIndex + step) % source.size()));
        }
        int insertion = Math.min(Math.max(0, places), otherVisits.size());
        otherVisits.add(insertion, displayed);
        return new Deferral(otherVisits, 0);
    }

    public static Deferral failAndDefer(List<Entry> source, int displayedIndex, int places) {
        if (source == null || displayedIndex < 0 || displayedIndex >= source.size()) {
            throw new IllegalArgumentException("displayed anchoring index outside queue");
        }
        ArrayList<Entry> failed = new ArrayList<>(source);
        Entry current = failed.get(displayedIndex);
        int failures = current.failures + 1;
        ItqanProtocol protocol = failures >= 3 ? ItqanProtocol.FULL : current.protocol;
        failed.set(displayedIndex, new Entry(current.start, current.end, current.origin, protocol, failures));
        return defer(failed, displayedIndex, places);
    }
}
