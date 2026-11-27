package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts declared verse ranges to complete, bounded physical learning spans in the configured order. */
final class UniversalLearningRoute {
    static final class Span {
        final int first;
        final int last;
        Span(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    private final List<Span> spans;

    private UniversalLearningRoute(List<Span> spans) {
        this.spans = spans;
    }

    static List<VerseRange> parse(String input) {
        List<VerseRange> ranges = new ArrayList<>();
        if (input.trim().isEmpty()) return ranges;
        for (String part : input.trim().split("[,;\\n]+", -1)) {
            String[] endpoints = part.trim().split("-", -1);
            if (endpoints.length == 0 || endpoints.length > 2) throw new IllegalArgumentException("Invalid range");
            VerseRef start = GeometryRepository.parseVerse(endpoints[0].trim());
            VerseRef end = endpoints.length == 1 ? start : GeometryRepository.parseVerse(endpoints[1].trim());
            ranges.add(new VerseRange(start, end));
        }
        return ranges;
    }

    static UniversalLearningRoute of(List<VerseRange> ranges, GeometryRepository geometry) {
        List<Span> spans = new ArrayList<>();
        for (VerseRange range : ranges) {
            int first = geometry.firstLineIndex(range.getStart());
            int last = geometry.lastLineIndex(range.getEndInclusive());
            if (first < 0 || last < first || last >= geometry.lineCount())
                throw new IllegalArgumentException("Invalid QCF boundaries");
            for (int index = first; index <= last; index++) {
                for (VerseRef verse : geometry.line(index).verses) {
                    if (!range.contains(verse))
                        throw new IllegalArgumentException("Range cuts a QCF line");
                }
            }
            for (Span prior : spans) {
                if (first <= prior.last && prior.first <= last)
                    throw new IllegalArgumentException("Overlapping learning ranges");
            }
            spans.add(new Span(first, last));
        }
        return new UniversalLearningRoute(Collections.unmodifiableList(spans));
    }

    Span at(int rangeIndex) {
        if (rangeIndex < 0 || rangeIndex > spans.size())
            throw new IllegalStateException("Invalid learning range checkpoint");
        return rangeIndex == spans.size() ? null : spans.get(rangeIndex);
    }

    int remaining(int rangeIndex, int cursor) {
        Span current = at(rangeIndex);
        if (current == null) return 0;
        int count = Math.max(0, current.last - Math.max(cursor, current.first) + 1);
        for (int i = rangeIndex+1; i < spans.size(); i++) {
            Span span = spans.get(i);
            count += span.last - span.first + 1;
        }
        return count;
    }
}
