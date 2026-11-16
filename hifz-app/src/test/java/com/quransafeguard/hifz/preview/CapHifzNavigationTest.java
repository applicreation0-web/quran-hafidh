package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.IntUnaryOperator;

import static org.junit.Assert.*;

/** Read-only regression cases for the BOOX Cap Hifdh forecast navigator. */
public final class CapHifzNavigationTest {
    @Test public void singleChevronAdvancesEveryRealCheckpointAndSkipsEmptyYears() {
        IntUnaryOperator counts = y -> y == 2026 ? 2 : y == 2028 ? 3 : 0;
        CapHifzNavigation.Cursor first = new CapHifzNavigation.Cursor(2026, 0);
        CapHifzNavigation.Cursor second = CapHifzNavigation.move(
            first.year, first.index, 1, 2026, 2031, counts);
        assertEquals(2026, second.year);
        assertEquals(1, second.index);
        CapHifzNavigation.Cursor third = CapHifzNavigation.move(
            second.year, second.index, 1, 2026, 2031, counts);
        assertEquals("Skip 2027 without inventing a checkpoint", 2028, third.year);
        assertEquals(0, third.index);
        CapHifzNavigation.Cursor back = CapHifzNavigation.move(
            third.year, third.index, -1, 2026, 2031, counts);
        assertEquals(2026, back.year);
        assertEquals(1, back.index);
        assertFalse(CapHifzNavigation.canMove(new CapHifzNavigation.Cursor(2028, 2),
            1, 2026, 2031, counts));
        assertFalse(CapHifzNavigation.canMove(first, -1, 2026, 2031, counts));
    }

    @Test public void noForecastMeansNoPhantomStepAndNeverChangesYear() {
        IntUnaryOperator none = year -> 0;
        for (int direction : new int[]{-1, 1}) {
            CapHifzNavigation.Cursor same = CapHifzNavigation.move(
                2031, 0, direction, 2026, 2031, none);
            assertEquals(2031, same.year);
            assertEquals(0, same.index);
        }
        assertFalse(CapHifzNavigation.canMove(
            new CapHifzNavigation.Cursor(2031, 0), 1, 2026, 2031, none));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedYearNavigation() {
        CapHifzNavigation.move(2025, 0, -1, 2026, 2031, y -> 3);
    }

    @Test public void panelExposesBothYearAndCheckpointControlsWithoutExtraRow() throws Exception {
        Path path = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzPanel.java");
        if (!Files.exists(path)) path = Paths.get("..").resolve(path);
        String panel = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(panel.contains("button(navigator, \"«\", v -> moveForecastYear(-1))"));
        assertTrue(panel.contains("button(navigator, \"»\", v -> moveForecastYear(1))"));
        assertTrue(panel.contains("moveForecastStep(-1)"));
        assertTrue(panel.contains("moveForecastStep(1)"));
        assertTrue(panel.contains("CapHifzNavigation.canMove(position"));
        assertTrue(panel.contains("shownCheckpoints.get(checkpointIndex).verse"));
        assertTrue(panel.contains("PROJECTION_ATTENDANCE_PERCENT = 90"));
        assertFalse(panel.contains("completeDailyLesson("));
    }
}
