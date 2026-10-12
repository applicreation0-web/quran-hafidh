package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Regressions from the second independent Claude audit of 0.7.5. */
public final class ClaudeRound2RegressionInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private GeometryRepository geometry;

    @Before public void setUp() {
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC));
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        HifzClock.resetClockForTests();
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void ownerlessStabilizationRangeDoesNotCrashHomeOrPoisonQueue() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit().putString("programStartDate", HifzClock.today().toString()).commit());
        assertTrue(CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            new VerseRef(53, 2), new VerseRef(53, 2), geometry).isEmpty());
        assertTrue(main.edit()
            .putString("promotedRanges", "[{\"start\":\"53:2\",\"end\":\"53:2\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"53:2\",\"end\":\"53:2\"}]")
            .putBoolean("anchoringQueueInitialized", false)
            .putString("anchoringQueue", "[]")
            .putInt("anchoringQueueIndex", 0)
            .commit());

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertTrue("Home must stay alive with an ownerless configured verse", awaitTodayEnabled(scenario));
        }
        HifzPrefs reloaded = new HifzPrefs(context);
        assertTrue("Ownerless verse must not remain in the Stabilisation queue", reloaded.anchoringQueue().isEmpty());
        assertTrue(reloaded.stabilizedConsolidationUnits(HifzClock.today()).isEmpty());
    }

    @Test public void completedConsolidationTodayStillListsScheduledCadence() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit().putString("programStartDate", HifzClock.today().toString()).commit());
        List<String> stabilized = new ArrayList<>();
        for (GeometryRepository.LineMeta line : pageLines(48)) stabilized.add(line.id);
        assertTrue(main.edit()
            .putString("anchoringQueue", "[{\"start\":\"2:282\",\"end\":\"2:282\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("v6StabilizedLineIds", json(stabilized))
            .putString("v6AcquiredCreditLineIds", "[]")
            .putString("v6LegacyPartialAcquiredLineIds", "[]")
            .putString("v6QuarantineLineIds", "[]")
            .putString("lastRecentSabqiReviewDate", HifzClock.today().toString())
            .putString("lastRecentSabqiReviewLabel", "Consolidation · séance validée")
            .commit());

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertTrue(awaitTodayEnabled(scenario));
            scenario.onActivity(activity -> {
                List<String> modes = todayModes(activity);
                assertTrue(modes.contains(HifzSessionActivity.SABQI));
                assertTrue("completed Consolidation must not reappear",
                    !modes.contains(HifzSessionActivity.RECENT_SABQI_REVIEW));
            });
        }
    }

    @Test public void rangesMergeAdjacentInsideSurahWithoutSplittingCrossSurahInput() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit()
            .putString("itqanRanges", "[{\"start\":\"53:1\",\"end\":\"53:30\"},{\"start\":\"53:31\",\"end\":\"53:62\"},{\"start\":\"54:1\",\"end\":\"54:55\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"49:1\",\"end\":\"114:6\"}]")
            .commit());

        List<VerseRange> acquired = prefs.itqanRanges();
        assertEquals(2, acquired.size());
        assertEquals(new VerseRef(53, 1), acquired.get(0).getStart());
        assertEquals(new VerseRef(53, 62), acquired.get(0).getEndInclusive());
        assertEquals(new VerseRef(54, 1), acquired.get(1).getStart());
        assertEquals(new VerseRef(54, 55), acquired.get(1).getEndInclusive());

        List<VerseRange> pending = prefs.unconsolidatedPromotedRanges();
        assertEquals("A stored cross-surah range must remain one range", 1, pending.size());
        assertEquals(new VerseRef(49, 1), pending.get(0).getStart());
        assertEquals(new VerseRef(114, 6), pending.get(0).getEndInclusive());
    }

    private boolean awaitTodayEnabled(ActivityScenario<MainActivity> scenario) throws Exception {
        boolean enabled = false;
        for (int attempt = 0; attempt < 100 && !enabled; attempt++) {
            AtomicBoolean ready = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                View action = findByContentDescription(activity.getWindow().getDecorView(), "Ouvrir la séance du jour");
                ready.set(action != null && action.isEnabled());
            });
            enabled = ready.get();
            if (!enabled) Thread.sleep(50L);
        }
        return enabled;
    }

    @SuppressWarnings("unchecked")
    private static List<String> todayModes(MainActivity activity) {
        try {
            Method method = MainActivity.class.getDeclaredMethod(
                "todaySessions", java.time.LocalDate.class, String.class, java.time.LocalDate.class);
            method.setAccessible(true);
            List<MainActivity.TodaySession> sessions = (List<MainActivity.TodaySession>) method.invoke(
                activity, HifzClock.today(), null, null);
            ArrayList<String> modes = new ArrayList<>();
            for (MainActivity.TodaySession session : sessions) modes.add(session.mode);
            return modes;
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private List<GeometryRepository.LineMeta> pageLines(int page) {
        ArrayList<GeometryRepository.LineMeta> out = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (line.page == page) out.add(line);
        }
        return out;
    }

    private static View findByContentDescription(View root, String wanted) {
        if (root == null) return null;
        CharSequence description = root.getContentDescription();
        if (description != null && wanted.contentEquals(description)) return root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findByContentDescription(group.getChildAt(i), wanted);
            if (found != null) return found;
        }
        return null;
    }

    private static String json(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array.toString();
    }
}
