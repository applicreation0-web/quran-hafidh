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

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.ScheduledCadence;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        LinearLayout sabqi = Ui.modeCard(this, "", "Apprentissage", v -> openMode(HifzSessionActivity.SABQI));
        LinearLayout itqan = Ui.modeCard(this, "", "Stabilisation", v -> openMode(HifzSessionActivity.ITQAN));
        LinearLayout murajaah = Ui.modeCard(this, "", "Révision", v -> openMode(HifzSessionActivity.MURAJAAH));
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

    private void openMode(String mode) { openMode(mode,HifzClock.today()); }

    private void openMode(String mode,LocalDate scheduledDate) {
        Intent intent=new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        if(scheduledDate!=null)intent.putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduledDate.toString());
        startActivity(intent);
    }

    private boolean modeComplete(LocalDate date,String mode){
        if(ledger.find(date,mode)!=null)return true;
        return hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(mode,date);
    }

    private boolean cadenceComplete(LocalDate date){
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek());
        switch(action){
            case LEARNING:return modeComplete(date,HifzSessionActivity.SABQI)
                &&modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
            case STABILIZATION:return modeComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:return modeComplete(date,HifzSessionActivity.MURAJAAH);
            default:return false;
        }
    }

    private ScheduledCadence nextDueCadence(LocalDate todayDate){
        LinkedHashSet<LocalDate> completed=new LinkedHashSet<>();
        LocalDate cursor=prefs.programStartDate();
        while(!cursor.isAfter(todayDate)){
            if(cadenceComplete(cursor))completed.add(cursor);
            cursor=cursor.plusDays(1);
        }
        return HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed);
    }

    private boolean progressionConsolidationDue(GeometryRepository g){
        if(g==null)return false;
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Session open=prefs.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.STABILIZATION);
        return open!=null || !prefs.stabilizedConsolidationUnits(g,3).isEmpty();
    }

    private String nextMode(ScheduledCadence due){
        if(progressionConsolidationDue(geometry))return HifzSessionActivity.RECENT_SABQI_REVIEW;
        if(due==null)return null;
        LocalDate date=due.getScheduledDate();
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return !modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW)?HifzSessionActivity.SABQI_TODAY_REVIEW:null;
            case STABILIZATION:return !modeComplete(date,HifzSessionActivity.ITQAN)?HifzSessionActivity.ITQAN:null;
            case REVISION:return !modeComplete(date,HifzSessionActivity.MURAJAAH)?HifzSessionActivity.MURAJAAH:null;
            default:return null;
        }
    }

    private void openToday() {
        LocalDate current=HifzClock.today();
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null)return;
        LocalDate scheduled=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ? current : due.getScheduledDate();
        openMode(mode,scheduled);
    }

    private void refreshToday() {
        GeometryRepository g=geometry;
        if(g==null){today.setText("…");return;}
        LocalDate current=HifzClock.today();
        if(current.isBefore(prefs.programStartDate())){
            today.setText("Parcours non démarré");todayAction.setEnabled(false);return;
        }
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        boolean consolidation=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode);
        String prefix=!consolidation&&due!=null&&due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(consolidation){
                detail="Consolidation · déclenchée par progression";
            }else if(HifzSessionActivity.SABQI.equals(mode)){
                int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · 5 lignes";
            }else if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)){
                detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(HifzSessionActivity.ITQAN.equals(mode)){
                detail=anchoringTodayDetail(prefs,g);
            }else if(HifzSessionActivity.MURAJAAH.equals(mode)){
                detail="Révision · "+HifzSchedule.MAINTENANCE_MINUTES+" min";
            }else detail="Parcours à vérifier";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        today.setText(prefix+detail);todayAction.setEnabled(true);
    }

    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
        List<String> owned=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
        List<StabilizationHalfPagePolicy.Unit> planned=StabilizationHalfPagePolicy.planPage(
            geometry.linesForExactIds(owned));
        int blocks=Math.max(1,planned.size());
        if(blocks<=1)return "Stabilisation · "+shortRange(start,end)+" · ×"+reps;
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        return "Stabilisation · "+shortRange(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
    }

    private void refreshRecentSabqiAdvisory() {
        if (recentSabqiAdvisory == null) return;
        recentSabqiAdvisory.setText("");
        recentSabqiAdvisory.setVisibility(View.GONE);
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
