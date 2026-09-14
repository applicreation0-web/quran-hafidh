package com.quransafeguard.hifz.preview;

import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;

/** Small J10 prelude fed by the single acquired-line list; it is not a new weekly session type. */
public final class J10ReviewActivity extends android.app.Activity implements MushafView.Listener {
    public static final String EXTRA_HOST_MODE = "j10HostMode";
    public static final String EXTRA_RESULT_REASON = "j10ResultReason";
    public static final String RESULT_EMPTY = "empty";

    private J10ReviewPlanner planner;
    private J10ReviewPlanner.PriorityGroup group;
    private J10ReviewProgress reviewProgress;
    private HifzPrefs prefs;
    private J10HostBudgetStore hostBudgetStore;
    private MushafView mushaf;
    private TextView title;
    private TextView status;
    private LinearLayout actions;
    private int currentPage = 1;
    private boolean shown;
    private String hostMode;
    private long activeStartedAt = -1L;
    private final Handler hostBudgetHandler = new Handler(Looper.getMainLooper());
    private final Runnable hostBudgetWatchdog = new Runnable() {
        @Override public void run() {
            if (hostMode == null || activeStartedAt < 0L || isFinishing()) return;
            if (PreviewConfig.timedSessionComplete(hostElapsedIncludingActive(), hostTargetMinutes())) {
                LocalDate today = HifzClock.today();
                if (persistActiveHostTime(false)) {
                    if (!hostBudgetStore.markSlotConsumed(hostMode, today)) {
                        Log.e("QuranHifz", "Unable to persist J10-preempted host slot for " + hostMode);
                    } else {
                        // A fully substituted slot is not a completed Reprise/Consolidation/Entretien.
                        // Clear the normal-session elapsed trigger before returning to the host activity.
                        prefs.setElapsedFor(hostMode, 0L);
                    }
                    finish();
                    return;
                }
            }
            hostBudgetHandler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        planner = new J10ReviewPlanner(this);
        prefs = new HifzPrefs(this);
        hostBudgetStore = new J10HostBudgetStore(this);
        hostMode = safeHostMode(getIntent().getStringExtra(EXTRA_HOST_MODE));
        buildUi();
        renderPriority();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 6), 0);
        top.addView(Ui.iconButton(this, "", "Plus tard", v -> moveTaskToBack(true)));
        title = Ui.text(this, "Priorité J10", 12.5f, true);
        Ui.weight(title, 1f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title);
        root.addView(top);

