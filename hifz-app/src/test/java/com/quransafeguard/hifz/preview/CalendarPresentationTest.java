package com.quransafeguard.hifz.preview;
import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;
public final class CalendarPresentationTest {
    private final LocalDate today=LocalDate.of(2026,10,26);
    @Test public void fourActivitiesKeepSeparateReceiptsAndFullDetails(){
        WeeklyDashboardPlanner.Row row=new WeeklyDashboardPlanner.Row(today,"Lun",
            "✓ Révision active · 10 min + Révision passive · 10 min · Al-Baqarah 1–12",
            "Apprentissage · 5 lignes · ×3 + Renforcement · révision cumulative","À faire");
        CalendarPresentation.Day day=CalendarPresentation.day(row,today);
        assertEquals(4,day.activities.size());assertEquals(1,day.completed);
        assertEquals("Partiellement terminé",day.state);
        assertEquals("Al-Baqarah 1–12",day.activities.get(1).detail.split(" · ")[2]);
        assertEquals(HifzSessionActivity.SABQI,day.activities.get(2).mode);
    }
    @Test public void noSessionIsNeverCompleted(){
        CalendarPresentation.Day day=CalendarPresentation.day(new WeeklyDashboardPlanner.Row(today,"Lun","—","—","Parcours non démarré"),today);
        assertEquals(0,day.activities.size());assertEquals("Aucune séance",day.state);
    }
    @Test public void completedAndFutureAndUnverifiedAreDistinct(){
        assertEquals("Terminé",CalendarPresentation.day(new WeeklyDashboardPlanner.Row(today,"Lun","✓ Finale Sabqi","—","Validé"),today).state);
        assertEquals("À venir",CalendarPresentation.day(new WeeklyDashboardPlanner.Row(today.plusDays(1),"Mar","Révision active · 5 min","—","À faire"),today).state);
        assertEquals("En attente",CalendarPresentation.day(new WeeklyDashboardPlanner.Row(today,"Lun","Matin · détails à vérifier","Soir · détails à vérifier","Projection à vérifier"),today).state);
    }
    @Test public void emptyFinalIsNotAnActivityOrCompletion(){
        CalendarPresentation.Day day=CalendarPresentation.day(new WeeklyDashboardPlanner.Row(today,"Dim","Finale Sabqi · aucun passage admissible","—","Aucune finale à valider"),today);
        assertEquals(0,day.activities.size());assertEquals("Aucune séance",day.state);
    }
    @Test public void dateNavigationDoesNotReassignActivities(){
        for(int n:new int[]{28,56,180,365})for(int i=0;i<n;i++){
            LocalDate date=today.plusDays(i);WeeklyDashboardPlanner.Row row=new WeeklyDashboardPlanner.Row(date,"Jour","Révision active · 8 min","—","À faire");
            CalendarPresentation.Day day=CalendarPresentation.day(row,today);
            assertEquals(date,day.date);assertEquals(row.morning,day.activities.get(0).detail);
        }
        assertEquals(LocalDate.of(2027,1,4),CalendarPresentation.monday(LocalDate.of(2027,1,8)));
        assertEquals(LocalDate.of(2026,12,28),CalendarPresentation.monday(LocalDate.of(2027,1,1)));
    }
}
