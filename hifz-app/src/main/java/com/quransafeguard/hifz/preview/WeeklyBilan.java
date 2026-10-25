package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Read-only weekly evidence. Counts only scheduled, date-verifiable Sabqi/Itqan
 * and daily active/passive revisions; no inference of mastery or Hifz credits.
 * Past reviews not completed are noted, never automatically rescheduled.
 */
final class WeeklyBilan {
    interface Calendar { CadenceAction action(LocalDate day); }
    interface Attendance { boolean completed(LocalDate day, String mode); }

    static final class Summary {
        final int planned, validated, lateLessons, missedReviews, dueToday, unverifiedDays;
        Summary(int planned, int validated, int lateLessons, int missedReviews,
                int dueToday, int unverifiedDays) {
            this.planned=planned;
            this.validated=validated;
            this.lateLessons=lateLessons;
            this.missedReviews=missedReviews;
            this.dueToday=dueToday;
            this.unverifiedDays=unverifiedDays;
        }
        String display() {
            String result="BILAN · LUNDI À AUJOURD’HUI\n"
                +validated+"/"+planned+" validations contrôlées"
                +" (Apprentissage, Stabilisation, Révisions)"
                +" · "+lateLessons+" leçon(s) à rattraper"
                +" · "+missedReviews+" révision(s) non effectuée(s)"
                +" · "+dueToday+" séance(s) à faire aujourd’hui";
            if(unverifiedDays>0)
                result+="\n"+unverifiedDays+" jour(s) hors historique vérifiable";
            result+="\nRenforcement et Consolidation hors de ce compteur.";
            return result;
        }
    }

    private WeeklyBilan() {}

    static Summary calculate(LocalDate today, LocalDate programStart, LocalDate receiptCoverage,
            Calendar calendar, Attendance attendance) {
        if(today==null || programStart==null || receiptCoverage==null
                || calendar==null || attendance==null)
            throw new IllegalArgumentException("Dates and validated evidence are required");
        int planned=0, validated=0, late=0, missed=0, due=0, unknown=0;
        LocalDate monday=today.minusDays(today.getDayOfWeek().getValue()-1);
        for(LocalDate day=monday;!day.isAfter(today);day=day.plusDays(1)) {
            if(day.isBefore(programStart))continue;
            if(day.isBefore(receiptCoverage)) { unknown++; continue; }
            CadenceAction action=calendar.action(day);
            String lesson=action==CadenceAction.LEARNING ? HifzSessionActivity.SABQI
                :action==CadenceAction.STABILIZATION ? HifzSessionActivity.ITQAN : null;
            if(lesson!=null) {
                planned++;
                if(attendance.completed(day,lesson))validated++;
                else if(day.isBefore(today))late++;
                else due++;
            }
            // General reviews are never scheduled on Sunday, including historical programmes.
            if(day.getDayOfWeek()!=DayOfWeek.SUNDAY) {
                String[] reviews={HifzSessionActivity.MURAJAAH_ACTIVE,HifzSessionActivity.MURAJAAH};
                for(String mode:reviews) {
                    planned++;
                    if(attendance.completed(day,mode))validated++;
                    else if(day.isBefore(today))missed++;
                    else due++;
                }
            }
        }
        return new Summary(planned,validated,late,missed,due,unknown);
    }
}
