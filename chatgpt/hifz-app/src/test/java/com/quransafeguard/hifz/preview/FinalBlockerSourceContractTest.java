package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source-level closeout contracts for the final blocker fixes. */
public final class FinalBlockerSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void corruptConsolidationIsVisibleAndRoutesToDiagnostic() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("Consolidation · état à vérifier"));
        assertTrue(main.contains("consolidationNeedsAttention"));
        assertTrue(main.contains("SettingsActivity.class"));
    }

    @Test public void quarantineHasUserRecoveryPath() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(prefs.contains("List<String> v6QuarantineLineIds()"));
        assertTrue(settings.contains("RETURN_TO_STABILIZATION"));
        assertTrue(settings.contains("Quarantaine"));
    }
}
