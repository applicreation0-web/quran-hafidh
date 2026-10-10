package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import static org.junit.Assert.*;

/** J4: no Sabqi engine/Quiz change; only Itqan stabilization consolidation x7. */
public final class ConsolidationSevenAndSundayFinalTest {
    @Test public void newlyStabilizedSevenAndPreviousContinuousOnce() {
        assertArrayEquals(new int[]{7,0,0,0,0},
            ConsolidationCycleEngine.stageVector(
                ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN,2,1));
        assertArrayEquals(new int[]{1,0,0,0,0},
            ConsolidationCycleEngine.stageVector(
                ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS,2,0));
        assertArrayEquals(new int[]{10,0,0,0,0},
            ConsolidationCycleEngine.stageVector(
                ConsolidationCycleEngine.Protocol.SNOWBALL,2,0));
        assertArrayEquals(new int[]{3,0,0,0,0},
            ConsolidationCycleEngine.stageVector(
                ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED,2,0));
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("itqan-week",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("old-continuous",
                ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS));
        cycle=engine.addUnit(cycle,new ConsolidationCycleEngine.Unit("today-new",
            ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,"evening");
        assertEquals(1,session.quotaAt(0));
        assertEquals(7,session.quotaAt(1));
        session=engine.recordRepetition(session);
        assertEquals(1,session.nextUnitIndex());
        for(int i=0;i<7;i++)session=engine.recordRepetition(session);
        assertTrue(session.readyToClose());
        assertEquals(8,1+7);
    }

    @Test public void alreadyOpenLegacyCycleStillRequiresTenEach() {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("old-itqan",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("old-unmodified",
                ConsolidationCycleEngine.Protocol.SNOWBALL));
        ConsolidationCycleEngine.Session old=engine.openSession(cycle,"legacy");
        for(int i=0;i<5;i++)old=engine.recordRepetition(old);
        ConsolidationCycleEngine.Session resume=engine.restoreOpenSession(cycle,"legacy",1,
            old.stage(),old.nextUnitIndex(),old.donePerStage());
        assertEquals(5,resume.donePerStage());
        assertEquals(10,resume.quotaAt(0));
        for(int i=0;i<5;i++)resume=engine.recordRepetition(resume);
        assertTrue(resume.readyToClose());
    }

    @Test public void newStabilizationProtocolsNeverApplyToRenforcement() {
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        try {
            engine.startCycle("bad",ConsolidationCycleEngine.Family.LEARNING,
                new ConsolidationCycleEngine.Unit("abc",
                    ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
            fail("Seven-times protocol erroneously applied to Sabqi");
        } catch(IllegalArgumentException expected){}
        try {
            engine.startCycle("bad",ConsolidationCycleEngine.Family.LEARNING,
                new ConsolidationCycleEngine.Unit("abc",
                    ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS));
            fail("Continuous stabilization protocol erroneously applied to Sabqi");
        } catch(IllegalArgumentException expected){}
    }

    @Test public void sundayRecommendedIsItqanPlusTwoFinalsNeverReview() throws Exception {
        assertEquals(com.quransafeguard.hifz.core.CadenceAction.STABILIZATION,
            WeeklyProgramPolicy.actionFor(DayOfWeek.SUNDAY,
                HifzPrefs.WeeklyProgram.RECOMMENDED,3));
        String main=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String dashboard=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("if(recommendedSunday(date))"));
        assertTrue(main.contains("if(!learningFinalResolved(date))return HifzSessionActivity.LEARNING_FINAL"));
        assertTrue(main.contains("if(!consolidationFinalResolved(date))return HifzSessionActivity.CONSOLIDATION_FINAL"));
        assertTrue(main.contains("if(recommendedSunday(today)){"));
        assertTrue(main.contains("return out; // Absolutely no active/passive revisions on Sunday."));
        assertTrue(dashboard.contains("Finale Renforcement Sabqi"));
        assertTrue(dashboard.contains("Finale Consolidation Itqān"));
    }

    private static String read(String repoPath) throws Exception {
        Path p=Paths.get(repoPath);
        if(!Files.exists(p))p=Paths.get("..",repoPath);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
}
