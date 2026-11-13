package com.quransafeguard.hifz.preview;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Frozen physical Sabqi receipts: no new target for an opened old V1 lesson. */
public final class SabqiFortyFiveRecoveryStoreTest {
    private static InMemoryPrefs lesson(int version) {
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("sabqiRep",0);
        d.disk.put("sabqiAssisted",0);
        d.disk.put("sabqiLineCursor",263);
        String base="{\"version\":"+version+",\"date\":\"2026-11-12\",\"sessionId\":\"original-token\","
            +"\"start\":263,\"end\":267,\"lines\":[\"19:10\",\"19:11\",\"19:12\",\"19:13\",\"19:14\"],"
            +"\"reps\":0,\"assisted\":0";
        if(version==2)base+=",\"passReveals\":0,\"target\":45";
        d.disk.put("sabqiPhysicalLessonSnapshotV1",base+"}");
        return d;
    }

    @Test public void firstPassFailedRecallGivesOnlyTenExtraUncreditedRepetitions() {
        InMemoryPrefs d=lesson(2);
        HifzPrefs h=new HifzPrefs(d);
        assertEquals(45,h.sabqiLessonTargetReps());
        for(int i=0;i<45;i++)
            assertTrue("Earn "+i,h.recordSabqiRepetition("original-token",i,i==43));
        assertEquals(45,h.sabqiRep());
        assertEquals(55,h.sabqiLessonTargetReps());
        assertFalse("Failed block must not be creditable",h.sabqiLessonReadyForCredit());
        for(int i=45;i<55;i++)
            assertTrue("Recovery "+i,h.recordSabqiRepetition("original-token",i,i==54));
        h=new HifzPrefs(d);
        assertEquals(55,h.sabqiRep());
        assertEquals(65,h.sabqiLessonTargetReps());
        assertFalse(h.sabqiLessonReadyForCredit());
        for(int i=55;i<65;i++)
            assertTrue("Second recovery "+i,h.recordSabqiRepetition("original-token",i,false));
        assertTrue("Successful recall at 100% restores eligibility",h.sabqiLessonReadyForCredit());
        assertFalse("No phantom double tap",h.recordSabqiRepetition("original-token",64,false));
    }

    @Test public void declaredFailureKeepsOriginalRepetitionsAndFrozens() throws Exception {
        InMemoryPrefs d=lesson(2);
        HifzPrefs h=new HifzPrefs(d);
        for(int i=0;i<45;i++)assertTrue(h.recordSabqiRepetition("original-token",i,false));
        assertTrue(h.sabqiLessonReadyForCredit());
        assertTrue(h.beginSabqiRecovery("original-token"));
        assertEquals(45,h.sabqiRep());
        assertEquals(55,h.sabqiLessonTargetReps());
        JSONObject frozen=new JSONObject(d.getString("sabqiPhysicalLessonSnapshotV1",""));
        assertEquals("2026-11-12",frozen.getString("date"));
        assertEquals(263,frozen.getInt("start"));
        assertFalse("Unaided recovery is not yet earned",h.sabqiLessonReadyForCredit());
    }

    @Test public void preexistingThirtySevenRepetitionsNeverBecomeFortyFive() throws Exception {
        InMemoryPrefs d=lesson(1);
        HifzPrefs h=new HifzPrefs(d);
        assertEquals(37,h.sabqiLessonTargetReps());
        assertEquals(PreviewConfig.sabqiMaskForNextRep(26),h.sabqiLessonMaskForNextRep(26));
        for(int i=0;i<37;i++)assertTrue(h.recordSabqiRepetition("original-token",i,false));
        assertTrue(h.sabqiLessonReadyForCredit());
        assertFalse(h.beginSabqiRecovery("original-token"));
        assertTrue(h.restartSabqiLesson("original-token"));
        assertEquals(37,h.sabqiLessonTargetReps());
        assertEquals(0,h.sabqiRep());
        JSONObject frozen=new JSONObject(d.getString("sabqiPhysicalLessonSnapshotV1",""));
        assertEquals("2026-11-12",frozen.getString("date"));
        assertEquals(1,frozen.getInt("version"));
    }

    @Test public void corruptedV2TargetFailsClosed() {
        InMemoryPrefs d=lesson(2);
        d.disk.put("sabqiPhysicalLessonSnapshotV1",
            ((String)d.disk.get("sabqiPhysicalLessonSnapshotV1")).replace("\"target\":45","\"target\":44"));
        HifzPrefs h=new HifzPrefs(d);
        assertEquals(-1,h.sabqiLessonTargetReps());
        assertFalse(h.sabqiLessonReadyForCredit());
        assertFalse(h.recordSabqiRepetition("original-token",0,false));
    }

    @Test public void resumedLegacyProofKeepsItsDateButTheReceiptBelongsToTheEligibleDay() {
        InMemoryPrefs disk=lesson(1);
        disk.disk.put("schema",6);
        disk.disk.put("sabqiEnd","2:286");
        for(String key : java.util.List.of("v6LearnedLineIds","v6StabilizedLineIds",
                "v6AcquiredCreditLineIds","v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"))
            disk.disk.put(key,"[]");
        disk.disk.put("weeklyProgramTransitionsV1","[{\"from\":\"2026-11-01\",\"mode\":\"RECOMMENDED\"}]");
        var geometry=MorningProtocolFixture.realGeometry();
        var prefs=new HifzPrefs(disk,geometry);
        var proofDate=java.time.LocalDate.parse("2026-11-12");
        var today=java.time.LocalDate.parse("2026-11-13");
        HifzClock.setClockForTests(java.time.Clock.fixed(today.atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant(),java.time.ZoneOffset.UTC));
        try {
            assertEquals(proofDate,prefs.openSabqiLesson(geometry,today));
            var block=prefs.frozenSabqiBlock(geometry,geometry.lineCount()-1);
            for(int i=0;i<37;i++) assertTrue(prefs.recordSabqiRepetition("original-token",i,false));
            assertFalse(prefs.completeCurrentSabqi(block,"stale",proofDate,today,"Sabqi"));
            assertTrue(prefs.completeCurrentSabqi(block,"original-token",proofDate,today,"Sabqi"));
            assertEquals(today.toString(),prefs.lastSabqiDate());
            assertFalse(prefs.completeCurrentSabqi(block,"original-token",proofDate,today,"duplicate"));
        } finally {
            HifzClock.resetClockForTests();
        }
    }
}
