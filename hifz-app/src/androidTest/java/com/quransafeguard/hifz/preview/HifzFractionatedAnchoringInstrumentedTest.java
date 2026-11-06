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
        raw.edit().putInt("schema", 4)
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("hardAnchoringSurahs", "not-json").commit();
        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertTrue(raw.contains("itqanBlockIndex"));
        assertTrue(prefs.hardAnchoringSurahs().isEmpty());

        raw.edit().putString("hardAnchoringSurahs", "[0,53,53,115,55]").commit();
        assertEquals(Arrays.asList(53, 55), prefs.hardAnchoringSurahs());
    }

    private HifzPrefs seededPage526() {
        String range = "[{\"start\":\"53:1\",\"end\":\"53:26\"}]";
        String queue = "[{\"start\":\"53:1\",\"end\":\"53:26\","
            + "\"origin\":\"RECONSTRUCTION\",\"protocol\":\"FULL\",\"failures\":0}]";
        assertTrue(raw.edit().putString("itqanRanges",range)
            .putString("promotedRanges",range)
            .putString("unconsolidatedPromotedRanges",range)
            .putString("anchoringQueue",queue)
            .putBoolean("anchoringQueueInitialized",true)
            .putInt("anchoringQueueIndex",0)
            .putString("itqanRotationStart","53:1")
            .putString("itqanCursor","53:1").commit());
        return new HifzPrefs(context);
    }

    @Test public void page526UsesThreeBalancedBlocksAndEachIntermediateAdvanceIsAtomic() {
        HifzPrefs prefs=seededPage526();
        GeometryRepository geometry=GeometryRepository.get(context);
        VerseRef start=new VerseRef(53,1),end=new VerseRef(53,26);
        EligibleCorpus corpus=EligibleCorpus.Companion.of(
            Collections.singletonList(new VerseRange(start,end)));
        GeometryRepository.VerseUnit unit=geometry.eligiblePageUnit(start,corpus);
        assertEquals(526,unit.page);
        assertEquals(13,unit.lineIds.size());
        assertArrayEquals(new int[]{5,4,4},PreviewConfig.fractionatedBlockSizes(13));
        assertTrue(prefs.setHardAnchoringSurahs(Collections.singletonList(53)));
        assertFalse("Retired date-only advancement must not grant a block",
            prefs.advanceItqanBlock(1,"2026-09-13","forged"));
        assertEquals(0,prefs.itqanBlockIndex());
        assertTrue(AndroidItqanProofFixture11712.completeOneBlock(
            prefs,geometry,start,end,"2026-09-13","bloc 1/3"));
        assertEquals(1,prefs.itqanBlockIndex());
        assertEquals(0,prefs.itqanRep());
        assertEquals(0,prefs.itqanAssisted());
        assertEquals(0,prefs.itqanFinalReveals());
        assertEquals("2026-09-13",prefs.lastItqanDate());
        assertTrue(AndroidItqanProofFixture11712.completeOneBlock(
            prefs,geometry,start,end,"2026-09-14","bloc 2/3"));
        assertEquals(2,prefs.itqanBlockIndex());
        assertEquals("2026-09-14",prefs.lastItqanDate());
        assertEquals(0,prefs.itqanRep());
    }

    @Test public void finalFractionatedBlockCompletesWholePageAndNeverTeleportsMaintenanceCursor() {
        HifzPrefs prefs=seededPage526();
        GeometryRepository geometry=GeometryRepository.get(context);
        VerseRef start=new VerseRef(53,1),end=new VerseRef(53,26);
        VerseRef maintenanceBefore=prefs.murajaahCursor();
        assertTrue(prefs.isUnconsolidatedPromoted(start));
        for(int i=0;i<3;i++)
            assertTrue("All three physical blocks must earn repetitions",
                AndroidItqanProofFixture11712.completeOneBlock(prefs,geometry,start,end,
                    "2026-09-"+(13+i),"block "+(i+1)));
        assertEquals(0,prefs.itqanBlockIndex());
        assertEquals(end,prefs.lastItqanCreditEnd());
        assertEquals(maintenanceBefore,prefs.murajaahCursor());
        assertTrue("Ancrage alone cannot invent the evening Consolidation finale",
            prefs.isUnconsolidatedPromoted(start));
    }

    @Test public void threeBlockFractionatedProgressionRequiresEachEarnedProofAndLeavesNoPhantom() {
        HifzPrefs prefs=seededPage526();
        GeometryRepository geometry=GeometryRepository.get(context);
        VerseRef start=new VerseRef(53,1),end=new VerseRef(53,26);
        for(int i=0;i<3;i++) {
            assertFalse("Scalar close without proof must be rejected",
                prefs.advanceItqanBlock(i+1,"2026-09-"+(13+i),"forged"));
            assertTrue(AndroidItqanProofFixture11712.completeOneBlock(
                prefs,geometry,start,end,"2026-09-"+(13+i),"earned "+(i+1)));
            assertEquals(i==2?0:i+1,prefs.itqanBlockIndex());
            assertEquals(0,prefs.itqanRep());
        }
        assertEquals("2026-09-15",prefs.lastItqanDate());
        assertTrue(prefs.murajaahCorpus().contains(start));
    }
}
