package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source and editorial invariants; no Hifz engine or synthetic literary certification. */
public final class CorrespondenceContextContractTest {
    private static String source(String path)throws Exception{
        Path p=Paths.get(path);
        if(!Files.exists(p))p=Paths.get("..",path);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
    @Test public void exactlyFourHistoricallyReviewedDemoCases(){
        assertEquals(4,CorrespondenceCases.editorialPilotForTest().size());
        assertEquals("R030",CorrespondenceCases.editorialPilotForTest().get(0).id);
        assertEquals("R037",CorrespondenceCases.editorialPilotForTest().get(1).id);
        assertEquals(4,CorrespondenceCases.editorialPilotForTest().get(2).verses.size());
        assertEquals(31,CorrespondenceCases.editorialPilotForTest().get(3).verses.size());
        assertTrue(CorrespondenceCases.editorialPilotForTest().get(3)
            .contains(new com.quransafeguard.hifz.core.VerseRef(55,77)));
        assertFalse(CorrespondenceCases.editorialPilotForTest().get(3)
            .contains(new com.quransafeguard.hifz.core.VerseRef(55,76)));
    }
    @Test public void productionNeverActivatesUncertifiedCatalogue()throws Exception{
        String gradle=source("hifz-app/build.gradle.kts");
        assertTrue(gradle.contains("CORRESPONDENCES_EDITORIAL_PREVIEW"));
        assertTrue(gradle.contains("HAAFIDH_CORRESPONDENCES_PILOT"));
        assertTrue(gradle.contains("buildConfigField(\"boolean\", \"CORRESPONDENCES_EDITORIAL_PREVIEW\", \"false\")"));
        assertTrue(source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceCases.java")
            .contains("if(!enabled()"));
    }
    @Test public void exactLucideGitCompareIsNotChain()throws Exception{
        String glyph=source("hifz-app/src/main/res/drawable/ic_ui_correspondances.xml");
        String gutter=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceGutterMarkers.java");
        String similarity=source("hifz-app/src/main/res/drawable/ic_ui_atlas_similar.xml");
        assertTrue(glyph.contains("M13,6h3a2,2 0,0 1,2 2v7"));
        assertTrue(glyph.contains("M11,18H8a2,2 0,0 1,-2 -2V9"));
        assertTrue(glyph.contains("M9,6a3,3"));
        assertTrue(glyph.contains("M21,18a3,3"));
        assertTrue(gutter.contains("M13 6h3a2 2 0 0 1 2 2v7"));
        assertTrue(gutter.contains("M11 18H8a2 2 0 0 1-2-2V9"));
        assertTrue(gutter.contains("M9 17H7A5 5 0 0 1 7 7h2"));
        assertTrue(gutter.contains("M15 7h2a5 5 0 1 1 0 10h-2"));
        assertTrue(gutter.contains("stroke-width=\\\"2\\\""));
        assertTrue(similarity.contains("M9 17H7A5 5 0 0 1 7 7h2 M15 7h2a5 5 0 1 1 0 10h-2 M8 12H16"));
        assertTrue(similarity.contains("android:strokeWidth=\"2\""));
        assertTrue(glyph.contains("android:strokeWidth=\"2\""));
        assertFalse(glyph.contains("M10,13"));
    }
    @Test public void bothMarkersStayOutsideTheAuthenticQcf()throws Exception{
        String marker=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceGutterMarkers.java");
        assertTrue(marker.contains("m.querySelectorAll('.ayahPolygon')"));
        assertTrue(marker.contains("placed={left:[],right:[]}"));
        assertTrue(marker.contains("item.kind==='similarity'?'left':'right'"));
        assertTrue(marker.contains("width:44px;height:44px"));
        assertTrue(marker.contains("HifzNative.comparisonTap"));
        assertTrue(marker.contains("HifzNative.correspondenceTap"));
        assertTrue(marker.contains("__haafidhAtlasMarkerDispose"));
        assertTrue(marker.contains("observer.disconnect()"));
        assertTrue(marker.contains("if(!side){item.button.style.visibility='hidden';return;}"));
        assertFalse(marker.contains("m.appendChild"));
        assertFalse(marker.contains("new HifzPrefs("));
    }
    @Test public void passiveClockPausesBeforeTheSelectorAndResumesOnCancellation()throws Exception{
        String hifz=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int start=hifz.indexOf("private void selectRevisionCorrespondence");
        int pause=hifz.indexOf("clock.pause();",start);
        int select=hifz.indexOf("CorrespondenceComparisonActivity.chooseCase(",start);
        assertTrue(start>=0&&pause>start&&select>pause);
        assertTrue(hifz.contains("if(!opened&&wasRunning)resumeSessionClock();"));
    }
    @Test public void activeComparisonUsesValidatedAnchorIndependentOfClosingFolio()throws Exception{
        String hifz=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(hifz.contains("verse.equals(validatedCorrespondenceVerse)"));
        assertTrue(hifz.contains("openValidatedCorrespondence"));
        assertTrue(hifz.contains("state.putString(\"correspondenceValidated\""));
    }
    @Test public void activeAnchorMustBeInsideTheValidatedTraversalPrefix(){
        com.quransafeguard.hifz.core.VerseRef start=new com.quransafeguard.hifz.core.VerseRef(2,46);
        com.quransafeguard.hifz.core.VerseRef anchor=new com.quransafeguard.hifz.core.VerseRef(2,47);
        com.quransafeguard.hifz.core.VerseRef end=new com.quransafeguard.hifz.core.VerseRef(2,48);
        java.util.List<com.quransafeguard.hifz.core.VerseRef> traversal=
            java.util.Arrays.asList(start,anchor,end);
        if(!CorrespondenceCases.enabled()){
            assertNull(CorrespondenceCases.lastValidatedAnchor(traversal,end));
            return;
        }
        assertNull(CorrespondenceCases.lastValidatedAnchor(traversal,start));
        assertEquals(anchor,CorrespondenceCases.lastValidatedAnchor(traversal,anchor));
        assertEquals(anchor,CorrespondenceCases.lastValidatedAnchor(traversal,end));
        assertNull(CorrespondenceCases.lastValidatedAnchor(traversal,
            new com.quransafeguard.hifz.core.VerseRef(2,49)));
    }
    @Test public void readerAndReviewGateNoRecitationLeaks()throws Exception{
        String reader=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String hifz=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String compare=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceComparisonActivity.java");
        String manifest=source("hifz-app/src/main/AndroidManifest.xml");
        assertTrue(reader.contains("if(verse==null||freeActiveMask)return;"));
        assertTrue(reader.contains("CorrespondenceGutterMarkers.show(mushaf,simRows,corrRows)"));
        assertTrue(hifz.contains("if(!canCompareCorrespondenceVerse(verse))return;"));
        assertTrue(hifz.contains("RevisionAtlasAccess.allowed(active,sessionCompleted,"));
        assertTrue(hifz.contains("clock.pause();"));
        assertTrue(hifz.contains("CorrespondenceGutterMarkers.show(mushaf,atlasRows,corrRows)"));
        assertTrue(manifest.contains("android:name=\".CorrespondenceComparisonActivity\" android:exported=\"false\""));
        assertTrue(compare.contains("new MapQcfThumbnails(this"));
        assertTrue(compare.contains("surface.setAtlasSinglePageCompact(true)"));
        assertTrue(compare.contains("TafsirRepository.Entry entry=tafsir.load(verse)"));
        assertFalse(compare.contains("new HifzPrefs("));
        assertFalse(compare.contains("setSabqiProgress"));
        assertFalse(compare.contains("setItqanProgress"));
        assertFalse(compare.contains("setMurajaah"));
    }
}