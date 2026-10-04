package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JVM-only SharedPreferences: commit() applies atomically to a backing map that outlives any
 * HifzPrefs wrapping it, so wrapping the same map again is a faithful "process restart".
 */
final class InMemoryPrefs implements SharedPreferences {
    final Map<String, Object> disk = new LinkedHashMap<>();

    @Override public Map<String, ?> getAll() { return new HashMap<>(disk); }
    @Override public String getString(String key, String defValue) { Object v = disk.get(key); return v instanceof String ? (String) v : defValue; }
    @SuppressWarnings("unchecked")
    @Override public Set<String> getStringSet(String key, Set<String> defValues) { Object v = disk.get(key); return v instanceof Set ? (Set<String>) v : defValues; }
    @Override public int getInt(String key, int defValue) { Object v = disk.get(key); return v instanceof Integer ? (Integer) v : defValue; }
    @Override public long getLong(String key, long defValue) { Object v = disk.get(key); return v instanceof Long ? (Long) v : defValue; }
    @Override public float getFloat(String key, float defValue) { Object v = disk.get(key); return v instanceof Float ? (Float) v : defValue; }
    @Override public boolean getBoolean(String key, boolean defValue) { Object v = disk.get(key); return v instanceof Boolean ? (Boolean) v : defValue; }
    @Override public boolean contains(String key) { return disk.containsKey(key); }
    @Override public Editor edit() { return new InMemoryEditor(); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    private final class InMemoryEditor implements Editor {
        private final Map<String, Object> pending = new LinkedHashMap<>();
        private boolean clear;

        @Override public Editor putString(String key, String value) { pending.put(key, value); return this; }
        @Override public Editor putStringSet(String key, Set<String> values) { pending.put(key, values); return this; }
        @Override public Editor putInt(String key, int value) { pending.put(key, value); return this; }
        @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
        @Override public Editor putFloat(String key, float value) { pending.put(key, value); return this; }
        @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
        @Override public Editor remove(String key) { pending.put(key, this); return this; }
        @Override public Editor clear() { clear = true; return this; }
        @Override public boolean commit() {
            if (clear) disk.clear();
            for (Map.Entry<String, Object> e : pending.entrySet()) {
                if (e.getValue() == this || e.getValue() == null) disk.remove(e.getKey());
                else disk.put(e.getKey(), e.getValue());
            }
            return true;
        }
        @Override public void apply() { commit(); }
    }
}
