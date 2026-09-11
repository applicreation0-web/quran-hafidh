package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.HifzState;
import com.quransafeguard.hifz.core.VerseRef;

/** Small versioned persistent state. It stores state only; it does not decide Hifz policy. */
public final class HifzStateStore {
    private static final String NAME = "quran_hifz_state";
    private static final int SCHEMA = 1;

    private final SharedPreferences prefs;

    public HifzStateStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        int existing = prefs.getInt("schema", SCHEMA);
        if (existing != SCHEMA) {
            throw new IllegalStateException("Unsupported Quran Hifz state schema: " + existing);
        }
        if (!prefs.contains("schema")) {
            prefs.edit().putInt("schema", SCHEMA).apply();
        }
    }

    public HifzState loadOrNull() {
        String lower = prefs.getString("lowerEligibleBound", null);
        String frontier = prefs.getString("promotedFrontier", null);
        String tail = prefs.getString("upperTailStart", null);
        String itqan = prefs.getString("itqanCursor", null);
        String murajaah = prefs.getString("murajaahItqanCursor", null);
        if (lower == null || frontier == null || tail == null || itqan == null || murajaah == null) {
            return null;
        }
        return new HifzState(parse(lower), parse(frontier), parse(tail), parse(itqan), parse(murajaah));
    }

    public void save(HifzState state) {
        prefs.edit()
            .putString("lowerEligibleBound", state.getLowerEligibleBound().toString())
            .putString("promotedFrontier", state.getPromotedFrontier().toString())
            .putString("upperTailStart", state.getUpperTailStart().toString())
            .putString("itqanCursor", state.getItqanCursor().toString())
            .putString("murajaahItqanCursor", state.getMurajaahItqanCursor().toString())
            .apply();
    }

    public void clear() {
        prefs.edit().clear().putInt("schema", SCHEMA).apply();
    }

    private static VerseRef parse(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 2) throw new IllegalStateException("Invalid verse reference in Hifz state");
        return new VerseRef(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
