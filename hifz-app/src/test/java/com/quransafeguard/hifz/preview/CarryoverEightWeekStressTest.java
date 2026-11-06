package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import org.junit.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

/** All 64 attendance combinations after each of 1..8 completely missed weeks. */
public final class CarryoverEightWeekStressTest {
    @Test public void openEveningsSurviveEightWeeksAndLegacyFinalSlotMigrationWithoutLosingTheirDate() throws Exception {
        EarnedEveningProtocolRegressionTest.setup();
        GeometryRepository geometry=EarnedEveningProtocolRegressionTest.geometry;
        for(int weeks=1;weeks<=8;weeks++) for(ConsolidationCycleEngine.Family family:
                ConsolidationCycleEngine.Family.values()) {
            boolean learning=family==ConsolidationCycleEngine.Family.LEARNING;
            String prefix=learning?"learning":"stabilization";
            LocalDate date=LocalDate.of(2026,12,21).plusDays(learning?0:1);
            InMemoryPrefs disk=PartialFinalEightWeekStressTest.empty();
            HifzPrefs prefs=new HifzPrefs(disk);
            List<String> ids=EarnedEveningProtocolRegressionTest.five();
            List<GeometryRepository.LineMeta> meta=geometry.linesForExactIds(ids);
            int start=meta.get(0).globalIndex;
            if(learning)prefs.setSabqiLineCursor(start);
            if(learning)assertTrue(MorningProtocolFixture.completeSabqi(prefs,start,start+4,start+5,ids,date.toString(),"stress"));
            else {
                List<com.quransafeguard.hifz.core.VerseRef> end=meta.get(4).verses;
                assertTrue(MorningProtocolFixture.completeItqan(prefs,ids,1,false,meta.get(0).verses.get(0),
                    end.get(end.size()-1),end.get(end.size()-1),date.toString(),"stress",Collections.emptyList()));
            }
            List<ConsolidationCycleEngine.Unit> units=learning?prefs.learningConsolidationUnits(date):
                prefs.stabilizedConsolidationUnits(date);
            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
            ConsolidationCycleEngine.Session open=engine.openSession(
                engine.startCycle(prefix+"-"+date,family,units.get(0)),"long-open");
            open=engine.recordRepetition(open);assertTrue(prefs.persistConsolidationSession(open));
            String snapshot=prefs.frozenConsolidationSnapshot(family,prefix+"-"+date);
            for(int day=1;day<=weeks*7;day++) {
                disk=PartialFinalEightWeekStressTest.backupRoundTrip(disk);prefs=new HifzPrefs(disk);
                Map<String,Object> before=new LinkedHashMap<>(disk.disk);
                assertEquals(date.toString(),prefs.unfinishedConsolidationSessionDate(family,prefix+"-"));
                open=prefs.restoreConsolidationSession(engine,family);
                assertEquals(1,open.donePerStage());
                assertEquals(snapshot,prefs.frozenConsolidationSnapshot(family,prefix+"-"+date));
                assertTrue((learning?prefs.learningSnowballFinalUnits(date.plusDays(day),geometry):
                    prefs.stabilizationSnowballFinalUnits(date.plusDays(day),geometry)).isEmpty());
                assertEquals(before,disk.disk);
            }
            while(!open.readyToClose()) {open=engine.recordRepetition(open);assertTrue(prefs.persistConsolidationSession(open));}
            assertTrue(prefs.completeSnowballEvening(family,date.toString(),open));
            assertEquals(date.toString(),learning?prefs.lastLearningSnowballEveningDate():prefs.lastStabilizationSnowballEveningDate());
            assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
            LocalDate sunday=date.plusWeeks(weeks).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            List<ConsolidationCycleEngine.Unit> finalUnits=learning?prefs.learningSnowballFinalUnits(sunday,geometry):
                prefs.stabilizationSnowballFinalUnits(sunday,geometry);
            assertEquals(1,finalUnits.size());
            ConsolidationCycleEngine.Session finale=engine.openSession(
                engine.startCycle(prefix+"-final-"+sunday,family,finalUnits.get(0)),"long-final");
            finale=engine.recordRepetition(finale);assertTrue(prefs.persistConsolidationSession(finale));
            String oldKey=learning?"v6ConsolidationLearningState":"v6ConsolidationStabilizationState";
            String raw=disk.getString(oldKey+"FinalV1","");assertFalse(raw.isEmpty());
            // Actual legacy migration: a Sunday OPEN snapshot lived in the weekday slot.
            disk.disk.put(oldKey,raw);disk.disk.remove(oldKey+"FinalV1");
            disk=PartialFinalEightWeekStressTest.backupRoundTrip(disk);prefs=new HifzPrefs(disk);
            finale=prefs.restoreConsolidationSession(engine,family,prefix+"-final-"+sunday);
            assertEquals(raw,disk.getString(oldKey+"FinalV1",""));assertFalse(disk.contains(oldKey));
            assertEquals(1,finale.donePerStage());
            while(!finale.readyToClose()) {finale=engine.recordRepetition(finale);assertTrue(prefs.persistConsolidationSession(finale));}
            assertTrue(learning?prefs.completeLearningConsolidationSessionV6(finale,geometry,sunday.toString(),"stress"):
                prefs.completeConsolidationSessionV6(finale,geometry,sunday.toString(),"stress"));
            assertEquals(new LinkedHashSet<>(ids),new HifzPrefs(disk).progressionSnapshotV6().acquired);
        }
    }

    @Test public void allAttendanceMasksKeepDatesFamiliesAndHistoricalAbsencesIndependent() {
        LocalDate start=LocalDate.of(2026,12,21); // includes a year boundary
        for(int weeks=1;weeks<=8;weeks++) for(int mask=0;mask<64;mask++) {
            LocalDate resume=start.plusWeeks(weeks);
            Set<LocalDate> sabqi=new HashSet<>(),itqan=new HashSet<>();
            for(int day=0;day<14;day++) {
                LocalDate today=resume.plusDays(day);
                List<HifzBacklog.Due> due=HifzBacklog.calculate(start,start,today,sabqi,itqan,
                    d->WeeklyProgramPolicy.actionFor(d.getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED,3));
                HifzBacklog.Due current=HifzBacklog.onDate(due,today);
                int weekday=day%7;
                if(weekday==6) assertNull("Sunday must never become a make-up lesson",current);
                else {
                    assertNotNull(current);
                    assertEquals(today,current.date);
                    assertEquals(weekday%2==0?"SABQI":"ITQAN",current.mode);
                    if((mask&(1<<weekday))!=0) (weekday%2==0?sabqi:itqan).add(today);
                }
                List<HifzBacklog.Due> after=HifzBacklog.calculate(start,start,today,sabqi,itqan,
                    d->WeeklyProgramPolicy.actionFor(d.getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED,3));
                assertEquals(start,HifzBacklog.first(after).date);
                for(HifzBacklog.Due absent:after) {
                    assertFalse((absent.mode.equals("SABQI")?sabqi:itqan).contains(absent.date));
                    assertNotEquals(java.time.DayOfWeek.SUNDAY,absent.date.getDayOfWeek());
                }
                assertEquals("No historical absence is back-credited",weeks*6+(day/7)*6+
                    Math.min(weekday+1,6)-sabqi.size()-itqan.size(),after.size());
                assertEquals(sabqi.contains(today)||itqan.contains(today)||weekday==6,
                    HifzBacklog.onDate(after,today)==null);
            }
        }
    }
}
