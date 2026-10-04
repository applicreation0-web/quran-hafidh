package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only Quiz projection over the canonical schema6 Progression snapshot.
 * Eligibility is recomputed from learned ∪ stabilized ∪ acquired and is never persisted.
 */
final class QuizCorpus {
    enum Mode { MIXED, CONTINUE, PREVIOUS }

    private final GeometryRepository geometry;
    private final WordGeometryRepository words;

    QuizCorpus(GeometryRepository geometry, WordGeometryRepository words) {
        if (geometry == null || words == null) throw new IllegalArgumentException("quiz repositories required");
        this.geometry = geometry;
        this.words = words;
    }

    List<VerseRef> eligibleVerses(HifzPrefs.ProgressionSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("progression snapshot required");
        LinkedHashSet<String> eligibleLines = new LinkedHashSet<>();
        eligibleLines.addAll(snapshot.learned);
        eligibleLines.addAll(snapshot.stabilized);
        eligibleLines.addAll(snapshot.acquired);

        LinkedHashMap<VerseRef, Boolean> fullyEligible = new LinkedHashMap<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            boolean lineEligible = eligibleLines.contains(line.id);
            for (VerseRef verse : line.verses) {
                Boolean previous = fullyEligible.get(verse);
                fullyEligible.put(verse, (previous == null || previous) && lineEligible);
            }
        }

        ArrayList<VerseRef> result = new ArrayList<>();
        for (Map.Entry<VerseRef, Boolean> entry : fullyEligible.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) result.add(entry.getKey());
        }
        result.sort((a, b) -> Integer.compare(GeometryRepository.ordinal(a), GeometryRepository.ordinal(b)));
        return Collections.unmodifiableList(result);
    }

    List<QuizQuestion> questions(HifzPrefs.ProgressionSnapshot snapshot, Mode mode, int maxCount) {
        if (mode == null || maxCount < 1) throw new IllegalArgumentException("quiz request");
        List<VerseRef> eligible = eligibleVerses(snapshot);
        Set<VerseRef> eligibleSet = new HashSet<>(eligible);
        ArrayList<QuizQuestion> candidates = new ArrayList<>();

        for (VerseRef verse : eligible) {
            int page = singlePage(verse);
            if (page < 1) continue;
            int wordCount = words.wordCountForVerse(page, verse);
            if ((mode == Mode.MIXED || mode == Mode.CONTINUE) && wordCount > 3) {
                candidates.add(new QuizQuestion(QuizQuestion.Type.CONTINUE, verse, verse, page, page));
            }
            if (mode == Mode.MIXED || mode == Mode.PREVIOUS) {
                VerseRef previous = GeometryRepository.previous(verse);
                if (previous == null || previous.getSurah() != verse.getSurah() || !eligibleSet.contains(previous)) continue;
                int previousPage = singlePage(previous);
                if (previousPage < 1 || wordCount < 1 || words.wordCountForVerse(previousPage, previous) < 1) continue;
                candidates.add(new QuizQuestion(QuizQuestion.Type.PREVIOUS, verse, previous, page, previousPage));
            }
        }

        Collections.shuffle(candidates);
        if (candidates.size() > maxCount) candidates.subList(maxCount, candidates.size()).clear();
        return Collections.unmodifiableList(candidates);
    }

    private int singlePage(VerseRef verse) {
        int first = geometry.firstLineIndex(verse);
        int last = geometry.lastLineIndex(verse);
        int page = geometry.line(first).page;
        return geometry.line(last).page == page ? page : -1;
    }
}