        status = Ui.text(this, "", 10.8f, false);
        status.setTextColor(Ui.MUTED);
        status.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 2));
        root.addView(status);

        mushaf = new MushafView(this);
        mushaf.setListener(this);
        root.addView(mushaf, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        actions = Ui.row(this);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 2));
        root.addView(actions);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void renderPriority() {
        actions.removeAllViews();
        shown = false;
        group = planner.priorityGroup(HifzClock.today());
        if (group == null || group.isEmpty()) {
            setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_RESULT_REASON, RESULT_EMPTY));
            finish();
            return;
        }
        reviewProgress = new J10ReviewProgress(group.firstPage, group.lastPage);
        currentPage = group.firstPage;
        title.setText("Priorité J10 · J" + group.maxAgeDays + " · " + group.lineIds.size() + " ligne(s)");
        updateStatus();
        showCurrent();
        actions.addView(Ui.roundAction(this, "✓", "Revu", v -> validateReviewed()));
    }

    private String priorityStatusText() {
        if (group == null) return "";
        if (group.forecast.sustainability == J10ReviewPolicy.Sustainability.NON_TENABLE) {
            return "Prioritaire · déficit prévu " + group.forecast.deficitMinutes + " min / 10 jours";
        }
        if (group.forecast.sustainability == J10ReviewPolicy.Sustainability.TENSION) {
            return "Prioritaire · charge J10 élevée";
        }
        return "Récitez ce passage avant de reprendre la séance prévue.";
    }

    private void updateStatus() {
        if (status == null || group == null) return;
        String text = priorityStatusText();
        if (currentPage < group.lastPage) text += " · Le passage continue à la page suivante →";
        status.setText(text);
    }

    private void validateReviewed() {
        if (group == null || group.isEmpty()) return;
        if (reviewProgress == null || !reviewProgress.canValidate()) {
            Toast.makeText(this, "Affichez tout le passage avant de valider.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!planner.markReviewed(group.lineIds, HifzClock.today())) {
            Toast.makeText(this, "Impossible d’enregistrer la révision J10.", Toast.LENGTH_LONG).show();
            return;
        }
        mushaf.cycleCompleted();
        renderPriority();
    }

    private void showCurrent() {
        if (group == null || group.isEmpty()) return;
        shown = true;
        updateStatus();
        mushaf.showLineFocus(currentPage, group.verses, group.lineIds);
    }

    private void goPage(int delta) {
        if (group == null || group.isEmpty()) return;
        int target = Math.max(group.firstPage, Math.min(group.lastPage, currentPage + delta));
        if (target == currentPage) return;
        currentPage = target;
        showCurrent();
    }

    @Override public void onReady() { if (!shown) showCurrent(); }
    @Override public void onError(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override public void onPageShown(int page) {
        currentPage = page;
        updateStatus();
        if (reviewProgress != null) reviewProgress.markShown(page);
    }
    @Override public void onPageSwipe(int delta) { goPage(delta); }
    @Override public void onVerseTap(VerseRef verse) {}

    @Override protected void onResume() {
        super.onResume();
        activeStartedAt = SystemClock.elapsedRealtime();
        hostBudgetHandler.removeCallbacks(hostBudgetWatchdog);
        hostBudgetHandler.post(hostBudgetWatchdog);
    }

    @Override protected void onPause() {
        hostBudgetHandler.removeCallbacks(hostBudgetWatchdog);
        persistActiveHostTime(false);
        super.onPause();
    }

    private long hostElapsedIncludingActive() {
        long active = activeStartedAt < 0L ? 0L
            : Math.max(0L, SystemClock.elapsedRealtime() - activeStartedAt);
        return J10SessionBudget.addConsumed(prefs.elapsedFor(hostMode), active);
    }

    private boolean persistActiveHostTime(boolean keepActive) {
        if (activeStartedAt < 0L || hostMode == null) return true;
        long now = SystemClock.elapsedRealtime();
        long consumed = Math.max(0L, now - activeStartedAt);
        if (consumed <= 0L) {
            activeStartedAt = keepActive ? now : -1L;
            return true;
        }
        LocalDate today = HifzClock.today();
        if (!hostBudgetStore.addConsumed(hostMode, today, consumed)) {
            Log.e("QuranHifz", "Unable to persist J10 host-slot consumption for " + hostMode);
            return false;
        }
        prefs.setElapsedFor(hostMode,
            J10SessionBudget.addConsumed(prefs.elapsedFor(hostMode), consumed));
        activeStartedAt = keepActive ? now : -1L;
        return true;
    }

    private int hostTargetMinutes() {
        SessionKind kind;
        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(hostMode)) kind = SessionKind.SABQI_TODAY_REVIEW;
        else if (HifzSessionActivity.ITQAN.equals(hostMode)) kind = SessionKind.ITQAN;
        else if (HifzSessionActivity.RECENT_SABQI_REVIEW.equals(hostMode)) kind = SessionKind.RECENT_SABQI_REVIEW;
        else kind = SessionKind.OLD_ITQAN_MURAJAAH;
        return HifzSchedule.INSTANCE.targetMinutesFor(kind);
    }

    @Override public void onBackPressed() { moveTaskToBack(true); }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_PAGE_UP) { goPage(-1); return true; }
        if (code == KeyEvent.KEYCODE_PAGE_DOWN) { goPage(1); return true; }
        return super.onKeyDown(code, event);
    }

    @Override protected void onDestroy() {
        hostBudgetHandler.removeCallbacksAndMessages(null);
        if (mushaf != null) mushaf.destroySafely();
        super.onDestroy();
    }

    private static String safeHostMode(String mode) {
        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
                || HifzSessionActivity.ITQAN.equals(mode)
                || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
                || HifzSessionActivity.MURAJAAH.equals(mode)) return mode;
        return null;
    }
}
