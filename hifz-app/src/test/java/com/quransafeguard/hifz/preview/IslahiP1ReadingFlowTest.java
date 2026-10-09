package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.Assert.*;

public final class IslahiP1ReadingFlowTest {
    private String source(String path) throws Exception {
        java.nio.file.Path p = Paths.get(path);
        if (!Files.exists(p)) p = Paths.get("..", path);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test public void sourceCheckedOfflineNotesAreScopedToAnNasOnly() {
        assertTrue(IslahiPilotContent.hasEnglishReadingNotes(114, 1, 6));
        assertFalse(IslahiPilotContent.hasEnglishReadingNotes(9, 38, 42));
        assertFalse(IslahiPilotContent.hasEnglishReadingNotes(2, 63, 82));
        assertTrue(IslahiPilotContent.isMultiPageNavigationPilot(9, 38, 42));
        assertFalse(IslahiPilotContent.isMultiPageNavigationPilot(114, 1, 6));
        List<IslahiPilotContent.Section> sections = IslahiPilotContent.nasSections();
        assertEquals(3, sections.size());
        assertEquals(1, sections.get(0).firstAyah);
        for (int i=1; i<sections.size(); i++) {
            assertEquals(sections.get(i-1).lastAyah+1, sections.get(i).firstAyah);
        }
        assertEquals(6, sections.get(2).lastAyah);
        for (IslahiPilotContent.Section section : sections) {
            assertFalse(section.readingNote.trim().isEmpty());
            assertTrue(section.printedPages.startsWith("Printed p"));
        }
    }

    @Test public void readerPreservesPositionAndRoutesToMushafWithoutHifzPrefs() throws Exception {
        String reader=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IslahiTafsirActivity.java");
        assertTrue(reader.contains("readingScrollY"));
        assertTrue(reader.contains("StudyReaderActivity.EXTRA_JUMP_PAGE"));
        assertTrue(reader.contains("StudyReaderActivity.EXTRA_ISLAHI_SURAH"));
        assertTrue(reader.contains("StudyReaderActivity.EXTRA_ISLAHI_START"));
        assertTrue(reader.contains("StudyReaderActivity.EXTRA_ISLAHI_END"));
        assertFalse(reader.contains("new HifzPrefs("));
        assertFalse(reader.contains("Intent.ACTION_VIEW"));
        assertFalse(reader.contains("android.permission.INTERNET"));
    }

    @Test public void mapKeepsGrayscaleThumbsAndDedicatedReadingAction() throws Exception {
        String map=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IslahiMapActivity.java");
        assertTrue(map.contains("Read Tafsir  ›"));
        assertTrue(map.contains("IslahiTafsirActivity.forBlock("));
        assertTrue(map.contains("#595959"));
    }

    @Test public void hatchOverlayMustBeSeparateVisibleAndRecreated() throws Exception {
        String js=source("hifz-app/src/main/assets/hifzreader/reader.js");
        String mushaf=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String study=source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(js.contains("islahi-p1-hatching"));
        assertTrue(js.contains(".islahi-p1-hatching').forEach(n=>n.remove())"));
        assertTrue(js.contains("shape.setAttribute('fill-opacity', '1')"));
        assertTrue(js.contains("setIslahiHighlights(list)"));
        assertTrue(mushaf.contains("public void setIslahiHighlightVerses("));
        assertTrue(study.contains("mushaf.setIslahiHighlightVerses(verses);"));
        assertTrue(study.contains("Jalalayn · short commentary"));
    }
}
