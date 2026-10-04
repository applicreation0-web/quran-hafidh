package com.quransafeguard.hifz.preview;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Immutable line-level result of the schema-5 to schema-6 classification. */
public final class HifzCorpusState {
    private final Set<String> toAnchorLineIds;
    private final Set<String> legacyPartialAcquiredLineIds;
    private final Set<String> quarantineLineIds;
    private final Map<String, Long> quarantineLegacyLastReviewed;
    private final Map<String, Long> activeLastReviewedEpochDays;
    private final Set<String> unknownDueLineIds;
    private final Set<String> legacyImportedLineIds;
    private final Map<String, Long> legacyOrphanDates;
    private final Set<String> acquiredCreditLineIds;

    HifzCorpusState(
            Set<String> toAnchorLineIds,
            Set<String> legacyPartialAcquiredLineIds,
            Set<String> quarantineLineIds,
            Map<String, Long> quarantineLegacyLastReviewed,
            Map<String, Long> activeLastReviewedEpochDays,
            Set<String> unknownDueLineIds,
            Set<String> legacyImportedLineIds,
            Map<String, Long> legacyOrphanDates,
            Set<String> acquiredCreditLineIds) {
        this.toAnchorLineIds = immutableSet(toAnchorLineIds);
        this.legacyPartialAcquiredLineIds = immutableSet(legacyPartialAcquiredLineIds);
        this.quarantineLineIds = immutableSet(quarantineLineIds);
        this.quarantineLegacyLastReviewed = immutableMap(quarantineLegacyLastReviewed);
        this.activeLastReviewedEpochDays = immutableMap(activeLastReviewedEpochDays);
        this.unknownDueLineIds = immutableSet(unknownDueLineIds);
        this.legacyImportedLineIds = immutableSet(legacyImportedLineIds);
        this.legacyOrphanDates = immutableMap(legacyOrphanDates);
        this.acquiredCreditLineIds = immutableSet(acquiredCreditLineIds);
        validate();
    }

    public Set<String> toAnchorLineIds() { return toAnchorLineIds; }
    public Set<String> legacyPartialAcquiredLineIds() { return legacyPartialAcquiredLineIds; }
    public Set<String> quarantineLineIds() { return quarantineLineIds; }
    public Map<String, Long> quarantineLegacyLastReviewed() { return quarantineLegacyLastReviewed; }
    public Map<String, Long> activeLastReviewedEpochDays() { return activeLastReviewedEpochDays; }
    public Set<String> unknownDueLineIds() { return unknownDueLineIds; }
    public Set<String> legacyImportedLineIds() { return legacyImportedLineIds; }
    public Map<String, Long> legacyOrphanDates() { return legacyOrphanDates; }
    public Set<String> acquiredCreditLineIds() { return acquiredCreditLineIds; }

    public Set<String> activeJ10LineIds() {
        LinkedHashSet<String> result = new LinkedHashSet<>(activeLastReviewedEpochDays.keySet());
        result.addAll(unknownDueLineIds);
        return Collections.unmodifiableSet(result);
    }

    private void validate() {
        Set<String> active = activeJ10LineIds();
        requireDisjoint(acquiredCreditLineIds, toAnchorLineIds, "acquired/toAnchor");
        requireDisjoint(quarantineLineIds, acquiredCreditLineIds, "quarantine/acquired");
        requireDisjoint(quarantineLineIds, toAnchorLineIds, "quarantine/toAnchor");
        requireDisjoint(legacyOrphanDates.keySet(), active, "orphan/activeJ10");
        requireDisjoint(quarantineLegacyLastReviewed.keySet(), active, "quarantineDate/activeJ10");
        if (!acquiredCreditLineIds.containsAll(active)) {
            throw new IllegalStateException("Every active J10 line must be credit eligible");
        }
        if (!acquiredCreditLineIds.containsAll(legacyPartialAcquiredLineIds)) {
            throw new IllegalStateException("Legacy partial acquired lines must be credit eligible");
        }
        if (!quarantineLineIds.containsAll(quarantineLegacyLastReviewed.keySet())) {
            throw new IllegalStateException("Quarantine date must belong to a quarantined line");
        }
    }

    private static void requireDisjoint(Set<String> a, Set<String> b, String label) {
        for (String id : a) {
            if (b.contains(id)) throw new IllegalStateException("Overlapping " + label + " line: " + id);
        }
    }

    private static Set<String> immutableSet(Set<String> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(
            source == null ? Collections.emptySet() : source));
    }

    private static Map<String, Long> immutableMap(Map<String, Long> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
            source == null ? Collections.emptyMap() : source));
    }
}
