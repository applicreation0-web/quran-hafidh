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
            try (ActivityScenario<HifzSessionActivity> activity = open(context, PM)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Sunday evening review");
                assertTrue(elapsed(activity) < prefs.activeReviewSlots().draft(SUNDAY, AM).elapsedMs);
                activity.onActivity(a -> assertFalse(
                    ((View) field(a, "murajaahFinishButton")).isEnabled()));
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
