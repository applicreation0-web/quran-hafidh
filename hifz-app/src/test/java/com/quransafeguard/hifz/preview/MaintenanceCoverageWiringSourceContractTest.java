package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Dated active review uses the fixed corpus bands and persisted targets.
 * The legacy passive reader remains compatible with older sessions.
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

    @Test public void weeklyDashboardPlannerReadsFixedCorpusBandsAndFrozenTargets() throws Exception {
        String planner = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertFalse(planner.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(planner.contains("duration=prefs.activeReviewMinutes(geometry);"));
        assertTrue(planner.contains("targetMinutes(date,am,duration)"));
        assertTrue(planner.contains("targetMinutes(date,pm,duration)"));
        assertFalse(planner.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }

    @Test public void mainActivityTodayLabelUsesFrozenOrFixedActiveDuration() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse(main.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue("Home must show a short name, never a false Sabqi 1/3 ordinal",
            main.contains("setToday(prefix+todaySummary(mode),true,R.string.today_continue,overdue);")
            && main.contains("if(HifzSessionActivity.SABQI.equals(mode))return "));
        assertFalse("No misleading Sabqi fraction on Home",
            main.contains("WeeklyDashboardPlanner.learningSessionCue("));
        assertTrue("The actual dynamic Révision duration is still implemented by the session",
            read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")
                .contains("MaintenanceCoveragePolicy.minutes("));
        assertTrue(main.contains("new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());"));
        assertTrue(main.contains("reviews.targetMinutes(today,slot,prefs.activeReviewMinutes(geometry))"));
        assertFalse(main.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }

    @Test public void revisionSelectorKeepsLegacyPassiveDurationAndUsesFixedActiveBands() throws Exception {
        String selector = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/RevisionSelector.java");
        assertTrue("Passive menu duration uses the acquired corpus",
            selector.contains("MaintenanceCoveragePolicy.minutes(")
                && selector.contains("prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine()"));
        assertTrue("Active slots retain their own computed targets",
            selector.contains("store.targetMinutes(today, slot, geometry == null ? ActiveReviewSlots.LEGACY_MINUTES : prefs.activeReviewMinutes(geometry))")
                && selector.contains("\"Révision passive · \" + maintenanceCue"));
        assertFalse("The historical 15-minute label must never return to the menu",
            selector.contains("Révision active · 15 min"));
    }

    @Test public void settingsShowFixedBandsAndConfirmedCoverage() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertFalse(settings.contains("HifzSchedule.MAINTENANCE_MINUTES"));
        assertTrue(settings.contains("int minutes=prefs.activeReviewMinutes(geometry);"));
        assertTrue(settings.contains("0–15 juz · 40 + 5 min"));
        assertTrue(settings.contains(">15–25 juz · 60 + 10 min"));
        assertTrue(settings.contains(">25–30 juz · 70 + 15 min"));
        assertTrue(settings.contains("prefs.activeCoverageSummary()"));
        assertFalse(settings.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
    }
}
