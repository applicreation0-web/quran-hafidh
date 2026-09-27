package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the ACQUIRED candidate snapshot SpatialQuizEngine draws questions from. This is the
 * read-only bridge between real Hifz state (HifzPrefs, GeometryRepository, VerseText) and the
 * engine, which itself has zero knowledge of any of them.
 *
 * `HifzPrefs.activeMurajaahCorpus()` is only a range prefilter (Révision active's own "settled
 * material" ranges) — it is not proof that every physical line inside those ranges has actually
 * been credited. Each verse is only admitted once every physical line it spans is independently
 * confirmed ACQUIRED; any quarantine/legacy-partial/unresolved line anywhere in that verse
 * silently drops the whole verse from the pool rather than guessing. No corpus widening happens
 * here beyond what activeMurajaahCorpus() itself already allows.
 */
final class SpatialQuizCorpus {
    private SpatialQuizCorpus() {}

    static final class Snapshot {
        final List<SpatialQuizEngine.Candidate> candidates;
        final List<SpatialQuizEngine.TransitionPair> transitions;

        Snapshot(List<SpatialQuizEngine.Candidate> candidates, List<SpatialQuizEngine.TransitionPair> transitions) {
            this.candidates = candidates;
            this.transitions = transitions;
        }
    }

    static Snapshot build(HifzPrefs prefs, GeometryRepository geometry, VerseText verseText) {
        EligibleCorpus corpus = prefs.activeMurajaahCorpus();
        Map<String, SpatialQuizEngine.Candidate> byVerse = new LinkedHashMap<>();
        for (VerseRange range : corpus.getRanges()) {
            VerseRef cursor = range.getStart();
            while (true) {
                SpatialQuizEngine.Candidate candidate = candidateFor(cursor, prefs, geometry, verseText);
                if (candidate != null) byVerse.put(cursor.toString(), candidate);
                if (cursor.equals(range.getEndInclusive())) break;
                VerseRef advanced = QuranCanon.INSTANCE.next(cursor);
                if (advanced == null) break;
                cursor = advanced;
            }
        }

        List<SpatialQuizEngine.Candidate> candidates = new ArrayList<>(byVerse.values());

        List<SpatialQuizEngine.TransitionPair> transitions = new ArrayList<>();
        for (SpatialQuizEngine.Candidate from : candidates) {
            VerseRef successor = QuranCanon.INSTANCE.next(from.verse);
            if (successor == null) continue;
            SpatialQuizEngine.Candidate to = byVerse.get(successor.toString());
            if (to == null) continue;
            transitions.add(new SpatialQuizEngine.TransitionPair(from, to));
        }
        return new Snapshot(candidates, transitions);
    }

    /** Null whenever the verse can't be admitted without ambiguity: any support line not
        confirmed ACQUIRED, missing geometry, or missing verse text. */
    private static SpatialQuizEngine.Candidate candidateFor(
        VerseRef verse, HifzPrefs prefs, GeometryRepository geometry, VerseText verseText
    ) {
        int first, last;
        try {
            first = geometry.firstLineIndex(verse);
            last = geometry.lastLineIndex(verse);
        } catch (IllegalArgumentException noGeometry) {
            // Shouldn't happen for any canonical verse (every one of the 6236 appears somewhere
            // in the 604-page rendering) — kept only as a hard guard against a data gap.
            return null;
        }
        for (int i = first; i <= last; i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (!isAcquired(line.id, prefs)) return null;
        }
        GeometryRepository.LineMeta targetLine = geometry.line(first);
        String text = verseText.textFor(verse);
        if (text == null || text.isEmpty()) return null;
        String snippet = snippet(text);
        return new SpatialQuizEngine.Candidate(verse, targetLine.page, targetLine.id, snippet);
    }

    private static boolean isAcquired(String lineId, HifzPrefs prefs) {
        try {
            return prefs.progressStateV6(lineId) == HifzPrefs.ProgressState.ACQUIRED;
        } catch (IllegalStateException unresolved) {
            // Quarantine/legacy-partial/unknown all surface as a thrown, undifferentiated state
            // from progressStateV6 — any of them makes this line's status ambiguous, so the verse
            // it supports is excluded rather than guessed at.
            return false;
        }
    }

    /** 3-6 first words of the real Uthmani verse text, length-capped; never translit­erated. */
    static String snippet(String verseText) {
        String[] words = verseText.trim().split("\\s+");
        int count = Math.min(6, words.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        String result = sb.toString();
        final int maxLen = 60;
        return result.length() > maxLen ? result.substring(0, maxLen) : result;
    }
}
