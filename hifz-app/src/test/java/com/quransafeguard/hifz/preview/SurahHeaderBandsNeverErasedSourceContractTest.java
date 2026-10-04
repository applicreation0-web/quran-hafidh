package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Device report (page 515, Stabilisation at 50%): the basmala of Al-Ḥujurāt was erased because
 * the KFQC geometry folds the basmala band into the first text line (and the surah title into the
 * last line above it). Double-height lines are narrowed to their own words' ink before masking,
 * and the full-erase seam sealing never re-extends across that band.
 */
public final class SurahHeaderBandsNeverErasedSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void doubleHeightLinesAreNarrowedToTheirOwnInkBeforeMasking() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(reader.contains("function computeLineInkBands(lines,svg){"));
        assertTrue(reader.contains("if(!(bottom-top>typical*1.5))return;"));
        assertTrue(reader.contains("lineInkBands=clamped&&lines.length?computeLineInkBands(lines,svg):new Map();"));
        assertTrue(reader.contains("const top=band?band.top:Number(line.top),bottom=band?band.bottom:Number(line.bottom);"));
        assertTrue(reader.contains("if(prev&&!band.clampedTop&&!prevBand.clampedBottom&&"));
        assertTrue(reader.contains("if(next&&!band.clampedBottom&&!nextBand.clampedTop&&"));
    }
}
