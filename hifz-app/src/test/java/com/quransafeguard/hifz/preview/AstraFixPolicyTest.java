package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AstraFixPolicyTest {
    @Test public void j10ForecastFailsAtFirstMissedDeadlineNotEndOfWindow() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        List<LocalDate> reviews = new ArrayList<>();
        for (int i = 0; i < 1200; i++) reviews.add(today.minusDays(10));
        int[] dailyCapacityMinutes = {75, 75, 75, 75, 75, 75, 75, 75, 75, 75};

        J10ReviewPolicy.Forecast forecast = J10ReviewPolicy.forecastByDay(
            reviews, today, 9.0, dailyCapacityMinutes);

        assertEquals(J10ReviewPolicy.Sustainability.NON_TENABLE, forecast.sustainability);
        assertEquals(180, forecast.requiredMinutes);
        assertEquals(75, forecast.availableMinutes);
        assertEquals(105, forecast.deficitMinutes);
    }

    @Test public void unknownHistoricalAcquisitionStartsDueNotAtJ0() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        assertEquals(today.minusDays(10), J10ReviewPolicy.unknownHistoricalSeed(today));
    }

    @Test public void j10CannotValidateUntilEveryRequiredPageWasShown() {
        J10ReviewProgress progress = new J10ReviewProgress(120, 122);
        assertFalse(progress.canValidate());
        progress.markShown(120);
        progress.markShown(121);
        assertFalse(progress.canValidate());
        progress.markShown(122);
        assertTrue(progress.canValidate());
    }

    @Test public void j10ElapsedTimeConsumesHostSessionBudget() {
        assertEquals(150_000L, J10SessionBudget.addConsumed(30_000L, 120_000L));
        assertEquals(30_000L, J10SessionBudget.addConsumed(30_000L, -1L));
    }

    @Test public void lateConsolidationAttendanceCanEventuallyPromote() {
        LocalDate added = LocalDate.of(2026, 1, 1);
        LocalDate through = LocalDate.of(2026, 6, 30);
        List<LocalDate> completed = new ArrayList<>();
        LocalDate d = added.plusDays(91);
        while (d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) d = d.plusDays(1);
        for (int i = 0; i < 17; i++) {
            completed.add(d);
            d = d.plusWeeks(1);
        }

        RecentPromotionPolicy.Decision decision = RecentPromotionPolicy.evaluate(
            added, through, added, completed, 20, true);

        assertTrue("valid later Sundays must recover attendance debt", decision.promote);
        assertTrue(decision.completedSessions >= decision.requiredSessions);
    }

    @Test public void newPromotionIsInsertedBeforePendingReconstruction() {
        AnchoringQueue.Entry reconstruction = new AnchoringQueue.Entry(
            "49:1", "49:5", AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.Protocol.LIGHT, 0);
        AnchoringQueue.Entry promoted = new AnchoringQueue.Entry(
            "2:75", "2:82", AnchoringQueue.Origin.PROMOTED, AnchoringQueue.Protocol.FULL, 0);

        List<AnchoringQueue.Entry> merged = AnchoringQueue.mergeWithPromotionPriority(
            Collections.singletonList(reconstruction), 0, false, Collections.singletonList(promoted));

        assertEquals(2, merged.size());
        assertEquals(AnchoringQueue.Origin.PROMOTED, merged.get(0).origin);
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, merged.get(1).origin);
    }

    @Test public void inProgressFractionatedEntryIsNeverPreemptedByNewPromotion() {
        AnchoringQueue.Entry current = new AnchoringQueue.Entry(
            "49:1", "49:5", AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.Protocol.LIGHT, 0);
        AnchoringQueue.Entry promoted = new AnchoringQueue.Entry(
            "2:75", "2:82", AnchoringQueue.Origin.PROMOTED, AnchoringQueue.Protocol.FULL, 0);

        List<AnchoringQueue.Entry> merged = AnchoringQueue.mergeWithPromotionPriority(
            Collections.singletonList(current), 0, true, Arrays.asList(promoted));

        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, merged.get(0).origin);
        assertEquals(AnchoringQueue.Origin.PROMOTED, merged.get(1).origin);
    }
}
