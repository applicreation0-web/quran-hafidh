package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: the home screen's "Révision" quick-access card always showed a static
 * "30 min" cue — a leftover from before the daily Révision active/passive split, wrong on both
 * counts now (15 min active, 45 min passive). Ui.modeCard can only derive a static cue from the
 * card's label ("Révision" never changes), so MainActivity — which already knows exactly which of
 * the two murajaahQuickAccessMode() will actually open — must overwrite that cue itself, and keep
 * it in sync on every onResume via refreshAll(), since the due submode can change within the same
 * app session (e.g. right after finishing today's active recall).
 */
public final class HomeMurajaahQuickAccessCueSourceContractTest {
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

    @Test public void homeCardTracksWhicheverMurajaahSubmodeWillActuallyOpen() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");

        assertTrue("the card must be kept as a field so its cue can be updated after creation",
            main.contains("private LinearLayout murajaahQuickAccess;"));
        assertTrue("the field must actually be wired to the card built for the Révision quick access",
            main.contains("murajaahQuickAccess = murajaah;"));
        assertTrue("the cue must be refreshed on every onResume, since the due submode can change "
                + "within the same app session",
            main.contains("private void refreshAll() { refreshQuickAccessCadenceGating(); "
                + "refreshMurajaahQuickAccessCue(); refreshToday(); refreshRecentSabqiAdvisory(); refreshDashboard(); }"));

        String cue = method(main,
            "private void refreshMurajaahQuickAccessCue() {", "\n    }");
        assertTrue("must ask murajaahQuickAccessMode() which submode will actually open, not guess",
            cue.contains("HifzSessionActivity.MURAJAAH_ACTIVE.equals(murajaahQuickAccessMode())"));
        assertTrue("active due must show the real active-review target, not a hardcoded number",
            cue.contains("activeNext ? SessionKind.ACTIVE_MURAJAAH : SessionKind.OLD_ITQAN_MURAJAAH"));
        assertTrue("must actually source the minutes from HifzSchedule, not repeat a stale literal",
            cue.contains("HifzSchedule.INSTANCE.targetMinutesFor("));
        assertTrue("no leftover hardcoded pre-split minute literal may remain in this method",
            !cue.contains("30 min") && !cue.contains("\"30\""));
    }
}
