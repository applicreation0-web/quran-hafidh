package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent invariant tests: deliberately RED while audited bypasses remain. */
public final class AuditRemainingBypasses11711Test {
    private static InMemoryPrefs disk() {
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("schema",6);
        for(String k:Arrays.asList("v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds",
                "v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"))d.disk.put(k,"[]");
        return d;
    }
    private static void trace(String name,boolean accepted,Map<String,Object> before,InMemoryPrefs d) {
        System.out.println(name+" accepted="+accepted+" BEFORE="+before+" AFTER="+d.disk);
    }

    @Test public void unearnedStabilizationMustNotBeDowngradedToOnePass() {
        InMemoryPrefs d=disk(); HifzPrefs p=new HifzPrefs(d);
        LocalDate day=LocalDate.of(2026,11,6);
        ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.STABILIZATION;
        String a=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("525:0","525:1"));
        String b=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("525:2","525:3"));
        d.disk.put("stabilizationSnowballWeekAnchor","2026-11-02");
        d.disk.put("stabilizationSnowballUnitIds",new JSONArray().put(a).put(b).toString());
        d.disk.put("v6StabilizedLineIds",new JSONArray(Arrays.asList("525:0","525:1","525:2","525:3")).toString());
        List<ConsolidationCycleEngine.Unit> planned=p.stabilizedConsolidationUnits(day);
        assertEquals("two ×7 blocks plus one ×5 non-crediting cumulative pass",3,planned.size());
        for(int i=0;i<2;i++)
            assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN,
                planned.get(i).protocol());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS_FIVE,
            planned.get(2).protocol());
        ConsolidationCycleEngine e=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle c=e.startCycle("stabilization-"+day,f,
            new ConsolidationCycleEngine.Unit(a,ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS));
        c=e.addUnit(c,new ConsolidationCycleEngine.Unit(b,ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        ConsolidationCycleEngine.Session s=e.openSession(c,"independent-downgraded-protocol");
        assertTrue(p.persistConsolidationSession(s));
        while(!s.readyToClose()){s=e.recordRepetition(s);assertTrue(p.persistConsolidationSession(s));}
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        boolean accepted=p.completeSnowballEvening(f,day.toString(),s);
        trace("DOWNGRADED_EVENING",accepted,before,d);
        assertFalse("A has never earned seven passes; one pass must not close source A+B",accepted);
        assertEquals(before,d.disk);
    }

    @Test public void sabqiRepetitionsMustBeBoundToExactPhysicalLines() throws Exception {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);
        d.disk.put("sabqiStart","1:1");d.disk.put("sabqiEnd","114:6");
        java.nio.file.Path path=java.nio.file.Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!java.nio.file.Files.exists(path))path=java.nio.file.Paths.get("..",path.toString());
        GeometryRepository geometry=GeometryRepository.fromJson(java.nio.file.Files.readString(path));
        p.setSabqiLineCursor(100);
        LocalDate day=LocalDate.of(2026,11,6);
        assertEquals(day,p.openSabqiLesson(geometry,day));
        String token=p.sabqiLessonToken();
        for(int n=0;n<p.sabqiLessonTargetReps();n++)assertTrue(p.recordSabqiRepetition(token,n,false));
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        boolean accepted=p.completeSabqiBlockV6(100,104,105,
            Arrays.asList("604:0","604:1","604:2","604:3","604:4"),"2026-11-06","foreign physical payload");
        trace("FOREIGN_SABQI_LINES",accepted,before,d);
        assertFalse("line indices 100..104 cannot authenticate unrelated page604 lines",accepted);
        assertEquals(before,d.disk);
    }

    @Test public void tinyItqanMustNotAcquireAtZeroRepetitions() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);
        assertEquals(0,p.itqanRep());Map<String,Object> before=new LinkedHashMap<>(d.disk);
        VerseRef start=new VerseRef(52,1),end=new VerseRef(52,2);
        boolean accepted=p.completeItqanTinyBlockV6(Arrays.asList("525:0","525:1"),1,false,start,end,new VerseRef(52,3));
        trace("ZERO_REP_TINY_ITQAN",accepted,before,d);
        assertFalse("zero-repetition fragment cannot become Acquis",accepted);
        assertEquals(before,d.disk);
    }

    @Test public void legacySabqiApiMustNotAdvanceCursorAtZeroRepetitions() {
        InMemoryPrefs d=disk();HifzPrefs p=new HifzPrefs(d);p.setSabqiLineCursor(100);
        assertEquals(0,p.sabqiRep());Map<String,Object> before=new LinkedHashMap<>(d.disk);
        boolean accepted=p.completeSabqiBlock(100,104,105,"2026-11-06","legacy direct completion");
        trace("ZERO_REP_LEGACY_SABQI",accepted,before,d);
        assertFalse("legacy API cannot skip unperformed physical lesson",accepted);
        assertEquals(before,d.disk);
    }
}
