package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Two separately launched instrumentation processes with external am force-stop between them. */
public final class SundayFinalProcessDeathInstrumentedTest {
    private static final String STORE = "quran_hifz_preview_v1", GATE = "sunday_final_death_fixture";
    @Test public void prepare() {
        Context c = ApplicationProvider.getApplicationContext();
        SharedPreferences raw = c.getSharedPreferences(STORE,0), gate = c.getSharedPreferences(GATE,0);
        assertTrue(raw.edit().clear().commit()); assertTrue(gate.edit().clear().commit());
        HifzPrefs prefs = new HifzPrefs(c);
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        String unit = ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            GeometryRepository.get(c).lineIdsOnPage(1).subList(0, 2));
        for (ConsolidationCycleEngine.Family family : ConsolidationCycleEngine.Family.values()) {
            String stem = family == ConsolidationCycleEngine.Family.LEARNING ? "learning" : "stabilization";
            for (boolean finale : new boolean[]{false,true}) {
                String id = stem + (finale ? "-final-2026-10-25" : "-2026-10-24");
                ConsolidationCycleEngine.Protocol protocol = finale
                    ? ConsolidationCycleEngine.Protocol.SUNDAY_PAGE : ConsolidationCycleEngine.Protocol.SNOWBALL;
                ConsolidationCycleEngine.Session s = engine.openSession(engine.startCycle(id,family,
                    new ConsolidationCycleEngine.Unit(unit,protocol)),id+"-frozen");
                assertTrue(prefs.persistConsolidationSession(engine.recordRepetition(s)));
            }
        }
        SharedPreferences.Editor e = gate.edit();
        for (Map.Entry<String,?> entry : raw.getAll().entrySet()) {
            if (entry.getKey().startsWith("v6Consolidation")) e.putString(entry.getKey(), (String)entry.getValue());
        }
        assertTrue(e.putBoolean("armed",true).commit());
        assertEquals(5,gate.getAll().size());
    }
    @Test public void verify() {
        Context c = ApplicationProvider.getApplicationContext();
        SharedPreferences gate = c.getSharedPreferences(GATE,0), raw = c.getSharedPreferences(STORE,0);
        assertTrue("prepare phase must precede external force-stop",gate.getBoolean("armed",false));
        HifzPrefs prefs = new HifzPrefs(c);
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        for (Map.Entry<String,?> e : gate.getAll().entrySet()) {
            if (!"armed".equals(e.getKey())) assertEquals(e.getValue(),raw.getString(e.getKey(),null));
        }
        for (ConsolidationCycleEngine.Family family : ConsolidationCycleEngine.Family.values()) {
            String stem = family == ConsolidationCycleEngine.Family.LEARNING ? "learning" : "stabilization";
            for (boolean finale : new boolean[]{false,true}) {
                String id = stem + (finale ? "-final-2026-10-25" : "-2026-10-24");
                ConsolidationCycleEngine.Session s = prefs.restoreConsolidationSession(engine,family,id);
                assertNotNull(s); assertTrue(s.open()); assertEquals(1,s.donePerStage());
                assertEquals(finale ? ConsolidationCycleEngine.Protocol.SUNDAY_PAGE : ConsolidationCycleEngine.Protocol.SNOWBALL,
                    s.cycle().units().get(0).protocol());
            }
        }
        assertTrue(gate.edit().clear().commit());
    }
}
