package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

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
    private static final String EXPECTED_START = "expectedStart";
    private static final String EXPECTED_END = "expectedEnd";
    private static final long ACTIVE_REVIEW_ELAPSED_MS = 7L * 60_000L + 321L;
    private static final String LEDGER_PREFS = "quran_hifz_dashboard_ledger_v1";
    private static final String J10_SENTINEL = "line:m9-backup-fixture";
    private static final long J10_SENTINEL_DATE = 20540L;
    private static final java.time.LocalDate OLD_COMPLETION = java.time.LocalDate.of(2026,1,4);


    @Test public void a_prepareFixtureForExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences raw = context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();

        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry entry = prefs.currentAnchoringEntry(geometry);
        assertNotNull(entry);
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        VerseRef end = GeometryRepository.parseVerse(entry.end);
        assertTrue(prefs.advanceItqanBlock(2, start, end, HifzClock.today().toString(),
            "Ancrage fractionné · process death fixture"));
        assertTrue(prefs.setItqanProgress(3, 1, 1, start, end));
        assertTrue(prefs.setSabqiProgress(11, 3));
        prefs.setElapsedFor(HifzSessionActivity.MURAJAAH_ACTIVE, ACTIVE_REVIEW_ELAPSED_MS);
        assertTrue(context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ARMED, true)
            .putString(EXPECTED_START, start.toString())
            .putString(EXPECTED_END, end.toString()).commit());

        assertPersistedFixture(context);
        // P0 regression: M9 durable completion and legacy J10 are distinct
        // SharedPreferences stores, not contained in quran_hifz_preview_v1.
        SharedPreferences ledger=context.getSharedPreferences(LEDGER_PREFS,Context.MODE_PRIVATE);
        SharedPreferences legacy=context.getSharedPreferences(J10ReviewStore.NAME,Context.MODE_PRIVATE);
        assertTrue(ledger.edit().clear()
            .putString("completed_session_keys_v1",new org.json.JSONArray().put(OLD_COMPLETION+"|"+HifzSessionActivity.SABQI).toString())
            .putString("started","2026-01-01").commit());
        assertTrue(legacy.edit().clear().putLong(J10_SENTINEL,J10_SENTINEL_DATE).commit());
        try {
            org.json.JSONObject backup=HifzBackup.export(context);
            assertEquals(2,backup.getInt("version"));
            assertTrue(backup.getJSONObject("dashboard").has("completed_session_keys_v1"));
            assertTrue(backup.getJSONObject("j10").has(J10_SENTINEL));
            assertTrue(ledger.edit().clear().commit());
            assertTrue(legacy.edit().clear().commit());
            assertTrue(HifzBackup.restore(context,backup)>0);
            assertPersistedFixture(context);
            assertBackupLedgerAndJ10(context);
            // A legacy V1 backup still imports, but cannot invent missing receipts.
            org.json.JSONObject oldV1=new org.json.JSONObject(backup.toString());
            oldV1.put("version",1);
            oldV1.remove("dashboard");
            oldV1.remove("j10");
            assertTrue(HifzBackup.restore(context,oldV1)>0);
            assertTrue("V1 import must clear stale M9 receipts",
                !context.getSharedPreferences(LEDGER_PREFS,Context.MODE_PRIVATE)
                    .contains("completed_session_keys_v1"));
            assertTrue("V1 import must clear unrelated target J10 history",
                !legacy.contains(J10_SENTINEL));
            // Restoring the real V2 snapshot recovers exact receipts and dates.
            assertTrue(HifzBackup.restore(context,backup)>0);
            assertBackupLedgerAndJ10(context);
            // A malformed receipt must be rejected BEFORE replacing good data.
            org.json.JSONObject corrupt=new org.json.JSONObject(backup.toString());
            corrupt.getJSONObject("dashboard").put("completed_session_keys_v1",
                new org.json.JSONObject().put("t","s").put("v",new org.json.JSONArray().put("not-a-date|sabqi").toString()));
            try { HifzBackup.restore(context,corrupt); 
                throw new AssertionError("Corrupt receipt accepted"); }
            catch(org.json.JSONException expected){}
            assertBackupLedgerAndJ10(context);
        }catch(org.json.JSONException invalid){throw new AssertionError(invalid);}
    }

    private static void assertBackupLedgerAndJ10(Context context) {
        assertNotNull("M9 completion lost after JSON restore",
            new DashboardLedger(context).find(OLD_COMPLETION,HifzSessionActivity.SABQI));
        assertEquals("J10 historic review timestamp lost",
            J10_SENTINEL_DATE,context.getSharedPreferences(
                J10ReviewStore.NAME,Context.MODE_PRIVATE).getLong(J10_SENTINEL,-1));
    }

    @Test public void b_verifyFixtureAfterExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue("Process-death gate was not prepared",
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).getBoolean(ARMED, false));
        try {
            assertPersistedFixture(context);
            assertBackupLedgerAndJ10(context);
        } finally {
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(LEDGER_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    private static void assertPersistedFixture(Context context) {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        SharedPreferences gate = context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE);
        VerseRef expectedStart = GeometryRepository.parseVerse(gate.getString(EXPECTED_START, ""));
        VerseRef expectedEnd = GeometryRepository.parseVerse(gate.getString(EXPECTED_END, ""));
        assertEquals(2, prefs.itqanBlockIndex());
        assertEquals(3, prefs.itqanRep());
        assertEquals(1, prefs.itqanAssisted());
        assertEquals(1, prefs.itqanFinalReveals());
        assertEquals(11, prefs.sabqiRep());
        assertEquals(3, prefs.sabqiAssisted());
        assertEquals(15, HifzSchedule.ACTIVE_REVIEW_MINUTES);
        assertEquals(ACTIVE_REVIEW_ELAPSED_MS,
            prefs.elapsedFor(HifzSessionActivity.MURAJAAH_ACTIVE));
        assertEquals(expectedStart, prefs.itqanUnitStart());
        assertEquals(expectedEnd, prefs.itqanUnitEnd());
        assertTrue(PreviewConfig.fractionatedBlockCount(
            geometry.surahSegmentLineCounts(expectedStart, expectedEnd)) >= 3);
        AnchoringQueue.Entry entry = prefs.currentAnchoringEntry(geometry);
        assertNotNull(entry);
        assertEquals(expectedStart.toString(), entry.start);
        assertEquals(expectedEnd.toString(), entry.end);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, entry.protocol);
        assertEquals(40, PreviewConfig.itqanTotalReps(entry.protocol));
    }
}
