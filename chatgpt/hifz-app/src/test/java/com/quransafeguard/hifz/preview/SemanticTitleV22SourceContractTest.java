package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression fence: V2.2 may replace French titles only; V2.1 stays authoritative otherwise. */
public final class SemanticTitleV22SourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void runtimeJoinsAuditedV22TitlesOntoFrozenV21Passages() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(source.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(source.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(source.contains("semantic/semantic_titles_v2_2.json"));
        assertTrue(source.contains("4148cb96f68b85121ba3753676d97d9e7b3adaf7f0717707b74b8b8ca0baf071"));
        assertTrue(source.contains("c4700d626c6e55869de017e1841eb5b9e3ef0eb6139d7f0e84ffd7feff7e6db8"));
        assertTrue(source.contains("parseTitleOverlay"));
        assertTrue(source.contains("String title = meta.titleV22"));
        assertTrue(source.contains("requiredText(row, \"title_fr_v2_1\")"));
        assertTrue(source.contains("anchor_arabic_v2_1"));
        assertTrue(source.contains("anchor_word_count_v2_1"));
        assertTrue(source.contains("minimality_verified_v2_1"));
        assertFalse("V2.2 title overlay must not supply boundaries",
            source.contains("titleRow.optInt(\"surah_start\"") || source.contains("titleRow.optInt(\"start_line\""));
    }

    @Test public void buildMaterializesBothFrozenV21AndSeparateV22TitleOverlay() throws Exception {
        String build = read("hifz-app/build.gradle.kts");
        String materializer = read("scripts/materialize_semantic_v2_2_titles.py");
        assertTrue(build.contains("prepareSemanticV21"));
        assertTrue(build.contains("prepareSemanticV22Titles"));
        assertTrue(build.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(build.contains("semantic/semantic_titles_v2_2.json"));
        assertTrue(build.contains("src/main/semantic-source/v2_2_titles"));
        assertTrue(materializer.contains("EXPECTED_TITLE_COUNT = 1256"));
        assertTrue(materializer.contains("c4700d626c6e55869de017e1841eb5b9e3ef0eb6139d7f0e84ffd7feff7e6db8"));
        assertTrue(materializer.contains("4148cb96f68b85121ba3753676d97d9e7b3adaf7f0717707b74b8b8ca0baf071"));
        assertTrue(materializer.contains("actual_ids == expected_ids"));
        assertTrue(materializer.contains("output.write_bytes(raw_titles)"));
    }

    @Test public void titleLayerCannotLeakIntoSabqiItqanOrQuizProgression() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String quiz = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        assertFalse(session.contains("title_fr_v2_2"));
        assertFalse(quiz.contains("title_fr_v2_2"));
        assertTrue(session.contains("mushaf.clearSemanticCues()"));
        assertTrue(session.contains("activeCuePrompt.setText(activeRecallCue.anchorArabic)"));
        assertTrue(quiz.contains("aucun changement de progression"));
    }
}
