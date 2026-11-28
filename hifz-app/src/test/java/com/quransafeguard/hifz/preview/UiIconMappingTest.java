package com.quransafeguard.hifz.preview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/** There is no locale-dependent lookup between a compact action and its drawable. */
public final class UiIconMappingTest {
    private static final Pattern LABEL_ROUTED_ACTION = Pattern.compile(
        "(?:Ui\\.(?:iconButton|roundAction)\\s*\\([^,]+,|addRoundAction\\s*\\()\\s*\"");

    @Test public void compactActionsNeverSelectArtworkFromTranslatedStrings() throws Exception {
        Path directory = Path.of("src/main/java/com/quransafeguard/hifz/preview");
        if (!Files.isDirectory(directory)) directory = Path.of("hifz-app").resolve(directory);
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse("Action with a string icon argument in " + file,
                    LABEL_ROUTED_ACTION.matcher(source).find());
                assertFalse("Localized icon lookup in " + file, source.contains("iconFor("));
            }
        }
    }
}
