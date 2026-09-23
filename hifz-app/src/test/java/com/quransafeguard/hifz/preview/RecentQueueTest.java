package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class RecentQueueTest {
    private static final LocalDate DAY = LocalDate.of(2026, 1, 1);

    @Test public void reviewedIncrementsOnlyTheDisplayedBlock() {
        List<HifzPrefs.RecentSabqi> source = queue();
        List<HifzPrefs.RecentSabqi> result = HifzPrefs.markReviewed(source, 1);

        assertEquals(0, result.get(0).reviewStreak);
        assertEquals(3, result.get(1).reviewStreak);
        assertEquals(1, result.get(2).reviewStreak);
        assertEquals(20, result.get(1).startLine);
        assertEquals(DAY.plusDays(1), result.get(1).addedOn);
    }

    @Test public void strengthenResetsDisplayedBlockAndMovesItToTail() {
        List<HifzPrefs.RecentSabqi> source = queue();
        List<HifzPrefs.RecentSabqi> result = HifzPrefs.deferRecent(source, 1);

        assertEquals(10, result.get(0).startLine);
        assertEquals(30, result.get(1).startLine);
        assertEquals(20, result.get(2).startLine);
        assertEquals(0, result.get(2).reviewStreak);
        assertEquals(DAY.plusDays(1), result.get(2).addedOn);
        assertEquals(1, HifzPrefs.indexAfterDeferral(1, 3));
    }

    @Test public void strengtheningLastBlockWrapsToFirst() {
        List<HifzPrefs.RecentSabqi> result = HifzPrefs.deferRecent(queue(), 2);
        assertEquals(30, result.get(2).startLine);
        assertEquals(0, result.get(2).reviewStreak);
        assertEquals(0, HifzPrefs.indexAfterDeferral(2, 3));
    }

    @Test public void promotionOrderRemainsCanonicalAfterStrengthenReordersDisplayQueue() {
        List<HifzPrefs.RecentSabqi> display = HifzPrefs.deferRecent(queue(), 0);
        assertEquals(20, display.get(0).startLine);
        assertEquals(10, display.get(2).startLine);

        List<HifzPrefs.RecentSabqi> canonical = HifzPrefs.canonicalRecentOrder(display);
        assertEquals(10, canonical.get(0).startLine);
        assertEquals(20, canonical.get(1).startLine);
        assertEquals(30, canonical.get(2).startLine);
    }

    @Test public void removingPromotedCanonicalBlocksRemovesExactBlocksFromDisplayOrder() {
        List<HifzPrefs.RecentSabqi> display = HifzPrefs.deferRecent(queue(), 0); // 20,30,10
        List<HifzPrefs.RecentSabqi> canonical = HifzPrefs.canonicalRecentOrder(display); // 10,20,30
        List<HifzPrefs.RecentSabqi> remaining = HifzPrefs.withoutRecentBlocks(display, canonical.subList(0, 2));

        assertEquals(1, remaining.size());
        assertEquals(30, remaining.get(0).startLine);
    }

    private static List<HifzPrefs.RecentSabqi> queue() {
        ArrayList<HifzPrefs.RecentSabqi> out = new ArrayList<>();
        out.add(new HifzPrefs.RecentSabqi(10, 14, DAY, 0));
        out.add(new HifzPrefs.RecentSabqi(20, 24, DAY.plusDays(1), 2));
        out.add(new HifzPrefs.RecentSabqi(30, 34, DAY.plusDays(2), 1));
        return out;
    }
}