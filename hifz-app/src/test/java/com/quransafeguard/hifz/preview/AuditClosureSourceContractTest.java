package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guards for the post-Claude audit closure. */
public final class AuditClosureSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void anchoringFailureIsAtomicAndSinglePageRetryIsExplicit() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String queue = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AnchoringQueue.java");
        assertTrue(prefs.contains(".putLong(\"itqanElapsedMs\", 0L)"));
        assertTrue(prefs.contains("anchoringRetryAfterDate"));
        assertTrue(queue.contains("return new Deferral(Collections.singletonList(displayed), -1)"));
    }

    @Test public void queueCachingAndDashboardProjectionUsePersistedVisitOrder() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String dashboard = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(prefs.contains("!p.getBoolean(\"anchoringQueueInitialized\", false)"));
        assertTrue(main.contains("prefs.currentAnchoringEntry(loaded)"));
        assertTrue(dashboard.contains("AnchoringQueue.visitOrder"));
        assertTrue(dashboard.contains("prefs.anchoringQueueIndex()"));
        assertTrue(dashboard.contains("date.equals(today)&&prefs.anchoringDeferredToday()"));
        assertTrue(dashboard.contains("Stabilisation · unité reportée"));
        assertFalse(dashboard.contains("Ancrage · page reportée"));
    }

    @Test public void localeTimerAndMurajaahCursorFixesRemainClosed() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(prefs.contains("toLowerCase(Locale.ROOT) + \"ElapsedMs\""));
        assertTrue(session.contains("SessionTimerPolicy.label(mode, elapsed, targetMinutes())"));
        assertTrue(session.contains("EligibleCorpus corpus = prefs.murajaahCorpus()"));
        assertTrue(session.contains("if (!corpus.contains(through))"));
        assertTrue(session.contains("validation possible à tout moment"));
        assertFalse(session.contains("VerseRef itqanBefore = prefs.itqanCursor()"));
        assertFalse(session.contains("prefs.setItqanCursor(itqanBefore)"));
    }

    @Test public void forcedPromotionIsVisibleAndCompletionFailsClosed() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(prefs.contains("forcedPromotedRanges"));
        assertTrue(session.contains("promotion de sécurité"));
        assertTrue(prefs.contains("if (completedIndex < 0) return false"));
    }

    @Test public void slidingDashboardStartsWithToday() throws Exception {
        String dashboard = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(dashboard.contains("today.plusDays(i)"));
        assertTrue(dashboard.contains("return \"Aujourd’hui\""));
        assertFalse(dashboard.contains("previousOrSame"));
    }
}
