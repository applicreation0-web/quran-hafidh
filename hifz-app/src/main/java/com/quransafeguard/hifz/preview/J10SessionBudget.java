package com.quransafeguard.hifz.preview;

/** Overflow-safe accounting for time consumed by the J10 prelude inside an existing session. */
final class J10SessionBudget {
    private J10SessionBudget() {}

    static long addConsumed(long existingMs, long consumedMs) {
        long base = Math.max(0L, existingMs);
        long extra = Math.max(0L, consumedMs);
        if (Long.MAX_VALUE - base < extra) return Long.MAX_VALUE;
        return base + extra;
    }
}
