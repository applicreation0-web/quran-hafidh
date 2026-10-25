package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.ScheduledCadence;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

/**
 * M8R (2026-10-14): NEW narrow regression, not a replay of the missing
 * historical M8 80-case harness. Uses real HifzPrefs V6 transitions, QCF line
 * identities, dated WeeklyProgramPolicy and DashboardLedger receipt/expiry
 * functions. Does NOT claim to execute 548 days of Android UI session taps.
 */
public final class M8TargetedLongitudinalRegressionTest {
    private static final LocalDate START=LocalDate.of(2026,10,5);
    private static final int[] BOUNDARIES={119,120,121,122,179,180,364,365,547};

    private static String modeFor(CadenceAction action) {
        if(action==CadenceAction.LEARNING)return HifzSessionActivity.SABQI;
        if(action==CadenceAction.STABILIZATION)return HifzSessionActivity.ITQAN;
        return "M8R_HISTORY_REVISION";
    }

    /** Independent minimal date oracle: does NOT call WeeklyProgramPolicy. */
    private static LocalDate firstUnfinished(LocalDate current,Set<LocalDate> done) {
        for(LocalDate d=START;!d.isAfter(current);d=d.plusDays(1))
            if(!done.contains(d))return d;
        return null;
    }

    private static GeometryRepository geometry() throws Exception {
        java.nio.file.Path p=java.nio.file.Paths.get(
            "app/src/main/assets/reader109/geometry.json");
        if(!java.nio.file.Files.exists(p))
            p=java.nio.file.Paths.get("../app/src/main/assets/reader109/geometry.json");
        return GeometryRepository.fromJson(new String(java.nio.file.Files.readAllBytes(p),
            java.nio.charset.StandardCharsets.UTF_8));
    }

    private static List<String> physicalFive(GeometryRepository geo,int start) {
        List<String> ids=new ArrayList<>();
        for(int i=0;i<5;i++)ids.add(geo.line(start+i).id);
        assertEquals(5,new HashSet<>(ids).size());
        return ids;
    }

    private static void assertNoDuplicateCredits(HifzPrefs prefs,Set<String> expectedAcquired,
                                                  String label) {
        HifzPrefs.ProgressionSnapshot state=prefs.progressionSnapshotV6();
        assertEquals(label+" acquired",expectedAcquired,state.acquired);
        assertTrue(label+" duplicate learned/acquired",
            Collections.disjoint(state.learned,state.acquired));
        assertTrue(label+" duplicate stabilized/acquired",
            Collections.disjoint(state.stabilized,state.acquired));
        assertTrue(label+" duplicate learned/stabilized",
            Collections.disjoint(state.learned,state.stabilized));
    }

