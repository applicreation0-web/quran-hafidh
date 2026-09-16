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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Runtime persistence contract for schema-6 corpus state.
 *
 * Canonical UI semantics:
 * Learned = Appris, Stabilized = Stabilisé, Acquired = Acquis.
 * Quarantine is migration-only internal state and is never user-facing here.
 */
public final class HifzV6PersistentStateInstrumentedTest {
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

    @Test public void freshSchemaSixInitializesEveryDurableV6KeyWithoutTouchingLegacyJ10() throws Exception {
        assertTrue(legacyJ10.edit().putLong("line:legacy-sentinel", 20_000L).commit());
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertEmptyArray(LEARNED);
        assertEmptyArray(STABILIZED);
        assertEmptyArray(ACQUIRED);
        assertEmptyArray(LEGACY_PARTIAL_ACQUIRED);
        assertEmptyArray(QUARANTINE);
        assertEmptyArray(UNKNOWN_DUE);
        assertEmptyArray(LEGACY_IMPORTED);
        assertEmptyMap(QUARANTINE_DATES);
        assertEmptyMap(ACTIVE_J10);
        assertEmptyMap(ORPHAN_J10);
        assertEquals("fresh schema6 must never touch retained legacy J10", legacyBefore, snapshot(legacyJ10));
    }

    @Test public void keepAsAcquiredPersistsExactLegacyDateAndSurvivesReopen() throws Exception {
        seedSingleConflictSchemaFive();
        String conflict = firstOwned(new VerseRef(49, 1), new VerseRef(49, 18));
        long exact = LocalDate.of(2026, 8, 30).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:" + conflict, exact).commit());

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(stringSet(QUARANTINE).contains(conflict));
        assertEquals(Long.valueOf(exact), longMap(QUARANTINE_DATES).get(conflict));
        Map<String, ?> legacyBeforeResolution = snapshot(legacyJ10);

        resolvePersisted(prefs, conflict, HifzV6Migration.QuarantineResolution.KEEP_AS_ACQUIRED);

        assertFalse(stringSet(QUARANTINE).contains(conflict));
        assertFalse(longMap(QUARANTINE_DATES).containsKey(conflict));
        assertTrue(stringSet(ACQUIRED).contains(conflict));
        assertEquals(Long.valueOf(exact), longMap(ACTIVE_J10).get(conflict));
        assertTrue(stringSet(LEGACY_IMPORTED).contains(conflict));
        assertFalse(stringSet(UNKNOWN_DUE).contains(conflict));
        assertFalse(stringSet(LEARNED).contains(conflict));
        assertFalse(longMap(ORPHAN_J10).containsKey(conflict));
        assertEquals(legacyBeforeResolution, snapshot(legacyJ10));

        Map<String, ?> afterResolution = snapshot(main);
        new HifzPrefs(context);
        assertEquals("resolved schema6 state must be idempotent across reopen", afterResolution, snapshot(main));
        assertEquals(legacyBeforeResolution, snapshot(legacyJ10));
    }

    @Test public void keepAsAcquiredWithoutDatePersistsUnknownDueAndNeverInventsToday() throws Exception {
        seedSingleConflictSchemaFive();
        String conflict = firstOwned(new VerseRef(49, 1), new VerseRef(49, 18));

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(stringSet(QUARANTINE).contains(conflict));
        assertFalse(longMap(QUARANTINE_DATES).containsKey(conflict));
        Map<String, ?> legacyBeforeResolution = snapshot(legacyJ10);

        resolvePersisted(prefs, conflict, HifzV6Migration.QuarantineResolution.KEEP_AS_ACQUIRED);

        assertFalse(stringSet(QUARANTINE).contains(conflict));
        assertTrue(stringSet(ACQUIRED).contains(conflict));
        assertTrue(stringSet(UNKNOWN_DUE).contains(conflict));
        assertFalse(longMap(ACTIVE_J10).containsKey(conflict));
        assertFalse(stringSet(LEGACY_IMPORTED).contains(conflict));
        assertFalse(longMap(ORPHAN_J10).containsKey(conflict));
        assertEquals(legacyBeforeResolution, snapshot(legacyJ10));
    }

    @Test public void returnToStabilizationPersistsOnlyInactiveHistoricalDateAndNoDualState() throws Exception {
        seedSingleConflictSchemaFive();
        String conflict = firstOwned(new VerseRef(49, 1), new VerseRef(49, 18));
        long exact = LocalDate.of(2026, 8, 29).toEpochDay();
        assertTrue(legacyJ10.edit().putLong("line:" + conflict, exact).commit());

        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(stringSet(QUARANTINE).contains(conflict));
        Map<String, ?> legacyBeforeResolution = snapshot(legacyJ10);

        resolvePersisted(prefs, conflict, HifzV6Migration.QuarantineResolution.RETURN_TO_STABILIZATION);

        assertFalse(stringSet(QUARANTINE).contains(conflict));
        assertFalse(longMap(QUARANTINE_DATES).containsKey(conflict));
        assertTrue("pending Stabilisation is durably represented as Appris", stringSet(LEARNED).contains(conflict));
        assertEquals(Long.valueOf(exact), longMap(ORPHAN_J10).get(conflict));
        assertFalse(stringSet(ACQUIRED).contains(conflict));
        assertFalse(stringSet(UNKNOWN_DUE).contains(conflict));
        assertFalse(stringSet(LEGACY_IMPORTED).contains(conflict));
        assertFalse(longMap(ACTIVE_J10).containsKey(conflict));
        assertFalse(stringSet(STABILIZED).contains(conflict));
        assertEquals(legacyBeforeResolution, snapshot(legacyJ10));
    }

