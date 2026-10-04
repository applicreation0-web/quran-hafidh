package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Device-level regressions for the Claude 0.7.5 independent audit. */
public final class ClaudeNoGoRegressionInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private SharedPreferences legacyJ10;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        legacyJ10 = context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE);
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
    }

    @Test public void homeTodayOpensProgressionTriggeredConsolidation() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        prefs.setProgramStartDate(HifzClock.today());
        List<GeometryRepository.LineMeta> page = pageLines(48);
        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(page);
        assertEquals(2, units.size());
        List<String> stabilized = units.get(0).lineIds;

        assertTrue(main.edit()
            .putString("anchoringQueue", "[{\"start\":\"2:282\",\"end\":\"2:282\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("v6StabilizedLineIds", json(stabilized))
            .putString("v6AcquiredCreditLineIds", "[]")
            .putString("v6LegacyPartialAcquiredLineIds", "[]")
            .putString("v6QuarantineLineIds", "[]")
            .commit());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
            HifzSessionActivity.class.getName(), null, false);
        Activity opened = null;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            boolean enabled = false;
            for (int attempt = 0; attempt < 100 && !enabled; attempt++) {
                AtomicBoolean ready = new AtomicBoolean(false);
                scenario.onActivity(activity -> {
                    View action = findByContentDescription(
                        activity.getWindow().getDecorView(), "Ouvrir la séance du jour");
                    ready.set(action != null && action.isEnabled());
                });
                enabled = ready.get();
                if (!enabled) Thread.sleep(50L);
            }
            assertTrue("Today action never became enabled", enabled);

            scenario.onActivity(activity -> {
                View action = findByContentDescription(
                    activity.getWindow().getDecorView(), "Ouvrir la séance du jour");
                assertNotNull(action);
                action.performClick();
            });

            opened = monitor.waitForActivityWithTimeout(5_000L);
            assertNotNull("HifzSessionActivity was not opened", opened);
            Intent intent = opened.getIntent();
            assertEquals(HifzSessionActivity.RECENT_SABQI_REVIEW,
                intent.getStringExtra(HifzSessionActivity.EXTRA_MODE));
            assertEquals(HifzClock.today().toString(),
                intent.getStringExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE));
        } finally {
            if (opened != null) opened.finish();
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test public void legacyFiveLineProgressMigratesToStabilizedAndContinuesOnNewPhysicalUnit() {
        seedLegacyPage529SchemaFive();

        HifzPrefs prefs = new HifzPrefs(context);
        List<GeometryRepository.LineMeta> page = pageLines(529);
        assertEquals(15, page.size());
        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(page);
        assertEquals(2, units.size());
        assertEquals(7, units.get(0).lineIds.size());
        assertEquals(8, units.get(1).lineIds.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("legacy validated five-line work must become Stabilized",
                HifzPrefs.ProgressState.STABILIZED, prefs.progressStateV6(page.get(i).id));
        }
        for (int i = 10; i < 15; i++) {
            assertEquals("unvalidated tail must remain Learned",
                HifzPrefs.ProgressState.LEARNED, prefs.progressStateV6(page.get(i).id));
        }
        assertEquals("new 7+8 plan must resume on its first incomplete unit", 1, prefs.itqanBlockIndex());

        assertTrue(prefs.completeStabilizationBlockV6(
            units.get(1).lineIds, 2, true,
            new VerseRef(54, 7), new VerseRef(54, 27), null,
            "2026-09-16", "migration regression"));
        for (GeometryRepository.LineMeta line : page) {
            assertEquals(HifzPrefs.ProgressState.STABILIZED, prefs.progressStateV6(line.id));
        }
    }

    @Test public void verseBoundaryOwnedLinesProduceReadyConsolidationUnitWithoutPartialAcquiredCrash() {
        HifzPrefs prefs = new HifzPrefs(context);
        List<GeometryRepository.LineMeta> page11 = pageLines(11);
        List<String> stabilized = new ArrayList<>();
        for (int i = 7; i <= 14; i++) stabilized.add(page11.get(i).id);
        String previousOwnerSharedLine = page11.get(6).id;

        assertTrue(main.edit()
            .putString("anchoringQueue", "[{\"start\":\"2:74\",\"end\":\"2:76\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("v6StabilizedLineIds", json(stabilized))
            .putString("v6AcquiredCreditLineIds", json(Collections.singletonList(previousOwnerSharedLine)))
            .putString("v6LegacyPartialAcquiredLineIds", "[]")
            .putString("v6QuarantineLineIds", "[]")
            .commit());

        List<ConsolidationCycleEngine.Unit> ready = prefs.stabilizedConsolidationUnits(geometry, 3);
        assertEquals(1, ready.size());
        assertEquals(stabilized,
            ConsolidationPhysicalUnitPolicy.decodeLineUnit(ready.get(0).id()));
    }

    @Test public void adjacentSurahRangesStayDistinctAndManualEditClearsFrozenConsolidation() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(main.edit().putString("itqanRanges",
            "[{\"start\":\"53:1\",\"end\":\"53:62\"},{\"start\":\"54:1\",\"end\":\"54:55\"}]").commit());

        List<VerseRange> ranges = prefs.itqanRanges();
        assertEquals(2, ranges.size());
        assertEquals(new VerseRef(53, 1), ranges.get(0).getStart());
        assertEquals(new VerseRef(53, 62), ranges.get(0).getEndInclusive());
        assertEquals(new VerseRef(54, 1), ranges.get(1).getStart());
        assertEquals(new VerseRef(54, 55), ranges.get(1).getEndInclusive());

        assertTrue(main.edit().putString("v6ConsolidationStabilizationState", "{\"sentinel\":true}").commit());
        assertTrue(prefs.setV6StabilizationRanges(Collections.emptyList(), geometry));
        assertFalse(main.contains("v6ConsolidationStabilizationState"));
    }

    @Test public void realPage48SingleAyahUsesSevenPlusEightDistinctPhysicalLines() {
        List<GeometryRepository.LineMeta> page = pageLines(48);
        assertEquals(15, page.size());
        for (GeometryRepository.LineMeta line : page) {
            assertEquals(Collections.singletonList(new VerseRef(2, 282)), line.verses);
        }
        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(page);
        assertEquals(2, units.size());
        assertEquals(7, units.get(0).lineIds.size());
        assertEquals(8, units.get(1).lineIds.size());
        assertFalse(units.get(0).lineIds.equals(units.get(1).lineIds));
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

    private void seedLegacyPage529SchemaFive() {
        assertTrue(main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:74")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", "[{\"start\":\"54:7\",\"end\":\"54:27\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"54:7\",\"end\":\"54:27\"}]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[{\"start\":\"54:7\",\"end\":\"54:27\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[54]")
            .putInt("itqanBlockIndex", 2)
            .putInt("itqanRep", 9)
            .putInt("itqanAssisted", 1)
            .putInt("itqanFinalReveals", 1)
            .putString("itqanUnitStart", "54:7")
            .putString("itqanUnitEnd", "54:27")
            .putString("itqanRotationStart", "54:7")
            .putString("itqanCursor", "54:7")
            .putString("murajaahCursor", "2:1")
            .putString("recentSabqi", "[]")
            .putString("consolidationAttendanceDates", "[]")
            .putFloat("murajaahSecPerLine", 9.0f)
            .putBoolean("murajaahSpeedCalibrated", false)
            .commit());
    }

    private static String json(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array.toString();
    }
}
