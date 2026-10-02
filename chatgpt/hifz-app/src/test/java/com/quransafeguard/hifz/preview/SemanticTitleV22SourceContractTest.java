package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression fence: V2.3 may replace French titles only; V2.1 stays authoritative otherwise. */
public final class SemanticTitleV22SourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void runtimeJoinsAuditedV23TitlesOntoFrozenV21Passages() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(source.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(source.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(source.contains("semantic/semantic_titles_v2_3.json"));
        assertTrue(source.contains("46c8c905beaf2e03b2589c296d1574e7417dbab59f9e580911e9ebe0c8073bea"));
        assertTrue(source.contains("19b7a5048ef2201d2a8967652c0f533c4fc46bd1aa66d7b31f12daf196857336"));
        assertTrue(source.contains("parseTitleOverlay"));
        assertTrue(source.contains("EXPECTED_CANONICAL_MUNIR_PASSAGES = 1243"));
        assertTrue(source.contains("tafsir_munir_grouping"));
        assertTrue(source.contains("group.titleMunirAr"));
        assertTrue(source.contains("requiredText(row, \"title_fr_v2_1\")"));
        assertTrue(source.contains("anchor_arabic_v2_1"));
        assertTrue(source.contains("anchor_word_count_v2_1"));
        assertTrue(source.contains("minimality_verified_v2_1"));
        assertFalse("V2.3 title overlay must not supply boundaries",
            source.contains("titleRow.optInt(\"surah_start\"") || source.contains("titleRow.optInt(\"start_line\""));
    }

    @Test public void buildMaterializesBothFrozenV21AndSeparateV23TitleOverlay() throws Exception {
        String build = read("hifz-app/build.gradle.kts");
        String materializer = read("scripts/materialize_semantic_v2_3_titles.py");
        assertTrue(build.contains("prepareSemanticV21"));
        assertTrue(build.contains("prepareSemanticV23Titles"));
        assertTrue(build.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(build.contains("semantic/semantic_titles_v2_3.json"));
        assertTrue(build.contains("src/main/semantic-source/v2_3_titles"));
        assertTrue(materializer.contains("EXPECTED_TITLE_COUNT = 1256"));
        assertTrue(materializer.contains("EXPECTED_FRAGMENTS"));
        assertTrue(materializer.contains("FULL_WASIT_REVIEWED_CONFIRMED"));
        assertTrue(materializer.contains("FULL_WASIT_REVIEWED_CHANGED"));
        assertTrue(materializer.contains("19b7a5048ef2201d2a8967652c0f533c4fc46bd1aa66d7b31f12daf196857336"));
        assertTrue(materializer.contains("46c8c905beaf2e03b2589c296d1574e7417dbab59f9e580911e9ebe0c8073bea"));
        assertTrue(materializer.contains("actual_ids == expected_ids"));
        assertTrue(materializer.contains("output.write_bytes(raw_titles)"));
    }

    @Test public void semanticLayerNeverLeaksIntoSabqiOrItqanAndNoSpatialQuizRemains() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertFalse(session.contains("title_fr_v2_3"));
        assertTrue(session.contains("mushaf.clearSemanticCues()"));
        assertTrue(session.contains("applyActiveRevisionPageCues()"));
        assertFalse(session.contains("activeCuePrompt"));
        assertFalse(manifest.contains("SpatialQuizActivity"));
    }
}
