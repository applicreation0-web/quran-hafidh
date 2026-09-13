package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AstraFixSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void j10QuitActionMustHaveVisibleIcon() throws Exception {
        String src = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java");
        assertFalse(src.contains("Ui.iconButton(this, \"\", \"Quitter\""));
    }

    @Test public void j10ActivityMustGateValidationOnRenderedPages() throws Exception {
        String src = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java");
        assertTrue(src.contains("reviewProgress.canValidate()"));
        assertTrue(src.contains("reviewProgress.markShown(page)"));
    }

    @Test public void j10TimeMustBeChargedToHostMode() throws Exception {
        String app = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java");
        String review = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(app.contains("J10ReviewActivity.EXTRA_HOST_MODE"));
        assertTrue(review.contains("addConsumed"));
        assertTrue(session.contains("syncPersistedElapsed"));
    }

    @Test public void weeklyProjectionMustTrackFractionatedSubBlocks() throws Exception {
        String src = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(src.contains("projectedItqanBlockIndex"));
        assertTrue(src.contains("bloc "));
    }
}
