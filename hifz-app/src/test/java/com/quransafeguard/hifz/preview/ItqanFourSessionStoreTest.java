package com.quransafeguard.hifz.preview;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanFourSessionStoreTest {
 static GeometryRepository geo;
 @BeforeClass public static void init()throws Exception {
   Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
   if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
   geo=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
 }
 private static ItqanFourSessionSnapshot sample(FaultInjectingPrefs db,int page) {
   List<String> ids=new ArrayList<>(geo.lineIdsOnPage(page));
   ids.addAll(geo.lineIdsOnPage(page+1));
   List<GeometryRepository.LineMeta> actual=geo.linesForExactIds(ids);
   VerseRef cursor=actual.get(0).verses.get(0);
   ItqanRotationPolicy.State state=new ItqanRotationPolicy.State(
       ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,cursor,false);
   db.disk.put(ItqanRegimeStore.LEG,state.leg.name());
   db.disk.put(ItqanRegimeStore.CURSOR,cursor.toString());
   db.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,false);
   return ItqanFourSessionSnapshot.freeze(ItqanFourSessionPolicy.plan(actual,true),
       state,LocalDate.of(2026,10,12),geo);
 }
 @Test public void frozenPlanSurvivesRestartAndRejectsStaleTap() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snap,geo));
   assertFalse("Never replace existing plan",ItqanFourSessionStore.open(db,snap,geo));
   assertTrue(ItqanFourSessionStore.recordRep(db,snap,geo));
   assertFalse("Stale tap cannot repeat the rep",ItqanFourSessionStore.recordRep(db,snap,geo));
   ItqanFourSessionSnapshot reboot=ItqanFourSessionStore.read(db,geo);
   assertEquals(1,reboot.reps);
   assertEquals(snap.fingerprint,reboot.fingerprint);
   assertEquals(1,db.getInt("itqanRep",0));
 }
 @Test public void revealAssistanceAndRepetitionSaveAtomically() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snap,geo));
   db.failCommit=db.commits+1;
   assertFalse(ItqanFourSessionStore.recordRep(db,snap,geo,true));
   assertEquals(0,db.getInt("itqanAssisted",0));
   assertEquals(0,db.getInt("itqanRep",0));
   db.failCommit=-1;
   assertTrue(ItqanFourSessionStore.recordRep(db,snap,geo,true));
   assertEquals(1,db.getInt("itqanAssisted",0));
   assertEquals(1,db.getInt("itqanRep",0));
   assertFalse(ItqanFourSessionStore.recordRep(db,snap,geo,true));
   assertEquals("Stale tap cannot add assistance",1,db.getInt("itqanAssisted",0));
 }
 @Test public void aFailedCommitLeavesTheEntireSnapshotAndRepUntouched() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snap,geo));
   db.failCommit=db.commits+1;
   assertFalse(ItqanFourSessionStore.recordRep(db,snap,geo));
   assertEquals(0,ItqanFourSessionStore.read(db,geo).reps);
   assertEquals(0,db.getInt("itqanRep",0));
   db.failCommit=-1;
   assertTrue(ItqanFourSessionStore.recordRep(db,snap,geo));
   assertEquals(1,ItqanFourSessionStore.read(db,geo).reps);
 }
 @Test public void existingOpenLegacySessionCannotBeOverwritten() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,603);
   db.disk.put("itqanRep",27);
   try{
     ItqanFourSessionStore.open(db,snap,geo);
     fail("Open 1.17.1 Itqan repetition must remain untouched");
   }catch(IllegalStateException expected){}
   assertFalse(db.contains(ItqanFourSessionStore.KEY));
   assertEquals(27,db.getInt("itqanRep",0));
 }
 @Test public void aBlockCannotAdvanceBeforeFortyRepetitions() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,603);
   try{ItqanFourSessionStore.nextAfterValidatedBlock(snap.withProgress(0,0,39,geo),geo);
       fail("Incomplete ×40 block advanced");}
   catch(IllegalStateException expected){}
   ItqanFourSessionSnapshot next=ItqanFourSessionStore.nextAfterValidatedBlock(
       snap.withProgress(0,0,40,geo),geo);
   assertEquals(0,next.reps);
   // One date may legitimately contain only one same-surah subblock.
   boolean moreBlocksInFirstDate=snap.plan.sessions.get(0).blocks.size()>1;
   assertEquals(moreBlocksInFirstDate?0:1,next.sessionIndex);
   assertEquals(moreBlocksInFirstDate?1:0,next.blockIndex);
 }
 private static void seedV6(FaultInjectingPrefs p) {
   p.disk.put("schema",6);
   for(String k:new String[]{"v6LearnedLineIds","v6StabilizedLineIds",
       "v6AcquiredCreditLineIds","v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"})
     p.disk.put(k,"[]");
 }
 private static ItqanFourSessionSnapshot completeRepetitions(FaultInjectingPrefs db) {
   for(int rep=0;rep<40;rep++) {
     ItqanFourSessionSnapshot current=ItqanFourSessionStore.read(db,geo);
     assertEquals(rep,current.reps);
     assertTrue("Each repetition must persist once",ItqanFourSessionStore.recordRep(db,current,geo));
   }
   ItqanFourSessionSnapshot ready=ItqanFourSessionStore.read(db,geo);
   assertEquals(40,ready.reps);
   return ready;
 }
 @Test public void completedFortyRepsCreditsExactlyOneSubblockAndPersistsCursor() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   seedV6(db);
   ItqanFourSessionSnapshot snapshot=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snapshot,geo));
   ItqanFourSessionSnapshot ready=completeRepetitions(db);
   List<String> due=ready.plan.sessions.get(0).blocks.get(0).lineIds;
   HifzPrefs prefs=new HifzPrefs(db);
   assertTrue(prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-13","first x40"));
   assertTrue(new HifzPrefs(db).progressionSnapshotV6().stabilized.containsAll(due));
   ItqanFourSessionSnapshot next=ItqanFourSessionStore.read(db,geo);
   assertNotNull(next);
   assertEquals(0,next.reps);
   int successful=db.commits;
   assertFalse("Stale completion must not credit twice",
     new HifzPrefs(db).completeFrozenItqanSubblockV6(ready,geo,"2026-10-13","duplicate"));
   assertEquals(successful,db.commits);
   assertTrue(new HifzPrefs(db).progressionSnapshotV6().stabilized.containsAll(due));
 }
 @Test public void failedFourDateBlockCommitDoesNotCreditOrAdvanceAndCanRetry() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   seedV6(db);
   ItqanFourSessionSnapshot snapshot=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snapshot,geo));
   ItqanFourSessionSnapshot ready=completeRepetitions(db);
   List<String> due=ready.plan.sessions.get(0).blocks.get(0).lineIds;
   db.failCommit=db.commits+1;
   HifzPrefs prefs=new HifzPrefs(db);
   assertFalse(prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-13","failed"));
   assertEquals(40,ItqanFourSessionStore.read(db,geo).reps);
   for(String id:due)assertFalse(new HifzPrefs(db).progressionSnapshotV6().stabilized.contains(id));
   db.failCommit=-1;
   assertTrue(prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-13","retry"));
   assertEquals(0,ItqanFourSessionStore.read(db,geo).reps);
   assertTrue(new HifzPrefs(db).progressionSnapshotV6().stabilized.containsAll(due));
 }
 @Test public void frozenNextDateCannotBeValidatedTwiceOnSameDayOrBackdated() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   seedV6(db);
   ItqanFourSessionSnapshot original=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,original,geo));
   HifzPrefs prefs=new HifzPrefs(db);
   int blocks=original.plan.sessions.get(0).blocks.size();
   assertTrue(blocks>0);
   for(int b=0;b<blocks;b++) {
     ItqanFourSessionSnapshot ready=completeRepetitions(db);
     assertEquals(0,ready.sessionIndex);
     assertEquals(b,ready.blockIndex);
     assertTrue(prefs.completeFrozenItqanSubblockV6(
         ready,geo,"2026-10-13","first date"));
   }
   ItqanFourSessionSnapshot next=ItqanFourSessionStore.read(db,geo);
   assertEquals(1,next.sessionIndex);
   assertEquals("2026-10-13",prefs.lastItqanDate());
   ItqanFourSessionSnapshot ready=completeRepetitions(db);
   int committed=db.commits;
   assertFalse("The next date must not be credited on the same day",
       prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-13","replayed"));
   assertFalse("A backward device clock must never credit a second date",
       prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-12","backdated"));
   assertEquals("No progress on rejected dates",committed,db.commits);
   assertEquals(40,ItqanFourSessionStore.read(db,geo).reps);
   assertTrue("The same saved 40 repetitions remain valid on the proper next day",
       prefs.completeFrozenItqanSubblockV6(ready,geo,"2026-10-14","second date"));
 }
 @Test public void finalFourDateCreditPinsPhysicalCursorWithoutLosingTheFollowingLine() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();seedV6(db);
   ItqanFourSessionSnapshot original=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,original,geo));
   String lastOriginalId=geo.lineIdsOnPage(521).get(geo.lineIdsOnPage(521).size()-1);
   int nextPhysical=geo.linesForExactIds(Collections.singletonList(lastOriginalId)).get(0).globalIndex+1;
   assertTrue(nextPhysical<geo.lineCount());
   String expectedNext=geo.line(nextPhysical).id;
   LocalDate day=LocalDate.of(2026,10,13);
   int completed=0;
   while(ItqanFourSessionStore.read(db,geo)!=null) {
     ItqanFourSessionSnapshot ready=completeRepetitions(db);
     int sessionBefore=ready.sessionIndex;
     assertTrue(new HifzPrefs(db).completeFrozenItqanSubblockV6(ready,geo,
          day.toString(),"atomic physical cursor "+completed));
     ItqanFourSessionSnapshot after=ItqanFourSessionStore.read(db,geo);
     if(after!=null&&after.sessionIndex>sessionBefore)day=day.plusDays(1);
     assertTrue("Four-date plan must terminate",++completed<=30);
   }
   assertEquals(expectedNext,db.getString("itqanFourNextPhysicalLineIdV1",""));
   assertEquals("The plan was closed after full V6 credit",
        "",db.getString(ItqanFourSessionStore.KEY,""));
   ItqanFourSessionSnapshot resumed=new HifzPrefs(db).openOrResumeFrozenItqan(
       geo,day.plusDays(1));
   assertNotNull("A fresh window follows the credited QCF line",resumed);
   assertEquals(expectedNext,
       resumed.plan.sessions.get(0).blocks.get(0).lineIds.get(0));
 }
 @Test public void cleanOpeningPinsExactlyThePreviewedPhysicalWindowAndSurvivesReboot() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();seedV6(db);
   sample(db,520); // seed the first-tail rotation at a real QCF page
   HifzPrefs prefs=new HifzPrefs(db);
   ItqanFourSessionPolicy.Plan preview=prefs.previewFourDateItqanPlan(geo);
   assertNotNull(preview);
   LocalDate day=LocalDate.of(2026,10,13);
   ItqanFourSessionSnapshot opened=prefs.openOrResumeFrozenItqan(geo,day);
   assertNotNull(opened);
   assertEquals(preview.lines(),opened.plan.lines());
   assertEquals(preview.sessions.get(0).blocks.get(0).lineIds.get(0),
       opened.plan.sessions.get(0).blocks.get(0).lineIds.get(0));
   int committed=db.commits;
   assertEquals(opened.encode(),new HifzPrefs(db).openOrResumeFrozenItqan(
       geo,day.plusDays(1)).encode());
   assertEquals("Restart must never reopen/repartition the same window",committed,db.commits);
 }
 @Test public void legacyOpenBlocksNeverStartNewFourDatePlanOrResetRotation() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();seedV6(db);
   ItqanFourSessionSnapshot sample=sample(db,520);
   db.disk.put("itqanRep",27);
   db.disk.put("itqanUnitStart",sample.originCursor);
   db.disk.put("itqanUnitEnd",sample.originCursor);
   int prior=db.commits;
   assertNull(new HifzPrefs(db).openOrResumeFrozenItqan(
       geo,LocalDate.of(2026,10,13)));
   assertEquals(prior,db.commits);
   assertFalse(db.contains(ItqanFourSessionStore.KEY));
   assertEquals(27,db.getInt("itqanRep",0));
 }
 @Test public void frozenCurrentEntryMustNeverInvokeLegacyRotationPicker() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();seedV6(db);
   ItqanFourSessionSnapshot snap=sample(db,603);
   assertTrue(ItqanFourSessionStore.open(db,snap,geo));
   int prior=db.commits;
   AnchoringQueue.Entry entry=new HifzPrefs(db).currentAnchoringEntry(geo);
   assertNotNull(entry);
   assertEquals(snap.plan.sessions.get(0).blocks.get(0).lineIds.get(0),
       geo.linesForExactIds(Collections.singletonList(
         snap.plan.sessions.get(0).blocks.get(0).lineIds.get(0))).get(0).id);
   assertEquals("Dashboard must never flip the rotation leg",prior,db.commits);
   assertEquals(snap.originCursor,db.getString(ItqanRegimeStore.CURSOR,""));
 }
 @Test public void aCorruptSnapshotNeverResetsToCurrentGeometry() {
   FaultInjectingPrefs db=new FaultInjectingPrefs();
   ItqanFourSessionSnapshot snap=sample(db,520);
   assertTrue(ItqanFourSessionStore.open(db,snap,geo));
   db.disk.put(ItqanFourSessionStore.KEY,"{\"version\":999}");
   try{ItqanFourSessionStore.read(db,geo);fail("Corrupt saved plan silently accepted");}
   catch(IllegalStateException expected){}
 }
}
