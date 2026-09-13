package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.QuranCanon;
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

/** Single acquired-line list feeding J10 review priority across the existing weekly sessions. */
final class J10ReviewPlanner {
    static final int MAX_PRIORITY_LINES = 15;

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
    private final J10ReviewStore store;

    J10ReviewPlanner(Context context) {
        prefs = new HifzPrefs(context);
        speedStore = new HifzSpeedStore(context);
        geometry = GeometryRepository.get(context);
        store = new J10ReviewStore(context);
    }

    /**
     * Reconcile only material already acquired: stable Entretien corpus, recent learned blocks,
     * and completed sub-blocks of an in-progress fractionated Ancrage. Pending reconstruction is
     * deliberately excluded until it is actually completed.
     */
    boolean syncAcquired(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today required");
        LinkedHashSet<String> acquired = new LinkedHashSet<>();

        EligibleCorpus stable = prefs.murajaahCorpus();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            for (VerseRef verse : line.verses) {
                if (stable.contains(verse)) { acquired.add(line.id); break; }
            }
        }

        for (HifzPrefs.RecentSabqi recent : prefs.recentSabqi()) {
            for (int i = Math.max(0, recent.startLine); i <= recent.endLine && i < geometry.lineCount(); i++) {
                acquired.add(geometry.line(i).id);
            }
        }

        int completedBlocks = prefs.itqanBlockIndex();
        List<AnchoringQueue.Entry> queue = prefs.anchoringQueue();
        if (completedBlocks > 0 && !queue.isEmpty()) {
            AnchoringQueue.Entry entry = queue.get(prefs.anchoringQueueIndex(queue.size()));
            VerseRef start = GeometryRepository.parseVerse(entry.start);
            VerseRef end = GeometryRepository.parseVerse(entry.end);
            List<VerseRef> verses = geometry.versesForRange(start, end);
            if (prefs.isFractionatedUnit(verses)) {
                List<String> unitLines = geometry.lineIdsForVerseRange(start, end);
                int count = PreviewConfig.fractionatedBlockCount(unitLines.size());
                for (int block = 0; block < Math.min(completedBlocks, count); block++) {
                    int from = PreviewConfig.fractionatedBlockStart(unitLines.size(), block);
                    int len = PreviewConfig.fractionatedBlockLength(unitLines.size(), block);
                    acquired.addAll(unitLines.subList(from, from + len));
                }
            }
        }
        return store.acquireLines(acquired, today);
    }

    J10ReviewPolicy.Forecast forecast(LocalDate today) {
        syncAcquired(today);
        Map<String, LocalDate> snapshot = store.snapshot();
        int available = scheduledCapacityMinutes(today, J10ReviewPolicy.FORECAST_DAYS,
            prefs.recentSabqi().size(), prefs.recentConsolidationActivatedOn() != null);
        return J10ReviewPolicy.forecast(snapshot.values(), today,
            speedStore.maintenanceSecondsPerLine(), available);
    }

    PriorityGroup priorityGroup(LocalDate today) {
        syncAcquired(today);
        Map<String, LocalDate> snapshot = store.snapshot();
        J10ReviewPolicy.Forecast forecast = J10ReviewPolicy.forecast(snapshot.values(), today,
            speedStore.maintenanceSecondsPerLine(),
            scheduledCapacityMinutes(today, J10ReviewPolicy.FORECAST_DAYS,
                prefs.recentSabqi().size(), prefs.recentConsolidationActivatedOn() != null));

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

    static int scheduledCapacityMinutes(LocalDate start, int days,
                                        int recentBlockCount, boolean consolidationActivated) {
        if (start == null || days <= 0) return 0;
        int total = 0;
        for (int offset = 0; offset < days; offset++) {
            DailyPlan plan = HifzSchedule.INSTANCE.planFor(
                start.plusDays(offset).getDayOfWeek(),
                Math.max(0, recentBlockCount), consolidationActivated);
            total += Math.max(0, plan.getMorning().getTargetMinutes());
            total += Math.max(0, plan.getEvening().getTargetMinutes());
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
