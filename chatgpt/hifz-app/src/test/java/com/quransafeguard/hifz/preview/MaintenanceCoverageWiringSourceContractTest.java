package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Product contract: Entretien is a fixed 30-minute session everywhere it is displayed or planned. */
public final class MaintenanceCoverageWiringSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void dynamicMaintenancePolicyIsGone() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(session.contains("MaintenanceCoveragePolicy"));
        assertTrue(session.contains("HifzSchedule.INSTANCE.targetMinutesFor(kind)"));
    }

    @Test public void weeklyDashboardUsesTheFixedCoreConstant() throws Exception {
        String planner = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(planner.contains("return HifzSchedule.MAINTENANCE_MINUTES;"));
    }

    @Test public void mainActivityUsesTheFixedCoreConstant() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("detail=\"Révision · \"+HifzSchedule.MAINTENANCE_MINUTES+\" min\";"));
        assertFalse(main.contains("MaintenanceCoveragePolicy"));
    }

    @Test public void settingsUseTheFixedCoreConstant() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("return HifzSchedule.MAINTENANCE_MINUTES;"));
        assertFalse(settings.contains("MaintenanceCoveragePolicy"));
    }
}
