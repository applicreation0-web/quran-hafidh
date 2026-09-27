package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pure question generator/scorer for the adaptive spatial quiz (P2). The Mushaf itself is the
 * memory palace: every question is grounded in a real ACQUIRED verse's real page/line, never an
 * invented locus. This class has no Android or HifzPrefs dependency — it operates entirely over
 * an in-memory ACQUIRED candidate snapshot and adaptive stats the caller hands it, and never
 * writes Hifz progression. One instance is meant to live for a single quiz session, since the
 * post-error cooldown (never re-asking the just-missed question immediately) is session-scoped.
 */
final class SpatialQuizEngine {

    enum Kind { TEXT_TO_POSITION, POSITION_TO_TEXT, TRANSITION }

    enum Verdict { EXACT, ALMOST, REVIEW }

    /** One real, ACQUIRED verse/line — the raw material questions are built from. */
    static final class Candidate {
        final VerseRef verse;
        final int page;
        final String targetLineId;
        final String arabicSnippet;

        Candidate(VerseRef verse, int page, String targetLineId, String arabicSnippet) {
            this.verse = verse;
            this.page = page;
            this.targetLineId = targetLineId;
            this.arabicSnippet = arabicSnippet;
        }
    }

    static final class Question {
        final String id;
        final Kind kind;
        /** What is actually displayed: the snippet to tap (TEXT_TO_POSITION), the position shown
            (POSITION_TO_TEXT), or the passage ending shown (TRANSITION). */
        final Candidate prompt;
        /** The candidate that constitutes a correct answer. Equal to prompt for
            TEXT_TO_POSITION/POSITION_TO_TEXT; the real successor (a different candidate) for
            TRANSITION. */
        final Candidate answer;
        /** Empty for TEXT_TO_POSITION (the answer is a tap, not a choice). */
        final List<Candidate> choices;

        Question(String id, Kind kind, Candidate prompt, Candidate answer, List<Candidate> choices) {
            this.id = id;
            this.kind = kind;
            this.prompt = prompt;
            this.answer = answer;
            this.choices = choices;
        }
    }

    static final class Answer {
        final Verdict verdict;
        final long responseMs;

        Answer(Verdict verdict, long responseMs) {
            this.verdict = verdict;
            this.responseMs = responseMs;
        }
    }

    /**
     * Adaptive history for one question/landmark. Mirrored 1:1 by SpatialQuizStore's persisted
     * JSON — this is the shared, dependency-free shape both sides read/write. lastVerdict alone
     * drives the draw weight (section 6's "poids proposé" table is about the most recent
     * outcome); the cumulative counters exist for the SOLIDE/À SURVEILLER/FRAGILE/TRÈS FRAGILE
     * classification a caller may want to surface, not for weighting itself.
     */
    static final class Stats {
        int presentations;
        int exact;
        int almost;
        int review;
        Verdict lastVerdict;
        long lastResponseMs;
        long lastShownEpochDay;
    }

    /** Fast/slow threshold for the "exact rapide" vs "exact lente" weight split. */
    private static final long SLOW_RESPONSE_MS = 6000;
    private static final int COOLDOWN_MIN = 3;
    private static final int COOLDOWN_MAX = 6;

    private final List<Candidate> textToPositionPool;
    private final List<Candidate> positionToTextPool;
    private final List<TransitionPair> transitionPool;
    private final Map<String, Stats> history;
    private final Random random;
    private final Deque<String> cooldown = new ArrayDeque<>();

    /** A verse whose canonical successor is also fully ACQUIRED — pre-computed by the caller so
        the engine never has to reach for QuranCanon/EligibleCorpus itself. */
    static final class TransitionPair {
        final Candidate from;
        final Candidate to;

        TransitionPair(Candidate from, Candidate to) {
            this.from = from;
            this.to = to;
        }
    }

    SpatialQuizEngine(
        List<Candidate> textToPositionPool,
        List<Candidate> positionToTextPool,
        List<TransitionPair> transitionPool,
        Map<String, Stats> history,
        Random random
    ) {
        this.textToPositionPool = new ArrayList<>(textToPositionPool);
        this.positionToTextPool = new ArrayList<>(positionToTextPool);
        this.transitionPool = new ArrayList<>(transitionPool);
        this.history = history;
        this.random = random;
    }

    static String questionId(Kind kind, String targetLineId) {
        return kind.name() + ':' + targetLineId;
    }

    private double weightFor(String questionId) {
        if (cooldown.contains(questionId)) return 0.0;
        Stats s = history.get(questionId);
        if (s == null || s.lastVerdict == null) return 1.0;
        switch (s.lastVerdict) {
            case REVIEW: return 4.0;
            case ALMOST: return 2.0;
            case EXACT: return s.lastResponseMs > SLOW_RESPONSE_MS ? 1.5 : 0.5;
            default: return 1.0;
        }
    }