    @Test public void persistentResolutionRejectsNonQuarantinedLineAndNonSchemaSixState() throws Exception {
        HifzPrefs fresh = new HifzPrefs(context);
        expectIllegalState(() -> resolvePersisted(
            fresh, "not-quarantined", HifzV6Migration.QuarantineResolution.KEEP_AS_ACQUIRED));

        seedSingleConflictSchemaFive();
        String conflict = firstOwned(new VerseRef(49, 1), new VerseRef(49, 18));
        HifzPrefs migrated = new HifzPrefs(context);
        assertTrue(stringSet(QUARANTINE).contains(conflict));
        assertTrue(main.edit().putInt("schema", 5).commit());
        expectIllegalState(() -> resolvePersisted(
            migrated, conflict, HifzV6Migration.QuarantineResolution.KEEP_AS_ACQUIRED));
    }

    @Test public void disjointPendingRangesMigrateIndependentlyWithoutCursorResetOrCrossRangeBleed() throws Exception {
        seedDisjointRangesSchemaFive();
        String learnedRangeLine = firstOwned(new VerseRef(49, 1), new VerseRef(49, 18));
        String conflictingRangeLine = firstOwned(new VerseRef(50, 1), new VerseRef(50, 16));
        String stableLine = firstOwned(new VerseRef(2, 1), new VerseRef(2, 74));

        long learnedHistorical = LocalDate.of(2026, 9, 1).toEpochDay();
        long conflictHistorical = LocalDate.of(2026, 9, 2).toEpochDay();
        long stableHistorical = LocalDate.of(2026, 9, 3).toEpochDay();
        assertTrue(legacyJ10.edit()
            .putLong("line:" + learnedRangeLine, learnedHistorical)
            .putLong("line:" + conflictingRangeLine, conflictHistorical)
            .putLong("line:" + stableLine, stableHistorical)
            .commit());
        Map<String, ?> legacyBefore = snapshot(legacyJ10);

        new HifzPrefs(context);

        assertTrue(stringSet(LEARNED).contains(learnedRangeLine));
        assertEquals(Long.valueOf(learnedHistorical), longMap(ORPHAN_J10).get(learnedRangeLine));
        assertFalse(stringSet(QUARANTINE).contains(learnedRangeLine));
        assertFalse(stringSet(ACQUIRED).contains(learnedRangeLine));

        assertTrue(stringSet(QUARANTINE).contains(conflictingRangeLine));
        assertEquals(Long.valueOf(conflictHistorical), longMap(QUARANTINE_DATES).get(conflictingRangeLine));
        assertFalse(stringSet(LEARNED).contains(conflictingRangeLine));
        assertFalse(stringSet(ACQUIRED).contains(conflictingRangeLine));

        assertTrue(stringSet(ACQUIRED).contains(stableLine));
        assertEquals(Long.valueOf(stableHistorical), longMap(ACTIVE_J10).get(stableLine));
        assertFalse(stringSet(LEARNED).contains(stableLine));
        assertFalse(stringSet(QUARANTINE).contains(stableLine));

        assertEquals("49:1", main.getString("itqanCursor", null));
        assertEquals("2:25", main.getString("murajaahCursor", null));
        assertEquals(1234, main.getInt("sabqiLineCursor", -1));
        assertEquals(legacyBefore, snapshot(legacyJ10));
    }

    @Test public void manualMultiRangesPersistDisjointStatePreserveCursorsAndNeverInventJ10Dates() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String itqanCursorBefore = main.getString("itqanCursor", null);
        String murajaahCursorBefore = main.getString("murajaahCursor", null);
        int sabqiCursorBefore = main.getInt("sabqiLineCursor", -1);

