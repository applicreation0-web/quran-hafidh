package com.quransafeguard.hifz.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/** Counts only Activity-active time. Time in background/other apps is never consumed. */
final class SessionClock {
    interface Listener { void onTick(long elapsedMs); }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private long accumulatedMs;
    private long startedAt = -1L;
    private boolean disposed;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (disposed) return;
            listener.onTick(elapsedMs());
            if (startedAt >= 0L) handler.postDelayed(this, 1000L);
        }
    };

    SessionClock(long initialMs, Listener listener) {
        accumulatedMs = Math.max(0L, initialMs);
        this.listener = listener;
    }
    void resume() {
        if (disposed || startedAt >= 0L) return;
        startedAt = SystemClock.elapsedRealtime(); handler.removeCallbacks(tick); handler.post(tick);
    }
    long pause() {
        if (startedAt >= 0L) {
            accumulatedMs += Math.max(0L, SystemClock.elapsedRealtime() - startedAt); startedAt = -1L;
        }
        handler.removeCallbacks(tick); listener.onTick(accumulatedMs); return accumulatedMs;
    }
    long elapsedMs() { return accumulatedMs + (startedAt >= 0L ? Math.max(0L, SystemClock.elapsedRealtime() - startedAt) : 0L); }
    void dispose() { pause(); disposed = true; handler.removeCallbacksAndMessages(null); }
    static String format(long millis) {
        long seconds = Math.max(0L, millis) / 1000L, h = seconds / 3600L, m = (seconds % 3600L) / 60L, s = seconds % 60L;
        return h > 0 ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h,m,s) : String.format(java.util.Locale.ROOT, "%02d:%02d", m,s);
    }
}
