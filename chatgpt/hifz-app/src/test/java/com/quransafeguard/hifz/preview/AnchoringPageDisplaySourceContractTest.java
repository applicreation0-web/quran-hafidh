package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** A page is visual support, never a pedagogical boundary for Ancrage. */
public final class AnchoringPageDisplaySourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void midPageBoundaryKeepsWholePageButTargetsOnlyEligibleVersesAndLines() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");

        assertTrue(geometry.contains("int page = lines.get(firstIndex).page"));
        assertTrue(geometry.contains("ordinal(ref) >= cursorOrdinal && corpus.contains(ref)"));
        assertTrue(geometry.contains("return new VerseUnit(page, cursor"));

        assertTrue(mushaf.contains("String svg = readPageSvg(page)"));
        assertTrue(mushaf.contains("replace(SVG_SLOT, svg)"));
        assertTrue(mushaf.contains(".put(\"selection\", verses)"));
        assertTrue(mushaf.contains(".put(\"lines\", lines)"));
        assertTrue(mushaf.contains(".put(\"mask\", Math.max(0, Math.min(100, maskPercent)))"));
    }
}
