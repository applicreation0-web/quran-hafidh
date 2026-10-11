package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Work P0-01 repro inverted: a final Itqan validation must be atomic. */
public final class AtomicItqanRotationRegressionTest {
 static GeometryRepository geometry;
 @BeforeClass public static void load()throws Exception{
   Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
   if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
   geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
 }
 static FaultInjectingPrefs disk(){
   FaultInjectingPrefs p=new FaultInjectingPrefs();
   p.disk.put("schema",6);
   for(String key:new String[]{"v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds",
     "v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"})p.disk.put(key,"[]");
   p.disk.put("sabqiStart","2:75");p.disk.put("sabqiEnd","48:29");
   p.disk.put("sabqiLineCursor",geometry.firstLineIndex(new VerseRef(2,75)));
   p.disk.put("itqanRanges","[]");p.disk.put("promotedRanges","[]");
   p.disk.put("unconsolidatedPromotedRanges","[]");p.disk.put("forcedPromotedRanges","[]");
   p.disk.put("anchoringQueue","[]");p.disk.put("anchoringQueueInitialized",true);
   return p;
 }
 static AnchoringQueue.Entry terminal(){
   List<AnchoringQueue.Entry> units=HifzPrefs.physicalUnitsInLeg(
     ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,new VerseRef(2,75),geometry,
     ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS);
   return units.get(units.size()-1);
 }
 private static void startAt(FaultInjectingPrefs disk,VerseRef start){
   disk.disk.put(ItqanRegimeStore.LEG,ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS.name());
   disk.disk.put(ItqanRegimeStore.CURSOR,start.toString());
   disk.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,false);
 }
 @Test public void rejectedCommitLeavesNeitherCreditNorRotationNorPostNasLatch() {
   FaultInjectingPrefs disk=disk();
   AnchoringQueue.Entry unit=terminal();
   VerseRef start=GeometryRepository.parseVerse(unit.start),end=GeometryRepository.parseVerse(unit.end);
   assertEquals(new VerseRef(114,6),end);
   startAt(disk,start);
   HifzPrefs prefs=new HifzPrefs(disk);
   List<String> lines=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
   int before=disk.commits;
   disk.failCommit=before+1;
   assertFalse(prefs.completeStabilizationBlockV6(lines,1,true,start,end,new VerseRef(49,1),
       "2026-10-11","audit-atomic",Collections.emptyList()));
   assertEquals(before+1,disk.commits);
   HifzPrefs reboot=new HifzPrefs(disk);
   assertFalse(reboot.progressionSnapshotV6().stabilized.containsAll(lines));
   assertEquals("",reboot.lastItqanDate());
   assertEquals(start,reboot.itqanRotationState().cursor);
   assertFalse(reboot.itqanPostNasMaintenance());
 }
 @Test public void successPersistsCreditRotationAndNasLatchInExactlyOneCommit() {
   FaultInjectingPrefs disk=disk();
   AnchoringQueue.Entry unit=terminal();
   VerseRef start=GeometryRepository.parseVerse(unit.start),end=GeometryRepository.parseVerse(unit.end);
   startAt(disk,start);
   HifzPrefs prefs=new HifzPrefs(disk);
   List<String> lines=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
   int before=disk.commits;
   assertTrue(prefs.completeStabilizationBlockV6(lines,1,true,start,end,new VerseRef(49,1),
      "2026-10-11","audit-atomic",Collections.emptyList()));
   assertEquals("one durable transaction",before+1,disk.commits);
   HifzPrefs reboot=new HifzPrefs(disk);
   assertTrue(reboot.progressionSnapshotV6().stabilized.containsAll(lines));
   assertEquals("2026-10-11",reboot.lastItqanDate());
   assertTrue(reboot.itqanPostNasMaintenance());
   assertEquals(ItqanRotationPolicy.advancedPast(
       new ItqanRotationPolicy.State(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,start,false),end).cursor,
       reboot.itqanRotationState().cursor);
 }
}
