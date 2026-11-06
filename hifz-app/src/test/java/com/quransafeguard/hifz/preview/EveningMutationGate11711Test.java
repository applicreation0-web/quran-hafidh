package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

/** Anonymous physical-line reproductions; these call the actual persistence endpoints. */
public final class EveningMutationGate11711Test {
    static final ConsolidationCycleEngine.Family LEARNING=ConsolidationCycleEngine.Family.LEARNING;
    static final LocalDate FRIDAY=LocalDate.of(2026,11,6);
    static final String WED=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("525:0","525:1","525:2","525:3","525:4"));
    static InMemoryPrefs disk() {
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("schema",6);
        for(String key:Arrays.asList("v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds","v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"))d.disk.put(key,"[]");
        d.disk.put("learningSnowballWeekAnchor","2026-11-02");
        d.disk.put("learningSnowballUnitIds",new JSONArray().put(WED).toString());
        d.disk.put("lastSabqiDate","2026-11-04");
        d.disk.put("lastLearningSnowballEveningDate","2026-11-04");
        return d;
    }
    static ConsolidationCycleEngine.Session session(String unit, boolean checkpoint,HifzPrefs p) {
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Session s=e.openSession(e.startCycle("learning-"+FRIDAY,LEARNING,
            new ConsolidationCycleEngine.Unit(unit,ConsolidationCycleEngine.Protocol.SNOWBALL)),"friday-session");
        if(checkpoint)assertTrue(p.persistConsolidationSession(s));
        while(!s.readyToClose()) {s=e.recordRepetition(s);if(checkpoint)assertTrue(p.persistConsolidationSession(s));}
        return s;
    }
    @Test public void T01FridayBeforeSabqiLegacyClosureMustNotWriteSuccess() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("date-only API must not manufacture Friday completion",p.completeSnowballEvening(LEARNING,FRIDAY.toString()));
        assertEquals(before,d.disk);
    }
    @Test public void T02CompletedEveningRepetitionsCannotReplaceFridayLesson() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Session s=session(WED,true,p);
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("Wednesday source without Friday Sabqi must not earn Friday receipt",p.completeSnowballEvening(LEARNING,FRIDAY.toString(),s));
        assertEquals(before,d.disk);
    }
    @Test public void T18DateOnlyLessonMarkerDoesNotProvePhysicalLesson() {
        InMemoryPrefs d=disk();d.disk.put("lastSabqiDate",FRIDAY.toString());HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Session s=session(WED,true,p);Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("legacy date marker alone is not a lesson certificate",p.completeSnowballEvening(LEARNING,FRIDAY.toString(),s));
        assertEquals(before,d.disk);
    }
    @Test public void T28UncheckpointedSessionCannotEarnNewPhysicalReceipt() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);
        ConsolidationCycleEngine.Session s=session(WED,false,p);Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("first physical receipt also requires exact durable OPEN",p.completeSnowballEvening(LEARNING,FRIDAY.toString(),s));
        assertEquals(before,d.disk);
    }
    @Test public void T25ForeignPhysicalUnitCannotCloseCurrentSource() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);
        String foreign=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("604:0"));
        ConsolidationCycleEngine.Session s=session(foreign,false,p);Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse("unrelated completed session must not consume the current fingerprint",p.completeSnowballEvening(LEARNING,FRIDAY.toString(),s));
        assertEquals(before,d.disk);
    }
}
