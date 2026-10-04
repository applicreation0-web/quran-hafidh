package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Boox-stylus annotations: free-form pen marks (e.g. a pronunciation-error note) drawn directly on
 * the Mushaf page, like pencil marks on a physical copy, with undo and per-page clear. Available in
 * both Lecture (StudyReaderActivity) and every HifzSessionActivity mode — including Entretien
 * (MURAJAAH/MURAJAAH_ACTIVE) — so a note taken while reciting stays visible on later maintenance
 * passes over the same page, since AnnotationStore keys strokes by physical page, not by mode.
 */
public final class ReaderAnnotationSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void overlayOnlyHandlesStylusInputSoFingerNavigationStaysUndisturbed() throws Exception {
        String overlay = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AnnotationOverlayView.java");
        assertTrue("a non-stylus touch must fall through to MushafView beneath",
            overlay.contains("event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return false"));
        assertTrue("strokes must be normalized (0..1) so they survive a remeasure",
            overlay.contains("event.getX() / w") && overlay.contains("event.getY() / h"));
    }

    @Test public void storeUsesTheSameSharedPreferencesFileAsHifzPrefsSoBackupPicksItUpAutomatically() throws Exception {
        String store = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/AnnotationStore.java");
        assertTrue("must reuse HifzPrefs' own preview store, not a separate file HifzBackup would miss",
            store.contains("\"quran_hifz_preview_v1\""));
        assertTrue("each page's annotations must be keyed by physical page number",
            store.contains("KEY_PREFIX + page"));
    }

    @Test public void studyReaderWiresOverlayAboveMushafWithUndoAndClearControls() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue("overlay must sit in the same container as the Mushaf view, stacked above it",
            study.contains("mushafContainer.addView(mushaf,") && study.contains("mushafContainer.addView(annotationOverlay,"));
        assertTrue("page changes (navigation and swipes alike) must resync the overlay's page",
            study.contains("annotationOverlay.setPage(shown);"));
        assertTrue("learner must be able to undo the last stroke", study.contains("annotationOverlay.undoLastStroke()"));
        assertTrue("learner must be able to erase all annotations on the current page",
            study.contains("annotationOverlay.clearCurrentPage()"));
        assertFalse("annotation overlay must never be inserted directly into readerStack in place of the Mushaf container",
            study.contains("readerStack.addView(mushaf,"));
    }

    @Test public void hifzSessionWiresTheSameOverlaySoNotesStayVisibleDuringEntretien() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("Entretien shares the same mushaf field/layout as every other session mode, so wiring "
                + "the overlay once at the base layout covers MURAJAAH/MURAJAAH_ACTIVE with no per-mode branch",
            session.contains("mushafContainer.addView(mushaf,") && session.contains("mushafContainer.addView(annotationOverlay,"));
        assertTrue("page changes during any session mode must resync the overlay, since notes are keyed by physical page",
            session.contains("annotationOverlay.setPage(page);"));
        assertTrue("learner must be able to undo a note during a session", session.contains("annotationOverlay.undoLastStroke()"));
        assertTrue("learner must be able to erase a page's notes during a session", session.contains("annotationOverlay.clearCurrentPage()"));
        assertFalse("annotation overlay must never be inserted directly into root in place of the Mushaf container",
            session.contains("root.addView(mushaf,"));
    }
}
