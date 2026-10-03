package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks in the generated waqf.json asset (see scripts/build_waqf_marks.py). Reads it as raw text
 * rather than through org.json, matching this project's JVM-test convention of avoiding the
 * Android org.json stubs. The لا/mamnu' mark is deliberately absent from the source data — see
 * the script's docstring — so this asserts its continued absence rather than its presence.
 */
public final class WaqfMarksAssetTest {
    @Test public void assetExistsWithExpectedSchemaAndCoverage() throws Exception {
        String json = readAsset();
        assertTrue("asset must declare its schema", json.contains("\"schema\":1"));
        assertTrue("asset must record its source", json.contains("\"source\":\"tanzil-uthmani\""));
        assertTrue("marks map must be present", json.contains("\"marks\":{"));
    }

    /** Cross-checked against the real Mushaf page examined this session (Al-Hujurat, page 516). */
    @Test public void alHujuratNinePreservesItsThreeVerifiedMarks() throws Exception {
        String json = readAsset();
        assertTrue("49:9 must carry its sili mark after word 6 (بينهما)",
            json.contains("\"49:9\":[{\"afterWord\":6,\"type\":\"sili\"}"));
        assertTrue("49:9 must carry its jaiz mark after word 19",
            json.contains("{\"afterWord\":19,\"type\":\"jaiz\"}"));
        assertTrue("49:7 must carry its jaiz mark after word 4 (رسول الله)",
            json.contains("\"49:7\":[{\"afterWord\":4,\"type\":\"jaiz\"}"));
    }

    @Test public void mamnuMarkIsKnownAbsentFromThisSource() throws Exception {
        String json = readAsset();
        assertFalse("the لا/mamnu' mark is not present in this source (documented gap)",
            json.contains("\"type\":\"mamnu\""));
    }

    private static String readAsset() throws Exception {
        String relative = "app/src/main/assets/reader109/waqf.json";
        Path direct = Paths.get(relative);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", relative);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing waqf.json asset");
    }
}
