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
    private static final String LEGACY_GATES = "hifz_preview_session_gates";
    private static final String GATE_SENTINEL = "backup-gate-marker";
    private static final String J10_SENTINEL = "line:m9-backup-fixture";
    private static final long J10_SENTINEL_DATE = 20540L;
    private static final java.time.LocalDate OLD_COMPLETION = java.time.LocalDate.of(2026,1,4);


    @Test public void a_prepareFixtureForExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences raw = context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(LEGACY_GATES, Context.MODE_PRIVATE).edit().clear().commit();

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
        assertTrue(context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE).edit()
            .putString(GATE_SENTINEL,"persist-me").commit());
        try {
            org.json.JSONObject backup=HifzBackup.export(context);
            assertEquals(2,backup.getInt("version"));
            // Kill after each individually committed store: a fresh process must
            // recover the exact old snapshot, never a partially imported state.
            java.util.Map<String,?> mainBefore=new java.util.HashMap<>(raw.getAll());
            java.util.Map<String,?> ledgerBefore=new java.util.HashMap<>(ledger.getAll());
            java.util.Map<String,?> j10Before=new java.util.HashMap<>(legacy.getAll());
            java.util.Map<String,?> gatesBefore=new java.util.HashMap<>(
                context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE).getAll());
            for(String store:new String[]{"MAIN","LEDGER","J10","GATES"}){
                org.json.JSONObject changed=new org.json.JSONObject(backup.toString());
                for(String section:new String[]{"values","dashboard","j10","legacy_gates"}){
                    changed.getJSONObject(section).put("transaction-probe",
                        new org.json.JSONObject().put("t","s").put("v",store));
                }
                HifzBackup.failAfterStoreForTest=store;
                try {
                    HifzBackup.restore(context,changed);
                    throw new AssertionError("Missing injected process death after "+store);
                }catch(AssertionError expected){
                    assertTrue(expected.getMessage(),
                        expected.getMessage().startsWith("Simulated process death after "));
                }finally{HifzBackup.failAfterStoreForTest=null;}
                // The same recovery is called from Application.onCreate.
                HifzBackup.recoverIfNecessary(context);
                assertEquals(mainBefore,raw.getAll());
                assertEquals(ledgerBefore,ledger.getAll());
                assertEquals(j10Before,legacy.getAll());
                assertEquals(gatesBefore,context.getSharedPreferences(
                    LEGACY_GATES,Context.MODE_PRIVATE).getAll());
                HifzBackup.recoverIfNecessary(context); // idempotent
                assertEquals(mainBefore,raw.getAll());
            }
            assertTrue(backup.getJSONObject("dashboard").has("completed_session_keys_v1"));
            assertTrue(backup.getJSONObject("j10").has(J10_SENTINEL));
            assertTrue(backup.getJSONObject("legacy_gates").has(GATE_SENTINEL));
            assertTrue(ledger.edit().clear().commit());
            assertTrue(legacy.edit().clear().commit());
            assertTrue(context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE).edit().clear().commit());
            assertTrue(HifzBackup.restore(context,backup)>0);
            assertPersistedFixture(context);
            assertBackupLedgerAndJ10(context);
            // A legacy V1 backup still imports, but cannot invent missing receipts.
            org.json.JSONObject oldV1=new org.json.JSONObject(backup.toString());
            oldV1.put("version",1);
            oldV1.remove("dashboard");
            oldV1.remove("j10");
            // Real planner contract: import an old programme date without receipts.
            org.json.JSONObject oldValues=oldV1.getJSONObject("values");
            oldValues.put("programStartDate",new org.json.JSONObject().put("t","s")
                .put("v",HifzClock.today().minusDays(185).toString()));
            assertTrue(HifzBackup.restore(context,oldV1)>0);
            assertEquals("No fabricated receipt after V1 import","[]",
                context.getSharedPreferences(LEDGER_PREFS,Context.MODE_PRIVATE)
                    .getString("completed_session_keys_v1","missing"));
            DashboardLedger importedLedger=new DashboardLedger(context);
            assertTrue("Pre-import attendance remains UNKNOWN, not a debt",
                importedLedger.historyUnverifiable(HifzClock.today().minusDays(1)));
            assertTrue("Post-import missed days must still be due",
                !importedLedger.historyUnverifiable(HifzClock.today()));
            // Call the ACTUAL Android MainActivity scheduler, not a source-only proxy.
            try(androidx.test.core.app.ActivityScenario<MainActivity> scenario=
                    androidx.test.core.app.ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(activity->{
                    try{
                        java.lang.reflect.Method planner=MainActivity.class.getDeclaredMethod(
                            "nextDueCadence",java.time.LocalDate.class);
                        planner.setAccessible(true);
                        com.quransafeguard.hifz.core.ScheduledCadence due=
                            (com.quransafeguard.hifz.core.ScheduledCadence)
                                planner.invoke(activity,HifzClock.today());
                        assertTrue("Legacy V1 import must NEVER invent historical backlog",
                            due==null || !due.getScheduledDate().isBefore(HifzClock.today()));
                    }catch(ReflectiveOperationException error){
                        throw new AssertionError("Actual MainActivity planner failed",error);
                    }
                });
            }
            assertTrue("V1 import must clear unrelated target J10 history",
                !legacy.contains(J10_SENTINEL));
            assertTrue("V1 import must not retain stale gates from the target install",
                context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE).getAll().isEmpty());
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

            // Leave a genuinely pending cross-store journal on disk for the
            // EXTERNAL ADB force-stop. Do NOT recover in this test process.
            org.json.JSONObject interrupted=new org.json.JSONObject(backup.toString());
            interrupted.getJSONObject("values").put("p0-incomplete-restore-probe",
                new org.json.JSONObject().put("t","s").put("v","new-state"));
            HifzBackup.failAfterStoreForTest="MAIN";
            try {
                HifzBackup.restore(context,interrupted);
                throw new AssertionError("No injected interruption before external force-stop");
            }catch(AssertionError expected){
                assertTrue(expected.getMessage(),
                    expected.getMessage().startsWith("Simulated process death after MAIN"));
            }finally{HifzBackup.failAfterStoreForTest=null;}
            assertTrue("Main store must contain the uncommitted new value",
                raw.contains("p0-incomplete-restore-probe"));
            assertTrue("A durable prewrite journal must await new process recovery",
                context.getSharedPreferences("quran_hifz_restore_journal_v1",
                    Context.MODE_PRIVATE).contains("pending_restore"));
        }catch(org.json.JSONException invalid){throw new AssertionError(invalid);}
    }

    private static void assertBackupLedgerAndJ10(Context context) {
        assertNotNull("M9 completion lost after JSON restore",
            new DashboardLedger(context).find(OLD_COMPLETION,HifzSessionActivity.SABQI));
        assertEquals("J10 historic review timestamp lost",
            J10_SENTINEL_DATE,context.getSharedPreferences(
                J10ReviewStore.NAME,Context.MODE_PRIVATE).getLong(J10_SENTINEL,-1));
        assertEquals("Legacy gate data must round-trip without reviving target data",
            "persist-me",context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE)
                .getString(GATE_SENTINEL,null));
    }

    @Test public void b_verifyFixtureAfterExternalForceStop() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue("Process-death gate was not prepared",
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).getBoolean(ARMED, false));
        try {
            // The full-suite run may still share one process; the external
            // two-phase runner kills the app before starting this verifier.
            HifzBackup.recoverIfNecessary(context);
            assertTrue("Interrupted import must revert staged main-store mutation",
                !context.getSharedPreferences(HIFZ_PREFS,Context.MODE_PRIVATE)
                    .contains("p0-incomplete-restore-probe"));
            assertTrue("Recovery must consume the pending journal",
                !context.getSharedPreferences("quran_hifz_restore_journal_v1",
                    Context.MODE_PRIVATE).contains("pending_restore"));
            assertPersistedFixture(context);
            assertBackupLedgerAndJ10(context);
        } finally {
            context.getSharedPreferences(GATE_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(LEDGER_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
            context.getSharedPreferences(LEGACY_GATES, Context.MODE_PRIVATE).edit().clear().commit();
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
