package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RecentPromotionPolicyTest {
    private static final LocalDate ADDED = LocalDate.of(2024, 1, 1);

    @Test public void eightyNineDaysNeverPromotes() {
        RecentPromotionPolicy.Decision decision = RecentPromotionPolicy.evaluate(
            ADDED, ADDED.plusDays(89), ADDED, completedSundays(13), 40, true);
        assertFalse(decision.promote);
    }

    @Test public void ninetyOneDaysAndOriginalAttendanceRatioPromotes() {
        RecentPromotionPolicy.Decision decision = RecentPromotionPolicy.evaluate(
            ADDED, ADDED.plusDays(91), ADDED, completedSundays(10), 40, true);
        assertEquals(13, decision.plannedSessions);
        assertEquals(10, decision.requiredSessions);
        assertEquals(10, decision.completedSessions);
        assertTrue(decision.promote);
        assertFalse(decision.forced);
    }

    @Test public void ninetyOneDaysWithoutEnoughAttendanceStaysRecent() {
        RecentPromotionPolicy.Decision decision = RecentPromotionPolicy.evaluate(
            ADDED, ADDED.plusDays(91), ADDED, completedSundays(9), 40, true);
        assertFalse(decision.promote);
    }

    @Test public void sixtyFirstBlockForcesOnlyTheOldest() {
        RecentPromotionPolicy.Decision oldest = RecentPromotionPolicy.evaluate(
            ADDED.plusDays(88), ADDED.plusDays(89), ADDED, completedSundays(0), 61, true);
        RecentPromotionPolicy.Decision second = RecentPromotionPolicy.evaluate(
            ADDED.plusDays(88), ADDED.plusDays(89), ADDED, completedSundays(0), 61, false);
        assertTrue(oldest.promote);
        assertTrue(oldest.forced);
        assertFalse(second.promote);
    }

    private static List<LocalDate> completedSundays(int count) {
        ArrayList<LocalDate> dates = new ArrayList<>();
        LocalDate date = LocalDate.of(2024, 1, 7);
        for (int i = 0; i < count; i++) dates.add(date.plusWeeks(i));
        return dates;
    }
}
