package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small per-current-session timing store used for Itqan average/median and Sabqi speed monitoring. */
public final class RepetitionTimingStore {
    private final SharedPreferences p;
    public RepetitionTimingStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences("quran_hifz_rep_timing_v1", Context.MODE_PRIVATE);
    }

    public void reset(String mode) { p.edit().remove(mode + ":durations").apply(); }

    public void add(String mode, long activeMillis) {
        if (activeMillis < 0L) return;
        List<Long> values = values(mode); values.add(activeMillis);
        JSONArray a = new JSONArray(); for (Long v : values) a.put(v);
        p.edit().putString(mode + ":durations", a.toString()).apply();
    }

    public List<Long> values(String mode) {
        ArrayList<Long> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString(mode + ":durations", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Math.max(0L, a.getLong(i)));
        } catch (Exception error) { throw new IllegalStateException("Corrupt repetition timing", error); }
        return out;
    }

    public long average(String mode) {
        List<Long> v = values(mode); if (v.isEmpty()) return 0L;
        long sum = 0L; for (Long n : v) sum += n; return sum / v.size();
    }

    public long median(String mode) {
        List<Long> v = values(mode); if (v.isEmpty()) return 0L;
        Collections.sort(v); int n = v.size();
        return (n & 1) == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2L;
    }
}
