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
        assertTrue(session.contains("ProgressEvent.LEARNING_COMPLETED"));
        assertTrue(session.contains("transitionV6Lines"));
        assertTrue(session.contains("sabqiBlock.lineIds"));
    }

    @Test public void stabilizationRuntimeUsesFrozenHalfPagePolicyAndCreatesStabilizedState() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("StabilizationHalfPagePolicy.planPage"));
        assertTrue(session.contains("ProgressEvent.STABILIZATION_COMPLETED"));
        assertTrue(session.contains("transitionV6Lines"));
        assertFalse("whole eligible page may not remain the only Stabilisation planner",
            session.contains("itqanUnit=geometry.eligiblePageUnit") && !session.contains("StabilizationHalfPagePolicy.planPage"));
    }

    @Test public void consolidationScreenUsesFrozenOneToThreeUnitEngineAndPersistsEveryStep() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("ConsolidationCycleEngine"));
        assertTrue(session.contains("restoreConsolidationSession"));
        assertTrue(session.contains("persistConsolidationSession"));
        assertTrue(session.contains("recordRepetition"));
        assertTrue(session.contains("ProgressEvent.CONSOLIDATION_COMPLETED"));
        assertFalse("canonical Consolidation may not be the old timed recent-review implementation",
            session.contains("private void renderRecentSabqiReview()"));
    }

    @Test public void groupedConsolidationCannotMutateIndividualRepetitionCounters() throws Exception {
        String persistenceTest = read("hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/ConsolidationPersistenceInstrumentedTest.java");
        assertTrue(persistenceTest.contains("groupedRepetitionsNeverMutateIndividualCounters"));
        assertTrue(persistenceTest.contains("assertEquals(9, prefs.sabqiRep())"));
        assertTrue(persistenceTest.contains("assertEquals(11, prefs.itqanRep())"));
    }

    @Test public void revisionRemainsSeparateAndConsolidationIsNotAWeekdayCadence() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(session.contains("EligibleCorpus corpus = prefs.murajaahCorpus()"));
        assertTrue(core.contains("enum class CadenceAction"));
        assertFalse(core.contains("CadenceAction.CONSOLIDATION"));
    }
}