    private <T> T weightedPick(List<T> items, java.util.function.Function<T, String> idOf) {
        if (items.isEmpty()) return null;
        double total = 0;
        double[] weights = new double[items.size()];
        for (int i = 0; i < items.size(); i++) {
            weights[i] = weightFor(idOf.apply(items.get(i)));
            total += weights[i];
        }
        if (total <= 0) {
            // Everything eligible is on cooldown: fall back to a plain uniform draw rather than
            // returning no question at all.
            return items.get(random.nextInt(items.size()));
        }
        double draw = random.nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < items.size(); i++) {
            acc += weights[i];
            if (draw <= acc) return items.get(i);
        }
        return items.get(items.size() - 1);
    }

    /** Picks the next question, favoring the kinds whose pool is non-empty; returns null only
        when every pool is empty (nothing ACQUIRED yet to ask about). */
    Question next() {
        List<Kind> available = new ArrayList<>();
        if (!textToPositionPool.isEmpty()) available.add(Kind.TEXT_TO_POSITION);
        if (positionToTextPool.size() >= 3) available.add(Kind.POSITION_TO_TEXT);
        if (!transitionPool.isEmpty()) available.add(Kind.TRANSITION);
        if (available.isEmpty()) return null;
        Kind kind = available.get(random.nextInt(available.size()));
        switch (kind) {
            case TEXT_TO_POSITION: {
                Candidate c = weightedPick(textToPositionPool, cand -> questionId(Kind.TEXT_TO_POSITION, cand.targetLineId));
                return new Question(questionId(Kind.TEXT_TO_POSITION, c.targetLineId), Kind.TEXT_TO_POSITION, c, c, Collections.emptyList());
            }
            case POSITION_TO_TEXT: {
                Candidate c = weightedPick(positionToTextPool, cand -> questionId(Kind.POSITION_TO_TEXT, cand.targetLineId));
                List<Candidate> choices = threeChoices(c, positionToTextPool, c);
                return new Question(questionId(Kind.POSITION_TO_TEXT, c.targetLineId), Kind.POSITION_TO_TEXT, c, c, choices);
            }
            default: {
                TransitionPair pair = weightedPick(transitionPool, p -> questionId(Kind.TRANSITION, p.from.targetLineId));
                List<Candidate> choices = threeChoices(pair.to, positionToTextPool, pair.from);
                return new Question(questionId(Kind.TRANSITION, pair.from.targetLineId), Kind.TRANSITION, pair.from, pair.to, choices);
            }
        }
    }

    /** The correct answer plus up to 2 distractors drawn from the ACQUIRED pool, excluding the
        candidate the prompt itself displays (a passage can't be its own distractor). */
    private List<Candidate> threeChoices(Candidate correct, List<Candidate> distractorPool, Candidate excludePrompt) {
        List<Candidate> distractors = new ArrayList<>(distractorPool);
        distractors.remove(correct);
        distractors.remove(excludePrompt);
        Collections.shuffle(distractors, random);
        List<Candidate> choices = new ArrayList<>();
        choices.add(correct);
        for (int i = 0; i < 2 && i < distractors.size(); i++) choices.add(distractors.get(i));
        Collections.shuffle(choices, random);
        return choices;
    }

    /** Call after a question is shown, whatever the eventual verdict, so the cooldown window is
        measured in "questions since shown" rather than wall-clock time. */
    private void armCooldownIfMissed(String questionId, Verdict verdict) {
        if (verdict == Verdict.EXACT) return;
        cooldown.addLast(questionId);
        int window = COOLDOWN_MIN + random.nextInt(COOLDOWN_MAX - COOLDOWN_MIN + 1);
        while (cooldown.size() > window) cooldown.removeFirst();
    }

    Answer scorePosition(Question q, String tappedLineId, long responseMs, List<GeometryRepository.LineMeta> pageLines) {
        Verdict verdict;
        if (q.answer.targetLineId.equals(tappedLineId)) {
            verdict = Verdict.EXACT;
        } else {
            verdict = adjacentLine(q.answer.targetLineId, tappedLineId, pageLines) ? Verdict.ALMOST : Verdict.REVIEW;
        }
        armCooldownIfMissed(q.id, verdict);
        return new Answer(verdict, responseMs);
    }

    private boolean adjacentLine(String targetLineId, String tappedLineId, List<GeometryRepository.LineMeta> pageLines) {
        Integer targetIndex = null, tappedIndex = null;
        for (GeometryRepository.LineMeta line : pageLines) {
            if (line.id.equals(targetLineId)) targetIndex = line.lineIndexOnPage;
            if (line.id.equals(tappedLineId)) tappedIndex = line.lineIndexOnPage;
        }
        return targetIndex != null && tappedIndex != null && Math.abs(targetIndex - tappedIndex) == 1;
    }

    Answer scoreChoice(Question q, VerseRef selected, long responseMs) {
        Verdict verdict = selected != null && selected.equals(q.answer.verse) ? Verdict.EXACT : Verdict.REVIEW;
        armCooldownIfMissed(q.id, verdict);
        return new Answer(verdict, responseMs);
    }
}
