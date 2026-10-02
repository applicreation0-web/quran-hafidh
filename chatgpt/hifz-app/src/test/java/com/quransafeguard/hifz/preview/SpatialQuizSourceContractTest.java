package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level safety contract for the optional acquired-only spatial quiz. */
public final class SpatialQuizSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void quizReadsOnlyRealAcquiredLinesAndNeverWritesProgression() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        assertTrue(source.contains("HifzPrefs.ProgressionSnapshot snapshot = prefs.progressionSnapshotV6()"));
        assertTrue(source.contains("snapshot.acquired"));
        assertFalse(source.contains("effectiveItqanRanges"));
        assertFalse(source.contains("itqanRanges()"));
        assertFalse(source.contains("transitionV6Lines"));
        assertFalse(source.contains("completeStabilization"));
        assertFalse(source.contains("validateLearningFinalReview"));
        assertFalse(source.contains("validateConsolidationFinalReview"));
        assertTrue(source.contains("aucun changement de progression"));
    }

    @Test public void timerStartsOnlyAfterAValidQuestionAndIsCappedAtFifteenMinutes() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        assertTrue(source.contains("QUIZ_LIMIT_MS = 15L * 60L * 1000L"));
        assertTrue(source.contains("if (nextQuestion()) startTimerAfterFirstValidQuestion();"));
        assertTrue(source.contains("acquiredLines.isEmpty()"));
        assertTrue(source.contains("Quiz indisponible"));
    }

    @Test public void quizUsesExactMushafLineGeometryNotAnApproximateGrid() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(activity.contains("GeometryRepository.LineMeta"));
        assertTrue(activity.contains("mushaf.setQuizGuide"));
        assertTrue(mushaf.contains("pageGeometryJson(requestedPage)"));
        assertTrue(reader.contains("function quizGuideLayer(svg)"));
        assertTrue(reader.contains("const line=(pageGeo.lines||[]).find"));
        assertTrue(reader.contains("quizVisibleLines.has(lineId)"));
        assertTrue(reader.contains("Fail closed: whitespace/out-of-band taps are not snapped"));
        assertFalse("placement taps must never select a nearest line",
            reader.contains("lines.reduce((best,item)"));
        assertFalse(activity.contains("GridLayout"));
        assertFalse(activity.contains("15x"));
    }

    @Test public void revisionSelectorAddsQuizWithoutAddingAnotherHomeCard() throws Exception {
        String selector = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/RevisionSelector.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(selector.contains("Quiz spatial · ≤15 min · facultatif"));
        assertTrue(selector.contains("void openSpatialQuiz()"));
        assertTrue(main.contains("startActivity(new Intent(this, SpatialQuizActivity.class))"));
        assertFalse("no second permanent quiz tile on the home screen",
            main.contains("Ui.modeCard(this, \"\", \"Quiz spatial\""));
    }

    @Test public void manifestKeepsQuizPrivate() throws Exception {
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\".SpatialQuizActivity\""));
        int at = manifest.indexOf("android:name=\".SpatialQuizActivity\"");
        String local = manifest.substring(at, Math.min(manifest.length(), at + 260));
        assertTrue(local.contains("android:exported=\"false\""));
    }
}
