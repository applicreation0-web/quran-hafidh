package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported: nothing warned before changing learningDaysPerWeek mid-week, even though the weekly
 * cadence (and its boule de neige accumulator) resets every Monday with Sunday as the reserved
 * boundary day. Changing the split mid-week leaves already-passed days classified under the old
 * split while the rest of the same week follows the new one, so a day still due can be reclassified
 * differently once actually caught up (nextDue reclassifies by today's stored setting, not the
 * setting as it was on the day originally scheduled). This can't corrupt data (no history is
 * rewritten), so it's a one-time confirmable warning, not a hard block until Sunday — a full block
 * would trap a learner who simply picked the wrong split and wants to fix it right away.
 */
public final class LearningDaysPerWeekMidWeekGuardSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void changingTheSplitMidWeekAsksForConfirmationFirst() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");

        String choose = method(settings,
            "private void chooseLearningDaysPerWeek(){", "private void warnBeforeMidWeekCadenceChange(int days){");
        assertTrue("picking the same value that's already active must never trigger the warning",
            choose.contains("if(days==current){applyLearningDaysPerWeek(days);return;}"));
        assertTrue("Sunday is the reserved cadence boundary, so a change made there always starts a "
                + "clean week and must apply immediately without a warning",
            choose.contains("if(HifzClock.today().getDayOfWeek()==DayOfWeek.SUNDAY){applyLearningDaysPerWeek(days);return;}"));
        assertTrue("any other day with an actual value change must route through the warning",
            choose.contains("warnBeforeMidWeekCadenceChange(days);"));

        String warn = method(settings,
            "private void warnBeforeMidWeekCadenceChange(int days){", "private void applyLearningDaysPerWeek(int days){");
        assertTrue("the warning must be overridable, not a hard block until Sunday",
            warn.contains(".setPositiveButton(\"Continuer\",(dialog,which)->applyLearningDaysPerWeek(days))"));
        assertTrue("cancelling must leave the stored setting untouched",
            warn.contains(".setNegativeButton(\"Annuler\",null)"));
    }
}
