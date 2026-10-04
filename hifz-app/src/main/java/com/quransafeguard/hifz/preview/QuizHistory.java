package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Small Quiz-only history, deliberately isolated from Progression, Révision and weak spots. */
final class QuizHistory {
    enum Result { CORRECT, HESITATION, REVIEW }

    private static final String STORE = "quran_hifz_quiz_v1";
    private static final String KEY = "attempts";
    private static final int MAX_ATTEMPTS = 200;
    private final SharedPreferences prefs;

    QuizHistory(Context context) {
        prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    void record(QuizQuestion question, Result result, int retryCount, boolean recordUsed) {
        if (question == null || result == null) return;
        try {
            JSONArray previous = new JSONArray(prefs.getString(KEY, "[]"));
            JSONArray next = new JSONArray();
            int keepFrom = Math.max(0, previous.length() - (MAX_ATTEMPTS - 1));
            for (int i = keepFrom; i < previous.length(); i++) next.put(previous.getJSONObject(i));
            next.put(item(question, result, retryCount, recordUsed));
            prefs.edit().putString(KEY, next.toString()).apply();
        } catch (Exception malformedHistory) {
            try {
                prefs.edit().putString(KEY, new JSONArray()
                    .put(item(question, result, retryCount, recordUsed)).toString()).apply();
            } catch (Exception ignored) {
                // History is non-critical and must never block Quran reading.
            }
        }
    }

    private static JSONObject item(QuizQuestion question, Result result, int retryCount, boolean recordUsed) throws Exception {
        return new JSONObject()
            .put("at", System.currentTimeMillis())
            .put("type", question.type.name())
            .put("prompt", question.prompt.toString())
            .put("expected", question.expected.toString())
            .put("result", result.name())
            .put("retryCount", Math.max(0, retryCount))
            .put("recordUsed", recordUsed);
    }
}
