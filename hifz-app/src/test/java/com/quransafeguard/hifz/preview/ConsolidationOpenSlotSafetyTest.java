package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** OPEN slots may advance their own repetitions, never replace another frozen session. */
public final class ConsolidationOpenSlotSafetyTest {
    private final ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
    private ConsolidationCycleEngine.Session open(String id, String sessionId) {
        String unit = ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p001-l01", "p001-l02"));
        return engine.openSession(engine.startCycle(id, ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit(unit, ConsolidationCycleEngine.Protocol.SUNDAY_PAGE)), sessionId);
    }
    @Test public void anotherDateCannotSilentlyReplaceAnOpenFinale() {
        InMemoryPrefs disk = new InMemoryPrefs();
        HifzPrefs prefs = new HifzPrefs(disk);
        assertTrue(prefs.persistConsolidationSession(open("learning-final-2026-10-18", "old")));
        Map<String, ?> before = disk.getAll();
        try {
            prefs.persistConsolidationSession(open("learning-final-2026-10-25", "new"));
            fail("Another OPEN finale must be refused");
        } catch (IllegalStateException expected) { }
        assertEquals("Every original preference must survive refusal", before, disk.getAll());
    }
    @Test public void anotherSessionCannotReplaceTheSameDatedFrozenFinale() {
        InMemoryPrefs disk = new InMemoryPrefs();
        HifzPrefs prefs = new HifzPrefs(disk);
        assertTrue(prefs.persistConsolidationSession(open("learning-final-2026-10-25", "old")));
        Map<String, ?> before = disk.getAll();
        try {
            prefs.persistConsolidationSession(open("learning-final-2026-10-25", "replacement"));
            fail("Frozen session identity must be preserved");
        } catch (IllegalStateException expected) { }
        assertEquals(before, disk.getAll());
    }
    @Test public void ownNextRepetitionPreservesSnapshotAndSurvivesReopen() {
        InMemoryPrefs disk = new InMemoryPrefs();
        HifzPrefs prefs = new HifzPrefs(disk);
        ConsolidationCycleEngine.Session session = open("learning-final-2026-10-25", "same");
        assertTrue(prefs.persistConsolidationSession(session));
        String snapshot = prefs.frozenConsolidationSnapshot(ConsolidationCycleEngine.Family.LEARNING,
            "learning-final-2026-10-25");
        assertTrue(prefs.persistConsolidationSession(engine.recordRepetition(session)));
        HifzPrefs reopened = new HifzPrefs(disk);
        ConsolidationCycleEngine.Session restored = reopened.restoreConsolidationSession(engine,
            ConsolidationCycleEngine.Family.LEARNING, "learning-final-2026-10-25");
        assertEquals(1, restored.donePerStage());
        assertEquals(snapshot, reopened.frozenConsolidationSnapshot(
            ConsolidationCycleEngine.Family.LEARNING, "learning-final-2026-10-25"));
    }
}
