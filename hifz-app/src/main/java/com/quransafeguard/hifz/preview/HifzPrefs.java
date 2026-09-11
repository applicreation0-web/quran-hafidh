package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzState;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Versioned preview persistence. Structured Hifz and free memorization are intentionally isolated. */
public final class HifzPrefs {
    public static final class RecentSabqi {
        public final int startLine;
        public final int endLine;
        RecentSabqi(int startLine, int endLine) { this.startLine = startLine; this.endLine = endLine; }
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
            p.edit()
                .putInt("schema", PreviewConfig.SCHEMA_VERSION)
                .putString("programStartDate", LocalDate.now().toString())
                .putString("lowerBound", "2:1")
                .putString("promotedFrontier", "2:74")
                .putString("upperTailStart", "49:1")
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
                .putLong("sabqiElapsedMs", 0L)
                .putLong("itqanElapsedMs", 0L)
                .putLong("murajaahElapsedMs", 0L)
                .putFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING)
                .putBoolean("forceEink", false)
                .putString("lastSabqiDate", "")
                .putString("lastSabqiLabel", "")
                .putString("lastItqanDate", "")
                .putString("lastItqanLabel", "")
                .putString("lastMurajaahDate", "")
                .putString("lastMurajaahLabel", "")
                .apply();
        } else if (schema != PreviewConfig.SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported Hifz preview schema: " + schema);
        }
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

    public VerseRef lowerBound() { return ref("lowerBound"); }
    public VerseRef promotedFrontier() { return ref("promotedFrontier"); }
    public VerseRef upperTailStart() { return ref("upperTailStart"); }
    public VerseRef itqanCursor() { return ref("itqanCursor"); }
    public VerseRef murajaahCursor() { return ref("murajaahCursor"); }

    public void setPromotedFrontier(VerseRef value) { putRef("promotedFrontier", value); }
    public void setItqanCursor(VerseRef value) { putRef("itqanCursor", value); }
    public void setMurajaahCursor(VerseRef value) { putRef("murajaahCursor", value); }

    public EligibleCorpus corpus() {
        return EligibleCorpus.Companion.dynamic(lowerBound(), promotedFrontier(), upperTailStart());
    }

    public HifzState state() {
        return new HifzState(lowerBound(), promotedFrontier(), upperTailStart(), itqanCursor(), murajaahCursor());
    }

    public int sabqiLineCursor() { return p.getInt("sabqiLineCursor", -1); }
    public void setSabqiLineCursor(int value) { p.edit().putInt("sabqiLineCursor", value).apply(); }
    public int sabqiRep() { return p.getInt("sabqiRep", 0); }
    public int sabqiAssisted() { return p.getInt("sabqiAssisted", 0); }
    public boolean setSabqiProgress(int rep, int assisted) {
        return p.edit().putInt("sabqiRep", rep).putInt("sabqiAssisted", assisted).commit();
    }

    /** Atomic Sabqi completion: queue, promotion, cursor, reset, timer and day gate together. */
    public boolean completeSabqiBlock(int startLine, int endLine, VerseRef promotion,
                                      int nextLineCursor, String date, String label) {
        List<RecentSabqi> queue = recentSabqi();
        queue.add(new RecentSabqi(startLine, endLine));
        String queueJson = recentJson(queue);
        VerseRef frontier = promotedFrontier();
        SharedPreferences.Editor e = p.edit()
            .putString("recentSabqi", queueJson)
            .putInt("sabqiLineCursor", nextLineCursor)
            .putInt("sabqiRep", 0)
            .putInt("sabqiAssisted", 0)
            .putLong("sabqiElapsedMs", 0L)
            .putString("lastSabqiDate", date)
            .putString("lastSabqiLabel", label);
        if (promotion != null && GeometryRepository.ordinal(promotion) > GeometryRepository.ordinal(frontier)) {
            e.putString("promotedFrontier", promotion.toString());
        }
        return e.commit();
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

    public boolean completeMurajaah(VerseRef nextCursor, String date, String label) {
        return p.edit()
            .putString("murajaahCursor", nextCursor.toString())
            .putLong("murajaahElapsedMs", 0L)
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
}
