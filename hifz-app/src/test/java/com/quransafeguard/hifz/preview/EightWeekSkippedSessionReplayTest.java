package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Eight-week dated replay: synthetic, anonymized session events modelled on
 * the schema-6 seven-day cadence. The private real backup is NOT a test fixture.
 * Uses production backlog, ledger balance, final QCF receipt and queue policies.
 */
public final class EightWeekSkippedSessionReplayTest {
    private static final LocalDate START=LocalDate.of(2026,10,26);

    private CadenceAction action(LocalDate date) {
        return WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
            HifzPrefs.WeeklyProgram.RECOMMENDED,3);
    }

    private String mode(LocalDate day) {
        return action(day)==CadenceAction.LEARNING
            ?HifzSessionActivity.SABQI:HifzSessionActivity.ITQAN;
    }

    private static String key(LocalDate day,String mode) {return day+"|"+mode;}

    private Set<String> completedExcept(Set<LocalDate> missing) {
        Set<String> out=new HashSet<>();
        for(int day=0;day<56;day++){
            LocalDate date=START.plusDays(day);
            if(date.getDayOfWeek().getValue()==7 || missing.contains(date))continue;
            out.add(key(date,mode(date)));
        }
        return out;
    }

    private List<HifzBacklog.Due> pending(Set<String> receipts,LocalDate through){
        Set<LocalDate> sabqi=new HashSet<>(),itqan=new HashSet<>();
        for(String receipt:receipts){
            String[] split=receipt.split("\\|",2);
            if(split.length!=2)continue;
            if(HifzSessionActivity.SABQI.equals(split[1]))sabqi.add(LocalDate.parse(split[0]));
            if(HifzSessionActivity.ITQAN.equals(split[1]))itqan.add(LocalDate.parse(split[0]));
        }
        return HifzBacklog.calculate(START,START,through,sabqi,itqan,this::action);
    }

    @Test public void eightFullWeeksHaveFortyEightOrdinaryLessonsAndNoSundayDebt(){
        Set<String> completed=completedExcept(Collections.emptySet());
        assertEquals(48,completed.size());
        assertTrue(pending(completed,START.plusDays(55)).isEmpty());
        for(int week=0;week<8;week++){
            LocalDate sunday=START.plusWeeks(week).plusDays(6);
            assertEquals(CadenceAction.REVISION,action(sunday));
            assertTrue(pending(completed,sunday).isEmpty());
        }
    }

    @Test public void threeOmittedLessonsRemainExactDatedDebtsOverFourAndEightWeeks(){
        Set<LocalDate> absent=new HashSet<>(Arrays.asList(
            LocalDate.parse("2026-11-03"),LocalDate.parse("2026-11-06"),
            LocalDate.parse("2026-11-21")));
        Set<String> completed=completedExcept(absent);
        List<HifzBacklog.Due> w4=pending(completed,START.plusDays(27));
        List<HifzBacklog.Due> w8=pending(completed,START.plusDays(55));
        assertEquals(3,w4.size());assertEquals(3,w8.size());
        assertEquals(LocalDate.parse("2026-11-03"),HifzBacklog.first(w8).date);
        assertEquals("ITQAN",w8.get(0).mode);
        assertEquals("SABQI",w8.get(1).mode);
        assertEquals("ITQAN",w8.get(2).mode);
        completed.add(key(w8.get(0).date,w8.get(0).mode)); // real dated makeup receipt
        assertEquals(2,pending(completed,START.plusDays(55)).size());
        assertEquals(LocalDate.parse("2026-11-06"),
            HifzBacklog.first(pending(completed,START.plusDays(55))).date);
        assertEquals(47,completed.size()); // no fabricated catchup for other lessons
    }

    @Test public void twoWeeksCompletelyMissedRetainTwelveDatedLessonsAtWeekEight(){
        Set<LocalDate> absent=new HashSet<>();
        for(int day=14;day<28;day++)absent.add(START.plusDays(day));
        Set<String> completed=completedExcept(absent);
        List<HifzBacklog.Due> w4=pending(completed,START.plusDays(27));
        List<HifzBacklog.Due> w8=pending(completed,START.plusDays(55));
        assertEquals(12,w4.size());assertEquals(12,w8.size());
        assertEquals(LocalDate.parse("2026-11-09"),w8.get(0).date);
        assertEquals(LocalDate.parse("2026-11-21"),w8.get(11).date);
        assertEquals(36,completed.size());
    }

    @Test public void skippedMorningReviewsNeverBecomeAcquiredLessonCreditsOrSabqiDebt(){
        Set<String> completed=completedExcept(Collections.emptySet());
        Set<String> attendance=new HashSet<>(completed);
        for(int d=0;d<56;d++){
            LocalDate date=START.plusDays(d);
            if(date.getDayOfWeek().getValue()==7)continue;
            if(d%3!=0){
                attendance.add(key(date,HifzSessionActivity.MURAJAAH_ACTIVE));
                attendance.add(key(date,HifzSessionActivity.MURAJAAH));
            }
        }
        assertTrue(pending(completed,START.plusDays(55)).isEmpty());
        LocalDate sunday=START.plusDays(6);
        WeeklyBilan.Summary week1=WeeklyBilan.calculate(sunday,START,START,
            this::action,(d,m)->attendance.contains(key(d,m)));
        assertEquals(18,week1.planned); // 6 ordinary lessons + 12 review receipts
        assertEquals(0,week1.lateLessons);
        assertEquals(4,week1.missedReviews); // Monday and Thursday skipped both
        assertEquals(0,week1.dueToday);
    }

    @Test public void omittedEveningReceiptCannotGraduatePhysicalLinesAtAnyLaterSunday(){
        List<String> page=Arrays.asList("p523-l1","p523-l2","p523-l3");
        String onlyTwo=ConsolidationPhysicalUnitPolicy.encodeLineUnit(page.subList(0,2));
        String full=ConsolidationPhysicalUnitPolicy.encodeLineUnit(page);
        assertFalse(SundayFinalPagePolicy.earnedEveryLine(page,
            Collections.singletonList(onlyTwo),Collections.emptySet()));
        assertTrue(SundayFinalPagePolicy.earnedEveryLine(page,
            Collections.singletonList(full),Collections.emptySet()));
        assertTrue(SundayFinalPagePolicy.earnedEveryLine(page,
            Collections.singletonList(onlyTwo),Collections.singleton(page.get(2))));
    }

    @Test public void fourSuccessiveSundayReviewsStayOrderedAfterProcessRestarts(){
        InMemoryPrefs disk=new InMemoryPrefs();
        PostFinalRecallStore queue=new PostFinalRecallStore(disk);
        List<PostFinalCumulativePolicy.Page> physical=Arrays.asList(
            new PostFinalCumulativePolicy.Page(19,Arrays.asList("p19-l1"),false),
            new PostFinalCumulativePolicy.Page(20,Arrays.asList("p20-l1"),true));
        for(int w=0;w<4;w++){
            String sunday=START.plusWeeks(w).plusDays(6).toString();
            for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
                queue.prepare(family,sunday,physical);
                assertNull(queue.nextReady("","")); // no final receipt
                assertTrue(queue.markReady(family,sunday));
            }
        }
        for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
            queue=new PostFinalRecallStore(disk);
            String latest="2026-11-22";
            PostFinalRecallStore.Recap recap=queue.recapFor(family,latest,latest);
            assertEquals("2026-11-01",recap.date);
            assertEquals(recap.date,queue.nextReadyFor(family,latest,latest).date);
            for(int step=0;step<2;step++){
                PostFinalRecallStore.Review page=queue.nextReadyFor(family,latest,latest);
                assertTrue(queue.advance(page,latest,latest));
                assertFalse(queue.advance(page,latest,latest));
                queue=new PostFinalRecallStore(disk);
            }
            assertEquals("2026-11-08",queue.recapFor(family,latest,latest).date);
        }
        assertFalse(disk.disk.containsKey("v6AcquiredCreditLineIds"));
    }

    @Test public void unfinishedFrozenSundayRetainsItsOwnThreePassProtocol(){
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("learning-final-2026-11-01",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("page20",ConsolidationCycleEngine.Protocol.SUNDAY_PAGE));
        ConsolidationCycleEngine.Session open=engine.openSession(cycle,"persisted");
        assertFalse(open.readyToClose());
        open=engine.recordRepetition(open);
        assertEquals(1,open.donePerStage());
        ConsolidationCycleEngine.Session resumed=engine.restoreOpenSession(cycle,"persisted",
            1,open.stage(),open.nextUnitIndex(),open.donePerStage());
        assertFalse(resumed.readyToClose());
        resumed=engine.recordRepetition(resumed);
        assertFalse(resumed.readyToClose());
        resumed=engine.recordRepetition(resumed);
        assertTrue(resumed.readyToClose());
    }
}
