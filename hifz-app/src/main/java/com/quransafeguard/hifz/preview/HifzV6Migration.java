package com.quransafeguard.hifz.preview;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Pure schema-5 to schema-6 classifier. It performs no persistence writes. */
public final class HifzV6Migration {
    private HifzV6Migration() {}

    public static final class Input {
        final Set<String> pendingLineIds;
        final Set<String> structurallyCompletedPendingLineIds;
        final Set<String> legacyStableLineIds;
        final Map<String, Long> legacyJ10EpochDays;
        final Map<String, Long> recentSabqiAddedOnEpochDays;

        public Input(
                Set<String> pendingLineIds,
                Set<String> structurallyCompletedPendingLineIds,
                Set<String> legacyStableLineIds,
                Map<String, Long> legacyJ10EpochDays,
                Map<String, Long> recentSabqiAddedOnEpochDays) {
            this.pendingLineIds = copySet(pendingLineIds);
            this.structurallyCompletedPendingLineIds = copySet(structurallyCompletedPendingLineIds);
            this.legacyStableLineIds = copySet(legacyStableLineIds);
            this.legacyJ10EpochDays = copyMap(legacyJ10EpochDays);
            this.recentSabqiAddedOnEpochDays = copyMap(recentSabqiAddedOnEpochDays);
        }
    }

    public static HifzCorpusState classify(Input input) {
        if (input == null) throw new IllegalArgumentException("input required");
        if (!input.pendingLineIds.containsAll(input.structurallyCompletedPendingLineIds)) {
            throw new IllegalStateException("Completed pending lines must belong to pending corpus");
        }

        LinkedHashSet<String> toAnchor = new LinkedHashSet<>();
        LinkedHashSet<String> overlay = new LinkedHashSet<>();
        LinkedHashSet<String> quarantine = new LinkedHashSet<>();
        LinkedHashMap<String, Long> quarantineDates = new LinkedHashMap<>();
        LinkedHashMap<String, Long> activeDates = new LinkedHashMap<>();
        LinkedHashSet<String> unknownDue = new LinkedHashSet<>();
        LinkedHashSet<String> imported = new LinkedHashSet<>();
        LinkedHashMap<String, Long> orphanDates = new LinkedHashMap<>();
        LinkedHashSet<String> acquired = new LinkedHashSet<>();

        LinkedHashSet<String> stableEvidence = new LinkedHashSet<>(input.legacyStableLineIds);
        stableEvidence.addAll(input.recentSabqiAddedOnEpochDays.keySet());

        for (String lineId : input.pendingLineIds) {
            Long oldDate = input.legacyJ10EpochDays.get(lineId);
            Long recentDate = input.recentSabqiAddedOnEpochDays.get(lineId);

            if (input.structurallyCompletedPendingLineIds.contains(lineId)) {
                overlay.add(lineId);
                acquired.add(lineId);
                Long date = oldDate != null ? oldDate : recentDate;
                if (date == null) {
                    unknownDue.add(lineId);
                } else {
                    activeDates.put(lineId, date);
                    imported.add(lineId);
                }
            } else if (stableEvidence.contains(lineId)) {
                quarantine.add(lineId);
                Long date = oldDate != null ? oldDate : recentDate;
                if (date != null) quarantineDates.put(lineId, date);
            } else {
                toAnchor.add(lineId);
                if (oldDate != null) orphanDates.put(lineId, oldDate);
            }
        }

        LinkedHashSet<String> outsidePending = new LinkedHashSet<>();
        outsidePending.addAll(input.legacyStableLineIds);
        outsidePending.addAll(input.legacyJ10EpochDays.keySet());
        outsidePending.addAll(input.recentSabqiAddedOnEpochDays.keySet());
        outsidePending.removeAll(input.pendingLineIds);

        for (String lineId : outsidePending) {
            acquired.add(lineId);
            Long oldDate = input.legacyJ10EpochDays.get(lineId);
            Long recentDate = input.recentSabqiAddedOnEpochDays.get(lineId);
            if (oldDate != null) {
                activeDates.put(lineId, oldDate);
                imported.add(lineId);
            } else if (recentDate != null) {
                activeDates.put(lineId, recentDate);
                imported.add(lineId);
            } else {
                unknownDue.add(lineId);
            }
        }

        return new HifzCorpusState(
            toAnchor,
            overlay,
            quarantine,
            quarantineDates,
            activeDates,
            unknownDue,
            imported,
            orphanDates,
            acquired
        );
    }

    private static Set<String> copySet(Set<String> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(
            source == null ? Collections.emptySet() : source));
    }

    private static Map<String, Long> copyMap(Map<String, Long> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(
            source == null ? Collections.emptyMap() : source));
    }
}
