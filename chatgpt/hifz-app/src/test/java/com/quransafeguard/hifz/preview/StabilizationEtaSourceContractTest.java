package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Diagnostic's "Estimation fin Stabilisation" answers a real learner question ("when will à
 * stabiliser be done?") from the same live data everything else in Diagnostic already reads —
 * unconsolidatedPromotedRanges' physical lines not yet Stabilized/Acquired — divided by
 * Stabilisation's actual fixed weekly pace (PreviewConfig.STABILIZATION_WEEKLY_LINES), never a
 * hand-typed guess.
 */
public final class StabilizationEtaSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void remainingLinesCountExcludesAlreadyStabilizedOrAcquired() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must walk the real à-stabiliser ranges, not a stale snapshot",
            prefs.contains("for (VerseRange range : unconsolidatedPromotedRanges()) {"));
        assertTrue("must resolve physical line ownership the same way Stabilisation itself does",
            prefs.contains("CorpusLinePolicy.ownedLineIdsForRangeOnPage("));
        assertTrue("a line already Stabilized must not count as remaining work",
            prefs.contains("if (!stabilized.contains(lineId) && !acquired.contains(lineId)) remaining++;"));
    }

    @Test public void diagnosticShowsALiveEtaDerivedFromThePace() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("Diagnostic must surface the estimate, not just raw counts",
            settings.contains("+\"\\nEstimation fin Stabilisation : \"+stabilizationEtaSummary()"));
        assertTrue("the projection must divide by Stabilisation's real fixed weekly pace, never a "
                + "second hardcoded copy of the number",
            settings.contains("(remaining+PreviewConfig.STABILIZATION_WEEKLY_LINES-1)/PreviewConfig.STABILIZATION_WEEKLY_LINES"));
        assertTrue("the projection must be dated from today, not a static offset",
            settings.contains("HifzClock.today().plusWeeks(weeks)"));
        assertTrue("a cleared queue must say so plainly instead of a meaningless \"0 weeks\"",
            settings.contains("if(remaining==0)return \"à jour\";"));
    }
}
