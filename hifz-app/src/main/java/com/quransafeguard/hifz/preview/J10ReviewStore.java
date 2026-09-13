package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Independent v1 persistence for per-physical-line J10 review dates. */
final class J10ReviewStore {
    static final String NAME = "quran_hifz_j10_v1";
    private static final String KEY_STATE = "lastReviewedByLine";
    private final SharedPreferences prefs;

    J10ReviewStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    synchronized Map<String, LocalDate> snapshot() {
        return Collections.unmodifiableMap(readState());
    }

    synchronized boolean syncAcquired(Collection<String> lineIds, LocalDate seedDate) {
        if (seedDate == null) throw new IllegalArgumentException("seedDate required");
        LinkedHashSet<String> wanted = sanitize(lineIds);
        LinkedHashMap<String, LocalDate> old = readState();
        LinkedHashMap<String, LocalDate> next = new LinkedHashMap<>();
        List<String> ordered = new ArrayList<>(wanted);
        Collections.sort(ordered);
        for (String id : ordered) {
            LocalDate existing = old.get(id);
            next.put(id, existing == null ? seedDate : existing);
        }
        return writeState(next);
    }

    synchronized boolean markReviewed(Collection<String> lineIds, LocalDate reviewedOn) {
        if (reviewedOn == null) throw new IllegalArgumentException("reviewedOn required");
        LinkedHashSet<String> reviewed = sanitize(lineIds);
        LinkedHashMap<String, LocalDate> state = readState();
        boolean changed = false;
        for (String id : reviewed) {
            if (state.containsKey(id) && !reviewedOn.equals(state.get(id))) {
                state.put(id, reviewedOn);
                changed = true;
            }
        }
        return !changed || writeState(state);
    }

    private LinkedHashMap<String, LocalDate> readState() {
        LinkedHashMap<String, LocalDate> out = new LinkedHashMap<>();
        String raw = prefs.getString(KEY_STATE, "{}");
        try {
            JSONObject json = new JSONObject(raw == null ? "{}" : raw);
            ArrayList<String> keys = new ArrayList<>();
            java.util.Iterator<String> it = json.keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys);
            for (String key : keys) {
                if (key == null || key.trim().isEmpty()) continue;
                try {
                    out.put(key, LocalDate.parse(json.getString(key)));
                } catch (RuntimeException ignored) {
                    // A corrupt optional entry must not make the Hifz programme unusable.
                }
            }
        } catch (Exception ignored) {
            // Treat a corrupt isolated J10 payload as empty; the next sync safely re-seeds acquired lines.
        }
        return out;
    }

    private boolean writeState(Map<String, LocalDate> state) {
        JSONObject json = new JSONObject();
        try {
            ArrayList<String> keys = new ArrayList<>(state.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                LocalDate date = state.get(key);
                if (key != null && !key.trim().isEmpty() && date != null) json.put(key, date.toString());
            }
        } catch (Exception error) {
            throw new IllegalStateException("Unable to encode J10 state", error);
        }
        return prefs.edit().putString(KEY_STATE, json.toString()).commit();
    }

    private static LinkedHashSet<String> sanitize(Collection<String> lineIds) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (lineIds == null) return out;
        for (String id : lineIds) if (id != null && !id.trim().isEmpty()) out.add(id);
        return out;
    }
}
