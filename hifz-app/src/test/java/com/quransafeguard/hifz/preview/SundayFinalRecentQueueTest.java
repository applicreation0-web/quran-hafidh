package com.quransafeguard.hifz.preview;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public final class SundayFinalRecentQueueTest {
    @Test public void combinedPageGraduationRemovesThreeSeparateFiveLineBlocks() {
        LocalDate original=LocalDate.of(2026,10,19);
        List<HifzPrefs.RecentSabqi> originalBlocks=Arrays.asList(
            new HifzPrefs.RecentSabqi(100,104,original,2),
            new HifzPrefs.RecentSabqi(105,109,original,1),
            new HifzPrefs.RecentSabqi(110,114,original,0));
        Set<Integer> credited=new HashSet<>();
        for(int line=100;line<=114;line++)credited.add(line);
        assertTrue(HifzPrefs.withoutCreditedRecentLines(originalBlocks,credited).isEmpty());
    }

    @Test public void partialSundayLeavesOnlyUnacquiredIntervalsWithHistoricalMetadata() {
        LocalDate earlier=LocalDate.of(2026,10,12);
        HifzPrefs.RecentSabqi block=new HifzPrefs.RecentSabqi(100,109,earlier,3);
        List<HifzPrefs.RecentSabqi> left=HifzPrefs.withoutCreditedRecentLines(
            Collections.singletonList(block),new HashSet<>(Arrays.asList(102,103,104,107,108)));
        assertEquals(3,left.size());
        assertEquals(100,left.get(0).startLine); assertEquals(101,left.get(0).endLine);
        assertEquals(105,left.get(1).startLine); assertEquals(106,left.get(1).endLine);
        assertEquals(109,left.get(2).startLine); assertEquals(109,left.get(2).endLine);
        for(HifzPrefs.RecentSabqi part:left) {
            assertEquals(earlier,part.addedOn);
            assertEquals(3,part.reviewStreak);
        }
    }

    @Test public void unearnedLinesAndOtherWeeksAreUnaffected() {
        HifzPrefs.RecentSabqi a=new HifzPrefs.RecentSabqi(20,24,LocalDate.of(2026,10,19),1);
        HifzPrefs.RecentSabqi b=new HifzPrefs.RecentSabqi(25,29,LocalDate.of(2026,10,12),2);
        List<HifzPrefs.RecentSabqi> remaining=HifzPrefs.withoutCreditedRecentLines(
            Arrays.asList(a,b),new HashSet<>(Arrays.asList(20,21,22,23,24)));
        assertEquals(1,remaining.size());assertEquals(25,remaining.get(0).startLine);
        assertEquals(29,remaining.get(0).endLine);
        assertEquals(b.addedOn,remaining.get(0).addedOn);
    }
}
