package com.quransafeguard.hifz.preview;
import java.nio.file.*;import java.time.*;import java.util.*;import org.json.*;import static org.junit.Assert.*;
/** Local private backup read-only differential; no personal fixture in CI. */
public final class Calendar1179DifferentialReplay {
 public static void main(String[] args)throws Exception{
  GeometryRepository geometry=GeometryRepository.fromJson(Files.readString(Path.of(args[1])));
  JSONObject backup=new JSONObject(Files.readString(Path.of(args[0])));
  for(int horizon:new int[]{28,56,180,365}){
   PrivateBackupReplay.MemoryContext context=PrivateBackupReplay.MemoryContext.create();HifzBackup.restore(context,new JSONObject(backup.toString()));
   InMemoryPrefs disk=context.stores.get("quran_hifz_preview_v1");HifzPrefs prefs=new HifzPrefs(disk);
   DashboardLedger ledger=new DashboardLedger(context);HifzSpeedStore speed=new HifzSpeedStore(context);
   // Existing legacy slot migration/cache warm-up is identical for both reference and candidate.
   WeeklyDashboardPlannerBaseline baseline=new WeeklyDashboardPlannerBaseline(prefs,geometry,ledger,speed);
   LocalDate today=LocalDate.of(2026,10,26);
   List<WeeklyDashboardPlannerBaseline.Row> expected=baseline.week(today);
   Map<String,?> protectedBefore=new TreeMap<>(disk.getAll());
   WeeklyDashboardPlanner candidate=new WeeklyDashboardPlanner(prefs,geometry,ledger,speed);
   List<WeeklyDashboardPlanner.Row> actual=candidate.week(today);
   assertEquals(expected.size(),actual.size());
   for(int i=0;i<7;i++){
    assertEquals(expected.get(i).date,actual.get(i).date);assertEquals(expected.get(i).morning,actual.get(i).morning);
    assertEquals(expected.get(i).evening,actual.get(i).evening);assertEquals(expected.get(i).state,actual.get(i).state);
   }
   int rows=0;
   for(int offset=0;offset<horizon;offset+=7){
    List<WeeklyDashboardPlanner.Row> week=candidate.week(today,today.plusDays(offset));assertEquals(7,week.size());
    for(int i=0;i<7;i++){assertEquals(today.plusDays(offset+i),week.get(i).date);assertFalse(week.get(i).state.contains("Projection à vérifier"));rows++;}
    Map<String,?> after=disk.getAll();
    Set<String> keys=new HashSet<>(after.keySet());keys.addAll(protectedBefore.keySet());
    for(String key:keys)if(!key.startsWith("itqanLegPlanV1."))assertEquals("Protected key modified: "+key,protectedBefore.get(key),after.get(key));
   }
   System.out.println("PASS read-only shifted-window horizon="+horizon+" rows="+rows+" baseline first-week exact, acquired/OPEN/cursors/config unchanged");
  }
  PrivateBackupReplay.MemoryContext context=PrivateBackupReplay.MemoryContext.create();HifzBackup.restore(context,new JSONObject(backup.toString()));
  InMemoryPrefs disk=context.stores.get("quran_hifz_preview_v1");HifzPrefs prefs=new HifzPrefs(disk);
  prefs.saveItqanRotationState(new ItqanRotationPolicy.State(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT,new com.quransafeguard.hifz.core.VerseRef(2,1),true));
  String originalFinal=prefs.unfinishedConsolidationSessionDate(ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-");
  DashboardLedger ledger=new DashboardLedger(context);HifzSpeedStore speed=new HifzSpeedStore(context);
  for(int week=0;week<8;week++)for(WeeklyDashboardPlanner.Row row:new WeeklyDashboardPlanner(prefs,geometry,ledger,speed).week(LocalDate.of(2026,10,26),LocalDate.of(2026,10,26).plusWeeks(week)))
      if(row.date.getDayOfWeek()==DayOfWeek.SUNDAY)assertFalse("No new post-Nas final on "+row.date,row.evening.contains("Finale Itqān"));
  assertEquals(originalFinal,prefs.unfinishedConsolidationSessionDate(ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-"));
  System.out.println("PASS post-Nas eight future Sundays: no generated Itqan finale, historical OPEN original date unchanged");
 }
}
