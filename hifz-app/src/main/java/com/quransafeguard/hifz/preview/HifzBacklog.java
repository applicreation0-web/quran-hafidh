package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only, receipt-backed chronological backlog. No change to acquired lines,
 * per-mode cursors, calendar policy or repetition counters.
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
            date -> WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
                prefs.weeklyProgramFor(date),learningDays));
    }

    static int lateCount(List<Due> pending,LocalDate today) {
        int count=0;
        for(Due due:pending)if(due.overdue(today))count++;
        return count;
    }

    static Due first(List<Due> pending) {
        return pending.isEmpty()?null:pending.get(0);
    }
}
