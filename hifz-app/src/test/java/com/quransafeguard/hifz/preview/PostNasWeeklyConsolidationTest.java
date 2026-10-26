package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** New after-An-Nas weekly recall is separate from old finale credits and is not a seventh lesson. */
public final class PostNasWeeklyConsolidationTest {
    @Test public void onlyActualWeeklyMaintiensAreReadOnceOnLastItqanDayWithoutCredits() throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("p4ItqanRotationInitialTailCompleted",true);
        disk.disk.put("postNasMaintainedWeekAnchorV1","2026-10-26");
        String first=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p520-l01","p520-l02"));
        String second=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p521-l01","p521-l02"));
        String third=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p522-l01","p522-l02"));
        disk.disk.put("postNasMaintainedHizbsV1",new JSONArray().put(first).put(second).put(third).toString());
        disk.disk.put("v6AcquiredCreditLineIds",new JSONArray().put("p100-l01").toString());
        LocalDate thursday=LocalDate.of(2026,10,29),saturday=LocalDate.of(2026,10,31);
        HifzPrefs prefs=new HifzPrefs(disk);
        assertTrue(prefs.itqanPostNasMaintenance());
        assertFalse("No repeated cumulative Consolidation during the week",
            prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,thursday));
        assertTrue(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,saturday));
        List<ConsolidationCycleEngine.Unit> units=prefs.stabilizedConsolidationUnits(saturday);
        assertEquals(3,units.size());
        assertEquals(first,units.get(0).id());
        assertEquals(second,units.get(1).id());
        assertEquals(third,units.get(2).id());
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("stabilization-2026-10-31",
            ConsolidationCycleEngine.Family.STABILIZATION,units.get(0));
        cycle=engine.addUnit(cycle,units.get(1));
        cycle=engine.addUnit(cycle,units.get(2));
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,"weekly-hizbs");
        assertTrue(prefs.persistConsolidationSession(session));
        session=engine.recordRepetition(session);
        assertTrue(prefs.persistConsolidationSession(session));
        HifzPrefs restarted=new HifzPrefs(disk);
        ConsolidationCycleEngine.Session resumed=restarted.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-2026-10-31");
        assertNotNull("Masked repetition is preserved after restart",resumed);
        assertEquals(1,resumed.nextUnitIndex());
        resumed=engine.recordRepetition(resumed);
        resumed=engine.recordRepetition(resumed);
        assertTrue(resumed.readyToClose());
        assertTrue(restarted.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION,
            "2026-10-31",resumed));
        assertFalse("Weekly pass cannot be credited twice",
            restarted.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,saturday));
        assertEquals("No acquired credit from an ordinary masked weekly pass",
            new JSONArray().put("p100-l01").toString(),disk.getString("v6AcquiredCreditLineIds",""));
        assertEquals("No new Sunday Itqan finale","",disk.getString("lastRecentSabqiReviewDate",""));
        assertFalse("A new week must never replay the previous week's Maintiens",
            restarted.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,
                LocalDate.of(2026,11,7)));
    }
}
