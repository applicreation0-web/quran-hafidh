package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Read-only source guard for the documentary Carte; never exercise Hifz progress. */
public final class IbnKathirMapSourceContractTest {
    private static String read(String name) throws Exception {
        Path path=Paths.get(name);
        if(!Files.exists(path))path=Paths.get("..",name);
        return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
    }
    @Test public void selectionIsExplicitAndStableAcrossScrolling() throws Exception {
        String source=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java");
        assertTrue(source.contains("groupsList.setOnItemClickListener"));
        assertTrue(source.contains("selectedGroupIndex=position;"));
        assertTrue(source.contains("setPreviewGroup(groups.get(position),0);"));
        assertFalse("Scrolling must not silently change the selected block",
            source.contains("previewFirstVisibleGroup") || source.contains("changePreview"));
        assertTrue(source.contains("selectedGroupIndex\",selectedGroupIndex"));
        assertTrue(source.contains("firstVisible"));
        assertTrue(source.contains("topOffset"));
        assertTrue(source.contains("previewPage"));
    }
    @Test public void oneTrueMushafRendersEveryVerifiedPageOfTheGroup() throws Exception {
        String source=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java");
        assertTrue(source.contains("new MushafView(this)"));
        assertTrue(source.contains("pagePreview.setHighlightVerses(exact)"));
        assertTrue(source.contains("geometry.pageForVerse(new VerseRef(group.surah,group.startAyah))"));
        assertTrue(source.contains("geometry.pageForVerse(new VerseRef(group.surah,group.endAyah))"));
        assertTrue(source.contains("turnPreviewPage(int delta)"));
        assertTrue(source.contains("onSurfaceTap() {openPreviewInMushaf();}"));
        assertTrue(source.contains("Bloc "));
        assertFalse(source.contains("al-Munir"));
    }
    @Test public void openingMapMushafIsReadOnlyAndHasReturnContext() throws Exception {
        String source=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java");
        assertTrue(source.contains("StudyReaderActivity.EXTRA_MAP_PREVIEW,true"));
        assertTrue(source.contains("StudyReaderActivity.EXTRA_JUMP_VERSE"));
        assertFalse(source.contains("transitionV6Lines"));
        assertFalse(source.contains("completeItqan"));
        assertFalse(source.contains("completeSabqi"));
        assertFalse(source.contains("HifzPrefs"));
    }
}
