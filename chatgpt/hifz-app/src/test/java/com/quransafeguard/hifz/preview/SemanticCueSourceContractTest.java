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
        assertTrue(source.contains("semanticButton = Ui.iconButton(this, \"\", \"Afficher les amorces\""));
        assertTrue(source.contains("Ui.setButtonIcon(semanticButton, icon)"));
        assertFalse("Amorces control must stay icon-only", source.contains("setButtonIconWithText(semanticButton"));
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
        assertFalse("active revision must not duplicate a Quranic amorce above the Mushaf",
            session.contains("activeCuePrompt"));
        assertTrue(session.contains("applyActiveRevisionPageCues()"));
        assertTrue(session.contains("\"Suivant\""));
        assertTrue(session.contains("\"Révéler\""));
        assertTrue(session.contains("\"Terminer\""));
        assertTrue(session.contains("advanceActiveRecallCue()"));
        assertTrue(session.contains("mushaf.setLandmarkLines(null, null)"));
        assertTrue(session.contains("Temps effectué : "));
        assertTrue("Al-Munir explicit grouping must be the canonical runtime structure",
            repository.contains("tafsir_munir_grouping")
                && repository.contains("EXPECTED_CANONICAL_MUNIR_PASSAGES = 1243"));
        assertTrue("legacy V2.1 IDs remain aliases only for persisted-cursor migration",
            repository.contains("legacy IDs as read-only aliases"));
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

    @Test public void maskedSemanticRecallOnlyExemptsExactAuditedWordBoxes() throws Exception {
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(js.contains("function protectedWordBoxes()"));
        assertTrue(js.contains("function applyProtectedWordHoles(layer,svg)"));
        assertTrue(js.contains("function wordBoxInSvgSpace(box,svg)"));
        assertTrue(js.contains("layer.setAttribute('mask','url(#hifz-exact-word-holes)')"));
        assertTrue(js.contains("same quiet fill grammar as Sabqi/Itqan"));
        assertTrue(js.contains("fill-opacity','var(--sel-op)"));
        assertTrue(js.contains("rect.setAttribute('stroke','none')"));
        assertFalse("active recall must not derive word holes by proportional line arithmetic",
            js.contains("approximateWord") || js.contains("guessWord"));
    }
    @Test public void spatialQuizIsCompletelyRemoved() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String selector = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/RevisionSelector.java");
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertFalse(main.contains("SpatialQuiz"));
        assertFalse(selector.contains("Quiz · Acquis"));
        assertFalse(selector.contains("openSpatialQuiz"));
        assertFalse(manifest.contains("SpatialQuizActivity"));
        assertFalse(mushaf.contains("Quiz"));
        assertFalse(js.contains("quizTargetLine"));
        assertFalse(js.contains("quizVisibleLines"));
        assertFalse(js.contains("quizGuideLayer"));
    }

}
