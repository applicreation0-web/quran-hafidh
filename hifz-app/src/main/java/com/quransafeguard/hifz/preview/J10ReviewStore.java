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

/** Independent v1 persistence for the single acquired-line J10 list. */
final class J10ReviewStore {
    static final String NAME = "quran_hifz_j10_v1";
    static final Object LEGACY_STORE_LOCK = new Object();
    private static final String MAIN_PREFS_NAME = "quran_hifz_preview_v1";
    private static final int LEGACY_J10_LAST_SCHEMA = 5;
    private static final String LINE_PREFIX = "line:";
    private final SharedPreferences prefs;
    private final SharedPreferences mainPrefs;

    J10ReviewStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        mainPrefs = context.getSharedPreferences(MAIN_PREFS_NAME, Context.MODE_PRIVATE);
    }

    Map<String, LocalDate> snapshot() {
        synchronized (LEGACY_STORE_LOCK) {
            LinkedHashMap<String, LocalDate> out = new LinkedHashMap<>();
            Map<String, ?> all = prefs.getAll();
            List<String> keys = new ArrayList<>(all.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                if (!key.startsWith(LINE_PREFIX)) continue;
                Object raw = all.get(key);
                if (!(raw instanceof Long)) continue;
                try {
                    out.put(key.substring(LINE_PREFIX.length()), LocalDate.ofEpochDay((Long) raw));
                } catch (RuntimeException ignored) {
                    // A corrupt isolated entry is ignored; migration classifies only reliable dates.
                }
            }
            return Collections.unmodifiableMap(out);
        }
    }

    /** Add newly acquired lines without ever aging or removing existing acquired material. */
    boolean acquireLines(Collection<String> lineIds, LocalDate acquiredOn) {
        if (acquiredOn == null) throw new IllegalArgumentException("acquiredOn required");
        synchronized (LEGACY_STORE_LOCK) {
            if (!legacyWritesAllowedLocked()) return true;
            LinkedHashSet<String> wanted = sanitize(lineIds);
            Map<String, ?> all = prefs.getAll();
            SharedPreferences.Editor editor = prefs.edit();
            boolean changed = false;
            long seed = acquiredOn.toEpochDay();
            for (String id : wanted) {
                String key = key(id);
                if (!(all.get(key) instanceof Long)) {
                    editor.putLong(key, seed);
                    changed = true;
                }
            }
            return !changed || editor.commit();
        }
    }

    /**
     * Test/migration helper that mirrors an exact acquired set. Runtime planning uses acquireLines()
     * so a pedagogical status change can never silently remove already acquired Quran material.
     */
    boolean syncAcquired(Collection<String> lineIds, LocalDate seedDate) {
        if (seedDate == null) throw new IllegalArgumentException("seedDate required");
        synchronized (LEGACY_STORE_LOCK) {
            if (!legacyWritesAllowedLocked()) return true;
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
                if (!(all.get(key) instanceof Long)) {
                    editor.putLong(key, seedDate.toEpochDay());
                    changed = true;
                }
            }
            return !changed || editor.commit();
        }
    }

    /** Opening/displaying never counts; only an explicit validated recitation calls this method. */
    boolean markReviewed(Collection<String> lineIds, LocalDate reviewedOn) {
        if (reviewedOn == null) throw new IllegalArgumentException("reviewedOn required");
        synchronized (LEGACY_STORE_LOCK) {
            if (!legacyWritesAllowedLocked()) return true;
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
    }

    private boolean legacyWritesAllowedLocked() {
        return mainPrefs.getInt("schema", 0) <= LEGACY_J10_LAST_SCHEMA;
    }

    private static String key(String lineId) { return LINE_PREFIX + lineId; }

    private static LinkedHashSet<String> sanitize(Collection<String> lineIds) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (lineIds == null) return out;
        for (String id : lineIds) if (id != null && !id.trim().isEmpty()) out.add(id);
        return out;
    }
}
