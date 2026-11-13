package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public final class ActiveReviewSlotsInstrumentedTest {
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 15);
    private static final VerseRef START = new VerseRef(30, 35);
    private static final VerseRef END = new VerseRef(30, 37);
    private static final ActiveReviewSlots.Slot AM = ActiveReviewSlots.Slot.MORNING;
    private static final ActiveReviewSlots.Slot PM = ActiveReviewSlots.Slot.EVENING;

    @Test public void sundayClockAccumulatesOnlyWhileForegroundAndUnpaused() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original = HifzBackup.export(context);
        try {
            HifzClock.setClockForTests(Clock.fixed(
                SUNDAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
            HifzPrefs prefs = fixture(context);
            try (ActivityScenario<HifzSessionActivity> activity = open(context, AM)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Sunday morning review");
                SystemClock.sleep(1100L);
                assertTrue(elapsed(activity) >= 1000L);
                activity.onActivity(a -> {
                    View pause = field(a, "activeClockAction");
                    assertEquals("Pause", pause.getContentDescription());
                    assertTrue(pause.performClick());
                });
                long paused = elapsed(activity);
                SystemClock.sleep(1100L);
                assertEquals(paused, elapsed(activity));
                activity.recreate();
                assertEquals(paused, elapsed(activity));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Paused morning review");
                activity.onActivity(a -> {
                    View resume = field(a, "activeClockAction");
                    assertEquals("Reprendre", resume.getContentDescription());
                    assertTrue(resume.performClick());
                });
                activity.moveToState(Lifecycle.State.CREATED);
                long background = prefs.activeReviewSlots().draft(SUNDAY, AM).elapsedMs;
                SystemClock.sleep(1100L);
                assertEquals(background, prefs.activeReviewSlots().draft(SUNDAY, AM).elapsedMs);
                assertNull(prefs.activeReviewSlots().draft(SUNDAY, PM));
                activity.moveToState(Lifecycle.State.RESUMED);
                SystemClock.sleep(1100L);
                assertTrue(elapsed(activity) > background);
                assertNull(prefs.activeReviewSlots().receipt(SUNDAY, AM));
            }
            // A started, unfinished morning session must remain resumable;
            // opening evening must not silently reread or claim the same pages.
            long morningElapsed = prefs.activeReviewSlots().draft(SUNDAY, AM).elapsedMs;
            try (ActivityScenario<HifzSessionActivity> activity = open(context, PM)) {
                activity.onActivity(a -> assertTrue(
                    ((android.widget.TextView) field(a, "program")).getText()
                        .toString().contains("en attente")));
                assertNull(prefs.activeReviewSlots().draft(SUNDAY, PM));
                assertNull(prefs.activeReviewSlots().receipt(SUNDAY, PM));
                assertEquals(morningElapsed,
                    prefs.activeReviewSlots().draft(SUNDAY, AM).elapsedMs);
            }
            try (ActivityScenario<HifzSessionActivity> resumed = open(context, AM)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    resumed, "mushaf", "Unfinished morning resumes");
                assertTrue(elapsed(resumed) >= morningElapsed);
            }
        } finally {
            HifzClock.resetClockForTests();
            HifzBackup.restore(context, original);
        }
    }

    @Test public void realBackupRetainsMorningReceiptAndEveningCorrectiveCheckpoint() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original = HifzBackup.export(context);
        try {
            HifzClock.setClockForTests(Clock.fixed(
                SUNDAY.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
            HifzPrefs prefs = fixture(context);
            ActiveReviewSlots slots = prefs.activeReviewSlots();
            GeometryRepository geometry = GeometryRepository.get(context);
            int page = geometry.pageForVerse(START);
            VerseRef next = prefs.activeMurajaahCorpus().next(END);
            assertTrue(slots.checkpoint(SUNDAY, AM, START, END, page, ActiveReviewSlots.DURATION_MS));
            assertTrue(slots.complete(SUNDAY, AM, START, next, "Reçu du matin"));
            assertTrue(slots.checkpoint(SUNDAY, PM, next, next, page, ActiveReviewSlots.DURATION_MS));
            ActiveReviewCorrectionStore replay = slots.correction(SUNDAY, PM);
            assertTrue(replay.recordReveal(SUNDAY.toString(), next, page));
            assertTrue(replay.begin(SUNDAY.toString(), next, next, ActiveReviewSlots.DURATION_MS,
                Collections.singletonList(page), page));
            assertTrue(replay.pageReread(replay.pending()));
            assertTrue(replay.revealDuringControl(replay.pending()));
            JSONObject saved = HifzBackup.export(context);
            assertTrue(context.getSharedPreferences("quran_hifz_preview_v1", 0).edit().clear().commit());
            HifzBackup.restore(context, saved);

            HifzPrefs restored = new HifzPrefs(context);
            ActiveReviewSlots restoredSlots = restored.activeReviewSlots();
            assertEquals("Reçu du matin", restoredSlots.receipt(SUNDAY, AM));
            assertNull(restoredSlots.receipt(SUNDAY, PM));
            assertEquals(next, restored.activeMurajaahCursor());
            ActiveReviewCorrectionStore correction = restoredSlots.correction(SUNDAY, PM);
            assertEquals("RECALL", correction.pending().stage);
            assertTrue(correction.pending().assisted);
            assertFalse(correction.pageRecalled(correction.pending()));
            assertTrue(correction.restartPage(correction.pending()));
            assertTrue(correction.pageReread(correction.pending()));
            assertTrue(correction.pageRecalled(correction.pending()));
            assertTrue(restoredSlots.complete(SUNDAY, PM, next, restored.activeMurajaahCorpus().next(next),
                "Reçu du soir"));
            DashboardLedger ledger = new DashboardLedger(context);
            ledger.capture(restored, SUNDAY);
            assertEquals("Reçu du matin", ledger.find(SUNDAY, AM.mode).label);
            assertEquals("Reçu du soir", ledger.find(SUNDAY, PM.mode).label);
        } finally {
            HifzClock.resetClockForTests();
            HifzBackup.restore(context, original);
        }
    }

    @Test public void interruptedEveningFromYesterdayResumesOnTodayWithoutBackdatedCredit()
            throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        try {
            HifzClock.setClockForTests(Clock.fixed(
                SUNDAY.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
            HifzPrefs prefs=fixture(context);
            int page=GeometryRepository.get(context).pageForVerse(START);
            assertTrue(prefs.activeReviewSlots().checkpoint(SUNDAY, PM,
                START,END,page,600_000L,true,45));
            LocalDate monday=SUNDAY.plusDays(1);
            HifzClock.setClockForTests(Clock.fixed(
                monday.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
            try(ActivityScenario<HifzSessionActivity> activity=open(context,AM)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    activity,"mushaf","Resumed real active review after midnight");
                activity.onActivity(a->{
                    assertEquals(monday,field(a,"sessionDate"));
                    assertEquals(45,((ActiveReviewSlots.Draft)field(a,"activeDraft")).targetMinutes);
                    assertEquals(600_000L,((ActiveReviewSlots.Draft)field(a,"activeDraft")).elapsedMs);
                    assertFalse(((android.widget.TextView)field(a,"program")).getText()
                        .toString().contains("en attente"));
                });
                assertNull(prefs.activeReviewSlots().draft(SUNDAY,PM));
                assertNull(prefs.activeReviewSlots().receipt(SUNDAY,PM));
                assertEquals(600_000L,prefs.activeReviewSlots().draft(monday,AM).elapsedMs);
            }
            ActiveReviewSlots slots=new HifzPrefs(context).activeReviewSlots();
            assertTrue(slots.checkpoint(monday,AM,START,END,page,2_700_000L,true,45));
            VerseRef next=new HifzPrefs(context).activeMurajaahCorpus().next(END);
            ActiveReviewSlots.Draft proof=slots.draft(monday,AM);
            assertNotNull("Carried proof must survive Activity close",proof);
            assertEquals("Carried target duration must be frozen",45,proof.targetMinutes);
            assertEquals("Carried duration must be fully earned",2_700_000L,proof.elapsedMs);
            assertEquals("Carried endpoint must be present",END,proof.end);
            assertTrue("Current cursor/revision must match carried start: "
                +new HifzPrefs(context).activeMurajaahCursor(),
                slots.isCurrent(proof,new HifzPrefs(context).activeMurajaahCursor()));
            assertTrue("Carried slot must be open",slots.canOpen(monday,AM));
            ActiveReviewCorrectionStore correction=slots.correction(monday,AM);
            assertNull("Correction must not be claimed completed automatically",correction.pending());
            java.util.List<Integer> revealed=ActiveReviewCorrectionPlan.select(
                correction.revealed(monday.toString(),START));
            // Public CI lacks private Ayah Themes word geometry, so the app
            // safely displays its fallback page in clear. This is REAL help:
            // it must be logged, then explicitly reread and recalled masked.
            assertFalse("Unmasked fallback page must require correction",revealed.isEmpty());
            assertFalse("A revealed page cannot earn a receipt",slots.complete(
                monday,AM,START,next,"unearned"));
            assertTrue(correction.begin(monday.toString(),START,END,2_700_000L,revealed,page));
            while(!"DONE".equals(correction.pending().stage)) {
                ActiveReviewCorrectionStore.State state=correction.pending();
                if("READ".equals(state.stage))assertTrue(correction.pageReread(state));
                else assertTrue(correction.pageRecalled(state));
            }
            assertTrue("Completed visible+masked correction can earn TODAY only",
                slots.complete(monday,AM,START,next,"Reprise effectuée lundi"));
            assertNull(slots.receipt(SUNDAY,PM));
            assertEquals("Reprise effectuée lundi",slots.receipt(monday,AM));
        } finally {
            HifzClock.resetClockForTests();
            HifzBackup.restore(context,original);
        }
    }

    @Test public void openForegroundReviewCrossesMidnightAndKeepsItsExactCheckpoint()
            throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        try {
            HifzClock.setClockForTests(Clock.fixed(
                SUNDAY.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
            HifzPrefs prefs=fixture(context);
            try(ActivityScenario<HifzSessionActivity> activity=open(context,AM)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    activity,"mushaf","Before midnight active review");
                long before=elapsed(activity);
                LocalDate monday=SUNDAY.plusDays(1);
                HifzClock.setClockForTests(Clock.fixed(
                    monday.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
                activity.moveToState(Lifecycle.State.CREATED);
                activity.moveToState(Lifecycle.State.RESUMED);
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                    activity,"mushaf","After midnight active review");
                activity.onActivity(a->assertEquals(monday,field(a,"sessionDate")));
                assertNull(prefs.activeReviewSlots().draft(SUNDAY,AM));
                assertNull(prefs.activeReviewSlots().receipt(SUNDAY,AM));
                assertNotNull(prefs.activeReviewSlots().draft(monday,AM));
                assertTrue(elapsed(activity)>=before);
            }
        } finally {
            HifzClock.resetClockForTests();
            HifzBackup.restore(context,original);
        }
    }

    private static HifzPrefs fixture(Context context) {
        assertTrue(context.getSharedPreferences("quran_hifz_preview_v1", 0).edit().clear().commit());
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(
            new VerseRange(START, new VerseRef(30, 40))), GeometryRepository.get(context)));
        prefs.setActiveMurajaahCursor(START);
        return prefs;
    }

    private static ActivityScenario<HifzSessionActivity> open(Context context, ActiveReviewSlots.Slot slot) {
        return ActivityScenario.launch(new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, slot.mode));
    }

    private static long elapsed(ActivityScenario<HifzSessionActivity> activity) {
        AtomicLong result = new AtomicLong();
        activity.onActivity(a -> result.set(((SessionClock) field(a, "clock")).elapsedMs()));
        return result.get();
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(HifzSessionActivity activity, String name) {
        try {
            Field field = HifzSessionActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(activity);
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError(missing);
        }
    }
}
