package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** No progress writes: independent dated attendance/coverage oracles. */
public final class WeeklyBilanTest {
    static final LocalDate MON=LocalDate.of(2026,10,19);
    static CadenceAction program(LocalDate date) {
        DayOfWeek day=date.getDayOfWeek();
        return day==DayOfWeek.MONDAY || day==DayOfWeek.WEDNESDAY
            || day==DayOfWeek.FRIDAY ? CadenceAction.LEARNING : CadenceAction.STABILIZATION;
    }
    static String key(LocalDate date, String mode) { return date+"|"+mode; }

    @Test public void confirmedReceiptsAreNeverFakedForMissedLessonsOrReviews() {
        LocalDate today=MON.plusDays(4); // Friday: 5 lessons, 10 revisions
        Set<String> receipts=new HashSet<>();
        receipts.add(key(MON,HifzSessionActivity.SABQI));
        receipts.add(key(MON,HifzSessionActivity.MURAJAAH_ACTIVE));
        receipts.add(key(MON,HifzSessionActivity.MURAJAAH));
        receipts.add(key(MON.plusDays(1),HifzSessionActivity.ITQAN));
        receipts.add(key(MON.plusDays(1),HifzSessionActivity.MURAJAAH_ACTIVE));
        receipts.add(key(MON.plusDays(2),HifzSessionActivity.SABQI));
        WeeklyBilan.Summary result=WeeklyBilan.calculate(today,MON,MON,
            WeeklyBilanTest::program,(date,mode)->receipts.contains(key(date,mode)));
        assertEquals(15,result.planned);
        assertEquals(6,result.validated);
        assertEquals(1,result.lateLessons); // Thursday; Friday is only due today
        assertEquals(5,result.missedReviews); // Tue passive, Wed/Thu both
        assertEquals(3,result.dueToday); // Friday lesson + two revisions
        assertEquals(0,result.unverifiedDays);
        assertEquals(6,receipts.size()); // No side-effect from reporting
    }

    @Test public void unknownPreCoverageDaysAreNotCalledMissing() {
        WeeklyBilan.Summary result=WeeklyBilan.calculate(MON.plusDays(4),MON,
            MON.plusDays(2),WeeklyBilanTest::program,(date,mode)->false);
        assertEquals(9,result.planned);
        assertEquals(0,result.validated);
        assertEquals(2,result.unverifiedDays);
        assertEquals(2,result.lateLessons); // Wednesday/Thursday
        assertEquals(4,result.missedReviews);
        assertEquals(3,result.dueToday);
    }

    @Test public void sundayNeverInventsActiveOrPassiveReview() {
        LocalDate sunday=MON.plusDays(6);
        WeeklyBilan.Summary result=WeeklyBilan.calculate(sunday,MON,MON,
            WeeklyBilanTest::program,(date,mode)->false);
        assertEquals(19,result.planned); // 6 x (lesson + 2 reviews) + Sunday Itqan
        assertEquals(0,result.validated);
        assertEquals(6,result.lateLessons);
        assertEquals(12,result.missedReviews);
        assertEquals(1,result.dueToday);
    }

    @Test public void noHistoricalCountsBeforeProgramBegins() {
        WeeklyBilan.Summary result=WeeklyBilan.calculate(MON.plusDays(1),
            MON.plusDays(1),MON,WeeklyBilanTest::program,(date,mode)->false);
        assertEquals(3,result.planned);
        assertEquals(0,result.lateLessons);
        assertEquals(3,result.dueToday);
    }
}
