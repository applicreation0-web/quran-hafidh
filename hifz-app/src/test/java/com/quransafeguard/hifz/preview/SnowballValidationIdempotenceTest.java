package com.quransafeguard.hifz.preview;
import java.time.LocalDate;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
/** Real persisted protocols replace the old tests that expected date-only synthetic completion. */
public final class SnowballValidationIdempotenceTest {
    static GeometryRepository geo;
    @BeforeClass public static void setup() throws Exception {EarnedEveningProtocolRegressionTest.setup();geo=EarnedEveningProtocolRegressionTest.geometry;}
    static void morning(HifzPrefs p,ConsolidationCycleEngine.Family f,LocalDate day,int offset) {
        int start=geo.firstLineIndex(new com.quransafeguard.hifz.core.VerseRef(2,75))+offset;
        List<String> ids=new ArrayList<>();for(int i=0;i<5;i++)ids.add(geo.line(start+i).id);
        if(f==ConsolidationCycleEngine.Family.LEARNING)assertTrue(MorningProtocolFixture.completeSabqi(p,start,start+4,start+5,ids,day.toString(),"fixture"));
        else {
            List<GeometryRepository.LineMeta> m=geo.linesForExactIds(ids);
            com.quransafeguard.hifz.core.VerseRef first=m.get(0).verses.get(0),last=m.get(4).verses.get(m.get(4).verses.size()-1);
            assertTrue(MorningProtocolFixture.completeItqan(p,ids,p.itqanBlockIndex()+1,false,first,last,last,day.toString(),"fixture",Collections.emptyList()));
        }
    }
    static ConsolidationCycleEngine.Session open(HifzPrefs p,ConsolidationCycleEngine.Family f,LocalDate day) {
        List<ConsolidationCycleEngine.Unit> units=f==ConsolidationCycleEngine.Family.LEARNING?p.learningConsolidationUnits(day):p.stabilizedConsolidationUnits(day);
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();ConsolidationCycleEngine.Cycle c=null;
        String id=(f==ConsolidationCycleEngine.Family.LEARNING?"learning-":"stabilization-")+day;
        for(ConsolidationCycleEngine.Unit u:units)c=c==null?e.startCycle(id,f,u):e.addUnit(c,u);
        return e.openSession(c,"fixture-"+day);
    }
    static void close(HifzPrefs p,ConsolidationCycleEngine.Family f,LocalDate day,ConsolidationCycleEngine.Session s) {
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();assertTrue(p.persistConsolidationSession(s));
        while(!s.readyToClose()){s=e.recordRepetition(s);assertTrue(p.persistConsolidationSession(s));}
        assertTrue(p.completeSnowballEvening(f,day.toString(),s));
        assertFalse(p.completeSnowballEvening(f,day.toString(),s));
    }
    @Test public void mondayRenforcementThenTuesdayNeverRecreditsSameCumulativeLines() {
        InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.LEARNING;LocalDate mon=LocalDate.of(2026,10,5);
        morning(p,f,mon,0);close(p,f,mon,open(p,f,mon));
        assertFalse(new HifzPrefs(d).snowballEveningPending(f,mon.plusDays(1)));
        morning(p,f,mon.plusDays(2),5);assertTrue(p.snowballEveningPending(f,mon.plusDays(2)));
        close(p,f,mon.plusDays(2),open(p,f,mon.plusDays(2)));assertFalse(p.snowballEveningPending(f,mon.plusDays(3)));
    }
    @Test public void tueThuSatConsolidationSameVersusNewUnits() {
        InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.STABILIZATION;LocalDate tue=LocalDate.of(2026,10,6);
        for(int slot=0;slot<3;slot++){LocalDate day=tue.plusDays(slot*2);morning(p,f,day,slot*5);close(p,f,day,open(p,f,day));assertFalse(p.snowballEveningPending(f,day.plusDays(1)));}
    }
    @Test public void legacyDateOnlyCompletionNeverDeletesAnInterruptedOpen() {
        InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.LEARNING;LocalDate day=LocalDate.of(2026,10,5);
        morning(p,f,day,0);ConsolidationCycleEngine.Session s=open(p,f,day);assertTrue(p.persistConsolidationSession(s));
        Map<String,Object> before=new LinkedHashMap<>(d.disk);assertFalse(p.completeSnowballEvening(f,day.toString()));assertEquals(before,d.disk);
        assertNotNull(new HifzPrefs(d).restoreConsolidationSession(new ConsolidationCycleEngine(),f));
    }
    @Test public void pausedTuesdaySessionResumesThursdayWithFrozenUnitsAndLeavesNewBlockDue() {
        InMemoryPrefs d=EarnedEveningProtocolRegressionTest.disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.STABILIZATION;LocalDate tue=LocalDate.of(2026,10,6);
        morning(p,f,tue,0);ConsolidationCycleEngine.Session old=open(p,f,tue);assertTrue(p.persistConsolidationSession(old));
        String snapshot=p.frozenConsolidationSnapshot(f,"stabilization-"+tue);morning(p,f,tue.plusDays(2),5);
        p=new HifzPrefs(d);old=p.restoreConsolidationSession(new ConsolidationCycleEngine(),f);
        assertEquals(snapshot,p.frozenConsolidationSnapshot(f,"stabilization-"+tue));
        close(p,f,tue,old);assertEquals(tue.toString(),p.lastStabilizationSnowballEveningDate());
        assertTrue(p.snowballEveningPending(f,tue.plusDays(2)));
    }
}
