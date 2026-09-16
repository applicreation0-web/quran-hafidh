package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Frozen Stabilisation working-unit policy derived from canonical physical Mushaf lines. */
final class StabilizationHalfPagePolicy {
    static final class Unit {
        final int page;
        final int surah;
        final List<String> lineIds;

        Unit(int page, int surah, List<String> lineIds) {
            this.page = page;
            this.surah = surah;
            this.lineIds = Collections.unmodifiableList(new ArrayList<>(lineIds));
        }
    }

    private StabilizationHalfPagePolicy() {}

    static List<Unit> planPage(List<GeometryRepository.LineMeta> pageLines) {
        if (pageLines == null || pageLines.isEmpty()) {
            throw new IllegalStateException("Stabilisation page requires canonical physical lines");
        }
        if (pageLines.size() > 15) {
            throw new IllegalStateException("Canonical Mushaf page exceeds 15 physical lines");
        }

        int page = pageLines.get(0).page;
        ArrayList<Integer> surahs = new ArrayList<>(pageLines.size());
        for (GeometryRepository.LineMeta line : pageLines) {
            if (line == null || line.page != page) {
                throw new IllegalStateException("Stabilisation unit may not cross page boundary");
            }
            surahs.add(singleSurah(line));
        }

        ArrayList<Unit> units = new ArrayList<>();
        int segmentStart = 0;
        while (segmentStart < pageLines.size()) {
            int surah = surahs.get(segmentStart);
            int segmentEnd = segmentStart + 1;
            while (segmentEnd < pageLines.size() && surahs.get(segmentEnd) == surah) segmentEnd++;
            appendSegment(units, pageLines, page, surah, segmentStart, segmentEnd);
            segmentStart = segmentEnd;
        }
        return Collections.unmodifiableList(units);
    }

    private static void appendSegment(
            List<Unit> units,
            List<GeometryRepository.LineMeta> lines,
            int page,
            int surah,
            int start,
            int end) {
        int count = end - start;
        if (count <= 11) {
            units.add(unit(page, surah, lines, start, end));
            return;
        }

        int bestLeft = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int left = 5; left <= count - 5; left++) {
            int right = count - left;
            double score = Math.max(Math.abs(left - 7.5), Math.abs(right - 7.5));
            if (score < bestScore) {
                bestScore = score;
                bestLeft = left;
            }
        }

        if (bestLeft < 0) {
            units.add(unit(page, surah, lines, start, end));
            return;
        }
        units.add(unit(page, surah, lines, start, start + bestLeft));
        units.add(unit(page, surah, lines, start + bestLeft, end));
    }

    private static Unit unit(
            int page,
            int surah,
            List<GeometryRepository.LineMeta> lines,
            int start,
            int end) {
        ArrayList<String> ids = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) ids.add(lines.get(i).id);
        return new Unit(page, surah, ids);
    }

    private static int singleSurah(GeometryRepository.LineMeta line) {
        if (line.verses == null || line.verses.isEmpty()) {
            throw new IllegalStateException("Physical Mushaf line must contain Quran verses");
        }
        int surah = line.verses.get(0).getSurah();
        for (VerseRef verse : line.verses) {
            if (verse.getSurah() != surah) {
                throw new IllegalStateException("Physical Mushaf line crosses surah boundary: " + line.id);
            }
        }
        return surah;
    }
}
