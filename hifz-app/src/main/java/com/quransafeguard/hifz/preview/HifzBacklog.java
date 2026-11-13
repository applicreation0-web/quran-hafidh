package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only ledger of calendar slots that were not attended. These historical
 * absences are NOT a queue of mandatory make-up sessions: the next real Sabqi
 * or Itqan lesson uses the next unconsumed physical cursor on the next eligible
 * day of ITS OWN family. The actual attendance receipt belongs to that day.
 * No synthetic lines, credits, date changes or repeated work are created.
 */
final class HifzBacklog {
    interface ActionForDate { CadenceAction at(LocalDate day); }
    static final class Due {
        final LocalDate date;
        final String mode;
        Due(LocalDate date, String mode) { this.date=date; this.mode=mode; }
        boolean overdue(LocalDate today) { return date.isBefore(today); }
        String frenchName() {
            return HifzSessionActivity.SABQI.equals(mode) ? "Apprentissage" : "Stabilisation";
        }
    }

    private HifzBacklog() {}

    static List<Due> calculate(LocalDate start, LocalDate coverage, LocalDate today,
                               Set<LocalDate> sabqiDone, Set<LocalDate> itqanDone,
                               ActionForDate schedule) {
        if(start==null || coverage==null || today==null || schedule==null
                || sabqiDone==null || itqanDone==null)
            throw new IllegalArgumentException("Backlog requires dated proven receipts");
        LocalDate first=start.isBefore(coverage) ? coverage : start;
        ArrayList<Due> pending=new ArrayList<>();
        for(LocalDate date=first;!date.isAfter(today);date=date.plusDays(1)) {
            CadenceAction action=schedule.at(date);
            if(action==CadenceAction.LEARNING && !sabqiDone.contains(date))
                pending.add(new Due(date,HifzSessionActivity.SABQI));
            else if(action==CadenceAction.STABILIZATION && !itqanDone.contains(date))
                pending.add(new Due(date,HifzSessionActivity.ITQAN));
            // Ordinary revision/finale receipts are not fabricated as lesson debt.
        }
        return Collections.unmodifiableList(pending);
    }

    static List<Due> pending(HifzPrefs prefs,DashboardLedger ledger,LocalDate today,
                             int learningDays) {
        LocalDate start=prefs.programStartDate();
        LocalDate coverage=ledger.coverageStartDate();
        LocalDate first=start.isBefore(coverage)?coverage:start;
        if(today.isBefore(first))return Collections.emptyList();
        Set<LocalDate> sabqiDone=new HashSet<>(ledger.completedDates(
            HifzSessionActivity.SABQI,first,today));
        Set<LocalDate> itqanDone=new HashSet<>(ledger.completedDates(
            HifzSessionActivity.ITQAN,first,today));
        return calculate(start,coverage,today,sabqiDone,itqanDone,
            date -> {
                CadenceAction action=WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
                    prefs.weeklyProgramFor(date),learningDays);
                return action==CadenceAction.STABILIZATION && prefs.weeklyItqanPageClosed(date)
                    ? CadenceAction.REVISION : action;
            });
    }

    static int lateCount(List<Due> pending,LocalDate today) {
        int count=0;
        for(Due due:pending)if(due.overdue(today))count++;
        return count;
    }

    static Due first(List<Due> pending) {
        return pending.isEmpty()?null:pending.get(0);
    }

    /**
     * The only lesson permitted to open now. A missed Tuesday Itqan remains
     * historically absent, but Thursday opens today's Itqan with the same next
     * physical lines; it does NOT replay Tuesday's date or block Wednesday Sabqi.
     * Identical symmetric rule for missed Monday/Wednesday/Friday Sabqi.
     */
    static Due onDate(List<Due> pending, LocalDate date) {
        if(pending==null || date==null)return null;
        for(Due entry:pending)if(date.equals(entry.date))return entry;
        return null;
    }
}
