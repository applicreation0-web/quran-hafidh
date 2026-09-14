package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** C1/C2/C4 integration proof through the registered process-global preference listener. */
public final class J10GlobalListenerInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";

    private Context context;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertTrue("test must exercise the production Application listener", context instanceof QuranHifzApp);
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void structuredItqanCommitCreditsExactDisplayedBlockWithoutParsingLabel() {
        LocalDate today = HifzClock.today();
        VerseRef[] range = pageVerseRange(534);
        List<String> unit = geometry.lineIdsForVerseRange(range[0], range[1]);
        int[] segments = geometry.surahSegmentLineCounts(range[0], range[1]);
        assertTrue("page 534 must exercise a cross-surah layout", segments.length > 1);
        int from = PreviewConfig.fractionatedBlockStart(segments, 0);
        int length = PreviewConfig.fractionatedBlockLength(segments, 0);
        List<String> expected = new ArrayList<>(unit.subList(from, from + length));
        assertFalse(expected.isEmpty());

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.advanceItqanBlock(
            1, range[0], range[1], today.toString(), "Libellé libre sans plage visible"));

        Map<String, LocalDate> snapshot = awaitCredit(expected, today);
        for (String id : expected) assertEquals(today, snapshot.get(id));
        if (from + length < unit.size()) {
            assertFalse("unfinished lines must not receive today's validated credit",
                today.equals(snapshot.get(unit.get(from + length))));
        }
    }

    @Test public void failedFullReconcileCannotBeClearedByUnrelatedPreferenceDelta() {
        QuranHifzApp app = (QuranHifzApp) context;
        android.content.SharedPreferences raw = context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE);
        raw.unregisterOnSharedPreferenceChangeListener(app);
        try {
            HifzPrefs prefs = new HifzPrefs(context);
            raw.edit().putString("recentSabqi", "{broken-json").commit();
            LocalDate today = LocalDate.of(2026, 9, 14);

            try {
                app.reconcilePreferenceChange("itqanRanges", today);
                fail("corrupt recent Sabqi must make the full reconciliation fail");
            } catch (IllegalStateException expected) {
                // pending full reconciliation must survive this failure.
            }

            raw.edit().putString("recentSabqi", "[]").commit();
            app.reconcilePreferenceChange("forceEink", today);

            String stableLine = geometry.lineIdsForVerseRange(
                new VerseRef(2, 1), new VerseRef(2, 1)).get(0);
            assertEquals(today.minusDays(10), new J10ReviewStore(context).snapshot().get(stableLine));
        } finally {
            raw.registerOnSharedPreferenceChangeListener(app);
        }
    }

    private Map<String, LocalDate> awaitCredit(List<String> ids, LocalDate date) {
        long deadline = SystemClock.elapsedRealtime() + 5_000L;
        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        while (SystemClock.elapsedRealtime() < deadline) {
            boolean done = true;
            for (String id : ids) if (!date.equals(snapshot.get(id))) { done = false; break; }
            if (done) return snapshot;
            SystemClock.sleep(25L);
            snapshot = new J10ReviewStore(context).snapshot();
        }
        return snapshot;
    }

    private VerseRef[] pageVerseRange(int page) {
        ArrayList<VerseRef> refs = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (line.page == page) refs.addAll(line.verses);
        }
        refs.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        if (refs.isEmpty()) throw new AssertionError("No geometry for page " + page);
        return new VerseRef[]{refs.get(0), refs.get(refs.size() - 1)};
    }
}