    private static void runHorizon(int horizon) throws Exception {
        InMemoryPrefs disk=LongHorizonHifzPersistenceStressTest.freshV6Disk();
        JSONArray history=new JSONArray()
            .put(new JSONObject().put("from",START.toString()).put("mode","RECOMMENDED"))
            .put(new JSONObject().put("from",START.plusDays(horizon/2).toString())
                .put("mode","MANUAL_SEVEN"))
            .put(new JSONObject().put("from",START.plusDays(horizon*3/4).toString())
                .put("mode","RECOMMENDED"));
        disk.disk.put("weeklyProgramTransitionsV1",history.toString());
        HifzPrefs prefs=new HifzPrefs(disk);
        GeometryRepository geo=geometry();
        int physicalCursor=geo.firstLineIndex(new VerseRef(49,1));
        List<String> weekUnit=null;
        LinkedHashSet<String> acquiredOracle=new LinkedHashSet<>();
        HashSet<LocalDate> done=new HashSet<>();
        HashSet<String> receiptOracle=new HashSet<>();
        LinkedHashSet<String> receipts=new LinkedHashSet<>();
        List<DashboardLedger.Record> visible=new ArrayList<>();
        int missed=0,restored=0,sundays=0,checks=0;
        boolean pastNas=false;

        for(int i=0;i<horizon;i++) {
            LocalDate date=START.plusDays(i);
            DayOfWeek weekday=date.getDayOfWeek();
            prefs=new HifzPrefs(disk); // real persistent preferences reopened
            HifzPrefs.WeeklyProgram programme=prefs.weeklyProgramFor(date);
            CadenceAction action=WeeklyProgramPolicy.actionFor(weekday,programme,3);
            if(weekday==DayOfWeek.SUNDAY){
                sundays++;
                assertEquals("Sunday is a dedicated finale day, not an ordinary Hifz lesson",
                    CadenceAction.REVISION,action);
            }
            LocalDate expected=firstUnfinished(date,done);
            ScheduledCadence offered=WeeklyProgramPolicy.nextDue(prefs,START,date,done,3);
            assertEquals("Oldest pending day "+horizon+"/"+i,
                expected,offered==null?null:offered.getScheduledDate());
            if(expected!=null)
                assertEquals("Overdue mismatch "+i,expected.isBefore(date),offered.getOverdue());

            // At most one overdue date is caught up first; the new date is
            // skipped deliberately at selected boundaries to create a debt.
            if(expected!=null && expected.isBefore(date)) {
                receipts=completeDate(prefs,expected,date,done,receipts,receiptOracle,visible);
                visible=DashboardLedger.mergeRecords(visible,expected,
                    modeFor(WeeklyProgramPolicy.actionFor(expected.getDayOfWeek(),
                        prefs.weeklyProgramFor(expected),3)),"caught-up",date);
                restored++;
            }
            boolean skip=i==18 || i==119 || i==120 || i==horizon-3;
            if(skip)missed++;
            else{
                receipts=completeDate(prefs,date,date,done,receipts,receiptOracle,visible);
                visible=DashboardLedger.mergeRecords(visible,date,modeFor(action),"validated",date);
            }
            LocalDate remaining=firstUnfinished(date,done);
            ScheduledCadence stillDue=WeeklyProgramPolicy.nextDue(prefs,START,date,done,3);
            assertEquals("After validation "+horizon+"/"+i,remaining,
                stillDue==null?null:stillDue.getScheduledDate());

            // Independent bounded display vs immutable earned receipts.
            assertTrue("120-day display retention only",visible.size()<=122);
            if(i>=121)assertTrue("Day zero receipt resurrected/lost after display expiry",
                DashboardLedger.containsCompletion(receipts,START,
                    modeFor(WeeklyProgramPolicy.actionFor(DayOfWeek.MONDAY,
                        HifzPrefs.WeeklyProgram.RECOMMENDED,3))));
            assertEquals("No fabricated/double attendance "+i,done.size(),receipts.size());

            // Three real schema6 stages per seven-day week, with a QCF line
            // unit. The stage transitions are genuine, not fake in-memory sets.
            if(weekday==DayOfWeek.MONDAY){
                weekUnit=physicalFive(geo,physicalCursor);
                physicalCursor+=5;
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
                prefs=new HifzPrefs(disk);
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
            } else if(weekday==DayOfWeek.TUESDAY && weekUnit!=null) {
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
                prefs=new HifzPrefs(disk);
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
            } else if(weekday==DayOfWeek.SUNDAY && weekUnit!=null) {
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
                prefs=new HifzPrefs(disk);
                prefs.transitionV6Lines(weekUnit,HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
                acquiredOracle.addAll(weekUnit);
                weekUnit=null;
            }

            // Deliberately place the existing rotation close to An-Nas: checks
            // the real one-way post-Nas persistence, not every physical unit.
            if(i==170) {
                assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
                    ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,new VerseRef(113,1),false)));
            }
            if(i==171){
                assertTrue(prefs.advanceItqanRotationPast(new VerseRef(114,6)));
                pastNas=true;
            }
            if(pastNas)assertTrue("An-Nas latch lost after restart "+i,
                new HifzPrefs(disk).itqanPostNasMaintenance());

            for(int checkpoint:BOUNDARIES)
                if(i==checkpoint){
                    assertNoDuplicateCredits(new HifzPrefs(disk),acquiredOracle,
                        "M8R "+horizon+" day "+i);
                    System.out.println("M8R_CHECKPOINT horizon="+horizon+" day="+i
                        +" acquired="+acquiredOracle.size()
                        +" receipts="+receipts.size()
                        +" overdue="+(remaining==null?"none":remaining)
                        +" postNas="+pastNas);
                    checks++;
                }
        }
        LocalDate end=START.plusDays(horizon-1);
        assertNull("Final scheduled debt must be paid (horizon "+horizon+")",
            WeeklyProgramPolicy.nextDue(new HifzPrefs(disk),START,end,done,3));
        assertEquals(horizon,done.size());
        assertEquals(horizon,receiptOracle.size());
        assertEquals(horizon,receipts.size());
        assertTrue("post-Nas must remain latched",new HifzPrefs(disk).itqanPostNasMaintenance());
        assertNoDuplicateCredits(new HifzPrefs(disk),acquiredOracle,"M8R final "+horizon);
        System.out.println("M8R_PASS horizon="+horizon+" days="+horizon
            +" receipts="+receipts.size()+" acquired="+acquiredOracle.size()
            +" missed="+missed+" caught_up="+restored+" sundays="+sundays
            +" checkpoints="+checks+" programme_switches=2");
    }

    private static LinkedHashSet<String> completeDate(HifzPrefs prefs,LocalDate date,
            LocalDate today,Set<LocalDate> done,LinkedHashSet<String> receipts,
            Set<String> oracle,List<DashboardLedger.Record> display) {
        CadenceAction action=WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
            prefs.weeklyProgramFor(date),3);
        String mode=modeFor(action);
        String key=date+"|"+mode;
        assertTrue("A due date cannot be earned twice "+date,oracle.add(key));
        done.add(date);
        LinkedHashSet<String> after=DashboardLedger.mergeCompletionKeys(
            receipts,display,date,mode);
        LinkedHashSet<String> duplicate=DashboardLedger.mergeCompletionKeys(
            after,display,date,mode);
        assertEquals("Double tap must never credit again",after,duplicate);
        assertTrue(DashboardLedger.containsCompletion(after,date,mode));
        return after;
    }

    @Test public void datedHifz180DaysWithPurgeAndCatchup() throws Exception {runHorizon(180);}
    @Test public void datedHifz365DaysWithProgrammeTransitions() throws Exception {runHorizon(365);}
    @Test public void datedHifz548DaysWithPostNasAndDurableAttendance() throws Exception {runHorizon(548);}
}
