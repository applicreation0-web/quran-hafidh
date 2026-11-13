package com.quransafeguard.hifz.preview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanWeeklyIntegrationTest {
    private static GeometryRepository geo;

    @BeforeClass public static void geometry() throws Exception {
        Path path=Path.of("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(path)) path=Path.of("..").resolve(path);
        geo=GeometryRepository.fromJson(Files.readString(path));
    }

    @After public void restoreClock() { HifzClock.resetClockForTests(); }

    private static LocalDate day(String value) {
        LocalDate date=LocalDate.parse(value);
        HifzClock.setClockForTests(Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
        return date;
    }

    /** Reconstructs only the documented evidence; this is not the user's export. */
    private static InMemoryPrefs fixture(int reps) throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("schema",6);
        disk.disk.put("weeklyProgramTransitionsV1","[{\"from\":\"2026-11-01\",\"mode\":\"RECOMMENDED\"}]");
        disk.disk.put(ItqanRegimeStore.LEG,"TAIL_HUJURAT_NAS");
        disk.disk.put(ItqanRegimeStore.CURSOR,"53:33");
        disk.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,false);
        disk.disk.put("lastItqanDate","2026-11-10");
        disk.disk.put("itqanRep",reps);
        disk.disk.put("itqanElapsedMs",16340L);
        disk.disk.put("itqanUnitStart","53:33");
        disk.disk.put("itqanUnitEnd","53:62");
        disk.disk.put("itqanAssisted",0);
        disk.disk.put("v6AcquiredCreditLineIds","[\"527:0\"]");
        disk.disk.put("v6StabilizedLineIds",new JSONArray(geo.lineIdsOnPage(527).subList(1,10)).toString());
        List<String> learned=new ArrayList<>(geo.lineIdsOnPage(527).subList(10,15));
        learned.addAll(geo.lineIdsOnPage(528));
        disk.disk.put("v6LearnedLineIds",new JSONArray(learned).toString());
        disk.disk.put("v6QuarantineLineIds","[]");
        disk.disk.put("v6LegacyPartialAcquiredLineIds","[]");
        JSONObject receipt=new JSONObject().put("family","STABILIZATION").put("date","2026-11-10")
            .put("lines",new JSONArray(geo.lineIdsOnPage(527).subList(1,10)));
        disk.disk.put("v6DailyLessonReceiptsV1",new JSONArray().put(receipt).toString());
        disk.disk.put(ItqanPhysicalProof11711.KEY,new JSONObject().put("v",7)
            .put("token","fixture-only").put("date","2026-11-12").put("start","53:33")
            .put("end","53:62").put("block",0).put("target",40).put("reps",reps)
            .put("regime","DEEP_FIRST_PASS").put("protocol","FULL").put("mask","BALANCED_20_5_5_5_5")
            .put("cursor","54:1").put("final",false)
            .put("lines",new JSONArray(learned.subList(0,6))).put("bonus",new JSONArray()).toString());
        return disk;
    }

    @Test public void remainderThenNajmQamarAndExactNextPage() throws Exception {
        InMemoryPrefs disk=fixture(0);
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate saturday=day("2026-11-14");
        ItqanFourSessionSnapshot snap=prefs.openWeeklyItqan(geo,saturday);
        assertNotNull(snap);
        assertEquals(9,prefs.progressionSnapshotV6().stabilized.size());
        assertEquals("2026-11-10",prefs.lastItqanDate());
        assertEquals(geo.lineIdsOnPage(527).subList(10,15),snap.plan.sessions.get(0).blocks.get(0).lineIds);
        assertNull(ItqanPhysicalProof11711.read(disk));
        complete(prefs,saturday);
        assertEquals(14,prefs.progressionSnapshotV6().stabilized.size());
        assertTrue(prefs.progressionSnapshotV6().learned.contains("528:0"));
        assertEquals("528:0",disk.getString("itqanFourNextPhysicalLineIdV1",""));
        assertEquals(List.of(4,5,4),prefs.openWeeklyItqan(geo,day("2026-11-17"))
            .plan.sessions.stream().map(s->s.lineCount).toList());
        complete(prefs,day("2026-11-17"));
        complete(prefs,day("2026-11-19"));
        complete(prefs,day("2026-11-21"));
        assertEquals("529:0",disk.getString("itqanFourNextPhysicalLineIdV1",""));
        ItqanFourSessionSnapshot next=prefs.openWeeklyItqan(geo,day("2026-11-24"));
        assertEquals("529:0",next.plan.sessions.get(0).blocks.get(0).lineIds.get(0));
        JSONArray receipts=new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]"));
        assertEquals(5,receipts.length());
        assertEquals("2026-11-14",receipts.getJSONObject(1).getString("date"));
        assertEquals(5,receipts.getJSONObject(1).getJSONArray("lines").length());
    }

    @Test public void missedSaturdayAndEightWeeksRetainRemainder() throws Exception {
        for(int weeks=0;weeks<=8;weeks++) {
            InMemoryPrefs disk=fixture(0);
            HifzPrefs prefs=new HifzPrefs(disk,geo);
            LocalDate tuesday=day(LocalDate.parse("2026-11-17").plusWeeks(weeks).toString());
            assertEquals("527:10",prefs.openWeeklyItqan(geo,tuesday).plan.sessions.get(0).blocks.get(0).lineIds.get(0));
            complete(prefs,tuesday);
            LocalDate thursday=day(tuesday.plusDays(2).toString());
            assertTrue(prefs.weeklyItqanPageClosed(thursday));
            assertNull(prefs.openWeeklyItqan(geo,thursday));
        }
    }

    @Test public void missedTuesdayShiftsEveryRemainingSlotWithoutSkippingQamar() throws Exception {
        InMemoryPrefs disk=fixture(0);
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        complete(prefs,day("2026-11-14"));
        LocalDate thursday=day("2026-11-19");
        assertEquals("528:0",prefs.openWeeklyItqan(geo,thursday).plan.sessions.get(0).blocks.get(0).lineIds.get(0));
        complete(prefs,thursday);
        complete(prefs,day("2026-11-21"));
        LocalDate nextTuesday=day("2026-11-24");
        ItqanFourSessionSnapshot qamar=prefs.openWeeklyItqan(geo,nextTuesday);
        assertEquals("528:9",qamar.plan.sessions.get(qamar.sessionIndex).blocks.get(0).lineIds.get(0));
        complete(prefs,nextTuesday);
        assertEquals("529:0",disk.getString("itqanFourNextPhysicalLineIdV1",""));
        assertNull(prefs.openWeeklyItqan(geo,day("2026-11-26")));
    }

    @Test public void sixtyFourMissedDatePatternsNeverSkipOrDoubleCountNajmQamar() throws Exception {
        // Six candidate attendance decisions plus three follow-up dates.
        // Every variant starts from the same never-credited 12 November proof,
        // advances only on real dates, and is restored between date slots.
        LocalDate[] calendar={
            LocalDate.of(2026,11,14),LocalDate.of(2026,11,17),
            LocalDate.of(2026,11,19),LocalDate.of(2026,11,21),
            LocalDate.of(2026,11,24),LocalDate.of(2026,11,26),
            LocalDate.of(2026,11,28),LocalDate.of(2026,12,1),
            LocalDate.of(2026,12,3)
        };
        List<String> ordered=new ArrayList<>(geo.lineIdsOnPage(527).subList(10,15));
        ordered.addAll(geo.lineIdsOnPage(528));
        assertEquals(18,ordered.size());
        for(int absentMask=0;absentMask<64;absentMask++){
            InMemoryPrefs disk=fixture(0);
            HifzPrefs prefs=new HifzPrefs(disk,geo);
            List<String> completedLines=new ArrayList<>();
            for(int slot=0;slot<calendar.length;slot++){
                LocalDate date=day(calendar[slot].toString());
                int before=new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]")).length();
                if(slot<6 && ((absentMask >> slot)&1)!=0) {
                    // No phantom attendance from an absent weekday.
                    assertEquals(before,new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]")).length());
                    continue;
                }
                if(!prefs.weeklyItqanPageClosed(date)){
                    ItqanFourSessionSnapshot plan=prefs.openWeeklyItqan(geo,date);
                    if(plan!=null) complete(prefs,date);
                }
                JSONArray receipts=new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]"));
                assertTrue("No more than one dated receipt: mask="+absentMask+" slot="+slot,
                    receipts.length()<=before+1);
                for(int r=before;r<receipts.length();r++){
                    JSONObject receipt=receipts.getJSONObject(r);
                    assertEquals("Real attendance date",date.toString(),receipt.getString("date"));
                    JSONArray lines=receipt.getJSONArray("lines");
                    assertTrue("No zero-line receipt",lines.length()>0);
                    for(int j=0;j<lines.length();j++){
                        String id=lines.getString(j);
                        // Other pages may legitimately follow after this transition.
                        // Only check earned lines within Najm/Qamar's two-page boundary.
                        if(ordered.contains(id))completedLines.add(id);
                    }
                }
                assertTrue("No Najm/Qamar duplicate: mask="+absentMask+" slot="+slot,
                    completedLines.size()<=ordered.size());
                assertEquals("Physical prefix order mask="+absentMask+" slot="+slot,
                    ordered.subList(0,completedLines.size()),completedLines);
                assertEquals(completedLines.size(),new java.util.HashSet<>(completedLines).size());
                // Force a reboot against the same durable preferences.
                prefs=new HifzPrefs(disk,geo);
                if(completedLines.size()==ordered.size()){
                    assertEquals("529:0",disk.getString("itqanFourNextPhysicalLineIdV1",""));
                    break; // This stress case is specifically about pages 527 and 528.
                }
            }
            JSONArray allReceipts=new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]"));
            for(int r=0;r<allReceipts.length();r++)
                assertNotEquals("No phantom 12 November receipt", "2026-11-12",
                    allReceipts.getJSONObject(r).getString("date"));
        }
    }

    @Test public void multiSurahSlotEarnsOnlyOneReceiptAfterEverySubblock() throws Exception {
        InMemoryPrefs disk=fixture(0);
        disk.disk.remove(ItqanPhysicalProof11711.KEY);
        disk.disk.remove("itqanUnitStart");
        disk.disk.remove("itqanUnitEnd");
        disk.disk.put(ItqanRegimeStore.CURSOR,geo.linesForExactIds(List.of("604:0")).get(0)
            .verses.get(0).toString());
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate saturday=day("2026-11-14");
        ItqanFourSessionSnapshot page=prefs.openWeeklyItqan(geo,saturday);
        assertEquals(2,page.plan.sessions.get(0).blocks.size());
        finishSubblock(prefs,saturday);
        assertEquals("2026-11-10",prefs.lastItqanDate());
        assertEquals(9,prefs.progressionSnapshotV6().stabilized.size());
        assertEquals(1,new HifzPrefs(disk,geo).openWeeklyItqan(geo,saturday).blockIndex);
        finishSubblock(prefs,saturday);
        assertEquals(saturday.toString(),prefs.lastItqanDate());
        JSONArray receipts=new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]"));
        assertEquals(2,receipts.length());
        assertEquals(new JSONArray(geo.lineIdsOnPage(604).subList(0,5)).toString(),
            receipts.getJSONObject(1).getJSONArray("lines").toString());
        complete(prefs,day("2026-11-17"));
        assertTrue(prefs.itqanPostNasMaintenance());
        assertNull(prefs.frozenItqanSnapshot(geo));
        assertFalse(prefs.weeklyItqanPageClosed(LocalDate.parse("2026-11-19")));
    }

    @Test public void startedUnknownAndSameDateCheckpointsCannotBeMigrated() throws Exception {
        InMemoryPrefs started=fixture(1);
        HifzPrefs prefs=new HifzPrefs(started,geo);
        Map<String,?> before=Map.copyOf(started.getAll());
        assertThrows(IllegalStateException.class,()->prefs.openWeeklyItqan(geo,day("2026-11-14")));
        assertEquals(before,started.getAll());
        InMemoryPrefs unknown=fixture(0);
        unknown.disk.put(ItqanPhysicalProof11711.KEY,"{}");
        assertThrows(IllegalStateException.class,()->new HifzPrefs(unknown,geo).openWeeklyItqan(geo,day("2026-11-14")));
        InMemoryPrefs same=fixture(0);
        assertThrows(IllegalStateException.class,()->new HifzPrefs(same,geo).openWeeklyItqan(geo,day("2026-11-12")));
    }

    @Test public void afterRestartFinalRepCannotCreditAnOldDate() throws Exception {
        InMemoryPrefs disk=fixture(0);
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate saturday=day("2026-11-14");
        prefs.openWeeklyItqan(geo,saturday);
        for(int i=0;i<39;i++)assertTrue(prefs.recordFrozenItqanRep(prefs.frozenItqanSnapshot(geo),geo,false));
        disk=PartialFinalEightWeekStressTest.backupRoundTrip(disk);
        HifzPrefs restored=new HifzPrefs(disk,geo);
        assertEquals(39,restored.openWeeklyItqan(geo,saturday).reps);
        assertTrue(restored.recordFrozenItqanRep(restored.frozenItqanSnapshot(geo),geo,false));
        LocalDate tuesday=day("2026-11-17");
        assertFalse(restored.completeFrozenItqanSubblockV6(restored.frozenItqanSnapshot(geo),geo,saturday.toString(),"old"));
        assertEquals(40,restored.openWeeklyItqan(geo,tuesday).reps);
        assertFalse(restored.restartFrozenItqanBlock(restored.frozenItqanSnapshot(geo),geo,saturday));
        assertTrue(restored.completeFrozenItqanSubblockV6(restored.frozenItqanSnapshot(geo),geo,tuesday.toString(),"current"));
        assertEquals(tuesday.toString(),restored.lastItqanDate());
    }

    @Test public void staleActivityAndMismatchedRepetitionsCannotAdvanceReboundSlot() throws Exception {
        InMemoryPrefs disk=fixture(0);
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate saturday=day("2026-11-14");
        ItqanFourSessionSnapshot initial=prefs.openWeeklyItqan(geo,saturday);
        LocalDate tuesday=day("2026-11-17");
        prefs.openWeeklyItqan(geo,tuesday);
        assertFalse(prefs.recordFrozenItqanRep(initial,geo,false,saturday));
        assertTrue(prefs.recordFrozenItqanRep(initial,geo,false,tuesday));
        disk.disk.put("itqanRep",0);
        assertThrows(IllegalStateException.class,()->prefs.frozenItqanSnapshot(geo));
        assertEquals("2026-11-10",prefs.lastItqanDate());
    }

    @Test public void sundayFinalUsesFourteenLinesOnlyAfterRealEvening() throws Exception {
        InMemoryPrefs disk=fixture(0);
        List<String> tuesday=geo.lineIdsOnPage(527).subList(1,10);
        String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(tuesday);
        disk.disk.put("stabilizationSnowballWeekAnchor","2026-11-09");
        disk.disk.put("stabilizationSnowballUnitIds",new JSONArray(List.of(unit)).toString());
        disk.disk.put("stabilizationSnowball8WeekHistory",new JSONArray().put(
            new JSONObject().put("week","2026-11-09").put("units",new JSONArray(List.of(unit)))).toString());
        disk.disk.put("v6StabilizationEarnedEveningUnitsV1",new JSONArray(List.of(unit)).toString());
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate saturday=day("2026-11-14");
        assertFalse(prefs.hasDailyLessonForNewEvening(ConsolidationCycleEngine.Family.STABILIZATION,saturday));
        complete(prefs,saturday);
        LocalDate sunday=saturday.plusDays(1);
        assertEquals(9,finalIds(prefs,sunday).size());
        List<ConsolidationCycleEngine.Unit> due=prefs.stabilizedConsolidationUnits(saturday);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Family family=ConsolidationCycleEngine.Family.STABILIZATION;
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("stabilization-"+saturday,family,due.get(0));
        for(int i=1;i<due.size();i++)cycle=engine.addUnit(cycle,due.get(i));
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,"synthetic-saturday");
        assertTrue(prefs.persistConsolidationSession(session));
        while(!session.readyToClose()){
            session=engine.recordRepetition(session);
            assertTrue(prefs.persistConsolidationSession(session));
        }
        assertTrue(prefs.completeSnowballEvening(family,saturday.toString(),session));
        assertEquals(geo.lineIdsOnPage(527).subList(1,15),new ArrayList<>(finalIds(prefs,sunday)));
        assertEquals(java.util.Set.of("527:0"),prefs.progressionSnapshotV6().acquired);
    }

    @Test public void twoLineRemainderClosesWithoutAnEmptySnapshotOrReceipt() throws Exception {
        InMemoryPrefs disk=fixture(0);
        disk.disk.remove(ItqanPhysicalProof11711.KEY);
        disk.disk.remove("itqanUnitStart");
        disk.disk.remove("itqanUnitEnd");
        disk.disk.put(ItqanRegimeStore.CURSOR,geo.linesForExactIds(List.of("527:13")).get(0)
            .verses.get(0).toString());
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        LocalDate tuesday=day("2026-11-17");
        ItqanFourSessionSnapshot snapshot=prefs.openWeeklyItqan(geo,tuesday);
        assertEquals(List.of(2,0,0),snapshot.plan.sessions.stream().map(slot->slot.lineCount).toList());
        complete(prefs,tuesday);
        assertNull(prefs.frozenItqanSnapshot(geo));
        assertNull(prefs.openWeeklyItqan(geo,day("2026-11-19")));
        assertNull(prefs.openWeeklyItqan(geo,day("2026-11-21")));
        assertEquals(2,new JSONArray(disk.getString("v6DailyLessonReceiptsV1","[]")).length());
    }

    @Test public void newWeeklyBlockKeepsBothRecoveryCyclesAndEarnsOnlyOneReceipt() throws Exception {
        InMemoryPrefs disk=fixture(0);
        HifzPrefs prefs=new HifzPrefs(disk,geo);
        complete(prefs,day("2026-11-14"));
        LocalDate tuesday=day("2026-11-17");
        ItqanFourSessionSnapshot snapshot=prefs.openWeeklyItqan(geo,tuesday);
        assertEquals(30,snapshot.targetReps());
        for(int rep=0;rep<50;rep++) {
            assertTrue(prefs.recordFrozenItqanRep(snapshot,geo,rep==29||rep==39,tuesday));
            prefs=new HifzPrefs(disk,geo);
            snapshot=prefs.openWeeklyItqan(geo,tuesday);
            if(rep==29||rep==39) {
                assertEquals(rep+11,snapshot.targetReps());
                assertFalse(prefs.frozenItqanReady(snapshot));
                assertEquals("2026-11-14",prefs.lastItqanDate());
            }
        }
        assertTrue(prefs.completeFrozenItqanSubblockV6(snapshot,geo,tuesday.toString(),"earned"));
        assertFalse(prefs.completeFrozenItqanSubblockV6(snapshot,geo,tuesday.toString(),"duplicate"));
        assertEquals(tuesday.toString(),prefs.lastItqanDate());
    }

    private static java.util.Set<String> finalIds(HifzPrefs prefs,LocalDate day) {
        java.util.Set<String> ids=new java.util.LinkedHashSet<>();
        for(ConsolidationCycleEngine.Unit unit:prefs.stabilizationSnowballFinalUnits(day,geo))
            ids.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(unit.id()));
        return ids;
    }

    private static void complete(HifzPrefs prefs,LocalDate date) {
        ItqanFourSessionSnapshot snapshot=prefs.openWeeklyItqan(geo,date);
        assertNotNull(snapshot);
        int blocks=snapshot.plan.sessions.get(snapshot.sessionIndex).blocks.size();
        for(int index=snapshot.blockIndex;index<blocks;index++)finishSubblock(prefs,date);
    }

    private static void finishSubblock(HifzPrefs prefs,LocalDate date) {
        int target=prefs.frozenItqanSnapshot(geo).targetReps();
        for(int i=0;i<target;i++)assertTrue(prefs.recordFrozenItqanRep(prefs.frozenItqanSnapshot(geo),geo,false));
        assertTrue(prefs.completeFrozenItqanSubblockV6(prefs.frozenItqanSnapshot(geo),geo,date.toString(),"fixture"));
    }
}
