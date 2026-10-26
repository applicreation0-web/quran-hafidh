package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable display adapter. No Context, preference, clock mutation or scheduling API. */
final class CalendarPresentation {
    static final class Activity {
        final String title, detail, mode, period;
        final boolean completed;
        Activity(String title,String detail,String mode,String period,boolean completed){
            this.title=title;this.detail=detail;this.mode=mode;this.period=period;this.completed=completed;
        }
    }
    static final class Day {
        final LocalDate date;
        final List<Activity> activities;
        final int completed;
        final String state;
        Day(LocalDate date,List<Activity> activities,int completed,String state){
            this.date=date;this.activities=Collections.unmodifiableList(activities);
            this.completed=completed;this.state=state;
        }
    }
    static LocalDate monday(LocalDate date){return date.minusDays(date.getDayOfWeek().getValue()-1);}
    static Day day(WeeklyDashboardPlanner.Row row,LocalDate today){
        ArrayList<Activity> out=new ArrayList<>();
        append(out,row.morning,"MATIN");append(out,row.evening,"SOIR");
        int done=0;for(Activity a:out)if(a.completed)done++;
        boolean unknown=row.state.contains("vérifier")||row.state.contains("inconnu");
        String state=unknown?"En attente":out.isEmpty()?"Aucune séance"
            :done==out.size()?"Terminé":done>0?"Partiellement terminé"
            :row.state.contains("En cours")?"En cours"
            :row.date.isAfter(today)?"À venir":row.date.isBefore(today)?"En attente":"À faire";
        return new Day(row.date,out,done,state);
    }
    private static void append(List<Activity> out,String raw,String period){
        if(raw==null||raw.trim().isEmpty()||"—".equals(raw))return;
        // The planner's literal separator denotes independent activities, never validations.
        for(String item:raw.replace(" · ✓ Finale Sabqi"," + ✓ Finale Sabqi")
                .replace(" · Finale Sabqi"," + Finale Sabqi").split(" \\+ ")){
            if(item.contains("aucun passage admissible")||item.contains("aucune unité à stabiliser")
                    ||item.contains("fenêtre terminée"))continue;
            boolean done=item.startsWith("✓ ");
            String detail=done?item.substring(2):item;
            int split=detail.indexOf(" · ");
            String title=split<0?detail:detail.substring(0,split);
            String mode=modeFor(title);
            // Projection failures are shown as an explicit warning, not phantom activities.
            if(mode!=null)out.add(new Activity(title,detail,mode,period,done));
        }
    }
    private static String modeFor(String title){
        if(title.startsWith("Finale Sabqi"))return HifzSessionActivity.LEARNING_FINAL;
        if(title.startsWith("Finale Itqān"))return HifzSessionActivity.CONSOLIDATION_FINAL;
        if(title.startsWith("Révision active"))return HifzSessionActivity.MURAJAAH_ACTIVE;
        if(title.startsWith("Révision passive"))return HifzSessionActivity.MURAJAAH;
        if(title.startsWith("Apprentissage"))return HifzSessionActivity.SABQI;
        if(title.startsWith("Stabilisation")||title.startsWith("Maintien"))return HifzSessionActivity.ITQAN;
        if(title.startsWith("Renforcement"))return HifzSessionActivity.LEARNING_CONSOLIDATION;
        if(title.startsWith("Consolidation"))return HifzSessionActivity.RECENT_SABQI_REVIEW;
        return null;
    }
}
