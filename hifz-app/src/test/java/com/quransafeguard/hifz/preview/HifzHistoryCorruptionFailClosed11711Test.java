package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * P0: an interrupted/corrupt legacy backup cannot be interpreted as empty
 * weekly snowball history. Such a fallback silently retired unacquired blocks.
 * All checks are read-only, on synthetic preferences, with no private data.
 */
public final class HifzHistoryCorruptionFailClosed11711Test {
    private static final LocalDate WEDNESDAY=LocalDate.of(2026,11,4);

    private static InMemoryPrefs disk() {
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("schema",6);
        d.disk.put("v6AcquiredCreditLineIds","[]");
        d.disk.put("v6StabilizedLineIds","[]");
        d.disk.put("v6LearnedLineIds","[]");
        d.disk.put("v6QuarantineLineIds","[]");
        d.disk.put("v6LegacyPartialAcquiredLineIds","[]");
        d.disk.put("stabilizationSnowballWeekAnchor","2026-11-02");
        d.disk.put("stabilizationSnowballUnitIds","[]");
        d.disk.put("stabilizationSnowball8WeekHistory","[]");
        return d;
    }

    private static void appendSynthetic(HifzPrefs prefs) throws Exception {
        Method m=HifzPrefs.class.getDeclaredMethod(
            "weeklySnowballAppendUnits",ConsolidationCycleEngine.Family.class,
            List.class,LocalDate.class);
        m.setAccessible(true);
        List<List<String>> realPhysical=Collections.singletonList(
            Arrays.asList("525:8","525:9","525:10","525:11","525:12","525:13","525:14"));
        try {m.invoke(prefs,ConsolidationCycleEngine.Family.STABILIZATION,realPhysical,WEDNESDAY);}
        catch(InvocationTargetException e){
            Throwable cause=e.getCause();
            if(cause instanceof RuntimeException)throw (RuntimeException)cause;
            throw e;
        }
    }

    @Test public void malformedOldHistoryDoesNotBecomeAnEmptyWeekAtNextItqanCommit() throws Exception {
        InMemoryPrefs d=disk();
        d.disk.put("stabilizationSnowball8WeekHistory","[BROKEN AND PRESERVED");
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        try {appendSynthetic(new HifzPrefs(d));fail("Malformed history must fail closed");}
        catch(IllegalStateException expected){
            assertTrue(expected.getMessage().contains("Corrupt Hifz snowball history"));
        }
        assertEquals("Never overwrite historical source or any V6 progress",before,d.disk);
    }

    @Test public void corruptCurrentWeekCannotOverwriteItsOriginalUnits() throws Exception {
        InMemoryPrefs d=disk();
        d.disk.put("stabilizationSnowballUnitIds","{CORRUPTED CURRENT WEEK");
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        try {appendSynthetic(new HifzPrefs(d));fail("Malformed current week must fail closed");}
        catch(IllegalStateException expected){
            assertTrue(expected.getMessage().contains("Corrupt weekly Hifz snowball units"));
        }
        assertEquals(before,d.disk);
    }

    @Test public void malformedSundayHistoryMustNotSilentlyHideUnacquiredFinals() {
        InMemoryPrefs d=disk();
        d.disk.put("stabilizationSnowball8WeekHistory","{{not a valid JSON array}}");
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        try {
            new HifzPrefs(d).stabilizationSnowballFinalUnits(WEDNESDAY.plusDays(4));
            fail("Corrupt pending Sunday history must not appear empty");
        }catch(IllegalStateException expected){
            assertTrue(expected.getMessage().contains("Corrupt Hifz snowball history"));
        }
        assertEquals(before,d.disk);
    }

    @Test public void normalEmptyHistoryStillAllowsFirstVerifiedItqanBlockToBeEnrolled() throws Exception {
        InMemoryPrefs d=disk();
        appendSynthetic(new HifzPrefs(d));
        // The read-only planner must not mutate the original SharedPreferences:
        // real enrollment occurs atomically only inside its enclosing V6 commit.
        assertEquals("[]",d.getString("stabilizationSnowball8WeekHistory",""));
        assertEquals("[]",d.getString("stabilizationSnowballUnitIds",""));
    }
}
