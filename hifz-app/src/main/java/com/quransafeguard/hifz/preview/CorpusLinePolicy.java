package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure schema-6 policy separating physical display coverage from exclusive line ownership. */
public final class CorpusLinePolicy {
    private CorpusLinePolicy() {}

    public static VerseRef ownerVerse(GeometryRepository.LineMeta line) {
        if (line == null || line.verses == null || line.verses.isEmpty()) {
            throw new IllegalStateException("Mushaf line must contain at least one verse");
        }
        VerseRef owner = line.verses.get(0);
        for (int i = 1; i < line.verses.size(); i++) {
            VerseRef candidate = line.verses.get(i);
            if (candidate.compareTo(owner) < 0) owner = candidate;
        }
        return owner;
    }

    public static Set<String> ownedLineIds(
            List<VerseRange> ranges,
            List<GeometryRepository.LineMeta> lines) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (ranges == null || ranges.isEmpty() || lines == null) {
            return Collections.unmodifiableSet(result);
        }
        for (GeometryRepository.LineMeta line : lines) {
            if (contains(ranges, ownerVerse(line))) result.add(line.id);
        }
        return Collections.unmodifiableSet(result);
    }

    public static Set<String> touchedLineIds(
            List<VerseRange> ranges,
            List<GeometryRepository.LineMeta> lines) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (ranges == null || ranges.isEmpty() || lines == null) {
            return Collections.unmodifiableSet(result);
        }
        for (GeometryRepository.LineMeta line : lines) {
            ownerVerse(line); // enforce the non-empty-verse invariant
            for (VerseRef verse : line.verses) {
                if (contains(ranges, verse)) {
                    result.add(line.id);
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static boolean contains(List<VerseRange> ranges, VerseRef verse) {
        for (VerseRange range : ranges) {
            if (range != null && range.contains(verse)) return true;
        }
        return false;
    }
}
