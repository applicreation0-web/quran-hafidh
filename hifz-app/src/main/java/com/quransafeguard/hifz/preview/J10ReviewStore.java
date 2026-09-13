package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

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
    private static final String LINE_PREFIX = "line:";
    private final SharedPreferences prefs;

    J10ReviewStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    synchronized Map<String, LocalDate> snapshot() {
        LinkedHashMap<String, LocalDate> out = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(prefs.getAll().keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (!key.startsWith(LINE_PREFIX)) continue;
            Object raw = prefs.getAll().get(key);
            if (!(raw instanceof Long)) continue;
            try {
                out.put(key.substring(LINE_PREFIX.length()), LocalDate.ofEpochDay((Long) raw));
            } catch (RuntimeException ignored) {
                // Ignore a corrupt isolated entry; next acquired sync safely repairs it.
            }
        }
        return Collections.unmodifiableMap(out);
    }

    synchronized boolean syncAcquired(Collection<String> lineIds, LocalDate seedDate) {
        if (seedDate == null) throw new IllegalArgumentException("seedDate required");
        LinkedHashSet<String> wanted = sanitize(lineIds);
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;

        for (String key : all.keySet()) {
            if (!key.startsWith(LINE_PREFIX)) continue;
            String id = key.substring(LINE_PREFIX.length());
            if (!wanted.contains(id)) {
                editor.remove(key);
                changed = true;
            }
        }
        for (String id : wanted) {
            String key = key(id);
            Object existing = all.get(key);
            if (!(existing instanceof Long)) {
                editor.putLong(key, seedDate.toEpochDay());
                changed = true;
            }
        }
        return !changed || editor.commit();
    }

    synchronized boolean markReviewed(Collection<String> lineIds, LocalDate reviewedOn) {
        if (reviewedOn == null) throw new IllegalArgumentException("reviewedOn required");
        LinkedHashSet<String> reviewed = sanitize(lineIds);
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        long requested = reviewedOn.toEpochDay();
        for (String id : reviewed) {
            String key = key(id);
            Object raw = all.get(key);
            if (!(raw instanceof Long)) continue;
            long existing = (Long) raw;
            if (requested > existing) {
                editor.putLong(key, requested);
                changed = true;
            }
        }
        return !changed || editor.commit();
    }

    private static String key(String lineId) { return LINE_PREFIX + lineId; }

    private static LinkedHashSet<String> sanitize(Collection<String> lineIds) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (lineIds == null) return out;
        for (String id : lineIds) if (id != null && !id.trim().isEmpty()) out.add(id);
        return out;
    }
}
