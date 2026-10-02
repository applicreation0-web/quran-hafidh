package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly: the home screen's single "Révision" quick-access card showed one duration
 * cue (first a hardcoded "30 min", later a dynamic number picking whichever of the daily
 * active/passive pair was due next) — but tapping the card always opens a choice between BOTH
 * Révision active (20 min) and Entretien (its own dynamic duration), so any single number shown
 * on the outer card can only ever match one of the two and misleads about the other.
 *
 * Reported again after the fix: dropping the cue text entirely left this card with only two
 * lines (icon+caption) while its "Apprentissage"/"Stabilisation" siblings in the same row still
 * have three (icon+caption+cue) — an empty cue slot doesn't leave a blank gap under the shorter
 * card, it recentres the whole row's vertical alignment around it, visibly breaking the row. The
 * card now shows "Au choix" instead: honest (it genuinely is a choice, not a fixed duration) and
 * keeps the three-line height so the row stays aligned. RevisionSelector's dialog remains the only
 * place an actual duration is shown, stating both correctly, side by side, once tapped.
 */
public final class HomeMurajaahQuickAccessCueSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void modeCardGivesRevisionAHonestNonDurationCueNotAnEmptyOne() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue("Révision/Entretien must still match their own branch of the cue ternary",
            ui.contains("lower.contains(\"révision\") || lower.contains(\"revision\") "
                + "|| lower.contains(\"entretien\") || lower.contains(\"mur\")"));
        assertTrue("the cue must be a non-empty, non-numeric placeholder — never a specific duration "
                + "that would only match one of the two révisions this card can open",
            ui.contains("? \"Au choix\" : \"\";"));
    }

    @Test public void homeScreenCarriesNoLeftoverMurajaahCueMachinery() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        // Not a plain contains("murajaahQuickAccess"): murajaahQuickAccessMode() is a distinct,
        // still-needed method (picks which of the two is due next for the dashboard/selector) and
        // would otherwise false-fail this assertion by substring overlap.
        assertFalse("the card's duration cue field was removed",
            main.contains("murajaahQuickAccess;") || main.contains("murajaahQuickAccess ="));
        assertFalse("the per-submode cue refresh no longer has a cue to update",
            main.contains("refreshMurajaahQuickAccessCue"));
    }

    @Test public void revisionSelectorDialogStillStatesBothDurationsCorrectly() throws Exception {
        String selector = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/RevisionSelector.java");
        assertTrue("Révision active's own fixed duration must still be shown once the dialog opens",
            selector.contains("HifzSchedule.ACTIVE_REVIEW_MINUTES"));
        assertTrue("Entretien must use the product-frozen 30-minute core duration",
            selector.contains("HifzSchedule.MAINTENANCE_MINUTES"));
    }
}
