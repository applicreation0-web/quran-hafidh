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

    @Test public void lateConsolidationActivationDoesNotLowerTheAttendanceBar() {
        LocalDate activation = ADDED.plusDays(84);
        List<LocalDate> firstTwo = completedSundaysFrom(activation, 2);
        RecentPromotionPolicy.Decision early = RecentPromotionPolicy.evaluate(
            ADDED, ADDED.plusDays(91), activation, firstTwo, 40, true);
        assertEquals(13, early.plannedSessions);
        assertEquals(10, early.requiredSessions);
        // At J+91 only the first Sunday after activation has elapsed; the second one is still future.
        assertEquals(1, early.completedSessions);
        assertFalse(early.promote);

        LocalDate later = activation.plusDays(70);
        RecentPromotionPolicy.Decision ready = RecentPromotionPolicy.evaluate(
            ADDED, later, activation, completedSundaysFrom(activation, 10), 40, true);
        assertEquals(10, ready.completedSessions);
        assertTrue(ready.promote);
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
        return completedSundaysFrom(LocalDate.of(2024, 1, 1), count);
    }

    private static List<LocalDate> completedSundaysFrom(LocalDate start, int count) {
        ArrayList<LocalDate> dates = new ArrayList<>();
        LocalDate date = start;
        while (date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) date = date.plusDays(1);
        for (int i = 0; i < count; i++) dates.add(date.plusWeeks(i));
        return dates;
    }
}
