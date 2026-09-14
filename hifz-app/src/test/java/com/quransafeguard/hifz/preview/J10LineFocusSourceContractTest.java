package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Physical-test contract: J10 must focus the exact 1-5 physical lines, not whole verse polygons. */
public final class J10LineFocusSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void j10UsesExactPhysicalLineFocusAndExplainsMultiPageContinuation() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");

        assertTrue(activity.contains("mushaf.showLineFocus(currentPage, group.verses, group.lineIds)"));
        assertTrue(activity.contains("Le passage continue à la page suivante →"));
        assertTrue(mushaf.contains(".put(\"strictLineFocus\", strictLineFocus)"));
        assertTrue(reader.contains("let strictLineFocus=!!boot.strictLineFocus"));
        assertTrue(reader.contains("linefocuslayer"));
        assertTrue(reader.contains("if(strictLineFocus)return false"));
    }
}
