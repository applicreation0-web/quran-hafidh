package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AstraFixInstrumentedTest {
    private static final String HIFZ = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences raw;

    @Rule public final TestWatcher resetHifzClock = new TestWatcher() {
        @Override protected void finished(Description description) { HifzClock.resetClockForTests(); }
    };

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        raw = context.getSharedPreferences(HIFZ, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After public void tearDown() {
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void newPromotionPreemptsReconstructionOnNextAnchoringRead() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry before = prefs.currentAnchoringEntry(geometry);
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, before.origin);

        assertTrue(prefs.addPromotedVerses(Collections.singletonList(new VerseRef(2, 75)), false));
        AnchoringQueue.Entry after = prefs.currentAnchoringEntry(geometry);

        assertEquals(AnchoringQueue.Origin.PROMOTED, after.origin);
    }

    @Test public void schemaTwoMigratesDirectlyToFourWithoutLosingProgress() {
        raw.edit()
            .putInt("schema", 2)
            .putString("programStartDate", "2026-01-01")
            .putString("promotedRanges", "[]")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "2:1")
            .putString("lastSabqiLabel", "progression schema2")
            .putString("recentSabqi", "[]")
            .commit();

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(5, prefs.schema());
        assertEquals(LocalDate.of(2026, 1, 1), prefs.programStartDate());
        assertEquals("progression schema2", prefs.lastSabqiLabel());
        assertTrue(prefs.murajaahCorpus().contains(new VerseRef(2, 1)));
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(49, 1)));
    }
}
