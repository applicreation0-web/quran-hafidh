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
        assertTrue(session.contains("itqanTargetReps = ItqanMaintenancePolicy.totalReps(itqanUnitPlan);"));
        assertTrue("Real persisted post-Nas stamp decides ×3 or legacy ×4",
            session.contains("ItqanRegimeStore.UnitPlan plan = prefs.itqanUnitPlan(anchoringEntry);"));
        assertFalse(session.contains("PreviewConfig.itqanMaskForNextRep("));
        assertFalse(session.contains("PreviewConfig.isItqanValidationRep("));
        assertTrue(session.contains("ItqanMaintenancePolicy.isValidationRep(itqanUnitPlan, rep - 1)"));
        assertTrue("maintenance is one whole hizb unit, never 8/7/7 sub-blocks",
            session.contains("? Collections.singletonList(new StabilizationHalfPagePolicy.Unit("));
        assertTrue(session.contains("? prefs.completeItqanMaintenanceUnitV6(currentLineIds, itqanUnit.start, itqanUnit.end, next,"));
    }

    @Test public void anchoredRecallUsesIbnKathirQcfAmorcesAndFailsOpenWithoutExactHoles() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int start = session.indexOf("private void applyItqanAnchors() {");
        assertTrue(start >= 0);
        String method = session.substring(start, session.indexOf("\n    }\n", start));
        assertTrue("only during the maintained post-An-Nās anchored repetitions at 100%",
            method.contains("currentMask == 100") && method.contains("ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, prefs.itqanRep())"));
        assertTrue("Ibn Kathīr cues of the unit, as exact QCF holes in the paper mask",
            method.contains("semanticPassages.readerCuesForPage(currentPage)")
                && method.contains("ItqanMaintenancePolicy.amorceInsideUnit(")
                && method.contains("mushaf.setSemanticCues(cues, true, false);"));
        assertTrue("never the page first/last three words in this regime", method.contains("mushaf.clearPageLandmarkBoxes();")
            && !method.contains("semanticPassages.pageLandmarkBoxes("));
        assertTrue("an amorce without exact boxes makes the page fail open",
            method.contains("cue.getJSONArray(\"boxes\").length() != cue.getInt(\"anchorWordCount\")")
                && method.contains("if (cues.length() == 0) itqanAnchorFailOpen = true;")
                && session.contains("if (ITQAN.equals(mode) && itqanAnchorFailOpen) return 0;"));
        assertTrue(session.contains("if(ITQAN.equals(mode))applyItqanAnchors();"));
        assertTrue("no old half-line landmark in Itqān", method.contains("mushaf.setLandmarkLines(null, null);"));
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

    @Test public void homeCardCueIsRefreshedOnEveryReturnToHome() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertEquals(1, count(main, "navLine(path, \"Stabilisation\", stabilizationProgramCue(),"));
        assertEquals(1, count(main, "if (itqanCue != null) itqanCue.setText(stabilizationProgramCue());"));
        assertTrue("Post-Nas summary stays a concise hizb cue",
            main.contains("prefs.itqanPostNasMaintenance() ? \"1 hizb · entretien\""));
        assertTrue("Both weekly menu goals are expressed in pages, without session counts",
            main.contains("2 pages/semaine") && main.contains("1 page/semaine"));
        assertFalse("Detailed session count belongs inside Itqan",main.contains("2 pages / 4 séances"));
        assertFalse("A detailed weekly quota belongs inside Itqan, not its menu",
            main.contains("30 lignes/semaine · 4 séances"));
    }
}
