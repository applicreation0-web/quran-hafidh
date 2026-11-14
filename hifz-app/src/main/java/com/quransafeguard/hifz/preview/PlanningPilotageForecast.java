package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only forecast. No cursor, progress, session, programme or receipt writes.
 * The displayed attendance is a scenario, not an acquired-credit multiplier.
 */
final class PlanningPilotageForecast {
    static final class Quarter {
        final LocalDate date;
        final VerseRef sabqi;
        final VerseRef itqan; // null after the FIRST TAIL pass or when not in TAIL
        final boolean sabqiComplete;
        final boolean firstTailComplete;

        Quarter(LocalDate date, VerseRef sabqi, VerseRef itqan,
                boolean sabqiComplete, boolean firstTailComplete) {
            this.date = date;
            this.sabqi = sabqi;
            this.itqan = itqan;
            this.sabqiComplete = sabqiComplete;
            this.firstTailComplete = firstTailComplete;
        }
    }

    private PlanningPilotageForecast() {}

    static List<Quarter> quarters(GeometryRepository geometry, LocalDate today,
            int nextSabqiLine, int itqanStartPage, boolean tailAlreadyComplete,
            int sabqiSessionsPerWeek, double attendance) {
        if (geometry == null || today == null || sabqiSessionsPerWeek < 0
                || attendance <= 0 || attendance > 1)
            throw new IllegalArgumentException("Invalid forecast inputs");
        int limit = geometry.lastLineIndex(SabqiRoute.ROUTE_END);
        int sabqiCursor = Math.max(0, Math.min(nextSabqiLine, limit + 1));
        // The first TAIL is one physical QCF page per fully completed week.
        int itqanPage = Math.max(515, Math.min(604, itqanStartPage));
        int quarter = (today.getMonthValue() - 1) / 3;
        LocalDate date = LocalDate.of(today.getYear(), quarter * 3 + 1, 1)
            .plusMonths(3).minusDays(1);
        ArrayList<Quarter> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long days = Math.max(0L, java.time.temporal.ChronoUnit.DAYS.between(today, date));
            int learned = (int) Math.floor(days * (5.0 * sabqiSessionsPerWeek * attendance) / 7.0);
            int index = learned == 0 ? Math.max(0, sabqiCursor - 1)
                : Math.min(limit, sabqiCursor + learned - 1);
            boolean sabqiDone = sabqiCursor > limit || learned >= limit - sabqiCursor + 1;
            VerseRef sabqi = sabqiDone ? SabqiRoute.ROUTE_END
                : lastVerse(geometry.line(index));

            int completedPages = (int) Math.floor(days * attendance / 7.0);
            int itqanPageAtDate = Math.min(604, itqanPage + completedPages);
            // Nas is not a credit for the FRONT leg or later permanent rotation.
            boolean nas = tailAlreadyComplete || itqanPage + completedPages > 604;
            VerseRef itqan = tailAlreadyComplete ? null
                : lastVerse(geometry.line(geometry.lastLineIndex(
                    lastVerseOnPage(geometry, itqanPageAtDate))));
            if (nas) itqan = null;
            rows.add(new Quarter(date, sabqi, itqan, sabqiDone, nas));
            if (sabqiDone) break;
            date = date.plusMonths(3).withDayOfMonth(1).plusMonths(3).minusDays(1);
        }
        return Collections.unmodifiableList(rows);
    }

    private static VerseRef lastVerseOnPage(GeometryRepository geometry, int page) {
        // Read only the page's exact physical QCF lines (not a guessed 15-line offset).
        List<String> ids = geometry.lineIdsOnPage(page);
        if (ids.isEmpty()) throw new IllegalStateException("Empty QCF page");
        return lastVerse(geometry.linesForExactIds(ids).get(ids.size() - 1));
    }

    private static VerseRef lastVerse(GeometryRepository.LineMeta line) {
        return line.verses.get(line.verses.size() - 1);
    }

    static int juz(VerseRef ref) {
        return JuzProgressSummary.ownerJuzIndex(
            GeometryRepository.ordinal(ref), JuzProgressSummary.boundaries()) + 1;
    }

    static int hizb(VerseRef ref) {
        int ordinal = GeometryRepository.ordinal(ref);
        int result = 1;
        for (int[] rub : QuranRubBoundaries.TABLE) {
            if (rub[5] != 0) continue;
            int at = GeometryRepository.ordinal(new VerseRef(rub[1], rub[2]));
            if (at > ordinal) break;
            result = rub[4];
        }
        return result;
    }
}
