package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Versioned structured-Hifz persistence. Free memorization is intentionally stored elsewhere. */
public final class HifzProgressStore {
    public static final int SCHEMA = 2;
    private static final String NAME = "quran_hifz_program_v2";
    private final SharedPreferences p;

    public static final class RecentSabqi {
        public final int startGlobalLine;
        public final int endGlobalLine;
        RecentSabqi(int startGlobalLine, int endGlobalLine) { this.startGlobalLine = startGlobalLine; this.endGlobalLine = endGlobalLine; }
    }

    public HifzProgressStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        int existing = p.getInt("schema", 0);
        if (existing != 0 && existing != SCHEMA) throw new IllegalStateException("Unsupported Hifz program schema: " + existing);
        if (existing == 0) p.edit().putInt("schema", SCHEMA).putString("recentSabqi", "[]").putFloat("murajaahSecondsPerLine", 9f).apply();
    }

    public boolean isConfigured() {
        return readRef("sabqiStart") != null && readRef("sabqiEnd") != null && readRef("itqanStart") != null && readRef("itqanEnd") != null;
    }

    public void configure(VerseRef sabqiStart, VerseRef sabqiEnd, VerseRef itqanStart, VerseRef itqanEnd) {
        requireOrdered(sabqiStart, sabqiEnd, "Sabqi");
        requireOrdered(itqanStart, itqanEnd, "Itqan");
        p.edit()
            .putString("sabqiStart", sabqiStart.toString()).putString("sabqiEnd", sabqiEnd.toString())
            .putString("itqanStart", itqanStart.toString()).putString("itqanEnd", itqanEnd.toString())
            .putString("sabqiCursor", sabqiStart.toString()).putString("itqanCursor", itqanStart.toString())
            .putString("murajaahCursor", itqanStart.toString())
            .putInt("sabqiRep", 0).putInt("sabqiAssisted", 0)
            .putInt("itqanRep", 0).putInt("itqanAssisted", 0)
            .putString("recentSabqi", "[]")
            .putString("programStartDate", LocalDate.now().toString())
            .remove("lastSabqiDate").remove("lastItqanDate").remove("lastMurajaahDate")
            .apply();
    }

    public VerseRef sabqiStart() { return requiredRef("sabqiStart"); }
    public VerseRef sabqiEnd() { return requiredRef("sabqiEnd"); }
    public VerseRef itqanStart() { return requiredRef("itqanStart"); }
    public VerseRef itqanEnd() { return requiredRef("itqanEnd"); }
    public VerseRef sabqiCursor() { return requiredRef("sabqiCursor"); }
    public VerseRef itqanCursor() { return requiredRef("itqanCursor"); }
    public VerseRef murajaahCursor() { return requiredRef("murajaahCursor"); }

    public void setSabqiCursor(VerseRef ref) { putRef("sabqiCursor", clampInto(ref, sabqiStart(), sabqiEnd())); }
    public void setItqanCursor(VerseRef ref) { putRef("itqanCursor", clampInto(ref, itqanStart(), itqanEnd())); }
    public void setMurajaahCursor(VerseRef ref) { putRef("murajaahCursor", clampInto(ref, itqanStart(), itqanEnd())); }

    public VerseRef nextSabqi(VerseRef current) { return cycleNext(current, sabqiStart(), sabqiEnd()); }
    public VerseRef nextItqan(VerseRef current) { return cycleNext(current, itqanStart(), itqanEnd()); }
    public VerseRef nextMurajaah(VerseRef current) { return cycleNext(current, itqanStart(), itqanEnd()); }

    public int sabqiRep() { return p.getInt("sabqiRep", 0); }
    public int sabqiAssisted() { return p.getInt("sabqiAssisted", 0); }
    public void setSabqiProgress(int rep, int assisted) { p.edit().putInt("sabqiRep", rep).putInt("sabqiAssisted", assisted).apply(); }
    public int itqanRep() { return p.getInt("itqanRep", 0); }
    public int itqanAssisted() { return p.getInt("itqanAssisted", 0); }
    public void setItqanProgress(int rep, int assisted) { p.edit().putInt("itqanRep", rep).putInt("itqanAssisted", assisted).apply(); }

    public long elapsedMs(String mode) { return Math.max(0L, p.getLong(mode + "ElapsedMs", 0L)); }
    public void setElapsedMs(String mode, long ms) { p.edit().putLong(mode + "ElapsedMs", Math.max(0L, ms)).apply(); }

    public double murajaahSecondsPerLine() { return Math.max(1.0, p.getFloat("murajaahSecondsPerLine", 9f)); }
    public void setMurajaahSecondsPerLine(double value) {
        if (Double.isFinite(value) && value > 0.0) p.edit().putFloat("murajaahSecondsPerLine", (float) value).apply();
    }

    public boolean completedToday(String mode) { return LocalDate.now().toString().equals(p.getString("last" + mode + "Date", "")); }
    public void markCompletedToday(String mode, String label) {
        p.edit().putString("last" + mode + "Date", LocalDate.now().toString()).putString("last" + mode + "Label", label).apply();
    }
    public String lastLabel(String mode) { return p.getString("last" + mode + "Label", "Séance terminée"); }

    public List<RecentSabqi> recentSabqi() {
        ArrayList<RecentSabqi> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString("recentSabqi", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new RecentSabqi(o.getInt("start"), o.getInt("end")));
            }
        } catch (Exception error) { throw new IllegalStateException("Corrupt recent Sabqi queue", error); }
        return out;
    }

    public void addRecentSabqi(int startLine, int endLine) {
        List<RecentSabqi> queue = recentSabqi(); queue.add(new RecentSabqi(startLine, endLine)); saveRecent(queue);
    }
    public void removeFirstRecentSabqi() {
        List<RecentSabqi> queue = recentSabqi(); if (!queue.isEmpty()) queue.remove(0); saveRecent(queue);
    }

    public LocalDate programStartDate() {
        String value = p.getString("programStartDate", null);
        return value == null ? LocalDate.now() : LocalDate.parse(value);
    }

    private void saveRecent(List<RecentSabqi> queue) {
        JSONArray a = new JSONArray();
        try {
            for (RecentSabqi item : queue) {
                JSONObject o = new JSONObject(); o.put("start", item.startGlobalLine); o.put("end", item.endGlobalLine); a.put(o);
            }
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
        p.edit().putString("recentSabqi", a.toString()).apply();
    }

    private static void requireOrdered(VerseRef start, VerseRef end, String label) {
        QuranCanon.INSTANCE.requireValid(start); QuranCanon.INSTANCE.requireValid(end);
        if (QuranCanon.INSTANCE.ordinal(start) > QuranCanon.INSTANCE.ordinal(end)) throw new IllegalArgumentException(label + " start must precede end");
    }
    private VerseRef requiredRef(String key) {
        VerseRef ref = readRef(key); if (ref == null) throw new IllegalStateException("Missing Hifz setting: " + key); return ref;
    }
    private VerseRef readRef(String key) {
        String value = p.getString(key, null); if (value == null || value.isEmpty()) return null;
        try { String[] s = value.split(":", -1); return s.length == 2 ? new VerseRef(Integer.parseInt(s[0]), Integer.parseInt(s[1])) : null; }
        catch (RuntimeException invalid) { return null; }
    }
    private void putRef(String key, VerseRef ref) { p.edit().putString(key, ref.toString()).apply(); }
    private static VerseRef clampInto(VerseRef ref, VerseRef start, VerseRef end) {
        int o = QuranCanon.INSTANCE.ordinal(ref), lo = QuranCanon.INSTANCE.ordinal(start), hi = QuranCanon.INSTANCE.ordinal(end);
        if (o < lo) return start; if (o > hi) return end; return ref;
    }
    private static VerseRef cycleNext(VerseRef current, VerseRef start, VerseRef end) {
        if (QuranCanon.INSTANCE.ordinal(current) >= QuranCanon.INSTANCE.ordinal(end)) return start;
        VerseRef next = QuranCanon.INSTANCE.next(current); return next == null ? start : next;
    }
}
