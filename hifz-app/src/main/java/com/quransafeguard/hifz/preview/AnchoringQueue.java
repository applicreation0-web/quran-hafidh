package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persistent-domain model for one page in the anchoring queue. */
public final class AnchoringQueue {
    private AnchoringQueue() {}

    public enum Origin { RECONSTRUCTION, PROMOTED }
    public enum Protocol { LIGHT, FULL }

    public static final class Entry {
        public final String start;
        public final String end;
        public final Origin origin;
        public final Protocol protocol;
        public final int failures;

        public Entry(String start, String end, Origin origin, Protocol protocol, int failures) {
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

    /** Move the page actually displayed behind {@code places} following entries. */
    public static Deferral defer(List<Entry> source, int displayedIndex, int places) {
        if (source == null || source.isEmpty()) throw new IllegalArgumentException("anchoring queue required");
        if (displayedIndex < 0 || displayedIndex >= source.size()) {
            throw new IllegalArgumentException("displayed anchoring index outside queue");
        }
        ArrayList<Entry> next = new ArrayList<>(source);
        Entry displayed = next.remove(displayedIndex);
        int insertion = Math.min(next.size(), displayedIndex + Math.max(0, places));
        next.add(insertion, displayed);
        return new Deferral(next, Math.min(displayedIndex, next.size() - 1));
    }
}
