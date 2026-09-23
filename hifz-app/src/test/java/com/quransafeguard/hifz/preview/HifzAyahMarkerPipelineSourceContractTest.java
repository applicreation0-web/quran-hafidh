package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Writing-exercise ayah-end markers: a build-time Python step reads the real
 * ayah:x/ayah:y attributes already embedded in the bundled Mushaf glyph SVGs (the
 * exact verse-end rosette positions, already in geometry.json's page-space
 * coordinates), synced into hifz-app's assets, and loaded lazily at runtime by
 * AyahMarkerRepository — so the exercise canvas can keep showing the real
 * verse-end signs the user is used to seeing, never an approximated position.
 */
public final class HifzAyahMarkerPipelineSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void buildWiresTheGeneratorIntoPrepareHifzAssets() throws Exception {
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("the generator must be its own Exec task, not inlined ad hoc",
            gradle.contains("val generateHifzAyahMarkers by tasks.registering(Exec::class)"));
        assertTrue("it must invoke the real generator script, not a stand-in",
            gradle.contains("scripts/generate_hifz_ayah_markers.py"));
        assertTrue("prepareHifzAssets must depend on it so the asset is never stale",
            gradle.contains("dependsOn(prepareHifzTafsirRelease, generateHifzWordShapes, generateHifzAyahMarkers)"));
        assertTrue("the generated markers must be synced into the app's own assets under ayahmarkers/",
            gradle.contains("from(generatedHifzAyahMarkersDir) { into(\"ayahmarkers\") }"));
    }

    @Test public void generatorReadsTheRealEmbeddedMarkerPositions() throws Exception {
        String script = read("scripts/generate_hifz_ayah_markers.py");
        assertTrue("must read the real bundled KFQC glyph SVGs, never a separate copy",
            script.contains("svg_br_dir") && script.contains(".svg.br"));
        assertTrue("must read the real ayah:x/ayah:y attributes, never invent or approximate positions",
            script.contains("ayah:x=") && script.contains("ayah:y="));
        // Audited across the full 604-page corpus: exactly one marker per verse, matching the
        // corpus's total verse count exactly, with no page missing every one of its verse ends.
        assertTrue("must keep the audited full-corpus marker-count sanity check",
            script.contains("EXPECTED_TOTAL_MARKERS = 6236"));
    }

    @Test public void repositoryLoadsTheGeneratedAssetLazilyPerPage() throws Exception {
        String repo = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AyahMarkerRepository.java");
        assertTrue("must read the generator's own output path convention",
            repo.contains("\"ayahmarkers/%03d.json\""));
        assertTrue("must cache per page rather than eagerly loading all 604 pages into memory",
            repo.contains("Map<Integer, float[][]> cache"));
        assertTrue("must expose the per-page marker positions for the writing exercise to draw",
            repo.contains("markersForPage(int page)"));
    }
}
