package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * P4's real migration: the Ancrage/Stabilisation queue's own selection (HifzPrefs.
 * currentAnchoringEntry) no longer scans anchoringQueue cyclically from a persisted list index —
 * it now picks the earliest not-yet-Stabilisé/Acquis entry in PerpetualItqanSource's perpetual
 * TAIL(Al-Hujurāt→An-Nās)/FRONT(Al-Baqara→wherever Sabqi has actually reached) order, forever.
 * Consolidation is not separately wired: it only ever processes material that already cleared
 * Stabilisation here, so it inherits this same order for free.
 */
public final class AncragePerpetualRotationWiringSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void perpetualStateIsPersistedWithATailStartingDefault() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains(
            "p.getString(\"p4AncrageLeg\", PerpetualItqanSource.Leg.TAIL_HUJURAT_NAS.name())"));
        assertTrue(prefs.contains("p.getBoolean(\"p4AncrageInitialTailCompleted\", false)"));
    }

    @Test public void currentAnchoringEntryNoLongerScansTheQueueCyclically() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the historical cyclic list-index scan must be gone",
            !prefs.contains("int start = anchoringQueueIndex(queue.size());"));
        assertTrue("selection must go through PerpetualItqanSource, given every not-yet-done entry",
            prefs.contains("List<AnchoringQueue.Entry> notDone = new ArrayList<>();"));
        assertTrue(prefs.contains(
            "PerpetualItqanSource.Selection selection = PerpetualItqanSource.selectNext(\n"
                + "            currentState, notDone, currentSabqiPosition(geometry));"));
    }

    @Test public void aLegFlipIsPersistedButAStableLegIsNotRewrittenEveryCall() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("only an actual leg change should trigger a write, mirroring the old "
                + "index != start guard this replaces",
            prefs.contains("if (selection.state.leg != currentState.leg && !savePerpetualItqanState(selection.state)) {"));
    }
}
