package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Guard against regressing to the legacy queue (the Qāf 6/8 -> 10/13 October bug). */
public final class WeeklyItqanCursorWiringSourceContractTest {
    private static String source(String path) throws Exception {
        Path direct = Paths.get(path);
        if (!Files.exists(direct)) direct = Paths.get("..", path);
        return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
    }

    @Test public void weeklyPlannerUsesLiveRotationAndDoesNotRestartLegacyQueue() throws Exception {
        String planner = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(planner.contains("ItqanWeeklyForecast itqanForecast=new ItqanWeeklyForecast("));
        assertTrue(planner.contains("prefs.itqanRotationState(),currentItqan,prefs.itqanBlockIndex()"));
        assertTrue(planner.contains("prefs.cachedUnitsInLeg(leg,sabqiFrontier,geometry,"));
        assertTrue(planner.contains("itqanForecast.completedBlock(blocks);"));
        assertTrue("Never restart the forecast at legacy queue index zero",
            !planner.contains("ItqanRotationPolicy.projectedOrder(")
                && !planner.contains("projectedAnchoringIndex=0")
                && !planner.contains("prefs.anchoringQueue()"));
    }

    @Test public void displayOnlyForecastCannotPersistOrCreditProgress() throws Exception {
        String forecast = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanWeeklyForecast.java");
        assertTrue(forecast.contains("ItqanRotationPolicy.pick(state,"));
        assertTrue(forecast.contains("ItqanRotationPolicy.advancedPast(state, end)"));
        assertTrue(!forecast.contains("SharedPreferences"));
        assertTrue(!forecast.contains(".commit()"));
        assertTrue(!forecast.contains("completeStabilizationBlockV6"));
        assertTrue(!forecast.contains("saveItqanRotationState"));
    }
}
