package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Wiring of the latest user decision (ITQĀN FULL ONLY / POST-AN-NĀS) that JVM tests can't reach. */
public final class ItqanFullOnlyPostNasSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) n++;
        return n;
    }

    @Test public void lightIsOnlyEverNamedToFinishALegacyOpenUnit() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertEquals("the only LIGHT left in HifzPrefs is itqanUnitPlan's legacy inference",
            1, count(prefs, "AnchoringQueue.ItqanProtocol.LIGHT"));
        int plan = prefs.indexOf("ItqanRegimeStore.UnitPlan itqanUnitPlan(AnchoringQueue.Entry entry) {");
        int light = prefs.indexOf("AnchoringQueue.ItqanProtocol.LIGHT");
        assertTrue(plan >= 0 && light > plan && light < prefs.indexOf("\n    }", plan));
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse("the session never reads a queue entry's protocol directly any more",
            session.contains("itqanSessionProtocol = anchoringEntry.protocol;"));
        assertTrue(session.contains("ItqanRegimeStore.UnitPlan plan = prefs.itqanUnitPlan(anchoringEntry);"));
    }

    @Test public void sessionUsesTheRegimeForRepsMasksValidationAndCompletion() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("itqanTargetReps = ItqanMaintenancePolicy.totalReps(itqanRegime, itqanSessionProtocol);"));
        assertFalse(session.contains("PreviewConfig.itqanMaskForNextRep("));
        assertFalse(session.contains("PreviewConfig.isItqanValidationRep("));
        assertTrue(session.contains("ItqanMaintenancePolicy.isValidationRep(itqanRegime, itqanSessionProtocol, rep - 1)"));
        assertTrue("maintenance is one whole ≤15-line unit, never 8/7/7 sub-blocks",
            session.contains("? Collections.singletonList(new StabilizationHalfPagePolicy.Unit("));
        assertTrue(session.contains("? prefs.completeItqanMaintenanceUnitV6(currentLineIds, itqanUnit.start, itqanUnit.end, next,"));
    }

    @Test public void anchoredRecallReusesTheAuditedLandmarkHalfLinesOnRealGeometry() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int start = session.indexOf("private void applyItqanAnchors() {");
        assertTrue(start >= 0);
        String method = session.substring(start, session.indexOf("\n    }", start));
        assertTrue("only during the ten anchored repetitions at 100%",
            method.contains("currentMask == 100") && method.contains("ItqanMaintenancePolicy.anchoredRecallRep(itqanRegime, prefs.itqanRep())"));
        assertTrue("the existing Révision active landmark mechanism, not a new cue source",
            method.contains("mushaf.setLandmarkLines(onPage.get(0), onPage.size() > 1 ? onPage.get(onPage.size() - 1) : null);"));
        assertTrue("anchors come from the unit's own real physical lines on the shown page",
            method.contains("geometry.linesForExactIds(currentLineIds)") && method.contains("line.page == currentPage"));
        assertTrue("every other repetition clears them", method.contains("mushaf.setLandmarkLines(null, null);"));
        assertTrue(session.contains("if(ITQAN.equals(mode))applyItqanAnchors();"));
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(reader.contains("function landmarkCellIndices(cellCount,role){"));
    }

    @Test public void itqanCompletionNeverWritesSabqi() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        int start = prefs.indexOf("boolean completeItqanMaintenanceUnitV6(");
        String method = prefs.substring(start, prefs.indexOf("\n    }\n", start));
        assertFalse(method.toLowerCase(java.util.Locale.ROOT).contains("sabqi"));
        assertFalse(method.contains("Family.LEARNING"));
        String store = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanRegimeStore.java");
        assertFalse(store.toLowerCase(java.util.Locale.ROOT).contains("\"sabqi"));
    }
}
