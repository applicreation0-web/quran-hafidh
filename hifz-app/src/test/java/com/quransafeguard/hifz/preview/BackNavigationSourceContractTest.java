package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * ProgressMapActivity and WeakVersesActivity both shipped with no way back except the system
 * gesture/button — every other non-home screen (FreeMemActivity, HifzSessionActivity,
 * SettingsActivity, StudyReaderActivity) has an explicit "Retour" icon button, so this is the odd
 * one out rather than a deliberate choice. Pins the fix and guards every full-screen Activity
 * except MainActivity (the task root, which needs none) so a future screen can't ship the same gap.
 */
public final class BackNavigationSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static final String[] NON_HOME_ACTIVITIES = {
        "FreeMemActivity", "HifzSessionActivity", "SettingsActivity",
        "StudyReaderActivity", "ProgressMapActivity", "WeakVersesActivity",
    };

    @Test public void everyNonHomeActivityOffersAnExplicitWayBack() throws Exception {
        for (String name : NON_HOME_ACTIVITIES) {
            String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/" + name + ".java");
            assertTrue(name + " must offer an explicit \"Retour\" control — the system back "
                    + "gesture/button alone is not a reliable enough affordance on this app's "
                    + "target e-ink devices",
                source.contains("\"Retour\""));
        }
    }
}
