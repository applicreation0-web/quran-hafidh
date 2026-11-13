package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import org.json.JSONObject;

import static org.junit.Assert.*;

public final class ActiveReviewSlotsTest {
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 15);
    private static final VerseRef FIRST = new VerseRef(2, 1);
    private static final VerseRef END = new VerseRef(2, 2);
    private static final VerseRef NEXT = new VerseRef(2, 3);
    private final InMemoryPrefs disk = new InMemoryPrefs();
    private final ActiveReviewSlots store = new ActiveReviewSlots(disk);

    @Before public void sundayClock() { setDay(SUNDAY); }

    @After public void restoreClock() { HifzClock.resetClockForTests(); }

    private static void setDay(LocalDate day) {
        HifzClock.setClockForTests(java.time.Clock.fixed(
            day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),java.time.ZoneOffset.UTC));
    }

    @Test public void sundayMorningAndEveningEachRequireThirtyMinutesAndUseTheSameCursor() {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_799_999L));
        assertFalse(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertFalse(store.complete(SUNDAY, morning, FIRST, NEXT, "doublon"));
        assertEquals("matin", store.receipt(SUNDAY, morning));
        assertEquals(NEXT.toString(), disk.getString("activeMurajaahCursor", ""));
        assertNull(store.receipt(SUNDAY, evening));
        assertNull(store.draft(SUNDAY, evening));
        assertTrue(store.checkpoint(SUNDAY, evening, NEXT, NEXT, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, evening, NEXT, FIRST, "soir"));
        assertEquals("soir", store.receipt(SUNDAY, evening));
        assertEquals(FIRST.toString(), disk.getString("activeMurajaahCursor", ""));
        assertNull(store.receipt(SUNDAY.plusDays(1), morning));
        assertNull(store.draft(SUNDAY.plusDays(1), evening));
    }

    @Test public void unfinishedMorningResumesAndEveningStartsOnlyAfterMorningReceipt() {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 650_000L));
        assertFalse(store.canOpen(SUNDAY, evening));
        assertFalse(store.checkpoint(SUNDAY, evening, FIRST, NEXT, 3, 171_000L, true));
        ActiveReviewSlots resumed = new ActiveReviewSlots(disk);
        assertEquals(650_000L, resumed.draft(SUNDAY, morning).elapsedMs);
        assertNull(resumed.draft(SUNDAY, evening));
        assertTrue(resumed.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        assertTrue(resumed.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertTrue(resumed.canOpen(SUNDAY, evening));
        assertTrue(resumed.checkpoint(SUNDAY, evening, NEXT, NEXT, 3, 171_000L, true));
        ActiveReviewSlots restarted = new ActiveReviewSlots(disk);
        assertEquals(171_000L, restarted.draft(SUNDAY, evening).elapsedMs);
        assertEquals(NEXT, restarted.draft(SUNDAY, evening).start);
        assertEquals(NEXT, restarted.draft(SUNDAY, evening).end);
        assertEquals(3, restarted.draft(SUNDAY, evening).page);
        assertTrue(restarted.draft(SUNDAY, evening).paused);
        assertTrue(restarted.checkpoint(SUNDAY, evening, NEXT, NEXT, 3, 100_000L));
        assertEquals(171_000L, restarted.draft(SUNDAY, evening).elapsedMs);
    }

    @Test public void reopenedEveningCorrectionMustFinishBeforeTheReceipt() {
        ActiveReviewSlots.Slot slot = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, slot, FIRST, END, 2, 1_800_100L));
        ActiveReviewCorrectionStore evening = store.correction(SUNDAY, slot);
        assertTrue(evening.recordReveal(SUNDAY.toString(), FIRST, 2));
        assertFalse(store.complete(SUNDAY, slot, FIRST, NEXT, "soir"));
        assertTrue(evening.begin(SUNDAY.toString(), FIRST, END, 1_800_000L,
            Collections.singletonList(2), 2));
        assertTrue(evening.pageReread(evening.pending()));

        ActiveReviewSlots restored = new ActiveReviewSlots(disk);
        ActiveReviewCorrectionStore replay = restored.correction(SUNDAY, slot);
        assertEquals("RECALL", replay.pending().stage);
        assertNull(restored.correction(SUNDAY, ActiveReviewSlots.Slot.MORNING).pending());
        assertTrue(replay.revealDuringControl(replay.pending()));
        assertFalse(replay.pageRecalled(replay.pending()));
        assertTrue(replay.restartPage(replay.pending()));
        assertTrue(replay.pageReread(replay.pending()));
        assertTrue(replay.pageRecalled(replay.pending()));
        assertEquals(SUNDAY, restored.dateToResume(SUNDAY.plusDays(1), slot));
        assertEquals(SUNDAY.plusDays(1), restored.dateToResume(SUNDAY.plusDays(1),
            ActiveReviewSlots.Slot.MORNING));
        assertTrue(restored.complete(SUNDAY, slot, FIRST, NEXT, "soir"));
        assertEquals("soir", restored.receipt(SUNDAY, slot));
        assertNull(replay.pending());
    }

    @Test public void correctionRemainsAvailableAfterMidnightWithoutBackdating() {
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, evening, FIRST, END, 2, ActiveReviewSlots.DURATION_MS));
        ActiveReviewCorrectionStore replay = store.correction(SUNDAY, evening);
        assertTrue(replay.recordReveal(SUNDAY.toString(), FIRST, 2));
        assertTrue(replay.begin(SUNDAY.toString(), FIRST, END, ActiveReviewSlots.DURATION_MS,
            Collections.singletonList(2), 2));
        LocalDate monday=SUNDAY.plusDays(1);
        setDay(monday);
        assertEquals(SUNDAY,store.dateToResume(monday,evening));
        assertTrue(replay.pageReread(replay.pending()));
        assertTrue(replay.pageRecalled(replay.pending()));
        assertFalse(store.complete(SUNDAY,evening,FIRST,NEXT,"soir"));
        assertTrue(store.finishCorrectionWithoutReceipt(SUNDAY,evening));
        assertNull(store.receipt(SUNDAY,evening));
        assertNull(store.receipt(monday,evening));
        assertEquals("",disk.getString("activeMurajaahCursor",""));
        assertEquals(monday,store.dateToResume(monday,evening));
    }

    @Test public void revealBeforeTimeLimitIsRecoveredAsCorrectionOnlyAfterMidnight() {
        ActiveReviewSlots.Slot slot=ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY,slot,FIRST,null,2,540_000L));
        ActiveReviewCorrectionStore replay=store.correction(SUNDAY,slot);
        assertTrue(replay.recordReveal(SUNDAY.toString(),FIRST,2));
        setDay(SUNDAY.plusDays(1));
        assertEquals(SUNDAY,store.dateToResume(HifzClock.today(),slot));
        assertTrue(store.restoreExpiredRevealCorrection(SUNDAY,slot));
        assertEquals(540_000L,replay.pending().activeElapsedMs);
        assertTrue(replay.pageReread(replay.pending()));
        assertTrue(replay.pageRecalled(replay.pending()));
        assertTrue(store.finishCorrectionWithoutReceipt(SUNDAY,slot));
        assertNull(store.receipt(SUNDAY,slot));
        assertNull(store.draft(SUNDAY,slot));
    }

    @Test public void legacyOverlappingDraftCannotIssueDuplicateReceipt() throws Exception {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        // Simulate two drafts left by the prior version, bypassing the new gate.
        String legacy = new JSONObject().put("start", FIRST.toString())
            .put("end", FIRST.toString()).put("page", 2)
            .put("elapsedMs", 1_800_000L).put("revision", 0L)
            .put("targetMinutes", 30).put("paused", true).toString();
        disk.disk.put("activeReviewDraftV1|" + SUNDAY + "|EVENING", legacy);
        assertTrue(store.canOpen(SUNDAY, morning));
        assertFalse(store.canOpen(SUNDAY, evening));
        assertTrue(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertFalse(store.complete(SUNDAY, evening, FIRST, END, "doublon"));
        assertNull(store.receipt(SUNDAY, evening));
        assertNotNull(store.draft(SUNDAY, evening));
        assertEquals(NEXT.toString(), disk.getString("activeMurajaahCursor", ""));
    }

    @Test public void unfinishedCorrectionCannotBeDuplicatedByOtherSlot() {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        ActiveReviewCorrectionStore replay = store.correction(SUNDAY, morning);
        assertTrue(replay.recordReveal(SUNDAY.toString(), FIRST, 2));
        assertTrue(replay.begin(SUNDAY.toString(), FIRST, END, 1_800_000L,
            Collections.singletonList(2), 2));
        assertFalse(store.checkpoint(SUNDAY, evening, FIRST, END, 2, 1_800_000L));
        assertNull(store.draft(SUNDAY, evening));
        assertNull(store.receipt(SUNDAY, evening));
        assertTrue(replay.pageReread(replay.pending()));
        assertTrue(replay.pageRecalled(replay.pending()));
        assertTrue(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertTrue(store.checkpoint(SUNDAY, evening, NEXT, NEXT, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, evening, NEXT, FIRST, "soir"));
        assertEquals("matin", store.receipt(SUNDAY, morning));
        assertEquals("soir", store.receipt(SUNDAY, evening));
        assertNull(replay.pending());
        assertEquals(FIRST.toString(), disk.getString("activeMurajaahCursor", ""));
    }

    @Test public void delayedEveningStartCannotReopenMorningContent() {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertFalse(store.checkpoint(SUNDAY, evening, FIRST, END, 2, 1_800_000L));
        assertNull(store.draft(SUNDAY, evening));
        assertTrue(store.checkpoint(SUNDAY, evening, NEXT, NEXT, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, evening, NEXT, FIRST, "soir"));
        assertEquals("soir", store.receipt(SUNDAY, evening));
    }

    @Test public void skippedMorningDoesNotBlockEvening() {
        ActiveReviewSlots.Slot morning = ActiveReviewSlots.Slot.MORNING;
        ActiveReviewSlots.Slot evening = ActiveReviewSlots.Slot.EVENING;
        assertTrue(store.checkpoint(SUNDAY, evening, FIRST, END, 2, 1_800_000L));
        assertFalse(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, evening, FIRST, NEXT, "soir"));
        assertTrue(store.checkpoint(SUNDAY, morning, NEXT, NEXT, 2, 1_800_000L));
        assertTrue(store.complete(SUNDAY, morning, NEXT, FIRST, "matin"));
        assertEquals("soir", store.receipt(SUNDAY, evening));
        assertEquals("matin", store.receipt(SUNDAY, morning));
    }

    @Test public void unstartedMissedDayAddsNoMinutesAndNeverMovesTheCursor() {
        LocalDate monday=SUNDAY.plusDays(1);
        setDay(monday);
        assertFalse(store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.MORNING));
        assertTrue(store.checkpoint(monday,ActiveReviewSlots.Slot.MORNING,
            FIRST,END,2,ActiveReviewSlots.DURATION_MS));
        assertTrue(store.complete(monday,ActiveReviewSlots.Slot.MORNING,FIRST,NEXT,"today"));
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.MORNING));
        assertEquals("today",store.receipt(monday,ActiveReviewSlots.Slot.MORNING));
        assertEquals(NEXT.toString(),disk.getString("activeMurajaahCursor",""));
    }

    @Test public void yesterdayEveningCanFinishAsTodaysMorningWithoutBackdatedReceipt() {
        LocalDate monday=SUNDAY.plusDays(1);
        assertTrue(store.checkpoint(SUNDAY,ActiveReviewSlots.Slot.EVENING,
            FIRST,END,2,600_000L,true,45));
        setDay(monday);
        assertFalse(store.checkpoint(monday,ActiveReviewSlots.Slot.MORNING,FIRST,END,2,1L));
        assertTrue(store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertNull(store.draft(SUNDAY,ActiveReviewSlots.Slot.EVENING));
        ActiveReviewSlots.Draft carried=store.draft(monday,ActiveReviewSlots.Slot.MORNING);
        assertEquals(FIRST,carried.start);
        assertEquals(END,carried.end);
        assertEquals(600_000L,carried.elapsedMs);
        assertTrue(carried.paused);
        assertEquals(45,carried.targetMinutes);
        assertFalse(store.complete(monday,ActiveReviewSlots.Slot.MORNING,FIRST,NEXT,"premature"));
        assertTrue(store.checkpoint(monday,ActiveReviewSlots.Slot.MORNING,
            FIRST,END,2,2_700_000L,false,60));
        assertTrue(store.complete(monday,ActiveReviewSlots.Slot.MORNING,FIRST,NEXT,"today"));
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.EVENING));
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.MORNING));
        assertEquals("today",store.receipt(monday,ActiveReviewSlots.Slot.MORNING));
        assertEquals(45,store.targetMinutes(monday,ActiveReviewSlots.Slot.MORNING,90));
        assertEquals(NEXT.toString(),disk.getString("activeMurajaahCursor",""));
    }

    @Test public void revealedPageJournalMovesAtomicallyAndMustBeRecalled() {
        LocalDate monday=SUNDAY.plusDays(1);
        assertTrue(store.checkpoint(SUNDAY,ActiveReviewSlots.Slot.EVENING,
            FIRST,END,2,1_800_000L,true));
        ActiveReviewCorrectionStore prior=store.correction(SUNDAY,ActiveReviewSlots.Slot.EVENING);
        assertTrue(prior.recordReveal(SUNDAY.toString(),FIRST,2));
        assertTrue(prior.begin(SUNDAY.toString(),FIRST,END,1_800_000L,java.util.List.of(2),2));
        assertTrue(prior.pageReread(prior.pending()));
        setDay(monday);
        assertTrue(store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertNull(prior.pending());
        assertTrue(prior.revealed(SUNDAY.toString(),FIRST).isEmpty());
        ActiveReviewCorrectionStore carried=store.correction(monday,ActiveReviewSlots.Slot.MORNING);
        assertEquals("RECALL",carried.pending().stage);
        assertEquals(monday.toString(),carried.pending().date);
        assertEquals(java.util.Set.of(2),carried.revealed(monday.toString(),FIRST));
        assertFalse(store.complete(monday,ActiveReviewSlots.Slot.MORNING,FIRST,NEXT,"no replay"));
        assertTrue(carried.pageRecalled(carried.pending()));
        assertTrue(store.complete(monday,ActiveReviewSlots.Slot.MORNING,FIRST,NEXT,"earned now"));
        assertNull(carried.pending());
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.EVENING));
        assertEquals("earned now",store.receipt(monday,ActiveReviewSlots.Slot.MORNING));
        assertEquals(NEXT.toString(),disk.getString("activeMurajaahCursor",""));
    }

    @Test public void staleOldCursorNeverRollsIntoTodayOrMintsReceipt() {
        LocalDate monday=SUNDAY.plusDays(1);
        assertTrue(store.checkpoint(SUNDAY,ActiveReviewSlots.Slot.EVENING,FIRST,END,2,120_000L));
        disk.disk.put("activeMurajaahCursor",NEXT.toString());
        setDay(monday);
        java.util.Map<String,?> old=java.util.Map.copyOf(disk.getAll());
        assertFalse(store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertEquals(old,disk.getAll());
        assertNull(store.receipt(SUNDAY,ActiveReviewSlots.Slot.EVENING));
        assertNull(store.receipt(monday,ActiveReviewSlots.Slot.MORNING));
    }

    @Test public void todaysExistingDraftCannotBeReplacedByYesterday() {
        LocalDate monday=SUNDAY.plusDays(1);
        assertTrue(store.checkpoint(SUNDAY,ActiveReviewSlots.Slot.EVENING,FIRST,END,2,600_000L));
        setDay(monday);
        // Manually imported preexisting today checkpoint must win, never be overwritten.
        String frozen=disk.getString("activeReviewDraftV1|"+SUNDAY+"|EVENING","");
        disk.disk.put("activeReviewDraftV1|"+monday+"|MORNING",frozen);
        assertFalse(store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertNotNull(store.draft(SUNDAY,ActiveReviewSlots.Slot.EVENING));
        assertNotNull(store.draft(monday,ActiveReviewSlots.Slot.MORNING));
        assertNull(store.receipt(monday,ActiveReviewSlots.Slot.MORNING));
    }

    @Test public void malformedOldCorrectionRefusesCarryWithoutErasingAnyBytes() {
        LocalDate monday=SUNDAY.plusDays(1);
        assertTrue(store.checkpoint(SUNDAY,ActiveReviewSlots.Slot.EVENING,FIRST,END,2,600_000L));
        String corrupted="{\\"date\\":\\"2026-11-15\\",\\"start\\":\\"2:1\\",\\"pages\\":[999]}";
        disk.disk.put("activeReviewRevealedPagesV1|"+SUNDAY+"|EVENING",corrupted);
        setDay(monday);
        java.util.Map<String,?> before=java.util.Map.copyOf(disk.getAll());
        assertThrows(IllegalStateException.class,()->store.carryForward(monday,ActiveReviewSlots.Slot.MORNING));
        assertEquals(before,disk.getAll());
    }

    @Test public void durationIsFrozenPerSlotAndEachReceiptKeepsItsEarnedTarget() {
        var morning = ActiveReviewSlots.Slot.MORNING;
        var evening = ActiveReviewSlots.Slot.EVENING;
        int minutes45 = MaintenanceCoveragePolicy.minutes(4_410, 8_820, 8);
        int minutes60 = MaintenanceCoveragePolicy.minutes(5_000, 8_820, 10);
        assertEquals(30, MaintenanceCoveragePolicy.minutes(4_409, 8_820, 10));
        assertEquals(45, minutes45);
        assertEquals(60, minutes60);
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 1_800_000L, false, minutes45));
        assertFalse(store.complete(SUNDAY, morning, FIRST, NEXT, "too early"));
        assertTrue(store.checkpoint(SUNDAY, morning, FIRST, END, 2, 2_700_000L, false, minutes60));
        assertEquals(45, new ActiveReviewSlots(disk).draft(SUNDAY, morning).targetMinutes);
        assertTrue(store.complete(SUNDAY, morning, FIRST, NEXT, "matin"));
        assertTrue(store.checkpoint(SUNDAY, evening, NEXT, NEXT, 2, 3_600_000L, false, minutes60));
        var correction = store.correction(SUNDAY, evening);
        assertTrue(correction.recordReveal(SUNDAY.toString(), NEXT, 2));
        assertTrue(correction.begin(SUNDAY.toString(), NEXT, NEXT, 3_600_000L, java.util.List.of(2), 2));
        assertFalse(store.complete(SUNDAY, evening, NEXT, FIRST, "uncorrected"));
        assertTrue(correction.pageReread(correction.pending()));
        assertTrue(correction.pageRecalled(correction.pending()));
        assertTrue(store.complete(SUNDAY, evening, NEXT, FIRST, "soir"));
        assertEquals(45, store.targetMinutes(SUNDAY, morning, 60));
        assertEquals(60, store.targetMinutes(SUNDAY, evening, 30));
    }
}
