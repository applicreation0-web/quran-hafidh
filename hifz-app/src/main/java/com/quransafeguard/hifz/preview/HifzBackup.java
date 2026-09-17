package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Whole-progress export/import. Everything Hifz tracks (schema-6 state, ranges, speed
 * calibration, cursors) lives in one on-device SharedPreferences file with no account or
 * server behind it, so uninstalling the app or losing the device destroys it silently unless
 * the user has a copy of this file saved somewhere of their own choosing (Drive, email, …).
 * Each value keeps an explicit type tag so the round-trip through JSON numbers never confuses
 * a Float speed calibration with an Int or Long cursor.
 */
final class HifzBackup {
    private static final String PREFS_NAME = "quran_hifz_preview_v1";
    private static final String MAGIC = "quran-hifz-backup";
    private static final int VERSION = 1;

    private HifzBackup() {}

    static JSONObject export(Context context) throws JSONException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            values.put(entry.getKey(), encode(entry.getValue()));
        }
        JSONObject root = new JSONObject();
        root.put("magic", MAGIC);
        root.put("version", VERSION);
        root.put("exportedAtEpochMs", System.currentTimeMillis());
        root.put("values", values);
        return root;
    }

    /** Returns the number of restored preference entries. */
    static int restore(Context context, JSONObject root) throws JSONException {
        if (!MAGIC.equals(root.optString("magic"))) {
            throw new JSONException("Fichier de sauvegarde non reconnu.");
        }
        JSONObject values = root.getJSONObject("values");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        Iterator<String> keys = values.keys();
        int count = 0;
        while (keys.hasNext()) {
            String key = keys.next();
            apply(editor, key, values.getJSONObject(key));
            count++;
        }
        if (!editor.commit()) throw new JSONException("Échec de l'écriture des préférences.");
        return count;
    }

    private static JSONObject encode(Object value) throws JSONException {
        JSONObject entry = new JSONObject();
        if (value instanceof Boolean) {
            entry.put("t", "b").put("v", value);
        } else if (value instanceof Integer) {
            entry.put("t", "i").put("v", value);
        } else if (value instanceof Long) {
            entry.put("t", "l").put("v", value);
        } else if (value instanceof Float) {
            entry.put("t", "f").put("v", (double) (Float) value);
        } else if (value instanceof String) {
            entry.put("t", "s").put("v", value);
        } else if (value instanceof Set) {
            JSONArray array = new JSONArray();
            for (Object item : (Set<?>) value) array.put(item.toString());
            entry.put("t", "set").put("v", array);
        } else {
            throw new JSONException("Type de préférence non pris en charge : " + value.getClass());
        }
        return entry;
    }

    private static void apply(SharedPreferences.Editor editor, String key, JSONObject entry) throws JSONException {
        String type = entry.getString("t");
        switch (type) {
            case "b": editor.putBoolean(key, entry.getBoolean("v")); break;
            case "i": editor.putInt(key, entry.getInt("v")); break;
            case "l": editor.putLong(key, entry.getLong("v")); break;
            case "f": editor.putFloat(key, (float) entry.getDouble("v")); break;
            case "s": editor.putString(key, entry.getString("v")); break;
            case "set": {
                JSONArray array = entry.getJSONArray("v");
                LinkedHashSet<String> set = new LinkedHashSet<>();
                for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                editor.putStringSet(key, set);
                break;
            }
            default: throw new JSONException("Type de préférence inconnu : " + type);
        }
    }
}
