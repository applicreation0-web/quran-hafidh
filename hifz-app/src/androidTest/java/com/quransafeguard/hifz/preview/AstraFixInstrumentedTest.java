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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Test public void historicalAcquiredCorpusDoesNotBootstrapAtJ0() {
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-09-14T12:00:00Z"), ZoneId.of("UTC")));
        LocalDate today = HifzClock.today();
        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        String baseLine = geometry.lineIdsForVerseRange(new VerseRef(2, 1), new VerseRef(2, 1)).get(0);

        assertTrue(planner.syncAcquired(today));

        assertEquals(today.minusDays(10), planner.snapshot().get(baseLine));
    }

    @Test public void startupReconciliationRecoversHifzCommitMissedByObserver() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry first = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(first.start);
        VerseRef end = GeometryRepository.parseVerse(first.end);
        List<String> ids = geometry.lineIdsForVerseRange(start, end);
        J10ReviewStore store = new J10ReviewStore(context);
        assertTrue(store.acquireLines(ids, today.minusDays(10)));

        String label = "Ancrage · " + start + " → " + end;
        assertTrue(prefs.completeItqanUnitAndConsolidate(start, end,
            prefs.itqanWorkCorpus().next(end), today.toString(), label));

        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        new J10ReviewObserver(planner).reconcileAll(today);
        Map<String, LocalDate> snapshot = planner.snapshot();
        for (String id : ids) assertEquals(today, snapshot.get(id));
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
