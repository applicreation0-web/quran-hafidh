package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** User contract: 05:30 active/passive; 20:00 learning/stabilisation and snowball. */
public final class MorningEveningRevisionContractTest {
    private String source(String name) throws Exception {
        String dir="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";
        Path p=Paths.get(dir+name);
        if(!Files.exists(p))p=Paths.get("..",dir+name);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void weeklyPlannerSeparatesMorningAndEveningAndPreservesSunday() throws Exception {
        String plan=source("WeeklyDashboardPlanner.java");
        assertTrue(plan.contains("morning=active+\" + \"+passive;"));
        assertTrue(plan.contains("evening=lesson+\" + \"+snowball;"));
        assertTrue(plan.contains("Révision active · "));
        assertTrue(plan.contains("Révision passive · "));
        assertTrue(plan.contains("if(date.getDayOfWeek()!=java.time.DayOfWeek.SUNDAY)"));
        assertTrue(plan.contains("Sunday must NEVER forecast active/passive revision"));
        assertFalse(plan.contains("Entretien"));
    }

    @Test public void todayActionsOfferMorningReviewsBeforeEveningLearning() throws Exception {
        String main=source("MainActivity.java");
        int begin=main.indexOf("private List<TodaySession> todaySessions(");
        int active=main.indexOf("addToday(out,\"Révision active · matin",begin);
        int passive=main.indexOf("addToday(out,\"Révision passive · matin",begin);
        int lesson=main.indexOf("case LEARNING:",begin);
        assertTrue(begin>=0&&active>begin&&passive>active&&lesson>passive);
        assertTrue(main.contains("eveningLessonDetail=todayRow!=null?stripDone(compactSession(todayRow.evening))"));
        assertTrue(main.contains("if(!date.toString().equals(prefs.lastMurajaahDate()))"));
        assertTrue(main.contains("return null; // No active/passive review on Sunday in EITHER programme."));
        assertFalse(main.contains("Entretien"));
    }

    @Test public void displayUsesOnlyRevisionPassiveTerminology() throws Exception {
        String settings=source("SettingsActivity.java");
        String selector=source("RevisionSelector.java");
        assertTrue(settings.contains("Matin · Révision active "));
        assertTrue(settings.contains("Soir : Apprentissage ou Stabilisation"));
        assertTrue(selector.contains("\"Révision passive · \" + maintenanceCue"));
        assertFalse(settings.contains("Entretien"));
        assertFalse(selector.contains("Entretien"));
    }
}
