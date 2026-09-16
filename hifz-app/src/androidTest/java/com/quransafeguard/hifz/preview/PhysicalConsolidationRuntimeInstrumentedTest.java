package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Emulator-level contracts for exact physical Stabilisation -> Consolidation runtime identity. */
public final class PhysicalConsolidationRuntimeInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";

    private Context context;
    private SharedPreferences main;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        assertTrue(main.edit().clear().commit());
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        assertTrue(main.edit().clear().commit());
    }

    @Test public void fifteenPhysicalLinesInOneAyahProduceSevenPlusEightAndTwoConsolidationUnits() {
        VerseRef sameAyah = new VerseRef(2, 282);
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            lines.add(new GeometryRepository.LineMeta(i, "single-ayah-line-" + i, 48,
                Collections.singletonList(sameAyah)));
        }

        List<StabilizationHalfPagePolicy.Unit> planned = StabilizationHalfPagePolicy.planPage(lines);
        assertEquals(2, planned.size());
        assertEquals(7, planned.get(0).lineIds.size());
        assertEquals(8, planned.get(1).lineIds.size());

        LinkedHashSet<String> stabilized = new LinkedHashSet<>();
        for (GeometryRepository.LineMeta line : lines) stabilized.add(line.id);
        List<StabilizationHalfPagePolicy.Unit> ready = ConsolidationPhysicalUnitPolicy.readyUnits(
            planned, stabilized, Collections.emptySet(), 3);
        assertEquals(2, ready.size());
        assertEquals(planned.get(0).lineIds, ready.get(0).lineIds);
        assertEquals(planned.get(1).lineIds, ready.get(1).lineIds);
    }

    @Test public void surahBoundaryIsSplitBeforePhysicalSizePolicy() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lines.add(new GeometryRepository.LineMeta(i, "s49-" + i, 520,
                Collections.singletonList(new VerseRef(49, 1))));
        }
        for (int i = 8; i < 15; i++) {
            lines.add(new GeometryRepository.LineMeta(i, "s50-" + i, 520,
                Collections.singletonList(new VerseRef(50, 1))));
        }

        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(lines);
        assertEquals(2, units.size());
        assertEquals(49, units.get(0).surah);
        assertEquals(8, units.get(0).lineIds.size());
        assertEquals(50, units.get(1).surah);
        assertEquals(7, units.get(1).lineIds.size());
    }

    @Test public void completingFirstPhysicalHalfNeverAcquiresSecondHalfOfSamePage() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        List<StabilizationHalfPagePolicy.Unit> pair = firstCanonicalSplitPair();
        StabilizationHalfPagePolicy.Unit first = pair.get(0);
        StabilizationHalfPagePolicy.Unit second = pair.get(1);

        LinkedHashSet<String> all = new LinkedHashSet<>(first.lineIds);
        all.addAll(second.lineIds);
        assertTrue(main.edit()
            .putString("v6LearnedLineIds", "[]")
            .putString("v6StabilizedLineIds", json(all))
            .putString("v6AcquiredCreditLineIds", "[]")
            .commit());

        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Unit unit = new ConsolidationCycleEngine.Unit(
            ConsolidationPhysicalUnitPolicy.encodeLineUnit(first.lineIds),
            ConsolidationCycleEngine.Protocol.LIGHT);
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "physical-first-half", ConsolidationCycleEngine.Family.STABILIZATION, unit);
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "physical-first-half-session");
        while (!session.readyToClose()) session = engine.recordRepetition(session);

        assertTrue(prefs.completeConsolidationSessionV6(session, geometry, "2026-09-16", "physical-half"));
        Set<String> acquired = stringSet("v6AcquiredCreditLineIds");
        Set<String> stabilized = stringSet("v6StabilizedLineIds");
        for (String id : first.lineIds) {
            assertTrue("first half must become Acquired: " + id, acquired.contains(id));
            assertFalse(stabilized.contains(id));
        }
        for (String id : second.lineIds) {
            assertTrue("second half must remain Stabilised: " + id, stabilized.contains(id));
            assertFalse("second half must not be over-credited: " + id, acquired.contains(id));
        }
    }

    @Test public void processRecreationRestoresExactPhysicalLineIdsAndFrozenGroupsOneToThree() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.setSabqiProgress(23, 2));
        assertTrue(prefs.setItqanProgress(19, 4, 3, new VerseRef(49, 1), new VerseRef(49, 5)));
        int[] countersBefore = counters(prefs);

        for (int size = 1; size <= 3; size++) {
            ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
            ConsolidationCycleEngine.Cycle cycle = null;
            ArrayList<List<String>> exact = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                List<String> ids = Arrays.asList(
                    "group-" + size + "-unit-" + i + "|comma,slash/:colon",
                    "unicode-é-مرحبا-" + i);
                exact.add(ids);
                ConsolidationCycleEngine.Unit unit = new ConsolidationCycleEngine.Unit(
                    ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids),
                    i % 2 == 0 ? ConsolidationCycleEngine.Protocol.LIGHT : ConsolidationCycleEngine.Protocol.FULL);
                cycle = i == 0
                    ? engine.startCycle("cycle-" + size, ConsolidationCycleEngine.Family.STABILIZATION, unit)
                    : engine.addUnit(cycle, unit);
            }

            ConsolidationCycleEngine.Session open = engine.openSession(cycle, "session-" + size);
            open = engine.recordRepetition(open);
            assertTrue(prefs.persistConsolidationSession(open));
            assertArrayEquals(countersBefore, counters(prefs));

            HifzPrefs reopened = new HifzPrefs(context);
            ConsolidationCycleEngine.Session restored = reopened.restoreConsolidationSession(
                new ConsolidationCycleEngine(), ConsolidationCycleEngine.Family.STABILIZATION);
            assertEquals(size, restored.sessionGroupSize());
            assertEquals(open.stage(), restored.stage());
            assertEquals(open.nextUnitIndex(), restored.nextUnitIndex());
            assertEquals(open.donePerStage(), restored.donePerStage());
            for (int i = 0; i < size; i++) {
                assertEquals(exact.get(i), ConsolidationPhysicalUnitPolicy.decodeLineUnit(restored.unitIds().get(i)));
            }
            assertArrayEquals(countersBefore, counters(reopened));
            prefs = reopened;
        }
    }

    private List<StabilizationHalfPagePolicy.Unit> firstCanonicalSplitPair() {
        for (int page = 1; page <= 604; page++) {
            ArrayList<GeometryRepository.LineMeta> pageLines = new ArrayList<>();
            for (int i = 0; i < geometry.lineCount(); i++) {
                GeometryRepository.LineMeta line = geometry.line(i);
                if (line.page == page) pageLines.add(line);
            }
            if (pageLines.isEmpty()) continue;
            List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(pageLines);
            for (int i = 0; i + 1 < units.size(); i++) {
                StabilizationHalfPagePolicy.Unit left = units.get(i);
                StabilizationHalfPagePolicy.Unit right = units.get(i + 1);
                if (left.surah == right.surah && left.page == right.page) {
                    return Arrays.asList(left, right);
                }
            }
        }
        fail("canonical Mushaf must contain at least one physical split pair");
        throw new AssertionError();
    }

    private Set<String> stringSet(String key) throws Exception {
        JSONArray array = new JSONArray(main.getString(key, "[]"));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    private static String json(Iterable<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) array.put(value);
        return array.toString();
    }

    private static int[] counters(HifzPrefs prefs) {
        return new int[]{prefs.sabqiRep(), prefs.sabqiAssisted(), prefs.itqanRep(),
            prefs.itqanAssisted(), prefs.itqanFinalReveals()};
    }
}
