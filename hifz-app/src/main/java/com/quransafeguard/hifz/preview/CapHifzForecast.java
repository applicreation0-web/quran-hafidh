package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.QuranCanon;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Read-only forecast. No cursor, progress, session, programme or receipt writes.
 * The displayed attendance is a scenario, not an acquired-credit multiplier.
 */
final class CapHifzForecast {
    static final class Milestone {
        final LocalDate date;
        final VerseRef verse;
        final int hizb;

        Milestone(LocalDate date, VerseRef verse, int hizb) {
            this.date = date;
            this.verse = verse;
            this.hizb = hizb;
        }
    }

    static final class Quarter {
        final LocalDate date;
        final VerseRef sabqi;
        final VerseRef itqan; // null after the FIRST TAIL pass or when not in TAIL
        final boolean sabqiComplete;
        final boolean firstTailComplete;
        final List<Milestone> sabqiMilestones;
        final List<Milestone> tailMilestones;
        // Every surah FINISH is a selectable annual marker on its own track.
        final List<Milestone> sabqiSurahFinishes;
        final List<Milestone> tailSurahFinishes;
        final LocalDate sabqiFinish;
        final LocalDate tailFinish;

        Quarter(LocalDate date, VerseRef sabqi, VerseRef itqan,
                boolean sabqiComplete, boolean firstTailComplete) {
            this(date, sabqi, itqan, sabqiComplete, firstTailComplete, List.of(), List.of(), null, null);
        }

        Quarter(LocalDate date, VerseRef sabqi, VerseRef itqan, boolean sabqiComplete,
                boolean firstTailComplete, List<Milestone> sabqiMilestones, List<Milestone> tailMilestones,
                LocalDate sabqiFinish, LocalDate tailFinish) {
            this(date, sabqi, itqan, sabqiComplete, firstTailComplete,
                sabqiMilestones, tailMilestones, List.of(), List.of(), sabqiFinish, tailFinish);
        }

        Quarter(LocalDate date, VerseRef sabqi, VerseRef itqan, boolean sabqiComplete,
                boolean firstTailComplete, List<Milestone> sabqiMilestones, List<Milestone> tailMilestones,
                List<Milestone> sabqiSurahFinishes, List<Milestone> tailSurahFinishes,
                LocalDate sabqiFinish, LocalDate tailFinish) {
            this.date = date;
            this.sabqi = sabqi;
            this.itqan = itqan;
            this.sabqiComplete = sabqiComplete;
            this.firstTailComplete = firstTailComplete;
            this.sabqiMilestones = List.copyOf(sabqiMilestones);
            this.tailMilestones = List.copyOf(tailMilestones);
            this.sabqiSurahFinishes = List.copyOf(sabqiSurahFinishes);
            this.tailSurahFinishes = List.copyOf(tailSurahFinishes);
            this.sabqiFinish = sabqiFinish;
            this.tailFinish = tailFinish;
        }
    }

    /** The year of projected An-Nās completion is still shown; later
     * forecast years must not retain a completed Maîtrise trajectory.
     * Never converts a projection into persisted Hifz progress. */
    static boolean projectedTailFinishedBeforeYear(List<Quarter> quarters, int selectedYear) {
        for (Quarter row : quarters) {
            if (row.tailFinish != null)
                return row.tailFinish.getYear() < selectedYear;
        }
        return false;
    }

    private CapHifzForecast() {}

