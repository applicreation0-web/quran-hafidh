package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Zero-loss upgrade contract for an already-installed Quran Hifz 0.7.4 schema-v5 state. */
public final class HifzPrefsV6MigrationInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences main;
    private SharedPreferences legacyJ10;
    private SharedPreferences dashboardHistory;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        legacyJ10 = context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE);
        dashboardHistory = context.getSharedPreferences("quran_hifz_dashboard_ledger_v1", Context.MODE_PRIVATE);
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
        dashboardHistory.edit().clear().commit();
    }

    @After public void tearDown() {
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
        dashboardHistory.edit().clear().commit();
    }

    @Test public void calibratedInstalledProgressMigratesFromFiveToSixWithoutLoss() {
        seedRepresentativeSchemaFive(true, 7.25f);
        legacyJ10.edit()
            .putLong("line:49:1", LocalDate.of(2026, 9, 7).toEpochDay())
            .putLong("line:49:2", LocalDate.of(2026, 9, 8).toEpochDay())
            .commit();

        Map<String, ?> beforeMain = snapshot(main);
        Map<String, ?> beforeLegacyJ10 = snapshot(legacyJ10);

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertEquals(7.25, prefs.murajaahSecondsPerLine(), 0.0001);
        assertEquals(1234, prefs.sabqiLineCursor());
        assertEquals(17, prefs.sabqiRep());
        assertEquals(2, prefs.sabqiAssisted());
        assertEquals(0, prefs.itqanRep());
        assertEquals(0, prefs.itqanAssisted());
        assertEquals(0, prefs.itqanFinalReveals());
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals("49:1", prefs.itqanUnitStart().toString());
        assertEquals("49:18", prefs.itqanUnitEnd().toString());
        assertEquals(87_654L, prefs.elapsedFor(HifzSessionActivity.SABQI));
        assertEquals(0L, prefs.elapsedFor(HifzSessionActivity.ITQAN));
        assertEquals("Sabqi installé à conserver", prefs.lastSabqiLabel());
        assertEquals("Itqān installé à conserver", prefs.lastItqanLabel());
        assertEquals(1, prefs.recentSabqi().size());
        assertEquals(100, prefs.recentSabqi().get(0).startLine);
        assertEquals(104, prefs.recentSabqi().get(0).endLine);
        assertEquals(LocalDate.of(2026, 9, 12), prefs.recentSabqi().get(0).addedOn);
        assertEquals(2, prefs.recentSabqi().get(0).reviewStreak);

        assertExistingKeysUnchangedExcept(beforeMain, snapshot(main), "schema",
            "itqanRep", "itqanAssisted", "itqanFinalReveals", "itqanBlockIndex", "itqanElapsedMs");
        assertEquals(beforeLegacyJ10, snapshot(legacyJ10));
    }

    @Test public void uncalibratedHistoricalNineSecondDefaultMovesToEightWithoutOtherLoss() {
        seedRepresentativeSchemaFive(false, 9.0f);
        Map<String, ?> before = snapshot(main);

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertEquals(8.0, prefs.murajaahSecondsPerLine(), 0.0001);
        assertFalse(main.getBoolean("murajaahSpeedCalibrated", true));
        assertExistingKeysUnchangedExcept(before, snapshot(main), "schema", "murajaahSecPerLine",
            "itqanRep", "itqanAssisted", "itqanFinalReveals", "itqanBlockIndex", "itqanElapsedMs");
    }

    @Test public void reopeningMigratedStateIsIdempotentAndDoesNotTouchLegacyJ10() {
        seedRepresentativeSchemaFive(true, 6.75f);
        legacyJ10.edit()
            .putLong("line:2:10", LocalDate.of(2026, 9, 6).toEpochDay())
            .commit();

        new HifzPrefs(context);
        Map<String, ?> afterFirstMain = snapshot(main);
        Map<String, ?> afterFirstJ10 = snapshot(legacyJ10);

        HifzPrefs reopened = new HifzPrefs(context);

        assertEquals(6, reopened.schema());
        assertEquals(afterFirstMain, snapshot(main));
        assertEquals(afterFirstJ10, snapshot(legacyJ10));
    }

    @Test public void schemaFiveDashboardHistorySurvivesUpgradeByteForByte() {
        seedRepresentativeSchemaFive(true, 7.25f);
        assertTrue(dashboardHistory.edit()
  .putString("started", "2026-08-01")
  .putString("records", "[{\"date\":\"2026-09-14\",\"type\":\"ITQAN\",\"label\":\"Itqān installé à conserver\"}]")
  .commit());
        Map<String, ?> beforeHistory = snapshot(dashboardHistory);

        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        assertEquals(beforeHistory, snapshot(dashboardHistory));
        assertEquals("Stabilisation installée à conserver",
  HifzDisplayVocabulary.canonicalize("Itqān installé à conserver"));
    }

    @Test public void freshProfileInitializesCompleteSchemaSixState() {
        HifzPrefs prefs = new HifzPrefs(context);

        assertEquals(6, prefs.schema());
        for (String key : new String[]{
                "v6LearnedLineIds",
                "v6StabilizedLineIds",
                "v6AcquiredCreditLineIds",
                "v6LegacyPartialAcquiredLineIds",
                "v6QuarantineLineIds",
                "v6UnknownDueLineIds",
                "v6LegacyImportedLineIds"}) {
            assertTrue("fresh schema6 missing state key " + key, main.contains(key));
            assertEquals("fresh schema6 set must start empty for " + key, "[]", main.getString(key, null));
        }
        for (String key : new String[]{
                "v6QuarantineLegacyLastReviewed",
                "v6ActiveJ10LastReviewed",
                "v6LegacyOrphanJ10Dates"}) {
            assertTrue("fresh schema6 missing map key " + key, main.contains(key));
            assertEquals("fresh schema6 map must start empty for " + key, "{}", main.getString(key, null));
        }
        assertTrue("fresh schema6 must not create legacy J10 state", legacyJ10.getAll().isEmpty());
    }

    private void seedRepresentativeSchemaFive(boolean calibrated, float secondsPerLine) {
        boolean ok = main.edit()
            .putInt("schema", 5)
            .putString("programStartDate", "2026-08-01")
            .putString("lowerBound", "2:1")
            .putString("promotedFrontier", "2:150")
            .putString("upperTailStart", "49:1")
            .putString("sabqiStart", "2:75")
            .putString("sabqiEnd", "2:286")
            .putString("itqanRanges", "[{\"start\":\"2:1\",\"end\":\"2:74\"}]")
            .putString("promotedRanges", "[{\"start\":\"49:1\",\"end\":\"49:18\"}]")
            .putString("unconsolidatedPromotedRanges", "[{\"start\":\"49:1\",\"end\":\"49:18\"}]")
            .putString("legacyMurajaahPromotedRanges", "[]")
            .putString("forcedPromotedRanges", "[]")
            .putString("anchoringQueue", "[{\"start\":\"49:1\",\"end\":\"49:18\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":2}]")
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[49]")
            .putInt("itqanBlockIndex", 2)
            .putString("itqanRotationStart", "49:1")
            .putString("itqanCursor", "49:1")
            .putString("murajaahCursor", "2:25")
            .putInt("sabqiLineCursor", 1234)
            .putInt("sabqiRep", 17)
            .putInt("sabqiAssisted", 2)
            .putInt("itqanRep", 9)
            .putInt("itqanAssisted", 1)
            .putInt("itqanFinalReveals", 2)
            .putString("itqanUnitStart", "49:1")
            .putString("itqanUnitEnd", "49:18")
            .putString("recentSabqi", "[{\"start\":100,\"end\":104,\"addedOn\":\"2026-09-12\",\"reviewStreak\":2}]")
            .putString("recentConsolidationActivatedOn", "2026-09-13")
            .putString("consolidationAttendanceDates", "[\"2026-09-06\",\"2026-09-13\"]")
            .putLong("sabqiElapsedMs", 87_654L)
            .putLong("itqanElapsedMs", 54_321L)
            .putLong("sabqi_today_reviewElapsedMs", 12_345L)
            .putLong("recent_sabqi_reviewElapsedMs", 23_456L)
            .putLong("murajaahElapsedMs", 34_567L)
            .putString("sabqiTodayReviewDate", "2026-09-15")
            .putInt("sabqiTodayReviewStartLine", 100)
            .putInt("sabqiTodayReviewEndLine", 104)
            .putString("lastSabqiTodayReviewDate", "2026-09-14")
            .putString("lastSabqiTodayReviewLabel", "Sabqi review historique")
            .putInt("recentSabqiReviewIndex", 3)
            .putString("lastRecentSabqiReviewDate", "2026-09-13")
            .putString("lastRecentSabqiReviewLabel", "Consolidation historique")
            .putString("murajaahActualEnd", "2:30")
            .putFloat("murajaahSecPerLine", secondsPerLine)
            .putFloat("recentSecPerLine", 9.0f)
            .putBoolean("murajaahSpeedCalibrated", calibrated)
            .putInt("murajaahSpeedSamples", calibrated ? 4 : 0)
            .putBoolean("recentSpeedCalibrated", true)
            .putInt("recentSpeedSamples", 3)
            .putBoolean("forceEink", true)
            .putString("lastSabqiDate", "2026-09-15")
            .putString("lastSabqiLabel", "Sabqi installé à conserver")
            .putString("lastItqanDate", "2026-09-14")
            .putString("lastItqanLabel", "Itqān installé à conserver")
            .putString("lastItqanCreditStart", "49:1")
            .putString("lastItqanCreditEnd", "49:18")
            .putInt("lastItqanCreditBlockIndex", 1)
            .putString("lastMurajaahDate", "2026-09-13")
            .putString("lastMurajaahLabel", "Entretien installé à conserver")
            .putString("lastMurajaahCreditStart", "2:20")
            .putString("lastMurajaahCreditEnd", "2:30")
            .commit();
        assertTrue(ok);
    }

    private static Map<String, ?> snapshot(SharedPreferences prefs) {
        return new LinkedHashMap<>(prefs.getAll());
    }

    private static void assertExistingKeysUnchangedExcept(Map<String, ?> before, Map<String, ?> after,
                                                           String... allowedChangedKeys) {
        java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>();
        java.util.Collections.addAll(allowed, allowedChangedKeys);
        for (Map.Entry<String, ?> entry : before.entrySet()) {
            if (allowed.contains(entry.getKey())) continue;
            assertTrue("missing installed key after migration: " + entry.getKey(), after.containsKey(entry.getKey()));
            assertEquals("installed value changed for key " + entry.getKey(), entry.getValue(), after.get(entry.getKey()));
        }
    }
}
