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

    @Test public void repositoryFailsClosedOnAuditedV22TitleLayerWhileKeepingV21AnchorsFrozen() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(source.contains("semantic/semantic_passages_v2_2_titles.json"));
        assertTrue(source.contains("f8655e2d4269183b8ab5665a394db8255aeac20683f2d79bdf45c04f85e9bfde"));
        assertTrue("V2.1 provenance must remain explicit",
            source.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(source.contains("title_fr_v2_2"));
        assertTrue(source.contains("title_audit_status_v2_2"));
        assertTrue(source.contains("title_sources_v2_2"));
        assertTrue(source.contains("title_distinctiveness_v2_2"));
        assertTrue(source.contains("String title = meta.titleV22"));
        assertTrue(source.contains("title_fr_v2_1"));
        assertTrue(source.contains("anchor_arabic_v2_1"));
        assertTrue(source.contains("anchor_word_count_v2_1"));
        assertTrue(source.contains("minimality_verified_v2_1"));
        assertTrue(source.contains("V2.2 title must not be copied into page records"));
        assertTrue(source.contains("Fail closed"));
        assertTrue(source.contains("hasCompleteExactGeometryForPage"));
        assertTrue(source.contains("source-ink groups, not linguistic words"));
        assertFalse("runtime must not infer fake word boxes from line geometry",
            source.contains("guess") || source.contains("approximateWord"));
    }

    @Test public void frozenV21CorpusFeedsHashFencedV22TitleMaterialization() throws Exception {
        String build = read("hifz-app/build.gradle.kts");
        String v21Materializer = read("scripts/materialize_semantic_v2_1.py");
        String v22Materializer = read("scripts/materialize_semantic_v2_2_titles.py");
        assertTrue(build.contains("prepareSemanticV21"));
        assertTrue(build.contains("prepareSemanticV22Titles"));
        assertTrue(build.contains("semantic/semantic_passages_v2_1.json"));
        assertTrue(build.contains("semantic/semantic_passages_v2_2_titles.json"));
        assertTrue(build.contains("exclude(\"semantic/semantic_passages_v2_1.json\")"));
        assertTrue(v21Materializer.contains("b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"));
        assertTrue(v21Materializer.contains("Qaf 50:15 must remain autonomous"));
        assertTrue(v21Materializer.contains("Al-Hujurat 49:11-13 must remain continuous"));
        assertTrue(v21Materializer.contains("output.write_bytes(raw)"));
        assertFalse("V2.1 materializer must not normalize or rewrite semantic JSON",
            v21Materializer.contains("json.dumps("));
        assertTrue(v22Materializer.contains("EXPECTED_V21_SHA256"));
        assertTrue(v22Materializer.contains("EXPECTED_TITLES_TSV_SHA256"));
        assertTrue(v22Materializer.contains("f8655e2d4269183b8ab5665a394db8255aeac20683f2d79bdf45c04f85e9bfde"));
        assertTrue(v22Materializer.contains("page passage records changed during title materialization"));
        assertTrue(v22Materializer.contains("SP0001..SP1256"));
    }

    @Test public void readingUsesOneToggleAndTransientTitleInsteadOfButtonProliferation() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(source.contains("semanticButton = Ui.smallButton(this, \"Amorces\""));
        assertTrue(source.contains("setButtonIconWithText(semanticButton, icon, \"Amorces\")"));
        assertTrue(source.contains("toggleSemanticCues()"));
        assertTrue(source.contains("SemanticTitlePopup.show"));
        assertTrue(source.contains("annotationButton = Ui.iconButton(this, \"\", \"Annoter\""));
        assertTrue(source.contains("Ui.iconButton(this, \"\", \"Annuler la note\""));
        assertTrue(source.contains("Ui.iconButton(this, \"\", \"Effacer les notes\""));
        assertTrue("repères default OFF until user enables them",
            source.contains("getBoolean(\"semantic_cues_enabled\", false)"));
        assertFalse("no second permanent semantic-title button",
            source.contains("semanticTitleButton"));
    }

    @Test public void activeRevisionIsDrivenByAuditedAmorcesWithoutApproximateWordGeometry() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String repository = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(session.contains("activeCuePrompt.setText(activeRecallCue.anchorArabic)"));
        assertTrue(session.contains("Amorce suivante"));
        assertTrue(session.contains("advanceActiveRecallCue()"));
        assertTrue(session.contains("mushaf.setLandmarkLines(null, null)"));
        assertFalse("active revision must not fall back to unrelated half-line landmarks",
            session.contains("applyActiveLandmarks(") || session.contains("applyActiveRecallCues("));
        assertTrue("the semantic index must use frozen audited passage boundaries",
            repository.contains("surah_start") && repository.contains("ayah_start")
                && repository.contains("surah_end") && repository.contains("ayah_end"));
        assertTrue(repository.contains("firstEligibleCueAtOrContaining"));
        assertTrue(repository.contains("nextEligibleCue"));
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
