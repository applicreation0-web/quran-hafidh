package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Adversarial history/capacity test: >8 calendar weeks, >30 overdue frozen
 * physical blocks, repeated app restart, proof-only receipts, zero automatic Acquis.
 * Synthetic data only: never serialize a user's actual progress. */
public final class ItqanOverdueHistoryStress11711Test {
    private static final ConsolidationCycleEngine.Family STABILIZATION =
        ConsolidationCycleEngine.Family.STABILIZATION;

    private static InMemoryPrefs state(List<String> ids, List<String> stabilized) throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("schema",6);
        disk.disk.put("v6LearnedLineIds","[]");
        disk.disk.put("v6StabilizedLineIds",new JSONArray(stabilized).toString());
        disk.disk.put("v6AcquiredCreditLineIds","[]");
        disk.disk.put("v6QuarantineLineIds","[]");
        disk.disk.put("v6LegacyPartialAcquiredLineIds","[]");
        disk.disk.put("v6StabilizationEarnedEveningUnitsV1","[]");
        disk.disk.put("stabilizationSnowballWeekAnchor","2026-11-02");
        disk.disk.put("stabilizationSnowballUnitIds","[]");
        disk.disk.put("itqanCursor","53:1");
        JSONArray history=new JSONArray();
        for(int week=0;week<15;week++){
            LocalDate monday=LocalDate.of(2026,6,29).plusWeeks(week);
            JSONArray blocks=new JSONArray();
            for(int j=0;j<3;j++)blocks.put(ids.get(week*3+j));
            history.put(new JSONObject().put("week",monday.toString()).put("units",blocks));
        }
        disk.disk.put("stabilizationSnowball8WeekHistory",history.toString());
        return disk;
    }

    @Test public void fortyFiveOldBlocksSurviveCapacityWindowAndRequireRealReceipts() throws Exception {
        List<String> ids=new ArrayList<>();
        List<String> physical=new ArrayList<>();
        for(int i=0;i<45;i++){
            String first="overdue-"+i+"-a",second="overdue-"+i+"-b";
            physical.add(first);physical.add(second);
            ids.add(ConsolidationPhysicalUnitPolicy.encodeLineUnit(
                java.util.Arrays.asList(first,second)));
        }
        InMemoryPrefs disk=state(ids,physical);
        Map<String,Object> original=new LinkedHashMap<>(disk.disk);
        HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate day=LocalDate.of(2026,11,4);
        assertTrue(prefs.snowballEveningPending(STABILIZATION,day));
        assertEquals(30,prefs.stabilizedConsolidationUnits(day).size());
        assertEquals("Planning must be strictly read-only",original,disk.disk);

        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        Set<String> actuallyEarned=new LinkedHashSet<>();
        for(int round=0;round<2;round++){
            HifzPrefs restart=new HifzPrefs(disk);
            LocalDate now=day.plusDays(round);
            List<ConsolidationCycleEngine.Unit> due=restart.stabilizedConsolidationUnits(now);
            assertEquals(round==0?30:16,due.size()); // 30 x7; then 1 continuous + 15 x7
            ConsolidationCycleEngine.Cycle cycle=null;
            for(ConsolidationCycleEngine.Unit unit:due){
                cycle=cycle==null
                    ?engine.startCycle("stabilization-"+now,STABILIZATION,unit)
                    :engine.addUnit(cycle,unit);
            }
            ConsolidationCycleEngine.Session session=engine.openSession(cycle,"stress-"+now);
            assertTrue(restart.persistConsolidationSession(session));
            while(!session.readyToClose()) {
                session=engine.recordRepetition(session);
                assertTrue(restart.persistConsolidationSession(session));
            }
            assertTrue(restart.completeSnowballEvening(STABILIZATION,now.toString(),session));
            assertFalse("A verified cycle cannot be replayed",
                restart.completeSnowballEvening(STABILIZATION,now.toString(),session));
            assertEquals("53:1",disk.getString("itqanCursor",""));
            assertEquals("[]",disk.getString("v6AcquiredCreditLineIds","[]"));
            assertEquals("No regression to Appris","[]",disk.getString("v6LearnedLineIds","[]"));
            JSONArray receipts=new JSONArray(disk.getString("v6StabilizationEarnedEveningUnitsV1","[]"));
            for(int i=0;i<receipts.length();i++)actuallyEarned.add(receipts.getString(i));
            assertEquals(round==0?30:45,actuallyEarned.size());
        }
        assertTrue("Oldest work must not be dropped by the eight-week soft window",
            actuallyEarned.containsAll(ids));
        HifzPrefs end=new HifzPrefs(disk);
        assertFalse(end.snowballEveningPending(STABILIZATION,day.plusDays(2)));
        assertTrue(end.stabilizedConsolidationUnits(day.plusDays(2)).isEmpty());
        assertEquals("Never convert receipts directly to Acquis","[]",
            disk.getString("v6AcquiredCreditLineIds","[]"));
        // Sunday final remains bounded, without destroying overdue history.
        List<ConsolidationCycleEngine.Unit> sunday=end.stabilizationSnowballFinalUnits(
            LocalDate.of(2026,11,8));
        assertEquals(30,sunday.size()); // 29 blocks + continuous
        assertEquals("History is not silently discarded",
            15,new JSONArray(disk.getString("stabilizationSnowball8WeekHistory","[]")).length());
    }
}
