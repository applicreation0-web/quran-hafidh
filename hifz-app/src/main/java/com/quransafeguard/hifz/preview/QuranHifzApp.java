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
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App-level J10 guard: observes validated Hifz commits and preempts reusable review sessions only. */
public final class QuranHifzApp extends Application
        implements Application.ActivityLifecycleCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";
    private static final int LEGACY_J10_LAST_SCHEMA = 5;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private volatile J10ReviewPlanner planner;
    private volatile J10ReviewObserver observer;
    private volatile boolean pendingReconcile = true;
    private LocalDate lastFullReconcileDate;
    private final Set<Activity> suppressPriorityOnce = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<Activity> resumedActivities = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<Activity> priorityEvaluationPending = Collections.newSetFromMap(new WeakHashMap<>());
    private LocalDate lastAlertDate;
    private int lastAlertDeficit = -1;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this);
        // Geometry/J10 startup reconciliation is intentionally off the main thread.
        loader.execute(() -> {
            try { ensurePlannerSynced(HifzClock.today()); }
            catch (RuntimeException error) { Log.e("QuranHifz", "Initial J10 reconciliation failed", error); }
        });
    }

    private boolean legacyJ10RuntimeAllowed() {
        int schema = getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).getInt("schema", 0);
        return schema <= LEGACY_J10_LAST_SCHEMA;
    }

    private synchronized J10ReviewPlanner ensurePlannerObjects() {
        if (planner == null) {
            planner = new J10ReviewPlanner(this);
            observer = new J10ReviewObserver(planner);
            pendingReconcile = true;
        }
        return planner;
    }

    /** Must run on loader. Performs at most one full reconciliation per date unless state changed. */
    private J10ReviewPlanner ensurePlannerSynced(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today required");
        if (!legacyJ10RuntimeAllowed()) return null;
        J10ReviewPlanner current = ensurePlannerObjects();
        // Constructing HifzPrefs inside the legacy planner may migrate schema 5 to schema 6.
        // Once schema 6 is authoritative the retained quran_hifz_j10_v1 store is immutable backup only.
        if (!legacyJ10RuntimeAllowed()) return null;
        J10ReviewObserver currentObserver;
        boolean reconcile;
        synchronized (this) {
            currentObserver = observer;
            reconcile = pendingReconcile || !today.equals(lastFullReconcileDate);
        }
        if (reconcile && currentObserver != null) {
            currentObserver.reconcileAll(today);
            synchronized (this) {
                lastFullReconcileDate = today;
                pendingReconcile = false;
            }
        }
        return current;
    }

    void reconcilePreferenceChange(String key, LocalDate today) {
        if (key == null || today == null) return;
        if (J10ReviewObserver.requiresFullReconcileKey(key)) {
            synchronized (this) { pendingReconcile = true; }
        }
        J10ReviewPlanner current = ensurePlannerSynced(today);
        if (current == null) return;
        J10ReviewObserver currentObserver = observer;
        if (J10ReviewObserver.handlesPreferenceKey(key) && currentObserver != null) {
            currentObserver.onPreferenceChanged(key, today);
        }
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        // SharedPreferences callbacks often originate on the UI thread. Never reconcile here.
        loader.execute(() -> {
            try {
                reconcilePreferenceChange(key, HifzClock.today());
            } catch (RuntimeException error) {
                synchronized (this) { pendingReconcile = true; }
                Log.e("QuranHifz", "J10 reconciliation failed for " + key, error);
            }
        });
    }

    @Override public void onActivityResumed(Activity activity) {
        resumedActivities.add(activity);
        if (activity instanceof J10ReviewActivity) return;

        if (activity instanceof HifzSessionActivity) {
            evaluatePriorityAsync(activity);
            return;
        }

        if (activity instanceof MainActivity) {
            final LocalDate today = HifzClock.today();
            loader.execute(() -> {
                try {
                    J10ReviewPlanner current = ensurePlannerSynced(today);
                    if (current == null) return;
                    J10ReviewPolicy.Forecast forecast = current.forecast(today);
                    activity.runOnUiThread(() -> {
                        if (isHostForeground(activity)) showSustainabilityAlert(activity, forecast, today);
                    });
                } catch (RuntimeException error) {
                    Log.e("QuranHifz", "Unable to calculate J10 forecast", error);
                }
            });
        }
    }

    private void evaluatePriorityAsync(Activity activity) {
        String hostMode = activity.getIntent().getStringExtra(HifzSessionActivity.EXTRA_MODE);
        if (!isReusableJ10Host(hostMode)) return;
        LocalDate today = HifzClock.today();

        // A host slot fully substituted by J10 is closed, not credited as its normal protocol.
        if (new J10HostBudgetStore(this).isSlotConsumed(hostMode, today)) {
            suppressPriorityOnce.remove(activity);
            activity.finish();
            return;
        }
        if (suppressPriorityOnce.remove(activity)) return;
        if (!priorityEvaluationPending.add(activity)) return;

        loader.execute(() -> {
            try {
                J10ReviewPlanner current = ensurePlannerSynced(today);
                if (current == null) {
                    activity.runOnUiThread(() -> priorityEvaluationPending.remove(activity));
                    return;
                }
                J10ReviewPlanner.PriorityGroup priority = current.priorityGroup(today);
                activity.runOnUiThread(() -> {
                    priorityEvaluationPending.remove(activity);
                    if (!isHostForeground(activity) || priority == null || priority.isEmpty()) return;
                    suppressPriorityOnce.add(activity);
                    Intent intent = new Intent(activity, J10ReviewActivity.class)
                        .putExtra(J10ReviewActivity.EXTRA_HOST_MODE, hostMode);
                    activity.startActivity(intent);
                });
            } catch (RuntimeException error) {
                activity.runOnUiThread(() -> priorityEvaluationPending.remove(activity));
                Log.e("QuranHifz", "Unable to evaluate J10 priority", error);
            }
        });
    }

    private boolean isHostForeground(Activity activity) {
        return activity != null && resumedActivities.contains(activity)
            && !activity.isFinishing() && !activity.isDestroyed();
    }

    static boolean isReusableJ10Host(String mode) {
        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.ITQAN.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            || HifzSessionActivity.MURAJAAH.equals(mode);
    }

    private synchronized void showSustainabilityAlert(Activity activity, J10ReviewPolicy.Forecast forecast,
                                                       LocalDate today) {
        if (forecast == null || forecast.sustainability != J10ReviewPolicy.Sustainability.NON_TENABLE) return;
        if (today.equals(lastAlertDate) && forecast.deficitMinutes == lastAlertDeficit) return;
        lastAlertDate = today;
        lastAlertDeficit = forecast.deficitMinutes;
        Toast.makeText(activity,
            "J10 · rythme non tenable · déficit prévu " + forecast.deficitMinutes + " min sur 10 jours",
            Toast.LENGTH_LONG).show();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {
        resumedActivities.remove(activity);
    }
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {
        resumedActivities.remove(activity);
        suppressPriorityOnce.remove(activity);
        priorityEvaluationPending.remove(activity);
    }
}
