package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly, from a live export: a program that had already declared 49:1-49:8 as
 * Plages Acquises and just consolidated 49:9-49:17 via its first-ever Renforcement/Consolidation
 * finale got sent all the way back to 49:1 the moment the perpetual Itqān rotation
 * (ItqanRotationPolicy) needed a fresh position for the very first time. itqanRotationState's
 * documented fallback for a never-tracked cursor is to restart the leg from its own start — safe
 * in that nothing is skipped, but it cost the learner a full 35-repetition drill on material
 * already mastered weeks earlier, and would have kept doing so every time a fresh pick was needed
 * with no persisted cursor. Fixed by reconciling the very first fresh pick to the first verse not
 * yet fully Stabilisé/Acquis instead of the leg's absolute start, and persisting it immediately so
 * this only ever runs once.
 */
public final class ItqanRotationBootstrapSourceContractTest {
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

    @Test public void currentAnchoringEntryReconcilesAFreshRotationBeforeSelecting() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String entry = method(prefs, "public AnchoringQueue.Entry currentAnchoringEntry(GeometryRepository geometry) {",
            "\n    }");
        assertTrue("must run the shared one-time bootstrap before picking a unit",
            entry.contains("ensureItqanRotationBootstrapped(geometry);"));
        String bootstrap = method(prefs, "private void ensureItqanRotationBootstrapped(GeometryRepository geometry) {", "\n    }");
        assertTrue("must check whether the rotation has ever had a real position before trusting its default",
            bootstrap.contains("needsItqanRotationBootstrap()"));
        assertTrue("must reconcile against real progress, not the leg's absolute start",
            bootstrap.contains("reconciledLegStartCursor(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, geometry)"));
        assertTrue("the reconciled position must be persisted immediately so this runs only once",
            bootstrap.contains("saveItqanRotationState(new ItqanRotationPolicy.State("));
    }

    @Test public void reconciliationStopsAtTheFirstNotYetMasteredVerse() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "private VerseRef reconciledLegStartCursor(", "\n    }");
        assertTrue("must walk from the leg's own start",
            m.contains("? ItqanRotationPolicy.TAIL_START : ItqanRotationPolicy.FRONT_START"));
        assertTrue("must check both Stabilisé and Acquis lines — either counts as already mastered",
            m.contains("!stabilized.contains(id) && !acquired.contains(id)"));
        assertTrue("must return as soon as a not-fully-mastered verse is found, not walk the whole leg",
            m.contains("if (!done) return cursor;"));
    }

    /**
     * Reported directly, right after the first fix shipped: the learner extended Plages Acquises
     * in Settings (49:1-49:8 -> 49:1-49:18) specifically to force the bootstrap past the 1-verse
     * fragment straight to Qaf, but the reconciliation only checked v6StabilizedLineIds/
     * v6AcquiredCreditLineIds — a declared range in Settings never touches those (see
     * reconcileV6AcquiredBootstrap's one-time seeding gate), so the edit had no effect. Must also
     * honour itqanRanges() — the learner's own explicit "I already know this" declaration — while
     * still never trusting promotedRanges/effectiveItqanRanges for the same purpose, since those
     * only mark material ELIGIBLE for stabilisation, not already mastered.
     */
    @Test public void reconciliationAlsoHonoursADeclaredPlageAcquise() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "private VerseRef reconciledLegStartCursor(", "\n    }");
        assertTrue("must read the learner's own declared corpus",
            m.contains("List<VerseRange> declaredAcquired = itqanRanges();"));
        assertTrue("a verse inside a declared range must count as already mastered",
            m.contains("range.contains(cursor)"));
        assertFalse("must never trust the auto-grown eligible corpus as already mastered",
            m.contains("promotedRanges()") || m.contains("effectiveItqanRanges()"));
    }

    @Test public void bootstrapGateOnlyFiresWhenNoCursorWasEverPersisted() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "private boolean needsItqanRotationBootstrap() {", "\n    }");
        assertTrue("must gate on the real persisted cursor key, not a guess",
            m.contains("p.getString(\"p4ItqanRotationCursor\", \"\").isEmpty()"));
        assertTrue("must not re-bootstrap an install that migrated from the older leg-only rotation",
            m.contains("!p.contains(\"p4AncrageLeg\")"));
    }
}
