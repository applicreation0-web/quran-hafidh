package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SpatialQuizEngine is pure (no Android/HifzPrefs dependency) so it's tested directly against
 * in-memory candidate pools, exactly as a real caller would hand it an already-filtered ACQUIRED
 * snapshot. Building that snapshot from real HifzPrefs/GeometryRepository state is a separate,
 * Android-dependent concern tested elsewhere.
 */
public final class SpatialQuizEngineTest {
    private static List<SpatialQuizEngine.Candidate> pool(int count) {
        List<SpatialQuizEngine.Candidate> pool = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pool.add(new SpatialQuizEngine.Candidate(new VerseRef(2, 70 + i), 11, "line-" + i, "snippet" + i));
        }
        return pool;
    }

    private static List<GeometryRepository.LineMeta> linesFor(List<SpatialQuizEngine.Candidate> pool) {
        List<GeometryRepository.LineMeta> lines = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            lines.add(new GeometryRepository.LineMeta(i, "line-" + i, 11, i, i * 10, i * 10 + 9, Collections.emptyList()));
        }
        return lines;
    }

    private static List<SpatialQuizEngine.TransitionPair> transitions(List<SpatialQuizEngine.Candidate> pool) {
        List<SpatialQuizEngine.TransitionPair> pairs = new ArrayList<>();
        for (int i = 0; i < pool.size() - 1; i++) pairs.add(new SpatialQuizEngine.TransitionPair(pool.get(i), pool.get(i + 1)));
        return pairs;
    }

    @Test public void textToPositionOffersNoChoicesAndScoresAnExactTapAsExact() {
        List<SpatialQuizEngine.Candidate> pool = pool(6);
        SpatialQuizEngine engine = new SpatialQuizEngine(pool, Collections.emptyList(), Collections.emptyList(),
            new HashMap<>(), new Random(1));
        SpatialQuizEngine.Question q = engine.next();
        assertEquals(SpatialQuizEngine.Kind.TEXT_TO_POSITION, q.kind);
        assertTrue("TEXT_TO_POSITION's answer is a tap, not a choice", q.choices.isEmpty());
        SpatialQuizEngine.Answer answer = engine.scorePosition(q, q.answer.targetLineId, 900, linesFor(pool));
        assertEquals(SpatialQuizEngine.Verdict.EXACT, answer.verdict);
    }

    @Test public void adjacentLineScoresAlmostAndAnythingElseScoresReview() {
        List<SpatialQuizEngine.Candidate> pool = pool(6);
        SpatialQuizEngine engine = new SpatialQuizEngine(pool, Collections.emptyList(), Collections.emptyList(),
            new HashMap<>(), new Random(2));
        SpatialQuizEngine.Question q = engine.next();
        int idx = Integer.parseInt(q.answer.targetLineId.substring("line-".length()));
        String neighborId = "line-" + (idx == pool.size() - 1 ? idx - 1 : idx + 1);
        SpatialQuizEngine.Answer almost = engine.scorePosition(q, neighborId, 1200, linesFor(pool));
        assertEquals(SpatialQuizEngine.Verdict.ALMOST, almost.verdict);
        SpatialQuizEngine.Answer review = engine.scorePosition(q, "not-a-real-line", 1200, linesFor(pool));
        assertEquals(SpatialQuizEngine.Verdict.REVIEW, review.verdict);
    }

    @Test public void positionToTextOffersUpToThreeChoicesIncludingTheCorrectOne() {
        List<SpatialQuizEngine.Candidate> pool = pool(8);
        SpatialQuizEngine engine = new SpatialQuizEngine(Collections.emptyList(), pool, Collections.emptyList(),
            new HashMap<>(), new Random(3));
        SpatialQuizEngine.Question q = engine.next();
        assertEquals(SpatialQuizEngine.Kind.POSITION_TO_TEXT, q.kind);
        assertTrue(q.choices.size() >= 1 && q.choices.size() <= 3);
        assertTrue("the correct answer must always be one of the offered choices", q.choices.contains(q.answer));
        SpatialQuizEngine.Answer correct = engine.scoreChoice(q, q.answer.verse, 500);
        assertEquals(SpatialQuizEngine.Verdict.EXACT, correct.verdict);
        VerseRef wrong = new VerseRef(114, 1);
        SpatialQuizEngine.Answer wrongAnswer = engine.scoreChoice(q, wrong, 500);
        assertEquals(SpatialQuizEngine.Verdict.REVIEW, wrongAnswer.verdict);
    }

    @Test public void transitionsAnswerIsTheCanonicalSuccessorNotThePromptItself() {
        List<SpatialQuizEngine.Candidate> pool = pool(6);
        SpatialQuizEngine engine = new SpatialQuizEngine(Collections.emptyList(), Collections.emptyList(),
            transitions(pool), new HashMap<>(), new Random(4));
        SpatialQuizEngine.Question q = engine.next();
        assertEquals(SpatialQuizEngine.Kind.TRANSITION, q.kind);
        assertFalse("the successor shown as the answer must not be the same passage as the prompt",
            q.answer.targetLineId.equals(q.prompt.targetLineId));
        assertTrue(q.choices.contains(q.answer));
        SpatialQuizEngine.Answer correct = engine.scoreChoice(q, q.answer.verse, 500);
        assertEquals(SpatialQuizEngine.Verdict.EXACT, correct.verdict);
    }

    @Test public void aMissedQuestionDoesNotResurfaceDuringItsOwnCooldownWindow() {
        List<SpatialQuizEngine.Candidate> pool = pool(6);
        Map<String, SpatialQuizEngine.Stats> history = new HashMap<>();
        SpatialQuizEngine engine = new SpatialQuizEngine(pool, Collections.emptyList(), Collections.emptyList(),
            history, new Random(7));
        SpatialQuizEngine.Question missed = engine.next();
        String missedId = missed.id;
        engine.scorePosition(missed, "not-a-real-line", 500, linesFor(pool));
        for (int i = 0; i < 3; i++) {
            SpatialQuizEngine.Question q = engine.next();
            assertFalse("a just-missed question must not be re-asked within its cooldown window",
                q.id.equals(missedId));
            engine.scorePosition(q, q.answer.targetLineId, 500, linesFor(pool));
        }
    }

    @Test public void neverCrashesWhenEveryPoolIsEmpty() {
        SpatialQuizEngine engine = new SpatialQuizEngine(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), new HashMap<>(), new Random(9));
        assertEquals(null, engine.next());
    }

    @Test public void positionToTextNeedsAtLeastThreeCandidatesBeforeItIsOffered() {
        // Only 2 ACQUIRED candidates: not enough for a real 3-way choice, so POSITION_TO_TEXT
        // must never be picked, but the still-viable TEXT_TO_POSITION-shaped pool keeps working.
        List<SpatialQuizEngine.Candidate> tiny = pool(2);
        SpatialQuizEngine engine = new SpatialQuizEngine(Collections.emptyList(), tiny, Collections.emptyList(),
            new HashMap<>(), new Random(11));
        assertEquals(null, engine.next());
    }
}
