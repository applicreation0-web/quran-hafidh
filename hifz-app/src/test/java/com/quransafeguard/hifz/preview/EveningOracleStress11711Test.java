package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent oracle: only lesson + every persisted repetition can create an evening receipt. */
public final class EveningOracleStress11711Test {
    static final long SEED=0x6110611711L;
    static final ConsolidationCycleEngine.Family F=ConsolidationCycleEngine.Family.LEARNING;
    static InMemoryPrefs exactWednesday() {
        InMemoryPrefs d=EveningMutationGate11711Test.disk();
        d.disk.put("sabqiStart","1:1");d.disk.put("sabqiEnd","114:6");
        d.disk.put("v6LearnedLineIds",new JSONArray(ConsolidationPhysicalUnitPolicy.decodeLineUnit(EveningMutationGate11711Test.WED)).toString());
        d.disk.put("v6LearningEarnedEveningUnitsV1",new JSONArray().put(EveningMutationGate11711Test.WED).toString());
        d.disk.put("v6LastLearningSnowballCompletedSnapshotV1","2026-11-02|"+new JSONArray().put(EveningMutationGate11711Test.WED));
        return d;
    }
    static boolean morning(HifzPrefs p,LocalDate day,int cursor) {
        if(p.sabqiLineCursor()<0)p.setSabqiLineCursor(cursor);
        List<String> lines=new ArrayList<>();
        try {if(EarnedEveningProtocolRegressionTest.geometry==null)EarnedEveningProtocolRegressionTest.setup();}
        catch(Exception error){throw new AssertionError(error);}
        for(int i=0;i<5;i++)lines.add(EarnedEveningProtocolRegressionTest.geometry.line(cursor+i).id);
        return MorningProtocolFixture.completeSabqi(p,cursor,cursor+4,cursor+5,lines,day.toString(),"anonymous protocol");
    }
    static ConsolidationCycleEngine.Session open(HifzPrefs p,LocalDate day,String sessionId) {
        List<ConsolidationCycleEngine.Unit> units=p.learningConsolidationUnits(day);
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle c=e.startCycle("learning-"+day,F,units.get(0));
        for(int i=1;i<units.size();i++)c=e.addUnit(c,units.get(i));
        return e.openSession(c,sessionId);
    }
    static ConsolidationCycleEngine.Session finish(HifzPrefs p,ConsolidationCycleEngine.Session s) {
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();
        assertTrue(p.persistConsolidationSession(s));
        while(!s.readyToClose()) {s=e.recordRepetition(s);assertTrue(p.persistConsolidationSession(s));}
        return s;
    }
    @Test public void T01toT05ExactFridayLessonThenRealEveningAndNewSameDayBlock() throws Exception {
        InMemoryPrefs d=exactWednesday();HifzPrefs p=new HifzPrefs(d);LocalDate day=EveningMutationGate11711Test.FRIDAY;
        Map<String,Object> initial=new LinkedHashMap<>(d.disk);
        assertFalse(p.completeSnowballEvening(F,day.toString()));assertEquals(initial,d.disk);
        assertEquals("2026-11-04",p.lastLearningSnowballEveningDate());
        assertTrue(morning(p,day,100));p=new HifzPrefs(d);
        assertTrue(p.snowballEveningPending(F,day));assertEquals("2026-11-04",p.lastLearningSnowballEveningDate());
        ConsolidationCycleEngine.Session s=finish(p,open(p,day,"real-friday"));
        assertTrue(p.completeSnowballEvening(F,day.toString(),s));
        Map<String,Object> once=new LinkedHashMap<>(d.disk);
        assertFalse(p.completeSnowballEvening(F,day.toString(),s));assertEquals(once,d.disk);
        assertEquals(day.toString(),new HifzPrefs(d).lastLearningSnowballEveningDate());
        assertTrue(morning(p,day,105));assertTrue(p.snowballEveningPending(F,day));
        assertFalse(p.completeSnowballEvening(F,day.toString(),s));
        s=finish(p,open(p,day,"second-friday"));assertTrue(p.completeSnowballEvening(F,day.toString(),s));
        assertEquals(3,new JSONArray(d.getString("v6LearningEarnedEveningUnitsV1","[]")).length());
        assertEquals(15,p.progressionSnapshotV6().learned.size());assertTrue(p.progressionSnapshotV6().acquired.isEmpty());
    }
    @Test public void tenThousandReplayableTracesAgainstIndependentBooleanOracle() throws Exception {
        Random random=new Random(SEED);long actions=0;long started=System.nanoTime();
        for(int trace=0;trace<10000;trace++) {
            InMemoryPrefs d=exactWednesday();HifzPrefs p=new HifzPrefs(d);
            LocalDate day=EveningMutationGate11711Test.FRIDAY.plusWeeks(random.nextInt(8));
            String anchor=day.minusDays(4).toString();
            d.disk.put("learningSnowballWeekAnchor",anchor);
            d.disk.put("lastSabqiDate",day.minusDays(2).toString());
            d.disk.put("lastLearningSnowballEveningDate",day.minusDays(2).toString());
            d.disk.put("v6LastLearningSnowballCompletedSnapshotV1",anchor+"|"+new JSONArray().put(EveningMutationGate11711Test.WED));
            boolean lesson=random.nextBoolean();boolean full=random.nextBoolean();boolean persist=random.nextBoolean();
            // Independent oracle does not call production planners or status functions.
            boolean expected=lesson&&full&&persist;
            if(lesson) {assertTrue("seed="+SEED+" trace="+trace,morning(p,day,100));actions+=38;}
            ConsolidationCycleEngine.Session s=open(p,day,"seed-"+SEED+"-trace-"+trace);
            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
            if(persist)assertTrue(p.persistConsolidationSession(s));
            while(!s.readyToClose()) {
                ConsolidationCycleEngine.Session next=engine.recordRepetition(s);
                if(!full&&next.readyToClose())break;
                s=next;actions++;
                if(persist)assertTrue(p.persistConsolidationSession(s));
                if(random.nextInt(7)==0) {
                    d=PartialFinalEightWeekStressTest.backupRoundTrip(d);p=new HifzPrefs(d);actions++;
                    if(persist)s=p.restoreConsolidationSession(engine,F);
                }
            }
            Map<String,Object> before=new LinkedHashMap<>(d.disk);boolean actual=false;
            try {actual=p.completeSnowballEvening(F,day.toString(),s);}catch(IllegalStateException incomplete){assertFalse(full);}
            assertEquals("seed="+SEED+" trace="+trace,expected,actual);actions++;
            if(!expected)assertEquals("refused trace must not mutate any preference: "+trace,before,d.disk);
            else {
                Map<String,Object> committed=new LinkedHashMap<>(d.disk);
                for(int replay=0;replay<2;replay++)assertFalse(p.completeSnowballEvening(F,day.toString(),s));
                assertEquals(committed,d.disk);assertEquals(day.toString(),p.lastLearningSnowballEveningDate());
            }
            assertTrue(p.progressionSnapshotV6().acquired.isEmpty());
        }
        System.out.println("ORACLE_STRESS seed="+SEED+" traces=10000 actions="+actions+" failures=0 ms="+(System.nanoTime()-started)/1000000);
    }
    @Test public void thousandConcurrentCloseTransitionsHaveExactlyOneWinner() throws Exception {
        ExecutorService threads=Executors.newFixedThreadPool(2);long start=System.nanoTime();
        try {
            for(int pair=0;pair<500;pair++) {
                InMemoryPrefs d=exactWednesday();HifzPrefs p=new HifzPrefs(d);LocalDate day=EveningMutationGate11711Test.FRIDAY;
                assertTrue(morning(p,day,100));
                ConsolidationCycleEngine.Session s=finish(p,open(p,day,"parallel-"+pair));
                CountDownLatch gate=new CountDownLatch(1);
                Callable<Boolean> close=()->{gate.await();return new HifzPrefs(d).completeSnowballEvening(F,day.toString(),s);};
                Future<Boolean> a=threads.submit(close),b=threads.submit(close);gate.countDown();
                assertEquals(1,(a.get()?1:0)+(b.get()?1:0));
                assertEquals(2,new JSONArray(d.getString("v6LearningEarnedEveningUnitsV1","[]")).length());
                assertTrue(new HifzPrefs(d).progressionSnapshotV6().acquired.isEmpty());
            }
        } finally {threads.shutdownNow();}
        System.out.println("CONCURRENT_STRESS transitions=1000 pairs=500 duplicates=0 ms="+(System.nanoTime()-start)/1000000);
    }
}