        List<com.quransafeguard.hifz.core.VerseRange> stabilization = Arrays.asList(
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(49,1), new VerseRef(49,18)),
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(50,1), new VerseRef(50,16)));
        List<com.quransafeguard.hifz.core.VerseRange> acquired = Arrays.asList(
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,1), new VerseRef(2,20)),
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,30), new VerseRef(2,40)));

        assertTrue(prefs.setV6StabilizationRanges(stabilization, geometry));
        assertTrue(prefs.setV6AcquiredRanges(acquired, geometry));
        assertEquals(2, prefs.unconsolidatedPromotedRanges().size());
        assertEquals(2, prefs.itqanRanges().size());
        assertEquals(itqanCursorBefore, main.getString("itqanCursor", null));
        assertEquals(murajaahCursorBefore, main.getString("murajaahCursor", null));
        assertEquals(sabqiCursorBefore, main.getInt("sabqiLineCursor", -1));

        String acquiredLine = firstOwned(new VerseRef(2,1), new VerseRef(2,20));
        String pendingLine = firstOwned(new VerseRef(49,1), new VerseRef(49,18));
        assertTrue(stringSet(ACQUIRED).contains(acquiredLine));
        assertTrue(stringSet(UNKNOWN_DUE).contains(acquiredLine));
        assertFalse(longMap(ACTIVE_J10).containsKey(acquiredLine));
        assertTrue(stringSet(LEARNED).contains(pendingLine));
        assertFalse(stringSet(ACQUIRED).contains(pendingLine));

        Map<String, ?> beforeReopen = snapshot(main);
        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(beforeReopen, snapshot(main));
        assertEquals(2, reopened.unconsolidatedPromotedRanges().size());
        assertEquals(2, reopened.itqanRanges().size());
    }

    @Test public void manualMultiRangesRejectCrossListOverlapAtomically() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        Map<String, ?> before = snapshot(main);
        try {
            prefs.setV6StabilizationRanges(Arrays.asList(
                new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,10), new VerseRef(2,30))), geometry);
            fail("cross-list overlap must fail closed");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("à la fois Acquise et À stabiliser"));
        }
        assertEquals("rejected edit must be atomic", before, snapshot(main));
    }

    private void seedSingleConflictSchemaFive() {
        seedSchemaFive(
            "[{\"start\":\"49:1\",\"end\":\"49:18\"}]",
            "[{\"start\":\"49:1\",\"end\":\"49:18\"}]",
            "49:1", "49:18");
    }

    private void seedDisjointRangesSchemaFive() {
        seedSchemaFive(
            "[{\"start\":\"49:1\",\"end\":\"49:18\"},{\"start\":\"50:1\",\"end\":\"50:16\"}]",
            "[{\"start\":\"50:1\",\"end\":\"50:16\"}]",
            "49:1", "49:18");
    }

    private void seedSchemaFive(
            String pendingRanges,
            String legacyStablePendingRanges,
            String activeUnitStart,
            String activeUnitEnd) {
        boolean ok = main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:74")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", pendingRanges)
            .putString("unconsolidatedPromotedRanges", pendingRanges)
            .putString("legacyMurajaahPromotedRanges", legacyStablePendingRanges)
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[]")
            .putInt("itqanBlockIndex", 0)
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "2:25")
            .putInt("sabqiLineCursor", 1234)
            .putInt("sabqiRep", 0)
            .putInt("sabqiAssisted", 0)
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", activeUnitStart)
            .putString("itqanUnitEnd", activeUnitEnd)
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
            .putString("lastSabqiDate", "")
            .putString("lastSabqiLabel", "")
            .putString("lastItqanDate", "")
            .putString("lastItqanLabel", "")
            .putString("lastItqanCreditStart", "")
            .putString("lastItqanCreditEnd", "")
            .putInt("lastItqanCreditBlockIndex", -1)
            .putString("lastMurajaahDate", "")
            .putString("lastMurajaahLabel", "")
            .putString("lastMurajaahCreditStart", "")
            .putString("lastMurajaahCreditEnd", "")
            .commit();
        assertTrue(ok);
    }

    private void resolvePersisted(
            HifzPrefs prefs,
            String lineId,
            HifzV6Migration.QuarantineResolution resolution) throws Exception {
        Method method = HifzPrefs.class.getDeclaredMethod(
            "resolveV6Quarantine", String.class, HifzV6Migration.QuarantineResolution.class);
        method.setAccessible(true);
        try {
            method.invoke(prefs, lineId, resolution);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof IllegalStateException) throw (IllegalStateException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw error;
        }
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

    private Set<String> stringSet(String key) throws Exception {
        assertTrue("missing schema6 state key " + key, main.contains(key));
        JSONArray array = new JSONArray(main.getString(key, null));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    private Map<String, Long> longMap(String key) throws Exception {
        assertTrue("missing schema6 date map " + key, main.contains(key));
        JSONObject object = new JSONObject(main.getString(key, null));
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            out.put(id, object.getLong(id));
        }
        return out;
    }

    private void assertEmptyArray(String key) throws Exception {
        assertTrue("fresh schema6 missing durable key " + key, main.contains(key));
        assertEquals("fresh schema6 array must be empty: " + key, 0,
            new JSONArray(main.getString(key, null)).length());
    }

    private void assertEmptyMap(String key) throws Exception {
        assertTrue("fresh schema6 missing durable key " + key, main.contains(key));
        assertEquals("fresh schema6 map must be empty: " + key, 0,
            new JSONObject(main.getString(key, null)).length());
    }

    private static Map<String, ?> snapshot(SharedPreferences prefs) {
        return new LinkedHashMap<>(prefs.getAll());
    }

    private interface ThrowingRunnable { void run() throws Exception; }

    private static void expectIllegalState(ThrowingRunnable action) throws Exception {
        try {
            action.run();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
