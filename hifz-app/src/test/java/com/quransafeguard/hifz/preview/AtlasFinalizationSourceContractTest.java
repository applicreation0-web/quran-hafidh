package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Targeted source-level safeguards for the final Atlas interface adjustments.
 * These checks do not claim runtime BOOX or storage validation.
 */
public final class AtlasFinalizationSourceContractTest {
    private static String read(String file) throws Exception {
        Path path = Paths.get(file);
        if (!Files.exists(path)) path=Paths.get("..",file);
        return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
    }
    private static final String ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";

    @Test public void backgroundConsultationSuspendsAutoHideWithoutRevealingHiddenControls()throws Exception {
        String reader=read(ROOT+"StudyReaderActivity.java");
        assertTrue(reader.contains("@Override protected void onPause()"));
        assertTrue(reader.contains("if(mushaf!=null)mushaf.removeCallbacks(autoHide);"));
        assertTrue(reader.contains("if(controlsVisible)scheduleAutoHide();"));
    }

    @Test public void weeklyGoalLabelsUsePagesOnlyWithoutChangingSchedules() throws Exception {
        String home=read(ROOT+"MainActivity.java");
        assertTrue(home.contains("pathTile(learningModes, \"Apprentissage\", \"1 page/semaine\""));
        assertTrue(home.contains(": \"2 pages/semaine\";"));
        assertTrue("Post-Nas maintenance remains distinct",
            home.contains("prefs.itqanEntretienActive() ? \"1 hizb · répétitions\""));
        assertFalse(home.contains("pathTile(learningModes, \"Apprentissage\", \"5 lignes\""));
        assertFalse(home.contains("2 pages / 4 séances"));
    }

    @Test public void qcfParityAndSelectionNeverShareTheSameFillSwitch() throws Exception {
        String pages=read(ROOT+"MapQcfThumbnails.java");
        assertTrue(pages.contains("button.even{background:#121211;color:#faf8f0"));
        assertTrue(pages.contains("button.odd{background:#faf8f0;color:#121211"));
        assertTrue(pages.contains("outline:2px solid #121211;outline-offset:2px"));
        assertTrue(pages.contains("append(p%2==0?\"even\":\"odd\")"));
        assertTrue(pages.contains("if(p==selected)html.append(\" aria-current='page'\")"));
        // The selected style changes only size/border/ring, not pair/impair fill.
        int current=pages.indexOf("button[aria-current=page]{");
        int end=pages.indexOf("}",current);
        assertTrue(current>=0 && end>current);
        assertFalse(pages.substring(current,end).contains("background:"));
        assertTrue(pages.contains("MapPages.step(dx>0?1:-1)"));
    }

