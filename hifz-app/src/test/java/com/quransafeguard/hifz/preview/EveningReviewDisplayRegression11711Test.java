package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import static org.junit.Assert.*;

/** 06/11/2026 regression: Wednesday's earned snapshot cannot close Friday's review. */
public final class EveningReviewDisplayRegression11711Test {
    @Test public void mondayWednesdayFridayAwaitRealDailySabqi() {
        for(LocalDate date:new LocalDate[]{
                LocalDate.of(2026,11,2),LocalDate.of(2026,11,4),LocalDate.of(2026,11,6)}) {
            assertTrue(date.getDayOfWeek()==DayOfWeek.MONDAY
                ||date.getDayOfWeek()==DayOfWeek.WEDNESDAY
                ||date.getDayOfWeek()==DayOfWeek.FRIDAY);
            // Same source snapshot as Wednesday; Friday Sabqi is not started.
            EveningReviewDisplay.Status before=EveningReviewDisplay.forDay(false,false,false);
            assertEquals(EveningReviewDisplay.Status.WAITING_FOR_LESSON,before);
            assertEquals("Renforcement · en attente du Sabqi",
                EveningReviewDisplay.learningLabel(before));
            assertFalse(EveningReviewDisplay.resolved(before));
        }
    }

    @Test public void newFridayBlockReopensCumulativeEveningOnlyAfterSabqi() {
        assertEquals(EveningReviewDisplay.Status.TO_DO,
            EveningReviewDisplay.forDay(true,false,true));
        assertEquals("Renforcement · révision cumulative",
            EveningReviewDisplay.learningLabel(EveningReviewDisplay.forDay(true,false,true)));
        assertFalse(EveningReviewDisplay.resolved(
            EveningReviewDisplay.forDay(true,false,true)));
    }

    @Test public void noNewUnitAfterLessonIsNotAFalseCompletionReceipt() {
        EveningReviewDisplay.Status none=EveningReviewDisplay.forDay(true,false,false);
        assertEquals(EveningReviewDisplay.Status.NO_NEW_UNITS,none);
        assertEquals("Renforcement · aucune nouvelle unité",
            EveningReviewDisplay.learningLabel(none));
        assertFalse(EveningReviewDisplay.learningLabel(none).startsWith("✓"));
    }

    @Test public void onlyActualDatedReceiptShowsCompleted() {
        EveningReviewDisplay.Status earned=EveningReviewDisplay.forDay(true,true,false);
        assertEquals(EveningReviewDisplay.Status.COMPLETED,earned);
        assertEquals("✓ Renforcement",EveningReviewDisplay.learningLabel(earned));
        assertTrue(EveningReviewDisplay.resolved(earned));
        // Explicit receipt wins even if a delayed clock/cache reports morning incomplete.
        assertEquals(EveningReviewDisplay.Status.COMPLETED,
            EveningReviewDisplay.forDay(false,true,false));
    }

    @Test public void sameDayNewUnitOverridesAnEarlierCompletedReceipt() {
        // Example: a review was already performed on the old block, then a
        // Friday Sabqi or Saturday Itqan added a new physical block.
        EveningReviewDisplay.Status learning=EveningReviewDisplay.forDay(true,true,true);
        assertEquals(EveningReviewDisplay.Status.TO_DO,learning);
        assertEquals("Renforcement · révision cumulative",
            EveningReviewDisplay.learningLabel(learning));
        assertFalse(EveningReviewDisplay.resolved(learning));
        EveningReviewDisplay.Status preLesson=EveningReviewDisplay.forDay(false,true,true);
        assertEquals(EveningReviewDisplay.Status.WAITING_FOR_LESSON,preLesson);
        assertFalse(EveningReviewDisplay.resolved(preLesson));
        assertEquals("Consolidation · révision cumulative",
            EveningReviewDisplay.stabilizationLabel(learning,false));
    }

    @Test public void itqanUsesSameRealReceiptRuleWithoutChangingPostNasProtocol() {
        EveningReviewDisplay.Status before=EveningReviewDisplay.forDay(false,false,false);
        assertEquals("Consolidation · en attente de l’Itqān",
            EveningReviewDisplay.stabilizationLabel(before,false));
        EveningReviewDisplay.Status pending=EveningReviewDisplay.forDay(true,false,true);
        assertEquals("Consolidation · maintiens de la semaine · ×1 masqué",
            EveningReviewDisplay.stabilizationLabel(pending,true));
        assertEquals("✓ Consolidation",EveningReviewDisplay.stabilizationLabel(
            EveningReviewDisplay.forDay(true,true,false),false));
    }

    @Test public void todayWindowLabelsCannotCallWaitingCompleted() {
        assertTrue(EveningReviewDisplay.learningTodayLabel(
            EveningReviewDisplay.Status.WAITING_FOR_LESSON).contains("en attente"));
        assertTrue(EveningReviewDisplay.stabilizationTodayLabel(
            EveningReviewDisplay.Status.WAITING_FOR_LESSON).contains("en attente"));
        assertEquals("Renforcement · révision cumulative du soir",
            EveningReviewDisplay.learningTodayLabel(EveningReviewDisplay.Status.COMPLETED));
    }
}
