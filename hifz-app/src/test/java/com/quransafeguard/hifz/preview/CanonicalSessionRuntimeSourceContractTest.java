package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 0.7.5 closure: canonical labels are insufficient; each visible action must drive schema6.
 */
public final class CanonicalSessionRuntimeSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void learningCompletionCreatesExactlyTheLearnedState() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(session.contains("completeSabqiBlockV6"));
        assertTrue(session.contains("sabqiBlock.lineIds"));
        assertTrue(prefs.contains("v6LearnedLineIds"));
    }

    @Test public void stabilizationRuntimeUsesFrozenHalfPagePolicyAndCreatesStabilizedState() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("StabilizationHalfPagePolicy.planPage"));
        assertTrue(session.contains("completeStabilizationBlockV6"));
        assertTrue(session.contains("currentLineIds"));
        assertFalse("whole eligible page may not remain the only Stabilisation planner",
            session.contains("itqanUnit=geometry.eligiblePageUnit") && !session.contains("StabilizationHalfPagePolicy.planPage"));
    }

    @Test public void consolidationScreenUsesFrozenOneToThreeUnitEngineAndPersistsEveryStep() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("ConsolidationCycleEngine"));
        assertTrue(session.contains("restoreConsolidationSession"));
        assertTrue(session.contains("persistConsolidationSession"));
        assertTrue(session.contains("recordRepetition"));
        assertTrue(session.contains("completeConsolidationSessionV6"));
        assertFalse("canonical Consolidation may not be the old timed recent-review implementation",
            session.contains("private void renderRecentSabqiReview()"));
    }

    /**
     * Readiness for the evening snowball no longer re-derives "ready" units from raw state each
     * time (ConsolidationPhysicalUnitPolicy.readyUnits): it reads back the exact physical units
     * this week's morning completions already appended, via the same encode/decode round-trip.
     */
    @Test public void consolidationRuntimePreservesExactPhysicalStabilizationUnits() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("stabilizedConsolidationUnits"));
        assertTrue(prefs.contains("weeklySnowballUnits"));
        assertTrue(prefs.contains("ConsolidationPhysicalUnitPolicy.encodeLineUnit"));
        assertTrue(prefs.contains("ConsolidationPhysicalUnitPolicy.decodeLineUnit"));
        assertTrue(session.contains("prefs.stabilizedConsolidationUnits"));
        assertTrue(session.contains("ConsolidationPhysicalUnitPolicy.decodeLineUnit"));
        assertFalse(session.contains("private AnchoringQueue.Entry consolidationEntry("));
        assertFalse(session.contains("private static String consolidationUnitId(AnchoringQueue.Entry entry)"));
    }

    @Test public void groupedConsolidationCannotMutateIndividualRepetitionCounters() throws Exception {
        String persistenceTest = read("hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/ConsolidationPersistenceInstrumentedTest.java");
        assertTrue(persistenceTest.contains("persistingEveryGroupedRepetitionNeverMutatesIndividualCounters"));
        assertTrue(persistenceTest.contains("assertTrue(prefs.setSabqiProgress(29, 6))"));
        assertTrue(persistenceTest.contains("assertTrue(prefs.setItqanProgress(31, 7, 5"));
        assertTrue(persistenceTest.contains("assertArrayEquals(individualBefore, individualCounters(prefs))"));
    }

    @Test public void revisionRemainsSeparateAndConsolidationIsNotAWeekdayCadence() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(session.contains("EligibleCorpus corpus = prefs.murajaahCorpus()"));
        assertTrue(core.contains("enum class CadenceAction"));
        assertFalse(core.contains("CadenceAction.CONSOLIDATION"));
    }
}
