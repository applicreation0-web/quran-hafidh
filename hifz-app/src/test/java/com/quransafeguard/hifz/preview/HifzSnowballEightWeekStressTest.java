package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import static org.junit.Assert.*;

/** Adversarial tests for skipped lessons, actual evening receipts, physical order,
 * frozen restarts, partial Sunday finals and all-parts reading.
 * ALL state is synthetic or initialized by the real fixture protocol: no personal data. */
public final class HifzSnowballEightWeekStressTest {
    private static final ConsolidationCycleEngine.Family ITQAN =
        ConsolidationCycleEngine.Family.STABILIZATION;
    private static final ConsolidationCycleEngine.Family SABQI =
        ConsolidationCycleEngine.Family.LEARNING;
    private static final LocalDate THIS_WEDNESDAY=LocalDate.of(2026,11,4);
    private static final LocalDate WEEK_MONDAY=LocalDate.of(2026,11,2);
    private static GeometryRepository geometry;

    @BeforeClass public static void loadRealQcf() throws Exception {
        SnowballValidationIdempotenceTest.setup();
        geometry=SnowballValidationIdempotenceTest.geo;
    }

    private static InMemoryPrefs disk() {
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("schema",6);
        for(String key:Arrays.asList("v6LearnedLineIds","v6StabilizedLineIds",
                "v6AcquiredCreditLineIds","v6QuarantineLineIds",
                "v6LegacyPartialAcquiredLineIds"))
            d.disk.put(key,"[]");
        d.disk.put("v6StabilizationEarnedEveningUnitsV1","[]");
        d.disk.put("v6LearningEarnedEveningUnitsV1","[]");
        d.disk.put("stabilizationSnowballWeekAnchor",WEEK_MONDAY.toString());
        d.disk.put("stabilizationSnowballUnitIds","[]");
        d.disk.put("stabilizationSnowball8WeekHistory","[]");
        d.disk.put("learningSnowball8WeekHistory","[]");
        d.disk.put("itqanCursor","53:1");
        return d;
    }

    private static List<String> physical(int page,int first,int last) {
        ArrayList<String> ids=new ArrayList<>();
        for(int i=first;i<=last;i++)ids.add(page+":"+i);
        return ids;
    }