    @Test public void atlasUsesSingleSlidingUnitRailAndDirectAuthenticQcf() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue("Exactly five unit positions maximum",
            map.contains("Math.min(5,unitCount())"));
        assertTrue("Every unit remains reachable by sliding",
            map.contains("shiftUnitWindow(dx>0?-1:1)"));
        assertTrue("Active unit remains distinctly circled",
            map.contains("ordinal==activeMacro"));
        assertTrue("Original approved 17 October double-rail is restored",
            map.contains("private final class Graph extends View"));
        assertTrue("Only authentic passage boundaries are drawn beneath unit rail",
            map.contains("private float passageX(")
            && map.contains("AtlasPassageContext.intersectionStart(p.firstAyah,unit.start)"));
        assertTrue("Selected passage subtitle is visible in Atlas",
            map.contains("passageSubtitle=Ui.text(activity,\"\",12.5f,false)"));
        assertFalse("No initial miniature mosaic",map.contains("setMosaic(true)"));
        assertTrue("Unit title is larger and bold",map.contains("Ui.text(activity,\"\",16f,true)"));
        assertFalse("Passage titles are shown in Lecture, never as a map rail",
            map.contains("selectedPassageTitle=Ui.text"));
        assertTrue("Only direct authentic QCF full page is shown",
            map.contains("thumbnails.setMosaic(false)"));
        assertFalse("No redundant third page band above Mushaf",
            map.contains("c.drawText(\"Pages \"+firstPage"));
        assertTrue("QCF page pills stay below Mushaf",
            read(ROOT+"MapQcfThumbnails.java").contains("aria-current='page'"));
        assertTrue("Gray shading uses the entire active Ayah Themes passage",
            map.contains("thumbnails.showPages(first,last,page,surah,focusStart,focusEnd,false,"));
        assertTrue("Existing Lecture action is retained",map.contains("Ui.iconButton(activity,\"\",\"Lecture\""));
        assertTrue("Lecture carries the selected passage, not just the unit",
            map.contains(".putExtra(\"map114PassageId\",current().id)"));
        assertTrue("Open/return QCF page stays in its unit",
            map.contains("activity.startActivityForResult(intent,11436)"));
    }

    @Test public void mapRestoresTheActualSelectedPassageInsteadOfTheUnitBeginning() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue("Saved verse must select the actual passage after activity recreation",
            map.contains("b.putInt(\"map114Ayah\",focusStart);"));
        assertTrue(map.contains("b.putInt(\"map114UnitWindowStart\",unitWindowStart);"));
        assertTrue(map.contains("b.putInt(\"map114Page\",page);"));
    }

    @Test public void sourcePassagesRemainUnchangedButNeverAppearAsAtlasNodes() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue("Canonical Passage display title is resolved without rewriting its source",
            map.contains("macros.displayPassageTitle(current().id,titles.titleForPassage(current().id))"));
        assertTrue("Shared passages are bounded to their exact Atlas intersection",
            map.contains("macros.displayContextTitle(")
                && map.contains("AtlasPassageContext.intersectionStart(")
                && map.contains("AtlasPassageContext.intersectionEnd("));
        assertTrue("Atlas uses only source-backed unit labels",map.contains("passageTitle.setText(macros.displayUnitTitle(unit.id));"));
        assertTrue("No PDF action from selected unit title",map.contains("showTitle(macros.displayUnitTitle(macro().id));"));
        assertFalse("Do not open Islāḥī or Maududi commentaries from unit Atlas",
            map.substring(map.indexOf("private void showCurrentTitle(){"),
                map.indexOf("private void chooseUnit(){")).contains("AtlasSourcePdfActivity"));
    }

    @Test public void nextPrivateReleaseCannotDisplayUnapprovedGenericAtlasHeadings() throws Exception {
        String data=read(ROOT+"MawdudiMapData.java");
        assertTrue(data.contains("BuildConfig.AYAH_THEMES_FINAL_PRIVATE"));
        assertTrue(data.contains("approved == null || approved.size() != 366"));
        assertTrue(data.contains("Atlas private APK blocked: 366 verified titles required"));
        // Existing 120 approved labels remain bounded and checksummed for legacy previews.
        assertTrue(data.contains("entries.length()!=120"));
        assertTrue(data.contains("421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79"));
    }

    @Test public void atlasNamesAndPictogramsStayDistinct() throws Exception {
        String home=read(ROOT+"MainActivity.java");
        String map=read(ROOT+"MawdudiMapScreen.java");
        String reader=read(ROOT+"StudyReaderActivity.java");
        String ui=read(ROOT+"Ui.java");
        assertTrue(home.contains("primaryTile(readingRow, \"Atlas\", \"Atlas\""));
        assertTrue(map.contains("Ui.iconButton(activity,\"\",\"Atlas\""));
        assertTrue(map.contains("Unité Atlas · "));
        assertFalse("Passage captions belong to Lecture, not the unit-only Atlas",
            map.contains("Passage Ayah Themes · "));
        assertTrue(reader.contains("Passage Ayah Themes actif sur cette page"));
        assertTrue(reader.contains("\"Afficher les ancres\""));
        assertTrue(ui.contains("s.equals(\"atlas\")"));
        assertTrue("Ancre uses only its dedicated icon",
            ui.contains("if (s.contains(\"ancre\")) return R.drawable.ic_ui_semantic_anchor;"));
        assertTrue("Frozen Amorces retain the independently approved key-round icon",
            ui.contains("if (s.contains(\"amorce\")) return R.drawable.ic_ui_recall_key;"));
    }
    @Test public void sharedPassageLaunchCarriesCanonicalBoundsSeparatelyFromFocus() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue(map.contains(".putExtra(\"map114SelectedStart\",current().firstAyah)"));
        assertTrue(map.contains(".putExtra(\"map114SelectedEnd\",current().lastAyah)"));
        assertTrue(map.contains("b.putString(\"map114SelectedPassageId\",current().id)"));
    }

    @Test public void readerRestorationSupersedesLaunchVerseAndRetainsSelectedFocus() throws Exception {
        String reader=read(ROOT+"StudyReaderActivity.java");
        assertTrue(reader.contains("state.getString(\"map114SelectedVerse\")"));
        assertTrue(reader.contains("out.putBoolean(\"map114SemanticCues\",semanticCuesEnabled)"));
        assertTrue(reader.contains("out.putBoolean(\"map114TafsirOpen\",isTafsirOpen())"));
        assertTrue(reader.contains("if(selected==null||!focus.contains(selected))selected=focus.get(0);"));
    }

    @Test public void readerUnitCaptionUsesActualSixteenSpAndCanonicalPopup() throws Exception {
        String reader=read(ROOT+"StudyReaderActivity.java");
        assertTrue(reader.contains("new AbsoluteSizeSpan(Math.round(16f*getResources().getDisplayMetrics().scaledDensity))"));
        assertTrue(reader.contains("caption.setSpan(new StyleSpan(Typeface.ITALIC)"));
        assertTrue("Ayah Themes subtitle must be title-only",reader.contains("caption.append(MawdudiMapScreen.cleanPassageCaption(title))"));
        assertFalse("Never prefix verse ranges to the Atlas subtitle",reader.contains("caption.append(active.surah+\":\"+active.firstAyah"));
        assertTrue("Map subtitle remains source Ayah Themes title",read(ROOT+"MawdudiMapScreen.java").contains("passageSubtitle.setText(graph.selectedTitle())"));
        int start=reader.indexOf("private void showFullAtlasCue()");
        int end=reader.indexOf("private void updateAtlasCueFooter()",start);
        assertTrue(reader.substring(start,end).contains("ayahThemesCues.titleForPassage(active.id)"));
    }

    @Test public void atlasOnlyKeepsSelectedBoundariesAndNoDetachedRelationEntryPoint() throws Exception {
        String reader=read(ROOT+"StudyReaderActivity.java"),map=read(ROOT+"MawdudiMapScreen.java");
        assertFalse(reader.contains("CorrespondenceEntryPoints"));
        assertFalse(map.contains("CorrespondenceEntryPoints"));
        assertFalse(reader.contains("correspondenceHost"));
        assertFalse(map.contains("correspondenceHost"));
        assertTrue(reader.contains("map114FocusStart"));
        assertTrue(reader.contains("map114FocusEnd"));
        assertTrue(map.contains("AtlasPassageContext.intersectionStart"));
        assertTrue(map.contains("AtlasPassageContext.intersectionEnd"));
        assertTrue(reader.contains("applyFreeActiveMask()"));
    }

    @Test public void ordinaryLectureRestoresCursorInsteadOfReplayingInitialJump() throws Exception {
        String reader=read(ROOT+"StudyReaderActivity.java");
        assertTrue("Only a fresh activity may consume the initial page jump",
            reader.contains("int jumpPage = state==null ? getIntent().getIntExtra(EXTRA_JUMP_PAGE, 0) : 0;"));
        assertTrue(reader.contains("if(state!=null)page=state.getInt(\"map114ReaderPage\",page);"));
        assertTrue(reader.contains("String jumpVerse = state!=null ? state.getString(\"map114SelectedVerse\")"));
        int start=reader.indexOf("@Override protected void onSaveInstanceState(Bundle out)");
        int end=reader.indexOf("@Override public void onReady()",start);
        assertFalse("Every Lecture activity saves its cursor and presentation state",
            reader.substring(start,end).contains("if(mapPreview){"));
        assertTrue(reader.contains("if (!mapPreview) getSharedPreferences(\"hifz_study\", MODE_PRIVATE).edit().putInt(\"page\", page).apply();"));
    }

}
