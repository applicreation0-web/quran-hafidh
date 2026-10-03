package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Free-form stylus annotations drawn directly on the Mushaf page in Lecture (StudyReaderActivity)
 * — a margin note or underline the learner draws with a Boox pen, never verified/recognized,
 * exactly like pencil marks on a physical Mushaf. Stored per physical page (1..604), each stroke
 * as a flat list of x,y points normalized to the drawing surface's own width/height at capture
 * time (0..1), so a stroke redraws in the same relative position even if the surface is remeasured
 * between sessions (e.g. after a rotation or on a different device).
 *
 * Deliberately opens the same SharedPreferences file HifzPrefs itself uses ("quran_hifz_preview_v1",
 * duplicated here as a literal rather than exposing HifzPrefs' private NAME constant) so
 * HifzBackup's already-generic export/restore picks this up automatically, with no new key list
 * to maintain on that side.
 */
final class AnnotationStore {
    private static final String PREFS_NAME = "quran_hifz_preview_v1";
    private static final String KEY_PREFIX = "pageAnnotationsV1_";

    private final SharedPreferences prefs;

    AnnotationStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Each stroke is a flat [x0,y0,x1,y1,...] array of normalized (0..1) points, in draw order. */
    List<float[]> strokesForPage(int page) {
        List<float[]> out = new ArrayList<>();
        String raw = prefs.getString(KEY_PREFIX + page, "");
        if (raw.isEmpty()) return out;
        try {
            JSONArray strokes = new JSONArray(raw);
            for (int i = 0; i < strokes.length(); i++) {
                JSONArray points = strokes.getJSONArray(i);
                float[] flat = new float[points.length()];
                for (int j = 0; j < points.length(); j++) flat[j] = (float) points.getDouble(j);
                out.add(flat);
            }
        } catch (JSONException malformed) {
            // A corrupted value here only costs this page's own annotations, never a wrong
            // progression commit elsewhere — degrade to empty rather than throw.
            return new ArrayList<>();
        }
        return out;
    }

    /** Appends one finished stroke to the page's existing set and persists immediately. */
    boolean addStroke(int page, float[] normalizedPoints) {
        List<float[]> strokes = strokesForPage(page);
        strokes.add(normalizedPoints);
        return saveStrokes(page, strokes);
    }

    /** Removes the single most recently drawn stroke on this page, if any. */
    boolean undoLastStroke(int page) {
        List<float[]> strokes = strokesForPage(page);
        if (strokes.isEmpty()) return true;
        strokes.remove(strokes.size() - 1);
        return saveStrokes(page, strokes);
    }

    /** Erases every stroke on this page. */
    boolean clearPage(int page) {
        return prefs.edit().remove(KEY_PREFIX + page).commit();
    }

    private boolean saveStrokes(int page, List<float[]> strokes) {
        JSONArray out = new JSONArray();
        try {
            for (float[] stroke : strokes) {
                JSONArray points = new JSONArray();
                for (float v : stroke) points.put(v);
                out.put(points);
            }
        } catch (JSONException impossible) {
            // JSONArray.put(double) never throws.
            throw new AssertionError(impossible);
        }
        return prefs.edit().putString(KEY_PREFIX + page, out.toString()).commit();
    }
}
