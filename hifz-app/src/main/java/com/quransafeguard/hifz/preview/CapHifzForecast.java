package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Read-only forecast. No cursor, progress, session, programme or receipt writes.
 * The displayed attendance is a scenario, not an acquired-credit multiplier.
 */
final class CapHifzForecast {
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

    private CapHifzForecast() {}

    static List<Quarter> quarters(GeometryRepository geometry, LocalDate today,
            int nextSabqiLine, int nextItqanLine, boolean tailComplete,
            WeeklyBilan.Calendar calendar, boolean forecastSabqi, boolean forecastTail,
            List<ItqanFourSessionPolicy.Session> savedSlots, Set<String> stabilized,
            LocalDate blockedItqanWeek, double attendance) {
        if (!Double.isFinite(attendance) || attendance <= 0 || attendance > 1)
            throw new IllegalArgumentException("Invalid attendance hypothesis");
        int limit = geometry.lastLineIndex(SabqiRoute.ROUTE_END);
        int sabqiCursor = Math.max(0, Math.min(nextSabqiLine, limit + 1));
        Tail tail = new Tail(geometry, nextItqanLine, tailComplete, savedSlots, stabilized);
        tail.closedWeek = blockedItqanWeek;
        LocalDate date = LocalDate.of(today.getYear(), ((today.getMonthValue() - 1) / 3) * 3 + 1, 1)
            .plusMonths(3).minusDays(1);
        LocalDate day = today.plusDays(1);
        int sabqiOpportunities = 0;
        int itqanOpportunities = 0;
        int percent = (int) Math.round(attendance * 100);
        ArrayList<Quarter> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            while (!day.isAfter(date)) {
                CadenceAction action = calendar.action(day);
                if (forecastSabqi && sabqiCursor <= limit && action == CadenceAction.LEARNING
                        && attended(++sabqiOpportunities, percent)) {
                    sabqiCursor = geometry.fiveLineBlock(sabqiCursor, limit).endLineIndex + 1;
                }
                if (forecastTail && !tail.complete && action == CadenceAction.STABILIZATION
                        && tail.available(day) && attended(++itqanOpportunities, percent)) {
                    tail.advance(day);
                }
                day = day.plusDays(1);
            }
            boolean sabqiDone = sabqiCursor > limit;
            VerseRef sabqi = sabqiDone ? SabqiRoute.ROUTE_END
                : lastCompleteVerse(geometry, sabqiCursor - 1);
            rows.add(new Quarter(date, sabqi, tail.complete ? null : tail.position, sabqiDone, tail.complete));
            if ((sabqiDone || !forecastSabqi) && (tail.complete || !forecastTail)) break;
            date = date.plusDays(1).plusMonths(3).minusDays(1);
        }
        return Collections.unmodifiableList(rows);
    }

    private static boolean attended(int opportunities, int percent) {
        return opportunities * percent / 100 > (opportunities - 1) * percent / 100;
    }

    private static VerseRef lastCompleteVerse(GeometryRepository geometry, int index) {
        for (; index >= 0; index--) {
            List<VerseRef> refs = geometry.line(index).verses;
            for (int i = refs.size() - 1; i >= 0; i--) {
                VerseRef ref = refs.get(i);
                if (geometry.lastLineIndex(ref) <= index) return ref;
            }
        }
        return null;
    }

    private static final class Tail {
        final GeometryRepository geometry;
        final Set<String> stabilized;
        List<ItqanFourSessionPolicy.Session> slots;
        int nextLine;
        int slot;
        boolean complete;
        LocalDate closedWeek;
        VerseRef position;

        Tail(GeometryRepository geometry, int nextLine, boolean complete,
                List<ItqanFourSessionPolicy.Session> savedSlots, Set<String> stabilized) {
            this.geometry = geometry;
            this.nextLine = nextLine;
            if (nextLine >= 0 && !complete) {
                VerseRef previous = lastCompleteVerse(geometry, nextLine - 1);
                if (previous != null && previous.getSurah() >= ItqanRotationPolicy.TAIL_START.getSurah())
                    position = previous;
                if (savedSlots.isEmpty()) {
                    while (this.nextLine < geometry.lineCount()
                            && stabilized.contains(geometry.line(this.nextLine).id)) this.nextLine++;
                }
            }
            this.complete = complete;
            this.slots = savedSlots;
            this.stabilized = stabilized;
        }

        boolean available(LocalDate date) {
            return !week(date).equals(closedWeek) && nextLine >= 0 && nextLine < geometry.lineCount();
        }

        void advance(LocalDate date) {
            if (slots.isEmpty()) {
                int page = geometry.line(nextLine).page;
                Set<String> eligible = new java.util.LinkedHashSet<>();
                for (int i = nextLine; i < geometry.lineCount(); i++) {
                    GeometryRepository.LineMeta line = geometry.line(i);
                    if (line.page != page || stabilized.contains(line.id)) break;
                    eligible.add(line.id);
                }
                slots = ItqanFourSessionPolicy.weeklyPage(geometry, nextLine, eligible).sessions;
            }
            List<ItqanFourSessionPolicy.Block> blocks = slots.get(slot++).blocks;
            List<String> last = blocks.get(blocks.size() - 1).lineIds;
            nextLine = geometry.linesForExactIds(last).get(last.size() - 1).globalIndex + 1;
            position = lastCompleteVerse(geometry, nextLine - 1);
            complete = ItqanRotationPolicy.TAIL_END.equals(position);
            if (slot == slots.size() || slots.get(slot).blocks.isEmpty()) {
                // A short remainder also closes the page for the rest of its actual week.
                closedWeek = week(date);
                slots = Collections.emptyList();
                slot = 0;
                while (nextLine < geometry.lineCount() && stabilized.contains(geometry.line(nextLine).id))
                    nextLine++;
            }
        }

        private static LocalDate week(LocalDate date) {
            return date.minusDays(date.getDayOfWeek().getValue() - 1);
        }
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