    private static String encoded(List<String> lines) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(lines);
    }

    private static int totalLines(List<ConsolidationCycleEngine.Unit> units) {
        Set<String> ids=new LinkedHashSet<>();
        for(ConsolidationCycleEngine.Unit unit:units)
            ids.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(unit.id()));
        return ids.size();
    }

    private static ConsolidationCycleEngine.Session open(
            HifzPrefs prefs,ConsolidationCycleEngine.Family family,
            LocalDate date,List<ConsolidationCycleEngine.Unit> units) {
        String prefix=family==ITQAN?"stabilization-":"learning-";
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=null;
        for(ConsolidationCycleEngine.Unit unit:units)
            cycle=cycle==null?engine.startCycle(prefix+date,family,unit)
                :engine.addUnit(cycle,unit);
        assertNotNull(cycle);
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,
            "stress-"+family+"-"+date);
        assertTrue(prefs.persistConsolidationSession(session));
        return session;
    }

    private static ConsolidationCycleEngine.Session finishWithRestarts(
            InMemoryPrefs disk,ConsolidationCycleEngine.Family family,
            LocalDate date,ConsolidationCycleEngine.Session session) {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        int n=0,prior=0;
        while(!session.readyToClose()){
            session=engine.recordRepetition(session);
            assertTrue(new HifzPrefs(disk).persistConsolidationSession(session));
            n++;
            if(n%11==0 && !session.readyToClose()){
                ConsolidationCycleEngine.Session restored=new HifzPrefs(disk)
                    .restoreConsolidationSession(engine,family,
                        (family==ITQAN?"stabilization-":"learning-")+date);
                assertNotNull(restored);
                assertEquals(session.nextUnitIndex(),restored.nextUnitIndex());
                assertEquals(session.stage(),restored.stage());
                assertEquals(session.donePerStage(),restored.donePerStage());
                session=restored;
                prior++;
            }
            assertTrue("Cycle must terminate",n<400);
        }
        assertTrue(prior>=0);
        return session;
    }

    @Test public void itqanOneToEightWeeksOfMissedEveningsNeverDropOrInventReceipts()
            throws Exception {
        for(int weeks=1;weeks<=8;weeks++){
            InMemoryPrefs d=disk();
            JSONArray history=new JSONArray();
            JSONArray lines=new JSONArray();
            ArrayList<String> originalBlocks=new ArrayList<>();
            for(int week=0;week<weeks;week++){
                LocalDate monday=WEEK_MONDAY.minusWeeks(weeks-week);
                JSONArray units=new JSONArray();
                int page=500+week;
                for(int i=0;i<3;i++){
                    List<String> block=physical(page,i*5,i*5+4);
                    String id=encoded(block);
                    units.put(id);originalBlocks.add(id);
                    for(String l:block)lines.put(l);
                }
                history.put(new JSONObject().put("week",monday.toString()).put("units",units));
            }
            d.disk.put("stabilizationSnowball8WeekHistory",history.toString());
            d.disk.put("v6StabilizedLineIds",lines.toString());
            HifzPrefs prefs=new HifzPrefs(d);
            Map<String,Object> before=new LinkedHashMap<>(d.disk);
            List<ConsolidationCycleEngine.Unit> plan=prefs.stabilizedConsolidationUnits(THIS_WEDNESDAY);
            assertEquals("Week count "+weeks,3*weeks+1,plan.size());
            assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS,
                plan.get(plan.size()-1).protocol());
            assertEquals("Must read every genuinely stabilized line",15*weeks,
                ConsolidationPhysicalUnitPolicy.decodeLineUnit(
                    plan.get(plan.size()-1).id()).size());
            for(int i=0;i<originalBlocks.size();i++){
                assertEquals(originalBlocks.get(i),plan.get(i).id());
                assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN,
                    plan.get(i).protocol());
            }
            assertEquals("Planning must be read-only",before,d.disk);
            ConsolidationCycleEngine.Session active=open(prefs,ITQAN,THIS_WEDNESDAY,plan);
            active=finishWithRestarts(d,ITQAN,THIS_WEDNESDAY,active);
            assertEquals("No credit before actual verified close", "[]",
                d.getString("v6StabilizationEarnedEveningUnitsV1","[]"));
            assertTrue(new HifzPrefs(d).completeSnowballEvening(
                ITQAN,THIS_WEDNESDAY.toString(),active));
            assertEquals(3*weeks,new JSONArray(
                d.getString("v6StabilizationEarnedEveningUnitsV1","[]")).length());
            assertEquals("No invented final Acquis","[]",
                d.getString("v6AcquiredCreditLineIds","[]"));
            assertEquals("No cursor jumps","53:1",d.getString("itqanCursor",""));
            assertFalse("Paid history cannot reopen on Friday",
                new HifzPrefs(d).snowballEveningPending(
                    ITQAN,THIS_WEDNESDAY.plusDays(2)));
            assertFalse("A paid block cannot reenter the queue",
                new HifzPrefs(d).completeSnowballEvening(
                    ITQAN,THIS_WEDNESDAY.toString(),active));
        }
    }

    @Test public void randomizedOutOfOrderOldAndNewItqanIsCanonicalAndProofPreserving()
            throws Exception {
        for(int seed=1;seed<=128;seed++){
            Random random=new Random(seed);
            InMemoryPrefs d=disk();
            JSONArray current=new JSONArray();
            ArrayList<String> shuffled=new ArrayList<>();
            ArrayList<String> full=new ArrayList<>();
            JSONArray credits=new JSONArray();
            JSONArray stabilized=new JSONArray();
            for(int i=0;i<3;i++){
                List<String> physical=physical(526,i*3,i*3+2);
                String id=encoded(physical);
                shuffled.add(id);
                full.addAll(physical);
                for(String line:physical)stabilized.put(line);
                // Mixed genuine prior receipts and genuinely due units.
                if((seed & (1<<i))!=0)credits.put(id);
            }
            Collections.shuffle(shuffled,random);
            for(String id:shuffled)current.put(id);
            d.disk.put("stabilizationSnowballUnitIds",current.toString());
            d.disk.put("v6StabilizedLineIds",stabilized.toString());
            d.disk.put("v6StabilizationEarnedEveningUnitsV1",credits.toString());
            HifzPrefs prefs=new HifzPrefs(d);
            Map<String,Object> before=new LinkedHashMap<>(d.disk);
            List<ConsolidationCycleEngine.Unit> plan=prefs.stabilizedConsolidationUnits(THIS_WEDNESDAY);
            if(credits.length()==3){
                assertTrue("Nothing newly due means no new cycle",plan.isEmpty());
            } else {
                assertFalse(plan.isEmpty());
                assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS,
                    plan.get(plan.size()-1).protocol());
                assertEquals(full,ConsolidationPhysicalUnitPolicy.decodeLineUnit(
                    plan.get(plan.size()-1).id()));
                Set<String> unique=new LinkedHashSet<>();
                for(int i=0;i<plan.size()-1;i++){
                    ConsolidationCycleEngine.Unit u=plan.get(i);
                    unique.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(u.id()));
                }
                assertEquals(full,new ArrayList<>(unique));
            }
            assertEquals(before,d.disk);
        }
    }

    @Test public void sabqiSkippedMorningDoesNotCreateEveningOrSundayCredit()
            throws Exception {
        InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();
        HifzPrefs p=new HifzPrefs(d);
        LocalDate monday=LocalDate.of(2026,10,5);
        assertEquals(0,p.learningConsolidationUnits(monday).size());
        assertTrue(p.learningSnowballFinalUnits(monday.plusDays(6),geometry).isEmpty());
        assertFalse(p.snowballEveningPending(SABQI,monday));
        assertEquals("[]",d.getString("v6LearningEarnedEveningUnitsV1","[]"));
    }

    @Test public void sabqiMorningGapsOfOneToEightWeeksPreserveTheNextPhysicalLesson()
            throws Exception {
        for(int weeks=1;weeks<=8;weeks++){
            InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();
            HifzPrefs p=new HifzPrefs(d);
            LocalDate first=LocalDate.of(2026,10,5);
            SnowballValidationIdempotenceTest.morning(p,SABQI,first,0);
            List<ConsolidationCycleEngine.Unit> firstPlan=p.learningConsolidationUnits(first);
            assertEquals(1,firstPlan.size());
            LocalDate next=first.plusWeeks(weeks);
            // Every skipped calendar window must leave the next physical block unchanged.
            String last=p.lastSabqiDate();
            assertEquals(first.toString(),last);
            assertEquals(1,p.learningConsolidationUnits(next).size());
            assertEquals(firstPlan.get(0).id(),p.learningConsolidationUnits(next).get(0).id());
            // A dated real lesson is required before starting a new snowball.
            SnowballValidationIdempotenceTest.morning(p,SABQI,next,5);
            List<ConsolidationCycleEngine.Unit> plan=p.learningConsolidationUnits(next);
            assertEquals(3,plan.size());
            assertEquals(10,totalLines(plan));
            ConsolidationCycleEngine.Session session=open(p,SABQI,next,plan);
            session=finishWithRestarts(d,SABQI,next,session);
            assertTrue(p.completeSnowballEvening(SABQI,next.toString(),session));
            assertEquals(2,new JSONArray(
                d.getString("v6LearningEarnedEveningUnitsV1","[]")).length());
            assertEquals("[]",d.getString("v6AcquiredCreditLineIds","[]"));
            assertFalse(p.snowballEveningPending(SABQI,next.plusDays(1)));
        }
    }

    @Test public void sundayFinalIsPartialForZeroFiveTenFifteenActualSabqiLines()
            throws Exception {
        LocalDate monday=LocalDate.of(2026,10,5);
        for(int actual=0;actual<=3;actual++){
            InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();
            HifzPrefs p=new HifzPrefs(d);
            for(int i=0;i<actual;i++){
                LocalDate day=monday.plusDays(i*2);
                SnowballValidationIdempotenceTest.morning(p,SABQI,day,i*5);
                SnowballValidationIdempotenceTest.close(p,SABQI,day,
                    SnowballValidationIdempotenceTest.open(p,SABQI,day));
            }
            List<ConsolidationCycleEngine.Unit> sunday=
                p.learningSnowballFinalUnits(monday.plusDays(6),geometry);
            assertEquals("Sunday must use only verified weekday lines",actual*5,totalLines(sunday));
            assertEquals("Sunday planning must not award Acquis","[]",
                d.getString("v6AcquiredCreditLineIds","[]"));
        }
    }

    @Test public void itqanSundayFinalIsPartialForZeroEightFifteenTwentyTwoLines()
            throws Exception {
        ArrayList<String> physical=new ArrayList<>();
        int start=geometry.firstLineIndex(new VerseRef(2,75));
        for(int i=0;i<22;i++)physical.add(geometry.line(start+i).id);
        int[] stops={0,8,15,22};
        for(int stop:stops){
            InMemoryPrefs d=disk();
            JSONArray week=new JSONArray(),receipts=new JSONArray(),stabilized=new JSONArray();
            int begin=0;
            for(int end:new int[]{8,15,22}){
                if(end>stop)break;
                String id=encoded(physical.subList(begin,end));
                week.put(id);receipts.put(id);
                for(int i=begin;i<end;i++)stabilized.put(physical.get(i));
                begin=end;
            }
            d.disk.put("stabilizationSnowball8WeekHistory",new JSONArray()
                .put(new JSONObject().put("week",WEEK_MONDAY.toString())
                    .put("units",week)).toString());
            d.disk.put("v6StabilizedLineIds",stabilized.toString());
            d.disk.put("v6StabilizationEarnedEveningUnitsV1",receipts.toString());
            HifzPrefs p=new HifzPrefs(d);
            List<ConsolidationCycleEngine.Unit> sunday=
                p.stabilizationSnowballFinalUnits(WEEK_MONDAY.plusDays(6),geometry);
            assertEquals(stop,totalLines(sunday));
            assertEquals("No artificial Sunday credit","[]",d.getString("v6AcquiredCreditLineIds","[]"));
        }
    }

    @Test public void aForgedContinuousPassMustNotGrantAnyNewItqanReceipt()
            throws Exception {
        InMemoryPrefs d=disk();
        String a=encoded(physical(526,0,2));
        String b=encoded(physical(526,3,5));
        d.disk.put("stabilizationSnowballUnitIds",new JSONArray().put(a).put(b).toString());
        d.disk.put("v6StabilizedLineIds",new JSONArray(physical(526,0,5)).toString());
        HifzPrefs prefs=new HifzPrefs(d);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        // Fraudulent plan attempts to claim paid ×1 for a never-earned first block.
        ConsolidationCycleEngine.Cycle forged=engine.startCycle(
            "stabilization-"+THIS_WEDNESDAY,ITQAN,
            new ConsolidationCycleEngine.Unit(a,
                ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS));
        forged=engine.addUnit(forged,new ConsolidationCycleEngine.Unit(b,
            ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        forged=engine.addUnit(forged,new ConsolidationCycleEngine.Unit(
            ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(Arrays.asList(a,b)),
            ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS));
        ConsolidationCycleEngine.Session session=engine.openSession(forged,"forged");
        assertTrue(prefs.persistConsolidationSession(session));
        while(!session.readyToClose()){
            session=engine.recordRepetition(session);
            assertTrue(prefs.persistConsolidationSession(session));
        }
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("Forged old ×1 cannot satisfy a missing ×7 receipt",
            prefs.completeSnowballEvening(ITQAN,THIS_WEDNESDAY.toString(),session));
        assertEquals(before,d.disk);
    }
}
