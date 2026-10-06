package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: a strict-acquired-only reader stayed empty despite Réglages showing a real
 * "Plages acquises" range (2:1→2:74) — traced to a real bootstrap gap. ensureSchema seeds
 * itqanRanges (the range-level "Plages acquises" the user sees) but leaves
 * v6AcquiredCreditLineIds (the per-line credit every strict-acquired-only reader actually checks —
 * entryIsFullyStabilizedOrAcquired, ETA math) empty, since computing owned physical lines needs
 * GeometryRepository, real I/O the SharedPreferences-only constructor never does.
 * reconcileV6AcquiredBootstrap backfills it once real geometry is available, touching only a line
 * still in its pristine NONE state.
 */
public final class V6AcquiredBootstrapSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void bootstrapSeedsItqanRangesButLeavesAcquiredCreditEmpty() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains(".putString(\"itqanRanges\", defaultItqanRangesJson())"));
        assertTrue(prefs.contains(".putString(\"v6AcquiredCreditLineIds\", \"[]\")"));
    }

    @Test public void reconciliationBackfillsOnceAndOnlyUntouchedLines() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must be a one-time, idempotent backfill",
            prefs.contains("public boolean reconcileV6AcquiredBootstrap(GeometryRepository geometry) {\n"
                + "        if (p.getBoolean(\"v6AcquiredBootstrapSeeded\", false)) return true;"));
        assertTrue("must never touch a line schema6 already has an opinion about",
            prefs.contains("if (learned.contains(lineId) || stabilized.contains(lineId) || acquired.contains(lineId)\n"
                + "                        || quarantine.contains(lineId) || legacyPartial.contains(lineId)) continue;"));
        assertTrue("must credit exactly the physical lines itqanRanges (\"Plages acquises\") owns",
            prefs.contains("for (String lineId : CorpusLinePolicy.ownedLineIds(itqanRanges(), allLines)) {"));
    }

    @Test public void wiredAsSoonAsRealGeometryLoadsOnHomeScreen() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("must run before the anchoring selection that also needs this same geometry",
            main.contains("GeometryRepository loaded = GeometryRepository.get(getApplicationContext());\n"
                + "                prefs.reconcileV6AcquiredBootstrap(loaded);\n"
                + "                prefs.repairStraddlingAcquiredVerses(loaded);\n"
                + "                prefs.currentAnchoringEntry(loaded);"));
    }
}
