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
        assertTrue(session.contains("if (kind == SessionKind.OLD_ITQAN_MURAJAAH || kind == SessionKind.ACTIVE_MURAJAAH) {"));
        assertTrue(session.contains("return MaintenanceCoveragePolicy.minutes(\n"
            + "            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());"));
    }

    @Test public void weeklyDashboardPlannerNoLongerUsesTheFlatConstant() throws Exception {
        String planner = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertFalse("the 7-day ETA projection must use the same dynamic duration as a real session",
            planner.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(planner.contains("private int maintenanceMinutes(){"));
        assertTrue(planner.contains("HifzSpeedStore speedStore"));
        assertTrue(planner.contains("final int revisionMinutes=maintenanceMinutes();"));
        assertFalse(planner.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }

    @Test public void mainActivityTodayLabelAndPlannerCallSiteBothUseTheDynamicDuration() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse(main.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue("Home must show a simple label instead of a technical duration",
            main.contains("setToday(prefix+todaySummary(mode)+learningFraction,true);")
            && main.contains("WeeklyDashboardPlanner.learningSessionCue(scheduledForCue.getDayOfWeek()")
            && main.contains("HifzSessionActivity.SABQI.equals(mode)"));
        assertTrue("The actual dynamic Révision duration is still implemented by the session",
            read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")
                .contains("MaintenanceCoveragePolicy.minutes("));
        assertTrue(main.contains("new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());"));
        assertTrue(main.contains("final int revisionMinutes=MaintenanceCoveragePolicy.minutes("));
        assertFalse(main.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }

    @Test public void revisionSelectorUsesTheSameAcquiredDurationForBothModes() throws Exception {
        String selector = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/RevisionSelector.java");
        assertTrue("One acquired-corpus coverage calculation must drive both menu options",
            selector.contains("MaintenanceCoveragePolicy.minutes(")
                && selector.contains("prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine()"));
        assertTrue("Active revision must display the same dynamic duration as Entretien",
            selector.contains("\"Révision active · \" + maintenanceCue + \" · \"")
                && selector.contains("\"Entretien · \" + maintenanceCue + \" · \""));
        assertFalse("The historical 15-minute label must never return to the menu",
            selector.contains("Révision active · 15 min"));
    }

    @Test public void settingsActivityDisplayStringsBothUseTheDynamicDuration() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertFalse(settings.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(settings.contains("private int maintenanceMinutes(){"));
        assertTrue(settings.contains("+maintenanceMinutes()+\" min les jours de semaine. Aucune Révision le dimanche.\");"));
        assertTrue(settings.contains("+maintenanceMinutes()+\" min · chaque soir · position \"+prefs.murajaahCursor());"));
        assertTrue(settings.contains("Révision active \"+maintenanceMinutes()+\" min et Entretien passif"));
        assertFalse(settings.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }
}
