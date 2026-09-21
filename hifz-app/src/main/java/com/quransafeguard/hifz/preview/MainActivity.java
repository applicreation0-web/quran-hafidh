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
    private DashboardLedger ledger;
    private volatile GeometryRepository geometry;
    private boolean consolidationNeedsAttention;
    private boolean learningConsolidationNeedsAttention;
    private TextView today;
    private TextView recentSabqiAdvisory;
    private LinearLayout todayAction;
    private LinearLayout dashboard;
    private LinearLayout sabqiQuickAccess;
    private LinearLayout itqanQuickAccess;
    private LinearLayout murajaahQuickAccess;
    private final List<View> geometryActions = new ArrayList<>();
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
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
        LinearLayout murajaah = Ui.modeCard(this, "", "Révision", v -> openMode(murajaahQuickAccessMode()));
        sabqiQuickAccess = sabqi;
        itqanQuickAccess = itqan;
        murajaahQuickAccess = murajaah;
        geometryActions.add(sabqi);
        geometryActions.add(itqan);
        geometryActions.add(murajaah);
        addWeighted(direct, sabqi, 1f);
        addWeighted(direct, itqan, 1f);
        addWeighted(direct, murajaah, 1f);
        root.addView(direct);

        LinearLayout directEvening = Ui.row(this);
        directEvening.setGravity(Gravity.CENTER);
        LinearLayout renforcement = Ui.modeCard(this, "", "Renforcement", v -> openMode(HifzSessionActivity.LEARNING_CONSOLIDATION));
        LinearLayout consolidation = Ui.modeCard(this, "", "Consolidation", v -> openMode(HifzSessionActivity.RECENT_SABQI_REVIEW));
        geometryActions.add(renforcement);
        geometryActions.add(consolidation);
        setGeometryActionsEnabled(false);
        addWeighted(directEvening, renforcement, 1f);
        addWeighted(directEvening, consolidation, 1f);
        root.addView(directEvening);

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
        if (ledger == null) ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        if (today != null && geometry != null) refreshAll();
    }

    private void refreshAll() { refreshQuickAccessCadenceGating(); refreshMurajaahQuickAccessCue(); refreshToday(); refreshRecentSabqiAdvisory(); refreshDashboard(); }

    /**
     * Apprentissage/Stabilisation quick-access must respect the weekday-pinned cadence (Settings'
     * configurable Apprentissage-days-per-week split, Mon/Wed/Fri by default) — opening tomorrow's
     * Apprentissage today would let a learner get ahead of the weekly snowball attribution it's
     * built on. Révision/Renforcement/Consolidation stay free since they need to be
     * testable/catchable-up any day.
     */
    private void refreshQuickAccessCadenceGating() {
        if (geometry == null) return;
        CadenceAction action = HifzSchedule.INSTANCE.actionFor(HifzClock.today().getDayOfWeek(), prefs.learningDaysPerWeek());
        if (sabqiQuickAccess != null) sabqiQuickAccess.setEnabled(action == CadenceAction.LEARNING);
        if (itqanQuickAccess != null) itqanQuickAccess.setEnabled(action == CadenceAction.STABILIZATION);
    }

    /**
     * The "Révision" quick-access card's duration cue must track whichever of the daily
     * active/passive pair murajaahQuickAccessMode() will actually open — a leftover static
     * "30 min" (from before the 15 min active / 45 min passive split) is wrong for both.
     */
    private void refreshMurajaahQuickAccessCue() {
        if (murajaahQuickAccess == null || murajaahQuickAccess.getChildCount() < 3) return;
        boolean activeNext = HifzSessionActivity.MURAJAAH_ACTIVE.equals(murajaahQuickAccessMode());
        int minutes = HifzSchedule.INSTANCE.targetMinutesFor(
            activeNext ? SessionKind.ACTIVE_MURAJAAH : SessionKind.OLD_ITQAN_MURAJAAH);
        View cue = murajaahQuickAccess.getChildAt(2);
        if (cue instanceof TextView) ((TextView) cue).setText(minutes + " min");
    }

    private void openMode(String mode) { openMode(mode,HifzClock.today()); }

    private void openMode(String mode,LocalDate scheduledDate) {
        Intent intent=new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        if(scheduledDate!=null)intent.putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduledDate.toString());
        startActivity(intent);
    }

    private boolean modeComplete(LocalDate date,String mode){
        return ledger.find(date,mode)!=null;
    }

    /** Whichever of the daily active/passive Révision pair is still due today comes first. */
    private String murajaahQuickAccessMode(){
        String today=HifzClock.today().toString();
        return today.equals(prefs.lastActiveMurajaahDate())
            ? HifzSessionActivity.MURAJAAH : HifzSessionActivity.MURAJAAH_ACTIVE;
    }

    /**
     * A past Sunday's weekly snowball final review can never be caught up later — by the time it
     * would be revisited, the accumulator has already rolled to a new week — so once a Sunday is
     * no longer today it is treated as satisfied rather than stalling every later cadence day.
     */
    private boolean cadenceComplete(LocalDate date){
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), prefs.learningDaysPerWeek());
        switch(action){
            case LEARNING:return modeComplete(date,HifzSessionActivity.SABQI);
            case STABILIZATION:return modeComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:
                if(!date.equals(HifzClock.today()))return true;
                return learningFinalResolved(date)&&consolidationFinalResolved(date)
                    &&date.toString().equals(prefs.lastActiveMurajaahDate())
                    &&date.toString().equals(prefs.lastMurajaahDate());
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
        return HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed,prefs.learningDaysPerWeek());
    }

    /** Done, or nothing to review this week (rare, e.g. a brand-new install's first week). */
    private boolean learningFinalResolved(LocalDate date){
        return date.toString().equals(prefs.lastLearningConsolidationDate())
            || prefs.learningSnowballFinalUnits(date).isEmpty();
    }

    private boolean consolidationFinalResolved(LocalDate date){
        return date.toString().equals(prefs.lastRecentSabqiReviewDate())
            || prefs.stabilizationSnowballFinalUnits(date).isEmpty();
    }

    /** Lundi/Mercredi/Vendredi soir, once the morning Apprentissage is done: Renforcement, then Révision active, then Entretien. */
    private String eveningLearningMode(LocalDate today){
        String todayStr=today.toString();
        if(!todayStr.equals(prefs.lastLearningSnowballEveningDate())&&!prefs.learningConsolidationUnits(today).isEmpty())
            return HifzSessionActivity.LEARNING_CONSOLIDATION;
        if(!todayStr.equals(prefs.lastActiveMurajaahDate()))return HifzSessionActivity.MURAJAAH_ACTIVE;
        if(!todayStr.equals(prefs.lastMurajaahDate()))return HifzSessionActivity.MURAJAAH;
        return null;
    }

    /** Mardi/Jeudi/Samedi soir, once the morning Stabilisation is done: Consolidation, then Révision active, then Entretien. */
    private String eveningStabilizationMode(LocalDate today){
        String todayStr=today.toString();
        if(!todayStr.equals(prefs.lastStabilizationSnowballEveningDate())&&!prefs.stabilizedConsolidationUnits(today).isEmpty())
            return HifzSessionActivity.RECENT_SABQI_REVIEW;
        if(!todayStr.equals(prefs.lastActiveMurajaahDate()))return HifzSessionActivity.MURAJAAH_ACTIVE;
        if(!todayStr.equals(prefs.lastMurajaahDate()))return HifzSessionActivity.MURAJAAH;
        return null;
    }

    /** Dimanche soir, once the morning ×5 finales are done: Révision active, then ordinary Entretien, same as every other evening. */
    private String eveningRevisionMode(LocalDate today){
        String todayStr=today.toString();
        if(!todayStr.equals(prefs.lastActiveMurajaahDate()))return HifzSessionActivity.MURAJAAH_ACTIVE;
        if(!todayStr.equals(prefs.lastMurajaahDate()))return HifzSessionActivity.MURAJAAH;
        return null;
    }

    private String nextMode(ScheduledCadence due){
        consolidationNeedsAttention=false;
        learningConsolidationNeedsAttention=false;
        try{
            return computeNextMode(due);
        }catch(RuntimeException error){
            consolidationNeedsAttention=true;
            learningConsolidationNeedsAttention=true;
            android.util.Log.e("QuranHifz","Unable to evaluate next Hifz mode",error);
            return null;
        }
    }

    private String computeNextMode(ScheduledCadence due){
        if(due==null)return null;
        LocalDate today=HifzClock.today();
        LocalDate date=due.getScheduledDate();
        boolean isToday=date.equals(today);
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return isToday?eveningLearningMode(today):null;
            case STABILIZATION:
                if(!modeComplete(date,HifzSessionActivity.ITQAN))return HifzSessionActivity.ITQAN;
                return isToday?eveningStabilizationMode(today):null;
            case REVISION:
                if(!isToday)return null;
                if(!learningFinalResolved(today))return HifzSessionActivity.LEARNING_FINAL;
                if(!consolidationFinalResolved(today))return HifzSessionActivity.CONSOLIDATION_FINAL;
                return eveningRevisionMode(today);
            default:return null;
        }
    }

    private static boolean isTodayAnchoredMode(String mode){
        return HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ||HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode)
            ||HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode)
            ||HifzSessionActivity.LEARNING_FINAL.equals(mode)
            ||HifzSessionActivity.MURAJAAH.equals(mode)
            ||HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode);
    }

    private void openToday() {
        LocalDate current=HifzClock.today();
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention||learningConsolidationNeedsAttention){
            startActivity(new Intent(this,SettingsActivity.class));
            return;
        }
        if(mode==null)return;
        LocalDate scheduled=isTodayAnchoredMode(mode)?current:due.getScheduledDate();
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
        if(consolidationNeedsAttention||learningConsolidationNeedsAttention){
            today.setText("Consolidation · état à vérifier");
            todayAction.setEnabled(true);
            return;
        }
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String prefix=!isTodayAnchoredMode(mode)&&due!=null&&due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)){
                detail="Consolidation · boule de neige du soir";
            }else if(HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode)){
                detail="Renforcement · boule de neige du soir";
            }else if(HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode)){
                detail="Consolidation · révision finale ×5";
            }else if(HifzSessionActivity.LEARNING_FINAL.equals(mode)){
                detail="Renforcement · révision finale ×5";
            }else if(HifzSessionActivity.SABQI.equals(mode)){
                int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · "+b.lineIds.size()+" lignes";
            }else if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)){
                detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(HifzSessionActivity.ITQAN.equals(mode)){
                detail=anchoringTodayDetail(prefs,g);
            }else if(HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode)){
                detail="Révision active · "+HifzSchedule.ACTIVE_REVIEW_MINUTES+" min";
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
        if(blocks<=1){
            int lines=planned.isEmpty()?0:planned.get(0).lineIds.size();
            return "Stabilisation · "+shortRange(start,end)+" · "+lines+" lignes · ×"+reps;
        }
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        int lines=planned.get(block).lineIds.size();
        return "Stabilisation · "+shortRange(start,end)+" · bloc "+(block+1)+"/"+blocks+" · "+lines+" lignes · ×"+reps;
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
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger).week(HifzClock.today());
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
