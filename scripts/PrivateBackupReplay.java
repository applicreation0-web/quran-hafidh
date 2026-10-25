package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import org.json.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Local-only runner: args = external backup path, canonical geometry path.
 * Never copy a personal fixture or its values into CI, Git or the output log.
 * Uses production restore/export, session codecs and QCF geometry; only the
 * Android preferences storage boundary is replaced by InMemoryPrefs.
 */
public final class PrivateBackupReplay {
    public static final class MemoryContext extends ContextWrapper {
        Map<String, InMemoryPrefs> stores;
        private MemoryContext() { super(null); }
        static MemoryContext create() throws Exception {
            // Android's JVM mockable Context constructor throws. Allocate only this
            // test context without invoking that stub; every used operation is overridden.
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            MemoryContext c = (MemoryContext)((sun.misc.Unsafe)f.get(null)).allocateInstance(MemoryContext.class);
            c.stores = new LinkedHashMap<>();
            return c;
        }
        @Override public SharedPreferences getSharedPreferences(String name, int mode) {
            return stores.computeIfAbsent(name, k -> new InMemoryPrefs());
        }
        @Override public Context getApplicationContext() { return this; }
    }
    private static JSONObject stableExport(Context c) throws Exception {
        JSONObject out = HifzBackup.export(c); out.remove("exportedAtEpochMs"); return out;
    }
    private static void same(JSONObject a, JSONObject b) {
        assertTrue("Unexpected change in full typed backup", a.similar(b));
    }
    public static void main(String[] args) throws Exception {
        JSONObject original = new JSONObject(Files.readString(Path.of(args[0])));
        assertEquals(3, original.getInt("version"));
        assertEquals(6, original.getJSONObject("values").getJSONObject("schema").getInt("v"));
        JSONObject expected = new JSONObject(original.toString()); expected.remove("exportedAtEpochMs");
        MemoryContext c = MemoryContext.create();
        assertTrue(HifzBackup.restore(c, new JSONObject(original.toString())) > 0);
        same(expected, stableExport(c));
        System.out.println("PASS full V3 round-trip: all five stores, typed values, dates, cursors, journals and snapshots unchanged");
        InMemoryPrefs db = c.stores.get("quran_hifz_preview_v1");
        HifzPrefs prefs = new HifzPrefs(db);
        HifzPrefs.ProgressionSnapshot snapshot = prefs.progressionSnapshotV6();
        assertEquals(309, snapshot.acquired.size()); assertEquals(1098, snapshot.learned.size());
        assertEquals(21, snapshot.stabilized.size());
        assertTrue(Collections.disjoint(snapshot.acquired, snapshot.learned));
        assertTrue(Collections.disjoint(snapshot.acquired, snapshot.stabilized));
        assertTrue(Collections.disjoint(snapshot.learned, snapshot.stabilized));
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        String learningRaw = db.getString("v6ConsolidationLearningState", null);
        String finalRaw = db.getString("v6ConsolidationStabilizationState", null);
        assertNotNull(learningRaw); assertNotNull(finalRaw);
        JSONObject learning = new JSONObject(learningRaw), finale = new JSONObject(finalRaw);
        assertEquals("2026-10-24", prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING, "learning-"));
        assertEquals("2026-10-25", prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION, "stabilization-final-"));
        assertEquals(learningRaw, db.getString("v6ConsolidationLearningState", null));
        assertFalse(db.contains("v6ConsolidationStabilizationState"));
        assertEquals(finalRaw, db.getString("v6ConsolidationStabilizationStateFinalV1", null));
        for (JSONObject raw : new JSONObject[]{learning, finale}) {
            ConsolidationCycleEngine.Family family = ConsolidationCycleEngine.Family.valueOf(raw.getString("family"));
            ConsolidationCycleEngine.Session restored = new HifzPrefs(db).restoreConsolidationSession(engine, family, raw.getString("cycleId"));
            assertNotNull(restored); assertTrue(restored.open());
            assertEquals(raw.getInt("sessionGroupSize"), restored.sessionGroupSize());
            assertEquals(raw.getInt("stage"), restored.stage());
            assertEquals(raw.getInt("nextUnitIndex"), restored.nextUnitIndex());
            assertEquals(raw.getInt("donePerStage"), restored.donePerStage());
            assertEquals(raw.getString("sourceSnapshot"), prefs.frozenConsolidationSnapshot(family, raw.getString("cycleId")));
            for (int i = 0; i < restored.cycle().units().size(); i++) {
                assertEquals(raw.getJSONArray("units").getJSONObject(i).getString("protocol"), restored.cycle().units().get(i).protocol().name());
                assertEquals(raw.getJSONArray("units").getJSONObject(i).getString("id"), restored.cycle().units().get(i).id());
            }
        }
        JSONObject expectedMoved = new JSONObject(expected.toString());
        JSONObject values = expectedMoved.getJSONObject("values");
        values.put("v6ConsolidationStabilizationStateFinalV1", values.remove("v6ConsolidationStabilizationState"));
        same(expectedMoved, stableExport(c));
        System.out.println("PASS legacy OPEN migration: only existing finale slot moved; SNOWBALL/SNOWBALL_EXTENDED and sourceSnapshot exact");
        GeometryRepository g = GeometryRepository.fromJson(Files.readString(Path.of(args[1])));
        Map<String, ?> beforeRead = db.getAll();
        prefs.learningSnowballFinalUnits(java.time.LocalDate.of(2026,10,25), g);
        prefs.stabilizationSnowballFinalUnits(java.time.LocalDate.of(2026,10,25), g);
        assertEquals("Final projections must be read-only", beforeRead, db.getAll());
        for (String store : new String[]{"MAIN", "LEDGER", "J10", "GATES", "FREE_MEM"}) {
            JSONObject before = stableExport(c);
            JSONObject changed = new JSONObject(original.toString());
            changed.getJSONObject("values").put("private-replay-probe", new JSONObject().put("t","s").put("v","probe"));
            HifzBackup.failAfterStoreForTest = store;
            try { HifzBackup.restore(c, changed); fail("Injected interruption must occur"); }
            catch (AssertionError interrupted) {
                // Production recovery journal survives the simulated dead call stack.
                assertTrue(c.getSharedPreferences("quran_hifz_restore_journal_v1",0).contains("pending_restore"));
            } finally { HifzBackup.failAfterStoreForTest = null; }
            HifzBackup.recoverIfNecessary(c);
            same(before, stableExport(c));
        }
        System.out.println("PASS five interruption boundaries: complete rollback without loss or invented credit");
        System.out.println("PASS private fixture: acquired=309 learned=1098 stabilized=21, no overlap");
    }
}
