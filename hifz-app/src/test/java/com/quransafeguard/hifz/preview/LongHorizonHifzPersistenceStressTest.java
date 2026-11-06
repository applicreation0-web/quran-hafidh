package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.*;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

/** Reproducible persistence stress: 1000 real QCF line IDs, simulated
 *  app restarts, duplicate validations and 52 consecutive snowball weeks.
 *  Exercise the genuine HifzPrefs V6 production transition methods.
 */
public final class LongHorizonHifzPersistenceStressTest {
 static InMemoryPrefs freshV6Disk(){
   InMemoryPrefs db=new InMemoryPrefs();
   db.disk.put("schema",6);
        db.disk.put("sabqiStart","1:1");db.disk.put("sabqiEnd","114:6");
   for(String key:new String[]{"v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds",
     "v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"})db.disk.put(key,"[]");
   return db;
 }
 private static GeometryRepository geometry(){
   try{
      java.nio.file.Path p=java.nio.file.Paths.get("app/src/main/assets/reader109/geometry.json");
      if(!java.nio.file.Files.exists(p))p=java.nio.file.Paths.get("../app/src/main/assets/reader109/geometry.json");
      return GeometryRepository.fromJson(
        new String(java.nio.file.Files.readAllBytes(p),java.nio.charset.StandardCharsets.UTF_8));
   }catch(Exception e){throw new AssertionError("Canonical QCF fixture missing",e);}
 }
 @Test public void thousandLinesSurviveThreePromotionsAndOneThousandDuplicateEvents() {
   InMemoryPrefs db=freshV6Disk();
   HifzPrefs prefs=new HifzPrefs(db);
   List<String> lines=new ArrayList<>();
   GeometryRepository geo=geometry();
   for(int i=0;i<1000;i++)lines.add(geo.line(i).id);
   for(int start=0;start<1000;start+=25){
     List<String> batch=lines.subList(start,start+25);
     prefs.transitionV6Lines(batch,HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
     prefs=new HifzPrefs(db); // real commit then reopen, never reconstruct state from memory
     prefs.transitionV6Lines(batch,HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
     prefs.transitionV6Lines(batch,HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
     prefs=new HifzPrefs(db);
     prefs.transitionV6Lines(batch,HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
     prefs.transitionV6Lines(batch,HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
     prefs=new HifzPrefs(db);
   }
   HifzPrefs.ProgressionSnapshot before=prefs.progressionSnapshotV6();
   assertEquals(1000,before.acquired.size());
   assertTrue(before.learned.isEmpty()&&before.stabilized.isEmpty());
   assertEquals(new LinkedHashSet<>(lines),before.acquired);
   for(int i=0;i<1000;i++){
     prefs.transitionV6Lines(Collections.singletonList(lines.get(i)),
         HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
   }
   HifzPrefs.ProgressionSnapshot after=new HifzPrefs(db).progressionSnapshotV6();
   assertEquals(before.acquired,after.acquired);
   assertEquals(1000,after.acquired.size());
 }
 @Test public void fiftyTwoWeeksOfSnowballsAreIdempotentAndDoNotLeakAcrossWeeks() throws Exception {
   SnowballValidationIdempotenceTest.setup();
   InMemoryPrefs db=freshV6Disk();HifzPrefs p=new HifzPrefs(db);
   ConsolidationCycleEngine.Family f=ConsolidationCycleEngine.Family.LEARNING;
   LocalDate monday=LocalDate.of(2026,10,5);
   for(int w=0;w<52;w++) {
     LocalDate day=monday.plusWeeks(w);
     SnowballValidationIdempotenceTest.morning(p,f,day,w*5);
     SnowballValidationIdempotenceTest.close(p,f,day,SnowballValidationIdempotenceTest.open(p,f,day));
     p=new HifzPrefs(db);assertFalse(p.snowballEveningPending(f,day.plusDays(1)));
     assertFalse(p.completeSnowballEvening(f,day.toString()));
   }
 }
 @Test public void aSkippedPromotionIsAlwaysRejectedEvenAfterRestart() {
   InMemoryPrefs db=freshV6Disk();
   HifzPrefs prefs=new HifzPrefs(db);
   List<String> ids=Collections.singletonList(geometry().line(0).id);
   try {
     prefs.transitionV6Lines(ids,HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
     fail("Promoting NONE directly to Acquis must fail");
   }catch(IllegalStateException expected){}
   HifzPrefs.ProgressionSnapshot state=new HifzPrefs(db).progressionSnapshotV6();
   assertTrue(state.learned.isEmpty()&&state.stabilized.isEmpty()&&state.acquired.isEmpty());
 }
}
