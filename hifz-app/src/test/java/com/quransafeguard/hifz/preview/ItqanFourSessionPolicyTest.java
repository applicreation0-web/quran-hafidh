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
 @Test public void entireMushafExposesCompositeCapacityBeyondFourDaySlots() {
   int maxBlocks=0,worstStart=-1,multipleSurahPairs=0;
   for(int page=1;page<604;page++){
     List<String> ids=new ArrayList<>(geo.lineIdsOnPage(page));
     ids.addAll(geo.lineIdsOnPage(page+1));
     ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.plan(
       geo.linesForExactIds(ids),true);
     int blocks=0;
     for(ItqanFourSessionPolicy.Session session:plan.sessions)
       blocks+=session.blocks.size();
     if(blocks>maxBlocks){maxBlocks=blocks;worstStart=page;}
     if(blocks>4)multipleSurahPairs++;
   }
   System.out.println("ITQAN COMPOSITE CAPACITY: max subblocks="+maxBlocks
       +" at first page="+worstStart+"; pairs exceeding four daily snowball units="
       +multipleSurahPairs);
   assertTrue("At least one Mufassal pair has more than four surah-local subblocks",
       multipleSurahPairs>0);
   assertTrue("Two-page target must fit physical maximum without discarded units",maxBlocks<=30);
   // Every future weekly ledger must account for ALL these subblocks,
   // rather than silently truncate at itqanDaysPerWeek() (four).
 }
 @Test public void successiveTwoPageWindowsCoverTailWithoutGapsOrDuplicates() {
   // Physical cursor chaining is the only safe future runtime handoff:
   // the legacy ~22-line verse-unit picker can skip residual physical lines.
   int first=geo.firstLineIndex(new com.quransafeguard.hifz.core.VerseRef(49,1));
   int stop=geo.lineCount();
   LinkedHashSet<String> eligible=new LinkedHashSet<>();
   for(int i=first;i<stop;i++)eligible.add(geo.line(i).id);
   int cursor=first,windows=0;
   while(cursor<stop) {
     ItqanFourSessionPolicy.Plan plan=ItqanFourSessionPolicy.fromGeometry(geo,cursor,eligible);
     assertTrue("A frozen window must advance the physical cursor",plan.lines()>0);
     int physical=cursor;
     for(ItqanFourSessionPolicy.Session session:plan.sessions)
       for(ItqanFourSessionPolicy.Block block:session.blocks)
         for(String id:block.lineIds) {
           assertEquals("No line may be repeated or silently skipped in Itqan",
               geo.line(physical).id,id);
           physical++;
         }
     assertEquals(cursor+plan.lines(),physical);
     cursor=physical;
     assertTrue("Finite physical Itqan windows expected",++windows<=604);
   }
   assertEquals("Every physical line through An-Nas must be assigned once",stop,cursor);
 }
 @Test(expected=IllegalArgumentException.class) public void sameLineTwiceRejected(){
   List<GeometryRepository.LineMeta> real=geo.linesForExactIds(geo.lineIdsOnPage(518));
   ItqanFourSessionPolicy.plan(Arrays.asList(real.get(0),real.get(0)),true);
 }
}
