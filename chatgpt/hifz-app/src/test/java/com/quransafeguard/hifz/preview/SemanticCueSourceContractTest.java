package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression fence for semantic cues: optional in Lecture, exact-only in blind recall, absent in exams. */
public final class SemanticCueSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void repositoryFailsClosedUntilAuditedV21AssetIsPresent() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(source.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(source.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(source.contains("title_fr_v2_1"));
        assertTrue(source.contains("anchor_arabic_v2_1"));
        assertTrue(source.contains("anchor_word_count_v2_1"));
        assertTrue(source.contains("minimality_verified_v2_1"));
        assertTrue(source.contains("Fail closed"));
        assertTrue(source.contains("hasCompleteExactGeometryForPage"));
        assertTrue(source.contains("source-ink groups, not linguistic words"));
        assertFalse("runtime must not infer fake word boxes from line geometry",
            source.contains("guess") || source.contains("approximateWord"));
    }

    @Test public void frozenV21CorpusIsMaterializedIntoApkAssetsWithHashFence() throws Exception {
        String build = read("hifz-app/build.gradle.kts");
        String materializer = read("scripts/materialize_semantic_v2_1.py");
        assertTrue(build.contains("prepareSemanticV21"));
        assertTrue(build.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(build.contains("from(generatedSemanticAssetsDir)"));
        assertTrue(materializer.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(materializer.contains("Qaf 50:15 must remain autonomous"));
        assertTrue(materializer.contains("Al-Hujurat 49:11-13 must remain continuous"));
        assertTrue(materializer.contains("output.write_bytes(raw)"));
        assertFalse("materializer must not normalize or rewrite semantic JSON",
            materializer.contains("json.dumps("));
    }

    @Test public void readingUsesOneToggleAndTransientTitleInsteadOfButtonProliferation() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(source.contains("semanticButton = Ui.smallButton(this, \"Amorces\""));\n        assertTrue(source.contains("setButtonIconWithText(semanticButton, icon, \"Amorces\")"));
        assertTrue(source.contains("toggleSemanticCues()"));
        assertTrue(source.contains("SemanticTitlePopup.show"));
        assertTrue("repères default OFF until user enables them",
            source.contains("getBoolean(\"semantic_cues_enabled\", false)"));
        assertFalse("no second permanent semantic-title button",
            source.contains("semanticTitleButton"));
    }

    @Test public void activeRevisionKeepsLegacyLandmarksWhenExactAnchorGeometryIsIncomplete() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(source.contains("hasCompleteExactGeometryForPage(page)"));
        assertTrue(source.contains("mushaf.setSemanticCues(semanticPassages.readerCuesForPage(page), true)"));
        assertTrue(source.contains("return applyActiveLandmarks(page);"));
        assertTrue(source.contains("currentLineIds = applyActiveRecallCues(currentPage)"));
    }

    @Test public void sabqiAndItqanCannotInheritSemanticCueState() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int renderMode = source.indexOf("private void renderMode()");
        int renderSabqi = source.indexOf("private void renderSabqi");
        assertTrue(renderMode >= 0 && renderSabqi > renderMode);
        String dispatch = source.substring(renderMode, renderSabqi);
        assertTrue(dispatch.contains("mushaf.clearSemanticCues()"));
        assertFalse("semantic opt-in must not be wired into Sabqi dispatch", dispatch.contains("setSemanticCues("));
    }

    @Test public void maskedSemanticRecallOnlyExemptsAuditedCellRanges() throws Exception {
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(js.contains("semanticVisibleCellKeys"));
        assertTrue(js.contains("if(semanticVisible.has(key))return"));
        assertTrue(js.contains("Exact phrase outline is drawn only from audited visual ranges"));
        assertTrue(js.contains("if(gap<6)return"));
    }
}
