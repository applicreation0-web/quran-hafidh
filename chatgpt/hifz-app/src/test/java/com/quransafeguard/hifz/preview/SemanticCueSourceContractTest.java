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
        assertTrue("amorce popup must use the canonical Arabic Al-Munir heading",
            source.contains("SemanticTitlePopup.show(this, cue.title, semanticTitleDialog)"));
        assertFalse("French titles must never be shown by the Lecture semantic popup",
            source.contains("SemanticTitlePopup.show(this, cue.titleFr"));
        assertTrue("tapping the patterned amorce must open its title directly",
            read("hifz-app/src/main/assets/hifzreader/reader.js").contains("N?.semanticCueTap?.(String(cue.id))"));
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
        assertTrue(session.contains("\"Révéler\""));
        assertTrue(session.contains("\"Terminer\""));
        assertTrue(session.contains("\"Bloc précédent\""));
        assertTrue(session.contains("\"Bloc suivant\""));
        assertTrue(session.contains("recordActiveRevisionCurrentPage()"));
        assertFalse("an amorce must never be a navigation step", session.contains("advanceActiveRecallCue()"));
        assertFalse("active revision must not instruct amorce-by-amorce navigation",
            session.contains("passez à l’Amorce suivante"));
        assertTrue("active revision amorces stay on original paper, without grey overlay",
            session.contains("setSemanticCues(semanticPassages.readerCuesForPage(currentPage), exact, false)"));
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

    @Test public void memorizationMaskErasesInkOnPaperWithoutGreyTiles() throws Exception {
        String html = read("hifz-app/src/main/assets/hifzreader/index.html");
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("masked ink must blend into the original Mushaf sheet",
            html.contains(".maskcell{fill:var(--sheet)}"));
        assertFalse("mask cells must not look like rounded grey pills",
            js.contains("el.setAttribute('rx','2')") || js.contains("el.setAttribute('ry','2')"));
        assertTrue("partial paper erasure must keep the inter-line safety gap",
            js.contains("const partialPadX=eink?0.72:0.58,partialPadY=eink?0.42:0.34"));
        assertTrue("100% paper erasure must close residual BOOX diacritic fringes without broad line bleed",
            js.contains("const fullErase=segment.fullErase===true")
                && js.contains("const padY=fullErase?(eink?0.12:0.08):partialPadY")
                && js.contains("y:fullErase?cell.top+0.15:cell.top+0.6")
                && js.contains("fullErase"));
        assertTrue("generic verse selection may still use the quiet grey vocabulary",
            html.contains(".ayahPolygon.selected{fill-opacity:var(--sel-op)}"));
        assertTrue("Sabqi/Itqan work mode must suppress that large grey fill",
            js.contains("!workBlockMode&&shadeVerseSelection()"));
        assertTrue("Sabqi/Itqan boundaries must be shown as start/end brackets instead",
            js.contains("function workBlockBoundaryLayer(lines,polys)")
                && js.contains("draw(workBlockStart,'start')")
                && js.contains("draw(workBlockEnd,'end')"));
        assertTrue("work brackets must use one fixed visual length at both ends",
            js.contains("const span=eink?31:29"));
        assertTrue("brackets must follow the selected part of a shared physical line",
            js.contains("const relevant=polys&&polys.length")
                && js.contains("insideSelection(polys"));
        assertTrue("BOOX bracket rendering must use a strong simple solid stroke",
            js.contains("path.setAttribute('stroke-width',eink?'1.70':'1.45')")
                && js.contains("path.setAttribute('stroke','var(--ink)')"));
        assertTrue("Sabqi/Itqan B+ must keep real surrounding Mushaf faintly readable for visual memory",
            js.contains("function workContextLayer(svg,allLines,activeLines,polys)")
                && js.contains("contextMask.id='hifz-work-context-mask'")
                && js.contains("paper.setAttribute('fill','var(--sheet)')")
                && js.contains("paper.setAttribute('fill-opacity',eink?'0.72':'0.65')"));
        assertTrue("B+ must avoid true blur/filter on E-Ink",
            js.contains("No SVG blur/filter"));
        assertTrue("B+ must fail open if exact verse polygons are unavailable",
            js.contains("if(!polys||!polys.length)return g"));
        assertTrue("B+ mask holes must override source ayahPolygon fill-opacity=0",
            js.contains("'fill-opacity','stroke-opacity','opacity','mask','clip-path'")
                && js.contains("hole.setAttribute('fill-opacity','1')")
                && js.contains("hole.setAttribute('opacity','1')"));
        assertTrue("selection polygons must be shared by context, progressive mask and brackets",
            js.contains("const polys=maskFollowsSelection?selectedPolygons(svg):[]")
                && js.contains("const cells=maskCandidates(lines,polys)")
                && js.contains("workBlockBoundaryLayer(lines,polys)"));
    }

    @Test public void amorcesUseAnEinkSafePatternNotTheSabqiSolidGrey() throws Exception {
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(js.contains("pattern.id='hifz-semantic-hatch'"));
        assertTrue(js.contains("pattern.setAttribute('patternUnits','userSpaceOnUse')"));
        assertTrue(js.contains("hatch.setAttribute('d','M-2,7 L7,-2 M5,9 L9,5')"));
        assertTrue(js.contains("el.setAttribute('fill','url(#hifz-semantic-hatch)')"));
        assertTrue(js.contains("hatch.setAttribute('stroke-opacity',eink?'0.82':'0.46')"));
        assertTrue(js.contains("hatch.setAttribute('stroke-width',eink?'0.70':'0.54')"));
        assertTrue("exact amorce words on one physical line must share one regular frame",
            js.contains("function semanticExactRects(cue,svg)")
                && js.contains("function semanticCueRect(rect,cue)")
                && js.contains("rx','1.35'"));
    }

    @Test public void exhaustiveAlMunirFrenchOverlayIsHashFencedButArabicIsTheOnlyRuntimeDisplay() throws Exception {
        String repository = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        String materializer = read("scripts/materialize_semantic_al_munir_fr_v1.py");
        String build = read("hifz-app/build.gradle.kts");
        assertTrue(repository.contains("semantic/semantic_titles_al_munir_fr_v1.json"));
        assertTrue(repository.contains("7de847efb4fe8c2375ec7c3a8d1386869a52098b7a1d7dffa621467f87b83b19"));
        assertTrue(repository.contains("EXPECTED_CANONICAL_MUNIR_PASSAGES = 1243"));
        assertTrue("French overlay may remain packaged for provenance but must not drive reader payload",
            repository.contains("titlesAlMunirFr.get(meta.canonicalId)")
                && repository.contains("item.put(\"title\", cue.title)")
                && !repository.contains("item.put(\"title\", cue.titleFr)"));
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String popup = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticTitlePopup.java");
        assertTrue("Lecture must display Arabic Al-Munir only",
            study.contains("SemanticTitlePopup.show(this, cue.title, semanticTitleDialog)"));
        assertTrue("Active revision must display Arabic Al-Munir only",
            session.contains("SemanticTitlePopup.show(this, cue.title, semanticTitleDialog)"));
        assertFalse(study.contains("SemanticTitlePopup.show(this, cue.titleFr"));
        assertFalse(session.contains("SemanticTitlePopup.show(this, cue.titleFr"));
        assertTrue("Arabic semantic title popup must force RTL direction",
            popup.contains("View.TEXT_DIRECTION_RTL"));
        assertFalse("temporary Java title overrides must be gone",
            repository.contains("VERIFIED_FRENCH_TITLE_OVERRIDES"));
        assertTrue(build.contains("prepareAlMunirFrenchTitles"));
        assertTrue(materializer.contains("EXPECTED_TITLE_COUNT = 1243"));
        assertTrue(materializer.contains("411c89130b3c3ceb22700ec0864fc0ef1d1f06f3623a1442f03d41a4e8e290ee"));
        assertTrue(materializer.contains("SP0013"));
        assertTrue(materializer.contains("enseignement des langues"));
        assertTrue(materializer.contains("SP0351"));
        assertTrue(materializer.contains("Amalécites"));
        assertTrue(materializer.contains("SP1023"));
        assertTrue(materializer.contains("Les dahriyya"));
        assertTrue(materializer.contains("REJECTED_FRENCH"));
    }

    @Test public void tafsirPanelTracksTheLatestTappedVerse() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(source.contains("refreshOpenTafsir(verse);"));
        assertTrue(source.contains("private void refreshOpenTafsir(VerseRef verse)"));
        assertTrue("phone Tafsir must be a non-modal bottom panel",
            source.contains("private FrameLayout bottomTafsir")
                && source.contains("openBottomTafsir(verse)")
                && source.contains("bottomTafsir.addView(shell"));
        assertFalse("phone Tafsir must not use a modal Dialog anymore",
            source.contains("tafsirDialog"));
        assertTrue("an open phone Tafsir must stay open while a new verse is tapped",
            source.contains("boolean tafsirWasOpen = isTafsirOpen()")
                && source.contains("if (tafsirWasOpen && !largeScreen) hideControls();"));
        assertTrue("BOOX/tablet must keep the non-modal side panel",
            source.contains("if (largeScreen) {")
                && source.contains("openSideTafsir(verse)")
                && source.contains("eink.local(sideTafsir, hifzPrefs)"));
    }

    @Test public void amorceIconIsAnAnchorAndExactBoxesStayAudited() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String repository = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticPassageRepository.java");
        assertTrue(ui.contains("if (s.contains(\"amorce\")) return R.drawable.ic_ui_semantic_anchor"));
        assertTrue(repository.contains("wordGeometry.anchorBoxes(page, cue.startVerse, cue.anchorWordCount)"));
        assertTrue(repository.contains("!= cue.anchorWordCount"));
    }

    @Test public void maskedSemanticRecallOnlyExemptsExactAuditedWordBoxes() throws Exception {
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(js.contains("function protectedWordBoxes()"));
        assertTrue(js.contains("function applyProtectedWordHoles(layer,svg)"));
        assertTrue(js.contains("function wordBoxInSvgSpace(box,svg)"));
        assertTrue(js.contains("layer.setAttribute('mask','url(#hifz-exact-word-holes)')"));
        assertTrue("normal reading amorces must use the dedicated hatch, not Sabqi/Itqan solid grey",
            js.contains("el.setAttribute('fill','url(#hifz-semantic-hatch)')"));
        assertTrue(js.contains("pattern.id='hifz-semantic-hatch'"));
        assertTrue(js.contains("el.setAttribute('stroke','none')"));
        assertTrue("normal reading amorce highlight itself must be tappable",
            js.contains("el.setAttribute('pointer-events','all')"));
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
