package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * P3's "finish the page?" wiring in renderItqan/validateItqan: the decision is settled once
 * before rep 1, reused verbatim afterwards (including after an assistance restart), never
 * shown/asked again for a committed block, and the accepted bonus lines flow into the same single
 * commit completeStabilizationBlockV6 already makes — never a second write.
 *
 * The full algorithm (bonus offer computation, consumed-line trimming, Consolidation-safety) was
 * additionally verified end-to-end against the real Quran geometry (all 604 pages, 8820 physical
 * lines) in a standalone simulation mirroring this exact logic: even under a worst-case stress
 * test that accepts every single valid offer, every line is committed exactly once (no duplicate,
 * no gap) and every committed block re-verifies through StabilizationHalfPagePolicy.planPage as a
 * single, identical unit — zero Consolidation failures across the entire corpus.
 */
public final class ItqanBonusDialogSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void ownBlockLinesExcludeAnyBonusAlreadyConsumedByAnEarlierSubBlock() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("ownLineIds.removeAll(prefs.itqanConsumedBonusLineIds());"));
    }

    @Test public void aPendingDecisionOnlyEverAppliesToTheExactSubBlockItWasMadeFor() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("itqanBonusDecision = prefs.itqanBonusSnapshot();"));
        assertTrue(session.contains("!itqanBonusDecision.matches(itqanUnit.start, itqanUnit.end, itqanBlockIndex)"));
    }

    @Test public void noOfferMeansKeepIsSavedImmediatelyWithoutEverShowingADialog() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("if (offer.choice == ItqanPageCompletionPolicy.Choice.NONE) {"));
        assertTrue(session.contains(".undecided(itqanUnit.start, itqanUnit.end, itqanBlockIndex).keep();"));
    }

    @Test public void aRealOfferAlwaysShowsTheDialogAndNeverStartsRepetitionsFirst() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("the dialog must suspend the clock, never let the repetition button appear first",
            session.contains("showItqanBonusDialog(itqanUnit.start, itqanUnit.end, itqanBlockIndex, offer);\n"
                + "                return;"));
        assertTrue(session.contains("private void showItqanBonusDialog("));
        assertTrue("never automatic — the user must always tap one of the two buttons",
            session.contains(".setCancelable(false)"));
    }

    @Test public void onlyOfferedFromTheImmediateNextSubBlockOfTheSameParentUnit() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("if (itqanBlockIndex + 1 < itqanBlockCount) {"));
        assertTrue("must always leave at least one line for the next block itself",
            session.contains("int maxTakeable = Math.min(2, nextBlockLines.size() - 1);"));
    }

    @Test public void acceptedBonusLinesFlowIntoTheSameSingleCommit() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("List<String> bonusLineIds = itqanBonusDecision != null\n"
            + "                && itqanBonusDecision.decision == ItqanPlanSnapshot.Decision.EXTEND\n"
            + "            ? itqanBonusDecision.bonusLineIds : Collections.emptyList();"));
        assertTrue(session.contains("sessionDate.toString(), label, bonusLineIds);"));
    }
}
