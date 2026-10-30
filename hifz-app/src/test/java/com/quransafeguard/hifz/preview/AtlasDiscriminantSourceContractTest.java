package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** No fake Arabic, no internet, no Hifz state, and authentic two-page QCF vector rendering. */
public final class AtlasDiscriminantSourceContractTest {
    @Test public void pairedInlineSvgClipsHaveDistinctSourceScopedIds()throws Exception{
        String source=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QcfAtlasVerseStrips.java");
        assertFalse("Inline paired SVG cannot share a clip id",source.contains("id='atlas-word-window'"));
        assertTrue(source.contains("target.getSurah()+\"-\"+target.getAyah()"));
    }
    static String read(String path) throws Exception {
        Path p=Paths.get(path);
        if(!Files.exists(p))p=Paths.get("..",path);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
    @Test public void atlasEntryIsPrivateAndUsesCanonicalPageNavigation() throws Exception {
        String map=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MawdudiMapScreen.java");
        String manifest=read("hifz-app/src/main/AndroidManifest.xml");
        String activity=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantActivity.java");
        // Contextual comparison is entered from Lecture/Révision, not the Atlas home.
        assertFalse(map.contains("new Intent(activity,AtlasDiscriminantActivity.class)"));
        assertFalse(map.contains("\"Versets ressemblants\""));
        assertTrue(manifest.contains("android:name=\".AtlasDiscriminantActivity\" android:exported=\"false\""));
        assertTrue(activity.contains("geometry.pageForVerse(c.first)"));
        assertTrue(activity.contains("geometry.pageForVerse(c.second)"));
        assertTrue(activity.contains("new Intent(this,StudyReaderActivity.class)"));
        assertTrue(activity.contains("StudyReaderActivity.EXTRA_JUMP_VERSE"));
        assertFalse(activity.contains("new HifzPrefs("));
        assertFalse(activity.contains("completeActiveMurajaah"));
        assertFalse(activity.contains("setSabqiProgress"));
        assertFalse(activity.contains("setItqanProgress"));
    }
    @Test public void lectureContextLinkKeepsQcfAndHifzImmutable() throws Exception {
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String catalog=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java");
        assertTrue(reader.contains("discriminantButton=Ui.iconButton(this"));
        assertTrue(reader.contains("AtlasDiscriminantCases.indexFor(selected.getSurah(),selected.getAyah())>=0"));
        assertTrue(reader.contains("boolean available=!freeActiveMask&&selected!=null"));
        assertTrue(reader.contains("GeometryRepository.get(this).pageForVerse(source)!=page"));
        assertTrue(reader.contains("startActivity(new Intent(this,AtlasDiscriminantActivity.class)"));
        assertTrue(reader.contains("updateDiscriminantButton();"));
        assertTrue(catalog.contains("return -1;"));
        assertFalse(reader.contains("new HifzSessionActivity()"));
    }
    @Test public void lectureGutterMarkerIsSourceRestrictedAndNeverModifiesQcf() throws Exception {
        String marker=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantMarkers.java");
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String mushaf=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue(marker.contains("AtlasDiscriminantCases.PILOT"));
        assertTrue(marker.contains("geometry.pageForVerse(v)==page"));
        assertTrue(marker.contains("var side=left>=32?'left':right>=32?'right':null"));
        assertTrue(marker.contains("m.querySelectorAll('.ayahPolygon')"));
        assertTrue(marker.contains("HifzNative.comparisonTap"));
        assertTrue(reader.contains("onAtlasDiscriminantTap(VerseRef verse)"));
        assertTrue(reader.contains("if(verse==null||freeActiveMask"));
        assertTrue(reader.contains("AtlasDiscriminantMarkers.show(mushaf,shown"));
        assertTrue(mushaf.contains("default void onAtlasDiscriminantTap(VerseRef verse)"));
        assertFalse(marker.contains("HifzPrefs"));
        assertFalse(marker.contains("putInt("));
        assertFalse(marker.contains("innerHTML=svg"));
        assertFalse(marker.contains("mushaf.appendChild"));
    }

    @Test public void exactQcfAndWordCoordinatesOnly() throws Exception {
        String thumbnails=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MapQcfThumbnails.java");
        String excerpts=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QcfAtlasVerseStrips.java");
        String activity=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantActivity.java");
        assertTrue(thumbnails.contains("mushaf/hafs/kfqc/svg-br/%03d.svg.br"));
        assertTrue(thumbnails.contains("setAtlasSinglePageCompact"));
        assertTrue(thumbnails.contains("path.ayahPolygon[surah="));
        assertTrue(thumbnails.contains("svg.querySelector('path.ayahPolygon')"));
        assertTrue(excerpts.contains("svg.querySelector('path.ayahPolygon')"));
        assertFalse(thumbnails.contains("paths<20"));
        assertFalse(excerpts.contains("length<20"));
        assertTrue(excerpts.contains("MapQcfThumbnails.readSvg(getContext(),page)"));
        assertTrue(excerpts.contains("WordGeometryRepository.shared(getContext()).wordsForPage(page)"));
        assertTrue(excerpts.contains("lastPageForVerse(verse,geometry)==page"));
        assertTrue(excerpts.contains("return \"\"; // no guess for invalid source annotations"));
        assertTrue(activity.contains("left.showPages("));
        assertTrue(activity.contains("right.showPages("));
        // Excerpts must load only after both full-size QCF thumbnails reported image.onload.
        assertTrue(activity.contains("if(leftPageStatus==1 && rightPageStatus==1 && strips!=null)"));
        assertTrue(activity.contains("strips.show(entry.first,currentLeftPage,entry.firstWordIndices,"));
        assertFalse(excerpts.contains("java.net."));
        assertFalse(excerpts.contains("http://"));
    }
    @Test public void noExternalCorpusOrGeneratedExplanations() throws Exception {
        String catalog=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java");
        String activity=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantActivity.java");
        assertFalse(catalog.contains("Tarteel"));
        assertFalse(catalog.contains("similar_ayah.json"));
        assertFalse(activity.contains("ChatCompletion"));
        assertTrue(activity.contains("c.explanation"));
        assertTrue(activity.contains("c.sourceUrl"));
        assertFalse(activity.contains("class RelationRail extends View"));
        assertTrue(activity.contains("leftRead.setText(\"Lire \"+c.first+\" en Lecture\")"));
        assertTrue(activity.contains("rightRead.setText(\"Lire \"+c.second+\" en Lecture\")"));
        assertTrue(activity.contains("Ui.dp(this,compact?106:126)"));
        assertFalse(activity.contains("position.setText("));
    }
}
