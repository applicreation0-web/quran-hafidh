package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.CadenceAction;
import org.junit.Test;
import org.junit.After;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class ActiveReviewIndependenceTest {
    @After public void clearClock(){HifzClock.resetClockForTests();}
    private void today(LocalDate date){
        HifzClock.setClockForTests(java.time.Clock.fixed(
            date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),java.time.ZoneOffset.UTC));
    }

    @Test public void interruptionAfterRealRecallNeverReservesTheCursorOrDropsReveals(){
        LocalDate day=LocalDate.of(2026,11,26);today(day);
        InMemoryPrefs disk=new InMemoryPrefs();
        ActiveReviewSlots slots=new ActiveReviewSlots(disk);
        ActiveReviewSlots.Slot am=ActiveReviewSlots.Slot.MORNING,pm=ActiveReviewSlots.Slot.EVENING;
        VerseRef start=new VerseRef(2,1),end=new VerseRef(2,2),next=new VerseRef(2,3);
        assertTrue(slots.checkpoint(day,am,start,end,2,2_400_000L,false,40));
        ActiveReviewCorrectionStore correction=slots.correction(day,am);
        assertTrue(correction.recordReveal(day.toString(),start,2));
        assertTrue(correction.begin(day.toString(),start,end,2_400_000L,Arrays.asList(2,3),2));
        assertTrue(slots.completeRecall(day,am,start,next,"preuve"));
        slots=new ActiveReviewSlots(disk);
        assertEquals("preuve",slots.receipt(day,am));
        assertNull(slots.draft(day,am));
        assertEquals(next.toString(),disk.getString("activeMurajaahCursor",""));
        assertEquals(Arrays.asList(2,3),correction.queuedPages());
        assertTrue(slots.checkpoint(day,pm,next,next,3,0,false,40));
        assertTrue(correction.deferPage(correction.pending()));
        assertEquals(3,correction.pending().page());
        assertTrue(correction.pageReread(correction.pending()));
        assertTrue(correction.pageRecalled(correction.pending()));
        assertTrue(slots.finishIndependentCorrection(day,am,correction.pending()));
        assertEquals(Collections.singletonList(2),correction.queuedPages());
        assertEquals(next.toString(),disk.getString("activeMurajaahCursor",""));
        assertFalse(slots.completeRecall(day,am,start,next,"double"));
    }

    @Test public void futureSundayRestDoesNotRetroactivelyAlterOldSundayOrExistingDraft(){
        LocalDate sunday=LocalDate.of(2026,11,29);today(sunday);
        ActiveReviewSlots slots=new ActiveReviewSlots(new InMemoryPrefs());
        slots.ensureProgramme(sunday.minusDays(2));
        assertTrue(slots.scheduled(sunday.minusWeeks(1),ActiveReviewSlots.Slot.EVENING));
        assertFalse(slots.scheduled(sunday,ActiveReviewSlots.Slot.EVENING));
        assertTrue(slots.scheduled(sunday,ActiveReviewSlots.Slot.MORNING));
        WeeklyBilan.Summary result=WeeklyBilan.calculate(sunday,sunday,sunday,sunday,
            day->CadenceAction.REVISION,(day,mode)->ActiveReviewSlots.Slot.MORNING.mode.equals(mode),
            day->slots.scheduled(day,ActiveReviewSlots.Slot.EVENING));
        assertEquals(1,result.planned);assertEquals(1,result.validated);assertEquals(0,result.dueToday);
    }

    @Test public void standaloneCorrectionCannotCreateAnActiveReceipt(){
        InMemoryPrefs disk=new InMemoryPrefs();
        ActiveReviewSlots slots=new ActiveReviewSlots(disk);
        ActiveReviewCorrectionStore store=slots.manualCorrection();
        VerseRef anchor=new VerseRef(2,1);
        assertTrue(store.begin("2026-11-26",anchor,anchor,2_400_000L,Collections.singletonList(5),5));
        assertTrue(store.pageReread(store.pending()));
        assertTrue(store.pageRecalled(store.pending()));
        assertTrue(store.finishIndependent(store.pending()));
        assertTrue(slots.receipts().isEmpty());
        assertFalse(disk.contains("activeMurajaahCursor"));
    }
}
