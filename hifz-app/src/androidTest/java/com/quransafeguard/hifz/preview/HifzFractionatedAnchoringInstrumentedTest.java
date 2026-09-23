package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HifzFractionatedAnchoringInstrumentedTest {
    private static final String NAME = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences raw;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        raw = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
    }

    @After public void tearDown() {
        raw.edit().clear().commit();
    }

    @Test public void schemaFourOptionalStateIsAddedWithoutSchemaBumpAndMalformedFlagsFailOpen() {
        raw.edit().putInt("schema", 4).putString("hardAnchoringSurahs", "not-json").commit();
        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertTrue(raw.contains("itqanBlockIndex"));
        assertTrue(prefs.hardAnchoringSurahs().isEmpty());

        raw.edit().putString("hardAnchoringSurahs", "[0,53,53,115,55]").commit();
        assertEquals(Arrays.asList(53, 55), prefs.hardAnchoringSurahs());
    }

    @Test public void page526UsesThreeBalancedBlocksAndEachIntermediateAdvanceIsAtomic() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        VerseRef start = new VerseRef(53, 1);
        VerseRef end = new VerseRef(53, 26);
        EligibleCorpus pageCorpus = EligibleCorpus.Companion.of(Collections.singletonList(new VerseRange(start, end)));
        GeometryRepository.VerseUnit unit = geometry.eligiblePageUnit(start, pageCorpus);

        assertEquals(526, unit.page);
        assertEquals(13, unit.lineIds.size());
        assertArrayEquals(new int[]{5, 4, 4}, PreviewConfig.fractionatedBlockSizes(unit.lineIds.size()));
        assertTrue(prefs.setHardAnchoringSurahs(Collections.singletonList(53)));
        assertTrue(prefs.isFractionatedUnit(unit.verses));

        assertTrue(prefs.setItqanProgress(35, 7, 2, unit.start, unit.end));
        assertTrue(prefs.advanceItqanBlock(1, "2026-09-13", "bloc 1/3"));
        assertEquals(1, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(0, prefs.itqanAssisted());
        assertEquals(0, prefs.itqanFinalReveals());
        assertEquals(0L, prefs.elapsedFor(HifzSessionActivity.ITQAN));
        assertEquals("2026-09-13", prefs.lastItqanDate());

        assertTrue(prefs.advanceItqanBlock(2, "2026-09-14", "bloc 2/3"));
        assertEquals(2, prefs.itqanBlockIndex());
        assertEquals("2026-09-14", prefs.lastItqanDate());
    }

    @Test public void finalFractionatedBlockCompletesWholePageAndNeverTeleportsMaintenanceCursor() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        VerseRef start = new VerseRef(53, 1);
        VerseRef end = new VerseRef(53, 26);
        String range = "[{\"start\":\"53:1\",\"end\":\"53:26\"}]";
        String queue = "[{\"start\":\"53:1\",\"end\":\"53:26\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]";
        raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("itqanCursor", "53:1")
            .putInt("itqanBlockIndex", 2)
            .commit();
        VerseRef maintenanceBefore = prefs.murajaahCursor();

        assertFalse(prefs.murajaahCorpus().contains(start));
        assertTrue(prefs.completeItqanUnitAndConsolidate(start, end, new VerseRef(2, 1),
            "2026-09-15", "fractionated complete"));

        assertEquals(0, prefs.itqanBlockIndex());
        assertTrue(prefs.anchoringQueue().isEmpty());
        assertTrue(prefs.murajaahCorpus().contains(start));
        assertEquals(maintenanceBefore, prefs.murajaahCursor());
    }
    @Test public void threeBlockFractionatedProgressionResetsEachBlockAndLeavesQueueOnlyAtTheEnd() {
        HifzPrefs prefs = new HifzPrefs(context);
        VerseRef start = new VerseRef(53, 1);
        VerseRef end = new VerseRef(53, 26);
        String range = "[{\"start\":\"53:1\",\"end\":\"53:26\"}]";
        String queue = "[{\"start\":\"53:1\",\"end\":\"53:26\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]";
        raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("itqanCursor", "53:1")
            .commit();

        assertTrue(prefs.setItqanProgress(35, 3, 1, start, end));
        assertTrue(prefs.advanceItqanBlock(1, "2026-09-13", "bloc 1/3"));
        assertEquals(1, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(1, prefs.anchoringQueue().size());

        assertTrue(prefs.setItqanProgress(35, 2, 0, start, end));
        assertTrue(prefs.advanceItqanBlock(2, "2026-09-14", "bloc 2/3"));
        assertEquals(2, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(1, prefs.anchoringQueue().size());

        assertTrue(prefs.setItqanProgress(35, 1, 0, start, end));
        assertTrue(prefs.completeItqanUnitAndConsolidate(start, end, new VerseRef(2, 1),
            "2026-09-15", "bloc 3/3"));
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertTrue(prefs.anchoringQueue().isEmpty());
        assertTrue(prefs.murajaahCorpus().contains(start));
    }

}
