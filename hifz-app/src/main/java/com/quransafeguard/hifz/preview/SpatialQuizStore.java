package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Persists only the spatial quiz's own adaptive history — presentations/exact/almost/review
 * counts, last verdict and response time per question id. No Hifz status of any kind lives here.
 *
 * Deliberately opens the same SharedPreferences file HifzPrefs itself uses ("quran_hifz_preview_v1",
 * duplicated here as a literal rather than exposing HifzPrefs' private NAME constant) so
 * HifzBackup's already-generic export/restore picks this up automatically, with no new key list
 * to maintain on that side.
 */
final class SpatialQuizStore {
    private static final String PREFS_NAME = "quran_hifz_preview_v1";
    private static final String KEY = "spatialQuizStateV1";

    private final SharedPreferences prefs;

    SpatialQuizStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    Map<String, SpatialQuizEngine.Stats> loadAll() {
        Map<String, SpatialQuizEngine.Stats> out = new HashMap<>();
        String raw = prefs.getString(KEY, "");
        if (raw.isEmpty()) return out;
        try {
            JSONObject root = new JSONObject(raw);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String questionId = keys.next();
                JSONObject entry = root.getJSONObject(questionId);
                SpatialQuizEngine.Stats stats = new SpatialQuizEngine.Stats();
                stats.presentations = entry.optInt("presentations", 0);
                stats.exact = entry.optInt("exact", 0);
                stats.almost = entry.optInt("almost", 0);
                stats.review = entry.optInt("review", 0);
                stats.lastResponseMs = entry.optLong("lastResponseMs", 0L);
                stats.lastShownEpochDay = entry.optLong("lastShownEpochDay", 0L);
                String verdict = entry.optString("lastVerdict", "");
                if (!verdict.isEmpty()) {
                    try {
                        stats.lastVerdict = SpatialQuizEngine.Verdict.valueOf(verdict);
                    } catch (IllegalArgumentException malformed) {
                        stats.lastVerdict = null;
                    }
                }
                out.put(questionId, stats);
            }
        } catch (JSONException malformed) {
            // A corrupted/foreign value here only degrades adaptation (every question falls back
            // to its default weight) — never worth failing the quiz screen over.
            return new HashMap<>();
        }
        return out;
    }

    /** Merges one answered question's outcome into the persisted history and commits it. */
    void recordAnswer(String questionId, SpatialQuizEngine.Answer answer) {
        Map<String, SpatialQuizEngine.Stats> all = loadAll();
        SpatialQuizEngine.Stats stats = all.get(questionId);
        if (stats == null) {
            stats = new SpatialQuizEngine.Stats();
            all.put(questionId, stats);
        }
        stats.presentations++;
        switch (answer.verdict) {
            case EXACT: stats.exact++; break;
            case ALMOST: stats.almost++; break;
            case REVIEW: stats.review++; break;
        }
        stats.lastVerdict = answer.verdict;
        stats.lastResponseMs = answer.responseMs;
        stats.lastShownEpochDay = HifzClock.today().toEpochDay();
        saveAll(all);
    }

    private void saveAll(Map<String, SpatialQuizEngine.Stats> all) {
        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<String, SpatialQuizEngine.Stats> e : all.entrySet()) {
                SpatialQuizEngine.Stats stats = e.getValue();
                JSONObject entry = new JSONObject();
                entry.put("presentations", stats.presentations);
                entry.put("exact", stats.exact);
                entry.put("almost", stats.almost);
                entry.put("review", stats.review);
                entry.put("lastResponseMs", stats.lastResponseMs);
                entry.put("lastShownEpochDay", stats.lastShownEpochDay);
                if (stats.lastVerdict != null) entry.put("lastVerdict", stats.lastVerdict.name());
                root.put(e.getKey(), entry);
            }
        } catch (JSONException impossible) {
            // JSONObject.put(String, int/long/String) never throws for these value types.
            throw new AssertionError(impossible);
        }
        prefs.edit().putString(KEY, root.toString()).apply();
    }
}
