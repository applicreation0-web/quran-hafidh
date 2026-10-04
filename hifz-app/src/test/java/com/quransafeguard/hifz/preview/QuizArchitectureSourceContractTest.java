package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guardrails for the isolated, self-assessed free Quiz. */
public final class QuizArchitectureSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void quizReadsExactlyTheSameSchema6SetsAsProgressionAndNeverEffectiveRanges() throws Exception {
        String corpus = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizCorpus.java");
        assertTrue(corpus.contains("eligibleLines.addAll(snapshot.learned)"));
        assertTrue(corpus.contains("eligibleLines.addAll(snapshot.stabilized)"));
        assertTrue(corpus.contains("eligibleLines.addAll(snapshot.acquired)"));
        assertTrue(corpus.contains("fullyEligible.put(verse, (previous == null || previous) && lineEligible)"));
        assertFalse(corpus.contains("effectiveItqanRanges"));
        assertFalse(corpus.contains("transitionV6Lines"));
    }

    @Test public void quizCannotWriteProgressionRevisionOrWeakSpots() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java");
        String corpus = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizCorpus.java");
        String combined = activity + corpus;
        assertFalse(combined.contains("transitionV6Lines"));
        assertFalse(combined.contains("completeSabqi"));
        assertFalse(combined.contains("completeItqan"));
        assertFalse(combined.contains("completeConsolidation"));
        assertFalse(combined.contains("toggleMurajaahWeakVerse"));
        assertFalse(combined.contains("murajaahWeakVerses"));
        assertTrue(activity.contains("prefs.progressionSnapshotV6()"));
    }

    @Test public void promptsUseRealMushafWordGeometryAndNoSpeechRecognitionOrWriting() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java");
        String words = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WordGeometryRepository.java");
        assertTrue(activity.contains("words.boxesForVerse"));
        assertTrue(activity.contains("firstThree.put(all.optJSONArray(i))"));
        assertTrue(activity.contains("MediaRecorder"));
        assertTrue(activity.contains("getCacheDir()"));
        assertFalse(activity.contains("SpeechRecognizer"));
        assertFalse(activity.contains("RecognizerIntent"));
        assertFalse(activity.contains("EditText"));
        assertTrue(words.contains("word.surah == verse.getSurah() && word.ayah == verse.getAyah()"));
    }

    @Test public void previousQuestionRequiresBothVersesEligibleAndStaysInsideSurah() throws Exception {
        String corpus = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizCorpus.java");
        assertTrue(corpus.contains("previous.getSurah() != verse.getSurah()"));
        assertTrue(corpus.contains("!eligibleSet.contains(previous)"));
    }

    @Test public void freeQuizIsASeparateHomeEntryAndManifestOnlyAddsMicrophonePermission() throws Exception {
        String home = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue(home.contains("navLine(root, \"Quiz\", \"\", v -> startActivity(new Intent(this, QuizActivity.class)));"));
        assertTrue(home.contains("new Intent(this, QuizActivity.class)"));
        assertTrue(manifest.contains("android.permission.RECORD_AUDIO"));
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"));
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"));
        assertTrue(manifest.contains("android:name=\".QuizActivity\""));
    }

    @Test public void quizHistoryIsSeparateAndBounded() throws Exception {
        String history = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizHistory.java");
        assertTrue(history.contains("quran_hifz_quiz_v1"));
        assertTrue(history.contains("MAX_ATTEMPTS = 200"));
        assertFalse(history.contains("HifzPrefs"));
    }

    /** Spec UI pass 2, §13: page bounds (not verse bounds), contextual icons, temp audio purge. */
    @Test public void quizShowsRealPageBoundsAndOnlyContextualIconActions() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java");
        assertTrue(activity.contains("JSONArray pageBounds = words.pageLandmarkBoxes(question.promptPage);"));
        assertTrue("both page bounds are mandatory, else no question", activity.contains("if (pageBounds.length() != 6) return new JSONArray();"));
        assertTrue(activity.contains("private void updateQuestionActions() {"));
        assertFalse("no boxed buttons left in the Quiz", activity.contains("Ui.smallButton(") || activity.contains("Ui.button("));
        assertTrue(activity.contains("private void purgeQuizRecordings() {"));
    }
}
