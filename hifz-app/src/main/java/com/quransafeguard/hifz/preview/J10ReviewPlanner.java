package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Schema-6 acquired-line J10 priority planner. The 0.7.4 J10 archive is migration-only. */
final class J10ReviewPlanner {
    static final int MAX_PRIORITY_LINES = 5;

    static final class PriorityGroup {
        final List<Integer> lineIndexes;
        final List<String> lineIds;
        final List<VerseRef> verses;
        final int firstPage;
        final int lastPage;
        final int maxAgeDays;
        final J10ReviewPolicy.Forecast forecast;

        PriorityGroup(List<Integer> lineIndexes, List<String> lineIds, List<VerseRef> verses,
                      int firstPage, int lastPage, int maxAgeDays, J10ReviewPolicy.Forecast forecast) {
            this.lineIndexes = Collections.unmodifiableList(new ArrayList<>(lineIndexes));
            this.lineIds = Collections.unmodifiableList(new ArrayList<>(lineIds));
            this.verses = Collections.unmodifiableList(new ArrayList<>(verses));
            this.firstPage = firstPage;
            this.lastPage = lastPage;
            this.maxAgeDays = Math.max(0, maxAgeDays);
            this.forecast = forecast;
        }

        boolean isEmpty() { return lineIndexes.isEmpty(); }
    }

    private final HifzPrefs prefs;
    private final HifzSpeedStore speedStore;
    private final GeometryRepository geometry;
    private final J10V6Store store;

    J10ReviewPlanner(Context context) {
        prefs = new HifzPrefs(context);
        speedStore = new HifzSpeedStore(context);
        geometry = GeometryRepository.get(context);
        store = new J10V6Store(context);
    }

    /** Schema6 state is already authoritative; reconciliation must never invent UNKNOWN_DUE dates. */
    boolean syncAcquired(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today required");
        return store.syncAcquired();
    }

    J10ReviewPolicy.Forecast forecast(LocalDate today) {
        Map<String, LocalDate> snapshot = store.snapshot();
        return J10ReviewPolicy.forecastByDay(snapshot.values(), today,
            speedStore.maintenanceSecondsPerLine(), remainingCapacityByDayMinutes(today));
    }

