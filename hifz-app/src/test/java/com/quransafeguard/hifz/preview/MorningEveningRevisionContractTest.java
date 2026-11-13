package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Both active slots appear in the daily and weekly schedules, with Sunday finals preserved. */
public final class MorningEveningRevisionContractTest {
    private String source(String name) throws Exception {
        String dir="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";
        Path p=Paths.get(dir+name);
        if(!Files.exists(p))p=Paths.get("..",dir+name);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void weeklyPlannerSeparatesMorningAndEveningAndPreservesSunday() throws Exception {
        String plan=source("WeeklyDashboardPlanner.java");
        assertTrue(plan.contains("morning=joinSessions(activeMorning,passive);"));
        assertTrue(plan.contains("evening=joinSessions(evening,activeEvening);"));
        assertTrue(plan.contains("activeReviewLabel(am,morningDone,"));
        assertTrue(plan.contains("Révision passive · "));
        assertTrue(plan.contains("if(date.getDayOfWeek()!=java.time.DayOfWeek.SUNDAY)"));
        assertTrue(plan.contains("morning=joinSessions(activeMorning,morning);"));
        assertFalse("No obsolete user-facing Entretien label",
            plan.contains("\"Entretien ·"));
    }

    @Test public void todayActionsOfferMorningReviewsBeforeEveningLearning() throws Exception {
        String main=source("MainActivity.java");
        int begin=main.indexOf("private List<TodaySession> todaySessions(");
        int active=main.indexOf("addActiveReviewToday(out,today,ActiveReviewSlots.Slot.MORNING",begin);
        int passive=main.indexOf("addToday(out,\"Révision passive · matin",begin);
        int lesson=main.indexOf("case LEARNING:",begin);
        assertTrue(begin>=0&&active>begin&&passive>active&&lesson>passive);
        assertTrue(main.contains("eveningLessonDetail=todayRow!=null?stripDone(compactSession(todayRow.evening))"));
        assertTrue(main.indexOf("addActiveReviewToday(out,today,ActiveReviewSlots.Slot.EVENING",lesson)>lesson);
        assertTrue(main.contains("Itqān · Maintien de la maîtrise"));
        assertTrue(main.contains("Révision passive · matin"));
    }

    @Test public void lateDisplayFixesArePresentAndSundayItqanIsNotTheFinale() throws Exception {
        String main=source("MainActivity.java");
        String planner=source("WeeklyDashboardPlanner.java");
        String session=source("HifzSessionActivity.java");
        String vocabulary=source("HifzDisplayVocabulary.java");
        assertFalse("Sunday must never offer ordinary Itqan",
            main.contains("addToday(out,anchoringTodayDetail(prefs,geometry),HifzSessionActivity.ITQAN,"));
        assertTrue("Passive forecast must suppress its repeated duration",
            planner.contains("if(range.endsWith(trailing))"));
        assertTrue(session.contains("Validez d’abord les 5 lignes d’Apprentissage du soir."));
        assertTrue(session.contains(" · répétition d’un hizb · "));
        assertTrue(vocabulary.contains(".replace(\"Entretien\", \"Révision passive\")"));
        assertFalse("An old post-Nas user-facing label must be absent",
            session.contains(" · entretien 1 hizb · "));
    }

    @Test public void directShortcutOffersExplicitActiveSlot() throws Exception {
        String main=source("MainActivity.java");
        assertFalse(main.contains("Dimanche sans révision"));
        assertTrue(main.contains("openActiveRevision(ActiveReviewSlots.Slot slot) { openMode(slot.mode); }"));
        assertTrue(main.contains("HifzSessionActivity.MURAJAAH.equals(mode)"));
    }

    @Test public void displayUsesOnlyRevisionPassiveTerminology() throws Exception {
        String settings=source("SettingsActivity.java");
        String selector=source("RevisionSelector.java");
        assertTrue(settings.contains("min matin et soir"));
        assertTrue(settings.contains("Vitesse · Révision passive"));
        assertTrue(settings.contains("Position Révision passive : "));
        assertTrue(settings.contains("dimanche compris. Révision passive facultative"));
        assertTrue(selector.contains("\"Révision passive · \" + maintenanceCue"));
        assertTrue(settings.contains("Bascule automatique ; séances déjà ouvertes préservées."));
        assertFalse(settings.contains("Vitesse · Entretien"));
        assertFalse(selector.contains("Entretien"));
    }
}
