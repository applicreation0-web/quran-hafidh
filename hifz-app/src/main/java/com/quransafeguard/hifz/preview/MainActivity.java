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
import com.quransafeguard.hifz.core.HifzSchedule;
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
    private final List<View> geometryActions = new ArrayList<>();
    private LinearLayout itqanCard;
    private ScrollView homeScroll;
    private ScrollView parcoursScroll;
    private boolean showingParcours;
    private LinearLayout parcoursTodayAction;
    private TextView parcoursToday;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SemanticPassageRepository.preloadAsync(this);
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        ledger = new DashboardLedger(this);

        // Spec UI pass 2 (§4/§5): a short home — header with the Settings icon, "Aujourd’hui",
        // then five plain lines. The five Hifz paths and the week live one level down, in
        // "Parcours Hifz" (same activity, so every cadence/routing rule below stays untouched).
        LinearLayout root = page();
        homeScroll = scrollOf(root);
        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        TextView title = Ui.bookText(this, "Quran Haafidh", 21, true);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        header.addView(title);
        header.addView(Ui.iconButton(this, "", "Paramètres", v -> startActivity(new Intent(this, SettingsActivity.class))));
        root.addView(header);
        root.addView(Ui.divider(this));

        todayAction = todayLine();
        today = Ui.settingValue(todayAction);
        root.addView(todayAction);
        root.addView(Ui.divider(this));

        recentSabqiAdvisory = Ui.text(this, "", 10.8f, false);
        recentSabqiAdvisory.setTextColor(Ui.MUTED);
        recentSabqiAdvisory.setPadding(Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4));
        recentSabqiAdvisory.setVisibility(View.GONE);
        root.addView(recentSabqiAdvisory);

        LinearLayout study = navLine(root, "Lecture", "", v -> startActivity(new Intent(this, StudyReaderActivity.class)));
        LinearLayout free = navLine(root, "Mémorisation libre", "", v -> startActivity(new Intent(this, FreeMemActivity.class)));
        // Révision works memory; Quiz only questions it — free, read-only on Progression.
        LinearLayout quiz = navLine(root, "Quiz", "", v -> startActivity(new Intent(this, QuizActivity.class)));
        LinearLayout parcours = navLine(root, "Parcours Hifz", "", v -> showParcours());
        LinearLayout progress = navLine(root, "Progression", "", v -> startActivity(new Intent(this, ProgressMapActivity.class)));
        geometryActions.add(study);
        geometryActions.add(free);
        geometryActions.add(quiz);
        geometryActions.add(progress);

        // --- Parcours Hifz: the five paths, one line each, then this week.
        LinearLayout path = page();
        parcoursScroll = scrollOf(path);
        LinearLayout pathHeader = Ui.row(this);
        pathHeader.setGravity(Gravity.CENTER_VERTICAL);
        pathHeader.addView(Ui.iconButton(this, "‹", "Retour", v -> showHome()));
        TextView pathTitle = Ui.bookText(this, "Parcours Hifz", 19, true);
        pathTitle.setGravity(Gravity.CENTER);
        Ui.weight(pathTitle, 1f);
        pathHeader.addView(pathTitle);
        pathHeader.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        path.addView(pathHeader);
        path.addView(Ui.divider(this));
        parcoursTodayAction = todayLine();
        parcoursToday = Ui.settingValue(parcoursTodayAction);
        path.addView(parcoursTodayAction);
        path.addView(Ui.divider(this));

        LinearLayout sabqi = navLine(path, "Apprentissage", "5 lignes", v -> openMode(HifzSessionActivity.SABQI));
        itqanCard = navLine(path, "Stabilisation", Ui.stabilizationCue(prefs.itqanPostNasMaintenance()),
            v -> openMode(HifzSessionActivity.ITQAN));
        LinearLayout itqan = itqanCard;
        LinearLayout murajaah = navLine(path, "Révision", "Au choix", v -> showRevisionSelector());
        LinearLayout renforcement = navLine(path, "Renforcement", "Boule de neige", v -> openMode(renforcementQuickAccessMode()));
        LinearLayout consolidation = navLine(path, "Consolidation", "Boule de neige", v -> openMode(consolidationQuickAccessMode()));
        sabqiQuickAccess = sabqi;
        itqanQuickAccess = itqan;
        geometryActions.add(parcours);
        geometryActions.add(sabqi);
        geometryActions.add(itqan);
        geometryActions.add(murajaah);
        geometryActions.add(renforcement);
        geometryActions.add(consolidation);
        setGeometryActionsEnabled(false);

        TextView dashTitle = Ui.bookText(this, "Cette semaine", 15, true);
        dashTitle.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 3));
        path.addView(dashTitle);
        dashboard = Ui.column(this);
        dashboard.setPadding(0, 0, 0, 0);
        path.addView(dashboard);

        showHome();

        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                prefs.reconcileV6AcquiredBootstrap(loaded);
                prefs.repairStraddlingAcquiredVerses(loaded);
                prefs.currentAnchoringEntry(loaded);
                geometry = loaded;
                runOnUiThread(() -> {
                    setTodayEnabled(true);
                    setGeometryActionsEnabled(true);
                    ledger.capture(prefs);
                    refreshAll();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    setToday("Parcours indisponible", false);
                    setGeometryActionsEnabled(false);
                });
            }
        });
    }

    private void setGeometryActionsEnabled(boolean enabled) {
        for (View action : geometryActions) setLineEnabled(action, enabled);
    }

    /** A plain line greys out when it cannot be opened (no card to dim). */
    private static void setLineEnabled(View line, boolean enabled) {
        if (line == null) return;
        line.setEnabled(enabled);
        line.setAlpha(enabled ? 1f : 0.42f);
    }

    private LinearLayout page() {
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 16);
        root.setPadding(side, Ui.dp(this, 4), side, Ui.dp(this, 16));
        return root;
    }

    private ScrollView scrollOf(LinearLayout root) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Ui.PAPER);
        scroll.addView(holder, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int screen = getResources().getDisplayMetrics().widthPixels;
        int contentWidth = Math.max(Ui.dp(this, 300), Math.min(screen, Ui.dp(this, 720)));
        holder.addView(root, new FrameLayout.LayoutParams(
            contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        Ui.respectSystemBars(this, holder, 0, 0, 0, 0);
        return scroll;
    }

    /** "Aujourd’hui ··· next session ›" — the main destination, a line, never a card. */
    private LinearLayout todayLine() {
        LinearLayout line = Ui.settingRow(this, "Aujourd’hui", "…", v -> openToday());
        if (line.getChildAt(0) instanceof TextView) {
            ((TextView) line.getChildAt(0)).setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        }
        line.setContentDescription("Ouvrir la séance du jour");
        setLineEnabled(line, false);
        return line;
    }

    private LinearLayout navLine(LinearLayout parent, String label, String state, View.OnClickListener listener) {
        LinearLayout line = Ui.settingRow(this, label, state, listener);
        parent.addView(line);
        parent.addView(Ui.divider(this));
        return line;
    }

    /** "Aujourd’hui" is shown on the home and atop Parcours Hifz: both lines stay identical. */
    private void setToday(String text, boolean enabled) {
        today.setText(text);
        if (parcoursToday != null) parcoursToday.setText(text);
        setTodayEnabled(enabled);
    }

    private void setTodayEnabled(boolean enabled) {
        setLineEnabled(todayAction, enabled);
        setLineEnabled(parcoursTodayAction, enabled);
    }

    private void showHome() {
        showingParcours = false;
        setContentView(homeScroll);
    }

    private void showParcours() {
        showingParcours = true;
        setContentView(parcoursScroll);
    }

    @Override public void onBackPressed() {
        if (showingParcours) { showHome(); return; }
        super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        if (ledger == null) ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        TextView itqanCue = Ui.settingValue(itqanCard);
        if (itqanCue != null) itqanCue.setText(Ui.stabilizationCue(prefs.itqanPostNasMaintenance()));
        if (today != null && geometry != null) refreshAll();
    }

    private void refreshAll() { refreshQuickAccessCadenceGating(); refreshToday(); refreshRecentSabqiAdvisory(); refreshDashboard(); refreshStabilizationPosition(); }

    /** Option 7B: where the Stabilisation rotation stands, e.g. "Hizb 52 · 3/61 séances". */
    private void refreshStabilizationPosition() {
        TextView cue = Ui.settingValue(itqanCard);
        if (cue == null || geometry == null) return;
        boolean postNas = prefs.itqanPostNasMaintenance();
        String base = Ui.stabilizationCue(postNas);
        HifzPrefs.ItqanRotationProgress position = prefs.itqanRotationProgress(geometry);
        if (position == null) { cue.setText(base); return; }
        cue.setText(postNas
            ? "Hizb " + QuranRubBoundaries.hizbOf(position.start) + " · " + position.index + "/" + position.total + " séances"
            : base + " · " + position.index + "/" + position.total);
    }

    /**
     * Apprentissage/Stabilisation quick-access must respect the weekday-pinned cadence (Settings'
     * configurable Apprentissage-days-per-week split, Mon/Wed/Fri by default) — opening tomorrow's
     * Apprentissage today would let a learner get ahead of the weekly snowball attribution it's
     * built on. Révision/Renforcement/Consolidation stay free since they need to be
     * testable/catchable-up any day.
     */
    /** P4: the Roadmap's own automatic ratio for today, when active, otherwise the user's manual
     *  learningDaysPerWeek setting — see HifzPrefs.currentRoadmapDecision for exactly when the
     *  automatic ratio applies (Phase.CURRENT only, for now). */
    private int effectiveLearningDaysPerWeek() {
        RoadmapPolicy.Decision decision = geometry != null ? prefs.currentRoadmapDecision(geometry) : null;
        return decision != null ? decision.learningDays : prefs.learningDaysPerWeek();
    }

    private void refreshQuickAccessCadenceGating() {
        if (geometry == null) return;
        CadenceAction action = HifzSchedule.INSTANCE.actionFor(HifzClock.today().getDayOfWeek(), effectiveLearningDaysPerWeek());
        setLineEnabled(sabqiQuickAccess, action == CadenceAction.LEARNING);
        setLineEnabled(itqanQuickAccess, action == CadenceAction.STABILIZATION);
    }

    /** The Révision card opens this compact selector rather than jumping straight to a mode:
        Révision active and Entretien can now be done in any order. */
    private void showRevisionSelector() {
        RevisionSelector.show(this, prefs, geometry, speedStore, new RevisionSelector.Choice() {
            @Override public void openActiveRevision() { openMode(HifzSessionActivity.MURAJAAH_ACTIVE); }
            @Override public void openPassiveRevision() { openMode(HifzSessionActivity.MURAJAAH); }
        });
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

    /**
     * Reported directly: these two direct-access tiles always opened the non-graduating evening
     * snowball review (LEARNING_CONSOLIDATION/RECENT_SABQI_REVIEW), even on the Sunday the real
     * finale (LEARNING_FINAL/CONSOLIDATION_FINAL — the only thing that ever graduates material to
     * Acquis and unlocks it for Entretien) is due. A learner using these shortcuts instead of
     * "Aujourd'hui" could complete every review faithfully and still never graduate a single line,
     * with no error or warning — confirmed on a live export where recentSabqi/promotedRanges had
     * accumulated unpromoted material since day one. Mirror nextMode's own Sunday-due check so
     * these shortcuts reach the same finale "Aujourd'hui" would have sequenced them into.
     */
    private String renforcementQuickAccessMode(){
        LocalDate today=HifzClock.today();
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(today.getDayOfWeek(), effectiveLearningDaysPerWeek());
        return action==CadenceAction.REVISION && !learningFinalResolved(today)
            ? HifzSessionActivity.LEARNING_FINAL : HifzSessionActivity.LEARNING_CONSOLIDATION;
    }

    /** See renforcementQuickAccessMode: the Consolidation-track counterpart. */
    private String consolidationQuickAccessMode(){
        LocalDate today=HifzClock.today();
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(today.getDayOfWeek(), effectiveLearningDaysPerWeek());
        return action==CadenceAction.REVISION && !consolidationFinalResolved(today)
            ? HifzSessionActivity.CONSOLIDATION_FINAL : HifzSessionActivity.RECENT_SABQI_REVIEW;
    }

    /**
     * A past Sunday's weekly snowball final review can never be caught up later — by the time it
     * would be revisited, the accumulator has already rolled to a new week — so once a Sunday is
     * no longer today it is treated as satisfied rather than stalling every later cadence day.
     */
    private boolean cadenceComplete(LocalDate date){
        CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), effectiveLearningDaysPerWeek());
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
        return HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed,effectiveLearningDaysPerWeek());
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

    /** Dimanche soir, once the morning ×3 finales are done: Révision active, then ordinary Entretien, same as every other evening. */
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
        if(g==null){setToday("…",todayAction.isEnabled());return;}
        LocalDate current=HifzClock.today();
        if(current.isBefore(prefs.programStartDate())){
            setToday("Parcours non démarré",false);return;
        }
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention||learningConsolidationNeedsAttention){
            setToday("Consolidation · état à vérifier",true);
            return;
        }
        if(mode==null){setToday("Programme à jour",false);return;}
        String prefix=!isTodayAnchoredMode(mode)&&due!=null&&due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)){
                detail="Consolidation · boule de neige du soir";
            }else if(HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode)){
                detail="Renforcement · boule de neige du soir";
            }else if(HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode)){
                detail="Consolidation · révision finale ×3";
            }else if(HifzSessionActivity.LEARNING_FINAL.equals(mode)){
                detail="Renforcement · révision finale ×3";
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
                int maintenanceMinutes=MaintenanceCoveragePolicy.minutes(
                    prefs.acquiredLineCountV6(), g.lineCount(), speedStore.maintenanceSecondsPerLine());
                detail="Révision · "+maintenanceMinutes+" min";
            }else detail="Parcours à vérifier";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        setToday(prefix+detail,true);
    }

    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        ItqanRegimeStore.UnitPlan plan=prefs.itqanUnitPlan(entry);
        int reps=ItqanMaintenancePolicy.totalReps(plan.regime,plan.protocol);
        List<String> owned=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
        if(plan.regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE){
            java.util.LinkedHashSet<Integer> pages=new java.util.LinkedHashSet<>();
            for(GeometryRepository.LineMeta line:geometry.linesForExactIds(owned))pages.add(line.page);
            return "Stabilisation · "+shortRange(start,end)+" · "+pages.size()+" pages · ×"+reps+" entretien";
        }
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
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());
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
