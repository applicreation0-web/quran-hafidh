package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Schema-6 J10 runtime state. The retained quran_hifz_j10_v1 file is migration input only and
 * is deliberately never opened here.
 */
final class J10V6Store {
    private static final String MAIN = "quran_hifz_preview_v1";
    private static final String ACQUIRED = "v6AcquiredCreditLineIds";
    private static final String ACTIVE = "v6ActiveJ10LastReviewed";
    private static final String UNKNOWN = "v6UnknownDueLineIds";
    private static final String LEGACY_IMPORTED = "v6LegacyImportedLineIds";
    private static final Object LOCK = new Object();

    private final SharedPreferences prefs;

    J10V6Store(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        prefs = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
    }

    Map<String, LocalDate> snapshot() {
        synchronized (LOCK) {
            requireSchema6Locked();
            LinkedHashMap<String, LocalDate> out = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : readEpochDayMapLocked(ACTIVE).entrySet()) {
                try {
                    out.put(entry.getKey(), LocalDate.ofEpochDay(entry.getValue()));
                } catch (RuntimeException error) {
                    throw new IllegalStateException("Corrupt schema6 J10 date for " + entry.getKey(), error);
                }
            }
            return Collections.unmodifiableMap(out);
        }
    }

    Set<String> unknownDueLineIds() {
        synchronized (LOCK) {
            requireSchema6Locked();
            return Collections.unmodifiableSet(new LinkedHashSet<>(readSetLocked(UNKNOWN)));
        }
    }

    /**
     * Schema6 acquisition state is authoritative. Reconciliation must never synthesize dates for
     * UNKNOWN_DUE or re-import the retained legacy archive.
     */
    boolean syncAcquired() {
        synchronized (LOCK) {
            requireSchema6Locked();
            LinkedHashSet<String> acquired = readSetLocked(ACQUIRED);
            LinkedHashSet<String> unknown = readSetLocked(UNKNOWN);
            LinkedHashMap<String, Long> active = readEpochDayMapLocked(ACTIVE);
            if (!acquired.containsAll(unknown) || !acquired.containsAll(active.keySet())) {
                throw new IllegalStateException("Invalid schema6 J10 state outside Acquis");
            }
            return true;
        }
    }

    /** Adds a date only for already-Acquis material that has no explicit date/UNKNOWN_DUE state. */
    boolean acquireLines(Collection<String> lineIds, LocalDate acquiredOn) {
        if (acquiredOn == null) throw new IllegalArgumentException("acquiredOn required");
        synchronized (LOCK) {
            requireSchema6Locked();
            LinkedHashSet<String> acquired = readSetLocked(ACQUIRED);
            LinkedHashSet<String> unknown = readSetLocked(UNKNOWN);
            LinkedHashMap<String, Long> active = readEpochDayMapLocked(ACTIVE);
            boolean changed = false;
            for (String id : sanitize(lineIds)) {
                if (!acquired.contains(id) || unknown.contains(id) || active.containsKey(id)) continue;
                active.put(id, acquiredOn.toEpochDay());
                changed = true;
            }
            return !changed || persistJ10Locked(active, unknown, readSetLocked(LEGACY_IMPORTED));
        }
    }

    /** Opening/displaying never counts. Only validated review may call this method. */
    boolean markReviewed(Collection<String> lineIds, LocalDate reviewedOn) {
        if (reviewedOn == null) throw new IllegalArgumentException("reviewedOn required");
        synchronized (LOCK) {
            requireSchema6Locked();
            LinkedHashSet<String> acquired = readSetLocked(ACQUIRED);
            LinkedHashSet<String> unknown = readSetLocked(UNKNOWN);
            LinkedHashSet<String> imported = readSetLocked(LEGACY_IMPORTED);
            LinkedHashMap<String, Long> active = readEpochDayMapLocked(ACTIVE);
            boolean changed = false;
            long exact = reviewedOn.toEpochDay();
            for (String id : sanitize(lineIds)) {
                if (!acquired.contains(id)) continue;
                Long previous = active.put(id, exact);
                if (previous == null || previous.longValue() != exact) changed = true;
                if (unknown.remove(id)) changed = true;
                if (imported.remove(id)) changed = true;
            }
            return !changed || persistJ10Locked(active, unknown, imported);
        }
    }

    private boolean persistJ10Locked(Map<String, Long> active,
                                     Set<String> unknown,
                                     Set<String> imported) {
        SharedPreferences.Editor editor = prefs.edit()
            .putString(ACTIVE, epochMapJson(active))
            .putString(UNKNOWN, stringSetJson(unknown))
            .putString(LEGACY_IMPORTED, stringSetJson(imported));
        if (!editor.commit()) return false;
        requireSchema6Locked();
        return true;
    }

    private void requireSchema6Locked() {
        if (prefs.getInt("schema", -1) != 6) {
            throw new IllegalStateException("Schema6 J10 runtime requires schema 6");
        }
        if (!prefs.contains(ACQUIRED) || !prefs.contains(ACTIVE)
                || !prefs.contains(UNKNOWN) || !prefs.contains(LEGACY_IMPORTED)) {
            throw new IllegalStateException("Incomplete schema6 J10 runtime state");
        }
    }

    private LinkedHashSet<String> readSetLocked(String key) {
        try {
            JSONArray array = new JSONArray(prefs.getString(key, "[]"));
            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (int i = 0; i < array.length(); i++) {
                String value = array.getString(i);
                if (value != null && !value.trim().isEmpty()) out.add(value);
            }
            return out;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt schema6 J10 set: " + key, error);
        }
    }

    private LinkedHashMap<String, Long> readEpochDayMapLocked(String key) {
        try {
            JSONObject object = new JSONObject(prefs.getString(key, "{}"));
            LinkedHashMap<String, Long> out = new LinkedHashMap<>();
            java.util.ArrayList<String> keys = new java.util.ArrayList<>();
            java.util.Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) keys.add(iterator.next());
            java.util.Collections.sort(keys);
            for (String id : keys) out.put(id, object.getLong(id));
            return out;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt schema6 J10 map: " + key, error);
        }
    }

    private static LinkedHashSet<String> sanitize(Collection<String> values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values == null) return out;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) out.add(value);
        }
        return out;
    }

    private static String stringSetJson(Collection<String> values) {
        JSONArray array = new JSONArray();
        if (values != null) for (String value : values) array.put(value);
        return array.toString();
    }

    private static String epochMapJson(Map<String, Long> values) {
        JSONObject object = new JSONObject();
        if (values != null) {
            try {
                for (Map.Entry<String, Long> entry : values.entrySet()) {
                    object.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception error) {
                throw new IllegalStateException("Unable to encode schema6 J10 state", error);
            }
        }
        return object.toString();
    }
}
