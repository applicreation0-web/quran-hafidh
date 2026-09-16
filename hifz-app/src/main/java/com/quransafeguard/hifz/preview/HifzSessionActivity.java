package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

/** Structured Apprentissage / Stabilisation / Révision session using the independent domain engine. */
public final class HifzSessionActivity extends android.app.Activity implements MushafView.Listener {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SCHEDULED_DATE = "scheduled_date";
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
    private LocalDate sessionDate;
    private final ConsolidationCycleEngine consolidationEngine = new ConsolidationCycleEngine();
    private ConsolidationCycleEngine.Session consolidationSession;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !SABQI_TODAY_REVIEW.equals(mode) && !ITQAN.equals(mode)
                && !RECENT_SABQI_REVIEW.equals(mode) && !MURAJAAH.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        String recordedSessionDate = SABQI_TODAY_REVIEW.equals(mode) && prefs.elapsedFor(mode) > 0L
            ? prefs.sabqiTodayReviewDate()
            : "";
        String scheduledSessionDate = getIntent().getStringExtra(EXTRA_SCHEDULED_DATE);
        String requestedSessionDate = recordedSessionDate != null && !recordedSessionDate.isEmpty()
            ? recordedSessionDate
            : scheduledSessionDate;
        LocalDate capturedToday = HifzClock.today();
        try {
            sessionDate = requestedSessionDate == null || requestedSessionDate.isEmpty()
                ? capturedToday : LocalDate.parse(requestedSessionDate);
        } catch (RuntimeException invalidRecordedDate) {
            sessionDate = capturedToday;
        }
        speedStore = new HifzSpeedStore(this);
        metricsStore = new HifzSessionMetricsStore(this);
        try {
            geometry = GeometryRepository.get(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }
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
        program.setMaxLines(2);
        top.addView(program);
        root.addView(top);

        LinearLayout meta = Ui.row(this);
        meta.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,2));
        progress = Ui.text(this,"",10.8f,false);
        progress.setTextColor(Ui.MUTED);
        progress.setMaxLines(2);
        progress.setEllipsize(TextUtils.TruncateAt.END);
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
            else if (RECENT_SABQI_REVIEW.equals(mode)) renderConsolidationCycle();
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
        String today = sessionDate.toString();
        if (today.equals(prefs.lastSabqiDate())) {
            sessionCompleted = true;
            program.setText("Apprentissage · séance validée");
            progress.setText(prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : prefs.lastSabqiLabel());
            return;
        }
        int startLimit = geometry.firstLineIndex(prefs.sabqiStart());
        int endLimit = geometry.lastLineIndex(prefs.sabqiEnd());
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) { cursor = startLimit; prefs.setSabqiLineCursor(cursor); }
        if (cursor < startLimit || cursor > endLimit) {
            sessionCompleted = true;
            program.setText("Apprentissage · curseur à repositionner");
            progress.setText(prefs.sabqiStart() + " → " + prefs.sabqiEnd());
            return;
        }
        if (cursor + PreviewConfig.SABQI_LINES - 1 > endLimit) {
            sessionCompleted = true;
            int remaining = endLimit - cursor + 1;
            program.setText("Apprentissage · fin de plage");
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
            boolean assistancePassed = StructuredSessionPolicy.assistancePasses(prefs.sabqiAssisted());
            awaitingValidation = assistancePassed;
            sessionCompleted = true;
            clock.pause();
            currentMask = 0;
            program.setText("Apprentissage · " + sabqiBlock.verseLabel() + " · 5 lignes");
            progress.setText(assistancePassed
                ? "37/37 · prêt à valider · révélations " + prefs.sabqiAssisted()
                : "37/37 · à renforcer · révélations " + prefs.sabqiAssisted() + " · maximum 2");
            showCurrent();
            if (assistancePassed) addRoundAction("✓","Valider",v->validateSabqi());
            else addRoundAction("↻","Reprendre",v->restartSabqiAfterAssistance());
            return;
        }
        sessionCompleted = false;
        currentMask = PreviewConfig.sabqiMaskForNextRep(rep);
        program.setText("Apprentissage · " + sabqiBlock.verseLabel() + " · 5 lignes");
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
        eink.local(progress, prefs);
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
        if(!prefs.setSabqiProgress(rep,reveals)){onError("Impossible d’enregistrer la répétition de la Apprentissage.");return;}
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
        if (!StructuredSessionPolicy.assistancePasses(prefs.sabqiAssisted())) {
            restartSabqiAfterAssistance();
            return;
        }
        String label="Apprentissage · "+sabqiBlock.verseLabel()+" · 37/37 · révélations "+prefs.sabqiAssisted();
        boolean ok = prefs.completeSabqiBlockV6(
            sabqiBlock.startLineIndex,
            sabqiBlock.endLineIndex,
            sabqiBlock.endLineIndex+1,
            sabqiBlock.lineIds,
            sessionDate.toString(),
            label
        );
        if (!ok) { onError("Impossible d’enregistrer la Apprentissage."); return; }
        rebalanceRecentWindow(sessionDate);
        awaitingValidation=false;
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    private void restartSabqiAfterAssistance() {
        if (!prefs.setSabqiProgress(0, 0)) {
            onError("Impossible de relancer ce bloc de Apprentissage.");
            return;
        }
        lastCheckpointBucket = -1L;
        clock.reset();
        prefs.setElapsedFor(mode, 0L);
        awaitingValidation = false;
        sessionCompleted = false;
        clock.resume();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Calendar/attendance promotion; display order never determines mastery order. */
private void rebalanceRecentWindow(LocalDate today) {
    List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
    if (recent.isEmpty()) return;
    List<HifzPrefs.RecentSabqi> canonical = HifzPrefs.canonicalRecentOrder(recent);
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

    private RecentPromotionPolicy.Decision promotionStatus(HifzPrefs.RecentSabqi item, int blockCount, boolean oldest, LocalDate today) {
        LocalDate activation = prefs.recentConsolidationActivatedOn();
        LocalDate plannedStart = activation == null ? today.plusDays(1) : activation;
        List<LocalDate> completed = activation == null
            ? Collections.emptyList()
            : prefs.consolidationAttendanceDates(plannedStart, today);
        return RecentPromotionPolicy.evaluate(item.addedOn, today, plannedStart, completed, blockCount, oldest);
    }

    private void captureConsolidationAndRebalance() {
        DashboardLedger ledger = new DashboardLedger(this);
        ledger.capture(prefs, sessionDate);
        rebalanceRecentWindow(sessionDate);
    }

    private void renderSabqiTodayReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastSabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Apprentissage · séance validée");
            progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : prefs.lastSabqiTodayReviewLabel());
            return;
        }
        if (!today.equals(prefs.sabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Apprentissage · aucun bloc");
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
        program.setText("Apprentissage · " + block.verseLabel() + " · 5 lignes");
        progress.setText(timedSessionLimitReached ? "30 min atteintes" : "Répétez les mêmes 5 lignes.");
        showCurrent();
        if (!timedSessionLimitReached) addRoundAction("↻", "Répétition", v -> renderMode());
    }

    private static String consolidationUnitId(AnchoringQueue.Entry entry) {
        return entry.start + "|" + entry.end;
    }

    private static ConsolidationCycleEngine.Protocol consolidationProtocol(AnchoringQueue.Entry entry) {
        return entry.protocol == AnchoringQueue.Protocol.LIGHT
            ? ConsolidationCycleEngine.Protocol.LIGHT : ConsolidationCycleEngine.Protocol.FULL;
    }

    private AnchoringQueue.Entry consolidationEntry(String unitId) {
        String[] bounds = unitId.split("\\|", -1);
        if (bounds.length != 2) throw new IllegalStateException("Unité de Consolidation invalide : " + unitId);
        AnchoringQueue.Entry entry = prefs.anchoringEntryFor(
            GeometryRepository.parseVerse(bounds[0]), GeometryRepository.parseVerse(bounds[1]));
        if (entry == null) throw new IllegalStateException("Unité de Consolidation absente : " + unitId);
        return entry;
    }

    private void renderConsolidationCycle() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) {
            sessionCompleted = true;
            program.setText("Consolidation · séance validée");
            progress.setText(prefs.lastRecentSabqiReviewLabel().isEmpty() ? "Acquis" : prefs.lastRecentSabqiReviewLabel());
            return;
        }
        consolidationSession = prefs.restoreConsolidationSession(
            consolidationEngine, ConsolidationCycleEngine.Family.STABILIZATION);
        if (consolidationSession == null) {
            List<AnchoringQueue.Entry> entries = prefs.stabilizedAnchoringEntries(geometry, 3);
            if (entries.isEmpty()) {
                sessionCompleted = true;
                program.setText("Consolidation · rien à consolider");
                progress.setText("Aucune unité Stabilisée en attente.");
                return;
            }
            ConsolidationCycleEngine.Cycle cycle = null;
            for (int i = 0; i < entries.size(); i++) {
                AnchoringQueue.Entry entry = entries.get(i);
                ConsolidationCycleEngine.Unit unit = new ConsolidationCycleEngine.Unit(
                    consolidationUnitId(entry), consolidationProtocol(entry));
                cycle = i == 0
                    ? consolidationEngine.startCycle("stabilization-" + today, ConsolidationCycleEngine.Family.STABILIZATION, unit)
                    : consolidationEngine.addUnit(cycle, unit);
            }
            consolidationSession = consolidationEngine.openSession(cycle, "session-" + today);
            if (!prefs.persistConsolidationSession(consolidationSession))
                throw new IllegalStateException("Impossible d’enregistrer la Consolidation ouverte");
        }
        if (consolidationSession.readyToClose()) {
            sessionCompleted = true;
            program.setText("Consolidation · prête à valider");
            progress.setText("Toutes les répétitions du groupe sont terminées.");
            addRoundAction("✓", "Valider", v -> validateConsolidationCycle());
            return;
        }
        int position = consolidationSession.nextUnitIndex();
        AnchoringQueue.Entry entry = consolidationEntry(consolidationSession.unitIds().get(position));
        VerseRef start = GeometryRepository.parseVerse(entry.start);
        VerseRef end = GeometryRepository.parseVerse(entry.end);
        currentPage = geometry.pageForVerse(start);
        unitFirstPage = unitLastPage = currentPage;
        currentSelection = geometry.versesForRange(start, end);
        currentLineIds = geometry.lineIdsForVerseRange(start, end);
        currentMask = 0;
        sessionCompleted = false;
        int[] vector = consolidationSession.stageVectorAt(position);
        int target = vector[consolidationSession.stage()];
        program.setText("Consolidation · " + start + " → " + end + " · unité "
            + (position + 1) + "/" + consolidationSession.sessionGroupSize());
        progress.setText("Étape " + (consolidationSession.stage() + 1) + "/5 · "
            + consolidationSession.donePerStage() + "/" + target);
        showCurrent();
        addRoundAction("↻", "Répétition", v -> completeConsolidationRep());
    }

    private void completeConsolidationRep() {
        if (!takeRepLock() || consolidationSession == null || consolidationSession.readyToClose()) return;
        consolidationSession = consolidationEngine.recordRepetition(consolidationSession);
        if (!prefs.persistConsolidationSession(consolidationSession)) {
            onError("Impossible d’enregistrer la répétition de Consolidation.");
            return;
        }
        renderMode();
    }

    private void validateConsolidationCycle() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        String label = "Consolidation · " + consolidationSession.sessionGroupSize() + " unité(s) · Acquis";
        if (!prefs.completeConsolidationSessionV6(consolidationSession, geometry, sessionDate.toString(), label)) {
            onError("Impossible d’enregistrer la Consolidation.");
            return;
        }
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Commit a timed session whose foreground clock reached its limit before process death. */
    private boolean completeExpiredTimedSession() {
        if (!isTimedMode() || !PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes())) return false;
        String today = sessionDate.toString();
        if (SABQI_TODAY_REVIEW.equals(mode)
                && !today.equals(prefs.lastSabqiTodayReviewDate())
                && today.equals(prefs.sabqiTodayReviewDate())) {
            timedSessionLimitReached = true;
            prefs.completeSabqiTodayReview(today, "Apprentissage · 30 min");
            closeClockForCompletedSession();
            return true;
        }
        return false;
    }

    private boolean completeConsolidation(String today, long elapsedMs) {
        int lines = metricsStore.consolidationLines();
        SpeedCalibration.Result calibration = speedStore.calibrateConsolidation(lines, elapsedMs);
        String raw = HifzSpeedStore.instrumentationLabel(lines, elapsedMs, calibration);
        if (calibration.status == SpeedCalibration.Status.ATYPICAL) raw += "·atyp";
        String label = "Consolidation · " + targetMinutes() + " min · " + raw;
        if (!prefs.completeRecentSabqiReview(today, recentReviewIndex, label)) {
            onError("Impossible d’enregistrer la Consolidation.");
            return false;
        }
        metricsStore.clearConsolidation();
        captureConsolidationAndRebalance();
        return true;
    }

    /** Empty recent work is completed explicitly and never falls through to old Stabilisation. */
    private boolean completeEmptyRecentSabqiSession() {
        String today = sessionDate.toString();
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
        if (MURAJAAH.equals(mode)) {
            prefs.setElapsedFor(mode, Math.max(elapsed, clock.elapsedMs()));
            updateMurajaahProgress();
            return;
        }
        long saved = clock.pause();
        long effectiveElapsed = Math.max(elapsed, saved);
        prefs.setElapsedFor(mode, effectiveElapsed);
        String today = sessionDate.toString();
        if (SABQI_TODAY_REVIEW.equals(mode)) {
            prefs.completeSabqiTodayReview(today, "Apprentissage · 30 min");
            closeClockForCompletedSession();
            renderMode();
        }
    }

    private void renderItqan() {
        String today=sessionDate.toString();
        if(today.equals(prefs.lastItqanDate())){
            sessionCompleted = true;
            if (prefs.itqanBlockIndex() > 0) {
                program.setText("Stabilisation · séance terminée");
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Sous-bloc terminé":prefs.lastItqanLabel());
            } else {
                program.setText("Stabilisation · unité validée");
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Stabilisation terminé":prefs.lastItqanLabel());
            }
            return;
        }
        anchoringEntry = prefs.currentAnchoringEntry(geometry);
        if (anchoringEntry == null) {
            sessionCompleted = true;
            clock.pause();
            if (prefs.anchoringDeferredToday()) {
                program.setText("Stabilisation · page reportée");
                progress.setText("Cette page reviendra à la prochaine séance d’Stabilisation.");
            } else {
                program.setText("Stabilisation · aucune page en attente");
                progress.setText("Toutes les pages en attente sont acquises.");
            }
            return;
        }
        int rep=prefs.itqanRep();
        VerseRef savedStart=prefs.itqanUnitStart();
        VerseRef savedEnd=prefs.itqanUnitEnd();
        if((rep>0 || prefs.itqanBlockIndex()>0) && savedStart!=null && savedEnd!=null){
            currentPage=geometry.pageForVerse(savedStart);
            unitFirstPage = unitLastPage = currentPage;
            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=geometry.lineIdsForVerseRange(savedStart,savedEnd);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
            AnchoringQueue.Entry inProgress = prefs.anchoringEntryFor(savedStart, savedEnd);
            if (inProgress != null) anchoringEntry = inProgress;
        } else {
            VerseRef entryStart = GeometryRepository.parseVerse(anchoringEntry.start);
            VerseRef entryEnd = GeometryRepository.parseVerse(anchoringEntry.end);
            int page = geometry.pageForVerse(entryStart);
            itqanUnit = new GeometryRepository.VerseUnit(page, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd),
                geometry.lineIdsForVerseRange(entryStart, entryEnd));
            currentPage=itqanUnit.page;unitFirstPage=unitLastPage=currentPage;
        }

        itqanSessionProtocol = anchoringEntry.protocol;
        itqanTargetReps = PreviewConfig.itqanTotalReps(itqanSessionProtocol);
        List<StabilizationHalfPagePolicy.Unit> plannedUnits = StabilizationHalfPagePolicy.planPage(
            geometry.linesForIdsOnPage(itqanUnit.page, itqanUnit.lineIds));
        itqanBlockCount = plannedUnits.size();
        itqanBlockIndex = Math.max(0, Math.min(prefs.itqanBlockIndex(), itqanBlockCount - 1));
        StabilizationHalfPagePolicy.Unit workingUnit = plannedUnits.get(itqanBlockIndex);
        currentLineIds = new ArrayList<>(workingUnit.lineIds);
        currentSelection = geometry.versesOnLines(currentLineIds, itqanUnit.verses);
        fractionatedItqan = itqanBlockCount > 1;

        if(rep>=itqanTargetReps){
            boolean assistancePassed = StructuredSessionPolicy.assistancePasses(prefs.itqanAssisted());
            awaitingValidation=assistancePassed;sessionCompleted=true;clock.pause();currentMask=0;
            program.setText(itqanProgramLabel());
            progress.setText(assistancePassed
                ? itqanTargetReps+"/"+itqanTargetReps+" · prêt à valider · révélations "+prefs.itqanAssisted()
                : itqanTargetReps+"/"+itqanTargetReps+" · à renforcer · révélations "+prefs.itqanAssisted()+" · maximum 2");
            showCurrent();
            if (assistancePassed) addRoundAction("✓","Valider",v->validateItqan());
            else addRoundAction("↻","Reprendre",v->restartItqanAfterAssistance());
            return;
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
            return "Stabilisation · "+start+" → "+end+" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · ×"+itqanTargetReps;
        }
        return "Stabilisation · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+itqanTargetReps
            +(anchoringEntry.origin==AnchoringQueue.Origin.FORCED_PROMOTION?" · promotion de sécurité":"");
    }

    private void updateItqanProgress(int rep,int reveals){
        progress.setText(Math.min(rep+1,itqanTargetReps)+"/"+itqanTargetReps+" · masque "+currentMask+"% · révélations "+reveals);
        eink.local(progress, prefs);
    }

    private void completeItqanRep(){
        if (!takeRepLock()) return;
        boolean revealed=consumeReveal();
        int rep=prefs.itqanRep();if(rep>=itqanTargetReps)return;
        int oldMask=currentMask;rep++;int reveals=prefs.itqanAssisted()+(revealed?1:0);
        int finalReveals = prefs.itqanFinalReveals()
            + (revealed && PreviewConfig.isItqanValidationRep(rep - 1, itqanSessionProtocol) ? 1 : 0);
        if(!prefs.setItqanProgress(rep,reveals,finalReveals,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition d’Stabilisation.");return;}
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
        if (!StructuredSessionPolicy.assistancePasses(prefs.itqanAssisted())) {
            restartItqanAfterAssistance();
            return;
        }
        String metrics = anchoringInstrumentation();
        int nextBlock = itqanBlockIndex + 1;
        boolean finalBlock = nextBlock >= itqanBlockCount;
        EligibleCorpus corpus = prefs.itqanWorkCorpus();
        VerseRef next = corpus.nextAnchored(itqanUnit.end, prefs.itqanRotationStart());
        String label="Stabilisation · bloc "+(itqanBlockIndex+1)+"/"+itqanBlockCount
            +" validé · révélations "+prefs.itqanAssisted()+" · "+metrics;
        metricsStore.recordAnchoring("Stabilisation réussie · "+itqanUnit.start+" → "+itqanUnit.end
            +" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · "+metrics);
        boolean ok = prefs.completeStabilizationBlockV6(
            currentLineIds, nextBlock, finalBlock, itqanUnit.start, itqanUnit.end, next,
            sessionDate.toString(), label);
        if(!ok){onError("Impossible d’enregistrer la validation de la Stabilisation.");return;}
        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void restartItqanAfterAssistance() {
        if (itqanUnit == null || !prefs.setItqanProgress(0, 0, 0, itqanUnit.start, itqanUnit.end)) {
            onError("Impossible de relancer ce bloc d’Stabilisation.");
            return;
        }
        lastCheckpointBucket = -1L;
        clock.reset();
        prefs.setElapsedFor(mode, 0L);
        awaitingValidation = false;
        sessionCompleted = false;
        clock.resume();
        mushaf.cycleCompleted();
        renderMode();
    }

    private void renderMurajaah(){
        String today = sessionDate.toString();
        if (today.equals(prefs.lastMurajaahDate())) {
            sessionCompleted = true;
            program.setText("Révision · séance validée");
            progress.setText(prefs.lastMurajaahLabel().isEmpty() ? "Curseur sauvegardé" : prefs.lastMurajaahLabel());
            return;
        }
        if (!prefs.isMurajaahCursorValid()) {
            sessionCompleted = true;
            program.setText("Révision · curseur à vérifier");
            progress.setText("Le corpus acquis ne contient pas ce curseur.");
            return;
        }
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.murajaahCorpus();
        int lines = HifzCadence.targetLines(targetMinutes(), speedStore.maintenanceSecondsPerLine());
        murajaahPlan = geometry.planEligibleLines(prefs.murajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.murajaahActualEnd();
        if (murajaahActualEnd != null && !corpus.contains(murajaahActualEnd)) {
            murajaahActualEnd = null;
            prefs.setMurajaahActualEnd(null);
        }
        int savedPage = prefs.murajaahPage();
        currentPage = savedPage >= 1 && savedPage <= 604
            ? savedPage : geometry.pageForVerse(murajaahPlan.start);
        currentSelection = Collections.emptyList();
        currentLineIds = Collections.emptyList();
        currentMask = 0;
        program.setText("Révision · objectif " + murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd);
        updateMurajaahProgress();
        showCurrent();
        restoreMurajaahEndpointSelectionOnCurrentPage();
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider jusqu’ici", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(true);
        actions.addView(validateAction);
    }

    private void finishMurajaah(){
        if (!StructuredSessionPolicy.murajaahCanValidate(murajaahActualEnd != null)) {
            Toast.makeText(this, "Touchez d’abord le dernier verset réellement révisé.", Toast.LENGTH_LONG).show();
            return;
        }
        EligibleCorpus corpus = prefs.murajaahCorpus();
        VerseRef next = corpus.next(murajaahActualEnd);
        long elapsed = clock.elapsedMs();
        int lines = countMurajaahLinesThrough(murajaahActualEnd);
        SpeedCalibration.Result calibration = speedStore.calibrateMaintenance(lines, elapsed);
        String raw = HifzSpeedStore.instrumentationLabel(lines, elapsed, calibration);
        if (calibration.status == SpeedCalibration.Status.ATYPICAL) raw += "·atyp";
        String label = "Révision · réel : " + murajaahPlan.start + " → " + murajaahActualEnd
            + " · prochain curseur " + next + " · " + raw;
        boolean ok = prefs.completeMurajaah(next, murajaahPlan.start, murajaahActualEnd, sessionDate.toString(), label);
        if (!ok) { onError("Impossible d’enregistrer la validation de l’Révision."); return; }
        closeClockForCompletedSession();
        renderMode();
    }

    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(through)) throw new IllegalArgumentException("Fin d’Révision hors du corpus acquis : " + through);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        VerseRef cursor = murajaahPlan.start;
        for (int visited = 0; visited < 6236; visited++) {
            ids.addAll(geometry.lineIdsForVerseRange(cursor, cursor));
            if (cursor.equals(through)) return ids.size();
            cursor = corpus.next(cursor);
            if (cursor.equals(murajaahPlan.start)) break;
        }
        throw new IllegalArgumentException("Fin d’Révision inaccessible depuis le curseur courant : " + through);
    }

    private void updateMurajaahProgress() {
        if (!MURAJAAH.equals(mode) || progress == null || murajaahPlan == null) return;
        String target = "objectif " + targetMinutes() + " min";
        if (murajaahActualEnd == null) {
            progress.setText((timedSessionLimitReached ? target + " atteint" : target)
                + " · touchez le dernier verset réellement révisé");
        } else {
            progress.setText("Fin réelle · " + murajaahActualEnd
                + (timedSessionLimitReached ? " · " + target + " atteint" : " · validation possible à tout moment"));
        }
        eink.local(progress, prefs);
    }

    private void restoreMurajaahEndpointSelectionOnCurrentPage() {
        if (!MURAJAAH.equals(mode) || murajaahActualEnd == null || mushaf == null) return;
        if (geometry.pageForVerse(murajaahActualEnd) != currentPage) return;
        mushaf.setSelection(Collections.singletonList(murajaahActualEnd),
            geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd));
    }

    private void checkpointMurajaah(long elapsed){
        if (!MURAJAAH.equals(mode) || sessionCompleted) return;
        prefs.setMurajaahActualEnd(murajaahActualEnd);
        prefs.setMurajaahPage(currentPage);
    }

    @Override public void onVerseTap(VerseRef verse){
        if (!MURAJAAH.equals(mode) || murajaahPlan == null) return;
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(verse)) {
            Toast.makeText(this, "Ce verset n’appartient pas encore au corpus acquis.", Toast.LENGTH_SHORT).show();
            return;
        }
        murajaahActualEnd=verse;
        updateMurajaahProgress();
        mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
        checkpointMurajaah(clock.elapsedMs());
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
        return SABQI_TODAY_REVIEW.equals(mode) || MURAJAAH.equals(mode);
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
        if (SABQI.equals(mode)) return "Apprentissage";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Apprentissage";
        if (ITQAN.equals(mode)) return "Stabilisation";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Consolidation";
        return "Révision";
    }
    @Override public void onReady(){
        if(StructuredSessionPolicy.shouldInitialReaderShow(hasShown, sessionCompleted))showCurrent();
    }
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){
        currentPage=page;
        if(MURAJAAH.equals(mode)&&!sessionCompleted){
            prefs.setMurajaahPage(page);
            restoreMurajaahEndpointSelectionOnCurrentPage();
        }
    }
    @Override protected void onResume(){
        super.onResume();
        if(clock==null)return;
        clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&(!timedSessionLimitReached||MURAJAAH.equals(mode)))clock.resume();
    }
    @Override protected void onPause(){
        if(clock==null){super.onPause();return;}
        long elapsed=clock.pause();
        if(sessionCompleted&&!awaitingValidation)prefs.setElapsedFor(mode,0L);
        else{prefs.setElapsedFor(mode,elapsed);checkpointMurajaah(elapsed);}
        super.onPause();
    }
    @Override protected void onDestroy(){closeAudio();if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(clock==null)return super.onKeyDown(code,e);if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
