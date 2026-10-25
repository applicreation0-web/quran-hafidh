package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Protect current persisted V6 credit from synthetic bootstrap/backfill.
 * Historical initialization change 68ddc537 is reviewed separately, read-only.
 * These tests cannot authorize changes to initializations or migrations.
 */
public final class V6AcquiredBootstrapSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void freshBootstrapKeepsPhysicalCreditEmptyAndNeverAssumesAcquiredRanges() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains(".putString(\"itqanRanges\", \"[]\")"));
        assertTrue(prefs.contains(".putBoolean(\"v6AcquiredBootstrapSeeded\", true)"));
        assertTrue(prefs.contains(".putString(\"v6AcquiredCreditLineIds\", \"[]\")"));
    }

    // 68ddc537 removed automatic credit. This contract must never restore it.
    @Test public void reconciliationOnlyMarksCompletionAndNeverRewritesProgress() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        int start = prefs.indexOf("public boolean reconcileV6AcquiredBootstrap(");
        String method = prefs.substring(start, prefs.indexOf("\n    }", start));
        assertTrue(method.contains("if (geometry == null) throw new IllegalArgumentException"));
        assertTrue(method.contains("if (p.getBoolean(\"v6AcquiredBootstrapSeeded\", false)) return true;"));
        assertTrue(method.contains("requireSchema6ProgressionState();"));
        assertTrue(method.contains("putBoolean(\"v6AcquiredBootstrapSeeded\", true).commit()"));
        org.junit.Assert.assertFalse("Never synthesize credits from configured ranges", method.contains("ownedLineIds"));
        org.junit.Assert.assertFalse("Never rewrite progression, dates, journals or snapshots", method.contains("putString("));
        org.junit.Assert.assertFalse(method.contains(".remove("));
        org.junit.Assert.assertFalse(method.contains(".clear("));
    }

    @Test public void wiredAsSoonAsRealGeometryLoadsOnHomeScreen() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("must run before the anchoring selection that also needs this same geometry",
            main.contains("GeometryRepository loaded = GeometryRepository.get(getApplicationContext());\n"
                + "                prefs.reconcileV6AcquiredBootstrap(loaded);\n"
                + "                prefs.repairStraddlingAcquiredVerses(loaded);\n"
                + "                prefs.releaseRevisionCursorsFromFatiha();\n"
                + "                prefs.currentAnchoringEntry(loaded);"));
    }
}
