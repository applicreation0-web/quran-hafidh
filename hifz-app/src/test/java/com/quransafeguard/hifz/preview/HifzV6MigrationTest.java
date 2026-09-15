package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HifzV6MigrationTest {
    @Test public void pendingUnfinishedOutsideStableRemainsToAnchorAndLegacyDateBecomesOrphan() {
        long old = LocalDate.of(2026, 9, 1).toEpochDay();
        HifzCorpusState state = HifzV6Migration.classify(input(
            set("L1"),
            Collections.emptySet(),
            Collections.emptySet(),
            map("L1", old),
            Collections.emptyMap()
        ));

        assertTrue(state.toAnchorLineIds().contains("L1"));
        assertEquals(Long.valueOf(old), state.legacyOrphanDates().get("L1"));
        assertFalse(state.activeJ10LineIds().contains("L1"));
        assertFalse(state.legacyPartialAcquiredLineIds().contains("L1"));
    }

    @Test public void pendingCompletedBlockBecomesOverlayAndKeepsExactLegacyDate() {
        long old = LocalDate.of(2026, 9, 2).toEpochDay();
        HifzCorpusState state = HifzV6Migration.classify(input(
            set("L2"),
            set("L2"),
            Collections.emptySet(),
            map("L2", old),
            Collections.emptyMap()
        ));

        assertTrue(state.legacyPartialAcquiredLineIds().contains("L2"));
        assertTrue(state.acquiredCreditLineIds().contains("L2"));
        assertEquals(Long.valueOf(old), state.activeLastReviewedEpochDays().get("L2"));
        assertTrue(state.legacyImportedLineIds().contains("L2"));
        assertFalse(state.toAnchorLineIds().contains("L2"));
    }

    @Test public void pendingCompletedBlockWithoutDateIsUnknownDue() {
        HifzCorpusState state = HifzV6Migration.classify(input(
            set("L3"),
            set("L3"),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap()
        ));

        assertTrue(state.legacyPartialAcquiredLineIds().contains("L3"));
        assertTrue(state.unknownDueLineIds().contains("L3"));
        assertTrue(state.activeJ10LineIds().contains("L3"));
        assertFalse(state.activeLastReviewedEpochDays().containsKey("L3"));
    }

    @Test public void pendingUnfinishedAlsoStableIsQuarantinedWithoutSilentDemotion() {
        long old = LocalDate.of(2026, 8, 30).toEpochDay();
        HifzCorpusState state = HifzV6Migration.classify(input(
            set("L4"),
            Collections.emptySet(),
            set("L4"),
            map("L4", old),
            Collections.emptyMap()
        ));

        assertTrue(state.quarantineLineIds().contains("L4"));
        assertEquals(Long.valueOf(old), state.quarantineLegacyLastReviewed().get("L4"));
        assertFalse(state.toAnchorLineIds().contains("L4"));
        assertFalse(state.acquiredCreditLineIds().contains("L4"));
        assertFalse(state.activeJ10LineIds().contains("L4"));
        assertFalse(state.legacyOrphanDates().containsKey("L4"));
    }

    @Test public void stableLineOutsidePendingWithLegacyDateRemainsAcquiredAndImported() {
        long old = LocalDate.of(2026, 8, 29).toEpochDay();
        HifzCorpusState state = HifzV6Migration.classify(input(
            Collections.emptySet(),
            Collections.emptySet(),
            set("L5"),
            map("L5", old),
            Collections.emptyMap()
        ));

        assertTrue(state.acquiredCreditLineIds().contains("L5"));
        assertEquals(Long.valueOf(old), state.activeLastReviewedEpochDays().get("L5"));
        assertTrue(state.legacyImportedLineIds().contains("L5"));
    }

    @Test public void stableLineOutsidePendingWithoutDateIsUnknownDue() {
        HifzCorpusState state = HifzV6Migration.classify(input(
            Collections.emptySet(),
            Collections.emptySet(),
            set("L6"),
            Collections.emptyMap(),
            Collections.emptyMap()
        ));

        assertTrue(state.acquiredCreditLineIds().contains("L6"));
        assertTrue(state.unknownDueLineIds().contains("L6"));
    }

    @Test public void recentSabqiWithoutLegacyDateUsesExactAddedOnDate() {
        long added = LocalDate.of(2026, 8, 20).toEpochDay();
        HifzCorpusState state = HifzV6Migration.classify(input(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            map("S1", added)
        ));

        assertTrue(state.acquiredCreditLineIds().contains("S1"));
        assertEquals(Long.valueOf(added), state.activeLastReviewedEpochDays().get("S1"));
        assertTrue(state.legacyImportedLineIds().contains("S1"));
    }

    @Test(expected = IllegalStateException.class)
    public void completedPendingLineMustActuallyBelongToPending() {
        HifzV6Migration.classify(input(
            Collections.emptySet(),
            set("bad"),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap()
        ));
    }

    private static HifzV6Migration.Input input(
            Set<String> pending,
            Set<String> completedPending,
            Set<String> stable,
            Map<String, Long> legacyDates,
            Map<String, Long> recentSabqiAddedOn) {
        return new HifzV6Migration.Input(
            pending,
            completedPending,
            stable,
            legacyDates,
            recentSabqiAddedOn
        );
    }

    private static LinkedHashSet<String> set(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Collections.addAll(result, values);
        return result;
    }

    private static Map<String, Long> map(String key, long value) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }
}
