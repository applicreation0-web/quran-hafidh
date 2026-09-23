package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Two-phase fixture for the CI shell process-death gate.
 * CI runs a_prepare, launches the app, force-stops the real package, relaunches it,
 * then runs b_verify in a fresh instrumentation process.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class ProcessDeathPersistenceInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";
    private static final String GATE_PREFS = "hifz_process_death_gate";
    private static final String ARMED = "armed";

    @Test public void a_prepareFixtureForExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences raw = context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();

        new HifzPrefs(context);
        String range = "[{\"start\":\"55:70\",\"end\":\"56:16\"}]";
        String queue = "[{\"start\":\"55:70\",\"end\":\"56:16\","
            + "\"origin\":\"RECONSTRUCTION\",\"protocol\":\"FULL\",\"failures\":0}]";
        assertTrue(raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("hardAnchoringSurahs", "[55]")
            .commit());

        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry entry = prefs.currentAnchoringEntry(geometry);
        assertNotNull(entry);
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        VerseRef end = GeometryRepository.parseVerse(entry.end);
        assertTrue(prefs.advanceItqanBlock(2, start, end, HifzClock.today().toString(),
            "Ancrage fractionné · process death fixture"));
        assertTrue(prefs.setItqanProgress(3, 1, 1, start, end));
        assertTrue(context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ARMED, true).commit());

        assertPersistedFixture(context);
    }

    @Test public void b_verifyFixtureAfterExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue("Process-death gate was not prepared",
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).getBoolean(ARMED, false));
        try {
            assertPersistedFixture(context);
        } finally {
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    private static void assertPersistedFixture(Context context) {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        assertEquals(2, prefs.itqanBlockIndex());
        assertEquals(3, prefs.itqanRep());
        assertEquals(1, prefs.itqanAssisted());
        assertEquals(1, prefs.itqanFinalReveals());
        assertEquals(new VerseRef(55, 70), prefs.itqanUnitStart());
        assertEquals(new VerseRef(56, 16), prefs.itqanUnitEnd());
        assertArrayEquals(new int[]{6, 7}, geometry.surahSegmentLineCounts(prefs.itqanUnitStart(), prefs.itqanUnitEnd()));
        assertArrayEquals(new int[]{3, 3, 4, 3}, PreviewConfig.fractionatedBlockSizes(
            geometry.surahSegmentLineCounts(prefs.itqanUnitStart(), prefs.itqanUnitEnd())));
        AnchoringQueue.Entry entry = prefs.currentAnchoringEntry(geometry);
        assertNotNull(entry);
        assertEquals("55:70", entry.start);
        assertEquals("56:16", entry.end);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, entry.protocol);
        assertEquals(40, PreviewConfig.itqanTotalReps(entry.protocol));
    }
}
