package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contradictory real-QCF tests: physical reading order, disjoint Sabqi
 * repetitions, and missing Sunday persisted proof. No user data. */
public final class ContradictoryHifzGuard11714Test {
    private static GeometryRepository geometry;
    private static final LocalDate MON = LocalDate.of(2026, 11, 2);

    @BeforeClass public static void setupGeometry() throws Exception {
        Path path = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(path)) path = Path.of("..").resolve(path);
        geometry = GeometryRepository.fromJson(Files.readString(path));
    }

    private static List<String> physical(int first, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (int i=0; i<count; i++) out.add(geometry.line(first+i).id);
        return out;
    }

    private static String encoded(List<String> lines) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(lines);
    }

    private static InMemoryPrefs withSabqiWeek(String... encodedBlocks) {
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        disk.disk.put("learningSnowballWeekAnchor", MON.toString());
        JSONArray units = new JSONArray();
        for (String block:encodedBlocks) units.put(block);
        disk.disk.put("learningSnowballUnitIds", units.toString());
        return disk;
    }

    private static ConsolidationCycleEngine.Session prepared(
            HifzPrefs prefs, ConsolidationCycleEngine.Family family,
            String cycleId, List<ConsolidationCycleEngine.Unit> units,
            boolean persist) {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(cycleId, family, units.get(0));
        for (int i=1; i<units.size(); i++) cycle=engine.addUnit(cycle,units.get(i));
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,"adversarial-"+cycleId);
        if (persist) assertTrue(prefs.persistConsolidationSession(session));
        while (!session.readyToClose()) {
            session = engine.recordRepetition(session);
            if (persist) assertTrue(prefs.persistConsolidationSession(session));
        }
        return session;
    }

    @Test public void unorderedLearningDatesArePresentedInCanonicalQcfOrderAndGapsStaySeparate() {
        int start = geometry.firstLineIndex(new VerseRef(2,75));
        String early=encoded(physical(start,5));
        String next=encoded(physical(start+5,5));
        String distant=encoded(physical(start+20,5));
        HifzPrefs disjoint=new HifzPrefs(withSabqiWeek(distant,early,next),geometry);
        List<ConsolidationCycleEngine.Unit> units=disjoint.learningConsolidationUnits(MON.plusDays(4));
        assertEquals("No invented full Mushaf pass across an unlearned gap",3,units.size());
        assertEquals(List.of(early,next,distant),
                List.of(units.get(0).id(),units.get(1).id(),units.get(2).id()));
        for (ConsolidationCycleEngine.Unit unit:units)
            assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL,unit.protocol());

        HifzPrefs contiguous=new HifzPrefs(withSabqiWeek(next,early),geometry);
        units=contiguous.learningConsolidationUnits(MON.plusDays(2));
        assertEquals("A genuinely adjacent cumulative pass stays available",3,units.size());
        assertEquals(early,units.get(0).id());
        assertEquals(next,units.get(1).id());
        assertEquals(ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(
                List.of(early,next)),units.get(2).id());
        assertTrue(ConsolidationPhysicalUnitPolicy.physicallyContinuous(
                List.of(early,next),geometry));
    }

    @Test public void disjointEveningCompletesItsIndependentTenRepetitionReceiptsWithoutFakeWhole() {
        int start = geometry.firstLineIndex(new VerseRef(2,75));
        List<String> early=physical(start,5);
        List<String> later=physical(start+20,5);
        String earlyId=encoded(early), laterId=encoded(later);
        InMemoryPrefs disk=withSabqiWeek(laterId,earlyId);
        LocalDate day=MON.plusDays(4);
        disk.disk.put("v6DailyLessonReceiptsV1",new JSONArray().put(new JSONObject()
                .put("family","LEARNING").put("date",day.toString())
                .put("lines",new JSONArray(later))).toString());
        disk.disk.put("v6LearnedLineIds",new JSONArray(early).toString());
        // Source sets, not the test's fictitious date, govern what can be receipted.
        HifzPrefs prefs=new HifzPrefs(disk,geometry);
        List<ConsolidationCycleEngine.Unit> units=prefs.learningConsolidationUnits(day);
        assertEquals(2,units.size());
        ConsolidationCycleEngine.Session done=prepared(prefs,
                ConsolidationCycleEngine.Family.LEARNING,"learning-"+day,units,true);
        assertTrue("Both independently repeated blocks must earn their exact receipt",
                prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING,
                        day.toString(),done));
        JSONArray paid=new JSONArray(disk.getString("v6LearningEarnedEveningUnitsV1","[]"));
        assertEquals(2,paid.length());
        assertEquals(earlyId,paid.getString(0));
        assertEquals(laterId,paid.getString(1));
        assertTrue("Evening never grants Sunday Acquis",prefs.progressionSnapshotV6().acquired.isEmpty());
    }

    @Test public void readyButNotPersistedSundayFinalNeverPromotesEitherFamily() {
        int start=geometry.firstLineIndex(new VerseRef(2,75));
        List<String> lines=physical(start,5);
        String block=encoded(lines);
        LocalDate sunday=MON.plusDays(6);
        for(ConsolidationCycleEngine.Family family:ConsolidationCycleEngine.Family.values()) {
            InMemoryPrefs disk=PartialFinalEightWeekStressTest.empty();
            boolean learning=family==ConsolidationCycleEngine.Family.LEARNING;
            disk.disk.put(learning?"v6LearnedLineIds":"v6StabilizedLineIds",
                    new JSONArray(lines).toString());
            disk.disk.put(learning?"v6LearningEarnedEveningUnitsV1":
                    "v6StabilizationEarnedEveningUnitsV1",new JSONArray().put(block).toString());
            disk.disk.put(learning?"learningSnowball8WeekHistory":
                    "stabilizationSnowball8WeekHistory",new JSONArray().put(new JSONObject()
                    .put("week",MON.toString())
                    .put("units",new JSONArray().put(block))).toString());
            HifzPrefs prefs=new HifzPrefs(disk,geometry);
            List<ConsolidationCycleEngine.Unit> due=learning
                    ?prefs.learningSnowballFinalUnits(sunday,geometry)
                    :prefs.stabilizationSnowballFinalUnits(sunday,geometry);
            assertFalse(due.isEmpty());
            String id=(learning?"learning-final-":"stabilization-final-")+sunday;
            ConsolidationCycleEngine.Session inMemoryReady=prepared(prefs,family,id,due,false);
            Map<String,Object> before=new LinkedHashMap<>(disk.disk);
            assertThrows("No saved OPEN finale = no earned Acquis",
                    IllegalStateException.class,()-> {
                        if (learning) prefs.completeLearningConsolidationSessionV6(
                                inMemoryReady,geometry,sunday.toString(),"forged");
                        else prefs.completeConsolidationSessionV6(
                                inMemoryReady,geometry,sunday.toString(),"forged");
                    });
            assertEquals(before,disk.disk);
            assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
        }
    }
}
