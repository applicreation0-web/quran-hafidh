package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Behavioral C12 proof: a structured session keeps its recorded scheduled date across restart/carryover. */
public final class HifzSessionDateInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";
    private static final String SCHEDULED_DATE_EXTRA = "scheduled_date";
    private Context context;

    @Rule public final TestWatcher resetHifzClock = new TestWatcher() {
        @Override protected void finished(Description description) { HifzClock.resetClockForTests(); }
    };

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-09-15T12:00:00Z"), ZoneId.of("UTC")));
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After public void tearDown() {
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void expiredEveningReviewRestartedAfterMidnightCommitsAndCreditsRecordedDate() {
        LocalDate sessionDay = LocalDate.of(2026, 9, 14);
        LocalDate restartDay = HifzClock.today();
        assertEquals(LocalDate.of(2026, 9, 15), restartDay);

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.completeSabqiBlock(0, 4, 5, sessionDay.toString(), "morning block"));
        prefs.setElapsedFor(HifzSessionActivity.SABQI_TODAY_REVIEW, 30L * 60_000L);

        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.SABQI_TODAY_REVIEW);
        try (ActivityScenario<HifzSessionActivity> ignored = ActivityScenario.launch(intent)) {
            HifzPrefs reopened = new HifzPrefs(context);
            assertEquals(sessionDay.toString(), reopened.lastSabqiTodayReviewDate());
            assertEquals(0L, reopened.elapsedFor(HifzSessionActivity.SABQI_TODAY_REVIEW));
        }

        ((QuranHifzApp) context).reconcilePreferenceChange("lastSabqiTodayReviewDate", restartDay);
        String lineId = GeometryRepository.get(context).line(0).id;
        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        assertEquals("J10 credit must keep the recorded session date", sessionDay, snapshot.get(lineId));
    }

    @Test public void carriedLearningSessionCommitsOriginalScheduledDateInsteadOfToday() {
        LocalDate scheduledDay = LocalDate.of(2026, 9, 14);
        assertEquals(LocalDate.of(2026, 9, 15), HifzClock.today());

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.setSabqiProgress(PreviewConfig.SABQI_TOTAL_REPS, 0));

        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.SABQI)
            .putExtra(SCHEDULED_DATE_EXTRA, scheduledDay.toString());
        try (ActivityScenario<HifzSessionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button validate = findButton(activity.findViewById(android.R.id.content), "Valider");
                assertNotNull("carryover learning must expose its normal validation action", validate);
                assertTrue(validate.performClick());
            });
        }

        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(
            "carryover completion must be attributed to the original scheduled day",
            scheduledDay.toString(),
            reopened.lastSabqiDate());
    }

    private static Button findButton(View root, String label) {
        if (root instanceof Button && label.contentEquals(((Button) root).getText())) return (Button) root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            Button found = findButton(group.getChildAt(i), label);
            if (found != null) return found;
        }
        return null;
    }
}
