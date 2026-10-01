package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
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

    /**
     * Exact physical lines owned by a verse range, in Mushaf order — the range may span more than
     * one physical page (a Stabilisation weekly unit can now run to ~1.5 pages). A shared line
     * belongs to the earliest verse printed on it, so a boundary verse starting mid-line never
     * steals a line already owned by the preceding range.
     */
    public static List<String> ownedLineIdsForRangeOnPage(
            VerseRef start,
            VerseRef endInclusive,
            GeometryRepository geometry) {
        if (start == null || endInclusive == null || geometry == null) {
            throw new IllegalArgumentException("Owned physical range requires bounds and geometry");
        }
        if (start.compareTo(endInclusive) > 0) {
            throw new IllegalArgumentException("Owned physical range is reversed");
        }
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            VerseRef owner = ownerVerse(line);
            if (owner.compareTo(start) >= 0 && owner.compareTo(endInclusive) <= 0) {
                result.add(line.id);
            }
        }
        return Collections.unmodifiableList(result);
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
