package com.quransafeguard.hifz.preview;

import android.util.Log;

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

    /** Idempotent startup recovery closes any process-death gap between the Hifz and J10 writes. */
    void reconcileAll(LocalDate today) {
        planner.syncAcquired(today);
        reconcileRecent(today);
        LocalDate sabqi = parsedDate(prefs.lastSabqiDate());
        if (sabqi != null) creditCurrentSabqi(sabqi);
        LocalDate reprise = parsedDate(prefs.lastSabqiTodayReviewDate());
        if (reprise != null) creditCurrentSabqi(reprise);
        LocalDate itqan = parsedDate(prefs.lastItqanDate());
        if (itqan != null) creditItqan(itqan);
        LocalDate murajaah = parsedDate(prefs.lastMurajaahDate());
        if (murajaah != null) creditMurajaah(murajaah);
    }

    static boolean handlesPreferenceKey(String key) {
        return "recentSabqi".equals(key)
            || "lastSabqiDate".equals(key)
            || "lastSabqiTodayReviewDate".equals(key)
            || "lastItqanDate".equals(key)
            || "lastMurajaahDate".equals(key);
    }

    static boolean requiresFullReconcileKey(String key) {
        return "itqanRanges".equals(key)
            || "promotedRanges".equals(key)
            || "unconsolidatedPromotedRanges".equals(key)
            || "legacyMurajaahPromotedRanges".equals(key)
            || "hardAnchoringSurahs".equals(key)
            || "anchoringQueue".equals(key)
            || "anchoringQueueIndex".equals(key)
            || "itqanBlockIndex".equals(key);
    }

    void onPreferenceChanged(String key, LocalDate today) {
        if (key == null || today == null) return;
        switch (key) {
            case "recentSabqi":
                planner.syncAcquired(today);
                reconcileRecent(today);
                break;
            case "lastSabqiDate":
                planner.syncAcquired(today);
                creditCurrentSabqi(dateOrToday(prefs.lastSabqiDate(), today));
                break;
            case "lastSabqiTodayReviewDate":
                planner.syncAcquired(today);
                creditCurrentSabqi(dateOrToday(prefs.lastSabqiTodayReviewDate(), today));
                break;
            case "lastItqanDate":
                planner.syncAcquired(today);
                creditItqan(dateOrToday(prefs.lastItqanDate(), today));
                break;
            case "lastMurajaahDate":
                planner.syncAcquired(today);
                creditMurajaah(dateOrToday(prefs.lastMurajaahDate(), today));
                break;
            default:
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

    private void creditCurrentSabqi(LocalDate date) {
        int start = prefs.sabqiTodayReviewStartLine();
        int end = prefs.sabqiTodayReviewEndLine();
        if (start >= 0 && end >= start) planner.reviewIndexes(start, end, date);
    }

    private void creditItqan(LocalDate date) {
        VerseRef structuredStart = prefs.lastItqanCreditStart();
        VerseRef structuredEnd = prefs.lastItqanCreditEnd();
        if (structuredStart != null && structuredEnd != null) {
            List<String> unit = planner.lineIdsForVerseRange(structuredStart, structuredEnd);
            int blockIndex = prefs.lastItqanCreditBlockIndex();
            if (blockIndex < 0) {
                if (!unit.isEmpty()) planner.acquireAndReview(unit, date);
                return;
            }
            int[] segments = planner.surahSegmentLineCounts(structuredStart, structuredEnd);
            int count = PreviewConfig.fractionatedBlockCount(segments);
            if (blockIndex >= count) {
                Log.e("QuranHifz", "Ignoring invalid structured J10 Itqan block index " + blockIndex
                    + " for " + structuredStart + " → " + structuredEnd);
                return;
            }
            int from = PreviewConfig.fractionatedBlockStart(segments, blockIndex);
            int len = PreviewConfig.fractionatedBlockLength(segments, blockIndex);
            if (from < 0 || len <= 0 || from + len > unit.size()) {
                Log.e("QuranHifz", "Ignoring inconsistent structured J10 Itqan geometry for "
                    + structuredStart + " → " + structuredEnd);
                return;
            }
            planner.acquireAndReview(new ArrayList<>(unit.subList(from, from + len)), date);
            return;
        }

        // Historical v4 fallback only. New v5 commits never depend on a visible label.
        String label = prefs.lastItqanLabel();
        if (label == null || label.isEmpty()) return;
        Log.w("QuranHifz", "Using historical visible-label J10 Itqan fallback");

        if (label.startsWith("Ancrage fractionné") && label.contains("bloc ") && prefs.itqanBlockIndex() > 0) {
            List<AnchoringQueue.Entry> queue = prefs.anchoringQueue();
            if (queue.isEmpty()) return;
            AnchoringQueue.Entry entry = prefs.inProgressAnchoringEntry();
            if (entry == null) entry = queue.get(prefs.anchoringQueueIndex(queue.size()));
            VerseRef start = GeometryRepository.parseVerse(entry.start);
            VerseRef end = GeometryRepository.parseVerse(entry.end);
            List<String> unit = planner.lineIdsForVerseRange(start, end);
            int completed = Math.max(0, prefs.itqanBlockIndex() - 1);
            int[] segments = planner.surahSegmentLineCounts(start, end);
            int count = PreviewConfig.fractionatedBlockCount(segments);
            if (completed >= count) return;
            int from = PreviewConfig.fractionatedBlockStart(segments, completed);
            int len = PreviewConfig.fractionatedBlockLength(segments, completed);
            if (from + len <= unit.size()) {
                planner.acquireAndReview(new ArrayList<>(unit.subList(from, from + len)), date);
            }
            return;
        }

        VerseRef[] range = parseRange(label);
        if (range == null) return;
        List<String> unit = planner.lineIdsForVerseRange(range[0], range[1]);
        if (label.startsWith("Ancrage fractionné")) {
            int[] segments = planner.surahSegmentLineCounts(range[0], range[1]);
            int count = PreviewConfig.fractionatedBlockCount(segments);
            int last = Math.max(0, count - 1);
            int from = PreviewConfig.fractionatedBlockStart(segments, last);
            int len = PreviewConfig.fractionatedBlockLength(segments, last);
            if (from + len <= unit.size()) {
                planner.acquireAndReview(new ArrayList<>(unit.subList(from, from + len)), date);
            }
        } else {
            planner.acquireAndReview(unit, date);
        }
    }

    private void creditMurajaah(LocalDate date) {
        VerseRef structuredStart = prefs.lastMurajaahCreditStart();
        VerseRef structuredEnd = prefs.lastMurajaahCreditEnd();
        if (structuredStart != null && structuredEnd != null) {
            List<String> ids = planner.traversalLineIds(structuredStart, structuredEnd);
            if (!ids.isEmpty()) planner.acquireAndReview(ids, date);
            return;
        }

        // Historical v4 fallback only. New v5 commits never depend on a visible label.
        String label = prefs.lastMurajaahLabel();
        VerseRef[] range = parseRange(label);
        if (range == null) return;
        Log.w("QuranHifz", "Using historical visible-label J10 Murajaah fallback");
        List<String> ids = planner.traversalLineIds(range[0], range[1]);
        if (!ids.isEmpty()) planner.acquireAndReview(ids, date);
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

    private static LocalDate parsedDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try { return LocalDate.parse(text); }
        catch (RuntimeException invalid) { return null; }
    }

    private static LocalDate dateOrToday(String text, LocalDate today) {
        LocalDate parsed = parsedDate(text);
        return parsed == null ? today : parsed;
    }

    private static String recentKey(HifzPrefs.RecentSabqi item) {
        return item.startLine + ":" + item.endLine;
    }
}
