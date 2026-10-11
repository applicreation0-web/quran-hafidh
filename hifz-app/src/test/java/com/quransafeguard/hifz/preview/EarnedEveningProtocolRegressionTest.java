package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Work P0-02: a Sunday final cannot credit without completed weekday protocol. */
public final class EarnedEveningProtocolRegressionTest {
 static GeometryRepository geometry;
 @BeforeClass public static void setup()throws Exception{
   Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
   if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
   geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
 }
 static InMemoryPrefs disk(){
   InMemoryPrefs db=new InMemoryPrefs();
   db.disk.put("schema",6);
   for(String key:new String[]{"v6LearnedLineIds","v6StabilizedLineIds",
     "v6AcquiredCreditLineIds","v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"})
     db.disk.put(key,"[]");
   return db;
 }
 static List<String> five(){
   int start=geometry.firstLineIndex(new VerseRef(2,75));
   List<String> ids=new ArrayList<>();
   for(int i=0;i<5;i++)ids.add(geometry.line(start+i).id);
   return ids;
 }
 static ConsolidationCycleEngine.Session ready(
     ConsolidationCycleEngine.Family family,String cycleId,
     List<ConsolidationCycleEngine.Unit> units) {
   assertFalse("Need real physical units",units.isEmpty());
   ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
   ConsolidationCycleEngine.Cycle cycle=null;
   for(ConsolidationCycleEngine.Unit unit:units){
     cycle=cycle==null?engine.startCycle(cycleId,family,unit):engine.addUnit(cycle,unit);
   }
   ConsolidationCycleEngine.Session session=engine.openSession(cycle,"proof");
   while(!session.readyToClose())session=engine.recordRepetition(session);
   return session;
 }
 static void assertUnacquired(HifzPrefs p,List<String> ids){
   HifzPrefs.ProgressionSnapshot snapshot=p.progressionSnapshotV6();
   for(String id:ids)assertFalse("Premature Acquis: "+id,snapshot.acquired.contains(id));
 }
 @Test public void compositeItqanEveningEarnsEveryNewSevenTimeReceipt() throws Exception {
   InMemoryPrefs db=disk();
   List<String> ids=geometry.lineIdsOnPage(603);
   String earned=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
       Collections.singletonList(ids.get(0)));
   String newA=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
       Collections.singletonList(ids.get(1)));
   String newB=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
       Collections.singletonList(ids.get(2)));
   db.disk.put("stabilizationSnowballWeekAnchor","2026-10-12");
   db.disk.put("stabilizationSnowballUnitIds",
       new org.json.JSONArray().put(earned).put(newA).put(newB).toString());
   db.disk.put("v6StabilizationEarnedEveningUnitsV1",
       new org.json.JSONArray().put(earned).toString());
   HifzPrefs p=new HifzPrefs(db);
   LocalDate day=LocalDate.of(2026,10,13);
   List<ConsolidationCycleEngine.Unit> cycle=p.stabilizedConsolidationUnits(day);
   assertEquals(3,cycle.size());
   assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS,cycle.get(0).protocol());
   for(int i=1;i<3;i++)assertEquals(
       ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN,cycle.get(i).protocol());
   ConsolidationCycleEngine.Session session=ready(ConsolidationCycleEngine.Family.STABILIZATION,
       "stabilization-"+day,cycle);
   assertTrue(p.persistConsolidationSession(session));
   assertTrue(p.completeSnowballEvening(
       ConsolidationCycleEngine.Family.STABILIZATION,day.toString(),session));
   org.json.JSONArray receipts=new org.json.JSONArray(
       db.getString("v6StabilizationEarnedEveningUnitsV1","[]"));
   assertEquals(3,receipts.length());
   assertTrue(p.stabilizedConsolidationUnits(day).isEmpty());
   assertFalse(p.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,day));
 }
 @Test public void missedStabilizationEveningNeverPromotesDespiteFinalRepetitions(){
   InMemoryPrefs db=disk();
   HifzPrefs p=new HifzPrefs(db);
   LocalDate tue=LocalDate.of(2026,1,6),sun=tue.plusDays(5);
   List<String> ids=five();
   List<GeometryRepository.LineMeta> lines=geometry.linesForExactIds(ids);
   VerseRef first=lines.get(0).verses.get(0);
   List<VerseRef> lastVerses=lines.get(lines.size()-1).verses;
   VerseRef last=lastVerses.get(lastVerses.size()-1);
   assertTrue(p.completeStabilizationBlockV6(ids,1,true,first,last,last,
       tue.toString(),"missed x7",Collections.emptyList()));
   assertTrue(p.stabilizationSnowballFinalUnits(sun).isEmpty());
   String encoded=ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids);
   ConsolidationCycleEngine.Session finalSession=ready(
       ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-"+sun,
       Collections.singletonList(new ConsolidationCycleEngine.Unit(
           encoded,ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED)));
   try {
     p.completeConsolidationSessionV6(finalSession,geometry,sun.toString(),"unsafe");
     fail("A missed x7 must never graduate to Acquis");
   }catch(IllegalStateException expected){}
   assertUnacquired(new HifzPrefs(db),ids);
   assertTrue(new HifzPrefs(db).progressionSnapshotV6().stabilized.containsAll(ids));
 }
 @Test public void verifiedStabilizationSevenTimesUnlocksThatExactBlock(){
   InMemoryPrefs db=disk();HifzPrefs p=new HifzPrefs(db);
   LocalDate tue=LocalDate.of(2026,1,6),sun=tue.plusDays(5);
   List<String> ids=five();
   List<GeometryRepository.LineMeta> ls=geometry.linesForExactIds(ids);
   VerseRef first=ls.get(0).verses.get(0),last=ls.get(ls.size()-1).verses.get(ls.get(ls.size()-1).verses.size()-1);
   assertTrue(p.completeStabilizationBlockV6(ids,1,true,first,last,last,tue.toString(),"x7",Collections.emptyList()));
   ConsolidationCycleEngine.Session evening=ready(
       ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-"+tue,
       p.stabilizedConsolidationUnits(tue));
   assertTrue(p.persistConsolidationSession(evening));
   assertTrue(p.completeSnowballEvening(
       ConsolidationCycleEngine.Family.STABILIZATION,tue.toString(),evening));
   HifzPrefs reboot=new HifzPrefs(db);
   assertFalse(reboot.stabilizationSnowballFinalUnits(sun).isEmpty());
   ConsolidationCycleEngine.Session finalSession=ready(
       ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-"+sun,
       reboot.stabilizationSnowballFinalUnits(sun));
   assertTrue(reboot.completeConsolidationSessionV6(finalSession,geometry,sun.toString(),"earned"));
   assertTrue(new HifzPrefs(db).progressionSnapshotV6().acquired.containsAll(ids));
 }
 @Test public void missedLearningEveningCannotBeSubstitutedBySundayFinal(){
   InMemoryPrefs db=disk();HifzPrefs p=new HifzPrefs(db);
   LocalDate mon=LocalDate.of(2026,1,5),sun=mon.plusDays(6);
   List<String> ids=five();
   int firstIndex=geometry.linesForExactIds(ids).get(0).globalIndex;
   assertTrue(p.completeSabqiBlockV6(firstIndex,firstIndex+4,firstIndex+5,
        ids,mon.toString(),"missed x10"));
   assertTrue(p.learningSnowballFinalUnits(sun).isEmpty());
   ConsolidationCycleEngine.Session finalSession=ready(
       ConsolidationCycleEngine.Family.LEARNING,"learning-final-"+sun,
       Collections.singletonList(new ConsolidationCycleEngine.Unit(
          ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids),
          ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED)));
   try {
     p.completeLearningConsolidationSessionV6(finalSession,geometry,sun.toString(),"unsafe");
     fail("Missed Renforcement x10 must never be credited");
   }catch(IllegalStateException expected){}
   assertUnacquired(new HifzPrefs(db),ids);
 }
}
