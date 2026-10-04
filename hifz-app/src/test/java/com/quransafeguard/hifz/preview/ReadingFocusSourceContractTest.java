package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Locks the shared, bracket-free reading focus used by Apprentissage/Stabilisation and both snowballs. */
public final class ReadingFocusSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void focusUsesExactDueLinesWithNativeActiveTextAndFaintRealContext() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String focus = method(reader, "function focusContextLayer(", "\nfunction render(");
        assertTrue(focus.contains("lineClip.id='hifz-focus-lines'"));
        assertTrue("boundary verses must be clipped to the exact physical lines due",
            focus.contains("holes.setAttribute('clip-path','url(#hifz-focus-lines)')"));
        assertTrue("active Quran text is a hole in the paper veil, not a grey fill",
            focus.contains("hole.setAttribute('fill','black')"));
        assertTrue("BOOX keeps about 28% real surrounding-page context",
            focus.contains("paper.setAttribute('fill-opacity',eink?'0.72':'0.65')"));
        assertFalse("the old grey exact-line overlay must be gone", reader.contains("linefocuscell"));
    }

    @Test public void noWorkBlockBracketsRemainAnywhereInTheRuntime() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(reader.contains("workBlockBoundaryLayer"));
        assertFalse(reader.contains("workbracketlayer"));
        assertFalse(mushaf.contains("setWorkBlockBounds"));
        assertFalse(session.contains("setWorkBlockBounds"));
    }

    @Test public void theSameFocusCoversSabqiItqanRenforcementAndConsolidationWithoutChangingMasks() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String focusModes = method(session, "private boolean usesReadingFocus()", "\n    private void showCurrent()");
        assertTrue(focusModes.contains("SABQI.equals(mode)"));
        assertTrue(focusModes.contains("ITQAN.equals(mode)"));
        assertTrue(focusModes.contains("RECENT_SABQI_REVIEW.equals(mode)"));
        assertTrue(focusModes.contains("LEARNING_CONSOLIDATION.equals(mode)"));
        assertTrue(focusModes.contains("CONSOLIDATION_FINAL.equals(mode)"));
        assertTrue(focusModes.contains("LEARNING_FINAL.equals(mode)"));
        String grouped = method(session, "private void renderGroupedCycle(", "private void completeGroupedCycleRep() {");
        assertTrue("Renforcement/Consolidation keep their no-gomme protocol", grouped.contains("currentMask = 0;"));
        assertTrue("frozen physical units remain the only source of their exact reading zone",
            grouped.contains("ConsolidationPhysicalUnitPolicy.decodeLineUnit"));
        assertFalse("grouped sessions must not repurpose Itqan's fractionation state",
            grouped.contains("fractionatedItqan = true"));
    }

    @Test public void itqanFunctionalFractionationStillExistsButNoLongerControlsVisualFocus() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("fractionatedItqan = itqanBlockCount > 1;"));
        assertTrue(session.contains("if (fractionatedItqan)"));
        String show = method(session, "private void showCurrent()", "\n    private void goPage(");
        assertTrue(show.contains("boolean contextFocus=usesReadingFocus()&&!currentLineIds.isEmpty();"));
        assertTrue(show.contains("mushaf.show(currentPage,currentSelection,currentLineIds,displayedMask(),contextFocus);"));
        assertFalse(show.contains("fractionatedItqan"));
    }

    /** User decision: a thin hairline frames the zone of interest, above the paper eraser. */
    @Test public void zoneOfInterestHasAThinHairlineFrame() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue(reader.contains("function focusOutline(svg,activeLines,polys){"));
        assertTrue(reader.contains("const outline=focusOutline(svg,lines,maskFollowsSelection?selectedPolygons(svg):[]);"));
        assertTrue(index.contains(".focusoutline{fill:none;stroke:var(--sidemark);stroke-width:.7;"));
    }
}
