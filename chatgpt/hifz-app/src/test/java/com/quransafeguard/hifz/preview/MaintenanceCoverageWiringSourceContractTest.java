package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * P4's J-15 dynamic Entretien duration must replace every reader of the old flat
 * HifzSchedule.MAINTENANCE_MINUTES constant for the passive Murajaah/Entretien session — the
 * session's own target-minutes computation, the MainActivity dashboard's "today" label, the
 * WeeklyDashboardPlanner's 7-day ETA projection, and both SettingsActivity display strings.
 */
public final class MaintenanceCoverageWiringSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void acquiredLineCountV6ExposesOnlyACountNeverTheLiveSet() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("int acquiredLineCountV6() {\n"
            + "        return v6LineIdSet(\"v6AcquiredCreditLineIds\").size();\n    }"));
    }

    @Test public void sessionDurationUsesMaintenanceCoveragePolicyForPlainMurajaah() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("if (kind == SessionKind.OLD_ITQAN_MURAJAAH) {"));
        assertTrue(session.contains("return MaintenanceCoveragePolicy.minutes(\n"
            + "            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());"));
    }

    @Test public void weeklyDashboardPlannerNoLongerUsesTheFlatConstant() throws Exception {
        String planner = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertFalse("the 7-day ETA projection must use the same dynamic duration as a real session",
            planner.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(planner.contains("private int maintenanceMinutes(){"));
        assertTrue(planner.contains("HifzSpeedStore speedStore"));
    }

    @Test public void mainActivityTodayLabelAndPlannerCallSiteBothUseTheDynamicDuration() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse(main.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(main.contains("int maintenanceMinutes=MaintenanceCoveragePolicy.minutes(\n"
            + "                    prefs.acquiredLineCountV6(), g.lineCount(), speedStore.maintenanceSecondsPerLine());"));
        assertTrue(main.contains("new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());"));
    }

    @Test public void settingsActivityDisplayStringsBothUseTheDynamicDuration() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertFalse(settings.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(settings.contains("private int maintenanceMinutes(){"));
        assertTrue(settings.contains("+maintenanceMinutes()+\" min d’Entretien de l’Acquis, dimanche soir compris.\");"));
        assertTrue(settings.contains("+maintenanceMinutes()+\" min · chaque soir · position \"+prefs.murajaahCursor());"));
    }
}
