package com.quransafeguard.hifz.preview;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable state for one Révision session. */
final class RevisionSessionState {
    private final String sessionId;
    private final String startCursor;
    private final String endCursor;
    private final LinkedHashSet<String> visitedUniqueLineIds;
    private final int occurrences;
    private final int laps;
    private final long elapsedMs;
    private final boolean committed;

    RevisionSessionState(String sessionId,
                         String startCursor,
                         String endCursor,
                         Set<String> visitedUniqueLineIds,
                         int occurrences,
                         int laps,
                         long elapsedMs,
                         boolean committed) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId required");
        }
        if (startCursor == null || startCursor.trim().isEmpty()) {
            throw new IllegalArgumentException("startCursor required");
        }
        if (endCursor == null || endCursor.trim().isEmpty()) {
            throw new IllegalArgumentException("endCursor required");
        }
        if (occurrences < 0 || laps < 0 || elapsedMs < 0L) {
            throw new IllegalArgumentException("negative revision counters");
        }
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        if (visitedUniqueLineIds != null) {
            for (String id : visitedUniqueLineIds) {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("blank visited line id");
                }
                visited.add(id);
            }
        }
        if (visited.size() > occurrences) {
            throw new IllegalArgumentException("unique visits exceed occurrences");
        }
        this.sessionId = sessionId;
        this.startCursor = startCursor;
        this.endCursor = endCursor;
        this.visitedUniqueLineIds = visited;
        this.occurrences = occurrences;
        this.laps = laps;
        this.elapsedMs = elapsedMs;
        this.committed = committed;
    }

    String sessionId() { return sessionId; }
    String startCursor() { return startCursor; }
    String endCursor() { return endCursor; }
    Set<String> visitedUniqueLineIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(visitedUniqueLineIds));
    }
    int occurrences() { return occurrences; }
    int laps() { return laps; }
    long elapsedMs() { return elapsedMs; }
    boolean committed() { return committed; }
}
