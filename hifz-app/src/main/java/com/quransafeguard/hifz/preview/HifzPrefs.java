package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Locale;
import java.util.UUID;

/** Versioned local persistence. Structured Hifz and free memorization are intentionally isolated. */
public final class HifzPrefs {
    public static final class RecentSabqi {
        public final int startLine;
        public final int endLine;
        RecentSabqi(int startLine, int endLine) { this.startLine = startLine; this.endLine = endLine; }
        @Override public String toString() { return startLine + "–" + endLine; }
    }

    public static final class LineInterval {
        public final int startLine;
        public final int endLine;
        LineInterval(int startLine, int endLine) {
            this.startLine = Math.min(startLine, endLine);
            this.endLine = Math.max(startLine, endLine);
        }
    }

    private static final String NAME = "quran_hifz_preview_v1";
    private static final String LEGACY_GATES = "hifz_preview_session_gates";
    private final SharedPreferences p;

    public HifzPrefs(Context context) {
        p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        ensureSchema();
        migrateLegacyGates(context);
    }

    private void ensureSchema() {
        int schema = p.getInt("schema", 0);
        if (schema == 0) {
            SharedPreferences.Editor e = p.edit()
                .putInt("schema", PreviewConfig.SCHEMA_VERSION)
                .putString("programStartDate", LocalDate.now().toString())
                .putString("lowerBound", "2:1")
                .putString("promotedFrontier", "2:74")
                .putString("upperTailStart", "49:1")
                .putString("sabqiStart", "2:75")
                .putString("sabqiEnd", "2:286")
                .putString("itqanRanges", defaultItqanRangesJson())
                .putString("promotedRanges", "[]")
                .putString("unconsolidatedPromotedRanges", "[]")
                .putString("legacyMurajaahPromotedRanges", "[]")
                .putString("itqanRotationStart", "49:1")
                .putString("itqanCursor", "49:1")
                .putString("murajaahCursor", "49:1")
                .putInt("sabqiLineCursor", -1)
                .putInt("sabqiRep", 0)
                .putInt("sabqiAssisted", 0)
                .putInt("itqanRep", 0)
                .putInt("itqanAssisted", 0)
                .putString("itqanUnitStart", "")
                .putString("itqanUnitEnd", "")
                .putString("recentSabqi", "[]")
                .putString("stableRecentLines", "[]")
                .putLong("sabqiElapsedMs", 0L)
                .putLong("itqanElapsedMs", 0L)
                .putLong("sabqi_today_reviewElapsedMs", 0L)
                .putLong("recent_sabqi_reviewElapsedMs", 0L)
                .putLong("murajaahElapsedMs", 0L)
                .putString("sabqiTodayReviewDate", "")
                .putInt("sabqiTodayReviewStartLine", -1)
                .putInt("sabqiTodayReviewEndLine", -1)
                .putString("lastSabqiTodayReviewDate", "")
                .putString("lastSabqiTodayReviewLabel", "")
                .putInt("recentSabqiReviewIndex", 0)
                .putString("lastRecentSabqiReviewDate", "")
                .putString("lastRecentSabqiReviewLabel", "")
                .putString("murajaahActualEnd", "")
                .putFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING)
                .putFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING)
                .putBoolean("forceEink", false)
                .putString("lastSabqiDate", "")
                .putString("lastSabqiLabel", "")
                .putString("lastItqanDate", "")
                .putString("lastItqanLabel", "")
                .putString("lastMurajaahDate", "")
                .putString("lastMurajaahLabel", "");
            if (!e.commit()) throw new IllegalStateException("Unable to initialize Hifz schema");
            return;
        }
        if (schema == 1) {
            migrateV1ToV2();
            schema = 2;
        }
        if (schema == 2) {
            migrateV2ToV3();
            return;
        }
        if (schema != PreviewConfig.SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported Hifz preview schema: " + schema);
        }
    }

    private void migrateV1ToV2() {
        VerseRef legacyFrontier = safeRef(p.getString("promotedFrontier", "2:74"), new VerseRef(2,74));
        ArrayList<VerseRange> promoted = new ArrayList<>();
        if (GeometryRepository.ordinal(legacyFrontier) > GeometryRepository.ordinal(new VerseRef(2,74))) {
            promoted.add(new VerseRange(new VerseRef(2,75), legacyFrontier));
        }
        SharedPreferences.Editor e = p.edit()
            .putString("sabqiStart", p.getString("sabqiStart", "2:75"))
            .putString("sabqiEnd", p.getString("sabqiEnd", "2:286"))
            .putString("itqanRanges", defaultItqanRangesJson())
            .putString("promotedRanges", rangesJson(promoted))
            .putString("itqanRotationStart", p.getString("itqanRotationStart", "49:1"))
            .putString("stableRecentLines", p.getString("stableRecentLines", "[]"))
            .putFloat("recentSecPerLine", p.getFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING))
            .putString("murajaahPhase", p.getString("murajaahPhase", "A"))
            .putString("murajaahActualEnd", p.getString("murajaahActualEnd", ""))
            .putInt("murajaahRecentLinesDone", p.getInt("murajaahRecentLinesDone", 0))
            .putLong("murajaahBlockAElapsedMs", p.getLong("murajaahBlockAElapsedMs", 0L))
            .putLong("murajaahBlockBElapsedMs", p.getLong("murajaahBlockBElapsedMs", 0L))
            .putInt("schema", 2);
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v1 to v2");
    }

    /**
     * Schema v3 introduces snowball consolidation state without moving any existing cursor.
     * Existing promoted ranges stay visible to Murajaah for migration compatibility, but are
     * also marked unconsolidated so their next natural-cycle Itqan encounter performs a real ×40.
     */
    private void migrateV2ToV3() {
        String historical = p.getString("promotedRanges", "[]");
        long legacyRecentElapsed = p.getLong("murajaahBlockAElapsedMs", 0L);
        long legacyOldElapsed = p.getLong("murajaahBlockBElapsedMs", 0L);
        String legacyActualEnd = p.getString("murajaahActualEnd", "");
        int legacyRecentIndex = Math.max(0, p.getInt("murajaahRecentLinesDone", 0) / PreviewConfig.SABQI_LINES);
        SharedPreferences.Editor e = p.edit()
            .putString("unconsolidatedPromotedRanges", historical == null ? "[]" : historical)
            .putString("legacyMurajaahPromotedRanges", historical == null ? "[]" : historical)
            .putLong("recent_sabqi_reviewElapsedMs", legacyRecentElapsed)
            .putInt("recentSabqiReviewIndex", legacyRecentIndex)
            .putLong("murajaahElapsedMs", legacyOldElapsed)
            .putString("murajaahActualEnd", legacyActualEnd == null ? "" : legacyActualEnd)
            .remove("murajaahPhase")
            .remove("murajaahRecentLinesDone")
            .remove("murajaahBlockAElapsedMs")
            .remove("murajaahBlockBElapsedMs")
            .putInt("schema", 3);
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v2 to v3");
    }

    private void migrateLegacyGates(Context context) {
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_GATES, Context.MODE_PRIVATE);
        if (legacy.getAll().isEmpty()) return;
        SharedPreferences.Editor e = p.edit();
        copyLegacyIfMissing(legacy, e, "lastSabqiDate");
        copyLegacyIfMissing(legacy, e, "lastSabqiLabel");
        copyLegacyIfMissing(legacy, e, "lastItqanDate");
        copyLegacyIfMissing(legacy, e, "lastItqanLabel");
        copyLegacyIfMissing(legacy, e, "lastMurajaahDate");
        copyLegacyIfMissing(legacy, e, "lastMurajaahLabel");
        e.apply();
    }

    private void copyLegacyIfMissing(SharedPreferences legacy, SharedPreferences.Editor e, String key) {
        if (p.contains(key)) return;
        String value = legacy.getString(key, "");
        if (value != null && !value.isEmpty()) e.putString(key, value);
    }

    public int schema() { return p.getInt("schema", 0); }
    public LocalDate programStartDate() { return LocalDate.parse(required("programStartDate")); }
    public void setProgramStartDate(LocalDate value) { p.edit().putString("programStartDate", value.toString()).apply(); }

    private static String maskEntropyKey(String mode) {
        if (mode == null || mode.trim().isEmpty()) throw new IllegalArgumentException("mask entropy mode required");
        return "maskEntropy_" + mode.toLowerCase(Locale.ROOT);
    }

    public String maskEntropyFor(String mode) {
        String key = maskEntropyKey(mode);
        String current = p.getString(key, "");
        if (current != null && !current.isEmpty()) return current;
        String created = UUID.randomUUID().toString();
        if (!p.edit().putString(key, created).commit()) throw new IllegalStateException("Unable to persist Hifz mask entropy");
        return created;
    }

    public void clearMaskEntropy(String mode) {
        if (!p.edit().remove(maskEntropyKey(mode)).commit()) throw new IllegalStateException("Unable to clear Hifz mask entropy");
    }

    private static String maskEntropyKey(String mode) {
        if (mode == null || mode.trim().isEmpty()) throw new IllegalArgumentException("mask entropy mode required");
        return "maskEntropy_" + mode.toLowerCase(Locale.ROOT);
    }

    public String maskEntropyFor(String mode) {
        String key = maskEntropyKey(mode);
        String current = p.getString(key, "");
        if (current != null && !current.isEmpty()) return current;
        String created = UUID.randomUUID().toString();
        if (!p.edit().putString(key, created).commit()) throw new IllegalStateException("Unable to persist Hifz mask entropy");
        return created;
    }

    public void clearMaskEntropy(String mode) {
        if (!p.edit().remove(maskEntropyKey(mode)).commit()) throw new IllegalStateException("Unable to clear Hifz mask entropy");
    }

    // Legacy diagnostics retained for migration visibility; schema v3 corpus APIs below are authoritative.
    public VerseRef lowerBound() { return safeRef(p.getString("lowerBound", "2:1"), new VerseRef(2,1)); }
    public VerseRef promotedFrontier() { return safeRef(p.getString("promotedFrontier", "2:74"), new VerseRef(2,74)); }
    public VerseRef upperTailStart() { return safeRef(p.getString("upperTailStart", "49:1"), new VerseRef(49,1)); }
    public void setPromotedFrontier(VerseRef value) { putRef("promotedFrontier", value); }

    public VerseRef sabqiStart() { return ref("sabqiStart"); }
    public VerseRef sabqiEnd() { return ref("sabqiEnd"); }
    public void setSabqiStart(VerseRef value) { putRef("sabqiStart", value); }
    public void setSabqiEnd(VerseRef value) { putRef("sabqiEnd", value); }

    public List<VerseRange> itqanRanges() { return parseRanges("itqanRanges"); }
    public List<VerseRange> promotedRanges() { return parseRangesAllowEmpty("promotedRanges"); }
    public List<VerseRange> unconsolidatedPromotedRanges() { return parseRangesAllowEmpty("unconsolidatedPromotedRanges"); }
    public List<VerseRange> legacyMurajaahPromotedRanges() { return parseRangesAllowEmpty("legacyMurajaahPromotedRanges"); }

    public boolean setItqanRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return false;
        List<VerseRange> normalized = normalizeRanges(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }

    public VerseRef itqanRotationStart() { return ref("itqanRotationStart"); }
    public void setItqanRotationStart(VerseRef value) { putRef("itqanRotationStart", value); }

    public VerseRef itqanCursor() { return ref("itqanCursor"); }
    public VerseRef murajaahCursor() { return ref("murajaahCursor"); }
    public void setItqanCursor(VerseRef value) { putRef("itqanCursor", value); }

    /** All base Itqan plus every snowball promotion, irrespective of consolidation status. */
    public EligibleCorpus itqanWorkCorpus() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(promotedRanges());
        return EligibleCorpus.Companion.of(all);
    }

    /** Compatibility alias while the runtime migration is staged. */
    public EligibleCorpus corpus() { return itqanWorkCorpus(); }

    /**
     * Murajaah sees base Itqan, all already-consolidated promotions, and historical promoted
     * ranges retained during v2->v3 migration. Fresh unconsolidated promotions stay hidden.
     */
    public EligibleCorpus murajaahCorpus() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(legacyMurajaahPromotedRanges());
        List<VerseRange> consolidated = new ArrayList<>(promotedRanges());
        for (VerseRange pending : unconsolidatedPromotedRanges()) {
            consolidated = subtractCoverage(consolidated, pending.getStart(), pending.getEndInclusive());
        }
        all.addAll(consolidated);
        return EligibleCorpus.Companion.of(all);
    }

    public boolean isItqanCursorValid() { try { return itqanWorkCorpus().contains(itqanCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isMurajaahCursorValid() { try { return murajaahCorpus().contains(murajaahCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isRotationStartValid() { try { return itqanWorkCorpus().contains(itqanRotationStart()); } catch (RuntimeException e) { return false; } }

    public int sabqiLineCursor() { return p.getInt("sabqiLineCursor", -1); }
    public void setSabqiLineCursor(int value) { p.edit().putInt("sabqiLineCursor", value).apply(); }
    public int sabqiRep() { return p.getInt("sabqiRep", 0); }
    public int sabqiAssisted() { return p.getInt("sabqiAssisted", 0); }
    public boolean setSabqiProgress(int rep, int assisted) {
        return p.edit().putInt("sabqiRep", rep).putInt("sabqiAssisted", assisted).commit();
    }

    /** Sabqi completion queues the five-line block; promotion is deliberately deferred to recent-window pressure. */
    public boolean completeSabqiBlock(int startLine, int endLine, int nextLineCursor, String date, String label) {
        List<RecentSabqi> queue = recentSabqi();
        queue.add(new RecentSabqi(startLine, endLine));
        return p.edit()
            .putString("recentSabqi", recentJson(queue))
            .putInt("sabqiLineCursor", nextLineCursor)
            .putInt("sabqiRep", 0)
            .putInt("sabqiAssisted", 0)
            .putLong("sabqiElapsedMs", 0L)
            .putString("sabqiTodayReviewDate", date)
            .putInt("sabqiTodayReviewStartLine", startLine)
            .putInt("sabqiTodayReviewEndLine", endLine)
            .putLong("sabqi_today_reviewElapsedMs", 0L)
            .putString("lastSabqiDate", date)
            .putString("lastSabqiLabel", label)
            .commit();
    }

    /** Compatibility overload: promotion is handled by the sliding-window rebalance. */
    public boolean completeSabqiBlock(int startLine, int endLine, VerseRef ignoredPromotion,
                                      int nextLineCursor, String date, String label) {
        return completeSabqiBlock(startLine, endLine, nextLineCursor, date, label);
    }

    public int itqanRep() { return p.getInt("itqanRep", 0); }
    public int itqanAssisted() { return p.getInt("itqanAssisted", 0); }
    public VerseRef itqanUnitStart() { return optionalRef("itqanUnitStart"); }
    public VerseRef itqanUnitEnd() { return optionalRef("itqanUnitEnd"); }

    public boolean setItqanProgress(int rep, int assisted, VerseRef unitStart, VerseRef unitEnd) {
        return p.edit()
            .putInt("itqanRep", rep)
            .putInt("itqanAssisted", assisted)
            .putString("itqanUnitStart", unitStart == null ? "" : unitStart.toString())
            .putString("itqanUnitEnd", unitEnd == null ? "" : unitEnd.toString())
            .commit();
    }

    public boolean completeItqanUnit(VerseRef nextCursor, String date, String label) {
        return p.edit()
            .putString("itqanCursor", nextCursor.toString())
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .commit();
    }

    /** Atomically advances the natural Itqan cycle and consolidates any completed promoted overlap. */
    public boolean completeItqanUnitAndConsolidate(VerseRef start, VerseRef endInclusive,
                                                   VerseRef nextCursor, String date, String label) {
        List<VerseRange> pending = subtractCoverage(unconsolidatedPromotedRanges(), start, endInclusive);
        return p.edit()
            .putString("unconsolidatedPromotedRanges", rangesJson(pending))
            .putString("itqanCursor", nextCursor.toString())
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .commit();
    }

    public String sabqiTodayReviewDate() { return p.getString("sabqiTodayReviewDate", ""); }
    public int sabqiTodayReviewStartLine() { return p.getInt("sabqiTodayReviewStartLine", -1); }
    public int sabqiTodayReviewEndLine() { return p.getInt("sabqiTodayReviewEndLine", -1); }
    public String lastSabqiTodayReviewDate() { return p.getString("lastSabqiTodayReviewDate", ""); }
    public String lastSabqiTodayReviewLabel() { return p.getString("lastSabqiTodayReviewLabel", ""); }
    public boolean completeSabqiTodayReview(String date, String label) {
        return p.edit()
            .putLong("sabqi_today_reviewElapsedMs", 0L)
            .putString("lastSabqiTodayReviewDate", date)
            .putString("lastSabqiTodayReviewLabel", label)
            .commit();
    }

    public int recentSabqiReviewIndex() { return Math.max(0, p.getInt("recentSabqiReviewIndex", 0)); }
    public void setRecentSabqiReviewIndex(int value) {
        p.edit().putInt("recentSabqiReviewIndex", Math.max(0, value)).apply();
    }
    public String lastRecentSabqiReviewDate() { return p.getString("lastRecentSabqiReviewDate", ""); }
    public String lastRecentSabqiReviewLabel() { return p.getString("lastRecentSabqiReviewLabel", ""); }
    public boolean completeRecentSabqiReview(String date, int nextIndex, String label) {
        return p.edit()
            .putLong("recent_sabqi_reviewElapsedMs", 0L)
            .putInt("recentSabqiReviewIndex", Math.max(0, nextIndex))
            .putString("lastRecentSabqiReviewDate", date)
            .putString("lastRecentSabqiReviewLabel", label)
            .commit();
    }

    public VerseRef murajaahActualEnd() { return optionalRef("murajaahActualEnd"); }
    public void setMurajaahActualEnd(VerseRef value) {
        p.edit().putString("murajaahActualEnd", value == null ? "" : value.toString()).apply();
    }

    public boolean completeMurajaah(VerseRef nextCursor, String date, String label) {
        return p.edit()
            .putString("murajaahCursor", nextCursor.toString())
            .putLong("murajaahElapsedMs", 0L)
            .putString("murajaahActualEnd", "")
            .putString("lastMurajaahDate", date)
            .putString("lastMurajaahLabel", label)
            .commit();
    }

    public String lastSabqiDate() { return p.getString("lastSabqiDate", ""); }
    public String lastSabqiLabel() { return p.getString("lastSabqiLabel", ""); }
    public String lastItqanDate() { return p.getString("lastItqanDate", ""); }
    public String lastItqanLabel() { return p.getString("lastItqanLabel", ""); }
    public String lastMurajaahDate() { return p.getString("lastMurajaahDate", ""); }
    public String lastMurajaahLabel() { return p.getString("lastMurajaahLabel", ""); }

    public long elapsedFor(String mode) { return p.getLong(mode.toLowerCase() + "ElapsedMs", 0L); }
    public void setElapsedFor(String mode, long value) { p.edit().putLong(mode.toLowerCase() + "ElapsedMs", Math.max(0L, value)).apply(); }

    public double murajaahSecondsPerLine() { return p.getFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING); }
    public void setMurajaahSecondsPerLine(double value) {
        if (value > 0.0 && Double.isFinite(value)) p.edit().putFloat("murajaahSecPerLine", (float) value).apply();
    }
    public double recentSecondsPerLine() { return p.getFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING); }
    public void setRecentSecondsPerLine(double value) {
        if (value > 0.0 && Double.isFinite(value)) p.edit().putFloat("recentSecPerLine", (float) value).apply();
    }

    public boolean forceEink() { return p.getBoolean("forceEink", false); }
    public void setForceEink(boolean value) { p.edit().putBoolean("forceEink", value).apply(); }

    public List<RecentSabqi> recentSabqi() {
        ArrayList<RecentSabqi> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString("recentSabqi", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(new RecentSabqi(o.getInt("start"), o.getInt("end")));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt recent Sabqi queue", error);
        }
        return out;
    }

    public void addRecentSabqi(int start, int end) {
        List<RecentSabqi> queue = recentSabqi();
        queue.add(new RecentSabqi(start, end));
        saveRecent(queue);
    }

    public void removeFirstRecentSabqi() {
        List<RecentSabqi> queue = recentSabqi();
        if (!queue.isEmpty()) queue.remove(0);
        saveRecent(queue);
    }

    /** Mark the oldest recent block stable and preserve its line coverage for verse-level promotion. */
    public boolean markFirstRecentStable() {
        List<RecentSabqi> queue = recentSabqi();
        if (queue.isEmpty()) return false;
        RecentSabqi item = queue.remove(0);
        List<LineInterval> stable = stableRecentLines();
        stable.add(new LineInterval(item.startLine, item.endLine));
        stable = mergeIntervals(stable);
        return p.edit()
            .putString("recentSabqi", recentJson(queue))
            .putString("stableRecentLines", intervalsJson(stable))
            .commit();
    }

    /** Move a difficult block behind the other recent work instead of blocking the queue. */
    public boolean deferFirstRecentSabqi() {
        List<RecentSabqi> queue = recentSabqi();
        if (queue.isEmpty()) return false;
        RecentSabqi item = queue.remove(0);
        queue.add(item);
        return p.edit().putString("recentSabqi", recentJson(queue)).commit();
    }

    public List<LineInterval> stableRecentLines() {
        ArrayList<LineInterval> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString("stableRecentLines", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(new LineInterval(o.getInt("start"), o.getInt("end")));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt stable recent-line coverage", error);
        }
        return mergeIntervals(out);
    }

    /**
     * Idempotently grows the Itqan snowball. Only verses not already promoted are added to the
     * unconsolidated set, so a restart/recalculation can never re-open a completed ×40 passage.
     */
    public boolean addPromotedVerses(List<VerseRef> verses) {
        if (verses == null || verses.isEmpty()) return true;
        ArrayList<VerseRef> fresh = new ArrayList<>();
        for (VerseRef verse : verses) if (!isPromoted(verse)) fresh.add(verse);
        if (fresh.isEmpty()) return true;
        fresh.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        List<VerseRange> additions = rangesFromVerses(fresh);

        ArrayList<VerseRange> all = new ArrayList<>(promotedRanges());
        all.addAll(additions);
        ArrayList<VerseRange> pending = new ArrayList<>(unconsolidatedPromotedRanges());
        pending.addAll(additions);
        return p.edit()
            .putString("promotedRanges", rangesJson(normalizeRanges(all)))
            .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
            .commit();
    }

    public boolean isPromoted(VerseRef verse) {
        for (VerseRange range : promotedRanges()) if (range.contains(verse)) return true;
        return false;
    }

    public boolean isUnconsolidatedPromoted(VerseRef verse) {
        for (VerseRange range : unconsolidatedPromotedRanges()) if (range.contains(verse)) return true;
        return false;
    }

    /** Mark only the completed overlap consolidated; unrelated pending promotions remain queued in-place. */
    public boolean markPromotedConsolidated(VerseRef start, VerseRef endInclusive) {
        List<VerseRange> next = subtractCoverage(unconsolidatedPromotedRanges(), start, endInclusive);
        return p.edit().putString("unconsolidatedPromotedRanges", rangesJson(next)).commit();
    }

    private void saveRecent(List<RecentSabqi> queue) {
        p.edit().putString("recentSabqi", recentJson(queue)).apply();
    }

    private String recentJson(List<RecentSabqi> queue) {
        JSONArray array = new JSONArray();
        try {
            for (RecentSabqi item : queue) {
                JSONObject o = new JSONObject();
                o.put("start", item.startLine);
                o.put("end", item.endLine);
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    private List<VerseRange> parseRanges(String key) {
        List<VerseRange> ranges = parseRangesAllowEmpty(key);
        if (ranges.isEmpty()) throw new IllegalStateException("No Itqan range configured");
        return normalizeRanges(ranges);
    }

    private List<VerseRange> parseRangesAllowEmpty(String key) {
        ArrayList<VerseRange> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString(key, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(new VerseRange(GeometryRepository.parseVerse(o.getString("start")), GeometryRepository.parseVerse(o.getString("end"))));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt range list: " + key, error);
        }
        return out;
    }

    private static List<VerseRange> normalizeRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(EligibleCorpus.Companion.of(ranges).getRanges());
    }

    private static List<VerseRange> rangesFromVerses(List<VerseRef> orderedVerses) {
        ArrayList<VerseRange> additions = new ArrayList<>();
        if (orderedVerses.isEmpty()) return additions;
        VerseRef start = orderedVerses.get(0);
        VerseRef previous = start;
        for (int i = 1; i < orderedVerses.size(); i++) {
            VerseRef current = orderedVerses.get(i);
            if (GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1) {
                previous = current;
            } else {
                additions.add(new VerseRange(start, previous));
                start = previous = current;
            }
        }
        additions.add(new VerseRange(start, previous));
        return additions;
    }

    private static List<VerseRange> subtractCoverage(List<VerseRange> source, VerseRef cutStart, VerseRef cutEndInclusive) {
        int cutA = GeometryRepository.ordinal(cutStart);
        int cutB = GeometryRepository.ordinal(cutEndInclusive);
        if (cutB < cutA) { int swap = cutA; cutA = cutB; cutB = swap; }
        ArrayList<VerseRange> out = new ArrayList<>();
        for (VerseRange range : source) {
            int a = GeometryRepository.ordinal(range.getStart());
            int b = GeometryRepository.ordinal(range.getEndInclusive());
            if (cutB < a || cutA > b) {
                out.add(range);
                continue;
            }
            if (a < cutA) {
                out.add(new VerseRange(range.getStart(), QuranCanon.INSTANCE.fromOrdinal(cutA - 1)));
            }
            if (cutB < b) {
                out.add(new VerseRange(QuranCanon.INSTANCE.fromOrdinal(cutB + 1), range.getEndInclusive()));
            }
        }
        return normalizeRanges(out);
    }

    private static String defaultItqanRangesJson() {
        ArrayList<VerseRange> ranges = new ArrayList<>();
        ranges.add(new VerseRange(new VerseRef(2,1), new VerseRef(2,74)));
        ranges.add(new VerseRange(new VerseRef(49,1), new VerseRef(114,6)));
        return rangesJson(ranges);
    }

    private static String rangesJson(List<VerseRange> ranges) {
        JSONArray array = new JSONArray();
        try {
            for (VerseRange range : ranges) {
                JSONObject o = new JSONObject();
                o.put("start", range.getStart().toString());
                o.put("end", range.getEndInclusive().toString());
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    private static List<LineInterval> mergeIntervals(List<LineInterval> source) {
        ArrayList<LineInterval> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.comparingInt(a -> a.startLine));
        ArrayList<LineInterval> out = new ArrayList<>();
        for (LineInterval current : sorted) {
            if (out.isEmpty()) out.add(current);
            else {
                LineInterval previous = out.get(out.size() - 1);
                if (current.startLine <= previous.endLine + 1) {
                    out.set(out.size() - 1, new LineInterval(previous.startLine, Math.max(previous.endLine, current.endLine)));
                } else out.add(current);
            }
        }
        return out;
    }

    private static String intervalsJson(List<LineInterval> intervals) {
        JSONArray array = new JSONArray();
        try {
            for (LineInterval item : intervals) {
                JSONObject o = new JSONObject();
                o.put("start", item.startLine);
                o.put("end", item.endLine);
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    public void resetPreviewState() {
        p.edit().clear().commit();
        ensureSchema();
    }

    private VerseRef ref(String key) { return GeometryRepository.parseVerse(required(key)); }
    private VerseRef optionalRef(String key) {
        String value = p.getString(key, "");
        return value == null || value.isEmpty() ? null : GeometryRepository.parseVerse(value);
    }
    private void putRef(String key, VerseRef value) { p.edit().putString(key, value.toString()).apply(); }
    private String required(String key) {
        String value = p.getString(key, null);
        if (value == null) throw new IllegalStateException("Missing Hifz state key: " + key);
        return value;
    }
    private static VerseRef safeRef(String value, VerseRef fallback) {
        try { return value == null || value.isEmpty() ? fallback : GeometryRepository.parseVerse(value); }
        catch (RuntimeException error) { return fallback; }
    }
}
