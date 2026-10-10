package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Quiz corpus on the real KFQC line geometry and the audited quran-ws v1.1.2 word boxes. */
public final class QuizCorpusTest {
    private static GeometryRepository geometry;
    private static final Map<Integer, List<WordGeometryRepository.WordBox>> WORDS = new HashMap<>();

    private static Path repo(String repoPath) {
        Path direct = Paths.get(repoPath);
        return Files.exists(direct) ? direct : Paths.get("..", repoPath);
    }

    @BeforeClass public static void load() throws Exception {
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(
            repo("app/src/main/assets/reader109/geometry.json")), StandardCharsets.UTF_8));
        for (String chunk : new String[]{"001-150", "151-300", "301-450", "451-604"}) {
            WORDS.putAll(WordGeometryRepository.parseChunk(new String(Files.readAllBytes(
                repo("hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-" + chunk + ".json")),
                StandardCharsets.UTF_8)));
        }
    }

    private static QuizCorpus corpus() {
        return new QuizCorpus(geometry, (page, verse) -> WordGeometryRepository.wordCount(WORDS, page, verse));
    }

    private static HifzPrefs.ProgressionSnapshot snapshot(List<String> learned, List<String> stabilized, List<String> acquired) {
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("schema", 6);
        store.disk.put("v6LearnedLineIds", new JSONArray(learned).toString());
        store.disk.put("v6StabilizedLineIds", new JSONArray(stabilized).toString());
        store.disk.put("v6AcquiredCreditLineIds", new JSONArray(acquired).toString());
        store.disk.put("v6QuarantineLineIds", "[]");
        store.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
        return new HifzPrefs(store).progressionSnapshotV6();
    }

    @Test public void everyPageHasExactWordsAndVerseWordCountsMatchTheirKeys() {
        assertEquals(604, WORDS.size());
        int total = 0;
        for (List<WordGeometryRepository.WordBox> page : WORDS.values()) total += page.size();
        assertEquals(77432, total);
        assertEquals(4, WordGeometryRepository.wordCount(WORDS, 1, new VerseRef(1, 1)));
    }

    @Test public void aVerseIsEligibleOnlyWhenEveryPhysicalLineItTouchesIsInTheCorpus() {
        List<String> lines = CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(67, 1), new VerseRef(67, 30), geometry);
        List<String> partial = new ArrayList<>(lines.subList(0, lines.size() - 1));
        List<VerseRef> eligible = corpus().eligibleVerses(snapshot(partial, new ArrayList<>(), new ArrayList<>()));
        assertFalse(eligible.isEmpty());
        Set<String> corpusLines = new HashSet<>(partial);
        for (VerseRef verse : eligible) {
            for (int i = geometry.firstLineIndex(verse); i <= geometry.lastLineIndex(verse); i++) {
                if (geometry.line(i).verses.contains(verse))
                    assertTrue(verse + " touches a line outside the corpus", corpusLines.contains(geometry.line(i).id));
            }
        }
        assertFalse("67:30 sits on the excluded last line", eligible.contains(new VerseRef(67, 30)));
    }

    @Test public void learnedStabilizedAndAcquiredAllCountAndNothingElse() {
        List<String> a = CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(78, 1), new VerseRef(78, 40), geometry);
        List<String> b = CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(79, 1), new VerseRef(79, 46), geometry);
        List<String> c = CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(80, 1), new VerseRef(80, 42), geometry);
        List<VerseRef> eligible = corpus().eligibleVerses(snapshot(a, b, c));
        assertTrue(eligible.contains(new VerseRef(78, 10)));
        assertTrue(eligible.contains(new VerseRef(79, 10)));
        assertTrue(eligible.contains(new VerseRef(80, 10)));
        assertFalse(eligible.contains(new VerseRef(81, 1)));
        assertTrue(corpus().eligibleVerses(snapshot(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())).isEmpty());
    }

    /**
     * Regression: a new Quiz series must re-read the schema6 progression,
     * retaining week-1 verses while admitting newly learned week-2 material.
     * This tests the unchanged 1.17.1 QuizCorpus, not a new quiz engine.
     */
    @Test public void corpusGrowsAcrossWeeksFromLiveProgressionWithoutRecreditOrMutation() {
        List<String> firstWeek =
            CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(78, 1), new VerseRef(78, 40), geometry);
        List<String> secondWeek =
            CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(79, 1), new VerseRef(79, 46), geometry);

        HifzPrefs.ProgressionSnapshot week1 =
            snapshot(firstWeek, new ArrayList<>(), new ArrayList<>());
        HifzPrefs.ProgressionSnapshot week2 =
            snapshot(secondWeek, firstWeek, new ArrayList<>());
        HifzPrefs.ProgressionSnapshot week3 =
            snapshot(new ArrayList<>(), secondWeek, firstWeek);

        QuizCorpus quiz = corpus();
        List<VerseRef> a = quiz.eligibleVerses(week1);
        List<VerseRef> b = quiz.eligibleVerses(week2);
        List<VerseRef> c = quiz.eligibleVerses(week3);

        assertFalse(a.isEmpty());
        assertTrue("new week must expand actual eligible corpus", b.size() > a.size());
        assertTrue("old week still eligible after stabilization", b.containsAll(a));
        assertEquals("moving learned -> stabilized -> acquired cannot shrink the corpus", b, c);
        assertFalse("not eligible before learning", a.contains(new VerseRef(79, 10)));
        assertTrue("newly learned surah eligible in a fresh quiz", b.contains(new VerseRef(79, 10)));
        assertTrue("week 1 survives week 3", c.contains(new VerseRef(78, 10)));

        // Series questions are constructed from the supplied current snapshot;
        // no stale week-1 question cache is carried into a later new series.
        List<QuizQuestion> week1Questions =
            quiz.questions(week1, QuizCorpus.Mode.MIXED, 1000);
        List<QuizQuestion> week2Questions =
            quiz.questions(week2, QuizCorpus.Mode.MIXED, 1000);
        assertFalse(week1Questions.isEmpty());
        assertTrue(week2Questions.size() > week1Questions.size());
        for (QuizQuestion q : week2Questions) {
            assertTrue("prompt must be part of actual progression", b.contains(q.prompt));
            assertTrue("expected verse must be eligible", b.contains(q.expected));
        }
    }

    @Test public void questionsRespectContinueAndPreviousRules() {
        List<String> lines = CorpusLinePolicy.ownedLineIdsForRangeOnPage(new VerseRef(2, 1), new VerseRef(2, 141), geometry);
        HifzPrefs.ProgressionSnapshot snap = snapshot(new ArrayList<>(), new ArrayList<>(), lines);
        Set<VerseRef> eligible = new HashSet<>(corpus().eligibleVerses(snap));
        List<QuizQuestion> questions = corpus().questions(snap, QuizCorpus.Mode.MIXED, 400);
        assertFalse(questions.isEmpty());
        boolean sawContinue = false, sawPrevious = false;
        for (QuizQuestion q : questions) {
            assertEquals("V1: single-page prompt", geometry.line(geometry.firstLineIndex(q.prompt)).page,
                geometry.line(geometry.lastLineIndex(q.prompt)).page);
            assertTrue(eligible.contains(q.prompt));
            if (q.type == QuizQuestion.Type.CONTINUE) {
                sawContinue = true;
                assertEquals(q.prompt, q.expected);
                assertTrue("more than three words", WordGeometryRepository.wordCount(WORDS, q.promptPage, q.prompt) > 3);
            } else {
                sawPrevious = true;
                assertTrue(eligible.contains(q.expected));
                assertEquals("never across a surah boundary", q.prompt.getSurah(), q.expected.getSurah());
                assertEquals(GeometryRepository.previous(q.prompt), q.expected);
            }
        }
        assertTrue(sawContinue);
        assertTrue(sawPrevious);
        assertTrue(corpus().questions(snap, QuizCorpus.Mode.MIXED, 10).size() <= 10);
        for (QuizQuestion q : corpus().questions(snap, QuizCorpus.Mode.PREVIOUS, 50))
            assertEquals(QuizQuestion.Type.PREVIOUS, q.type);
    }
}
