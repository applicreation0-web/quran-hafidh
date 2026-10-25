package com.quransafeguard.hifz.preview;
import com.quransafeguard.hifz.core.CadenceAction;
import java.time.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SevenDayCalendarMigrationTest {
 @Test public void manualAllocatesSixOrdinaryDaysPlusSundayFinales() {
   for(int n=0;n<=6;n++) {
     int learning=0,stabilization=0;
     for(DayOfWeek day:DayOfWeek.values()) {
       CadenceAction c=WeeklyProgramPolicy.actionFor(day,HifzPrefs.WeeklyProgram.MANUAL_SEVEN,n);
       if(c==CadenceAction.LEARNING)learning++;
       if(c==CadenceAction.STABILIZATION)stabilization++;
       if(day==DayOfWeek.SUNDAY)
         assertEquals("Sunday is finals only",CadenceAction.REVISION,c);
       else
         assertNotEquals("Weekday must have an ordinary lesson",CadenceAction.REVISION,c);
     }
     assertEquals(n,learning);
     assertEquals(6-n,stabilization);
   }
 }
 @Test public void existingProgressionPreservedAndMigrationStartsAtMonday() {
   InMemoryPrefs db=new InMemoryPrefs();
   db.disk.put("v6LearnedLineIds","[\"p520-l07\"]");
   db.disk.put("v6AcquiredCreditLineIds","[\"p518-l01\"]");
   db.disk.put("lastItqanDate","2026-10-09");
   db.disk.put("lastRecentSabqiReviewDate","2026-10-08");
   db.disk.put("itqanRep",21);
   Map<String,Object> before=new HashMap<>(db.disk);
   HifzPrefs prefs=new HifzPrefs(db);
   LocalDate sunday=LocalDate.of(2026,10,11);
   prefs.ensureSevenDayProgramme(sunday,false);
   assertEquals(HifzPrefs.WeeklyProgram.CLASSIC,prefs.weeklyProgramFor(sunday));
   assertEquals(HifzPrefs.WeeklyProgram.RECOMMENDED,prefs.weeklyProgramFor(sunday.plusDays(1)));
   for(Map.Entry<String,Object> item:before.entrySet())assertEquals(item.getKey(),item.getValue(),db.disk.get(item.getKey()));
   assertEquals(21,prefs.itqanRep());
   HifzPrefs reloaded=new HifzPrefs(db);
   reloaded.ensureSevenDayProgramme(sunday,false);
   assertEquals(HifzPrefs.WeeklyProgram.RECOMMENDED,reloaded.weeklyProgramFor(sunday.plusDays(1)));
 }
 @Test public void freshInstallUsesSixOrdinaryDaysPlusSundayFinales() {
   InMemoryPrefs db=new InMemoryPrefs();
   HifzPrefs prefs=new HifzPrefs(db);
   LocalDate sunday=LocalDate.of(2026,10,11);
   prefs.ensureSevenDayProgramme(sunday,true);
   assertEquals(HifzPrefs.WeeklyProgram.RECOMMENDED,prefs.weeklyProgramFor(sunday));
   assertEquals(CadenceAction.REVISION,WeeklyProgramPolicy.actionFor(DayOfWeek.SUNDAY,prefs.weeklyProgramFor(sunday),3));
 }
}
