package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

/** Persists J10 time that substitutes for a scheduled timed review slot without crediting its normal protocol. */
final class J10HostBudgetStore {
    private static final String NAME = "quran_hifz_j10_host_v1";
    private static final String DATE_PREFIX = "date:";
    private static final String CONSUMED_PREFIX = "consumed:";
    private static final String SLOT_PREFIX = "slot:";

    private final SharedPreferences prefs;

    J10HostBudgetStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    synchronized boolean addConsumed(String mode, LocalDate date, long deltaMs) {
        if (!supported(mode) || date == null || deltaMs <= 0L) return false;
        String dateKey = DATE_PREFIX + mode;
        String consumedKey = CONSUMED_PREFIX + mode;
        String storedDate = prefs.getString(dateKey, "");
        long current = date.toString().equals(storedDate) ? Math.max(0L, prefs.getLong(consumedKey, 0L)) : 0L;
        long next = J10SessionBudget.addConsumed(current, deltaMs);
        return prefs.edit()
            .putString(dateKey, date.toString())
            .putLong(consumedKey, next)
            .commit();
    }

    synchronized long consumedMs(String mode, LocalDate date) {
        if (!supported(mode) || date == null) return 0L;
        if (!date.toString().equals(prefs.getString(DATE_PREFIX + mode, ""))) return 0L;
        return Math.max(0L, prefs.getLong(CONSUMED_PREFIX + mode, 0L));
    }

    synchronized boolean markSlotConsumed(String mode, LocalDate date) {
        if (!supported(mode) || date == null) return false;
        return prefs.edit().putString(SLOT_PREFIX + mode, date.toString()).commit();
    }

    synchronized boolean isSlotConsumed(String mode, LocalDate date) {
        return supported(mode) && date != null
            && date.toString().equals(prefs.getString(SLOT_PREFIX + mode, ""));
    }

    private static boolean supported(String mode) {
        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            || HifzSessionActivity.MURAJAAH.equals(mode);
    }
}
