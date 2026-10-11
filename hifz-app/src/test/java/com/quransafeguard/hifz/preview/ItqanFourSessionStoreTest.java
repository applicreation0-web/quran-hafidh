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
   assertEquals(0,next.sessionIndex);
   assertEquals(1,next.blockIndex);
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
