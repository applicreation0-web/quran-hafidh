package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** Wiring contract: never equate a snapshot match or an empty queue with "Terminé". */
public final class EveningReviewDisplayWiring11711Test {
    private static String source(String name) throws Exception {
        Path p=Path.of("src/main/java/com/quransafeguard/hifz/preview/",name);
        if(!Files.exists(p))p=Path.of("hifz-app/src/main/java/com/quransafeguard/hifz/preview/",name);
        return Files.readString(p,StandardCharsets.UTF_8);
    }

    @Test public void weeklyCalendarRequiresDatedReceiptsForBothFamilies() throws Exception {
        String planner=source("WeeklyDashboardPlanner.java");
        assertTrue(planner.contains("EveningReviewDisplay.forDay("));
        assertTrue(planner.contains("lessonDone,snowballActual!=null,eveningDue"));
        assertTrue(planner.contains("morningDone,snowballActual!=null,eveningDue"));
        assertFalse(planner.contains("&& !prefs.snowballEveningPending("));
        assertTrue(planner.contains("snowball.endsWith(\"aucune nouvelle unité\")"));
    }

    @Test public void todayWindowNoLongerConvertsNotPendingIntoCompleted() throws Exception {
        String main=source("MainActivity.java");
        assertTrue(main.contains("modeComplete(today,HifzSessionActivity.LEARNING_CONSOLIDATION)"));
        assertTrue(main.contains("modeComplete(today,HifzSessionActivity.RECENT_SABQI_REVIEW)"));
        assertTrue(main.contains("learningEvening==EveningReviewDisplay.Status.COMPLETED,nextMode"));
        assertTrue(main.contains("stabilizationEvening==EveningReviewDisplay.Status.COMPLETED,nextMode"));
        assertFalse(main.contains("today,!prefs.snowballEveningPending("));
        assertTrue(main.contains("session.label.contains(\"en attente\") && !session.done"));
    }
}
