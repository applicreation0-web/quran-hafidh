package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Frozen Stabilisation working-unit policy derived from canonical physical Mushaf lines. Despite
 * the name, an input segment is no longer restricted to one physical page: a Stabilisation weekly
 * unit (see GeometryRepository.eligibleWeeklyStabilizationUnit) can run to ~1.5 pages, split into
 * three sessions (8/7/7) instead of the ordinary single-page unit's one or two.
 */
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
        // The weekly unit's own line-count target is capped, but ownership resolution for its
        // last verse can still pull in a few more lines when that verse runs long (see
        // GeometryRepository.eligibleWeeklyStabilizationUnit) — this is a generous sanity bound
        // against genuine corruption, not the real target.
        if (pageLines.size() > 2 * PreviewConfig.STABILIZATION_WEEKLY_LINES) {
            throw new IllegalStateException("Stabilisation unit exceeds the sane line budget");
        }

        int page = pageLines.get(0).page;
        ArrayList<Integer> surahs = new ArrayList<>(pageLines.size());
        for (GeometryRepository.LineMeta line : pageLines) {
            if (line == null) throw new IllegalStateException("Stabilisation unit requires canonical physical lines");
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
        if (count <= 18) {
            appendTwoWaySplit(units, lines, page, surah, start, end, count);
            return;
        }
        appendThreeWaySplit(units, lines, page, surah, start, end, count);
    }

    private static void appendTwoWaySplit(
            List<Unit> units,
            List<GeometryRepository.LineMeta> lines,
            int page,
            int surah,
            int start,
            int end,
            int count) {
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
        int split = preferCleanVerseBoundary(lines, start, count, bestLeft);
        units.add(unit(page, surah, lines, start, start + split));
        units.add(unit(page, surah, lines, start + split, end));
    }

    /**
     * A validated Consolidation unit must independently re-verify as a single whole block when
     * later re-planned in isolation (completeConsolidationSessionV6/completeLearningConsolidation
     * SessionV6 re-run planPage on just that block's own lines and require exactly one result).
     * That only holds while every block stays within the ≤11-line "stays whole" tier, so no split
     * here may ever produce a block bigger than that — the same ceiling the two-way split already
     * respected implicitly (its own count was always ≤15, so a bestLeft/right of at most count-5
     * could never exceed 10).
     */
    private static final int MAX_INDEPENDENTLY_VERIFIABLE_BLOCK = 11;

    /**
     * Stabilisation's weekly unit (~22 lines, three sessions Tue/Thu/Sat) targets 8/7/7 lines,
     * the same "minimize the worst deviation from target" search and clean-verse-boundary
     * preference as the two-way split, generalized to two cut points instead of one.
     */
    private static void appendThreeWaySplit(
            List<Unit> units,
            List<GeometryRepository.LineMeta> lines,
            int page,
            int surah,
            int start,
            int end,
            int count) {
        int bestA = -1, bestB = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        int cap = MAX_INDEPENDENTLY_VERIFIABLE_BLOCK;
        for (int a = 5; a <= Math.min(cap, count - 10); a++) {
            for (int b = 5; b <= Math.min(cap, count - a - 5); b++) {
                int c = count - a - b;
                if (c < 5 || c > cap) continue;
                double score = Math.max(Math.abs(a - 8), Math.max(Math.abs(b - 7), Math.abs(c - 7)));
                if (score < bestScore) {
                    bestScore = score;
                    bestA = a;
                    bestB = b;
                }
            }
        }

        if (bestA < 0) {
            appendTwoWaySplit(units, lines, page, surah, start, end, count);
            return;
        }
        int split1 = preferCleanVerseBoundary(lines, start, count, bestA);
        int split2 = preferCleanVerseBoundary(lines, start, count, bestA + bestB);
        // Each cut is shifted independently by up to ±2 lines toward a clean verse boundary.
        // preferCleanVerseBoundary's own window keeps split1 and split2 each within [5, count-5]
        // of the segment's own ends, but nothing stops the two shifts from moving *toward* each
        // other — e.g. split1 pulled forward while split2 is pulled back — which can squeeze the
        // middle block down to just 1-4 lines even though bestA/bestB were both comfortably ≥5.
        // Fall back to the exact (guaranteed-≥5-per-block) target split whenever a shift would
        // starve any block below that floor, or push one past the independently-verifiable ceiling.
        if (split2 - split1 < 5 || split1 > cap || split2 - split1 > cap || count - split2 > cap) {
            split1 = bestA;
            split2 = bestA + bestB;
        }
        units.add(unit(page, surah, lines, start, start + split1));
        units.add(unit(page, surah, lines, start + split1, start + split2));
        units.add(unit(page, surah, lines, start + split2, end));
    }

    /**
     * The 7.5/7.5 target ignores verse boundaries and can land mid-verse, splitting one verse's
     * lines across both halves. Prefer the nearest point where a line's last verse actually differs
     * from the next line's first verse — but only within two lines of that target, and never
     * outside the same 5..count-5 safety margin the target itself respects. Beyond that tolerance,
     * cutting mid-verse is accepted for now: a real fix needs waqf-mark data this corpus doesn't
     * carry (no line/word-level waqf annotations exist in the shipped KFQC geometry corpus).
     */
    private static int preferCleanVerseBoundary(
            List<GeometryRepository.LineMeta> lines, int start, int count, int bestLeft) {
        int chosenLeft = bestLeft;
        int chosenDistance = Integer.MAX_VALUE;
        int lowLeft = Math.max(5, bestLeft - 2);
        int highLeft = Math.min(count - 5, bestLeft + 2);
        for (int left = lowLeft; left <= highLeft; left++) {
            List<VerseRef> beforeVerses = lines.get(start + left - 1).verses;
            VerseRef lastBefore = beforeVerses.get(beforeVerses.size() - 1);
            VerseRef firstAfter = lines.get(start + left).verses.get(0);
            if (lastBefore.equals(firstAfter)) continue;
            int distance = Math.abs(left - bestLeft);
            if (distance < chosenDistance) {
                chosenDistance = distance;
                chosenLeft = left;
            }
        }
        return chosenLeft;
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
