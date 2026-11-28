package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts declared verse ranges to complete, bounded physical learning spans in the configured order. */
final class UniversalLearningRoute {
    private static final java.util.regex.Pattern UNIT_RANGE =
        java.util.regex.Pattern.compile("([phjl])\\s*(\\d+)(?:\\s*-\\s*(\\d+))?", java.util.regex.Pattern.CASE_INSENSITIVE);
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
        return parse(input, null);
    }

    static List<VerseRange> parse(String input, GeometryRepository geometry) {
        List<VerseRange> ranges = new ArrayList<>();
        if (input.trim().isEmpty()) return ranges;
        for (String part : input.trim().replace('–','-').replace('—','-').split("[,;\\n]+", -1)) {
            var unit = UNIT_RANGE.matcher(part.trim());
            if (unit.matches()) {
                int first = Integer.parseInt(unit.group(2));
                int last = unit.group(3) == null ? first : Integer.parseInt(unit.group(3));
                ranges.add(canonicalUnit(unit.group(1).toLowerCase(java.util.Locale.ROOT), first, last, geometry));
                continue;
            }
            String[] endpoints = part.trim().split("-", -1);
            if (endpoints.length == 0 || endpoints.length > 2) throw new IllegalArgumentException("Invalid range");
            VerseRef start = GeometryRepository.parseVerse(endpoints[0].trim());
            VerseRef end = endpoints.length == 1 ? start : GeometryRepository.parseVerse(endpoints[1].trim());
            ranges.add(new VerseRange(start, end));
        }
        return ranges;
    }

    private static VerseRange canonicalUnit(String unit, int first, int last, GeometryRepository geometry) {
        if (first < 1 || last < first) throw new IllegalArgumentException("Invalid Quran unit");
        if ("h".equals(unit) || "j".equals(unit)) {
            int limit = "h".equals(unit) ? 60 : 30;
            if (last > limit) throw new IllegalArgumentException("Invalid Quran unit");
            int rubsPerUnit = "h".equals(unit) ? 4 : 8;
            int[] start = QuranRubBoundaries.rowForIndex((first - 1) * rubsPerUnit + 1);
            VerseRef end = new VerseRef(114, 6);
            if (last < limit) {
                int[] next = QuranRubBoundaries.rowForIndex(last * rubsPerUnit + 1);
                QuranCanon canon = QuranCanon.INSTANCE;
                end = canon.fromOrdinal(canon.ordinal(new VerseRef(next[1], next[2])) - 1);
            }
            return new VerseRange(new VerseRef(start[1], start[2]), end);
        }
        if (geometry == null) throw new IllegalArgumentException("QCF geometry required");
        int firstLine = first - 1;
        int lastLine = last - 1;
        if ("p".equals(unit)) {
            if (last > 604) throw new IllegalArgumentException("Invalid page range");
            firstLine = -1;
            lastLine = -1;
            for (int i = 0; i < geometry.lineCount(); i++) {
                int page = geometry.line(i).page;
                if (page == first && firstLine == -1) firstLine = i;
                if (page == last) lastLine = i;
                if (page > last) break;
            }
        }
        if (firstLine < 0 || lastLine < firstLine || lastLine >= geometry.lineCount())
            throw new IllegalArgumentException("Invalid physical bounds");
        var starts = geometry.line(firstLine).verses;
        var ends = geometry.line(lastLine).verses;
        VerseRef start = starts.get(0);
        VerseRef end = ends.get(ends.size() - 1);
        if (geometry.firstLineIndex(start) != firstLine || geometry.lastLineIndex(end) != lastLine)
            throw new IllegalArgumentException("Physical bounds split a verse");
        return new VerseRange(start, end);
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
