package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure, offline gate for QUL Similar Ayah / Mutashabihat references.
 *
 * No QUL dataset is bundled here: export licensing must first be checked separately.
 * This policy never constructs Quran text or authorizes a guessed word-to-QCF alignment.
 * A future QCF comparison can consume only source-verified word coordinates.
 */
final class QulSimilarityPolicy {
    enum Origin { SIMILAR_AYAH, MUTASHABIHAT }

    static final class Candidate {
        final VerseRef verse;
        final Origin origin;
        final int score;
        final int matchedWords;
        final int sourceCoverage;
        Candidate(VerseRef verse, Origin origin, int score, int matchedWords, int coverage) {
            this.verse = verse;
            this.origin = origin;
            this.score = score;
            this.matchedWords = matchedWords;
            this.sourceCoverage = coverage;
        }
    }

    private QulSimilarityPolicy() {}

    /**
     * Restrictive top-3 gate: QUL's raw score alone is not a guarantee of a useful
     * memorization comparison. No self-match, duplicate or low-coverage short phrase.
     * Same-surah repetitions are deliberately allowed.
     */
    static List<Candidate> shortlist(VerseRef source, Collection<Candidate> rows) {
        if (source == null || rows == null || rows.isEmpty()) return Collections.emptyList();
        Map<VerseRef, Candidate> byVerse = new LinkedHashMap<>();
        for (Candidate candidate : rows) {
            if (candidate == null || candidate.verse == null || candidate.origin == null
                    || source.equals(candidate.verse) || candidate.score < 0 || candidate.score > 100
                    || candidate.sourceCoverage < 0 || candidate.sourceCoverage > 100
                    || candidate.matchedWords < 4
                    || candidate.sourceCoverage < 40 || candidate.score < 70) continue;
            Candidate previous = byVerse.get(candidate.verse);
            if (previous == null || comparator().compare(candidate, previous) < 0)
                byVerse.put(candidate.verse, candidate);
        }
        List<Candidate> sorted = new ArrayList<>(byVerse.values());
        sorted.sort(comparator());
        if (sorted.size() > 3) sorted = new ArrayList<>(sorted.subList(0, 3));
        return Collections.unmodifiableList(sorted);
    }

    private static Comparator<Candidate> comparator() {
        return Comparator.comparingInt((Candidate row) -> row.score).reversed()
            .thenComparing(Comparator.comparingInt((Candidate row) -> row.sourceCoverage).reversed())
            .thenComparing(Comparator.comparingInt((Candidate row) -> row.matchedWords).reversed())
            .thenComparingInt(row -> row.verse.getSurah())
            .thenComparingInt(row -> row.verse.getAyah());
    }
}
