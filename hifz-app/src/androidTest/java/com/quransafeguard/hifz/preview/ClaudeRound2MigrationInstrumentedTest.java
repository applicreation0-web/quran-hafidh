package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Migration loss is a blocking 0.7.5 regression. */
public final class ClaudeRound2MigrationInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        main.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void compatibleOpenStabilizationPreservesRepetitionsAndElapsedTime() {
        VerseRef start = new VerseRef(86, 1);
        VerseRef end = new VerseRef(86, 17);
        List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end, geometry);
        assertEquals(7, owned.size());
        assertEquals(1, StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(owned)).size());

        assertTrue(main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:74")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", "[{\"start\":\"86:1\",\"end\":\"86:17\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"86:1\",\"end\":\"86:17\"}]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[{\"start\":\"86:1\",\"end\":\"86:17\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[]")
            .putInt("itqanBlockIndex", 0)
            .putInt("itqanRep", 9)
            .putInt("itqanAssisted", 1)
            .putInt("itqanFinalReveals", 2)
            .putLong("itqanElapsedMs", 54_321L)
            .putString("itqanUnitStart", "86:1")
            .putString("itqanUnitEnd", "86:17")
            .putString("itqanRotationStart", "86:1")
            .putString("itqanCursor", "86:1")
            .putString("murajaahCursor", "2:1")
            .putString("recentSabqi", "[]")
            .putString("consolidationAttendanceDates", "[]")
            .putFloat("murajaahSecPerLine", 9.0f)
            .putBoolean("murajaahSpeedCalibrated", false)
            .commit());

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals("Compatible open unit must preserve repetitions", 9, prefs.itqanRep());
        assertEquals("Compatible open unit must preserve assisted count", 1, prefs.itqanAssisted());
        assertEquals("Compatible open unit must preserve reveal count", 2, prefs.itqanFinalReveals());
        assertEquals("Compatible open unit must preserve elapsed time", 54_321L,
            prefs.elapsedFor(HifzSessionActivity.ITQAN));
        assertEquals(start, prefs.itqanUnitStart());
        assertEquals(end, prefs.itqanUnitEnd());
    }
}
