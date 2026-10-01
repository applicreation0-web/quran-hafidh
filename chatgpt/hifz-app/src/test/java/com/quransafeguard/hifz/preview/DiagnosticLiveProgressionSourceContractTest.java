package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly: Diagnostic's "Plages Acquises" and "À stabiliser" counted itqanRanges()/
 * unconsolidatedPromotedRanges() — the declared/manual corpus and the Sabqi-only promotion
 * bucket — neither of which the Itqan/Stabilisation track's own session-completion methods ever
 * update (the exact same bug ProgressMapActivity had). Those two numbers never moved as real
 * Itqan/Stabilisation work happened. They now read the same live per-line schema6 state
 * (progressionSnapshotV6) the Progress Map and every completion method use.
 */
public final class DiagnosticLiveProgressionSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void diagnosticReadsLivePerLineItqanProgressNotTheStaleRangeBuckets() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("must read the same live per-line snapshot the Progress Map uses",
            settings.contains("HifzPrefs.ProgressionSnapshot progression=prefs.progressionSnapshotV6();"));
        assertTrue("Acquis count must come from the live ACQUIRED set, honestly labeled in lines",
            settings.contains("\"\\nLignes acquises (Itqan) : \"+progression.acquired.size()"));
        assertTrue("À stabiliser count must come from the live STABILIZED set, honestly labeled in lines",
            settings.contains("\"\\nLignes à stabiliser (Itqan) : \"+progression.stabilized.size()"));
        assertFalse("must not fall back to the stale declared-corpus range count",
            settings.contains("prefs.itqanRanges().size()"));
        assertFalse("must not fall back to the stale Sabqi-only pending-promotion range count",
            settings.contains("prefs.unconsolidatedPromotedRanges().size()"));
    }

    @Test public void promotedRangesCountIsHonestlyLabeledAsRangesNotPages() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        // promotedRanges() is a live, correct Sabqi-track signal — only its old "Pages promues"
        // label was wrong: List<VerseRange>.size() counts discontiguous range segments, not pages.
        assertTrue("must relabel the range-segment count honestly instead of implying a page count",
            settings.contains("\"\\nPlages promues (Sabqi) : \"+prefs.promotedRanges().size()"));
        assertFalse("the old page-count-implying label must not remain",
            settings.contains("Pages promues"));
    }
}