    /** The projected year-end cursor moves with the requested forecast year,
     * independently on each route, including years AFTER An-Nās completion.
     * Pure read-only presentation helper: nothing is credited or persisted. */
    static VerseRef projectedPositionAtYear(List<Quarter> quarters, int year,
            boolean acquisition, VerseRef actualPosition) {
        VerseRef position = actualPosition;
        for (Quarter q : quarters) {
            if (q.date.getYear() > year) break;
            if (acquisition) {
                if (q.sabqi != null) position = q.sabqi;
            } else if (q.firstTailComplete) {
                position = ItqanRotationPolicy.TAIL_END;
            } else if (q.itqan != null) {
                position = q.itqan;
            }
        }
        return position;
    }

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
        List<Milestone> sabqiBoundaries = boundaries(geometry, 1, limit);
        List<Milestone> tailBoundaries = boundaries(geometry, 49, geometry.lineCount() - 1);
        int sabqiBoundary = firstPendingBoundary(geometry, sabqiBoundaries, sabqiCursor);
        int tailBoundary = firstPendingBoundary(geometry, tailBoundaries, nextItqanLine);
        List<Milestone> sabqiSurahBounds = surahEndBoundaries(geometry, 2, 48);
        List<Milestone> tailSurahBounds = surahEndBoundaries(geometry, 49, 114);
        int sabqiSurahBoundary = firstPendingBoundary(geometry, sabqiSurahBounds, sabqiCursor);
        int tailSurahBoundary = firstPendingBoundary(geometry, tailSurahBounds, nextItqanLine);
        // At the minimum displayed attendance, even one Sabqi slot per week covers
        // the entire route within this bound. Unreachable routes remain open.
        for (int i = 0; i < 240; i++) {
            List<Milestone> sabqiMilestones = new ArrayList<>();
            List<Milestone> tailMilestones = new ArrayList<>();
            List<Milestone> sabqiSurahFinishes = new ArrayList<>();
            List<Milestone> tailSurahFinishes = new ArrayList<>();
            LocalDate sabqiFinish = null;
            LocalDate tailFinish = null;
            while (!day.isAfter(date)) {
                CadenceAction action = calendar.action(day);
                if (forecastSabqi && sabqiCursor <= limit && action == CadenceAction.LEARNING
                        && attended(++sabqiOpportunities, percent)) {
                    sabqiCursor = geometry.fiveLineBlock(sabqiCursor, limit).endLineIndex + 1;
                    sabqiBoundary = crossings(geometry, sabqiBoundaries, sabqiBoundary, sabqiCursor, day, sabqiMilestones);
                    sabqiSurahBoundary = crossings(geometry, sabqiSurahBounds,
                        sabqiSurahBoundary, sabqiCursor, day, sabqiSurahFinishes);
                    if (sabqiCursor > limit) sabqiFinish = day;
                }
                if (forecastTail && !tail.complete && action == CadenceAction.STABILIZATION
                        && tail.available(day) && attended(++itqanOpportunities, percent)) {
                    tail.advance(day);
                    int covered = tail.position == null ? -1 : geometry.lastLineIndex(tail.position);
                    tailBoundary = crossings(geometry, tailBoundaries, tailBoundary, covered + 1, day, tailMilestones);
                    tailSurahBoundary = crossings(geometry, tailSurahBounds,
                        tailSurahBoundary, covered + 1, day, tailSurahFinishes);
                    if (tail.complete) tailFinish = day;
                }
                day = day.plusDays(1);
            }
            boolean sabqiDone = sabqiCursor > limit;
            VerseRef sabqi = sabqiDone ? SabqiRoute.ROUTE_END
                : lastCompleteVerse(geometry, sabqiCursor - 1);
            rows.add(new Quarter(date, sabqi, tail.complete ? null : tail.position, sabqiDone, tail.complete,
                sabqiMilestones, tailMilestones,
                sabqiSurahFinishes, tailSurahFinishes, sabqiFinish, tailFinish));
            if ((sabqiDone || !forecastSabqi) && (tail.complete || !forecastTail)) break;
            date = date.plusDays(1).plusMonths(3).minusDays(1);
        }
        return Collections.unmodifiableList(rows);
    }

    /** Canonical final ayah for EACH sourate in the relevant route; no Fatihah or
     * guessed completion date. A mark is dated only after its final QCF line
     * is actually crossed in the projection. */
    private static List<Milestone> surahEndBoundaries(GeometryRepository geometry,
            int firstSurah, int lastSurah) {
        ArrayList<Milestone> ends = new ArrayList<>();
        for (int surah = firstSurah; surah <= lastSurah; surah++) {
            VerseRef end = surah == 114 ? ItqanRotationPolicy.TAIL_END
                : QuranCanon.INSTANCE.fromOrdinal(
                    GeometryRepository.ordinal(new VerseRef(surah + 1, 1)) - 1);
            if (end.getSurah() != surah || geometry.lastLineIndex(end) < 0)
                throw new IllegalStateException("Invalid canonical end of surah " + surah);
            ends.add(new Milestone(null, end, hizb(end)));
        }
        return ends;
    }

    private static List<Milestone> boundaries(GeometryRepository geometry, int firstSurah, int lastLine) {
        List<Milestone> result = new ArrayList<>();
        for (int[] rub : QuranRubBoundaries.TABLE) {
            if (rub[5] != 0 || rub[1] < firstSurah) continue;
            VerseRef verse = new VerseRef(rub[1], rub[2]);
            if (geometry.lastLineIndex(verse) > lastLine) break;
            result.add(new Milestone(null, verse, rub[4]));
        }
        // Juz/Hizb are not necessarily surah starts: 3:1 (Āl ʿImrān) was
        // absent from the forecast, although canonical learning continues
        // from Al-Baqarah 2:286. Date every actual chapter transition on
        // the same projected QCF line-crossing day, with NO acquired credit.
        int from = firstSurah == 1 ? 3 : firstSurah + 1;
        int to = firstSurah == 1 ? SabqiRoute.ROUTE_END.getSurah() : 114;
        for (int surah = from; surah <= to; surah++) {
            VerseRef entry = new VerseRef(surah, 1);
            if (geometry.lastLineIndex(entry) > lastLine) break;
            boolean alreadyPresent = false;
            for (Milestone existing : result) {
                if (existing.verse.equals(entry)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) result.add(new Milestone(null, entry, hizb(entry)));
        }
        result.sort(Comparator.comparingInt(p -> GeometryRepository.ordinal(p.verse)));
        return result;
    }

    /** Keep the near-term step, then the next DIFFERENT surah.
     *  Otherwise two consecutive Hizb in Al-Baqarah hide Āl ʿImrān.
     *  Stops are actual forecast events, never evenly-spaced invented dates. */
    static List<Milestone> visibleMilestones(List<Milestone> chronological) {
        if (chronological.size() <= 2)
            return List.copyOf(chronological);
        Milestone nearest = chronological.get(0);
        for (int i = 1; i < chronological.size(); i++) {
            Milestone next = chronological.get(i);
            if (next.verse.getSurah() != nearest.verse.getSurah())
                return List.of(nearest, next);
        }
        return List.of(nearest, chronological.get(1));
    }

    private static int firstPendingBoundary(GeometryRepository geometry, List<Milestone> bounds, int cursor) {
        int index = 0;
        while (index < bounds.size() && geometry.lastLineIndex(bounds.get(index).verse) < cursor) index++;
        return index;
    }

    private static int crossings(GeometryRepository geometry, List<Milestone> bounds, int index,
            int cursor, LocalDate day, List<Milestone> output) {
        while (index < bounds.size() && geometry.lastLineIndex(bounds.get(index).verse) < cursor) {
            Milestone point = bounds.get(index++);
            output.add(new Milestone(day, point.verse, point.hizb));
        }
        return index;
    }

    private static boolean attended(int opportunities, int percent) {
        // Round missed opportunities DOWN, not attended opportunities DOWN.
        // At 90%, opportunities 1..9 are attended and the 10th is skipped.
        // The previous floor(attended) expression incorrectly skipped the
        // very first future lesson, shifting early surah dates.
        int skippedRate = 100 - percent;
        return opportunities * skippedRate / 100
            == (opportunities - 1) * skippedRate / 100;
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
