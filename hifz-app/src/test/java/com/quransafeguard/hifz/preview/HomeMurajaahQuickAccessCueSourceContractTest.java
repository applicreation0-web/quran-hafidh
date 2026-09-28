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
 * Révision active (15 min) and Entretien (its own dynamic duration), so any single number shown
 * on the outer card can only ever match one of the two and misleads about the other. The card now
 * carries no duration cue at all; RevisionSelector's dialog is the only place a duration is shown,
 * and it already states both correctly, side by side, once tapped.
 */
public final class HomeMurajaahQuickAccessCueSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void modeCardNeverAssignsADurationCueToRevisionOrEntretien() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertFalse("a single cue on this card would only ever match one of the two révisions it can open",
            ui.contains("lower.contains(\"révision\")") || ui.contains("lower.contains(\"revision\")")
                || ui.contains("lower.contains(\"entretien\")") || ui.contains("lower.contains(\"mur\")"));
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
            selector.contains("\"Révision active · 15 min · \""));
        assertTrue("Entretien must keep using the real dynamic J-15 duration, not a flat number",
            selector.contains("MaintenanceCoveragePolicy.minutes("));
    }
}
