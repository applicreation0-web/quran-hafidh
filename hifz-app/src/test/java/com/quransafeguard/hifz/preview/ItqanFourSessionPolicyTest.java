package com.quransafeguard.hifz.preview;
import org.junit.Test;
import org.junit.BeforeClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Full geometry / adversarial partition smoke tests; a planning proof only. */
public final class ItqanFourSessionPolicyTest {
 static GeometryRepository geo;
 @BeforeClass public static void load()throws Exception{
   Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
   if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
   geo=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
 }
 private static void assertPartition(ItqanFourSessionPolicy.Plan plan,List<GeometryRepository.LineMeta> original) {
   assertEquals(4,plan.sessions.size());
   List<String> found=new ArrayList<>();
   for(ItqanFourSessionPolicy.Session session:plan.sessions) {
     int count=0;
     for(ItqanFourSessionPolicy.Block part:session.blocks){
       assertTrue(part.lineIds.size()>0&&part.lineIds.size()<=11);
       List<GeometryRepository.LineMeta> resolved=geo.linesForExactIds(part.lineIds);
       for(GeometryRepository.LineMeta line:resolved) {
         count++;found.add(line.id);
         for(com.quransafeguard.hifz.core.VerseRef ref:line.verses)
           assertEquals("Never mix surahs in a ×40 block",part.surah,ref.getSurah());
       }
     }
     assertEquals(session.lineCount,count);
   }
   assertEquals(original.size(),found.size());
   for(int i=0;i<found.size();i++)assertEquals(original.get(i).id,found.get(i));
   assertEquals(new HashSet<>(found).size(),found.size());
 }
 @Test public void all604PagesAndEveryPairHaveExactFourWayCoverage(){
   for(int page=1;page<=604;page++){
     List<String> ids=new ArrayList<>(geo.lineIdsOnPage(page));
     if(page<604)ids.addAll(geo.lineIdsOnPage(page+1));
     List<GeometryRepository.LineMeta> lines=geo.linesForExactIds(ids);
     ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.plan(lines,true);
     assertPartition(plan,lines);
     assertEquals(page,plan.firstPage);
     assertEquals(Math.min(604,page+1),plan.lastPage);
   }
 }
 @Test public void qafDhariyat519To522NeverMixesSurahs() {
   for(int page=519;page<=521;page++){
     List<String> ids=new ArrayList<>(geo.lineIdsOnPage(page));
     ids.addAll(geo.lineIdsOnPage(page+1));
     List<GeometryRepository.LineMeta> lines=geo.linesForExactIds(ids);
     assertPartition(ItqanFourSessionPolicy.plan(lines,true),lines);
   }
 }
 @Test public void wholeMufassalFromPages562To604NeverDoubleCredits() {
   for(int page=562;page<604;page++){
     int i=geo.linesForExactIds(geo.lineIdsOnPage(page)).get(0).globalIndex;
     LinkedHashSet<String> eligible=new LinkedHashSet<>(geo.lineIdsOnPage(page));
     eligible.addAll(geo.lineIdsOnPage(page+1));
     ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.fromGeometry(geo,i,eligible);
     assertTrue(plan.fullWindow);
     assertPartition(plan,geo.linesForExactIds(new ArrayList<>(eligible)));
   }
 }
 @Test public void partialCorpusMustNeverSkipMissingLinesToFillQuota(){
   List<String> left=geo.lineIdsOnPage(520);
   List<String> right=geo.lineIdsOnPage(521);
   int start=geo.linesForExactIds(left).get(0).globalIndex;
   LinkedHashSet<String> eligible=new LinkedHashSet<>(left);
   eligible.addAll(right);
   eligible.remove(left.get(Math.min(3,left.size()-1)));
   ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.fromGeometry(geo,start,eligible);
   assertFalse(plan.fullWindow);
   assertEquals(Math.min(3,left.size()-1),plan.lines());
 }
 @Test public void oneLineNearNasYieldsThreeEmptySlotsWithoutJump(){
   List<String> p604=geo.lineIdsOnPage(604);
   List<GeometryRepository.LineMeta> last=geo.linesForExactIds(
       Collections.singletonList(p604.get(p604.size()-1)));
   ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.plan(last,false);
   assertEquals(1,plan.sessions.get(0).lineCount);
   for(int i=1;i<4;i++)assertEquals(0,plan.sessions.get(i).lineCount);
 }
 @Test public void lastPageOfNasHasNoFictitiousSecondPage() {
   List<String> ids=geo.lineIdsOnPage(604);
   List<GeometryRepository.LineMeta> actual=geo.linesForExactIds(ids);
   ItqanFourSessionPolicy.Plan p=ItqanFourSessionPolicy.fromGeometry(
      geo,actual.get(0).globalIndex,new LinkedHashSet<>(ids));
   assertFalse(p.fullWindow);
   assertEquals(604,p.firstPage);assertEquals(604,p.lastPage);
   assertPartition(p,actual);
 }
 @Test(expected=IllegalArgumentException.class) public void sameLineTwiceRejected(){
   List<GeometryRepository.LineMeta> real=geo.linesForExactIds(geo.lineIdsOnPage(518));
   ItqanFourSessionPolicy.plan(Arrays.asList(real.get(0),real.get(0)),true);
 }
}
