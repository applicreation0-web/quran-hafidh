package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts validated existing Hifz session commits into J10 acquired-line review credits. */
final class J10ReviewObserver {
    private static final Pattern RANGE = Pattern.compile("(\\d{1,3}):(\\d{1,3})\\s*→\\s*(\\d{1,3}):(\\d{1,3})");

    private final J10ReviewPlanner planner;
    private final HifzPrefs prefs;
    private final Map<String, Integer> recentStreaks = new HashMap<>();

    J10ReviewObserver(J10ReviewPlanner planner) {
        if (planner == null) throw new IllegalArgumentException("planner required");
        this.planner = planner;
        this.prefs = planner.prefs();
        seedRecentStreaks();
    }

    void reconcileAll(LocalDate today) {
        planner.syncAcquired(today);
        reconcileRecent(today);
    }

    void onPreferenceChanged(String key, LocalDate today) {
        if (key == null || today == null) return;
        switch (key) {
            case "recentSabqi":
                planner.syncAcquired(today);
                reconcileRecent(today);
                break;
            case "lastSabqiDate":
            case "lastSabqiTodayReviewDate":
                planner.syncAcquired(today);
                creditCurrentSabqi(today);
                break;
            case "lastItqanDate":
                planner.syncAcquired(today);
                creditItqan(today);
                break;
            case "lastMurajaahDate":
                planner.syncAcquired(today);
                creditMurajaah(today);
                break;
            default:
                // Repetition counters, masks, timers and cursors must not trigger an O(Mushaf) J10 scan.
                break;
        }
    }

    private void seedRecentStreaks() {
        for (HifzPrefs.RecentSabqi item : prefs.recentSabqi()) {
            recentStreaks.put(recentKey(item), item.reviewStreak);
        }
    }

    private void reconcileRecent(LocalDate today) {
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        Map<String, Integer> next = new HashMap<>();
        for (HifzPrefs.RecentSabqi item : recent) {
            String key = recentKey(item);
            Integer before = recentStreaks.get(key);
            planner.acquireIndexes(item.startLine, item.endLine, item.addedOn);
            if (before != null && item.reviewStreak > before) {
                planner.reviewIndexes(item.startLine, item.endLine, today);
            }
            next.put(key, item.reviewStreak);
        }
        recentStreaks.clear();
        recentStreaks.putAll(next);
    }

    private void creditCurrentSabqi(LocalDate today) {
        int start = prefs.sabqiTodayReviewStartLine();
        int end = prefs.sabqiTodayReviewEndLine();
        if (start >= 0 && end >= start) planner.reviewIndexes(start, end, today);
    }

    private void creditItqan(LocalDate today) {
        String label = prefs.lastItqanLabel();
        if (label == null || label.isEmpty()) return;

        if (label.startsWith("Ancrage fractionné") && label.contains("bloc ") && prefs.itqanBlockIndex() > 0) {
            List<AnchoringQueue.Entry> queue = prefs.anchoringQueue();
            if (queue.isEmpty()) return;
            AnchoringQueue.Entry entry = queue.get(prefs.anchoringQueueIndex(queue.size()));
            VerseRef start = GeometryRepository.parseVerse(entry.start);
            VerseRef end = GeometryRepository.parseVerse(entry.end);
            List<String> unit = planner.lineIdsForVerseRange(start, end);
            int completed = Math.max(0, prefs.itqanBlockIndex() - 1);
            int count = PreviewConfig.fractionatedBlockCount(unit.size());
            if (completed >= count) return;
            int from = PreviewConfig.fractionatedBlockStart(unit.size(), completed);
            int len = PreviewConfig.fractionatedBlockLength(unit.size(), completed);
            planner.acquireAndReview(new ArrayList<>(unit.subList(from, from + len)), today);
            return;
        }

        VerseRef[] range = parseRange(label);
        if (range == null) return;
        List<String> unit = planner.lineIdsForVerseRange(range[0], range[1]);
        if (label.startsWith("Ancrage fractionné")) {
            int count = PreviewConfig.fractionatedBlockCount(unit.size());
            int last = Math.max(0, count - 1);
            int from = PreviewConfig.fractionatedBlockStart(unit.size(), last);
            int len = PreviewConfig.fractionatedBlockLength(unit.size(), last);
            planner.acquireAndReview(new ArrayList<>(unit.subList(from, from + len)), today);
        } else {
            planner.acquireAndReview(unit, today);
        }
    }

    private void creditMurajaah(LocalDate today) {
        VerseRef[] range = parseRange(prefs.lastMurajaahLabel());
        if (range == null) return;
        List<String> ids = planner.traversalLineIds(range[0], range[1]);
        if (!ids.isEmpty()) planner.acquireAndReview(ids, today);
    }

    static VerseRef[] parseRange(String label) {
        if (label == null) return null;
        Matcher m = RANGE.matcher(label);
        if (!m.find()) return null;
        try {
            return new VerseRef[]{
                new VerseRef(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))),
                new VerseRef(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)))
            };
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static String recentKey(HifzPrefs.RecentSabqi item) {
        return item.startLine + ":" + item.endLine;
    }
}
