package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * HifzCore.kt used to carry a second, parallel session-scheduling engine — SessionType,
 * DailyPlan/PlannedSession, ScheduledSession, and HifzSchedule.typeFor/planFor/scheduled — that
 * nothing in hifz-app ever called; the real schedule lives in HifzSchedule.actionFor/nextDue and
 * is driven entirely from MainActivity/HifzPrefs. Its own hifz-core unit tests exercised it
 * (naming one "...ForCompatibility"), which let it pass CI while staying unreachable from the
 * built APK. This locks in that it's gone, not just unused, and that the pieces still actually
 * driving the schedule (SessionKind, CadenceAction, ScheduledCadence, actionFor, nextDue,
 * targetMinutesFor) remain.
 */
public final class HifzCoreDeadSchedulingEngineRemovedSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void deadSchedulingEngineIsGoneAndTheRealScheduleRemains() throws Exception {
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertFalse("SessionType must be gone, not just unreferenced", core.contains("enum class SessionType"));
        assertFalse("PlannedSession must be gone", core.contains("data class PlannedSession"));
        assertFalse("DailyPlan must be gone", core.contains("data class DailyPlan"));
        assertFalse("ScheduledSession must be gone", core.contains("data class ScheduledSession"));
        assertFalse("typeFor must be gone", core.contains("fun typeFor("));
        assertFalse("planFor must be gone", core.contains("fun planFor("));
        assertFalse("the dead scheduled() must be gone (distinct from the still-live ScheduledCadence)",
            core.contains("fun scheduled("));

        assertTrue("the schedule actually driving MainActivity must remain",
            core.contains("fun actionFor(day: DayOfWeek, learningDaysPerWeek: Int = DEFAULT_LEARNING_DAYS_PER_WEEK): CadenceAction"));
        assertTrue("the soft carry-over due-detection must remain", core.contains("fun nextDue("));
        assertTrue("ScheduledCadence (the type nextDue actually returns) must remain",
            core.contains("data class ScheduledCadence("));
        assertTrue("SessionKind must remain (still used by targetMinutesFor)", core.contains("enum class SessionKind"));

        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse("no dangling import of a type that no longer exists",
            main.contains("import com.quransafeguard.hifz.core.DailyPlan;"));
    }
}
