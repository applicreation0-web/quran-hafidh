package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10ReviewPolicyTest {
    @Test public void deadlineIsTenCalendarDaysAndHardDueStartsAtJ10() {
        LocalDate reviewed = LocalDate.of(2026, 9, 1);
        assertEquals(LocalDate.of(2026, 9, 11), J10ReviewPolicy.deadline(reviewed));
        assertEquals(9, J10ReviewPolicy.ageDays(reviewed, LocalDate.of(2026, 9, 10)));
        assertEquals(10, J10ReviewPolicy.ageDays(reviewed, LocalDate.of(2026, 9, 11)));
        assertEquals(0, J10ReviewPolicy.ageDays(reviewed, LocalDate.of(2026, 8, 31)));
    }

    @Test public void tenDayForecastCountsOnlyDeadlinesInsideTodayThroughDayNine() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        List<LocalDate> lastReviews = new ArrayList<>();
        lastReviews.add(today);                 // due D+10: outside this 10-day window
        lastReviews.add(today.minusDays(1));    // due D+9: inside
        lastReviews.add(today.minusDays(5));    // due D+5: inside

        J10ReviewPolicy.Forecast forecast = J10ReviewPolicy.forecast(lastReviews, today, 60.0, 10);
        assertEquals(2, forecast.requiredLines);
        assertEquals(2, forecast.requiredMinutes);
        assertEquals(10, forecast.availableMinutes);
        assertEquals(0, forecast.deficitMinutes);
        assertEquals(J10ReviewPolicy.Sustainability.NORMAL, forecast.sustainability);
    }

    @Test public void sustainabilityThresholdIsExactAndDeficitIsCeiled() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        List<LocalDate> seven = repeated(today.minusDays(1), 7);
        List<LocalDate> eight = repeated(today.minusDays(1), 8);
        List<LocalDate> eleven = repeated(today.minusDays(1), 11);

        assertEquals(J10ReviewPolicy.Sustainability.NORMAL,
            J10ReviewPolicy.forecast(seven, today, 60.0, 10).sustainability);
        assertEquals(J10ReviewPolicy.Sustainability.TENSION,
            J10ReviewPolicy.forecast(eight, today, 60.0, 10).sustainability);

        J10ReviewPolicy.Forecast overloaded = J10ReviewPolicy.forecast(eleven, today, 60.0, 10);
        assertEquals(J10ReviewPolicy.Sustainability.NON_TENABLE, overloaded.sustainability);
        assertEquals(1, overloaded.deficitMinutes);
    }

    @Test public void preemptionIsJ10ThenJ9WithJ8J7OnlyUnderPressure() {
        assertTrue(J10ReviewPolicy.shouldPreempt(10, J10ReviewPolicy.Sustainability.NORMAL));
        assertTrue(J10ReviewPolicy.shouldPreempt(9, J10ReviewPolicy.Sustainability.NORMAL));
        assertFalse(J10ReviewPolicy.shouldPreempt(8, J10ReviewPolicy.Sustainability.NORMAL));
        assertFalse(J10ReviewPolicy.shouldPreempt(7, J10ReviewPolicy.Sustainability.TENSION));
        assertTrue(J10ReviewPolicy.shouldPreempt(8, J10ReviewPolicy.Sustainability.TENSION));
        assertTrue(J10ReviewPolicy.shouldPreempt(8, J10ReviewPolicy.Sustainability.NON_TENABLE));
        assertTrue(J10ReviewPolicy.shouldPreempt(7, J10ReviewPolicy.Sustainability.NON_TENABLE));
        assertFalse(J10ReviewPolicy.shouldPreempt(6, J10ReviewPolicy.Sustainability.NON_TENABLE));
    }

    private static List<LocalDate> repeated(LocalDate date, int count) {
        ArrayList<LocalDate> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(date);
        return out;
    }
}
