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

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz personal home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private HifzSpeedStore speedStore;
    private J10HostBudgetStore hostBudgetStore;
    private DashboardLedger ledger;
    private volatile GeometryRepository geometry;
    private TextView today;
    private TextView recentSabqiAdvisory;
    private LinearLayout todayAction;
    private LinearLayout dashboard;
    private final List<View> geometryActions = new ArrayList<>();
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        hostBudgetStore = new J10HostBudgetStore(this);
        ledger = new DashboardLedger(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Ui.PAPER);
        scroll.addView(holder, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 18), bottom = Ui.dp(this, 22);
        root.setPadding(side, Ui.dp(this, 8), side, bottom);
        int screen = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = Math.max(Ui.dp(this, 300), Math.min(screen - Ui.dp(this, 18), Ui.dp(this, 900)));
        holder.addView(root, new FrameLayout.LayoutParams(
            contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        TextView title = Ui.bookText(this, "Quran Hifz", 24, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, Ui.dp(this, 1), 0, Ui.dp(this, 5));
        root.addView(title);

        todayAction = Ui.column(this);
        todayAction.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        todayAction.setClickable(true);
        todayAction.setFocusable(true);
        todayAction.setEnabled(false);
        todayAction.setContentDescription("Ouvrir la séance du jour");
        todayAction.setOnClickListener(v -> openToday());
        LinearLayout todayRow = Ui.row(this);
        TextView todayCaption = Ui.bookText(this, "Aujourd’hui", 13, true);
        Ui.weight(todayCaption, 1f);
        todayRow.addView(todayCaption);
        today = Ui.text(this, "…", 12, false);
        today.setTextColor(Ui.MUTED);
        today.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        todayRow.addView(today);
        todayAction.addView(todayRow);
        root.addView(todayAction);
        root.addView(Ui.divider(this));

        recentSabqiAdvisory = Ui.text(this, "", 10.8f, false);
        recentSabqiAdvisory.setTextColor(Ui.MUTED);
        recentSabqiAdvisory.setPadding(Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4));
        recentSabqiAdvisory.setVisibility(View.GONE);
        root.addView(recentSabqiAdvisory);

        LinearLayout primary = Ui.row(this);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 3));
        LinearLayout study = Ui.cardAction(this, "", "Lecture", v -> startActivity(new Intent(this, StudyReaderActivity.class)));
        LinearLayout free = Ui.cardAction(this, "", "Mémoriser", v -> startActivity(new Intent(this, FreeMemActivity.class)));
        LinearLayout settings = Ui.cardAction(this, "", "Paramètres", v -> startActivity(new Intent(this, SettingsActivity.class)));
        geometryActions.add(study);
        geometryActions.add(free);
        geometryActions.add(settings);
        addWeighted(primary, study, 1f);
        addWeighted(primary, free, 1f);
        addWeighted(primary, settings, 1f);
        root.addView(primary);

        TextView dashTitle = Ui.bookText(this, "Semaine", 17, true);
        dashTitle.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 3));
        root.addView(dashTitle);
        dashboard = Ui.column(this);
        dashboard.setPadding(0, 0, 0, 0);
        root.addView(dashboard);

        TextView directTitle = Ui.bookText(this, "Accès rapide", 15, true);
        directTitle.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 2));
        root.addView(directTitle);
        LinearLayout direct = Ui.row(this);
        direct.setGravity(Gravity.CENTER);
        LinearLayout sabqi = Ui.modeCard(this, "", "Leçon neuve", v -> openMode(HifzSessionActivity.SABQI));
        LinearLayout itqan = Ui.modeCard(this, "", "Ancrage", v -> openMode(HifzSessionActivity.ITQAN));
        LinearLayout murajaah = Ui.modeCard(this, "", "Entretien", v -> openMode(HifzSessionActivity.MURAJAAH));
        geometryActions.add(sabqi);
        geometryActions.add(itqan);
        geometryActions.add(murajaah);
        setGeometryActionsEnabled(false);
        addWeighted(direct, sabqi, 1f);
        addWeighted(direct, itqan, 1f);
        addWeighted(direct, murajaah, 1f);
        root.addView(direct);

        setContentView(scroll);
        Ui.respectSystemBars(this, holder, 0, 0, 0, 0);

        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                prefs.currentAnchoringEntry(loaded);
                geometry = loaded;
                runOnUiThread(() -> {
                    todayAction.setEnabled(true);
                    setGeometryActionsEnabled(true);
                    ledger.capture(prefs);
                    refreshAll();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    today.setText("Parcours indisponible");
                    todayAction.setEnabled(false);
                    setGeometryActionsEnabled(false);
                });
            }
        });
    }

    private void setGeometryActionsEnabled(boolean enabled) {
        for (View action : geometryActions) if (action != null) action.setEnabled(enabled);
    }

    @Override protected void onResume() {
        super.onResume();
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        if (hostBudgetStore == null) hostBudgetStore = new J10HostBudgetStore(this);
        if (ledger == null) ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        if (today != null && geometry != null) refreshAll();
    }

    private void refreshAll() { refreshToday(); refreshRecentSabqiAdvisory(); refreshDashboard(); }

    private DailyPlan planFor(LocalDate date) {
        return HifzSchedule.INSTANCE.planFor(
            date.getDayOfWeek(), prefs.recentSabqi().size(), prefs.recentConsolidationActivatedOn() != null);
    }

    private void openMode(String mode) {
        startActivity(new Intent(this, HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE, mode));
    }

    private void openToday() {
        String mode = firstIncompleteMode(HifzClock.today());
        if (mode != null) openMode(mode);
    }

    private String firstIncompleteMode(LocalDate date) {
        if (date.isBefore(prefs.programStartDate())) return null;
        DailyPlan plan = planFor(date);
        if (!isComplete(date, plan.getMorning().getKind())) return modeFor(plan.getMorning().getKind());
        if (!isComplete(date, plan.getEvening().getKind())) return modeFor(plan.getEvening().getKind());
        return null;
    }

    private boolean isComplete(LocalDate date, SessionKind kind) {
        if (normalProtocolComplete(date, kind)) return true;
        String mode = modeFor(kind);
        return hostBudgetStore != null && hostBudgetStore.isSlotConsumed(mode, date);
    }

    private boolean normalProtocolComplete(LocalDate date, SessionKind kind) {
        String key = date.toString();
        switch (kind) {
            case SABQI_NEW: return key.equals(prefs.lastSabqiDate());
            case SABQI_TODAY_REVIEW: return key.equals(prefs.lastSabqiTodayReviewDate());
            case ITQAN: return key.equals(prefs.lastItqanDate());
            case RECENT_SABQI_REVIEW: return key.equals(prefs.lastRecentSabqiReviewDate());
            case OLD_ITQAN_MURAJAAH: return key.equals(prefs.lastMurajaahDate());
            default: return false;
        }
    }

    private boolean hasJ10ConsumedSlot(LocalDate date, DailyPlan plan) {
        if (hostBudgetStore == null || plan == null) return false;
        SessionKind[] kinds = {plan.getMorning().getKind(), plan.getEvening().getKind()};
        for (SessionKind kind : kinds) {
            String mode = modeFor(kind);
            if (!normalProtocolComplete(date, kind) && hostBudgetStore.isSlotConsumed(mode, date)) return true;
        }
        return false;
    }

    private static String modeFor(SessionKind kind) {
        switch (kind) {
            case SABQI_NEW: return HifzSessionActivity.SABQI;
            case SABQI_TODAY_REVIEW: return HifzSessionActivity.SABQI_TODAY_REVIEW;
            case ITQAN: return HifzSessionActivity.ITQAN;
            case RECENT_SABQI_REVIEW: return HifzSessionActivity.RECENT_SABQI_REVIEW;
            case OLD_ITQAN_MURAJAAH: return HifzSessionActivity.MURAJAAH;
            default: throw new IllegalArgumentException("Unsupported session kind: " + kind);
        }
    }

    private void refreshToday() {
        GeometryRepository g = geometry;
        if (g == null) { today.setText("…"); return; }
        LocalDate date = HifzClock.today();
        if (date.isBefore(prefs.programStartDate())) {
            today.setText("Parcours non démarré");
            todayAction.setEnabled(false);
            return;
        }
        DailyPlan plan = planFor(date);
        SessionKind next = !isComplete(date, plan.getMorning().getKind())
            ? plan.getMorning().getKind()
            : !isComplete(date, plan.getEvening().getKind()) ? plan.getEvening().getKind() : null;
        if (next == null) {
            today.setText(hasJ10ConsumedSlot(date, plan)
                ? "Créneau J10 utilisé · programme du jour terminé"
                : "Matin ✓ · Soir ✓");
            todayAction.setEnabled(false);
            return;
        }
        String detail;
        try {
            switch (next) {
                case SABQI_NEW: {
                    int cursor = prefs.sabqiLineCursor();
                    if (cursor < 0) cursor = g.firstLineIndex(prefs.sabqiStart());
                    GeometryRepository.FiveLineBlock b = g.fiveLineBlock(cursor);
                    detail = "Matin · Leçon neuve · " + shortRange(b.startVerse,b.endVerse) + " · 5 lignes";
                    break;
                }
                case SABQI_TODAY_REVIEW:
                    detail = "Soir · Reprise du soir · 30 min";
                    break;
                case ITQAN: {
                    detail = anchoringTodayDetail(prefs, g);
                    break;
                }
                case RECENT_SABQI_REVIEW:
                    detail="Matin · Consolidation · 30 min";
                    break;
                case OLD_ITQAN_MURAJAAH:
                    detail="Soir · Entretien · "+plan.getEvening().getTargetMinutes()+" min";
                    break;
                default:
                    detail="Parcours à vérifier";
            }
        } catch (RuntimeException error) {
            detail="Parcours à vérifier";
        }
        today.setText(detail);
        todayAction.setEnabled(true);
    }

    static String anchoringTodayDetail(HifzPrefs prefs, GeometryRepository geometry) {
        AnchoringQueue.Entry entry = prefs.inProgressAnchoringEntry();
        if (entry == null) entry = prefs.currentAnchoringEntry(geometry);
        if (entry == null) return "Matin · Ancrage · aucune page en attente";
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        VerseRef end = GeometryRepository.parseVerse(entry.end);
        boolean fractionated = prefs.isFractionatedUnit(geometry.versesForRange(start, end));
        int reps = entry == null
            ? PreviewConfig.ITQAN_LIGHT_TOTAL_REPS
            : PreviewConfig.itqanTotalReps(entry.protocol);
        if (!fractionated) return "Matin · Ancrage · " + shortRange(start, end) + " · ×" + reps;
        int[] segments = geometry.surahSegmentLineCounts(start, end);
        int blocks = Math.max(1, PreviewConfig.fractionatedBlockCount(segments));
        int block = Math.max(0, Math.min(prefs.itqanBlockIndex(), blocks - 1));
        return "Matin · Ancrage fractionné · " + shortRange(start, end)
            + " · bloc " + (block + 1) + "/" + blocks + " · ×" + reps;
    }

    private void refreshRecentSabqiAdvisory() {
        if (recentSabqiAdvisory == null) return;
        DayOfWeek day = HifzClock.today().getDayOfWeek();
        boolean advisoryDay = day == DayOfWeek.WEDNESDAY || day == DayOfWeek.FRIDAY;
        if (!advisoryDay || prefs.recentSabqi().isEmpty()) {
            recentSabqiAdvisory.setVisibility(View.GONE);
            recentSabqiAdvisory.setText("");
            return;
        }
        int[] range = HifzCadence.advisoryFiveLineRange(speedStore.consolidationSecondsPerLine());
        recentSabqiAdvisory.setText("Consolidation · rappel libre 5–10 min · " + range[0] + "–" + range[1] + " lignes");
        recentSabqiAdvisory.setVisibility(View.VISIBLE);
    }

    private void refreshDashboard() {
        if (dashboard == null || geometry == null) return;
        dashboard.removeAllViews();
        LinearLayout header = Ui.row(this);
        header.setPadding(0, Ui.dp(this, 1), 0, Ui.dp(this, 2));
        addCell(header, "Jour", 0.62f, true, true);
        addCell(header, "Matin", 2.05f, true, false);
        addCell(header, "Soir", 2.05f, true, false);
        addCell(header, "État", 1.05f, true, false);
        dashboard.addView(header);
        dashboard.addView(Ui.divider(this));

        List<WeeklyDashboardPlanner.Row> rows;
        try {
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, hostBudgetStore).week(HifzClock.today());
        } catch (RuntimeException error) {
            dashboard.removeAllViews();
            TextView unavailable = Ui.text(this, "Semaine indisponible", 10.8f, false);
            unavailable.setTextColor(Ui.MUTED);
            unavailable.setPadding(Ui.dp(this, 4), Ui.dp(this, 3), Ui.dp(this, 4), Ui.dp(this, 3));
            dashboard.addView(unavailable);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            WeeklyDashboardPlanner.Row item = rows.get(i);
            LinearLayout row = Ui.row(this);
            row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
            addCell(row, item.day, 0.62f, true, true);
            addCell(row, compactSession(item.morning), 2.05f, false, false);
            addCell(row, compactSession(item.evening), 2.05f, false, false);
            addCell(row, compactState(item.state), 1.05f, false, false);
            dashboard.addView(row);
            if (i + 1 < rows.size()) dashboard.addView(Ui.divider(this));
        }
    }

    private void addCell(LinearLayout row, String value, float weight, boolean bold, boolean singleLine) {
        TextView cell = Ui.text(this, value, 10.8f, bold);
        cell.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));
        cell.setSingleLine(singleLine);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        row.addView(cell);
    }

    private void addWeighted(LinearLayout row, View view, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        int gap = Ui.dp(this, 2);
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
