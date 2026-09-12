package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledSession;
import com.quransafeguard.hifz.core.SessionType;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz personal home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private DashboardLedger ledger;
    private volatile GeometryRepository geometry;
    private TextView today;
    private LinearLayout todayAction;
    private LinearLayout dashboard;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        ledger = new DashboardLedger(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Ui.PAPER);
        scroll.addView(holder, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 18), bottom = Ui.dp(this, 24);
        root.setPadding(side, Ui.dp(this, 12), side, bottom);
        int screen = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = Math.max(Ui.dp(this, 300), Math.min(screen - Ui.dp(this, 18), Ui.dp(this, 900)));
        holder.addView(root, new FrameLayout.LayoutParams(
            contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        TextView title = Ui.bookText(this, "Quran Hifz", 28, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 10));
        root.addView(title);

        todayAction = Ui.column(this);
        todayAction.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        Ui.panel(todayAction);
        todayAction.setClickable(true);
        todayAction.setFocusable(true);
        todayAction.setEnabled(false);
        todayAction.setContentDescription("Ouvrir la séance du jour");
        todayAction.setOnClickListener(v -> openToday());
        TextView todayCaption = Ui.text(this, "Aujourd’hui", 10.5f, false);
        todayCaption.setTextColor(Ui.MUTED);
        todayAction.addView(todayCaption);
        today = Ui.bookText(this, "…", 16, true);
        today.setPadding(0, Ui.dp(this, 2), 0, 0);
        todayAction.addView(today);
        root.addView(todayAction);

        // The Today card is the session launcher. Keep only the three global destinations here.
        LinearLayout primary = Ui.row(this);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        LinearLayout study = Ui.cardAction(this, "", "Lecture", v -> startActivity(new Intent(this, StudyReaderActivity.class)));
        LinearLayout free = Ui.cardAction(this, "", "Mémoriser", v -> startActivity(new Intent(this, FreeMemActivity.class)));
        LinearLayout settings = Ui.cardAction(this, "", "Paramètres", v -> startActivity(new Intent(this, SettingsActivity.class)));
        addWeighted(primary, study, 1f);
        addWeighted(primary, free, 1f);
        addWeighted(primary, settings, 1f);
        root.addView(primary);

        TextView dashTitle = Ui.bookText(this, "Semaine", 20, true);
        dashTitle.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        root.addView(dashTitle);
        dashboard = Ui.column(this);
        dashboard.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        Ui.panel(dashboard);
        root.addView(dashboard);

        TextView directTitle = Ui.bookText(this, "Accès rapide", 17, true);
        directTitle.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 5));
        root.addView(directTitle);
        LinearLayout direct = Ui.row(this);
        direct.setGravity(Gravity.CENTER);
        LinearLayout sabqi = Ui.modeCard(this, "", "Sabqi", v -> openMode(HifzSessionActivity.SABQI));
        LinearLayout itqan = Ui.modeCard(this, "", "Itqān", v -> openMode(HifzSessionActivity.ITQAN));
        LinearLayout murajaah = Ui.modeCard(this, "", "Murājaʿah", v -> openMode(HifzSessionActivity.MURAJAAH));
        addWeighted(direct, sabqi, 1f);
        addWeighted(direct, itqan, 1f);
        addWeighted(direct, murajaah, 1f);
        root.addView(direct);

        setContentView(scroll);
        Ui.respectSystemBars(this, holder, 0, 0, 0, 0);

        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                geometry = loaded;
                runOnUiThread(() -> {
                    todayAction.setEnabled(true);
                    ledger.capture(prefs);
                    refreshAll();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    today.setText("Parcours indisponible");
                    todayAction.setEnabled(false);
                });
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        prefs = new HifzPrefs(this);
        if (ledger == null) ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        if (today != null && geometry != null) refreshAll();
    }

    private void refreshAll() { refreshToday(); refreshDashboard(); }

    private void openMode(String mode) {
        startActivity(new Intent(this, HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE, mode));
    }

    private void openToday() {
        LocalDate date = LocalDate.now();
        ScheduledSession scheduled = HifzSchedule.INSTANCE.scheduled(date, prefs.programStartDate(), date);
        if (scheduled == null) return;
        String todayKey = date.toString();
        boolean eveningMurajaah = HifzSchedule.INSTANCE.hasEveningMurajaah(date.getDayOfWeek());
        if (eveningMurajaah && todayKey.equals(prefs.lastItqanDate()) && !todayKey.equals(prefs.lastMurajaahDate())) {
            openMode(HifzSessionActivity.MURAJAAH);
            return;
        }
        switch (scheduled.getType()) {
            case SABQI: openMode(HifzSessionActivity.SABQI); break;
            case ITQAN: openMode(HifzSessionActivity.ITQAN); break;
            case MURAJAAH: openMode(HifzSessionActivity.MURAJAAH); break;
            default: throw new IllegalStateException("Unsupported Hifz session type: " + scheduled.getType());
        }
    }

    private void refreshToday() {
        GeometryRepository g = geometry;
        if (g == null) { today.setText("…"); return; }
        LocalDate date = LocalDate.now();
        ScheduledSession scheduled = HifzSchedule.INSTANCE.scheduled(date, prefs.programStartDate(), date);
        if (scheduled == null) {
            today.setText("Parcours non démarré");
            todayAction.setEnabled(false);
            return;
        }
        String detail;
        try {
            String todayKey = date.toString();
            boolean eveningMurajaah = HifzSchedule.INSTANCE.hasEveningMurajaah(date.getDayOfWeek());
            if (eveningMurajaah && todayKey.equals(prefs.lastItqanDate())) {
                detail = todayKey.equals(prefs.lastMurajaahDate())
                    ? "Itqān ✓ · Murājaʿah ✓"
                    : "Soir · Murājaʿah · Bloc " + prefs.murajaahPhase();
                today.setText(detail);
                todayAction.setEnabled(true);
                return;
            }
            SessionType kind = scheduled.getType();
            switch (kind) {
                case SABQI: {
                    int cursor = prefs.sabqiLineCursor();
                    if (cursor < 0) cursor = g.firstLineIndex(prefs.sabqiStart());
                    if (cursor < g.firstLineIndex(prefs.sabqiStart()) || cursor > g.lastLineIndex(prefs.sabqiEnd())) {
                        detail = "Sabqi · à repositionner";
                    } else {
                        GeometryRepository.FiveLineBlock b = g.fiveLineBlock(cursor);
                        int rep = prefs.sabqiRep();
                        String state = rep >= PreviewConfig.SABQI_TOTAL_REPS ? "prêt à valider" : rep > 0 ? (rep + 1) + "/37" : "37 répétitions";
                        detail = "Sabqi · " + shortRange(b.startVerse, b.endVerse) + " · " + state;
                    }
                    break;
                }
                case ITQAN: {
                    if (!prefs.isItqanCursorValid()) { detail = "Itqān · à repositionner"; break; }
                    int rep = prefs.itqanRep();
                    VerseRef start = prefs.itqanUnitStart(), end = prefs.itqanUnitEnd();
                    if (rep > 0 && start != null && end != null) {
                        String state = rep >= PreviewConfig.ITQAN_TOTAL_REPS ? "prêt à valider" : (rep + 1) + "/" + PreviewConfig.ITQAN_TOTAL_REPS;
                        detail = "Itqān · " + shortRange(start, end) + " · " + state;
                    } else {
                        GeometryRepository.VerseUnit u = g.eligiblePageUnit(prefs.itqanCursor(), prefs.corpus());
                        detail = "Itqān · " + shortRange(u.start, u.end) + " · ×" + PreviewConfig.ITQAN_TOTAL_REPS;
                    }
                    break;
                }
                case MURAJAAH:
                    detail = "Murājaʿah · Bloc " + prefs.murajaahPhase();
                    break;
                default:
                    throw new IllegalStateException("Unsupported Hifz session type: " + kind);
            }
        } catch (RuntimeException error) {
            detail = "Parcours à vérifier";
        }
        today.setText(detail);
        todayAction.setEnabled(true);
    }

    private void refreshDashboard() {
        if (dashboard == null || geometry == null) return;
        dashboard.removeAllViews();
        LinearLayout header = Ui.row(this);
        header.setPadding(0, Ui.dp(this, 1), 0, Ui.dp(this, 3));
        addCell(header, "Jour", 0.62f, true, true);
        addCell(header, "Matin", 2.05f, true, false);
        addCell(header, "Soir", 2.05f, true, false);
        addCell(header, "État", 1.05f, true, false);
        dashboard.addView(header);

        List<WeeklyDashboardPlanner.Row> rows = new WeeklyDashboardPlanner(prefs, geometry, ledger).week(LocalDate.now());
        for (WeeklyDashboardPlanner.Row item : rows) {
            LinearLayout row = Ui.row(this);
            row.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            addCell(row, item.day, 0.62f, true, true);
            addCell(row, compactSession(item.morning), 2.05f, false, false);
            addCell(row, compactSession(item.evening), 2.05f, false, false);
            addCell(row, compactState(item.state), 1.05f, false, false);
            dashboard.addView(row);
        }
    }

    private void addCell(LinearLayout row, String value, float weight, boolean bold, boolean singleLine) {
        TextView cell = Ui.text(this, value, 11.2f, bold);
        cell.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));
        cell.setSingleLine(singleLine);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        row.addView(cell);
    }

    private void addWeighted(LinearLayout row, View view, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        int gap = Ui.dp(this, 3);
        lp.setMargins(gap, gap, gap, gap);
        row.addView(view, lp);
    }

    private static String compactState(String state) {
        if (state == null || state.trim().isEmpty()) return "—";
        if (state.contains("Parcours non démarré")) return "—";
        if (state.startsWith("En cours")) return "En cours";
        return state;
    }

    private static String compactSession(String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        return value.replace("Sourate ", "S.").replace("v.", "");
    }

    private static String shortRange(VerseRef a, VerseRef b) {
        if (a.getSurah() == b.getSurah()) return a.getSurah() + ":" + a.getAyah() + "–" + b.getAyah();
        return a.getSurah() + ":" + a.getAyah() + " → " + b.getSurah() + ":" + b.getAyah();
    }

    @Override protected void onDestroy() {
        localLoader.shutdownNow();
        super.onDestroy();
    }
}
