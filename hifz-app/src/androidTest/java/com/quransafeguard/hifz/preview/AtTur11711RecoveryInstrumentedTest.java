package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Android/QCF in-place state-transition regression, with synthetic but physically exact
 * At-Tur page 525 evidence. Never load or publish a user's original backup.
 *
 * Tests the real Android SharedPreferences, 604-page QCF GeometryRepository,
 * frozen ×7 evening, page-based Sunday final, pending-queue cleanup, and active corpus.
 */
public final class AtTur11711RecoveryInstrumentedTest {
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 4);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 8);
    private static final List<String> MISSING = Arrays.asList(
        "525:8","525:9","525:10","525:11","525:12","525:13","525:14");
    private static final List<String> ALREADY_ACQUIRED = Arrays.asList(
        "525:0","525:1","525:2","525:3","525:4","525:5","525:6","525:7");
    private static final List<String> NEW_STABILIZED = Arrays.asList(
        "526:0","526:1","526:2","526:3","526:4","526:5","526:6","526:7");
    private static final List<String> OCT_EARNED_1 = Arrays.asList(
        "524:8","524:9","524:10","524:11","524:12","524:13","524:14","525:0","525:1");
    private static final List<String> OCT_EARNED_2 = Arrays.asList(
        "525:2","525:3","525:4","525:5","525:6","525:7");

    private static String enc(List<String> ids) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids);
    }
    private static JSONArray array(List<String> values) {
        JSONArray result=new JSONArray();
        for(String id:values)result.put(id);
        return result;
    }
    private static String ranges(String start,String end) throws Exception {
        return new JSONArray().put(new JSONObject().put("start",start).put("end",end)).toString();
    }
    private static ConsolidationCycleEngine.Session ready(
            ConsolidationCycleEngine.Family family, String cycleId,
            List<ConsolidationCycleEngine.Unit> units, HifzPrefs prefs) {
        assertFalse("Never auto-close an empty cycle",units.isEmpty());
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=null;
        for(ConsolidationCycleEngine.Unit unit:units)
            cycle=cycle==null ? engine.startCycle(cycleId,family,unit) : engine.addUnit(cycle,unit);
        ConsolidationCycleEngine.Session s=engine.openSession(cycle,"android-11711-"+cycleId);
        assertTrue(prefs.persistConsolidationSession(s));
        while(!s.readyToClose()) {
            s=engine.recordRepetition(s);
            assertTrue(prefs.persistConsolidationSession(s));
        }
        return s;
    }

    @Test public void exactAtTurStateRecoversOnlyByRealEveningThenRealSunday() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        // The test's debug package uses a disposable emulator ONLY.
        SharedPreferences disk=c.getSharedPreferences("quran_hifz_preview_v1",0);
        assertTrue(disk.edit().clear().commit());
        try {
            assertTrue("Non-production emulator build must keep its isolated package",
                BuildConfig.APPLICATION_ID.endsWith(".ibnkathirtest"));
            assertTrue("Public emulator build must keep its debug version suffix",
                BuildConfig.VERSION_NAME.endsWith("-IbnKathir-DEBUG"));
            assertTrue("Version code must not regress",BuildConfig.VERSION_CODE>=52);
            GeometryRepository geometry=GeometryRepository.get(c);
            assertEquals(7,geometry.linesForExactIds(MISSING).size());
            assertEquals(8,geometry.linesForExactIds(ALREADY_ACQUIRED).size());
            assertEquals(8,geometry.linesForExactIds(NEW_STABILIZED).size());
            for(GeometryRepository.LineMeta line:geometry.linesForExactIds(MISSING))
                assertEquals(525,line.page);

            JSONArray oldHistory=new JSONArray()
                .put(new JSONObject().put("week","2026-10-26").put("units",
                    array(Arrays.asList(enc(OCT_EARNED_1),enc(OCT_EARNED_2),enc(MISSING)))))
                .put(new JSONObject().put("week","2026-11-02").put("units",
                    array(Arrays.asList(enc(NEW_STABILIZED)))));
            JSONArray receipts=array(Arrays.asList(enc(OCT_EARNED_1),
                enc(OCT_EARNED_2),enc(NEW_STABILIZED)));
            JSONArray queue=new JSONArray().put(new JSONObject()
                .put("start","52:34").put("end","52:49")
                .put("origin","RECONSTRUCTION").put("protocol","LIGHT").put("failures",0));

            SharedPreferences.Editor e=disk.edit();
            e.putInt("schema",6);
            e.putString("v6LearnedLineIds","[]");
            e.putString("v6StabilizedLineIds",array(new ArrayList<String>(){{addAll(MISSING);addAll(NEW_STABILIZED);}}).toString());
            // The earlier 9-line historical receipt was ALREADY Acquis in the
            // real export, including 524:8..14. Never pretend it is uncredited:
            // Sunday must skip this old unit, not try to promote it again.
            LinkedHashSet<String> previouslyAcquired=new LinkedHashSet<>(ALREADY_ACQUIRED);
            previouslyAcquired.addAll(OCT_EARNED_1);
            previouslyAcquired.addAll(OCT_EARNED_2);
            e.putString("v6AcquiredCreditLineIds",
                array(new ArrayList<>(previouslyAcquired)).toString());
            e.putString("v6QuarantineLineIds","[]");
            e.putString("v6LegacyPartialAcquiredLineIds","[]");
            e.putString("itqanRanges",ranges("2:1","2:76"));
            e.putString("legacyMurajaahPromotedRanges","[]");
            e.putString("promotedRanges",ranges("50:1","114:6"));
            e.putString("unconsolidatedPromotedRanges",ranges("52:34","114:6"));
            e.putString("forcedPromotedRanges","[]");
            e.putString("anchoringQueue",queue.toString());
            e.putBoolean("anchoringQueueInitialized",true);
            e.putInt("anchoringQueueIndex",0);
            e.putString("stabilizationSnowball8WeekHistory",oldHistory.toString());
            e.putString("stabilizationSnowballWeekAnchor","2026-11-02");
            e.putString("stabilizationSnowballUnitIds",array(Arrays.asList(enc(NEW_STABILIZED))).toString());
            e.putString("v6StabilizationEarnedEveningUnitsV1",receipts.toString());
            e.putString("lastStabilizationSnowballEveningDate","2026-11-03");
            e.putString("v6LastStabilizationSnowballCompletedSnapshotV1",
                "2026-11-02|"+array(Arrays.asList(enc(NEW_STABILIZED))).toString());
            e.putString("lastRecentSabqiReviewDate","2026-11-01");
            e.putString("lastRecentSabqiReviewLabel","Consolidation · 2 unité(s) · Acquis");
            e.putString("itqanCursor","53:1");
            e.putString("activeMurajaahCursor","2:102");
            assertTrue(e.commit());

            HifzPrefs prefs=new HifzPrefs(disk);
            Map<String,?> before=new HashMap<>(disk.getAll());
            assertTrue(prefs.snowballEveningPending(
                ConsolidationCycleEngine.Family.STABILIZATION,WEDNESDAY));
            List<ConsolidationCycleEngine.Unit> due=prefs.stabilizedConsolidationUnits(WEDNESDAY);
            assertEquals(2,due.size());
            assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS,due.get(1).protocol());
            assertEquals(enc(NEW_STABILIZED),due.get(1).id());
            assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN,due.get(0).protocol());
            assertEquals(enc(MISSING),due.get(0).id());
            assertEquals("Read-only planner must preserve exact byte values",before,disk.getAll());

            // Before ×7, the Sunday finale MUST NOT include unreceipted 525:8..14.
            for(ConsolidationCycleEngine.Unit unit:prefs.stabilizationSnowballFinalUnits(SUNDAY,geometry))
                assertFalse("No inferred missing evening receipt",unit.id().equals(enc(MISSING)));
            Set<String> initialAcquired=new LinkedHashSet<>(prefs.progressionSnapshotV6().acquired);
            assertFalse(initialAcquired.contains("525:8"));

            ConsolidationCycleEngine.Session evening=ready(
                ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-"+WEDNESDAY,due,prefs);
            assertTrue(prefs.completeSnowballEvening(
                ConsolidationCycleEngine.Family.STABILIZATION,WEDNESDAY.toString(),evening));
            assertTrue(new JSONArray(disk.getString("v6StabilizationEarnedEveningUnitsV1","[]"))
                .toString().contains(enc(MISSING)));
            assertEquals("No Acquis from evening alone",initialAcquired,prefs.progressionSnapshotV6().acquired);
            assertEquals("53:1",disk.getString("itqanCursor",""));
            assertEquals("2:102",disk.getString("activeMurajaahCursor",""));

            List<ConsolidationCycleEngine.Unit> finals=prefs.stabilizationSnowballFinalUnits(SUNDAY,geometry);
            assertFalse("Physical QCF final must exist",finals.isEmpty());
            assertTrue("The recovered At-Tur page must now be in the actual Sunday final",
                finals.stream().anyMatch(u->u.id().equals(enc(MISSING))
                    && u.protocol()==ConsolidationCycleEngine.Protocol.SUNDAY_PAGE));
            ConsolidationCycleEngine.Session sunday=ready(
                ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-"+SUNDAY,finals,prefs);
            assertTrue(prefs.completeConsolidationSessionV6(
                sunday,geometry,SUNDAY.toString(),"At-Tur synthetic verified Sunday"));
            HifzPrefs reboot=new HifzPrefs(disk);
            HifzPrefs.ProgressionSnapshot snapshot=reboot.progressionSnapshotV6();
            assertTrue(snapshot.acquired.containsAll(MISSING));
            assertTrue(snapshot.acquired.containsAll(ALREADY_ACQUIRED));
            for(String line:MISSING)assertFalse(snapshot.stabilized.contains(line));
            assertEquals("53:1",disk.getString("itqanCursor",""));
            assertEquals("2:102",disk.getString("activeMurajaahCursor",""));
            assertFalse("At-Tur 52:34 must enter Révision active",
                reboot.activeMurajaahCorpus().getRanges().isEmpty());
            assertTrue("At-Tur first omitted verse must now be active",
                reboot.activeMurajaahCorpus().contains(new VerseRef(52,34)));
            assertTrue("Last verse of At-Tur must be active",
                reboot.activeMurajaahCorpus().contains(new VerseRef(52,49)));
            Map<String,?> after=new HashMap<>(disk.getAll());
            assertFalse("Sunday replay MUST NOT mutate credited state",
                reboot.completeConsolidationSessionV6(
                    sunday,geometry,SUNDAY.toString(),"duplicate final"));
            assertEquals(after,disk.getAll());
        } finally {
            assertTrue(disk.edit().clear().commit());
        }
    }
}
