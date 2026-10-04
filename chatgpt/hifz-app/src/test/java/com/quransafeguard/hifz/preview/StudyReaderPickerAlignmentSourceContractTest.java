package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: bumping surahPicker's font size above rubPicker's to visually match its
 * perceived weight (Arabic script vs. plain Latin digits at the same nominal size) broke their
 * baseline alignment, since pageRail centers each child by its own padding+line-height box.
 */
public final class StudyReaderPickerAlignmentSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void surahAndHizbPickersShareTheExactSameFontSizeSoTheirBaselinesAlign() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue("pageRail centers each child by its own bounding box, so a size mismatch between "
                + "the two pickers shifts their text baselines off the shared row line even though the "
                + "row itself looks centered",
            study.contains("surahPicker = Ui.bookText(this, \"Sourate\", 13f, true);")
                && study.contains("rubPicker = Ui.bookText(this, \"\", 13f, true);"));
    }
}