    PriorityGroup priorityGroup(LocalDate today) {
        Map<String, LocalDate> snapshot = store.snapshot();
        J10ReviewPolicy.Forecast forecast = J10ReviewPolicy.forecastByDay(snapshot.values(), today,
            speedStore.maintenanceSecondsPerLine(), remainingCapacityByDayMinutes(today));

        LinkedHashMap<Integer, LocalDate> indexed = new LinkedHashMap<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            LocalDate date = snapshot.get(geometry.line(i).id);
            if (date != null) indexed.put(i, date);
        }
        List<Integer> indexes = priorityLineIndexes(indexed, today, forecast.sustainability, MAX_PRIORITY_LINES);
        if (indexes.isEmpty()) return new PriorityGroup(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), 1, 1, 0, forecast);

        ArrayList<String> ids = new ArrayList<>();
        LinkedHashSet<VerseRef> verseSet = new LinkedHashSet<>();
        int firstPage = 604, lastPage = 1, maxAge = 0;
        for (int index : indexes) {
            GeometryRepository.LineMeta line = geometry.line(index);
            ids.add(line.id);
            verseSet.addAll(line.verses);
            firstPage = Math.min(firstPage, line.page);
            lastPage = Math.max(lastPage, line.page);
            maxAge = Math.max(maxAge, J10ReviewPolicy.ageDays(indexed.get(index), today));
        }
        ArrayList<VerseRef> verses = new ArrayList<>(verseSet);
        verses.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return new PriorityGroup(indexes, ids, verses, firstPage, lastPage, maxAge, forecast);
    }

    boolean acquireAndReview(Collection<String> lineIds, LocalDate date) {
        return store.acquireLines(lineIds, date) && store.markReviewed(lineIds, date);
    }

    boolean acquireIndexes(int start, int end, LocalDate date) {
        return store.acquireLines(lineIdsForIndexes(start, end), date);
    }

    boolean reviewIndexes(int start, int end, LocalDate date) {
        List<String> ids = lineIdsForIndexes(start, end);
        return store.acquireLines(ids, date) && store.markReviewed(ids, date);
    }

    boolean markReviewed(Collection<String> lineIds, LocalDate date) {
        return store.markReviewed(lineIds, date);
    }

    List<String> lineIdsForIndexes(int start, int end) {
        if (start < 0 || end < start || start >= geometry.lineCount()) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>();
        for (int i = start; i <= end && i < geometry.lineCount(); i++) out.add(geometry.line(i).id);
        return out;
    }

    List<String> lineIdsForVerseRange(VerseRef start, VerseRef end) {
        return geometry.lineIdsForVerseRange(start, end);
    }

    int[] surahSegmentLineCounts(VerseRef start, VerseRef end) {
        return geometry.surahSegmentLineCounts(start, end);
    }

    List<String> traversalLineIds(VerseRef start, VerseRef end) {
        if (start == null || end == null) return Collections.emptyList();
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(start) || !corpus.contains(end)) return Collections.emptyList();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        VerseRef cursor = start;
        for (int guard = 0; guard <= QuranCanon.TOTAL_VERSES; guard++) {
            ids.addAll(geometry.lineIdsForVerseRange(cursor, cursor));
            if (cursor.equals(end)) return new ArrayList<>(ids);
            cursor = corpus.next(cursor);
        }
        return Collections.emptyList();
    }

    HifzPrefs prefs() { return prefs; }
    GeometryRepository geometry() { return geometry; }
    Map<String, LocalDate> snapshot() { return store.snapshot(); }

    private int[] remainingCapacityByDayMinutes(LocalDate start) {
        int[] out = scheduledCapacityByDayMinutes(start, J10ReviewPolicy.FORECAST_DAYS,
            prefs.recentSabqi().size(), prefs.recentConsolidationActivatedOn() != null);
        if (out.length == 0) return out;
        DailyPlan todayPlan = HifzSchedule.INSTANCE.planFor(start.getDayOfWeek(),
            prefs.recentSabqi().size(), prefs.recentConsolidationActivatedOn() != null);
        int consumed = consumedMinutes(todayPlan.getMorning().getKind(), todayPlan.getMorning().getTargetMinutes())
            + consumedMinutes(todayPlan.getEvening().getKind(), todayPlan.getEvening().getTargetMinutes());
        out[0] = Math.max(0, out[0] - consumed);
        return out;
    }

    private int consumedMinutes(SessionKind kind, int targetMinutes) {
        if (targetMinutes <= 0 || !isReusableJ10Kind(kind)) return 0;
        String mode = modeFor(kind);
        if (mode == null) return 0;
        long elapsed = prefs.elapsedFor(mode);
        return Math.min(targetMinutes, (int) Math.ceil(Math.max(0L, elapsed) / 60_000.0));
    }

    private static String modeFor(SessionKind kind) {
        if (kind == null) return null;
        switch (kind) {
            case SABQI_NEW: return HifzSessionActivity.SABQI;
            case SABQI_TODAY_REVIEW: return HifzSessionActivity.SABQI_TODAY_REVIEW;
            case ITQAN: return HifzSessionActivity.ITQAN;
            case RECENT_SABQI_REVIEW: return HifzSessionActivity.RECENT_SABQI_REVIEW;
            case OLD_ITQAN_MURAJAAH: return HifzSessionActivity.MURAJAAH;
            default: return null;
        }
    }

    static int[] scheduledCapacityByDayMinutes(LocalDate start, int days,
                                               int recentBlockCount, boolean consolidationActivated) {
        if (start == null || days <= 0) return new int[0];
        int[] out = new int[days];
        for (int offset = 0; offset < days; offset++) {
            DailyPlan plan = HifzSchedule.INSTANCE.planFor(
                start.plusDays(offset).getDayOfWeek(), Math.max(0, recentBlockCount), consolidationActivated);
            out[offset] = reusableCapacityMinutes(
                    plan.getMorning().getKind(), plan.getMorning().getTargetMinutes())
                + reusableCapacityMinutes(
                    plan.getEvening().getKind(), plan.getEvening().getTargetMinutes());
        }
        return out;
    }

    private static int reusableCapacityMinutes(SessionKind kind, int targetMinutes) {
        return isReusableJ10Kind(kind) ? Math.max(0, targetMinutes) : 0;
    }

    static boolean isReusableJ10Kind(SessionKind kind) {
        return kind == SessionKind.SABQI_TODAY_REVIEW
            || kind == SessionKind.ITQAN
            || kind == SessionKind.OLD_ITQAN_MURAJAAH;
    }

    static int scheduledCapacityMinutes(LocalDate start, int days,
                                        int recentBlockCount, boolean consolidationActivated) {
        int total = 0;
        for (int value : scheduledCapacityByDayMinutes(start, days, recentBlockCount, consolidationActivated)) {
            total += value;
        }
        return total;
    }

    static List<Integer> priorityLineIndexes(Map<Integer, LocalDate> lastReviewedByIndex,
                                             LocalDate today,
                                             J10ReviewPolicy.Sustainability sustainability,
                                             int maxLines) {
        if (lastReviewedByIndex == null || lastReviewedByIndex.isEmpty()
                || today == null || maxLines <= 0) return Collections.emptyList();

        LinkedHashMap<Integer, Integer> eligibleAge = new LinkedHashMap<>();
        for (Map.Entry<Integer, LocalDate> entry : lastReviewedByIndex.entrySet()) {
            Integer index = entry.getKey();
            LocalDate reviewed = entry.getValue();
            if (index == null || index < 0 || reviewed == null) continue;
            int age = J10ReviewPolicy.ageDays(reviewed, today);
            if (J10ReviewPolicy.shouldPreempt(age, sustainability)) eligibleAge.put(index, age);
        }
        if (eligibleAge.isEmpty()) return Collections.emptyList();

        List<Integer> eligible = new ArrayList<>(eligibleAge.keySet());
        eligible.sort(Comparator
            .comparingInt((Integer index) -> eligibleAge.get(index)).reversed()
            .thenComparingInt(Integer::intValue));
        int start = eligible.get(0);

        ArrayList<Integer> out = new ArrayList<>();
        for (int index = start; out.size() < maxLines; index++) {
            if (!eligibleAge.containsKey(index)) break;
            out.add(index);
        }
        return Collections.unmodifiableList(out);
    }
}
