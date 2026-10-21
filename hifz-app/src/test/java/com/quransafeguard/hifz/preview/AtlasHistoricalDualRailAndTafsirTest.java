package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards the screenshot-approved 17 October Atlas flow; no Hifz state writes. */
public final class AtlasHistoricalDualRailAndTafsirTest {
    private static String code(String leaf)throws Exception {
        Path p=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/",leaf);
        if(!Files.exists(p))p=Paths.get("..",p.toString());
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
    @Test public void originalDualRailAndSourcedPassageSubtitlesStayVisible()throws Exception {
        String map=code("MawdudiMapScreen.java");
        assertTrue(map.contains("axis.addView(overview?overviewGraph:graph,"));
        assertTrue(map.contains("private final class Graph extends View"));
        assertTrue(map.contains("private float passageX("));
        assertTrue(map.contains("passageSubtitle.setText(graph.selectedTitle())"));
        assertTrue(map.contains("selected=next;") || map.contains("selected=n;"));
        assertTrue(map.contains("AtlasPassageContext.intersectionStart(p.firstAyah,unit.start)"));
        assertTrue(map.contains("AtlasPassageContext.intersectionEnd(p.lastAyah,unit.end)"));
        assertTrue(map.contains("thumbnails.showPages(first,last,page,surah,focusStart,focusEnd,false,"));
        assertTrue(map.contains(".putExtra(\"map114PassageId\",current().id)"));
        assertFalse("No invented Quran passage segments",map.contains("display_only_subnodes"));
    }
    @Test public void oldOutlinePrefixIsHiddenButRealTitleRemains() {
        assertEquals("They are transgressors",MawdudiMapScreen.cleanPassageCaption("c) They are transgressors"));
        assertEquals("They are transgressors",MawdudiMapScreen.cleanPassageCaption("C) They are transgressors"));
        assertEquals("Judgment by Revelation",MawdudiMapScreen.cleanPassageCaption("Judgment by Revelation"));
        assertEquals("The Creator and His Revelation",MawdudiMapScreen.cleanPassageCaption("The Creator and His Revelation"));
    }
    @Test public void jalalaynUsesEmbeddedPanelWithoutReplacingMushaf()throws Exception {
        String reader=code("StudyReaderActivity.java");
        String dialog=code("MapJalalaynDialog.java");
        assertTrue(reader.contains("readerStack.addView(bottomTafsir"));
        assertTrue(reader.contains("panelHost,()->{"));
        assertTrue(reader.contains("if(mapPreviewJalalayn.accepts(verse))mapPreviewJalalayn.selectVerse(verse);"));
        assertTrue(reader.contains("MawdudiMapScreen.cleanPassageCaption(title)"));
        assertTrue(dialog.contains("if(embeddedHost!=null) {"));
        assertTrue(dialog.contains("embeddedHost.addView(shell,"));
        assertTrue(dialog.contains("void selectVerse(VerseRef verse)"));
    }
}
