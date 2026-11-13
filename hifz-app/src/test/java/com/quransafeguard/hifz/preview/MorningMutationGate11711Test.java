package com.quransafeguard.hifz.preview;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class MorningMutationGate11711Test {
    @Test public void T28IncompleteSabqiCannotCreateLearnedLinesOrLessonCertificate() {
        InMemoryPrefs d=EveningMutationGate11711Test.disk();HifzPrefs p=new HifzPrefs(d);
        p.setSabqiLineCursor(100);
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse(p.completeSabqiBlockV6(100,104,105,Arrays.asList("525:5","525:6","525:7","525:8","525:9"),"2026-11-06","incomplete"));
        assertEquals(before,d.disk);
    }
    @Test public void T09CompletedRepetitionsCannotSkipSavedSabqiCursor() {
        InMemoryPrefs d=EveningMutationGate11711Test.disk();HifzPrefs p=new HifzPrefs(d);
        p.setSabqiLineCursor(100);p.setSabqiProgress(PreviewConfig.SABQI_TOTAL_REPS,0);
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse(p.completeSabqiBlockV6(105,109,110,Arrays.asList("525:5","525:6","525:7","525:8","525:9"),"2026-11-06","skip"));
        assertEquals(before,d.disk);
    }
    @Test public void T25TinySabqiRemnantMustNotBeAcquiredByOpeningScreen() {
        InMemoryPrefs d=EveningMutationGate11711Test.disk();HifzPrefs p=new HifzPrefs(d);
        p.setSabqiLineCursor(100);Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse(p.completeSabqiTinyBlockV6(Arrays.asList("525:5","525:6"),102,"2026-11-06","tiny"));
        assertEquals(before,d.disk);
    }
    @Test public void T07IncompleteItqanCannotCreateStabilizedLinesOrEveningPrerequisite() {
        InMemoryPrefs d=EveningMutationGate11711Test.disk();HifzPrefs p=new HifzPrefs(d);
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        com.quransafeguard.hifz.core.VerseRef start=new com.quransafeguard.hifz.core.VerseRef(52,1);
        assertFalse(p.completeStabilizationBlockV6(Arrays.asList("525:5","525:6"),1,false,start,start,start,"2026-11-05","incomplete",Collections.emptyList()));
        assertEquals(before,d.disk);
    }
    @Test public void T14StaleSabqiRepetitionCannotWriteIntoNextPhysicalLesson() throws Exception {
        InMemoryPrefs d=EveningOracleStress11711Test.exactWednesday();HifzPrefs p=new HifzPrefs(d);
        p.setSabqiLineCursor(100);EarnedEveningProtocolRegressionTest.setup();
        GeometryRepository geo=EarnedEveningProtocolRegressionTest.geometry;
        p.openSabqiLesson(geo,EveningMutationGate11711Test.FRIDAY);String old=p.sabqiLessonToken();
        for(int rep=0;rep<p.sabqiLessonTargetReps();rep++)assertTrue(p.recordSabqiRepetition(old,rep,false));
        java.util.List<String> ids=geo.fiveLineBlock(100,geo.lineCount()-1).lineIds;
        assertTrue(p.completeSabqiBlockV6(100,104,105,ids,"2026-11-06","first"));
        p.openSabqiLesson(geo,EveningMutationGate11711Test.FRIDAY);
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse(p.recordSabqiRepetition(old,0,false));assertFalse(p.setSabqiProgress(37,0));
        assertEquals(before,d.disk);assertEquals(0,p.sabqiRep());
    }
    @Test public void T19CounterOnlyLegacySabqiCannotBeRedatedDuringUpgrade() throws Exception {
        InMemoryPrefs d=EveningOracleStress11711Test.exactWednesday();d.disk.put("sabqiLineCursor",100);d.disk.put("sabqiRep",37);
        HifzPrefs p=new HifzPrefs(d);EarnedEveningProtocolRegressionTest.setup();Map<String,Object> before=new LinkedHashMap<>(d.disk);
        try {p.openSabqiLesson(EarnedEveningProtocolRegressionTest.geometry,EveningMutationGate11711Test.FRIDAY);fail("unknown legacy date must fail closed");}
        catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("date is unverified"));}
        assertEquals(before,d.disk);
    }
}
