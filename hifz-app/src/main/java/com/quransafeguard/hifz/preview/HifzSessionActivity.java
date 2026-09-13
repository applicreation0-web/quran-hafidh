package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Structured Leçon neuve / Ancrage / Entretien session using the independent domain engine. */
public final class HifzSessionActivity extends android.app.Activity implements MushafView.Listener {
    public static final String EXTRA_MODE = "mode";
    public static final String SABQI = "SABQI";
    public static final String SABQI_TODAY_REVIEW = "SABQI_TODAY_REVIEW";
    public static final String ITQAN = "ITQAN";
    public static final String RECENT_SABQI_REVIEW = "RECENT_SABQI_REVIEW";
    public static final String MURAJAAH = "MURAJAAH";

    private String mode;
    private HifzPrefs prefs;
    private HifzSpeedStore speedStore;
    private HifzSessionMetricsStore metricsStore;
    private GeometryRepository geometry;
    private MushafView mushaf;
    private TextView program, progress, timerText;
    private LinearLayout actions, audioHost;
    private HifzAudioDialog audioPlayer;
    private SessionClock clock;
    private final EinkController eink = new EinkController();
    private long lastCheckpointBucket = -1L;
    private int currentPage = 1;
    private List<VerseRef> currentSelection = Collections.emptyList();
    private List<String> currentLineIds = Collections.emptyList();
    private int currentMask = 0;
    private GeometryRepository.FiveLineBlock sabqiBlock;
    private GeometryRepository.VerseUnit itqanUnit;
    private AnchoringQueue.Entry anchoringEntry;
    private int itqanTargetReps = PreviewConfig.ITQAN_TOTAL_REPS;
    private AnchoringQueue.Protocol itqanSessionProtocol = AnchoringQueue.Protocol.FULL;
    private boolean fractionatedItqan;
    private int itqanBlockIndex;
    private int itqanBlockCount = 1;
    private GeometryRepository.EligibleLinePlan murajaahPlan;
    private VerseRef murajaahActualEnd;
    private boolean timedSessionLimitReached;
    private boolean repActionLocked;
    private boolean hasShown;
    private boolean sessionCompleted;
    private boolean awaitingValidation;
    private int unitFirstPage = 1;
    private int unitLastPage = 1;
    private boolean revealedThisRep;
    private Button revealButton;
    private Button murajaahFinishButton;
    private int recentReviewIndex;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !SABQI_TODAY_REVIEW.equals(mode) && !ITQAN.equals(mode)
                && !RECENT_SABQI_REVIEW.equals(mode) && !MURAJAAH.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        metricsStore = new HifzSessionMetricsStore(this);
        geometry = GeometryRepository.get(this);
        recentReviewIndex = prefs.recentSabqiReviewIndex();
        murajaahActualEnd = prefs.murajaahActualEnd();
        clock = new SessionClock(prefs.elapsedFor(mode), elapsed -> {
            if (timerText != null) {
                timerText.setText(SessionTimerPolicy.label(mode, elapsed, targetMinutes()));
            }
            long bucket = elapsed / 5_000L;
            if (bucket != lastCheckpointBucket) {
                lastCheckpointBucket = bucket;
                prefs.setElapsedFor(mode, elapsed);
                checkpointMurajaah(elapsed);
            }
            if (isTimedMode() && PreviewConfig.timedSessionComplete(elapsed, targetMinutes())) {
                onTimedSessionLimit(elapsed);
            }
        });
        buildUi();
        renderMode();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,4),0,Ui.dp(this,6),0);
        top.addView(Ui.iconButton(this,"","Retour",v->finish()));
        program = Ui.text(this,"Chargement…",12.5f,true);
        Ui.weight(program,1f);
        program.setGravity(Gravity.CENTER_VERTICAL);
        program.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),0);
        program.setMaxLines(1);
        top.addView(program);
        root.addView(top);

        LinearLayout meta = Ui.row(this);
        meta.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,2));
        progress = Ui.text(this,"",10.8f,false);
        progress.setTextColor(Ui.MUTED);
        progress.setMaxLines(1);
        Ui.weight(progress,1f);
        meta.addView(progress);
        timerText = Ui.text(this,"",10.5f,false);
        timerText.setTextColor(Ui.MUTED);
        timerText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        meta.addView(timerText);
        root.addView(meta);

        audioHost = Ui.column(this);
        audioHost.setPadding(0,0,0,0);
        audioHost.setVisibility(View.GONE);
        root.addView(audioHost, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mushaf = new MushafView(this);
        mushaf.setMaskEntropy(prefs.maskEntropyFor(mode));
        mushaf.setListener(this);
        root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout controlBar = Ui.row(this);
        controlBar.setGravity(Gravity.CENTER);
        controlBar.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,2));
        actions = Ui.row(this);
        actions.setGravity(Gravity.CENTER);
        controlBar.addView(actions);
        controlBar.addView(Ui.roundAction(this,"","Écouter",v->openAudio()));
        root.addView(controlBar);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void renderMode() {
        closeAudio();
        actions.removeAllViews();
        revealButton = null;
        murajaahFinishButton = null;
        awaitingValidation = false;
        unitFirstPage = 1;
        unitLastPage = 1;
        try {
            if (completeExpiredTimedSession()) { renderMode(); return; }
            if (SABQI.equals(mode)) renderSabqi();
            else if (SABQI_TODAY_REVIEW.equals(mode)) renderSabqiTodayReview();
            else if (ITQAN.equals(mode)) renderItqan();
            else if (RECENT_SABQI_REVIEW.equals(mode)) renderRecentSabqiReview();
            else renderMurajaah();
        } catch (RuntimeException error) {
            sessionCompleted = true;
            clock.pause();
            android.util.Log.e("QuranHifz", "Unable to render " + mode, error);
            program.setText(displayModeName() + " · état à vérifier");
            progress.setText("La séance ne peut pas être affichée. Ouvrez Diagnostic si nécessaire.");
        }
    }

    private void renderSabqi() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiDate())) {
            sessionCompleted = true;
            program.setText("Leçon neuve · séance validée");
            progress.setText(prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : prefs.lastSabqiLabel());
            return;
        }
        int startLimit = geometry.firstLineIndex(prefs.sabqiStart());
        int endLimit = geometry.lastLineIndex(prefs.sabqiEnd());
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) { cursor = startLimit; prefs.setSabqiLineCursor(cursor); }
        if (cursor < startLimit || cursor > endLimit) {
            sessionCompleted = true;
            program.setText("Leçon neuve · curseur à repositionner");
            progress.setText(prefs.sabqiStart() + " → " + prefs.sabqiEnd());
            return;
        }
        if (cursor + PreviewConfig.SABQI_LINES - 1 > endLimit) {
            sessionCompleted = true;
            int remaining = endLimit - cursor + 1;
            program.setText("Leçon neuve · fin de plage");
            progress.setText(remaining + " ligne(s) restante(s) · bloc requis : 5");
            return;
        }
        sabqiBlock = geometry.fiveLineBlock(cursor);
        currentPage = geometry.line(sabqiBlock.startLineIndex).page;
        unitFirstPage = currentPage;
        unitLastPage = geometry.line(sabqiBlock.endLineIndex).page;
        currentSelection = sabqiBlock.verses; currentLineIds = sabqiBlock.lineIds;
        int rep = prefs.sabqiRep();
        if (rep >= PreviewConfig.SABQI_TOTAL_REPS) {
            awaitingValidation = true;
            sessionCompleted = true;
            clock.pause();
            currentMask = 0;
            program.setText("Leçon neuve · " + sabqiBlock.verseLabel() + " · 5 lignes");
            progress.setText("37/37 · prêt à valider · révélations " + prefs.sabqiAssisted());
            showCurrent();
            addRoundAction("✓","Valider",v->validateSabqi());
            return;
        }
        sessionCompleted = false;
        currentMask = PreviewConfig.sabqiMaskForNextRep(rep);
        program.setText("Leçon neuve · " + sabqiBlock.verseLabel() + " · 5 lignes");
        updateSabqiProgress(rep, prefs.sabqiAssisted());
        showCurrent();
        addRoundAction("↻","Répétition",v->completeSabqiRep());
        LinearLayout revealAction = Ui.roundAction(this,"","Révéler",null);
        revealButton = (Button) revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    private void updateSabqiProgress(int rep, int reveals) {
        progress.setText(Math.min(rep+1,PreviewConfig.SABQI_TOTAL_REPS) + "/37 · masque " + currentMask + "% · révélations " + reveals);
        eink.local(progress);
    }

    private boolean takeRepLock() {
        if (repActionLocked) return false;
        repActionLocked = true;
        getWindow().getDecorView().postDelayed(() -> repActionLocked = false, 800L);
        return true;
    }

    private void completeSabqiRep() {
        if (!takeRepLock()) return;
        boolean revealed = consumeReveal();
        int rep=prefs.sabqiRep(); if(rep>=PreviewConfig.SABQI_TOTAL_REPS)return;
        int oldMask=currentMask;
        rep++; int reveals=prefs.sabqiAssisted()+(revealed?1:0);
        if(!prefs.setSabqiProgress(rep,reveals)){onError("Impossible d’enregistrer la répétition de la Leçon neuve.");return;}
        if(rep>=PreviewConfig.SABQI_TOTAL_REPS){
            currentMask=0;mushaf.setMask(0);
            long elapsed=clock.pause();prefs.setElapsedFor(mode,elapsed);
            awaitingValidation=true;sessionCompleted=true;
            renderMode();return;
        }
        currentMask=PreviewConfig.sabqiMaskForNextRep(rep);
        if(currentMask!=oldMask)mushaf.setMask(currentMask);
        updateRevealButton();
        if (currentPage != unitFirstPage) { currentPage = unitFirstPage; showCurrent(); }
        updateSabqiProgress(rep,reveals);
    }

    private void validateSabqi() {
        if (sabqiBlock==null || prefs.sabqiRep()<PreviewConfig.SABQI_TOTAL_REPS) return;
        String label="Leçon neuve · "+sabqiBlock.verseLabel()+" · 37/37 · révélations "+prefs.sabqiAssisted();
        boolean ok = prefs.completeSabqiBlock(
            sabqiBlock.startLineIndex,
            sabqiBlock.endLineIndex,
            sabqiBlock.endLineIndex+1,
            LocalDate.now().toString(),
            label
        );
        if (!ok) { onError("Impossible d’enregistrer la Leçon neuve."); return; }
        rebalanceRecentWindow();
        awaitingValidation=false;
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Calendar/attendance promotion; display order never determines mastery order. */
private void rebalanceRecentWindow() {
    List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
    if (recent.isEmpty()) return;
    List<HifzPrefs.RecentSabqi> canonical = HifzPrefs.canonicalRecentOrder(recent);
    LocalDate today = LocalDate.now();
    LocalDate activation = prefs.recentConsolidationActivatedOn();
    if (activation == null && recent.size() <= RecentPromotionPolicy.MAX_RECENT_BLOCKS) return;
    LocalDate plannedStart = activation == null ? today.plusDays(1) : activation;
    List<LocalDate> completed = activation == null
        ? Collections.emptyList()
        : prefs.consolidationAttendanceDates(plannedStart, today);

    int oldestStart = canonical.get(0).startLine;
    int safeEnd = -1;
    int safeBlocks = 0;
    boolean forceOldest = canonical.size() > RecentPromotionPolicy.MAX_RECENT_BLOCKS;
    for (int i = 0; i < canonical.size(); i++) {
        HifzPrefs.RecentSabqi item = canonical.get(i);
        if (i > 0 && item.startLine != canonical.get(i - 1).endLine + 1) break;
        RecentPromotionPolicy.Decision decision = RecentPromotionPolicy.evaluate(
            item.addedOn, today, plannedStart, completed, canonical.size(), i == 0);
        boolean include = decision.promote || (forceOldest && safeEnd < 0);
        if (!include) break;
        GeometryRepository.FiveLineBlock block = geometry.fiveLineBlock(item.startLine);
        if (!block.endsInsideVerse) {
            safeEnd = item.endLine;
            safeBlocks = i + 1;
            if (forceOldest) break;
        }
    }
    if (safeEnd < oldestStart || safeBlocks <= 0) return;

    ArrayList<VerseRef> complete = new ArrayList<>(geometry.versesFullyCoveredByLines(oldestStart, safeEnd));
    if (complete.isEmpty()) return;
    if (!prefs.addPromotedVerses(complete, forceOldest)) {
        onError("Impossible d’enregistrer la promotion de la Consolidation.");
        return;
    }
    List<HifzPrefs.RecentSabqi> promotedBlocks = new ArrayList<>(canonical.subList(0, safeBlocks));
    if (!prefs.removeRecentBlocks(promotedBlocks)) {
        onError("Impossible de retirer les blocs promus de la Consolidation.");
        return;
    }
    recentReviewIndex = prefs.recentSabqiReviewIndex();
}

    private RecentPromotionPolicy.Decision promotionStatus(HifzPrefs.RecentSabqi item, int blockCount, boolean oldest) {
        LocalDate today = LocalDate.now();
        LocalDate activation = prefs.recentConsolidationActivatedOn();
        LocalDate plannedStart = activation == null ? today.plusDays(1) : activation;
        List<LocalDate> completed = activation == null
            ? Collections.emptyList()
            : prefs.consolidationAttendanceDates(plannedStart, today);
        return RecentPromotionPolicy.evaluate(item.addedOn, today, plannedStart, completed, blockCount, oldest);
    }

    private void captureConsolidationAndRebalance() {
        DashboardLedger ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        rebalanceRecentWindow();
    }

    private void renderSabqiTodayReview() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Reprise du soir · séance validée");
            progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : prefs.lastSabqiTodayReviewLabel());
            return;
        }
        if (!today.equals(prefs.sabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Reprise du soir · aucun bloc");
            progress.setText("Validez d’abord les 5 lignes du matin.");
            return;
        }
        int start = prefs.sabqiTodayReviewStartLine();
        int end = prefs.sabqiTodayReviewEndLine();
        if (start < 0 || end < start) throw new IllegalStateException("Bloc de reprise absent");
        GeometryRepository.FiveLineBlock block = geometry.fiveLineBlock(start);
        currentPage = geometry.line(start).page;
        unitFirstPage = currentPage;
        unitLastPage = geometry.line(end).page;
        currentSelection = block.verses;
        currentLineIds = block.lineIds;
        currentMask = 0;
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        program.setText("Reprise du soir · " + block.verseLabel() + " · 5 lignes");
        progress.setText(timedSessionLimitReached ? "30 min atteintes" : "Répétez les mêmes 5 lignes.");
        showCurrent();
        if (!timedSessionLimitReached) addRoundAction("↻", "Répétition", v -> renderMode());
    }

    private void renderRecentSabqiReview() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) {
            sessionCompleted = true;
            program.setText("Consolidation · séance validée");
            progress.setText(prefs.lastRecentSabqiReviewLabel().isEmpty() ? "30 min terminées" : prefs.lastRecentSabqiReviewLabel());
            return;
        }
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) {
            if (completeEmptyRecentSabqiSession()) { renderMode(); return; }
            sessionCompleted = true;
            program.setText("Consolidation · aucun passage");
            progress.setText("Aucun bloc récent à consolider.");
            return;
        }
        recentReviewIndex = Math.floorMod(recentReviewIndex, recent.size());
        HifzPrefs.RecentSabqi item = recent.get(recentReviewIndex);
        GeometryRepository.FiveLineBlock block = geometry.fiveLineBlock(item.startLine);
        currentPage = geometry.line(item.startLine).page;
        unitFirstPage = currentPage;
        unitLastPage = geometry.line(item.endLine).page;
        currentSelection = block.verses;
        currentLineIds = block.lineIds;
        currentMask = 0;
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        program.setText("Consolidation · " + block.verseLabel());
        List<HifzPrefs.RecentSabqi> canonicalRecent = HifzPrefs.canonicalRecentOrder(recent);
        boolean canonicalOldest = !canonicalRecent.isEmpty()
            && canonicalRecent.get(0).startLine == item.startLine
            && canonicalRecent.get(0).endLine == item.endLine;
        RecentPromotionPolicy.Decision status = promotionStatus(item, recent.size(), canonicalOldest);
        String attendance = status.requiredSessions <= 0
            ? "Consolidation à venir"
            : status.completedSessions + "/" + status.requiredSessions + " séances";
        progress.setText("Bloc " + (recentReviewIndex + 1) + "/" + recent.size()
            + " · " + status.ageDays + " j · " + attendance
            + (status.forced ? " · promotion de sécurité" : ""));
        showCurrent();
        if (!timedSessionLimitReached) {
            addRoundAction("✓", "Revu", v -> markRecentReviewed());
            addRoundAction("!", "À renforcer", v -> deferRecentReview());
        }
    }

    private void markRecentReviewed() {
        if (!takeRepLock()) return;
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) { renderMode(); return; }
        int index = Math.floorMod(recentReviewIndex, recent.size());
        HifzPrefs.RecentSabqi item = recent.get(index);
        if (!prefs.markRecentReviewed(index)) {
            onError("Impossible d’enregistrer ce bloc comme revu.");
            return;
        }
        metricsStore.addConsolidationLines(Math.max(0, item.endLine - item.startLine + 1));
        recentReviewIndex = prefs.recentSabqiReviewIndex();
        renderMode();
    }

    private void deferRecentReview() {
        if (!takeRepLock()) return;
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) { renderMode(); return; }
        int index = Math.floorMod(recentReviewIndex, recent.size());
        HifzPrefs.RecentSabqi item = recent.get(index);
        if (!prefs.deferRecentBlock(index)) {
            onError("Impossible de reporter ce bloc à renforcer.");
            return;
        }
        metricsStore.addConsolidationLines(Math.max(0, item.endLine - item.startLine + 1));
        recentReviewIndex = prefs.recentSabqiReviewIndex();
        renderMode();
    }

    /** Commit a timed session whose foreground clock reached its limit before process death. */
    private boolean completeExpiredTimedSession() {
        if (!isTimedMode() || !PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes())) return false;
        String today = LocalDate.now().toString();
        if (SABQI_TODAY_REVIEW.equals(mode)
                && !today.equals(prefs.lastSabqiTodayReviewDate())
                && today.equals(prefs.sabqiTodayReviewDate())) {
            timedSessionLimitReached = true;
            prefs.completeSabqiTodayReview(today, "Reprise du soir · 30 min");
            closeClockForCompletedSession();
            return true;
        }
        if (RECENT_SABQI_REVIEW.equals(mode)
                && !today.equals(prefs.lastRecentSabqiReviewDate())) {
            timedSessionLimitReached = true;
            if (!completeConsolidation(today, clock.elapsedMs())) return false;
            closeClockForCompletedSession();
            return true;
        }
        return false;
    }

    private boolean completeConsolidation(String today, long elapsedMs) {
        int lines = metricsStore.consolidationLines();
        SpeedCalibrationPolicy.Result calibration = speedStore.calibrateConsolidation(lines, elapsedMs);
        String raw = HifzSpeedStore.instrumentationLabel(lines, elapsedMs, calibration);
        if (calibration.status == SpeedCalibrationPolicy.Status.ATYPICAL) raw += "·atyp";
        String label = "Consolidation · " + targetMinutes() + " min · " + raw;
        if (!prefs.completeRecentSabqiReview(today, recentReviewIndex, label)) {
            onError("Impossible d’enregistrer la Consolidation.");
            return false;
        }
        metricsStore.clearConsolidation();
        captureConsolidationAndRebalance();
        return true;
    }

    /** Empty recent work is completed explicitly and never falls through to old Ancrage. */
    private boolean completeEmptyRecentSabqiSession() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) return false;
        metricsStore.clearConsolidation();
        boolean ok = prefs.completeRecentSabqiReview(today, 0, "Consolidation · aucun bloc récent");
        if (ok) {
            captureConsolidationAndRebalance();
            closeClockForCompletedSession();
        }
        return ok;
    }

    private void onTimedSessionLimit(long elapsed) {
        if (timedSessionLimitReached) return;
        timedSessionLimitReached = true;
        long saved = clock.pause();
        long effectiveElapsed = Math.max(elapsed, saved);
        prefs.setElapsedFor(mode, effectiveElapsed);
        String today = LocalDate.now().toString();
        if (SABQI_TODAY_REVIEW.equals(mode)) {
            prefs.completeSabqiTodayReview(today, "Reprise du soir · 30 min");
            closeClockForCompletedSession();
            renderMode();
        } else if (RECENT_SABQI_REVIEW.equals(mode)) {
            if (!completeConsolidation(today, effectiveElapsed)) return;
            closeClockForCompletedSession();
            renderMode();
        } else if (MURAJAAH.equals(mode)) {
            progress.setText(murajaahActualEnd == null
                ? "Durée atteinte · touchez le dernier verset."
                : "Fin réelle · " + murajaahActualEnd);
            eink.local(progress);
            if (murajaahFinishButton != null) murajaahFinishButton.setEnabled(murajaahActualEnd != null);
        }
    }

    private void renderItqan() {
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastItqanDate())){
            sessionCompleted = true;
            if (prefs.itqanBlockIndex() > 0) {
                program.setText("Ancrage fractionné · séance terminée");
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Sous-bloc terminé":prefs.lastItqanLabel());
            } else {
                program.setText("Ancrage · unité validée");
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Ancrage terminé":prefs.lastItqanLabel());
            }
            return;
        }
        anchoringEntry = prefs.currentAnchoringEntry(geometry);
        if (anchoringEntry == null) {
            sessionCompleted = true;
            clock.pause();
            if (prefs.anchoringDeferredToday()) {
                program.setText("Ancrage · page reportée");
                progress.setText("Cette page reviendra à la prochaine séance d’Ancrage.");
            } else {
                program.setText("Ancrage · aucune page en attente");
                progress.setText("Toutes les pages en attente sont acquises.");
            }
            return;
        }
        int rep=prefs.itqanRep();
        VerseRef savedStart=prefs.itqanUnitStart();
        VerseRef savedEnd=prefs.itqanUnitEnd();
        if(rep>0 && savedStart!=null && savedEnd!=null){
            currentPage=geometry.pageForVerse(savedStart);
            unitFirstPage = unitLastPage = currentPage;
            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=geometry.lineIdsForVerseRange(savedStart,savedEnd);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
        } else {
            VerseRef entryStart = GeometryRepository.parseVerse(anchoringEntry.start);
            VerseRef entryEnd = GeometryRepository.parseVerse(anchoringEntry.end);
            int page = geometry.pageForVerse(entryStart);
            itqanUnit = new GeometryRepository.VerseUnit(page, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd),
                geometry.lineIdsForVerseRange(entryStart, entryEnd));
            currentPage=itqanUnit.page;unitFirstPage=unitLastPage=currentPage;
        }

        fractionatedItqan = prefs.isFractionatedUnit(itqanUnit.verses);
        itqanSessionProtocol = fractionatedItqan ? AnchoringQueue.Protocol.LIGHT : anchoringEntry.protocol;
        itqanTargetReps = PreviewConfig.itqanTotalReps(itqanSessionProtocol);
        if (fractionatedItqan) {
            int n = itqanUnit.lineIds.size();
            itqanBlockCount = PreviewConfig.fractionatedBlockCount(n);
            itqanBlockIndex = Math.max(0, Math.min(prefs.itqanBlockIndex(), itqanBlockCount - 1));
            int from = PreviewConfig.fractionatedBlockStart(n, itqanBlockIndex);
            int len = PreviewConfig.fractionatedBlockLength(n, itqanBlockIndex);
            currentLineIds = new ArrayList<>(itqanUnit.lineIds.subList(from, from + len));
            currentSelection = geometry.versesOnLines(currentLineIds, itqanUnit.verses);
        } else {
            itqanBlockCount = 1;
            itqanBlockIndex = 0;
            currentSelection=itqanUnit.verses;currentLineIds=itqanUnit.lineIds;
        }

        if(rep>=itqanTargetReps){
            awaitingValidation=true;sessionCompleted=true;clock.pause();currentMask=0;
            program.setText(itqanProgramLabel());
            progress.setText(itqanTargetReps+"/"+itqanTargetReps+" · prêt à valider · révélations finales "+prefs.itqanFinalReveals());
            showCurrent();addRoundAction("✓","Valider",v->validateItqan());return;
        }
        sessionCompleted=false;
        currentMask=PreviewConfig.itqanMaskForNextRep(rep, itqanSessionProtocol);
        program.setText(itqanProgramLabel());
        updateItqanProgress(rep,prefs.itqanAssisted());showCurrent();
        addRoundAction("↻","Répétition",v->completeItqanRep());
        LinearLayout revealAction = Ui.roundAction(this,"","Révéler",null);
        revealButton=(Button)revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    private String itqanProgramLabel() {
        if (fractionatedItqan) {
            VerseRef start = currentSelection.isEmpty() ? itqanUnit.start : currentSelection.get(0);
            VerseRef end = currentSelection.isEmpty() ? itqanUnit.end : currentSelection.get(currentSelection.size() - 1);
            return "Ancrage fractionné · "+start+" → "+end+" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · ×"+itqanTargetReps;
        }
        return "Ancrage · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+itqanTargetReps
            +(anchoringEntry.origin==AnchoringQueue.Origin.FORCED_PROMOTION?" · promotion de sécurité":"");
    }

    private void updateItqanProgress(int rep,int reveals){
        progress.setText(Math.min(rep+1,itqanTargetReps)+"/"+itqanTargetReps+" · masque "+currentMask+"% · révélations "+reveals);
        eink.local(progress);
    }

    private void completeItqanRep(){
        if (!takeRepLock()) return;
        boolean revealed=consumeReveal();
        int rep=prefs.itqanRep();if(rep>=itqanTargetReps)return;
        int oldMask=currentMask;rep++;int reveals=prefs.itqanAssisted()+(revealed?1:0);
        int finalReveals = prefs.itqanFinalReveals()
            + (revealed && PreviewConfig.isItqanValidationRep(rep - 1, itqanSessionProtocol) ? 1 : 0);
        if(!prefs.setItqanProgress(rep,reveals,finalReveals,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition d’Ancrage.");return;}
        if(rep>=itqanTargetReps){
            currentMask=0;mushaf.setMask(0);
            long elapsed=clock.pause();prefs.setElapsedFor(mode,elapsed);
            awaitingValidation=true;sessionCompleted=true;renderMode();return;
        }
        currentMask=PreviewConfig.itqanMaskForNextRep(rep, itqanSessionProtocol);if(currentMask!=oldMask)mushaf.setMask(currentMask);
        updateRevealButton();if(currentPage!=unitFirstPage){currentPage=unitFirstPage;showCurrent();}
        updateItqanProgress(rep,reveals);
    }

    private String anchoringInstrumentation() {
        if (itqanUnit == null) return "0L/0s/0r";
        int lines = fractionatedItqan ? currentLineIds.size()
            : geometry.lineCountForVerseRange(itqanUnit.start, itqanUnit.end);
        long elapsed = Math.max(clock.elapsedMs(), prefs.elapsedFor(mode));
        return Math.max(0, lines) + "L/" + Math.max(0L, elapsed / 1000L) + "s/" + prefs.itqanRep() + "r";
    }

    private void validateItqan(){
        if(itqanUnit==null||anchoringEntry==null||prefs.itqanRep()<itqanTargetReps)return;
        String metrics = anchoringInstrumentation();
        if (fractionatedItqan) {
            int nextBlock = itqanBlockIndex + 1;
            if (nextBlock < itqanBlockCount) {
                String label="Ancrage fractionné · bloc "+(itqanBlockIndex+1)+"/"+itqanBlockCount
                    +" validé · révélations "+prefs.itqanAssisted()+" · "+metrics;
                metricsStore.recordAnchoring("bloc fractionné réussi · "+itqanUnit.start+" → "+itqanUnit.end
                    +" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · "+metrics);
                if (!prefs.advanceItqanBlock(nextBlock, LocalDate.now().toString(), label)) {
                    onError("Impossible d’enregistrer le sous-bloc d’Ancrage fractionné.");
                    return;
                }
                awaitingValidation=false;
                closeClockForCompletedSession();
                mushaf.cycleCompleted();
                renderMode();
                return;
            }
        } else if (!PreviewConfig.itqanValidationPassed(prefs.itqanFinalReveals())) {
            metricsStore.recordAnchoring("échec · "+itqanUnit.start+" → "+itqanUnit.end+" · "+metrics);
            if (!prefs.failAndDeferAnchoring(itqanUnit.start, itqanUnit.end)) {
                onError("Impossible d’enregistrer le report de cette page d’Ancrage.");
                return;
            }
            awaitingValidation=false;
            sessionCompleted=false;
            restartAnchoringClockAfterDeferral();
            mushaf.cycleCompleted();
            renderMode();
            return;
        }
        EligibleCorpus corpus = prefs.itqanWorkCorpus();
        VerseRef next = corpus.nextAnchored(itqanUnit.end, prefs.itqanRotationStart());
        String label=(fractionatedItqan ? "Ancrage fractionné" : "Ancrage")+" · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+itqanTargetReps
            +" · révélations finales "+prefs.itqanFinalReveals()+" · "+metrics;
        metricsStore.recordAnchoring((fractionatedItqan ? "réussite fractionnée" : "réussite")+" · "+itqanUnit.start+" → "+itqanUnit.end+" · "+metrics);
        boolean ok=prefs.completeItqanUnitAndConsolidate(itqanUnit.start,itqanUnit.end,next,LocalDate.now().toString(),label);
        if(!ok){onError("Impossible d’enregistrer la validation de l’Ancrage.");return;}
        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void renderMurajaah(){
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastMurajaahDate())) {
            sessionCompleted = true;
            program.setText("Entretien · séance validée");
            progress.setText(prefs.lastMurajaahLabel().isEmpty() ? "Curseur sauvegardé" : prefs.lastMurajaahLabel());
            return;
        }
        if (!prefs.isMurajaahCursorValid()) {
            sessionCompleted = true;
            program.setText("Entretien · curseur à vérifier");
            progress.setText("Le corpus acquis ne contient pas ce curseur.");
            return;
        }
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.murajaahCorpus();
        int lines = HifzCadence.targetLines(targetMinutes(), speedStore.maintenanceSecondsPerLine());
        murajaahPlan = geometry.planEligibleLines(prefs.murajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.murajaahActualEnd();
        currentPage = geometry.pageForVerse(murajaahPlan.start);
        currentSelection = murajaahPlan.traversalVerses;
        currentLineIds = Collections.emptyList();
        currentMask = 0;
        program.setText("Entretien · " + murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd);
        progress.setText(timedSessionLimitReached
            ? (murajaahActualEnd == null ? "Durée atteinte · touchez le dernier verset." : "Fin réelle · " + murajaahActualEnd)
            : "Corpus acquis · " + targetMinutes() + " min");
        showCurrent();
        if (murajaahActualEnd != null && murajaahPlan.traversalVerses.contains(murajaahActualEnd)) {
            mushaf.setSelection(Collections.singletonList(murajaahActualEnd),
                geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd));
        } else if (murajaahActualEnd != null) {
            murajaahActualEnd = null;
            prefs.setMurajaahActualEnd(null);
        }
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(timedSessionLimitReached && murajaahActualEnd != null);
        actions.addView(validateAction);
    }

    private void finishMurajaah(){
        if (!timedSessionLimitReached) {
            Toast.makeText(this, "La durée prévue n’est pas encore atteinte.", Toast.LENGTH_LONG).show();
            return;
        }
        if (murajaahActualEnd == null) {
            Toast.makeText(this, "Touchez d’abord le dernier verset réellement révisé.", Toast.LENGTH_LONG).show();
            return;
        }
        EligibleCorpus corpus = prefs.murajaahCorpus();
        VerseRef next = corpus.next(murajaahActualEnd);
        long elapsed = clock.elapsedMs();
        int lines = countMurajaahLinesThrough(murajaahActualEnd);
        SpeedCalibrationPolicy.Result calibration = speedStore.calibrateMaintenance(lines, elapsed);
        String raw = HifzSpeedStore.instrumentationLabel(lines, elapsed, calibration);
        if (calibration.status == SpeedCalibrationPolicy.Status.ATYPICAL) raw += "·atyp";
        String label = "Entretien · réel : " + murajaahPlan.start + " → " + murajaahActualEnd
            + " · prochain curseur " + next + " · " + raw;
        boolean ok = prefs.completeMurajaah(next, LocalDate.now().toString(), label);
        if (!ok) { onError("Impossible d’enregistrer la validation de l’Entretien."); return; }
        closeClockForCompletedSession();
        renderMode();
    }

    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        if (MurajaahTraversalPolicy.endpointAmbiguous(murajaahPlan.traversalVerses, through)) return 0;
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (VerseRef verse : murajaahPlan.traversalVerses) {
            ids.addAll(geometry.lineIdsForVerseRange(verse, verse));
            if (verse.equals(through)) return ids.size();
        }
        throw new IllegalArgumentException("Fin d’Entretien hors du parcours planifié : " + through);
    }

    private void checkpointMurajaah(long elapsed){
        if (!MURAJAAH.equals(mode) || sessionCompleted) return;
        prefs.setMurajaahActualEnd(murajaahActualEnd);
    }

    @Override public void onVerseTap(VerseRef verse){
        if(MURAJAAH.equals(mode)&&murajaahPlan!=null&&murajaahPlan.traversalVerses.contains(verse)){
            murajaahActualEnd=verse;
            progress.setText("Fin réelle · "+verse);
            eink.local(progress);
            mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
            checkpointMurajaah(clock.elapsedMs());
            if (murajaahFinishButton != null) murajaahFinishButton.setEnabled(timedSessionLimitReached);
        }
    }

    @Override public void onPageSwipe(int delta){goPage(delta);}
    private void showCurrent(){hasShown=true;mushaf.show(currentPage,currentSelection,currentLineIds,currentMask);}

    private void goPage(int delta) {
        int target=Math.max(1,Math.min(604,currentPage+delta));
        boolean limited=SABQI.equals(mode)||SABQI_TODAY_REVIEW.equals(mode)||ITQAN.equals(mode)||RECENT_SABQI_REVIEW.equals(mode);
        if(limited)target=Math.max(unitFirstPage,Math.min(unitLastPage,target));
        if(target==currentPage)return;closeAudio();currentPage=target;showCurrent();
    }

    private void addRoundAction(String symbol,String label,View.OnClickListener listener){
        LinearLayout box=Ui.roundAction(this,symbol,label,listener);actions.addView(box);
    }

    private void configureRevealButton(Button button) {
        button.setOnTouchListener((view,event)->{
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN){
                if(currentMask<=0)return false;revealedThisRep=true;view.setPressed(true);mushaf.setMask(0);return true;
            }
            if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL){
                view.setPressed(false);mushaf.setMask(currentMask);if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;
            }
            return false;
        });
    }

    private boolean consumeReveal(){boolean revealed=revealedThisRep;revealedThisRep=false;return revealed;}
    private void updateRevealButton(){if(revealButton!=null)revealButton.setEnabled(currentMask>0);}

    private void openAudio(){
        HifzAudioGate gate=new HifzAudioGate(this);
        if(!gate.installed()){
            Toast.makeText(this,"Installez le pack audio dans Paramètres.",Toast.LENGTH_LONG).show();
            startActivity(new Intent(this,SettingsActivity.class));
            return;
        }
        closeAudio();
        audioPlayer=new HifzAudioDialog(this,mushaf,currentSelection);
        audioPlayer.attachInline(audioHost);
    }

    private void closeAudio(){
        HifzAudioDialog current=audioPlayer;
        audioPlayer=null;
        if(current!=null)current.detachInline();
    }

    private void restartAnchoringClockAfterDeferral(){
        lastCheckpointBucket=-1L;
        clock.reset();
        prefs.setElapsedFor(mode,0L);
        clock.resume();
    }

    private void closeClockForCompletedSession(){
        sessionCompleted=true;awaitingValidation=false;clock.pause();clock.reset();prefs.setElapsedFor(mode,0L);prefs.clearMaskEntropy(mode);
        if (RECENT_SABQI_REVIEW.equals(mode)) metricsStore.clearConsolidation();
    }
    private boolean isTimedMode() {
        return SABQI_TODAY_REVIEW.equals(mode) || RECENT_SABQI_REVIEW.equals(mode) || MURAJAAH.equals(mode);
    }
    private int targetMinutes(){
    SessionKind kind;
    if (SABQI.equals(mode)) kind = SessionKind.SABQI_NEW;
    else if (SABQI_TODAY_REVIEW.equals(mode)) kind = SessionKind.SABQI_TODAY_REVIEW;
    else if (ITQAN.equals(mode)) kind = SessionKind.ITQAN;
    else if (RECENT_SABQI_REVIEW.equals(mode)) kind = SessionKind.RECENT_SABQI_REVIEW;
    else kind = SessionKind.OLD_ITQAN_MURAJAAH;
    return HifzSchedule.INSTANCE.targetMinutesFor(kind);
}
    private String displayModeName(){
        if (SABQI.equals(mode)) return "Leçon neuve";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Reprise du soir";
        if (ITQAN.equals(mode)) return "Ancrage";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Consolidation";
        return "Entretien";
    }
    @Override public void onReady(){if(!hasShown)showCurrent();}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){currentPage=page;}
    @Override protected void onResume(){
        super.onResume();
        if(clock!=null)clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&!timedSessionLimitReached)clock.resume();
    }
    @Override protected void onPause(){
        long elapsed=clock.pause();
        if(sessionCompleted&&!awaitingValidation)prefs.setElapsedFor(mode,0L);
        else{prefs.setElapsedFor(mode,elapsed);checkpointMurajaah(elapsed);}
        super.onPause();
    }
    @Override protected void onDestroy(){closeAudio();if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
