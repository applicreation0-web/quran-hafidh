package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** A skipped weekday lesson is not a receipt; genuine missed evening work survives week turnover. */
public final class SabqiMissedEveningCarryoverTest {
    private static final ConsolidationCycleEngine.Family SABQI =
        ConsolidationCycleEngine.Family.LEARNING;

    @BeforeClass public static void geometry() throws Exception {
        SnowballValidationIdempotenceTest.setup();
    }

    @Test public void missedEveningReturnsAfterNextRealLessonAndFinishesAllParts() {
        InMemoryPrefs disk=EarnedEveningProtocolRegressionTest.disk();
        HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate first=LocalDate.of(2026,10,5);
        LocalDate next=first.plusWeeks(1);
        SnowballValidationIdempotenceTest.morning(prefs,SABQI,first,0);
        List<ConsolidationCycleEngine.Unit> original=prefs.learningConsolidationUnits(first);
        assertEquals(1,original.size());
        assertTrue(prefs.learningSnowballFinalUnits(first.plusDays(6)).isEmpty());
        // The next eligible morning is the next unskipped physical lesson.
        SnowballValidationIdempotenceTest.morning(prefs,SABQI,next,5);
        List<ConsolidationCycleEngine.Unit> units=prefs.learningConsolidationUnits(next);
        assertEquals(3,units.size());
        assertEquals(original.get(0).id(),units.get(0).id());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL,units.get(0).protocol());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL,units.get(1).protocol());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL,units.get(2).protocol());
        assertEquals(10,ConsolidationPhysicalUnitPolicy.decodeLineUnit(units.get(2).id()).size());
        SnowballValidationIdempotenceTest.close(prefs,SABQI,next,
            SnowballValidationIdempotenceTest.open(prefs,SABQI,next));
        assertFalse(prefs.snowballEveningPending(SABQI,next));
        assertEquals("No Sabqi lesson may become Acquis from an evening",
            "[]",disk.getString("v6AcquiredCreditLineIds",""));
        assertEquals(2,new org.json.JSONArray(
            disk.getString("v6LearningEarnedEveningUnitsV1","[]")).length());
    }

    @Test public void absenceOfNewMorningReceiptCannotValidateTheOverdueEvening() {
        InMemoryPrefs disk=EarnedEveningProtocolRegressionTest.disk();
        HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate first=LocalDate.of(2026,10,5), next=first.plusWeeks(1);
        SnowballValidationIdempotenceTest.morning(prefs,SABQI,first,0);
        List<ConsolidationCycleEngine.Unit> due=prefs.learningConsolidationUnits(next);
        assertEquals(1,due.size());
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(
            "learning-"+next,SABQI,due.get(0));
        ConsolidationCycleEngine.Session proof=engine.openSession(cycle,"no-lesson");
        assertTrue(prefs.persistConsolidationSession(proof));
        while(!proof.readyToClose()){
            proof=engine.recordRepetition(proof);
            assertTrue(prefs.persistConsolidationSession(proof));
        }
        assertFalse("A new snowball cannot close without that day's actual Sabqi lesson",
            prefs.completeSnowballEvening(SABQI,next.toString(),proof));
        assertEquals("[]",disk.getString("v6LearningEarnedEveningUnitsV1","[]"));
        assertEquals("[]",disk.getString("v6AcquiredCreditLineIds","[]"));
    }
}
