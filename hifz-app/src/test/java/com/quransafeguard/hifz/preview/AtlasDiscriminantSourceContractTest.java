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
        assertFalse("Reader actions must not grow a second bottom chain button",reader.contains("discriminantButton=Ui.iconButton(this"));
        assertTrue(reader.contains("AtlasDiscriminantCases.versesOnPage(page)"));
        assertTrue(reader.contains("if(freeActiveMask)return;"));
        assertTrue(reader.contains("if(selected!=null&&onPage.contains(selected))"));
        assertTrue(reader.contains("!AtlasDiscriminantCases.versesOnPage(page).contains(source)"));
        assertTrue(reader.contains("startActivity(new Intent(this,AtlasDiscriminantActivity.class)"));
        assertTrue(reader.contains("updateDiscriminantButton();"));
        assertTrue(catalog.contains("indices.isEmpty()?-1:indices.get(0)"));
        assertFalse(reader.contains("new HifzSessionActivity()"));
    }
    @Test public void lectureGutterMarkerIsSourceRestrictedAndNeverModifiesQcf() throws Exception {
        String marker=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantMarkers.java");
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String mushaf=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue(marker.contains("AtlasDiscriminantCases.versesOnPage(page)"));
        assertFalse("Page rendering must use the index, not rescan the catalogue",marker.contains("AtlasDiscriminantCases.PILOT"));
        assertTrue(marker.contains("var side=left>=46?'left':right>=46?'right':null"));
        assertTrue(marker.contains("m.querySelectorAll('.ayahPolygon')"));
        assertTrue(marker.contains("HifzNative.comparisonTap"));
        assertTrue(reader.contains("onAtlasDiscriminantTap(VerseRef verse)"));
        assertTrue(reader.contains("if(verse==null||freeActiveMask"));
        assertTrue(reader.contains("if(!AtlasDiscriminantCases.versesOnPage(page).contains(verse))return;"));
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
    @Test public void oneCrispInterlockingChainIconOnEveryEligiblePage() throws Exception {
        String xml=read("hifz-app/src/main/res/drawable/ic_ui_atlas_similar.xml");
        String markers=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantMarkers.java");
        assertTrue(xml.contains("M10,13"));
        assertTrue(xml.contains("M14,11"));
        assertTrue(markers.contains("M10 13a5"));
        assertTrue(markers.contains("M14 11a5"));
        assertTrue(markers.contains("AtlasDiscriminantCases.versesOnPage(page)"));
        assertEquals("A page has one grouped chain marker",1,occurrences(markers,"document.createElement('button')"));
        assertTrue("Choose the verse anchor before creating one grouped marker",
            markers.indexOf("});if(!anchor||!chosen)return;") < markers.indexOf("document.createElement('button')"));
        assertTrue(markers.contains("width:44px;height:44px"));
        assertTrue(markers.contains("100vw - 96px"));
        assertFalse("HTML SVG attributes must not contain literal backslashes",markers.contains("\\\\\\\""));
        assertTrue(markers.contains("root.appendChild(btn)"));
        assertFalse(markers.contains("M2.5 5.5h6"));
    }
    @Test public void fullJalalaynOnBothSidesAndTrueQcfWords() throws Exception {
        String activity=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantActivity.java");
        String catalog=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java");
        assertTrue(activity.contains("loadBothTafsirs(selectedLeftVerse,selectedRightVerse);"));
        assertTrue(activity.contains("tafsirRepository.load(verse)"));
        assertTrue(activity.contains("loadOneTafsir(first,leftTafsir,0,leftToken)"));
        assertTrue(activity.contains("loadOneTafsir(second,rightTafsir,1,rightToken)"));
        assertTrue(activity.contains("selectExactTafsir(0,page,new VerseRef(surah,ayah))"));
        assertTrue(activity.contains("selectExactTafsir(1,page,new VerseRef(surah,ayah))"));
        assertTrue(activity.contains("leftTafsir.setVisibility(side==0?View.VISIBLE:View.GONE)"));
        assertTrue(activity.contains("rightTafsir.setVisibility(side==1?View.VISIBLE:View.GONE)"));
        assertTrue(activity.contains("tafsirPanel.setVisibility(View.VISIBLE)"));
        assertTrue(activity.contains("if(lateralTafsir)workspace.addView(tafsirPanel"));
        assertFalse("Tafsir must not be buried below the page comparison",activity.contains("tafsirColumn(tafsirs)"));
        assertTrue(activity.contains("token!=(side==0?leftTafsirRequest:rightTafsirRequest)"));
        String qcf=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MapQcfThumbnails.java");
        assertTrue(qcf.contains("poly.isPointInFill(local)"));
        assertTrue(qcf.contains("matrix.inverse()"));
        assertTrue(qcf.contains("generation==fullGeneration"));
        assertTrue(qcf.contains("listener.onVerseChosen(page,surah,ayah)"));
        assertTrue(activity.contains("strips.show(entry.first,currentLeftPage,entry.firstWordIndices,"));
        assertTrue(catalog.contains("new Entry(2,63,2,93"));
        assertFalse(activity.contains("new HifzPrefs("));
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
        assertTrue("QCF page fit is recomputed when the contextual Tafsir is opened",
            activity.contains("comparisonScroll.post(this::fitVisiblePages)"));
        assertFalse(activity.contains("position.setText("));
    }
    @Test public void narrowReadingPageHasVerifiedLinkWithoutManualVerseTap() throws Exception {
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(reader.contains("contextualDiscriminantVerse"));
        assertTrue(reader.contains("List<VerseRef> onPage=AtlasDiscriminantCases.versesOnPage(page)"));
        assertTrue(reader.contains("else if(!onPage.isEmpty())contextualDiscriminantVerse=onPage.get(0)"));
        assertTrue(reader.contains("return !freeActiveMask&&contextualDiscriminantVerse!=null"));
        assertTrue(reader.contains("if(!AtlasDiscriminantCases.versesOnPage(page).contains(source))return;"));
        assertTrue(reader.contains("if(freeActiveMask||contextualDiscriminantVerse==null)return;"));
        assertFalse(reader.contains("selected=contextualDiscriminantVerse"));
    }

    @Test public void approvedCatalogueCarriesProvenanceWithoutInventedExplanations()throws Exception{
        String catalog=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java");
        int checked=0;
        java.util.regex.Pattern quoted=java.util.regex.Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
        for(String line:catalog.split("\\n")){
            if(!line.trim().startsWith("new Entry("))continue;
            java.util.regex.Matcher values=quoted.matcher(line);
            java.util.List<String> fields=new java.util.ArrayList<>();
            while(values.find())fields.add(values.group(1));
            assertEquals("Every generated case retains kind, two references, explanation and provenance",6,fields.size());
            assertEquals("A catalogue reference must not invent a religious explanation","",fields.get(3));
            assertTrue("Book cases retain the exact source book provenance",
                fields.get(5).startsWith("https://quranpedia.app/books/b10901/") ||
                (fields.get(4).startsWith("Ajout explicite de l’utilisateur") && fields.get(5).isEmpty()
                    && fields.get(1).equals("2:63") && fields.get(2).equals("2:93")));
            checked++;
        }
        assertTrue("The approved runtime catalogue must replace the five-case pilot",checked>5);
    }

    private static int occurrences(String source,String token){
        int count=0;for(int i=0;(i=source.indexOf(token,i))>=0;i+=token.length())count++;
        return count;
    }
}
