package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

/**
 * Every literal icon-only action on every production screen must map to a
 * real drawable; no silent fallback to an improvised unicode glyph.
 * Hifz engines and Quiz production source remain byte-identical to 1.17.1.
 */
public final class EveryScreenContractualIconTest {
    private static final String ROOT =
        "hifz-app/src/main/java/com/quransafeguard/hifz/preview";
    private static final Pattern STATIC_ACTION = Pattern.compile(
        "Ui\\.(?:iconButton|roundAction)\\s*\\(" +
        "[^,]*,\\s*\\\"[^\\\"]*\\\",\\s*\\\"([^\\\"]+)\\\""
    );

    private static Path repo(String relative) {
        Path root = Paths.get(relative);
        return Files.exists(root) ? root : Paths.get("..", relative);
    }

    @Test public void noLabelledIconOnlyActionFallsBackToGlyph() throws Exception {
        Path dir = repo(ROOT);
        String[] screens = {
            "MainActivity", "StudyReaderActivity", "IbnKathirMapActivity",
            "HifzSessionActivity", "QuizActivity", "FreeMemActivity",
            "ProgressMapActivity", "SettingsActivity", "WeakVersesActivity",
            "HifzAudioDialog"
        };
        int matches = 0;
        Set<String> labels = new HashSet<>();
        for (String screen : screens) {
            String java = new String(Files.readAllBytes(dir.resolve(screen + ".java")),
                StandardCharsets.UTF_8);
            Matcher matcher = STATIC_ACTION.matcher(java);
            int local = 0;
            while (matcher.find()) {
                String label = matcher.group(1);
                assertTrue("Fallback glyph forbidden in " + screen + " : " + label,
                    Ui.iconFor(label, "") != 0);
                local++;
                labels.add(label);
            }
            assertTrue("No icon actions parsed in " + screen, local > 0);
            matches += local;
        }
        assertTrue("Unexpected shrinkage of icon role coverage: " + matches, matches >= 50);
        assertTrue("Contractual action roles must be diverse", labels.size() >= 35);
    }

    @Test public void noLabelsPrintedUnderIconButtons() throws Exception {
        String ui = new String(Files.readAllBytes(repo(ROOT + "/Ui.java")),
            StandardCharsets.UTF_8);
        assertTrue(ui.contains("button.setText(\"\");"));
        assertTrue(ui.contains("box.addView(iconButton(context, symbol, label, listener));"));
        assertTrue(ui.contains("int size = dp(context, 48);"));
        assertTrue(ui.contains("button.setContentDescription(value);"));
    }
}
