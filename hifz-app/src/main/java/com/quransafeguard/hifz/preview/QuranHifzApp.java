package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App-level J10 guard: observes validated Hifz commits and preempts structured sessions only when needed. */
public final class QuranHifzApp extends Application
        implements Application.ActivityLifecycleCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private volatile J10ReviewPlanner planner;
    private volatile J10ReviewObserver observer;
    private volatile boolean openingPriority;
    private volatile boolean pendingReconcile;
    private LocalDate lastAlertDate;
    private int lastAlertDeficit = -1;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this);
    }

    private synchronized J10ReviewPlanner ensurePlanner() {
        if (planner == null) {
            planner = new J10ReviewPlanner(this);
            observer = new J10ReviewObserver(planner);
            observer.reconcileAll(LocalDate.now());
            pendingReconcile = false;
        } else if (pendingReconcile && observer != null) {
            observer.reconcileAll(LocalDate.now());
            pendingReconcile = false;
        }
        return planner;
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        J10ReviewObserver current = observer;
        if (current == null) {
            pendingReconcile = true;
            return;
        }
        try {
            current.onPreferenceChanged(key, LocalDate.now());
        } catch (RuntimeException error) {
            Log.e("QuranHifz", "J10 reconciliation failed for " + key, error);
        }
    }

    @Override public void onActivityResumed(Activity activity) {
        if (activity instanceof J10ReviewActivity) return;

        if (activity instanceof HifzSessionActivity) {
            try {
                J10ReviewPlanner p = ensurePlanner();
                J10ReviewPlanner.PriorityGroup priority = p.priorityGroup(LocalDate.now());
                if (!priority.isEmpty() && !openingPriority) {
                    openingPriority = true;
                    String hostMode = activity.getIntent().getStringExtra(HifzSessionActivity.EXTRA_MODE);
                    Intent intent = new Intent(activity, J10ReviewActivity.class)
                        .putExtra(J10ReviewActivity.EXTRA_HOST_MODE, hostMode);
                    activity.startActivity(intent);
                    activity.getWindow().getDecorView().postDelayed(() -> openingPriority = false, 500L);
                }
            } catch (RuntimeException error) {
                openingPriority = false;
                Log.e("QuranHifz", "Unable to evaluate J10 priority", error);
            }
            return;
        }

        if (activity instanceof MainActivity) {
            loader.execute(() -> {
                try {
                    J10ReviewPolicy.Forecast forecast = ensurePlanner().forecast(LocalDate.now());
                    activity.runOnUiThread(() -> showSustainabilityAlert(activity, forecast));
                } catch (RuntimeException error) {
                    Log.e("QuranHifz", "Unable to calculate J10 forecast", error);
                }
            });
        }
    }

    private synchronized void showSustainabilityAlert(Activity activity, J10ReviewPolicy.Forecast forecast) {
        if (forecast == null || forecast.sustainability != J10ReviewPolicy.Sustainability.NON_TENABLE) return;
        LocalDate today = LocalDate.now();
        if (today.equals(lastAlertDate) && forecast.deficitMinutes == lastAlertDeficit) return;
        lastAlertDate = today;
        lastAlertDeficit = forecast.deficitMinutes;
        Toast.makeText(activity,
            "J10 · rythme non tenable · déficit prévu " + forecast.deficitMinutes + " min sur 10 jours",
            Toast.LENGTH_LONG).show();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
