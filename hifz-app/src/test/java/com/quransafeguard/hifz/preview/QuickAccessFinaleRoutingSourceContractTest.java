package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly, confirmed on a live user export: the "Renforcement"/"Consolidation"
 * direct-access tiles on the home screen always opened the non-graduating evening snowball
 * review (LEARNING_CONSOLIDATION/RECENT_SABQI_REVIEW) — even on the Sunday the real finale
 * (LEARNING_FINAL/CONSOLIDATION_FINAL, the only thing that ever moves lines to Acquis and
 * unlocks them for Entretien) was due and unresolved. Only "Aujourd'hui" ever reached the
 * finale, via nextMode's own Sunday-due check. A learner using these shortcuts instead could
 * complete every review faithfully — with no error, no warning, an identical "séance validée"
 * screen — and never graduate a single line. The user's export showed recentSabqi still holding
 * every Sabqi block since program day one and promotedRanges/unconsolidatedPromotedRanges
 * identical, proving completeLearningConsolidationSessionV6/completeConsolidationSessionV6 had
 * never once run. Fixed by giving each tile the same due-check nextMode already uses, so tapping
 * either one on an unresolved Sunday reaches the finale directly instead of silently bypassing it.
 */
public final class QuickAccessFinaleRoutingSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void quickAccessTilesNoLongerHardcodeTheNonGraduatingReview() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("the Renforcement tile must resolve its mode instead of always opening the evening review",
            main.contains("\"Renforcement\", \"Boule de neige\", v -> openMode(renforcementQuickAccessMode())"));
        assertTrue("the Consolidation tile must resolve its mode instead of always opening the evening review",
            main.contains("\"Consolidation\", \"Boule de neige\", v -> openMode(consolidationQuickAccessMode())"));
        assertFalse("the tiles must not hardcode the non-final mode directly anymore",
            main.contains("v -> openMode(HifzSessionActivity.LEARNING_CONSOLIDATION)"));
        assertFalse("the tiles must not hardcode the non-final mode directly anymore",
            main.contains("v -> openMode(HifzSessionActivity.RECENT_SABQI_REVIEW)"));
    }

    @Test public void renforcementTileRoutesToTheFinaleWhenDueAndUnresolved() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String m = method(main, "private String renforcementQuickAccessMode(){", "\n    }");
        assertTrue("must use the same REVISION-day check nextMode uses for Sunday",
            m.contains("CadenceAction.REVISION"));
        assertTrue("must use the same unresolved check nextMode uses before routing to the finale",
            m.contains("!learningFinalResolved(today)"));
        assertTrue("must open the graduating finale when due and unresolved",
            m.contains("? HifzSessionActivity.LEARNING_FINAL : HifzSessionActivity.LEARNING_CONSOLIDATION"));
    }

    @Test public void consolidationTileRoutesToTheFinaleWhenDueAndUnresolved() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String m = method(main, "private String consolidationQuickAccessMode(){", "\n    }");
        assertTrue("must use the same REVISION-day check nextMode uses for Sunday",
            m.contains("CadenceAction.REVISION"));
        assertTrue("must use the same unresolved check nextMode uses before routing to the finale",
            m.contains("!consolidationFinalResolved(today)"));
        assertTrue("must open the graduating finale when due and unresolved",
            m.contains("? HifzSessionActivity.CONSOLIDATION_FINAL : HifzSessionActivity.RECENT_SABQI_REVIEW"));
    }
}
