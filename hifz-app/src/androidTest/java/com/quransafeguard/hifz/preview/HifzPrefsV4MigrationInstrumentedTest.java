package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HifzPrefsV4MigrationInstrumentedTest {
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

    @Test public void freshSchemaFourBootstrapsTailAsPendingReconstruction() {
        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(4, prefs.schema());
        assertEquals(1, prefs.itqanRanges().size());
        assertTrue(prefs.itqanWorkCorpus().contains(new VerseRef(49, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
        assertTrue(prefs.murajaahCorpus().contains(new VerseRef(2, 74)));
        assertEquals(new VerseRef(49, 1), prefs.itqanCursor());
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
    }

    @Test public void schemaThreeMigrationIsAtomicAndPreservesUnrelatedProgress() {
        raw.edit()
            .putInt("schema", 3)
            .putString("programStartDate", "2026-09-01")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"},{\"start\":\"49:1\",\"end\":\"114:6\"}]")
            .putString("promotedRanges", "[]")
            .putString("unconsolidatedPromotedRanges", "[]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "49:1")
            .putString("lastSabqiLabel", "progression à conserver")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(4, prefs.schema());
        assertEquals("progression à conserver", prefs.lastSabqiLabel());
        assertEquals(1, prefs.itqanRanges().size());
        assertTrue(prefs.isUnconsolidatedPromoted(new VerseRef(49, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
    }
}
