package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Palier 2 (writing-exercise coverage check) production pipeline: a build-time
 * Python step turns the real, already-bundled Mushaf glyph paths into a compact
 * per-page word-shape asset, synced into hifz-app's assets, and loaded lazily
 * at runtime by WordShapeRepository. Never the Python-prototype's throwaway output.
 */
public final class HifzWordShapePipelineSourceContractTest {
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
            gradle.contains("val generateHifzWordShapes by tasks.registering(Exec::class)"));
        assertTrue("it must invoke the real generator script, not a stand-in",
            gradle.contains("scripts/generate_hifz_word_shapes.py"));
        assertTrue("prepareHifzAssets must depend on it so the asset is never stale",
            gradle.contains("dependsOn(prepareHifzTafsirRelease, generateHifzWordShapes, generateHifzAyahMarkers)"));
        assertTrue("the generated shapes must be synced into the app's own assets under wordshapes/",
            gradle.contains("from(generatedHifzWordShapesDir) { into(\"wordshapes\") }"));
    }

    @Test public void generatorKeepsTheAuditedToleranceAndRealSourceData() throws Exception {
        String script = read("scripts/generate_hifz_word_shapes.py");
        assertTrue("must read the real bundled KFQC glyph SVGs, never a separate copy",
            script.contains("svg_br_dir") && script.contains(".svg.br"));
        assertTrue("must reuse the real reader109 geometry (line/cell boundaries), never invent its own",
            script.contains("geometry_json"));
        // A stricter per-subpath bounding-box tolerance was tried and rejected: audited across
        // all 604 pages it more than doubled the empty-cell rate (0.483% -> 2.163%), because
        // Arabic ascenders/descenders legitimately extend past a cell's nominal line band.
        assertTrue("must keep the audited generous tolerance, not the rejected stricter one",
            script.contains("Y_TOLERANCE = 1.0") && script.contains("X_TOLERANCE = 2.0"));
        assertTrue("must warn at build time if the empty-cell rate regresses past the audited ceiling",
            script.contains("2% sanity ceiling"));
    }

    @Test public void repositoryLoadsTheGeneratedAssetLazilyPerPage() throws Exception {
        String repo = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WordShapeRepository.java");
        assertTrue("must read the generator's own output path convention",
            repo.contains("\"wordshapes/%03d.json\""));
        assertTrue("must cache per page rather than eagerly loading all 604 pages into memory",
            repo.contains("Map<Integer, float[][][][]> cache"));
        assertTrue("must expose a per-cell shape accessor for the writing exercise to score against",
            repo.contains("public float[][] cellShape(int page, int lineIndex, int cellIndex)"));
    }
}
