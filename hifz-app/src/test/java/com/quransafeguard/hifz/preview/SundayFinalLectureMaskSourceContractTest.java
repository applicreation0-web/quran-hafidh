package com.quransafeguard.hifz.preview;

import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Source-only guard that Sunday finales share Lecture's actual first-paint mask. */
public final class SundayFinalLectureMaskSourceContractTest {
    private static String source(String path) throws Exception {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }
    @Test public void sundayUsesTheExactSameAyahThemesCueIndexAndAtomicReaderAsLecture() throws Exception {
        String activity=source("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String lecture=source("src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(lecture.contains("anchors=ayahThemesCues.forPage(page);"));
        assertTrue(lecture.contains("mushaf.showFreeActivePreparedPage(page,pageLines,landmarks,anchors,"));
        assertTrue(activity.contains("anchors = ayahThemesTitles.forPage(currentPage);"));
        assertTrue(activity.contains("WordGeometryRepository.shared(this).pageLandmarkBoxes(currentPage)"));
        assertTrue(activity.contains("mushaf.showFreeActivePreparedPage(currentPage, pageLines, landmarks, anchors, ready);"));
        assertTrue(activity.contains("sundayFinalAnchorUnavailable = !ready;"));
        assertTrue(activity.contains("if (isSundayFinalPageRepetition() && currentMask == 100 && sundayFinalAnchorUnavailable)"));
        assertFalse(activity.contains("applySundayFinalAnchors()"));
    }
    @Test public void noSecondAmorceCorpusForSundayMasks() throws Exception {
        String activity=source("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int start=activity.indexOf("private void showSundayFinalLectureMaskPage()");
        int end=activity.indexOf("/** True when an anchored post-An-Nās page",start);
        assertTrue(start>0 && end>start);
        String implementation=activity.substring(start,end);
        assertFalse(implementation.contains("semanticPassages"));
        assertFalse(implementation.contains("new WordBox"));
        assertTrue(implementation.contains("currentLineIds"));
    }
}
