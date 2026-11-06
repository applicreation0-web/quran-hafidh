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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Regression: a missed lesson stays unearned; its physical lines are done on
 * the next eligible weekday with the NEW real attendance date, not the old one. */
public final class HifzCarryoverDateInstrumentedTest {
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
    }

    @After public void tearDown() {
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("quran_hifz_dashboard_ledger_v1", Context.MODE_PRIVATE)
            .edit().clear().commit();
    }

    @Test public void missedMondayCannotBeBackdatedByOldIntentOnTuesday() {
        LocalDate missed = LocalDate.of(2026,9,14);
        assertEquals(LocalDate.of(2026,9,15),HifzClock.today());
        HifzPrefs prefs=new HifzPrefs(context);
        assertTrue(context.getSharedPreferences(HIFZ_PREFS,Context.MODE_PRIVATE)
            .edit().putString("programStartDate",missed.toString()).commit());
        assertTrue(context.getSharedPreferences("quran_hifz_dashboard_ledger_v1",Context.MODE_PRIVATE)
            .edit().putString("coverage_start_date_v1",missed.toString())
            .putString("started",missed.toString()).commit());
        DashboardLedger ledger=new DashboardLedger(context);
        ledger.capture(prefs);
        HifzBacklog.Due absent=HifzBacklog.first(HifzBacklog.pending(
            prefs,ledger,HifzClock.today(),prefs.learningDaysPerWeek()));
        assertNotNull(absent);
        assertEquals(missed,absent.date);
        assertEquals(HifzSessionActivity.SABQI,absent.mode);

        // Even fully recorded repetitions cannot be retroactively validated by
        // a stale direct intent; no phantom missed-Monday receipt is created.
        assertTrue(prefs.setSabqiProgress(PreviewConfig.SABQI_TOTAL_REPS,0));
        Intent old=new Intent(context,HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.SABQI)
            .putExtra(SCHEDULED_DATE_EXTRA,missed.toString());
        try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(old)){
            scenario.onActivity(activity -> assertNull(
                findButtonByDescription(activity.findViewById(android.R.id.content),"Valider")));
        }
        assertEquals("",new HifzPrefs(context).lastSabqiDate());
    }

    @Test public void nextWednesdaySabqiValidatesRealWednesdayNotMissedMonday() {
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-09-16T12:00:00Z"),ZoneId.of("UTC")));
        LocalDate missed=LocalDate.of(2026,9,14),today=HifzClock.today();
        HifzPrefs prefs=new HifzPrefs(context);
        assertTrue(context.getSharedPreferences(HIFZ_PREFS,Context.MODE_PRIVATE)
            .edit().putString("programStartDate",missed.toString()).commit());
        assertTrue(context.getSharedPreferences("quran_hifz_dashboard_ledger_v1",Context.MODE_PRIVATE)
            .edit().putString("coverage_start_date_v1",missed.toString())
            .putString("started",missed.toString()).commit());
        assertTrue(prefs.setWeeklyProgramMode(HifzPrefs.WeeklyProgram.RECOMMENDED));
        assertTrue(prefs.setSabqiProgress(PreviewConfig.SABQI_TOTAL_REPS,0));
        Intent intent=new Intent(context,HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.SABQI)
            .putExtra(SCHEDULED_DATE_EXTRA,today.toString());
        try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity -> {
                Button validate=findButtonByDescription(
                    activity.findViewById(android.R.id.content),"Valider");
                assertNotNull("Wednesday must offer the still-unread Monday lines",validate);
                assertTrue(validate.performClick());
            });
        }
        assertEquals("Completion must be dated by the actual eligible slot",
            today.toString(),new HifzPrefs(context).lastSabqiDate());
        assertEquals("Past absence must never be credited",missed.plusDays(2),today);
    }

    private static Button findButtonByDescription(View root, String description) {
        if (root instanceof Button
                && description.contentEquals(root.getContentDescription())) return (Button) root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            Button found = findButtonByDescription(group.getChildAt(i), description);
            if (found != null) return found;
        }
        return null;
    }
}
