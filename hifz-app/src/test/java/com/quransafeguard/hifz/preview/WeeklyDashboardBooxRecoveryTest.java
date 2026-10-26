package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

/** BOOX screenshot regression: incomplete finals must not erase seven-day navigation. */
public final class WeeklyDashboardBooxRecoveryTest {
    @Test public void sevenDatesRemainAvailableEvenWhenForecastCannotBeBuilt() {
        LocalDate today=LocalDate.of(2026,10,26);
        List<WeeklyDashboardPlanner.Row> rows=WeeklyDashboardPlanner.fallbackWeek(today);
        assertEquals(7,rows.size());
        assertEquals(today,rows.get(0).date);
        assertEquals(LocalDate.of(2026,11,1),rows.get(6).date);
        for(int i=0;i<7;i++){
            assertEquals(today.plusDays(i),rows.get(i).date);
            assertTrue(rows.get(i).morning.contains("à vérifier"));
            assertTrue(rows.get(i).evening.contains("à vérifier"));
            assertFalse("Fail closed: no invented completed lessons",
                rows.get(i).state.contains("Validé"));
        }
    }
    @Test public void mainScreenNoLongerUsesWeekHeadingForProgrammeChanges() throws Exception{
        Path source=Path.of("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String main=Files.readString(source,StandardCharsets.UTF_8);
        assertFalse("Heading must open calendar rather than programme picker",
            main.contains("dashboardTitle.setOnClickListener(v -> chooseWeeklyProgram())"));
        assertTrue(main.contains("Voir le calendrier sur sept jours"));
        assertTrue(main.contains("WeeklyDashboardPlanner.window(HifzClock.today())"));
    }
    @Test public void bothFamiliesOfferedSeparatelyAfterSuccessfulFinalCredit() throws Exception {
        String session=Files.readString(Path.of(
            "src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"),
            StandardCharsets.UTF_8);
        assertTrue(session.contains("promptPostFinalRecall(PostFinalRecallStore.ITQAN)"));
        assertTrue(session.contains("promptPostFinalRecall(PostFinalRecallStore.SABQI)"));
        assertTrue(session.contains("EXTRA_POST_FINAL_FAMILY"));
        String main=Files.readString(Path.of(
            "src/main/java/com/quransafeguard/hifz/preview/MainActivity.java"),
            StandardCharsets.UTF_8);
        assertTrue(main.contains("postFinalStore.nextReadyFor(family"));
        assertTrue(main.contains("session.postFinalFamily"));
    }
}
