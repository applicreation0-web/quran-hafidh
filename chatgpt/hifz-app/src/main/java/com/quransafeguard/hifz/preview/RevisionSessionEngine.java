package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure traversal/credit engine for one Révision session. */
final class RevisionSessionEngine {
    static final class CommitResult {
        final RevisionSessionState state;
        final Set<String> creditedLineIds;
        final String nextCursor;

        CommitResult(RevisionSessionState state, Set<String> creditedLineIds, String nextCursor) {
            this.state = state;
            this.creditedLineIds = Collections.unmodifiableSet(new LinkedHashSet<>(creditedLineIds));
            this.nextCursor = nextCursor;
        }
    }

    private final List<String> rotation;
    private final LinkedHashMap<String, Integer> indexById;
    private final Set<String> acquiredCreditLineIds;

    RevisionSessionEngine(List<String> rotationLineIds, Set<String> acquiredCreditLineIds) {
        if (rotationLineIds == null || rotationLineIds.isEmpty()) {
            throw new IllegalArgumentException("revision rotation required");
        }
        ArrayList<String> ordered = new ArrayList<>();
        LinkedHashMap<String, Integer> indexes = new LinkedHashMap<>();
        for (String id : rotationLineIds) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("blank rotation line id");
            if (indexes.containsKey(id)) throw new IllegalArgumentException("duplicate rotation line id: " + id);
            indexes.put(id, ordered.size());
            ordered.add(id);
        }
        LinkedHashSet<String> acquired = sanitize(acquiredCreditLineIds);
        if (!indexes.keySet().containsAll(acquired)) {
            throw new IllegalArgumentException("acquired credit must be inside revision rotation");
        }
        this.rotation = Collections.unmodifiableList(ordered);
        this.indexById = indexes;
        this.acquiredCreditLineIds = Collections.unmodifiableSet(acquired);
    }

    RevisionSessionState start(String sessionId, String startCursor) {
        if (startCursor == null || !indexById.containsKey(startCursor)) {
            throw new IllegalArgumentException("start cursor outside revision rotation");
        }
        return new RevisionSessionState(
            sessionId,
            startCursor,
            startCursor,
            Collections.emptySet(),
            0,
            0,
            0L,
            false);
    }

    RevisionSessionState visit(RevisionSessionState state, String lineId, long elapsedDeltaMs) {
        requireCompatibleOpenState(state);
        if (elapsedDeltaMs < 0L) throw new IllegalArgumentException("negative elapsed delta");
        if (lineId == null || !indexById.containsKey(lineId)) {
            throw new IllegalArgumentException("visited line outside revision rotation");
        }

        String expected = state.occurrences() == 0
            ? state.startCursor()
            : nextAfter(state.endCursor());
        if (!expected.equals(lineId)) {
            throw new IllegalStateException("non-sequential revision traversal: expected " + expected + " got " + lineId);
        }

        int laps = state.laps();
        if (state.occurrences() > 0
                && indexById.get(state.endCursor()) == rotation.size() - 1
                && indexById.get(lineId) == 0) {
            laps++;
        }

        LinkedHashSet<String> visited = new LinkedHashSet<>(state.visitedUniqueLineIds());
        visited.add(lineId);
        return new RevisionSessionState(
            state.sessionId(),
            state.startCursor(),
            lineId,
            visited,
            state.occurrences() + 1,
            laps,
            safeAdd(state.elapsedMs(), elapsedDeltaMs),
            false);
    }

    CommitResult commit(RevisionSessionState state, Collection<String> alreadyCreditedToday) {
        requireCompatibleOpenState(state);
        LinkedHashSet<String> already = sanitize(alreadyCreditedToday);
        LinkedHashSet<String> credited = new LinkedHashSet<>();
        for (String id : state.visitedUniqueLineIds()) {
            if (acquiredCreditLineIds.contains(id) && !already.contains(id)) credited.add(id);
        }
        String nextCursor = state.occurrences() == 0 ? state.startCursor() : nextAfter(state.endCursor());
        RevisionSessionState committed = new RevisionSessionState(
            state.sessionId(),
            state.startCursor(),
            state.endCursor(),
            state.visitedUniqueLineIds(),
            state.occurrences(),
            state.laps(),
            state.elapsedMs(),
            true);
        return new CommitResult(committed, credited, nextCursor);
    }

    private void requireCompatibleOpenState(RevisionSessionState state) {
        if (state == null) throw new IllegalArgumentException("revision state required");
        if (state.committed()) throw new IllegalStateException("revision session already committed");
        if (!indexById.containsKey(state.startCursor()) || !indexById.containsKey(state.endCursor())) {
            throw new IllegalStateException("revision cursor outside rotation");
        }
    }

    private String nextAfter(String lineId) {
        Integer index = indexById.get(lineId);
        if (index == null) throw new IllegalStateException("revision line outside rotation");
        return rotation.get((index + 1) % rotation.size());
    }

    private static LinkedHashSet<String> sanitize(Collection<String> values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values == null) return out;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) out.add(value);
        }
        return out;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            throw new IllegalArgumentException("revision elapsed overflow");
        }
        return left + right;
    }
}
