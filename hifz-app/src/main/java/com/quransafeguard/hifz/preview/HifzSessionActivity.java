package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
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
    public static final String LEARNING_CONSOLIDATION = "LEARNING_CONSOLIDATION";
    public static final String CONSOLIDATION_FINAL = "CONSOLIDATION_FINAL";
    public static final String LEARNING_FINAL = "LEARNING_FINAL";
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
    private LocalDate sessionDate;
    private final ConsolidationCycleEngine consolidationEngine = new ConsolidationCycleEngine();
    private ConsolidationCycleEngine.Session consolidationSession;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !SABQI_TODAY_REVIEW.equals(mode) && !ITQAN.equals(mode)
                && !RECENT_SABQI_REVIEW.equals(mode) && !LEARNING_CONSOLIDATION.equals(mode)
                && !CONSOLIDATION_FINAL.equals(mode) && !LEARNING_FINAL.equals(mode)
                && !MURAJAAH.equals(mode)) mode = SABQI;
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
            else if (LEARNING_CONSOLIDATION.equals(mode)) renderLearningConsolidationCycle();
            else if (CONSOLIDATION_FINAL.equals(mode)) renderConsolidationFinalReview();
            else if (LEARNING_FINAL.equals(mode)) renderLearningFinalReview();
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
            progress.setText(prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : HifzDisplayVocabulary.canonicalize(prefs.lastSabqiLabel()));
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
            if (assistancePassed) addRoundAction("✓","Valider",v->runValidationSafely(this::validateSabqi));
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
        if(!prefs.setSabqiProgress(rep,reveals)){onError("Impossible d’enregistrer la répétition de l’Apprentissage.");return;}
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
        if (!ok) { onError("Impossible d’enregistrer l’Apprentissage."); return; }
        awaitingValidation=false;
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    private void restartSabqiAfterAssistance() {
        if (!prefs.setSabqiProgress(0, 0)) {
            onError("Impossible de relancer ce bloc d’Apprentissage.");
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


    private void renderSabqiTodayReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastSabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Apprentissage · séance validée");
            progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : HifzDisplayVocabulary.canonicalize(prefs.lastSabqiTodayReviewLabel()));
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

    /** Shared plumbing for every grouped ×N repetition cycle (evening snowball or Sunday final review). */
    private void renderGroupedCycle(List<ConsolidationCycleEngine.Unit> units, ConsolidationCycleEngine.Family family,
                                     String cycleIdPrefix, String displayName, String emptyMessage,
                                     Runnable onValidate) {
        String today = sessionDate.toString();
        consolidationSession = prefs.restoreConsolidationSession(consolidationEngine, family);
        if (consolidationSession == null) {
            if (units.isEmpty()) {
                sessionCompleted = true;
                program.setText(displayName + " · rien à faire");
                progress.setText(emptyMessage);
                return;
            }
            ConsolidationCycleEngine.Cycle cycle = null;
            for (int i = 0; i < units.size(); i++) {
                ConsolidationCycleEngine.Unit unit = units.get(i);
                cycle = i == 0
                    ? consolidationEngine.startCycle(cycleIdPrefix + today, family, unit)
                    : consolidationEngine.addUnit(cycle, unit);
            }
            consolidationSession = consolidationEngine.openSession(cycle, "session-" + today);
            if (!prefs.persistConsolidationSession(consolidationSession))
                throw new IllegalStateException("Impossible d’enregistrer la séance de " + displayName + " ouverte");
        }
        if (consolidationSession.readyToClose()) {
            sessionCompleted = true;
            program.setText(displayName + " · prêt à valider");
            progress.setText("Toutes les répétitions du groupe sont terminées.");
            addRoundAction("✓", "Valider", v -> runValidationSafely(onValidate));
            return;
        }
        int position = consolidationSession.nextUnitIndex();
        List<String> exactLineIds = ConsolidationPhysicalUnitPolicy.decodeLineUnit(
            consolidationSession.unitIds().get(position));
        List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(exactLineIds);
        currentPage = physicalLines.get(0).page;
        unitFirstPage = unitLastPage = currentPage;
        currentLineIds = new ArrayList<>(exactLineIds);
        currentSelection = geometry.versesOnLines(currentLineIds);
        currentMask = 0;
        sessionCompleted = false;
        int[] vector = consolidationSession.stageVectorAt(position);
        int target = vector[consolidationSession.stage()];
        program.setText(displayName + " · " + currentLineIds.size() + " lignes · unité "
            + (position + 1) + "/" + consolidationSession.sessionGroupSize());
        progress.setText("Répétition " + consolidationSession.donePerStage() + "/" + target);
        showCurrent();
        addRoundAction("↻", "Répétition", v -> completeGroupedCycleRep());
    }

    private void completeGroupedCycleRep() {
        if (!takeRepLock() || consolidationSession == null || consolidationSession.readyToClose()) return;
        consolidationSession = consolidationEngine.recordRepetition(consolidationSession);
        if (!prefs.persistConsolidationSession(consolidationSession)) {
            onError("Impossible d’enregistrer la répétition.");
            return;
        }
        metricsStore.addConsolidationLines(currentLineIds.size());
        renderMode();
    }

    /** Mardi/Jeudi/Samedi soir: ×10 par unité, cumulatif dans la semaine ; ne graduate rien. */
    private void renderConsolidationCycle() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastStabilizationSnowballEveningDate())) {
            sessionCompleted = true;
            program.setText("Consolidation · séance validée");
            progress.setText("Boule de neige du soir terminée.");
            return;
        }
        renderGroupedCycle(prefs.stabilizedConsolidationUnits(sessionDate), ConsolidationCycleEngine.Family.STABILIZATION,
            "stabilization-", "Consolidation", "Aucune unité Stabilisée cette semaine.",
            this::validateConsolidationCycle);
    }

    private void validateConsolidationCycle() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        if (!prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION, sessionDate.toString())) {
            onError("Impossible d’enregistrer la Consolidation.");
            return;
        }
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Lundi/Mercredi/Vendredi soir: ×10 par unité, cumulatif dans la semaine ; ne graduate rien. */
    private void renderLearningConsolidationCycle() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastLearningSnowballEveningDate())) {
            sessionCompleted = true;
            program.setText("Renforcement · séance validée");
            progress.setText("Boule de neige du soir terminée.");
            return;
        }
        renderGroupedCycle(prefs.learningConsolidationUnits(sessionDate), ConsolidationCycleEngine.Family.LEARNING,
            "learning-", "Renforcement", "Aucun bloc d’Apprentissage cette semaine.",
            this::validateLearningConsolidationCycle);
    }

    private void validateLearningConsolidationCycle() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        if (!prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING, sessionDate.toString())) {
            onError("Impossible d’enregistrer le Renforcement.");
            return;
        }
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Dimanche matin : ×5 final de la boule de neige de la semaine — graduate vers Acquis. */
    private void renderConsolidationFinalReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) {
            sessionCompleted = true;
            program.setText("Consolidation · séance validée");
            progress.setText(prefs.lastRecentSabqiReviewLabel().isEmpty() ? "Acquis" : HifzDisplayVocabulary.canonicalize(prefs.lastRecentSabqiReviewLabel()));
            return;
        }
        renderGroupedCycle(prefs.stabilizationSnowballFinalUnits(sessionDate), ConsolidationCycleEngine.Family.STABILIZATION,
            "stabilization-final-", "Consolidation", "Aucune boule de neige à finaliser cette semaine.",
            this::validateConsolidationFinalReview);
    }

    private void validateConsolidationFinalReview() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
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

    /** Dimanche matin : ×5 final de la boule de neige de la semaine — graduate vers Acquis. */
    private void renderLearningFinalReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastLearningConsolidationDate())) {
            sessionCompleted = true;
            program.setText("Renforcement · séance validée");
            progress.setText(prefs.lastLearningConsolidationLabel().isEmpty() ? "Acquis" : HifzDisplayVocabulary.canonicalize(prefs.lastLearningConsolidationLabel()));
            return;
        }
        renderGroupedCycle(prefs.learningSnowballFinalUnits(sessionDate), ConsolidationCycleEngine.Family.LEARNING,
            "learning-final-", "Renforcement", "Aucune boule de neige à finaliser cette semaine.",
            this::validateLearningFinalReview);
    }

    private void validateLearningFinalReview() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        String label = "Renforcement · " + consolidationSession.sessionGroupSize() + " unité(s) · Acquis";
        if (!prefs.completeLearningConsolidationSessionV6(consolidationSession, geometry, sessionDate.toString(), label)) {
            onError("Impossible d’enregistrer le Renforcement.");
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
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Sous-bloc terminé":HifzDisplayVocabulary.canonicalize(prefs.lastItqanLabel()));
            } else {
                program.setText("Stabilisation · unité validée");
                progress.setText(prefs.lastItqanLabel().isEmpty()?"Stabilisation terminée":HifzDisplayVocabulary.canonicalize(prefs.lastItqanLabel()));
            }
            return;
        }
        anchoringEntry = prefs.currentAnchoringEntry(geometry);
        if (anchoringEntry == null) {
            sessionCompleted = true;
            clock.pause();
            if (prefs.anchoringDeferredToday()) {
                program.setText("Stabilisation · page reportée");
                progress.setText("Cette page reviendra à la prochaine séance de Stabilisation.");
            } else {
                program.setText("Stabilisation · aucune page en attente");
                progress.setText("Toutes les unités de Stabilisation sont terminées.");
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
            List<String> lineIds=CorpusLinePolicy.ownedLineIdsForRangeOnPage(savedStart,savedEnd,geometry);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
            AnchoringQueue.Entry inProgress = prefs.anchoringEntryFor(savedStart, savedEnd);
            if (inProgress != null) anchoringEntry = inProgress;
        } else {
            VerseRef entryStart = GeometryRepository.parseVerse(anchoringEntry.start);
            VerseRef entryEnd = GeometryRepository.parseVerse(anchoringEntry.end);
            int page = geometry.pageForVerse(entryStart);
            itqanUnit = new GeometryRepository.VerseUnit(page, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd),
                CorpusLinePolicy.ownedLineIdsForRangeOnPage(entryStart, entryEnd, geometry));
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
            if (assistancePassed) addRoundAction("✓","Valider",v->runValidationSafely(this::validateItqan));
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
        if(!prefs.setItqanProgress(rep,reveals,finalReveals,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition de la Stabilisation.");return;}
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
            onError("Impossible de relancer ce bloc de Stabilisation.");
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
            progress.setText(prefs.lastMurajaahLabel().isEmpty() ? "Curseur sauvegardé" : HifzDisplayVocabulary.canonicalize(prefs.lastMurajaahLabel()));
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
        program.setText("Révision · objectif " + murajaahObjectiveLabel());
        updateMurajaahProgress();
        showCurrent();
        restoreMurajaahEndpointSelectionOnCurrentPage();
        VerseRef nextSegment = murajaahNextSegmentAfterPage(currentPage);
        if (nextSegment != null) {
            VerseRef jumpTarget = nextSegment;
            LinearLayout jumpAction = Ui.roundAction(this, "", "Segment suivant", v -> {
                currentPage = geometry.pageForVerse(jumpTarget);
                currentSelection = Collections.emptyList();
                currentLineIds = Collections.emptyList();
                showCurrent();
                restoreMurajaahEndpointSelectionOnCurrentPage();
            });
            actions.addView(jumpAction);
        }
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider jusqu’ici", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(true);
        actions.addView(validateAction);
    }

    /**
     * The acquired corpus can hold several disjoint ranges (murajaahObjectiveLabel splits the
     * plan the same way); pages between segments are outside the corpus, so free swiping across
     * them is impractical. Offers the next segment's start once the current page's segment has
     * been passed, so the learner never has to hunt for it.
     */
    private List<VerseRef> murajaahSegmentStarts() {
        List<VerseRef> traversal = murajaahPlan.traversalVerses;
        List<VerseRef> starts = new ArrayList<>();
        if (traversal.isEmpty()) return starts;
        starts.add(traversal.get(0));
        for (int i = 1; i < traversal.size(); i++) {
            if (GeometryRepository.ordinal(traversal.get(i)) != GeometryRepository.ordinal(traversal.get(i - 1)) + 1) {
                starts.add(traversal.get(i));
            }
        }
        return starts;
    }

    private VerseRef murajaahNextSegmentAfterPage(int page) {
        for (VerseRef start : murajaahSegmentStarts()) {
            if (geometry.pageForVerse(start) > page) return start;
        }
        return null;
    }

    private VerseRef murajaahNextSegmentAfter(VerseRef end) {
        if (end == null) return null;
        for (VerseRef start : murajaahSegmentStarts()) {
            if (GeometryRepository.ordinal(start) > GeometryRepository.ordinal(end)) return start;
        }
        return null;
    }

    private void finishMurajaah(){
        if (!StructuredSessionPolicy.murajaahCanValidate(murajaahActualEnd != null)) {
            Toast.makeText(this, "Touchez d’abord le dernier verset réellement révisé.", Toast.LENGTH_LONG).show();
            return;
        }
        VerseRef unread = murajaahNextSegmentAfter(murajaahActualEnd);
        if (unread != null) {
            new AlertDialog.Builder(this).setTitle("Passage restant")
                .setMessage("L’objectif du jour continue plus loin (" + unread + "…). Valider maintenant clôturera la séance du jour sans le lire.")
                .setNegativeButton("Continuer la lecture", null)
                .setPositiveButton("Valider quand même", (d, w) -> completeMurajaahValidation())
                .show();
            return;
        }
        completeMurajaahValidation();
    }

    private void completeMurajaahValidation(){
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

    /**
     * The acquired corpus can be smaller than the time-budgeted line target, so a single
     * Révision session legitimately loops back through it more than once (e.g. a 60-minute
     * objective on a 366-line corpus). "through" may therefore sit on a second or later pass,
     * so this must not stop at the first return to the plan's start — only a hard cap
     * (comfortably above any realistic number of laps) guards against a corrupt corpus.
     */
    private int countMurajaahLinesThrough(VerseRef through){
        if (murajaahPlan == null || through == null) return 0;
        EligibleCorpus corpus = prefs.murajaahCorpus();
        if (!corpus.contains(through)) throw new IllegalArgumentException("Fin d’Révision hors du corpus acquis : " + through);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        VerseRef cursor = murajaahPlan.start;
        for (int visited = 0; visited < 6236 * 20; visited++) {
            ids.addAll(geometry.lineIdsForVerseRange(cursor, cursor));
            if (cursor.equals(through)) return ids.size();
            cursor = corpus.next(cursor);
        }
        throw new IllegalArgumentException("Fin d’Révision inaccessible depuis le curseur courant : " + through);
    }

    /**
     * Human-readable objective spanning every disjoint segment the plan actually traverses
     * (the acquired corpus may hold several ranges, and the plan can wrap back through it more
     * than once); a plain "start → end" would silently hide most of the session's real scope.
     */
    private String murajaahObjectiveLabel() {
        List<VerseRef> traversal = murajaahPlan.traversalVerses;
        if (traversal.isEmpty()) return murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd;
        StringBuilder label = new StringBuilder();
        VerseRef segmentStart = traversal.get(0);
        VerseRef previous = segmentStart;
        for (int i = 1; i <= traversal.size(); i++) {
            VerseRef current = i < traversal.size() ? traversal.get(i) : null;
            boolean contiguous = current != null
                && GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1;
            if (!contiguous) {
                if (label.length() > 0) label.append(" · puis ");
                label.append(segmentStart).append(" → ").append(previous);
                if (current != null) segmentStart = current;
            }
            if (current != null) previous = current;
        }
        return label.toString();
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
        boolean limited=SABQI.equals(mode)||SABQI_TODAY_REVIEW.equals(mode)||ITQAN.equals(mode)
            ||RECENT_SABQI_REVIEW.equals(mode)||LEARNING_CONSOLIDATION.equals(mode)
            ||CONSOLIDATION_FINAL.equals(mode)||LEARNING_FINAL.equals(mode);
        if(limited)target=Math.max(unitFirstPage,Math.min(unitLastPage,target));
        if(target==currentPage)return;closeAudio();currentPage=target;showCurrent();
    }

    private void addRoundAction(String symbol,String label,View.OnClickListener listener){
        LinearLayout box=Ui.roundAction(this,symbol,label,listener);actions.addView(box);
    }

    private void runValidationSafely(Runnable validation) {
        if (validation == null) return;
        try {
            validation.run();
        } catch (RuntimeException error) {
            android.util.Log.e("QuranHifz", "Validation failed for " + mode, error);
            onError("État de progression à vérifier. Ouvrez Diagnostic si nécessaire.");
        }
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
        if (RECENT_SABQI_REVIEW.equals(mode) || LEARNING_CONSOLIDATION.equals(mode)
            || CONSOLIDATION_FINAL.equals(mode) || LEARNING_FINAL.equals(mode)) metricsStore.clearConsolidation();
    }
    private boolean isTimedMode() {
        return SABQI_TODAY_REVIEW.equals(mode) || MURAJAAH.equals(mode);
    }
    private int targetMinutes(){
    SessionKind kind;
    if (SABQI.equals(mode)) kind = SessionKind.SABQI_NEW;
    else if (SABQI_TODAY_REVIEW.equals(mode)) kind = SessionKind.SABQI_TODAY_REVIEW;
    else if (ITQAN.equals(mode)) kind = SessionKind.ITQAN;
    else if (RECENT_SABQI_REVIEW.equals(mode) || LEARNING_CONSOLIDATION.equals(mode)
        || CONSOLIDATION_FINAL.equals(mode) || LEARNING_FINAL.equals(mode)) kind = SessionKind.RECENT_SABQI_REVIEW;
    else kind = SessionKind.OLD_ITQAN_MURAJAAH;
    return HifzSchedule.INSTANCE.targetMinutesFor(kind);
}
    private String displayModeName(){
        if (SABQI.equals(mode)) return "Apprentissage";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Apprentissage";
        if (ITQAN.equals(mode)) return "Stabilisation";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Consolidation";
        if (LEARNING_CONSOLIDATION.equals(mode)) return "Renforcement";
        if (CONSOLIDATION_FINAL.equals(mode)) return "Consolidation";
        if (LEARNING_FINAL.equals(mode)) return "Renforcement";
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
