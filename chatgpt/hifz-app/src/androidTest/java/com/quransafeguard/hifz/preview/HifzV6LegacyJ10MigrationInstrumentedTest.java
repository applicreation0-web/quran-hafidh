package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Runtime contract for importing the published 0.7.4 J10 store into schema 6.
 *
 * UI vocabulary is intentionally represented by the new durable states:
 * Learned = Appris, Stabilized = Stabilisé, Acquired = Acquis. Legacy naming is
 * confined to source compatibility and migration evidence.
 */
public final class HifzV6LegacyJ10MigrationInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private static final String LEARNED = "v6LearnedLineIds";
    private static final String STABILIZED = "v6StabilizedLineIds";
    private static final String ACQUIRED = "v6AcquiredCreditLineIds";
    private static final String LEGACY_PARTIAL_ACQUIRED = "v6LegacyPartialAcquiredLineIds";
    private static final String QUARANTINE = "v6QuarantineLineIds";
    private static final String QUARANTINE_DATES = "v6QuarantineLegacyLastReviewed";
    private static final String ACTIVE_J10 = "v6ActiveJ10LastReviewed";
    private static final String UNKNOWN_DUE = "v6UnknownDueLineIds";
    private static final String LEGACY_IMPORTED = "v6LegacyImportedLineIds";
    private static final String ORPHAN_J10 = "v6LegacyOrphanJ10Dates";

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

    @Test public void importsExactLegacyDatesAndSeparatesCompletedPendingFromLearnedDebt() throws Exception {
        seedSchemaFive(2, false);

        VerseRef start = new VerseRef(49, 1);
        VerseRef end = new VerseRef(49, 18);
        List<String> unitLines = geometry.lineIdsForVerseRange(start, end);
        Set<String> ownedPending = ownedLineIds(start, end);
        int[] segments = geometry.surahSegmentLineCounts(start, end);
        assertTrue("fixture requires at least three legacy sub-blocks",
            PreviewConfig.fractionatedBlockCount(segments) >= 3);

        int completedLineCount = 0;
        for (int block = 0; block < 2; block++) {
            completedLineCount += PreviewConfig.fractionatedBlockLength(segments, block);
        }
        LinkedHashSet<String> completedPhysical = new LinkedHashSet<>(
            unitLines.subList(0, Math.min(completedLineCount, unitLines.size())));

        String completed = firstIntersection(ownedPending, completedPhysical);
        String learned = firstDifference(ownedPending, completedPhysical);
        assertFalse(completed.equals(learned));

        long completedDate = LocalDate.of(2026, 9, 2).toEpochDay();
        long learnedLegacyDate = LocalDate.of(2026, 9, 3).toEpochDay();
        assertTrue(legacyJ10.edit()
            .putLong("line:" + completed, completedDate)
            .putLong("line:" + learned, learnedLegacyDate)
            .commit());
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        new HifzPrefs(context);

        assertEquals(6, main.getInt("schema", -1));
        assertTrue("validated legacy sub-blocks become Stabilisé", stringSet(STABILIZED).contains(completed));
        assertFalse("migration must not invent Acquis credit", stringSet(ACQUIRED).contains(completed));
        assertFalse("resolved legacy work must not remain partial debt", stringSet(LEGACY_PARTIAL_ACQUIRED).contains(completed));
        assertEquals("old J10 evidence is retained as archive, not active review credit",
            Long.valueOf(completedDate), longMap(ORPHAN_J10).get(completed));
        assertFalse(longMap(ACTIVE_J10).containsKey(completed));
        assertFalse(stringSet(LEGACY_IMPORTED).contains(completed));

        assertTrue("unfinished historical work becomes Appris", stringSet(LEARNED).contains(learned));
        assertEquals(Long.valueOf(learnedLegacyDate), longMap(ORPHAN_J10).get(learned));
        assertFalse(longMap(ACTIVE_J10).containsKey(learned));
        assertFalse(stringSet(ACQUIRED).contains(learned));

        String stableWithoutDate = firstOwned(new VerseRef(2, 1), new VerseRef(2, 74));
        assertTrue(stringSet(ACQUIRED).contains(stableWithoutDate));
        assertTrue(stringSet(UNKNOWN_DUE).contains(stableWithoutDate));

        assertTrue("legacy validated sub-blocks must populate Stabilisé", !stringSet(STABILIZED).isEmpty());
        assertEquals("legacy J10 must remain byte/logically untouched", legacyBefore, snapshot(legacyJ10));
    }

    @Test public void unfinishedStableAndPendingConflictIsQuarantinedWithoutInventedOperationalState() throws Exception {
        seedSchemaFive(0, true);
        VerseRef start = new VerseRef(49, 1);
        VerseRef end = new VerseRef(49, 18);
        String conflict = firstOwned(start, end);
        long oldDate = LocalDate.of(2026, 8, 30).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:" + conflict, oldDate).commit());
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        new HifzPrefs(context);

        assertTrue(stringSet(QUARANTINE).contains(conflict));
        assertEquals(Long.valueOf(oldDate), longMap(QUARANTINE_DATES).get(conflict));
        assertFalse(stringSet(LEARNED).contains(conflict));
        assertFalse(stringSet(ACQUIRED).contains(conflict));
        assertFalse(longMap(ACTIVE_J10).containsKey(conflict));
        assertFalse(longMap(ORPHAN_J10).containsKey(conflict));
        assertEquals(legacyBefore, snapshot(legacyJ10));
    }

    @Test public void schemaSixReopenNeverReadsOrRewritesLegacyJ10Again() throws Exception {
        seedSchemaFive(0, false);
        String stable = firstOwned(new VerseRef(2, 1), new VerseRef(2, 74));
        long originalDate = LocalDate.of(2026, 9, 1).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:" + stable, originalDate).commit());

        new HifzPrefs(context);
        Map<String, ?> mainAfterMigration = snapshot(main);

        long externallyChangedLegacyDate = LocalDate.of(2026, 9, 14).toEpochDay();
        assertTrue(legacyJ10.edit()
            .putLong("line:" + stable, externallyChangedLegacyDate)
            .putLong("line:legacy-only-after-v6", LocalDate.of(2026, 9, 15).toEpochDay())
            .commit());
        Map<String, ?> legacyAfterExternalChange = snapshot(legacyJ10);

        new HifzPrefs(context);

        assertEquals("schema 6 must be authoritative after the atomic main-store commit",
            mainAfterMigration, snapshot(main));
        assertEquals("schema 6 runtime must never rewrite the retained legacy store",
            legacyAfterExternalChange, snapshot(legacyJ10));
        assertEquals(Long.valueOf(originalDate), longMap(ACTIVE_J10).get(stable));
    }

    @Test public void schemaSixJ10RuntimeUsesOnlyMainStoreAndKeepsUnknownDueExplicit() throws Exception {
        seedSchemaFive(0, false);
        Set<String> stableLines = ownedLineIds(new VerseRef(2, 1), new VerseRef(2, 74));
        assertTrue("fixture requires multiple acquired lines", stableLines.size() > 1);
        Iterator<String> stableIterator = stableLines.iterator();
        String dated = stableIterator.next();
        String unknown = stableIterator.next();

        LocalDate importedDate = LocalDate.of(2026, 9, 1);
        assertTrue(legacyJ10.edit().putLong("line:" + dated, importedDate.toEpochDay()).commit());
        new HifzPrefs(context);

        assertEquals(Long.valueOf(importedDate.toEpochDay()), longMap(ACTIVE_J10).get(dated));
        assertTrue(stringSet(UNKNOWN_DUE).contains(unknown));
        assertFalse(longMap(ACTIVE_J10).containsKey(unknown));

        assertTrue(legacyJ10.edit()
            .putLong("line:" + dated, LocalDate.of(2026, 9, 14).toEpochDay())
            .putLong("line:" + unknown, LocalDate.of(2026, 9, 15).toEpochDay())
            .putLong("line:legacy-only-after-v6", LocalDate.of(2026, 9, 15).toEpochDay())
            .commit());
        Map<String, ?> legacyBeforeRuntime = snapshot(legacyJ10);

        J10V6Store store = new J10V6Store(context);
        Map<String, LocalDate> runtime = store.snapshot();
        assertEquals("runtime must use the exact date imported into schema6",
            importedDate, runtime.get(dated));
        assertFalse("post-migration legacy-only lines must never enter runtime",
            runtime.containsKey("legacy-only-after-v6"));

        LocalDate today = LocalDate.of(2026, 9, 15);
        assertTrue("schema6 sync must not access the retained legacy J10 archive",
            store.syncAcquired());
        assertTrue("UNKNOWN_DUE must stay explicit instead of receiving an invented date",
            stringSet(UNKNOWN_DUE).contains(unknown));
        assertFalse(longMap(ACTIVE_J10).containsKey(unknown));

        assertTrue(store.markReviewed(Collections.singleton(unknown), today));
        assertEquals(Long.valueOf(today.toEpochDay()), longMap(ACTIVE_J10).get(unknown));
        assertFalse(stringSet(UNKNOWN_DUE).contains(unknown));
        assertFalse(stringSet(LEGACY_IMPORTED).contains(unknown));
        assertEquals("schema6 J10 runtime must leave the retained legacy archive byte/logically untouched",
            legacyBeforeRuntime, snapshot(legacyJ10));
    }

    private void seedSchemaFive(int completedBlocks, boolean stablePendingOverlap) {
        String reconstruction = "[{\"start\":\"49:1\",\"end\":\"49:18\"}]";
        boolean ok = main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:74")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", reconstruction)
            .putString("unconsolidatedPromotedRanges", reconstruction)
            .putString("legacyMurajaahPromotedRanges", stablePendingOverlap ? reconstruction : "[]")
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[{\"start\":\"49:1\",\"end\":\"49:18\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[49]")
            .putInt("itqanBlockIndex", completedBlocks)
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "2:1")
            .putInt("sabqiLineCursor", 100)
            .putInt("sabqiRep", 0)
            .putInt("sabqiAssisted", 0)
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", "49:1")
            .putString("itqanUnitEnd", "49:18")
            .putString("recentSabqi", "[]")
            .putString("recentConsolidationActivatedOn", "")
            .putString("consolidationAttendanceDates", "[]")
            .putLong("sabqiElapsedMs", 0L)
            .putLong("itqanElapsedMs", 0L)
            .putLong("sabqi_today_reviewElapsedMs", 0L)
            .putLong("recent_sabqi_reviewElapsedMs", 0L)
            .putLong("murajaahElapsedMs", 0L)
            .putString("murajaahActualEnd", "")
            .putFloat("murajaahSecPerLine", 7.25f)
            .putFloat("recentSecPerLine", 9.0f)
            .putBoolean("murajaahSpeedCalibrated", true)
            .putInt("murajaahSpeedSamples", 4)
            .putBoolean("recentSpeedCalibrated", true)
            .putInt("recentSpeedSamples", 3)
            .putBoolean("forceEink", false)
            .putString("lastItqanCreditStart", "")
            .putString("lastItqanCreditEnd", "")
            .putInt("lastItqanCreditBlockIndex", -1)
            .putString("lastMurajaahCreditStart", "")
            .putString("lastMurajaahCreditEnd", "")
            .commit();
        assertTrue(ok);
    }

    private Set<String> ownedLineIds(VerseRef start, VerseRef end) {
        int low = Math.min(GeometryRepository.ordinal(start), GeometryRepository.ordinal(end));
        int high = Math.max(GeometryRepository.ordinal(start), GeometryRepository.ordinal(end));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            int owner = GeometryRepository.ordinal(CorpusLinePolicy.ownerVerse(line));
            if (owner >= low && owner <= high) out.add(line.id);
        }
        return out;
    }

    private String firstOwned(VerseRef start, VerseRef end) {
        Set<String> ids = ownedLineIds(start, end);
        assertFalse("fixture must own at least one physical line", ids.isEmpty());
        return ids.iterator().next();
    }

    private static String firstIntersection(Set<String> a, Set<String> b) {
        for (String value : a) if (b.contains(value)) return value;
        throw new AssertionError("fixture requires completed owned line");
    }

    private static String firstDifference(Set<String> a, Set<String> b) {
        for (String value : a) if (!b.contains(value)) return value;
        throw new AssertionError("fixture requires unfinished owned line");
    }

    private Set<String> stringSet(String key) throws Exception {
        assertTrue("missing schema-6 state key " + key, main.contains(key));
        String raw = main.getString(key, null);
        assertTrue("schema-6 state must be JSON for " + key, raw != null);
        JSONArray array = new JSONArray(raw);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    private Map<String, Long> longMap(String key) throws Exception {
        assertTrue("missing schema-6 date map " + key, main.contains(key));
        String raw = main.getString(key, null);
        assertTrue("schema-6 date map must be JSON for " + key, raw != null);
        JSONObject object = new JSONObject(raw);
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            out.put(id, object.getLong(id));
        }
        return out;
    }

    private static Map<String, ?> snapshot(SharedPreferences prefs) {
        return new LinkedHashMap<>(prefs.getAll());
    }
}
