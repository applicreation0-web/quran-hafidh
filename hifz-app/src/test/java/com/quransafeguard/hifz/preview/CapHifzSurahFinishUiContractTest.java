package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Fail closed if BOOX reverts to only 2 Hizb dots or drops the acquisition
 * track when projected Itqan transitions to perpetual Entretien. */
public final class CapHifzSurahFinishUiContractTest {
    private static String java(String name) throws Exception {
        Path p = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/" + name + ".java");
        if (!Files.exists(p)) p = Paths.get("..").resolve(p);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test public void allExactSurahEndingsReachYearViewWithoutTwoMarkerTruncation() throws Exception {
        String forecast = java("CapHifzForecast");
        String panel = java("CapHifzPanel");
        String view = java("CapHifzTimelineView");
        assertTrue(forecast.contains("surahEndBoundaries(geometry, 2, 48)"));
        assertTrue(forecast.contains("surahEndBoundaries(geometry, 49, 114)"));
        assertTrue(panel.contains("row.sabqiSurahFinishes"));
        assertTrue(panel.contains("row.tailSurahFinishes"));
        assertTrue(view.contains("yearSurahFinishes(sabqi)"));
        assertTrue(view.contains("for (int i = 0; i < annual.size(); i++)"));
        assertTrue(view.contains("pinX(i, annual.size()"));
        assertTrue(view.contains("selectedVerse.equals(point.verse)"));
        assertTrue(panel.contains("timeline.setSelection(choice.date, choice.sabqi, choice.verse)"));
    }

    @Test public void selectedCheckpointSaysWhichRouteAndOpensExactLastAyah() throws Exception {
        String panel = java("CapHifzPanel");
        assertTrue(panel.contains("choice.sabqi ? \"Acquisition\" : \"Maîtrise\""));
        assertTrue(panel.contains("QuranSurahNames.name(choice.verse.getSurah())"));
        assertTrue(panel.contains("choice.verse.getAyah()"));
        assertTrue(panel.contains("choice.date.format(DAY_MONTH)"));
        assertTrue(panel.contains("openMushaf(shownCheckpoints.get(checkpointIndex).verse)"));
        assertTrue(panel.contains("labelNavigator(previousYear, \"«\", \"ANNÉE\")"));
        assertTrue(panel.contains("labelNavigator(previous, \"‹\", \"JALON\")"));
        assertTrue(panel.contains("labelNavigator(next, \"›\", \"JALON\")"));
        assertTrue(panel.contains("labelNavigator(nextYear, \"»\", \"ANNÉE\")"));
    }

    @Test public void fullHorizonAndPostNasRemainReadOnly() throws Exception {
        String panel = java("CapHifzPanel");
        String forecast = java("CapHifzForecast");
        assertTrue(panel.contains("CapHifzForecast.projectedTailFinishedBeforeYear(quarters, year)"));
        assertTrue(panel.contains("boolean showMastery = tailForecast && !projectedPostNas;"));
        assertTrue(panel.contains("boolean showEntretien = postNas || projectedPostNas;"));
        assertTrue(panel.contains("forecastSabqi || tailForecast || postNas"));
        assertTrue(panel.contains("PROJECTION_ATTENDANCE_PERCENT = 90"));
        assertFalse(forecast.contains("prefs.edit()"));
        assertFalse(panel.contains("completeDailyLesson("));
    }
}
