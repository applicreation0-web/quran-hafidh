package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class J10CapacityTest {
    @Test public void tenDayCapacityUsesOnlyReusableTimedReviewSessions() {
        LocalDate sunday = LocalDate.of(2026, 9, 13);
        assertEquals(450, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 36, true));
        assertEquals(390, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 0, false));
    }

    @Test public void normalPriorityTakesJ10J9AndKeepsContiguousGroupSmall() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        Map<Integer, LocalDate> lines = new LinkedHashMap<>();
        lines.put(10, today.minusDays(10));
        lines.put(11, today.minusDays(10));
        lines.put(12, today.minusDays(8));
        lines.put(20, today.minusDays(9));

        List<Integer> picked = J10ReviewPlanner.priorityLineIndexes(
            lines, today, J10ReviewPolicy.Sustainability.NORMAL, 5);
        assertEquals(java.util.Arrays.asList(10, 11), picked);
    }

    @Test public void tensionPullsJ8ButNonTenableAlonePullsJ7() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        Map<Integer, LocalDate> lines = new LinkedHashMap<>();
        lines.put(30, today.minusDays(8));
        lines.put(31, today.minusDays(8));
        lines.put(32, today.minusDays(7));

        assertEquals(java.util.Arrays.asList(30, 31), J10ReviewPlanner.priorityLineIndexes(
            lines, today, J10ReviewPolicy.Sustainability.TENSION, 5));
        assertEquals(java.util.Arrays.asList(30, 31, 32), J10ReviewPlanner.priorityLineIndexes(
            lines, today, J10ReviewPolicy.Sustainability.NON_TENABLE, 5));
    }

    @Test public void oldestUrgentIslandWinsOverLaterUrgentIsland() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        Map<Integer, LocalDate> lines = new LinkedHashMap<>();
        lines.put(40, today.minusDays(9));
        lines.put(41, today.minusDays(9));
        lines.put(100, today.minusDays(10));
        lines.put(101, today.minusDays(10));

        assertEquals(java.util.Arrays.asList(100, 101), J10ReviewPlanner.priorityLineIndexes(
            lines, today, J10ReviewPolicy.Sustainability.NORMAL, 5));
    }
}
