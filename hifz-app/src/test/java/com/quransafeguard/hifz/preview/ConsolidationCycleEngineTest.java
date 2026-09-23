package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class ConsolidationCycleEngineTest {
    @Test public void exactQuotaAndStageVectorsAreFrozenForGroupSize() {
        assertPlan(ConsolidationCycleEngine.Protocol.LEARNING37, 1,
            new int[][]{{15,5,5,5,7}});
        assertPlan(ConsolidationCycleEngine.Protocol.LEARNING37, 2,
            new int[][]{{8,2,2,3,4},{7,2,3,3,3}});
        assertPlan(ConsolidationCycleEngine.Protocol.LEARNING37, 3,
            new int[][]{{5,2,2,2,2},{5,1,2,2,2},{5,1,2,2,2}});

        assertPlan(ConsolidationCycleEngine.Protocol.LIGHT, 1,
            new int[][]{{20,0,5,5,5}});
        assertPlan(ConsolidationCycleEngine.Protocol.LIGHT, 2,
            new int[][]{{10,0,2,3,3},{10,0,2,2,3}});
        assertPlan(ConsolidationCycleEngine.Protocol.LIGHT, 3,
            new int[][]{{7,0,1,2,2},{7,0,1,2,2},{6,0,1,2,2}});

        assertPlan(ConsolidationCycleEngine.Protocol.FULL, 1,
            new int[][]{{15,5,5,5,10}});
        assertPlan(ConsolidationCycleEngine.Protocol.FULL, 2,
            new int[][]{{7,2,3,3,5},{7,2,3,3,5}});
        assertPlan(ConsolidationCycleEngine.Protocol.FULL, 3,
            new int[][]{{5,2,2,2,3},{5,1,2,2,3},{5,1,2,2,3}});
    }

    /** Every weekday-pinned snowball unit gets the same ×10 (evening) or ×5 (Sunday) quota, regardless of group size. */
    @Test public void snowballProtocolsAreUniformAcrossGroupSizeAndPosition() {
        for (int size = 1; size <= 3; size++) {
            for (int position = 0; position < size; position++) {
                assertArrayEquals(new int[]{10, 0, 0, 0, 0},
                    ConsolidationCycleEngine.stageVector(ConsolidationCycleEngine.Protocol.SNOWBALL, size, position));
                assertEquals(10, ConsolidationCycleEngine.quota(ConsolidationCycleEngine.Protocol.SNOWBALL, size, position));
                assertArrayEquals(new int[]{5, 0, 0, 0, 0},
                    ConsolidationCycleEngine.stageVector(ConsolidationCycleEngine.Protocol.SNOWBALL_FINAL, size, position));
                assertEquals(5, ConsolidationCycleEngine.quota(ConsolidationCycleEngine.Protocol.SNOWBALL_FINAL, size, position));
            }
        }
    }

    @Test public void snowballProtocolsAreValidForBothFamilies() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        engine.startCycle("learning-snowball", ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("a", ConsolidationCycleEngine.Protocol.SNOWBALL));
        engine.startCycle("stabilization-snowball", ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("b", ConsolidationCycleEngine.Protocol.SNOWBALL_FINAL));
    }

    /** Mon→Wed→Fri: a session grows from one unit to three, each still needing ×10 fresh reps. */
    @Test public void weeklySnowballGrowsUnitsAcrossEveningsAndEachNeedsFullQuotaAgain() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle("learning-week1",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("mon", ConsolidationCycleEngine.Protocol.SNOWBALL));
        ConsolidationCycleEngine.Session monday = engine.openSession(cycle, "mon-session");
        for (int i = 0; i < 10; i++) monday = engine.recordRepetition(monday);
        assertTrue(monday.readyToClose());
        cycle = engine.closeSession(monday).cycle();

        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("wed", ConsolidationCycleEngine.Protocol.SNOWBALL));
        ConsolidationCycleEngine.Session wednesday = engine.openSession(cycle, "wed-session");
        assertEquals(2, wednesday.sessionGroupSize());
        for (int i = 0; i < 20; i++) wednesday = engine.recordRepetition(wednesday);
        assertTrue(wednesday.readyToClose());
    }

    /**
     * A week can now run up to six Apprentissage (or Stabilisation) days (Settings' configurable
     * split, full range 0..6), plus the evening snowball's extra "continuous" unit on top of the
     * week's own blocks — SNOWBALL/SNOWBALL_FINAL cycles must tolerate up to seven units total,
     * while the legacy protocols (never used with more than three) stay capped at three.
     */
    @Test public void snowballCyclesTolerateSevenUnitsForSixDaysPlusTheContinuousPassButLegacyProtocolsStayCappedAtThree() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle("learning-week2",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("mon", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("tue", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("wed", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("thu", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("fri", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("sat", ConsolidationCycleEngine.Protocol.SNOWBALL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("continuous", ConsolidationCycleEngine.Protocol.SNOWBALL));
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "session-sat");
        assertEquals(7, session.sessionGroupSize());
        assertEquals(10, session.quotaAt(6));

        try {
            engine.addUnit(engine.closeSession(session).cycle(),
                new ConsolidationCycleEngine.Unit("eighth", ConsolidationCycleEngine.Protocol.SNOWBALL));
            fail("snowball cycle must never exceed seven units");
        } catch (IllegalStateException expected) {
            // expected
        }

        ConsolidationCycleEngine.Cycle legacy = engine.startCycle("legacy",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("u1", ConsolidationCycleEngine.Protocol.LIGHT));
        legacy = engine.addUnit(legacy, new ConsolidationCycleEngine.Unit("u2", ConsolidationCycleEngine.Protocol.FULL));
        legacy = engine.addUnit(legacy, new ConsolidationCycleEngine.Unit("u3", ConsolidationCycleEngine.Protocol.LIGHT));
        try {
            engine.addUnit(legacy, new ConsolidationCycleEngine.Unit("u4", ConsolidationCycleEngine.Protocol.LIGHT));
            fail("legacy cycle must never exceed three units");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test public void mixedStabilizationProtocolsShareFrozenGroupSize() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "c1", ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("u1", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle,
            new ConsolidationCycleEngine.Unit("u2", ConsolidationCycleEngine.Protocol.FULL));

        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "s1");
        assertEquals(2, session.sessionGroupSize());
        assertEquals(Arrays.asList("u1", "u2"), session.unitIds());
        assertEquals(18, session.quotaAt(0));
        assertEquals(20, session.quotaAt(1));
        assertArrayEquals(new int[]{10,0,2,3,3}, session.stageVectorAt(0));
        assertArrayEquals(new int[]{7,2,3,3,5}, session.stageVectorAt(1));
    }

    @Test public void openSessionBlocksNewUnitAndSnapshotRemainsImmutable() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "c2", ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("a", ConsolidationCycleEngine.Protocol.LEARNING37));
        ConsolidationCycleEngine.Session opened = engine.openSession(cycle, "s2");
        assertTrue(opened.open());
        assertEquals(1, opened.sessionGroupSize());

        try {
            engine.addUnit(opened.cycle(),
                new ConsolidationCycleEngine.Unit("b", ConsolidationCycleEngine.Protocol.LEARNING37));
            fail("new unit must be blocked while a session is OPEN");
        } catch (IllegalStateException expected) {
            // expected
        }

        ConsolidationCycleEngine.Cycle closed = engine.closeSession(opened).cycle();
        assertFalse(closed.hasOpenSession());
        ConsolidationCycleEngine.Cycle expanded = engine.addUnit(closed,
            new ConsolidationCycleEngine.Unit("b", ConsolidationCycleEngine.Protocol.LEARNING37));
        assertEquals(2, expanded.units().size());
        assertEquals(1, opened.sessionGroupSize());
        assertEquals(Arrays.asList("a"), opened.unitIds());
    }

    @Test public void cycleRejectsParallelOpenSessionFourthUnitAndFamilyProtocolMismatch() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "c3", ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("u1", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("u2", ConsolidationCycleEngine.Protocol.FULL));
        cycle = engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("u3", ConsolidationCycleEngine.Protocol.LIGHT));
        try {
            engine.addUnit(cycle, new ConsolidationCycleEngine.Unit("u4", ConsolidationCycleEngine.Protocol.LIGHT));
            fail("cycle must never exceed three units");
        } catch (IllegalStateException expected) {
            // expected
        }

        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "s3");
        try {
            engine.openSession(session.cycle(), "s4");
            fail("only one OPEN session is allowed per cycle");
        } catch (IllegalStateException expected) {
            // expected
        }

        try {
            engine.startCycle("bad", ConsolidationCycleEngine.Family.LEARNING,
                new ConsolidationCycleEngine.Unit("x", ConsolidationCycleEngine.Protocol.LIGHT));
            fail("learning cycles require LEARNING37 protocol");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test public void openSessionProgressIsExactSkipsZeroStagesAndResumesDeterministically() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "c4", ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("l", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle,
            new ConsolidationCycleEngine.Unit("f", ConsolidationCycleEngine.Protocol.FULL));
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "s4");

        assertEquals(0, session.stage());
        assertEquals(0, session.nextUnitIndex());
        assertEquals(0, session.donePerStage());

        for (int i = 0; i < 10; i++) session = engine.recordRepetition(session);
        assertEquals(0, session.stage());
        assertEquals(1, session.nextUnitIndex());
        assertEquals(0, session.donePerStage());

        for (int i = 0; i < 7; i++) session = engine.recordRepetition(session);
        assertEquals("LIGHT zero stage is skipped when stage 0 closes", 1, session.stage());
        assertEquals("FULL unit is the only unit needing stage 1 work", 1, session.nextUnitIndex());
        assertEquals(0, session.donePerStage());

        session = engine.recordRepetition(session);
        assertEquals(1, session.stage());
        assertEquals(1, session.nextUnitIndex());
        assertEquals(1, session.donePerStage());
    }

    @Test public void restoredOpenSessionMustMatchFrozenSnapshotExactly() {
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "c5", ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("l", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle,
            new ConsolidationCycleEngine.Unit("f", ConsolidationCycleEngine.Protocol.FULL));

        ConsolidationCycleEngine.Session restored = engine.restoreOpenSession(
            cycle, "s5", 2, 1, 1, 1);
        assertTrue(restored.open());
        assertEquals("s5", restored.sessionId());
        assertEquals(2, restored.sessionGroupSize());
        assertEquals(1, restored.stage());
        assertEquals(1, restored.nextUnitIndex());
        assertEquals(1, restored.donePerStage());
        assertArrayEquals(new int[]{10,0,2,3,3}, restored.stageVectorAt(0));
        assertArrayEquals(new int[]{7,2,3,3,5}, restored.stageVectorAt(1));

        try {
            engine.restoreOpenSession(cycle, "bad", 3, 1, 1, 1);
            fail("restored group size must match frozen cycle snapshot");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    private static void assertPlan(ConsolidationCycleEngine.Protocol protocol, int size, int[][] expectedVectors) {
        assertEquals(size, expectedVectors.length);
        for (int position = 0; position < size; position++) {
            int[] actual = ConsolidationCycleEngine.stageVector(protocol, size, position);
            assertArrayEquals(expectedVectors[position], actual);
            int total = 0;
            for (int value : actual) total += value;
            assertEquals(ConsolidationCycleEngine.quota(protocol, size, position), total);
        }
    }
}
