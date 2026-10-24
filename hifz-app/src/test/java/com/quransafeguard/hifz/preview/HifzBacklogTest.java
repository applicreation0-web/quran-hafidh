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

/** Receipts and source dates determine what is owed: no calendar-based free credit. */
public final class HifzBacklogTest {
    private static CadenceAction recommended(LocalDate date) {
        DayOfWeek d=date.getDayOfWeek();
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
    @Test public void ordinaryRevisionDatesDoNotAccumulateLessonDebt() {
        LocalDate start=LocalDate.parse("2026-10-19");
        List<HifzBacklog.Due> debt=HifzBacklog.calculate(start,start,start.plusDays(1),
            Collections.emptySet(),Collections.emptySet(),
            date->date.equals(start)?CadenceAction.REVISION:CadenceAction.LEARNING);
        assertEquals(1,debt.size());
        assertEquals(start.plusDays(1),debt.get(0).date);
    }
}
