package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

/** Dated absences remain visible but do not block the next own-family slot. */
public final class HifzBacklogTest {
    private static CadenceAction recommended(LocalDate date) {
        DayOfWeek d=date.getDayOfWeek();
        if(d==DayOfWeek.SUNDAY)return CadenceAction.REVISION;
        return d==DayOfWeek.MONDAY||d==DayOfWeek.WEDNESDAY||d==DayOfWeek.FRIDAY
            ? CadenceAction.LEARNING:CadenceAction.STABILIZATION;
    }
    @Test public void oldestMissingSabqiAndItqanStayChronological() {
        LocalDate start=LocalDate.parse("2026-10-19"),now=start.plusDays(3);
        List<HifzBacklog.Due> all=HifzBacklog.calculate(start,start,now,
            Collections.emptySet(),Collections.emptySet(),HifzBacklogTest::recommended);
        assertEquals(4,all.size());
        assertEquals("SABQI",all.get(0).mode);
        assertEquals("ITQAN",all.get(1).mode);
        assertEquals("SABQI",all.get(2).mode);
        assertEquals(3,HifzBacklog.lateCount(all,now));
        assertEquals(start,HifzBacklog.first(all).date);
    }
    @Test public void completionReceiptsRemoveOnlyTheirOwnDateAndMode() {
        LocalDate start=LocalDate.parse("2026-10-19"),now=start.plusDays(2);
        List<HifzBacklog.Due> debt=HifzBacklog.calculate(start,start,now,
            new HashSet<>(Arrays.asList(start)),Collections.emptySet(),
            HifzBacklogTest::recommended);
        assertEquals(2,debt.size());
        assertEquals("ITQAN",debt.get(0).mode);
        assertEquals(start.plusDays(1),debt.get(0).date);
        assertEquals("SABQI",debt.get(1).mode);
    }
    @Test public void unknownHistoricalAttendanceIsNotInventedAsDebt() {
        LocalDate start=LocalDate.parse("2026-10-19"),coverage=start.plusDays(2);
        List<HifzBacklog.Due> debt=HifzBacklog.calculate(start,coverage,coverage,
            Collections.emptySet(),Collections.emptySet(),HifzBacklogTest::recommended);
        assertEquals(1,debt.size());
        assertEquals(coverage,debt.get(0).date);
    }
    @Test public void missingTuesdayItqanDoesNotStealWednesdaySabqiOrSaturdayItqan() {
        LocalDate mon=LocalDate.of(2026,11,2),tue=mon.plusDays(1),
            wed=mon.plusDays(2),thu=mon.plusDays(3),sat=mon.plusDays(5),
            nextTue=mon.plusDays(8);
        // Monday Sabqi was earned; Tuesday Itqan was not. Nothing is back-credited.
         // A new eligible day keeps its own DATE while the physical cursor
        // (in HifzPrefs) remains on the missed family's next unread lines.
        List<HifzBacklog.Due> thursday=HifzBacklog.calculate(mon,mon,thu,
            new HashSet<>(Arrays.asList(mon)),Collections.emptySet(),
            HifzBacklogTest::recommended);
        assertEquals(tue,HifzBacklog.first(thursday).date); // historical absence
        assertEquals(wed,HifzBacklog.onDate(thursday,wed).date);
        assertEquals("SABQI",HifzBacklog.onDate(thursday,wed).mode);
        assertEquals(thu,HifzBacklog.onDate(thursday,thu).date);
        assertEquals("ITQAN",HifzBacklog.onDate(thursday,thu).mode);
        List<HifzBacklog.Due> saturday=HifzBacklog.calculate(mon,mon,sat,
            new HashSet<>(Arrays.asList(mon,wed)),
            new HashSet<>(Arrays.asList(thu)),HifzBacklogTest::recommended);
        assertEquals(sat,HifzBacklog.onDate(saturday,sat).date);
        assertEquals("ITQAN",HifzBacklog.onDate(saturday,sat).mode);
        assertEquals(tue,HifzBacklog.first(saturday).date);
        List<HifzBacklog.Due> later=HifzBacklog.calculate(mon,mon,nextTue,
            new HashSet<>(Arrays.asList(mon,wed,mon.plusDays(4),mon.plusDays(7))),
            new HashSet<>(Arrays.asList(thu,sat)),HifzBacklogTest::recommended);
        assertEquals(nextTue,HifzBacklog.onDate(later,nextTue).date);
        assertEquals("ITQAN",HifzBacklog.onDate(later,nextTue).mode);
        assertEquals(tue,HifzBacklog.first(later).date);
    }

    @Test public void missingFridaySabqiWaitsUntilNextMondayWithoutMovingItqan() {
        LocalDate mon=LocalDate.of(2026,11,2),fri=mon.plusDays(4),
            sat=mon.plusDays(5),sun=mon.plusDays(6),nextMon=mon.plusDays(7);
        List<HifzBacklog.Due> pending=HifzBacklog.calculate(mon,mon,nextMon,
            new HashSet<>(Arrays.asList(mon,mon.plusDays(2))),
            new HashSet<>(Arrays.asList(mon.plusDays(1),mon.plusDays(3),sat)),
            HifzBacklogTest::recommended);
        assertEquals(fri,HifzBacklog.first(pending).date);
        assertNull("Sunday reserved for finals; no make-up lesson",
            HifzBacklog.onDate(pending,sun));
        assertEquals(nextMon,HifzBacklog.onDate(pending,nextMon).date);
        assertEquals("SABQI",HifzBacklog.onDate(pending,nextMon).mode);
        assertNull("Saturday Itqan is already genuinely completed",
            HifzBacklog.onDate(pending,sat));
        assertEquals(1,HifzBacklog.lateCount(pending,nextMon));
    }

    @Test public void ordinaryRevisionDatesDoNotAccumulateLessonDebt() {
        LocalDate start=LocalDate.parse("2026-10-19");
        List<HifzBacklog.Due> debt=HifzBacklog.calculate(start,start,start.plusDays(1),
            Collections.emptySet(),Collections.emptySet(),
            date->date.equals(start)?CadenceAction.REVISION:CadenceAction.LEARNING);
        assertEquals(1,debt.size());
        assertEquals(start.plusDays(1),debt.get(0).date);
    }
}
