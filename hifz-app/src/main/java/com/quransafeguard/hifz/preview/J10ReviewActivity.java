package com.quransafeguard.hifz.preview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;

/** Small J10 prelude fed by the single acquired-line list; it is not a new weekly session type. */
public final class J10ReviewActivity extends android.app.Activity implements MushafView.Listener {
    private J10ReviewPlanner planner;
    private J10ReviewPlanner.PriorityGroup group;
    private MushafView mushaf;
    private TextView title;
    private TextView status;
    private LinearLayout actions;
    private int currentPage = 1;
    private boolean shown;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        planner = new J10ReviewPlanner(this);
        buildUi();
        renderPriority();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setPadding(0, 0, 0, 0);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 6), 0);
        top.addView(Ui.iconButton(this, "", "Quitter", v -> moveTaskToBack(true)));
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
        group = planner.priorityGroup(LocalDate.now());
        if (group == null || group.isEmpty()) {
            finish();
            return;
        }
        currentPage = group.firstPage;
        title.setText("Priorité J10 · J" + group.maxAgeDays + " · " + group.lineIds.size() + " ligne(s)");
        if (group.forecast.sustainability == J10ReviewPolicy.Sustainability.NON_TENABLE) {
            status.setText("Prioritaire · déficit prévu " + group.forecast.deficitMinutes + " min / 10 jours");
        } else if (group.forecast.sustainability == J10ReviewPolicy.Sustainability.TENSION) {
            status.setText("Prioritaire · charge J10 élevée");
        } else {
            status.setText("Récitez ce passage avant de reprendre la séance prévue.");
        }
        showCurrent();
        actions.addView(Ui.roundAction(this, "✓", "Revu", v -> validateReviewed()));
    }

    private void validateReviewed() {
        if (group == null || group.isEmpty()) return;
        if (!planner.markReviewed(group.lineIds, LocalDate.now())) {
            Toast.makeText(this, "Impossible d’enregistrer la révision J10.", Toast.LENGTH_LONG).show();
            return;
        }
        mushaf.cycleCompleted();
        renderPriority();
    }

    private void showCurrent() {
        if (group == null || group.isEmpty()) return;
        shown = true;
        mushaf.show(currentPage, group.verses, group.lineIds, 0);
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
    @Override public void onPageShown(int page) { currentPage = page; }
    @Override public void onPageSwipe(int delta) { goPage(delta); }
    @Override public void onVerseTap(VerseRef verse) {}

    @Override public void onBackPressed() { moveTaskToBack(true); }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_PAGE_UP) { goPage(-1); return true; }
        if (code == KeyEvent.KEYCODE_PAGE_DOWN) { goPage(1); return true; }
        return super.onKeyDown(code, event);
    }

    @Override protected void onDestroy() {
        if (mushaf != null) mushaf.destroySafely();
        super.onDestroy();
    }
}
