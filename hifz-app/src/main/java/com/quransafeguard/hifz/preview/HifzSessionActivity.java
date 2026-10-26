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
import android.widget.FrameLayout;
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
    public static final String POST_FINAL_RECALL = "POST_FINAL_RECALL";
    public static final String EXTRA_POST_FINAL_FAMILY = "extra_post_final_family";
    public static final String MURAJAAH = "MURAJAAH";
    public static final String MURAJAAH_ACTIVE = "MURAJAAH_ACTIVE";

    private String mode;
    private HifzPrefs prefs;
    private HifzSpeedStore speedStore;
    private HifzSessionMetricsStore metricsStore;
    private GeometryRepository geometry;
    private MushafView mushaf;
    private AnnotationOverlayView annotationOverlay;
    private AnnotationStore annotationStore;
    private Button annotationButton;
    private Button annotationUndoButton;
    private Button annotationClearButton;
    // The pen starts closed; its undo/erase tools only appear while it is open.
    private boolean annotationEnabled = false;
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
    private AnchoringQueue.ItqanProtocol itqanSessionProtocol = AnchoringQueue.ItqanProtocol.FULL;
    private ItqanRegimeStore.UnitPlan itqanUnitPlan;
    private SemanticPassageRepository semanticPassages;
    private AyahThemesCueIndex ayahThemesTitles;
    private android.app.Dialog semanticTitleDialog;
    private ItqanMaintenancePolicy.Regime itqanRegime = ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS;
    private boolean fractionatedItqan;
    private int itqanBlockIndex;
    private int itqanBlockCount = 1;
    private ItqanPlanSnapshot itqanBonusDecision;
    /** Non-null only for a verified, immutable pre-An-Nas two-page window. */
    private ItqanFourSessionSnapshot frozenItqan;
    private GeometryRepository.EligibleLinePlan murajaahPlan;
    private VerseRef murajaahActualEnd;
    private boolean timedSessionLimitReached;
    private boolean repActionLocked;
    private boolean hasShown;
    private boolean sessionCompleted;
    private boolean awaitingValidation;
    private int unitFirstPage = 1;
    private int unitLastPage = 1;
    private int itqanBlockPage = 1;
    private boolean revealedThisRep;
    private Button revealButton;
    private Button murajaahFinishButton;
    /** Révision active only: armed by "Marquer", the next verse tap flags/unflags it instead of moving the cursor. */
    private boolean weakMarkMode;
    /** Pages revealed at least once during this Révision active session, for weak-spot streak decay. */
    private final java.util.Set<Integer> activeRevealedPages = new java.util.HashSet<>();
    /** Reported as confusing: Révéler alone records nothing — said once per session, not on every touch. */
    private boolean revealDoesNotMarkHintShown;
    private LocalDate sessionDate;
    private final ConsolidationCycleEngine consolidationEngine = new ConsolidationCycleEngine();
    private ConsolidationCycleEngine.Session consolidationSession;
    private PostFinalRecallStore postFinalStore;
    private PostFinalRecallStore.Review currentPostFinalReview;
    private String requestedPostFinalFamily;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !SABQI_TODAY_REVIEW.equals(mode) && !ITQAN.equals(mode)
                && !RECENT_SABQI_REVIEW.equals(mode) && !LEARNING_CONSOLIDATION.equals(mode)
                && !CONSOLIDATION_FINAL.equals(mode) && !LEARNING_FINAL.equals(mode)
                && !POST_FINAL_RECALL.equals(mode)
                && !MURAJAAH.equals(mode) && !MURAJAAH_ACTIVE.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        postFinalStore = new PostFinalRecallStore(this);
        requestedPostFinalFamily=getIntent().getStringExtra(EXTRA_POST_FINAL_FAMILY);
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
        // Resume the original frozen evening/final session, including its calendar date,
        // instead of assigning yesterday's repetitions to today's new cumulative set.
        ConsolidationCycleEngine.Family resumeFamily =
            LEARNING_CONSOLIDATION.equals(mode)||LEARNING_FINAL.equals(mode)
            ? ConsolidationCycleEngine.Family.LEARNING
            : RECENT_SABQI_REVIEW.equals(mode)||CONSOLIDATION_FINAL.equals(mode)
              ? ConsolidationCycleEngine.Family.STABILIZATION : null;
        if(resumeFamily!=null) {
            String prefix=LEARNING_FINAL.equals(mode)?"learning-final-"
                : CONSOLIDATION_FINAL.equals(mode)?"stabilization-final-"
                : LEARNING_CONSOLIDATION.equals(mode)?"learning-":"stabilization-";
            String original=prefs.unfinishedConsolidationSessionDate(resumeFamily,prefix);
            if(original!=null)sessionDate=LocalDate.parse(original);
        }
        speedStore = new HifzSpeedStore(this);
        metricsStore = new HifzSessionMetricsStore(this);
        try {
            geometry = GeometryRepository.get(this);
            semanticPassages = SemanticPassageRepository.shared(this);
            ayahThemesTitles = AyahThemesCueIndex.load(this);
        } catch (Throwable error) {
            Ui.showFatal(this, "La géométrie du Mushaf est indisponible. Fermez puis rouvrez l’application.");
            return;
        }
        if (SABQI.equals(mode) || ITQAN.equals(mode)) {
            // The source of truth is the persistent dated receipt ledger, not the
            // current weekday or a shortcut that could create a duplicate credit.
            DashboardLedger attendance=new DashboardLedger(this);
            attendance.capture(prefs);
            RoadmapPolicy.Decision roadmap=prefs.currentRoadmapDecision(geometry);
            int learningDays=roadmap==null?prefs.learningDaysPerWeek():roadmap.learningDays;
            HifzBacklog.Due oldest=HifzBacklog.first(HifzBacklog.pending(
                prefs,attendance,capturedToday,learningDays));
            if(oldest==null || !oldest.mode.equals(mode) || !oldest.date.equals(sessionDate)) {
                if(oldest==null) {
                    new AlertDialog.Builder(this).setTitle("Séance non due")
                        .setMessage("Aucune nouvelle séance Sabqi ou Itqān n’est due.")
                        .setPositiveButton("Retour",(dialog,which)->finish())
                        .setOnCancelListener(dialog->finish()).show();
                } else {
                    new AlertDialog.Builder(this).setTitle("Leçon en retard")
                        .setMessage("La priorité est "+oldest.frenchName()+" du "+oldest.date+
                            ". L’avancement actuel restera inchangé.")
                        .setPositiveButton("Ouvrir le report",(dialog,which)->{
                            Intent intent=new Intent(this,HifzSessionActivity.class);
                            intent.putExtra(EXTRA_MODE,oldest.mode);
                            intent.putExtra(EXTRA_SCHEDULED_DATE,oldest.date.toString());
                            finish();
                            startActivity(intent);
                        }).setNegativeButton("Retour",(dialog,which)->finish())
                        .setOnCancelListener(dialog->finish()).show();
                }
                return;
            }
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
        annotationStore = new AnnotationStore(this);
        annotationOverlay = new AnnotationOverlayView(this);
        annotationOverlay.setStore(annotationStore);
        annotationOverlay.setDrawingEnabled(annotationEnabled);
        FrameLayout mushafContainer = new FrameLayout(this);
        mushafContainer.addView(mushaf, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mushafContainer.addView(annotationOverlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(mushafContainer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout controlBar = Ui.row(this);
        controlBar.setGravity(Gravity.CENTER);
        controlBar.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,2));
        actions = Ui.row(this);
        actions.setGravity(Gravity.CENTER);
        controlBar.addView(actions);
        if (!MURAJAAH_ACTIVE.equals(mode)) {
            annotationButton = Ui.iconButton(this, "", "Annoter", v -> toggleAnnotationMode());
            annotationButton.setSelected(annotationEnabled);
            controlBar.addView(annotationButton);
            annotationUndoButton = Ui.iconButton(this, "", "Annuler la note", v -> annotationOverlay.undoLastStroke());
            annotationClearButton = Ui.iconButton(this, "", "Effacer les notes", v -> annotationOverlay.clearCurrentPage());
            annotationUndoButton.setVisibility(View.GONE);
            annotationClearButton.setVisibility(View.GONE);
            controlBar.addView(annotationUndoButton);
            controlBar.addView(annotationClearButton);
        }
        // Audio only when the recitation pack is actually installed.
        if (new HifzAudioGate(this).installed()) controlBar.addView(Ui.iconButton(this, "", "Écouter", v -> openAudio()));
        root.addView(controlBar);

        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void toggleAnnotationMode() {
        annotationEnabled = !annotationEnabled;
        annotationOverlay.setDrawingEnabled(annotationEnabled);
        annotationButton.setSelected(annotationEnabled);
        Ui.setIconDescription(annotationButton,
            annotationEnabled ? "Désactiver le crayon" : "Activer le crayon");
        annotationUndoButton.setVisibility(annotationEnabled ? View.VISIBLE : View.GONE);
        annotationClearButton.setVisibility(annotationEnabled ? View.VISIBLE : View.GONE);
    }

    private void renderMode() {
        closeAudio();
        // Amorces/page-word holes are never carried into another mode or a validation flow by
        // accident: Révision active and post-An-Nās Itqān recall opt back in explicitly.
        if (mushaf != null) { mushaf.clearSemanticCues(); mushaf.clearPageLandmarkBoxes(); }
        actions.removeAllViews();
        revealButton = null;
        murajaahFinishButton = null;
        awaitingValidation = false;
        weakMarkMode = false;
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
            else if (POST_FINAL_RECALL.equals(mode)) renderPostFinalRecall();
            else if (MURAJAAH_ACTIVE.equals(mode)) renderMurajaahActive();
            else renderMurajaah();
        } catch (RuntimeException error) {
            sessionCompleted = true;
            clock.pause();
            android.util.Log.e("QuranHifz", "Unable to render " + mode, error);
            program.setText(displayModeName() + " · état à vérifier");
            progress.setText("La séance ne peut pas être affichée. Ouvrez Diagnostic si nécessaire.");
        }
        if (sessionCompleted && !awaitingValidation) showClosingFatiha();
    }

    /**
     * User decision: Al-Fātiḥa, whole and unmasked, is the default screen at the end of every
     * session — read to close it, never part of the Révision loop. Shown only when its page holds
     * exactly 1:1–1:7 (fail closed: otherwise the reader keeps its previous state).
     */
    private void showClosingFatiha() {
        if (mushaf == null) return;
        try {
            int page = geometry.pageForVerse(new VerseRef(1, 1));
            if (geometry.pageForVerse(new VerseRef(1, 7)) != page
                    || geometry.pageForVerse(new VerseRef(2, 1)) == page) return;
            hasShown = true;
            currentPage = page;
            currentSelection = Collections.emptyList();
            currentLineIds = Collections.emptyList();
            currentMask = 0;
            mushaf.setLandmarkLines(null, null);
            mushaf.clearSemanticCues();
            mushaf.clearPageLandmarkBoxes();
            mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0, false);
        } catch (RuntimeException unavailable) {
            android.util.Log.w("QuranHifz", "Closing Al-Fatiha unavailable", unavailable);
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
            progress.setText(QuranSurahNames.range(prefs.sabqiStart(), prefs.sabqiEnd()));
            return;
        }
        if (cursor + PreviewConfig.SABQI_LINES - 1 > endLimit) {
            creditTinySabqiRemnant(cursor, endLimit, today);
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
            program.setText("Apprentissage · " + sabqiBlock.verseLabel() + " · " + sabqiBlock.lineIds.size() + " lignes");
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
        program.setText("Apprentissage · " + sabqiBlock.verseLabel() + " · " + sabqiBlock.lineIds.size() + " lignes");
        updateSabqiProgress(rep, prefs.sabqiAssisted());
        showCurrent();
        addRoundAction("↻","Répétition",v->completeSabqiRep());
        LinearLayout revealAction = Ui.roundAction(this,"","Révéler",null);
        revealButton = (Button) revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    /**
     * Reported directly: a 1-4 line remnant before sabqiEnd() used to dead-end forever, since a
     * fixed 5-line block (PreviewConfig.SABQI_LINES) has no smaller variant. Mirrors the Itqan tiny
     * fragment fast path — credits the remnant straight to Acquis, skipping the full repetition
     * drill, since there's no normal-sized lesson to fall back to on a remnant this short.
     */
    private void creditTinySabqiRemnant(int cursor, int endLimit, String date) {
        List<String> lineIds = new ArrayList<>();
        for (int i = cursor; i <= endLimit; i++) lineIds.add(geometry.line(i).id);
        String label = "Apprentissage · " + lineIds.size() + " ligne(s) · Acquis";
        if (!prefs.completeSabqiTinyBlockV6(lineIds, endLimit + 1, date, label)) {
            onError("Impossible d’enregistrer l’Apprentissage.");
            return;
        }
        sessionCompleted = true;
        program.setText("Apprentissage · séance validée");
        progress.setText(HifzDisplayVocabulary.canonicalize(label));
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
            progress.setText("Validez d’abord les 5 lignes d’Apprentissage du soir.");
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
        program.setText("Apprentissage · " + block.verseLabel() + " · " + block.lineIds.size() + " lignes");
        progress.setText(timedSessionLimitReached ? "30 min atteintes" : "Répétez les mêmes lignes.");
        showCurrent();
        if (!timedSessionLimitReached) addRoundAction("↻", "Répétition", v -> renderMode());
    }

    /**
     * How many of the week's own physical blocks a Sunday final session graduates — excluding the
     * weekly snowball's extra "continuous" unit (present whenever more than one block accumulated),
     * which repeats the same lines already counted by the blocks before it.
     */
    private static int physicalUnitCount(ConsolidationCycleEngine.Session session) {
        int groupSize = session.sessionGroupSize();
        return session.cycle().units().get(0).protocol() == ConsolidationCycleEngine.Protocol.SUNDAY_PAGE
            ? groupSize : groupSize > 1 ? groupSize - 1 : groupSize;
    }

    /** Shared plumbing for every grouped ×N repetition cycle (evening snowball or Sunday final review). */
    private void renderGroupedCycle(List<ConsolidationCycleEngine.Unit> units, ConsolidationCycleEngine.Family family,
                                     String cycleIdPrefix, String displayName, String emptyMessage,
                                     Runnable onValidate) {
        String today = sessionDate.toString();
        consolidationSession = prefs.restoreConsolidationSession(consolidationEngine, family,cycleIdPrefix+today);
        if (consolidationSession != null
                && !consolidationSession.cycle().cycleId().equals(cycleIdPrefix+today)) {
            // Cross-midnight or another final/evening session: NEVER attribute
            // repetitions from a different date to today's credits.
            throw new IllegalStateException("Séance antérieure encore ouverte : "
                +consolidationSession.cycle().cycleId()
                +". Reprendre l'ancienne séance avant une nouvelle validation.");
        }
        // A growing accumulator does not authorize discarding a legacy OPEN
        // session. Finish its exact frozen units; newly added work remains due.
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
        unitFirstPage = physicalLines.get(0).page;
        unitLastPage = physicalLines.get(physicalLines.size() - 1).page;
        currentPage = unitFirstPage;
        currentLineIds = new ArrayList<>(exactLineIds);
        currentSelection = geometry.versesOnLines(currentLineIds);
        // The page itself is one complete review unit: first fully visible, then twice
        // fully masked with exact anchors. Legacy OPEN grouped cycles are unchanged.
        ConsolidationCycleEngine.Protocol protocol=consolidationSession.cycle().units().get(position).protocol();
        boolean sundayPage=protocol==ConsolidationCycleEngine.Protocol.SUNDAY_PAGE;
        boolean maskedMaintien=protocol==ConsolidationCycleEngine.Protocol.POST_NAS_WEEKLY_MASKED;
        currentMask = maskedMaintien || (sundayPage && consolidationSession.donePerStage()>0) ? 100 : 0;
        // A grouped-cycle unit is a frozen physical-line window, never a freshly reconstructed
        // verse range. showCurrent() applies the same exact-line reading focus as Sabqi/Itqān,
        // clipped to these persisted line ids, without changing the unit or progression logic.
        sessionCompleted = false;
        program.setText(sundayPage
            ? displayName + " · page " + unitFirstPage + " · " + currentLineIds.size()
                + " lignes · " + (position + 1) + "/" + consolidationSession.sessionGroupSize()
            : maskedMaintien
                ? "Consolidation Itqān · hizb " + (position + 1) + "/"
                    + consolidationSession.sessionGroupSize() + " · un passage masqué"
                : displayName + " · " + currentLineIds.size() + " lignes · unité "
                    + (position + 1) + "/" + consolidationSession.sessionGroupSize());
        showCurrent();
        updateGroupedCycleRepAction();
    }

    /**
     * A multi-page unit (the weekly snowball's continuous pass can span two pages) must not let a
     * repetition count while the last page is still unseen — otherwise every repetition could be
     * validated from page one alone, without ever reading the rest. The action only appears once
     * the unit's last page is on screen; goPage() calls this again after every page swipe.
     */
    private void updateGroupedCycleRepAction() {
        actions.removeAllViews();
        if (currentPage != unitLastPage) {
            progress.setText("Tournez la page pour voir la suite avant de valider.");
            return;
        }
        int position = consolidationSession.nextUnitIndex();
        int[] vector = consolidationSession.stageVectorAt(position);
        int target = vector[consolidationSession.stage()];
        if (isSundayFinalPageRepetition() && currentMask == 100
                && (sundayFinalAnchorUnavailable || !sundayFinalMaskedPageReady)) {
            progress.setText(sundayFinalAnchorUnavailable
                ? "Amorces QCF indisponibles · validation masquée suspendue."
                : "Masque en cours de chargement · répétition indisponible.");
            return;
        }
        progress.setText("Répétition " + consolidationSession.donePerStage() + "/" + target
            + (isSundayFinalPageRepetition() ? currentMask == 100 ? " · masquée 100 %" : " · visible" : ""));
        addRoundAction("↻", "Répétition", v -> completeGroupedCycleRep());
    }

    private void completeGroupedCycleRep() {
        if (isSundayFinalPageRepetition() && currentMask == 100
                && (sundayFinalAnchorUnavailable || !sundayFinalMaskedPageReady)) {
            onError("Masque non prêt : répétition masquée non validée.");
            return;
        }
        if (!takeRepLock() || consolidationSession == null || consolidationSession.readyToClose()) return;
        consolidationSession = consolidationEngine.recordRepetition(consolidationSession);
        if (!prefs.persistConsolidationSession(consolidationSession)) {
            onError("Impossible d’enregistrer la répétition.");
            return;
        }
        metricsStore.addConsolidationLines(currentLineIds.size());
        renderMode();
    }

    /** Historic evening snowball or one continuous weekly post-Nas masked Maintien sequence. */
    private void renderConsolidationCycle() {
        String today = sessionDate.toString();
        if (!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,sessionDate)) {
            sessionCompleted = true;
            if(prefs.itqanPostNasMaintenance()) {
                boolean completed=sessionDate.toString().equals(prefs.lastStabilizationSnowballEveningDate());
                program.setText(completed?"Consolidation · séance validée":"Consolidation · à venir");
                progress.setText(completed
                    ?"Maintiens de la semaine récités une fois avec texte masqué."
                    :"Disponible après le dernier Maintien de la semaine.");
            } else {
                program.setText("Consolidation · séance validée");
                progress.setText("Révision cumulative du soir terminée.");
            }
            return;
        }
        renderGroupedCycle(prefs.stabilizedConsolidationUnits(sessionDate), ConsolidationCycleEngine.Family.STABILIZATION,
            "stabilization-", "Consolidation", "Aucune unité Stabilisée cette semaine.",
            this::validateConsolidationCycle);
    }

    private void validateConsolidationCycle() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        if (!prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION, sessionDate.toString(), consolidationSession)) {
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
        if (!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,sessionDate)) {
            sessionCompleted = true;
            program.setText("Renforcement · séance validée");
            progress.setText("Révision cumulative du soir terminée.");
            return;
        }
        renderGroupedCycle(prefs.learningConsolidationUnits(sessionDate), ConsolidationCycleEngine.Family.LEARNING,
            "learning-", "Renforcement", "Aucun bloc d’Apprentissage cette semaine.",
            this::validateLearningConsolidationCycle);
    }

    private void validateLearningConsolidationCycle() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        if (!prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING, sessionDate.toString(), consolidationSession)) {
            onError("Impossible d’enregistrer le Renforcement.");
            return;
        }
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Dimanche matin : ×10 final de la boule de neige de la semaine — graduate vers Acquis. */
    private void renderConsolidationFinalReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) {
            sessionCompleted = true;
            program.setText("Finale Itqān · validée");
            progress.setText(prefs.lastRecentSabqiReviewLabel().isEmpty() ? "Acquis" : HifzDisplayVocabulary.canonicalize(prefs.lastRecentSabqiReviewLabel()));
            offerPostFinalRecall(PostFinalRecallStore.ITQAN);
            return;
        }
        // The physical An-Nas latch forbids every NEW final, including a
        // stale/direct intent or an explicit programme choice. A real OPEN
        // first-regime final still restores its original protocol and date.
        if(prefs.itqanPostNasMaintenance()
                && prefs.unfinishedConsolidationSessionDate(
                    ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-")==null) {
            sessionCompleted=true;
            program.setText("Itqān · Maintien de la maîtrise");
            progress.setText("Aucune nouvelle finale Itqān. Reprendre la séance ordinaire d’un hizb ×3.");
            return;
        }
        renderGroupedCycle(prefs.stabilizationSnowballFinalUnits(sessionDate, geometry), ConsolidationCycleEngine.Family.STABILIZATION,
            "stabilization-final-", "Finale Itqān", "Aucun passage admissible à la finale cette semaine.",
            this::validateConsolidationFinalReview);
    }

    private void validateConsolidationFinalReview() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        String label = "Consolidation · " + physicalUnitCount(consolidationSession) + " unité(s) · Acquis";
        preparePostFinalRecall(PostFinalRecallStore.ITQAN);
        boolean closed;
        try { closed=prefs.completeConsolidationSessionV6(consolidationSession, geometry, sessionDate.toString(), label); }
        catch(IllegalStateException missingEvening) {
            onError("Finale refusée : Consolidation ×7 du soir manquant. Aucune ligne créditée.");
            return;
        }
        if (!closed) {
            onError("Impossible d’enregistrer la Consolidation.");
            return;
        }
        postFinalStore.markReady(PostFinalRecallStore.ITQAN,sessionDate.toString());
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
        promptPostFinalRecall(PostFinalRecallStore.ITQAN);
    }

    /** Dimanche matin : ×10 final de la boule de neige de la semaine — graduate vers Acquis. */
    private void renderLearningFinalReview() {
        String today = sessionDate.toString();
        if (today.equals(prefs.lastLearningConsolidationDate())) {
            sessionCompleted = true;
            program.setText("Finale Sabqi · validée");
            progress.setText(prefs.lastLearningConsolidationLabel().isEmpty() ? "Acquis" : HifzDisplayVocabulary.canonicalize(prefs.lastLearningConsolidationLabel()));
            offerPostFinalRecall(PostFinalRecallStore.SABQI);
            return;
        }
        // A known frozen Saturday evening session MUST be closed first.
        // Otherwise SundayFinalPagePolicy would see only the earlier receipts
        // and could close a 10/15-line partial final as though the week had ended.
        // Do not modify learned lines or synthesize missing repetition receipts.
        String overdue=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING,"learning-");
        String existingFinal=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING,"learning-final-");
        if(overdue!=null && existingFinal==null
                && !LocalDate.parse(overdue).isAfter(sessionDate)) {
            sessionCompleted=false;
            program.setText("Finale Sabqi · "+today+" · en attente");
            progress.setText("Les lignes sont Apprises. Reprendre le Renforcement du "
                +overdue+" avant la finale ; aucun apprentissage à recommencer.");
            actions.removeAllViews();
            addRoundAction("↻","Reprendre",
                v -> {
                    Intent intent=new Intent(this,HifzSessionActivity.class)
                        .putExtra(EXTRA_MODE,LEARNING_CONSOLIDATION)
                        .putExtra(EXTRA_SCHEDULED_DATE,overdue);
                    startActivity(intent);
                    finish();
                });
            return;
        }
        renderGroupedCycle(prefs.learningSnowballFinalUnits(sessionDate, geometry), ConsolidationCycleEngine.Family.LEARNING,
            "learning-final-", "Finale Sabqi", "Aucun passage admissible à la finale cette semaine.",
            this::validateLearningFinalReview);
    }

    private void validateLearningFinalReview() {
        if (consolidationSession == null || !consolidationSession.readyToClose()) return;
        speedStore.calibrateConsolidation(metricsStore.consolidationLines(), clock.elapsedMs());
        String label = "Renforcement · " + physicalUnitCount(consolidationSession) + " unité(s) · Acquis";
        preparePostFinalRecall(PostFinalRecallStore.SABQI);
        boolean closed;
        try { closed=prefs.completeLearningConsolidationSessionV6(consolidationSession, geometry, sessionDate.toString(), label); }
        catch(IllegalStateException missingEvening) {
            onError("Finale refusée : Renforcement ×10 du soir manquant. Aucune ligne créditée.");
            return;
        }
        if (!closed) {
            onError("Impossible d’enregistrer le Renforcement.");
            return;
        }
        postFinalStore.markReady(PostFinalRecallStore.SABQI,sessionDate.toString());
        consolidationSession = consolidationEngine.closeSession(consolidationSession);
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
        promptPostFinalRecall(PostFinalRecallStore.SABQI);
    }


    /** Capture exact acquired-before and current-final lines without changing credit engines. */
    private void preparePostFinalRecall(String family){
        if(consolidationSession==null)return;
        try {
            java.util.HashMap<String,GeometryRepository.LineMeta> byId=new java.util.HashMap<>();
            for(int i=0;i<geometry.lineCount();i++){GeometryRepository.LineMeta line=geometry.line(i);byId.put(line.id,line);}
            ConsolidationCycleEngine.Family sourceFamily =
                PostFinalRecallStore.SABQI.equals(family)
                    ? ConsolidationCycleEngine.Family.LEARNING
                    : PostFinalRecallStore.ITQAN.equals(family)
                        ? ConsolidationCycleEngine.Family.STABILIZATION
                        : null;
            if(sourceFamily==null || consolidationSession.cycle().family()!=sourceFamily)
                throw new IllegalStateException("Post-final family's source does not match validated finale");
            List<GeometryRepository.LineMeta> prior=new ArrayList<>();
            for(String id:postFinalStore.previouslyAcquired(family,
                    prefs.progressionSnapshotV6().acquired)){
                GeometryRepository.LineMeta line=byId.get(id);
                if(line==null)throw new IllegalStateException("Missing acquired line geometry");
                prior.add(line);
            }
            java.util.LinkedHashSet<String> frozen=new java.util.LinkedHashSet<>();
            List<ConsolidationCycleEngine.Unit> units=consolidationSession.cycle().units();
            for(int i=0;i<units.size();i++){
                ConsolidationCycleEngine.Unit unit=units.get(i);
                if(i==units.size()-1&&units.size()>1
                        &&unit.protocol()==ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED)continue;
                frozen.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(unit.id()));
            }
            List<GeometryRepository.LineMeta> current=new ArrayList<>();
            for(String id:frozen){
                GeometryRepository.LineMeta line=byId.get(id);
                if(line==null)throw new IllegalStateException("Missing final line geometry");
                current.add(line);
            }
            postFinalStore.prepare(family,sessionDate.toString(),PostFinalCumulativePolicy.plan(prior,current));
        }catch(RuntimeException unavailable){
            android.util.Log.e("QuranHifz","Post-final optional snapshot unavailable",unavailable);
            onError("Lecture cumulative indisponible ; la finale reste indépendante.");
        }
    }

    private void launchPostFinalRecall(String family){
        startActivity(new Intent(this,HifzSessionActivity.class)
            .putExtra(EXTRA_MODE,POST_FINAL_RECALL)
            .putExtra(EXTRA_POST_FINAL_FAMILY,family));
    }

    private PostFinalRecallStore.Review readyPostFinalRecall(String family){
        postFinalStore.repairLegacyPendingFamilyPlans(
            geometry,prefs.progressionSnapshotV6().acquired);
        return postFinalStore.nextReadyFor(family,
            prefs.lastLearningConsolidationDate(),prefs.lastRecentSabqiReviewDate());
    }

    private void offerPostFinalRecall(String family){
        PostFinalRecallStore.Review ready=readyPostFinalRecall(family);
        if(ready!=null)addRoundAction("↻","Lecture cumulative",
            v->launchPostFinalRecall(family));
    }

    /** Actual final credits already committed: offer the additional one-pass recall explicitly. */
    private void promptPostFinalRecall(String family){
        PostFinalRecallStore.Review ready=readyPostFinalRecall(family);
        if(ready==null){
            new android.app.AlertDialog.Builder(this)
                .setTitle("Finale validée · lecture cumulative à vérifier")
                .setMessage("La finale est enregistrée, mais la lecture cumulative n’a pas été préparée. "
                    +"Aucun crédit ne sera annulé. Conservez une sauvegarde avant toute correction.")
                .setPositiveButton("Compris",null).show();
            return;
        }
        String title=PostFinalRecallStore.ITQAN.equals(family)?"Itqān":"Sabqi";
        new android.app.AlertDialog.Builder(this)
            .setTitle("Finale "+title+" validée")
            .setMessage("Lecture supplémentaire : "+ready.previousCount
                +" page(s) précédemment acquises au maximum, plus les pages de cette finale "
                +"("+ready.total+" pages au total). Une récitation masquée par page, sans nouveau crédit.")
            .setPositiveButton("Commencer", (dialog,which)->launchPostFinalRecall(family))
            .setNegativeButton("Plus tard",null).show();
    }

    private void renderPostFinalRecall(){
        postFinalStore.repairLegacyPendingFamilyPlans(
            geometry,prefs.progressionSnapshotV6().acquired);
        currentPostFinalReview=requestedPostFinalFamily==null
            ?postFinalStore.nextReady(prefs.lastLearningConsolidationDate(),prefs.lastRecentSabqiReviewDate())
            :readyPostFinalRecall(requestedPostFinalFamily);
        if(currentPostFinalReview==null){
            sessionCompleted=true;
            String family=requestedPostFinalFamily;
            boolean blocked=family!=null
                ?postFinalStore.hasBlockedLegacyFamily(family)
                :postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.SABQI)
                    ||postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.ITQAN);
            program.setText(blocked?"Lecture cumulative · provenance à vérifier"
                :"Lecture cumulative · terminée");
            progress.setText(blocked
                ?"Ancienne lecture 1.17.7 conservée mais bloquée : impossible de prouver ses pages Sabqi/Itqān. Aucun crédit modifié."
                :"Une récitation masquée par page effectuée.");
            return;
        }
        PostFinalRecallStore.Review review=currentPostFinalReview;
        if(!geometry.lineIdsOnPage(review.page).containsAll(review.lines))
            throw new IllegalStateException("Invalid physical QCF recall lines");
        for(GeometryRepository.LineMeta line:geometry.linesForExactIds(review.lines))
            if(line.page!=review.page)throw new IllegalStateException("Cross-page recall");
        unitFirstPage=unitLastPage=currentPage=review.page;
        currentLineIds=new ArrayList<>(review.lines);
        currentSelection=geometry.versesOnLines(currentLineIds);
        currentMask=100;sessionCompleted=false;
        program.setText("Lecture cumulative "+(PostFinalRecallStore.SABQI.equals(review.family)?"Sabqi":"Itqān")
            +" · page "+(review.position+1)+"/"+review.total);
        progress.setText("20 dernières pages du parcours, finale comprise · 1 fois masquée");
        showCurrent();
        updatePostFinalRecallAction();
    }

    private void updatePostFinalRecallAction(){
        actions.removeAllViews();
        if(currentPostFinalReview==null||sessionCompleted)return;
        if(sundayFinalAnchorUnavailable||!sundayFinalMaskedPageReady
                ||currentPage!=currentPostFinalReview.page){
            progress.setText(sundayFinalAnchorUnavailable
                ?"Amorces QCF indisponibles : passage suspendu."
                :"Chargement du masque 100 % avant validation.");
            return;
        }
        progress.setText("Page "+(currentPostFinalReview.position+1)+"/"
            +currentPostFinalReview.total+" · 100 % masquée · un passage");
        addRoundAction("↻","Page récitée",v->{
            if(!sundayFinalMaskedPageReady||sundayFinalAnchorUnavailable||currentPostFinalReview==null
                    ||currentPage!=currentPostFinalReview.page)return;
            if(!postFinalStore.advance(currentPostFinalReview,prefs.lastLearningConsolidationDate(),
                    prefs.lastRecentSabqiReviewDate())){
                onError("Récitation non sauvegardée ; rouvrir cette page.");return;
            }
            sundayFinalMaskedPageReady=false;renderMode();
        });
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
        if (isMurajaahMode()) {
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

    /** P4 tiny-fragment fast path (see creditTinyItqanBlockAndChain): a bounded safety cap on how
     *  many consecutive 1-2 line fragments can auto-chain into the next portion in one render pass
     *  — real Mushaf data never runs this deep, but this guarantees no runaway recursion either way. */
    private static final int MAX_ITQAN_AUTO_CHAIN = 5;

    private void renderItqan() { renderItqan(0); }

    private void renderItqan(int autoChainDepth) {
        // Restore the exact saved plan BEFORE asking the legacy verse-unit picker.
        // Existing 1.17.1 OPEN units return null and finish unmodified.
        // Existing OPEN V1 four-date sessions remain exactly frozen. New
        // sessions use the established 8+7+7 physical-line Itqān engine.
        // Never create a four-date plan merely by entering this screen.
        frozenItqan=prefs.frozenItqanSnapshot(geometry);
        if(frozenItqan!=null){renderFrozenItqan();return;}
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
        // Reported directly, confirmed on a live export: this used to trust savedStart/savedEnd
        // as soon as rep or blockIndex was non-zero, with no check against anchoringEntry (the
        // position currentAnchoringEntry just computed) — so a session already mid-repetition on
        // a unit from before a rotation-position fix (or a Plages Acquises edit that moved the
        // real position forward) kept reappearing verbatim forever, no matter what the rotation
        // itself now correctly computed. A saved unit is only ever safe to resume when it's still
        // the exact unit anchoringEntry says is current; anything else is stale and must be
        // discarded so the fresh pick below actually takes over.
        boolean savedMatchesCurrentEntry = savedStart!=null && savedEnd!=null
            && savedStart.toString().equals(anchoringEntry.start) && savedEnd.toString().equals(anchoringEntry.end);
        if ((rep>0 || prefs.itqanBlockIndex()>0) && !savedMatchesCurrentEntry) {
            if (!prefs.discardStaleItqanProgress()) {
                onError("Impossible d’actualiser la position de Stabilisation.");
                return;
            }
            // Re-render from a clean slate rather than patching every local variable below (rep,
            // itqanBonusDecision, …) that assumed the stale unit — this is the same recursion the
            // tiny-fragment fast path already uses after a state change, and it terminates
            // immediately: the freshly-cleared prefs make this branch false on the next pass.
            renderItqan(autoChainDepth);
            return;
        }
        if(savedMatchesCurrentEntry){
            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=prefs.ordinaryItqanLineIds(savedStart,savedEnd,geometry);
            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(lineIds);
            unitFirstPage = physicalLines.get(0).page;
            unitLastPage = physicalLines.get(physicalLines.size() - 1).page;
            currentPage = unitFirstPage;
            itqanUnit=new GeometryRepository.VerseUnit(unitFirstPage,savedStart,savedEnd,verses,lineIds);
            AnchoringQueue.Entry inProgress = prefs.anchoringEntryFor(savedStart, savedEnd);
            if (inProgress != null) anchoringEntry = inProgress;
        } else {
            VerseRef entryStart = GeometryRepository.parseVerse(anchoringEntry.start);
            VerseRef entryEnd = GeometryRepository.parseVerse(anchoringEntry.end);
            List<String> lineIds = prefs.ordinaryItqanLineIds(entryStart, entryEnd, geometry);
            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(lineIds);
            unitFirstPage = physicalLines.get(0).page;
            unitLastPage = physicalLines.get(physicalLines.size() - 1).page;
            itqanUnit = new GeometryRepository.VerseUnit(unitFirstPage, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd), lineIds);
            currentPage=unitFirstPage;
        }

        // FULL first pass ×40; after the first An-Nās arrival, one hizb ×3
        // (2 visible + 1 anchored). A previously opened ×4 unit finishes
        // its persisted plan without dropping the fourth repetition.
        ItqanRegimeStore.UnitPlan plan = prefs.itqanUnitPlan(anchoringEntry);
        if (!prefs.stampItqanUnitPlan(anchoringEntry, plan)) {
            onError("Impossible d’enregistrer le protocole de Stabilisation.");
            return;
        }
        itqanRegime = plan.regime;
        itqanSessionProtocol = plan.protocol;
        itqanUnitPlan = plan;
        itqanTargetReps = ItqanMaintenancePolicy.totalReps(itqanUnitPlan);
        boolean maintenance = itqanRegime == ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE;
        List<GeometryRepository.LineMeta> unitLines = geometry.linesForExactIds(itqanUnit.lineIds);
        List<StabilizationHalfPagePolicy.Unit> plannedUnits = maintenance
            ? Collections.singletonList(new StabilizationHalfPagePolicy.Unit(unitLines.get(0).page,
                unitLines.get(0).verses.get(0).getSurah(), itqanUnit.lineIds))
            : StabilizationHalfPagePolicy.planPage(unitLines);
        itqanBlockCount = plannedUnits.size();
        itqanBlockIndex = Math.max(0, Math.min(prefs.itqanBlockIndex(), itqanBlockCount - 1));
        StabilizationHalfPagePolicy.Unit workingUnit = plannedUnits.get(itqanBlockIndex);
        List<String> ownLineIds = new ArrayList<>(workingUnit.lineIds);
        ownLineIds.removeAll(prefs.itqanConsumedBonusLineIds());
        currentLineIds = ownLineIds;
        currentSelection = geometry.versesOnLines(currentLineIds, itqanUnit.verses);
        fractionatedItqan = itqanBlockCount > 1;
        itqanBlockPage = geometry.linesForExactIds(currentLineIds).get(0).page;
        currentPage = itqanBlockPage;

        // P4 tiny-fragment fast path: a sub-block reduced to 1-2 physical lines (a leg-boundary or
        // Sabqi-frontier remnant, never a byproduct of splitting a larger chunk — see
        // StabilizationHalfPagePolicy.appendSegment) is too short for a real repeated session.
        // Ask once whether to credit it straight to Acquis and chain immediately into the next real
        // (7-8 line) portion the same day, or to do a normal session on it regardless — never
        // decided silently. Reinforcement laps are excluded: that material is already Acquired, so
        // "entering Acquis" is moot, and the existing full-protocol reinforcement pass already
        // applies to it.
        boolean tinyFastPathEligible = maintenance
            // Maintenance never moves Appris (Sabqi) or already-credited lines: only a fragment
            // made entirely of never-credited lines may take the direct-to-Acquis fast path.
            ? prefs.itqanUncreditedLines(workingUnit.lineIds).size() == workingUnit.lineIds.size()
            : !prefs.entryIsFullyStabilizedOrAcquired(anchoringEntry, geometry);
        if (rep == 0 && workingUnit.lineIds.size() <= 2 && autoChainDepth < MAX_ITQAN_AUTO_CHAIN
                && tinyFastPathEligible) {
            Boolean tinyBlockDecision = prefs.itqanTinyBlockDecisionFor(itqanUnit.start, itqanUnit.end, itqanBlockIndex);
            if (tinyBlockDecision == null) {
                showItqanTinyBlockDialog(itqanUnit.start, itqanUnit.end, itqanBlockIndex, workingUnit.lineIds.size());
                return;
            }
            if (tinyBlockDecision) {
                creditTinyItqanBlockAndChain(autoChainDepth);
                return;
            }
            // false: the learner refused the fast path — fall through to the normal P3 bonus
            // logic and repeated session below, exactly as if this block were never tiny.
        }

        // P3 "finish the page?" (Itqān only, never Sabqi): the decision is settled once, before
        // rep 1, and then reused verbatim for every later render of this exact sub-block —
        // including after an assistance restart, which repeats the same block rather than asking
        // again. A decision for a different sub-block or a different parent unit never applies.
        itqanBonusDecision = prefs.itqanBonusSnapshot();
        if (itqanBonusDecision != null
                && !itqanBonusDecision.matches(itqanUnit.start, itqanUnit.end, itqanBlockIndex)) {
            itqanBonusDecision = null;
        }
        if (rep == 0 && itqanBonusDecision == null) {
            // A post-An-Nās unit is already a whole hizb: never offer to extend it.
            ItqanPageCompletionPolicy.Offer offer = maintenance ? null
                : computeItqanBonusOffer(plannedUnits, currentLineIds);
            if (offer == null || offer.choice == ItqanPageCompletionPolicy.Choice.NONE) {
                ItqanPlanSnapshot keep = ItqanPlanSnapshot.undecided(itqanUnit.start, itqanUnit.end, itqanBlockIndex).keep();
                if (!prefs.saveItqanBonusDecision(keep)) {
                    onError("Impossible d’enregistrer la décision de Stabilisation.");
                    return;
                }
                itqanBonusDecision = keep;
            } else {
                showItqanBonusDialog(itqanUnit.start, itqanUnit.end, itqanBlockIndex, offer);
                return;
            }
        }
        if (itqanBonusDecision != null && itqanBonusDecision.decision == ItqanPlanSnapshot.Decision.EXTEND) {
            currentLineIds = new ArrayList<>(currentLineIds);
            currentLineIds.addAll(itqanBonusDecision.bonusLineIds);
            currentSelection = geometry.versesOnLines(currentLineIds, itqanUnit.verses);
        }

        if(rep>=itqanTargetReps){
            // Post-An-Nās maintenance: reveals are recorded, never a reason to restart the hizb.
            boolean assistancePassed = ItqanMaintenancePolicy.validationAllowed(itqanRegime, prefs.itqanAssisted());
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
        currentMask=ItqanBalancedMaskSchedule.maskForNextRep(itqanUnitPlan, rep);
        program.setText(itqanProgramLabel());
        updateItqanProgress(rep,prefs.itqanAssisted());showCurrent();
        addRoundAction("↻","Répétition",v->completeItqanRep());
        LinearLayout revealAction = Ui.roundAction(this,"","Révéler",null);
        revealButton=(Button)revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    /** Live four-date window: every subblock stays in its original physical
     * order and on its exact saved ×40 progress, including across process death.
     * No P3 bonus, 1–2 line auto-credit, or legacy picker can rewrite this plan. */
    private void renderFrozenItqan() {
        if(sessionDate.toString().equals(prefs.lastItqanDate())) {
            sessionCompleted=true;
            clock.pause();
            program.setText("Stabilisation · séance terminée");
            progress.setText("Prochaine séance sur la prochaine date prévue.");
            return;
        }
        if(frozenItqan.sessionIndex>=4)
            throw new IllegalStateException("Stale finished Itqan window");
        ItqanFourSessionPolicy.Session slot=
            frozenItqan.plan.sessions.get(frozenItqan.sessionIndex);
        if(frozenItqan.blockIndex<0||frozenItqan.blockIndex>=slot.blocks.size())
            throw new IllegalStateException("Frozen Itqan subblock index outside this date");
        ItqanFourSessionPolicy.Block block=slot.blocks.get(frozenItqan.blockIndex);
        List<GeometryRepository.LineMeta> physical=geometry.linesForExactIds(block.lineIds);
        if(physical.isEmpty())throw new IllegalStateException("Missing frozen QCF lines");
        GeometryRepository.LineMeta first=physical.get(0),last=physical.get(physical.size()-1);
        VerseRef start=first.verses.get(0);
        VerseRef end=last.verses.get(last.verses.size()-1);
        currentLineIds=new ArrayList<>(block.lineIds);
        List<VerseRef> verses=geometry.versesForRange(start,end);
        itqanUnit=new GeometryRepository.VerseUnit(first.page,start,end,verses,currentLineIds);
        anchoringEntry=new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
        currentSelection=geometry.versesOnLines(currentLineIds,verses);
        unitFirstPage=first.page;unitLastPage=last.page;
        itqanBlockPage=first.page;currentPage=first.page;
        itqanBlockCount=slot.blocks.size();
        itqanBlockIndex=frozenItqan.blockIndex;
        fractionatedItqan=slot.blocks.size()>1;
        itqanRegime=ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS;
        itqanSessionProtocol=AnchoringQueue.ItqanProtocol.FULL;
        itqanUnitPlan=new ItqanRegimeStore.UnitPlan(itqanRegime,itqanSessionProtocol,
            ItqanRegimeStore.MaskProfile.BALANCED_20_5_5_5_5);
        itqanTargetReps=40;
        if(frozenItqan.reps>=40) {
            boolean assistancePassed=ItqanMaintenancePolicy.validationAllowed(
                itqanRegime,prefs.itqanAssisted());
            awaitingValidation=assistancePassed;sessionCompleted=true;clock.pause();
            currentMask=0;
            program.setText(itqanProgramLabel());
            progress.setText(assistancePassed?"40/40 · prêt à valider"
                :"40/40 · révélations excessives : reprendre ce bloc");
            showCurrent();
            if(assistancePassed)addRoundAction("✓","Valider",v->runValidationSafely(this::validateItqan));
            else addRoundAction("↻","Reprendre",v->restartItqanAfterAssistance());
            return;
        }
        awaitingValidation=false;sessionCompleted=false;
        currentMask=ItqanBalancedMaskSchedule.maskForNextRep(itqanUnitPlan,frozenItqan.reps);
        program.setText(itqanProgramLabel());
        updateItqanProgress(frozenItqan.reps,prefs.itqanAssisted());
        showCurrent();
        addRoundAction("↻","Répétition",v->completeItqanRep());
        LinearLayout revealAction=Ui.roundAction(this,"","Révéler",null);
        revealButton=(Button)revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    private void validateFrozenItqan() {
        if(frozenItqan==null||frozenItqan.reps!=40)return;
        if(!ItqanMaintenancePolicy.validationAllowed(itqanRegime,prefs.itqanAssisted())) {
            restartItqanAfterAssistance();return;
        }
        String label=itqanProgramLabel()+" · validé · "+anchoringInstrumentation();
        if(!prefs.completeFrozenItqanSubblockV6(frozenItqan,geometry,sessionDate.toString(),label)) {
            onError("Validation non enregistrée : séance ou date à vérifier.");
            return;
        }
        metricsStore.recordAnchoring(label);
        awaitingValidation=false;
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    private String itqanProgramLabel() {
        if(frozenItqan!=null) {
            ItqanFourSessionPolicy.Session slot=frozenItqan.plan.sessions.get(frozenItqan.sessionIndex);
            String where=QuranSurahNames.block(geometry,currentLineIds,
                QuranSurahNames.range(itqanUnit.start,itqanUnit.end));
            return "Stabilisation · séance "+(frozenItqan.sessionIndex+1)+"/4"
                +" · bloc "+(frozenItqan.blockIndex+1)+"/"+slot.blocks.size()
                +" · "+where+" · ×40";
        }
        if (fractionatedItqan) {
            VerseRef start = currentSelection.isEmpty() ? itqanUnit.start : currentSelection.get(0);
            VerseRef end = currentSelection.isEmpty() ? itqanUnit.end : currentSelection.get(currentSelection.size() - 1);
            String where=QuranSurahNames.block(geometry,currentLineIds,QuranSurahNames.range(start,end));
            return "Stabilisation · bloc "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · "+where+" · ×"+itqanTargetReps;
        }
        String unitRange=QuranSurahNames.range(itqanUnit.start,itqanUnit.end);
        String where=itqanRegime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE
            ?unitRange:QuranSurahNames.block(geometry,currentLineIds,unitRange);
        return "Stabilisation · "+where+" · ×"+itqanTargetReps
            +(itqanRegime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE
                ?" · répétition d’un hizb · "+ItqanMaintenancePolicy.VISIBLE_REPS+" lectures + "+ItqanMaintenancePolicy.ANCHOR_REPS+" avec ancrages":"")
            +(anchoringEntry.origin==AnchoringQueue.Origin.FORCED_PROMOTION?" · promotion de sécurité":"");
    }

    /**
     * P3: only ever offers to borrow from the immediate next sub-block of the SAME parent unit —
     * never a different parent/week's own working unit, which would need queue-level bookkeeping
     * this feature deliberately does not take on. Refusing is always legal (see
     * ItqanPageCompletionPolicy), so a parent's own last sub-block, or a next sub-block that would
     * be left with nothing of its own, simply never gets an offer.
     */
    private ItqanPageCompletionPolicy.Offer computeItqanBonusOffer(
            List<StabilizationHalfPagePolicy.Unit> plannedUnits, List<String> ownBlockLineIds) {
        List<String> nextLineIds=itqanBlockIndex + 1 < itqanBlockCount
            ? plannedUnits.get(itqanBlockIndex + 1).lineIds : Collections.emptyList();
        // Physically inspect the next one/two lines against the actual Mushaf:
        // an extension is legal only if it closes this page or this surah.
        return ItqanCompletionBoundaryPolicy.offer(geometry,ownBlockLineIds,nextLineIds);
    }

    /** Suspends the clock and the repetition button until the user answers — never automatic,
     *  per spec. A process death before the answer simply re-shows this same dialog next launch,
     *  since nothing is persisted until one of the two buttons is actually tapped. */
    private void showItqanBonusDialog(VerseRef unitStart, VerseRef unitEnd, int blockIndex,
                                       ItqanPageCompletionPolicy.Offer offer) {
        sessionCompleted = true;
        clock.pause();
        boolean plusOne = offer.choice == ItqanPageCompletionPolicy.Choice.PLUS_ONE;
        String remaining = plusOne ? "1 ligne" : "2 lignes";
        program.setText(itqanProgramLabel());
        progress.setText("Il reste "+remaining+" pour terminer la page.");
        new AlertDialog.Builder(this).setTitle("Stabilisation")
            .setMessage("Il reste "+remaining+" pour terminer la page. Finir la page ?")
            .setCancelable(false)
            .setNegativeButton("Garder le bloc prévu", (d, w) -> {
                if (!prefs.saveItqanBonusDecision(
                        ItqanPlanSnapshot.undecided(unitStart, unitEnd, blockIndex).keep())) {
                    onError("Impossible d’enregistrer la décision de Stabilisation.");
                    return;
                }
                clock.resume();
                renderMode();
            })
            .setPositiveButton(plusOne ? "+1 ligne" : "+2 lignes", (d, w) -> {
                if (!prefs.saveItqanBonusDecision(
                        ItqanPlanSnapshot.undecided(unitStart, unitEnd, blockIndex).extend(offer.bonusLineIds))) {
                    onError("Impossible d’enregistrer la décision de Stabilisation.");
                    return;
                }
                clock.resume();
                renderMode();
            })
            .show();
    }

    private void updateItqanProgress(int rep,int reveals){
        String stage=ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, rep)
            ? "rappel avec ancrages" : "masque "+currentMask+"%";
        progress.setText(Math.min(rep+1,itqanTargetReps)+"/"+itqanTargetReps+" · "+stage+" · révélations "+reveals);
        eink.local(progress, prefs);
    }

    private void completeItqanRep(){
        if (!takeRepLock()) return;
        boolean revealed=consumeReveal();
        if(frozenItqan!=null) {
            if(!prefs.recordFrozenItqanRep(frozenItqan,geometry,revealed)) {
                onError("La répétition est périmée : aucune progression enregistrée.");
                return;
            }
            frozenItqan=prefs.frozenItqanSnapshot(geometry);
            int rep=frozenItqan.reps,oldMask=currentMask;
            if(rep>=40) {
                currentMask=0;mushaf.setMask(0);
                long elapsed=clock.pause();prefs.setElapsedFor(mode,elapsed);
                awaitingValidation=true;sessionCompleted=true;renderMode();return;
            }
            currentMask=ItqanBalancedMaskSchedule.maskForNextRep(itqanUnitPlan,rep);
            if(currentMask!=oldMask)mushaf.setMask(displayedMask());
            updateRevealButton();
            if(currentPage!=itqanBlockPage){currentPage=itqanBlockPage;showCurrent();}
            updateItqanProgress(rep,prefs.itqanAssisted());
            return;
        }
        int rep=prefs.itqanRep();if(rep>=itqanTargetReps)return;
        int oldMask=currentMask;rep++;int reveals=prefs.itqanAssisted()+(revealed?1:0);
        int finalReveals = prefs.itqanFinalReveals()
            + (revealed && ItqanMaintenancePolicy.isValidationRep(itqanUnitPlan, rep - 1) ? 1 : 0);
        if(!prefs.setItqanProgress(rep,reveals,finalReveals,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition de la Stabilisation.");return;}
        if(rep>=itqanTargetReps){
            currentMask=0;mushaf.setMask(0);
            long elapsed=clock.pause();prefs.setElapsedFor(mode,elapsed);
            awaitingValidation=true;sessionCompleted=true;renderMode();return;
        }
        currentMask=ItqanBalancedMaskSchedule.maskForNextRep(itqanUnitPlan, rep);
        if(ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, rep)!=ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, rep-1))
            applyItqanAnchors();
        if(currentMask!=oldMask)mushaf.setMask(displayedMask());
        updateRevealButton();if(currentPage!=itqanBlockPage){currentPage=itqanBlockPage;showCurrent();}
        updateItqanProgress(rep,reveals);
    }

    private String anchoringInstrumentation() {
        if (itqanUnit == null) return "0L/0s/0r";
        int lines = fractionatedItqan ? currentLineIds.size()
            : geometry.lineCountForVerseRange(itqanUnit.start, itqanUnit.end);
        long elapsed = Math.max(clock.elapsedMs(), prefs.elapsedFor(mode));
        return Math.max(0, lines) + "L/" + Math.max(0L, elapsed / 1000L) + "s/" + prefs.itqanRep() + "r";
    }

    /** Suspends the clock and the repetition button until the user answers — never automatic, same
     *  as showItqanBonusDialog. A process death before the answer simply re-shows this same dialog
     *  next launch, since nothing is persisted until one of the two buttons is actually tapped. */
    private void showItqanTinyBlockDialog(VerseRef unitStart, VerseRef unitEnd, int blockIndex, int lineCount) {
        sessionCompleted = true;
        clock.pause();
        String plural = lineCount > 1 ? "lignes" : "ligne";
        program.setText(itqanProgramLabel());
        progress.setText("Fragment de "+lineCount+" "+plural+" : valider directement en Acquis ?");
        new AlertDialog.Builder(this).setTitle("Stabilisation")
            .setMessage("Ce fragment ne fait que "+lineCount+" "+plural+". Le valider directement en "
                + "Acquis (sans répétitions), ou faire une séance normale dessus ?")
            .setCancelable(false)
            .setNegativeButton("Séance normale", (d, w) -> {
                if (!prefs.saveItqanTinyBlockDecision(unitStart, unitEnd, blockIndex, false)) {
                    onError("Impossible d’enregistrer la décision de Stabilisation.");
                    return;
                }
                clock.resume();
                renderMode();
            })
            .setPositiveButton("Valider en Acquis", (d, w) -> {
                if (!prefs.saveItqanTinyBlockDecision(unitStart, unitEnd, blockIndex, true)) {
                    onError("Impossible d’enregistrer la décision de Stabilisation.");
                    return;
                }
                clock.resume();
                renderMode();
            })
            .show();
    }

    /**
     * P4 tiny-fragment fast path: credits a 1-2 line Itqān sub-block straight to Acquis (see
     * HifzPrefs.completeItqanTinyBlockV6) and immediately re-renders — either the next sub-block of
     * the same parent unit, or, if this fragment was the whole unit, the next physical unit from
     * the advanced rotation. Mirrors validateItqan's own nextBlock/finalBlock bookkeeping exactly,
     * minus the repetition protocol and the Consolidation snowball enrollment this fragment skips.
     * Only reached once the learner has explicitly chosen this over a normal session (see
     * showItqanTinyBlockDialog).
     */
    private void creditTinyItqanBlockAndChain(int autoChainDepth) {
        int nextBlock = itqanBlockIndex + 1;
        boolean finalBlock = nextBlock >= itqanBlockCount;
        EligibleCorpus corpus = prefs.itqanWorkCorpus();
        VerseRef next = corpus.nextAnchored(itqanUnit.end, prefs.repairedItqanRotationStart());
        metricsStore.recordAnchoring("Stabilisation fragment auto-validé · "+QuranSurahNames.range(itqanUnit.start,itqanUnit.end)
            +" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · "+anchoringInstrumentation());
        if (!prefs.completeItqanTinyBlockV6(currentLineIds, nextBlock, finalBlock, itqanUnit.start, itqanUnit.end, next)) {
            onError("Impossible de créditer directement ce fragment de Stabilisation.");
            return;
        }
        renderItqan(autoChainDepth + 1);
    }

    private void validateItqan(){
        if(frozenItqan!=null){validateFrozenItqan();return;}
        if(itqanUnit==null||anchoringEntry==null||prefs.itqanRep()<itqanTargetReps)return;
        if (!ItqanMaintenancePolicy.validationAllowed(itqanRegime, prefs.itqanAssisted())) {
            restartItqanAfterAssistance();
            return;
        }
        String metrics = anchoringInstrumentation();
        int nextBlock = itqanBlockIndex + 1;
        boolean finalBlock = nextBlock >= itqanBlockCount;
        EligibleCorpus corpus = prefs.itqanWorkCorpus();
        VerseRef next = corpus.nextAnchored(itqanUnit.end, prefs.repairedItqanRotationStart());
        String label="Stabilisation · bloc "+(itqanBlockIndex+1)+"/"+itqanBlockCount
            +" validé · révélations "+prefs.itqanAssisted()+" · "+metrics;
        metricsStore.recordAnchoring("Stabilisation réussie · "+QuranSurahNames.range(itqanUnit.start,itqanUnit.end)
            +" · "+(itqanBlockIndex+1)+"/"+itqanBlockCount+" · "+metrics);
        List<String> bonusLineIds = itqanBonusDecision != null
                && itqanBonusDecision.decision == ItqanPlanSnapshot.Decision.EXTEND
            ? itqanBonusDecision.bonusLineIds : Collections.emptyList();
        // P4: once every physical line here is already ACQUIRED (a later Itqān lap over material
        // stabilised long ago), this is a perpetual reinforcement pass, not a first build — credit
        // it without re-touching progression state or the Consolidation snowball a second time.
        boolean reinforcementLap = prefs.entryIsFullyStabilizedOrAcquired(anchoringEntry, geometry);
        boolean ok = itqanRegime == ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE
            ? prefs.completeItqanMaintenanceUnitV6(currentLineIds, itqanUnit.start, itqanUnit.end, next,
                sessionDate.toString(), label, geometry)
            : reinforcementLap
            ? prefs.completeItqanReinforcementBlock(
                nextBlock, finalBlock, itqanUnit.start, itqanUnit.end, sessionDate.toString(), label)
            : prefs.completeStabilizationBlockV6(
                currentLineIds, nextBlock, finalBlock, itqanUnit.start, itqanUnit.end, next,
                sessionDate.toString(), label, bonusLineIds);
        if(!ok){onError("Impossible d’enregistrer la validation de la Stabilisation.");return;}
        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void restartItqanAfterAssistance() {
        if(frozenItqan!=null) {
            if(!prefs.restartFrozenItqanBlock(frozenItqan,geometry)) {
                onError("Impossible de reprendre la séance de Stabilisation.");
                return;
            }
            lastCheckpointBucket=-1L;
            clock.reset();
            prefs.setElapsedFor(mode,0L);
            awaitingValidation=false;
            sessionCompleted=false;
            clock.resume();
            mushaf.cycleCompleted();
            renderMode();
            return;
        }
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
        EligibleCorpus corpus = prefs.murajaahCorpus(geometry);
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
        mushaf.setMaskFollowsSelection(false);
        mushaf.setHighlightVerses(prefs.murajaahWeakVerses());
        showCurrent();
        restoreMurajaahEndpointSelectionOnCurrentPage();
        updateMurajaahActions();
    }

    /**
     * Daily, mandatory, masked-by-default recall test that must be completed before the passive
     * Entretien comes due. Reads its own restricted corpus (activeMurajaahCorpus: settled material
     * only, never "à stabiliser") and its own independent cursor — testing recall from memory on
     * material still mid-Stabilisation is discouraging rather than constructive, so active and
     * passive deliberately no longer share one traversal position; each advances its own cursor
     * on completion. The whole page is masked from the start (Révéler on demand) instead of shown
     * in clear.
     */
    private void renderMurajaahActive(){
        String today = sessionDate.toString();
        if (today.equals(prefs.lastActiveMurajaahDate())) {
            sessionCompleted = true;
            program.setText("Révision active · séance validée");
            progress.setText(prefs.lastActiveMurajaahLabel().isEmpty() ? "Curseur sauvegardé" : HifzDisplayVocabulary.canonicalize(prefs.lastActiveMurajaahLabel()));
            return;
        }
        if (!prefs.isActiveMurajaahCursorValid()) {
            sessionCompleted = true;
            program.setText("Révision active · curseur à vérifier");
            progress.setText("Le corpus acquis ne contient pas ce curseur.");
            return;
        }
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.activeMurajaahCorpus();
        int lines = HifzCadence.targetLines(targetMinutes(), speedStore.maintenanceSecondsPerLine());
        murajaahPlan = geometry.planEligibleLines(prefs.activeMurajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.activeMurajaahActualEnd();
        if (murajaahActualEnd != null && !corpus.contains(murajaahActualEnd)) {
            murajaahActualEnd = null;
            prefs.setActiveMurajaahActualEnd(null);
        }
        int savedPage = prefs.activeMurajaahPage();
        currentPage = savedPage >= 1 && savedPage <= 604
            ? savedPage : geometry.pageForVerse(murajaahPlan.start);
        currentSelection = Collections.emptyList();
        currentMask = 100;
        currentLineIds = applyActiveLandmarks(currentPage);
        program.setText("Révision active · objectif " + murajaahObjectiveLabel());
        updateMurajaahProgress();
        mushaf.setHighlightVerses(prefs.murajaahWeakVerses());
        showCurrent();
        restoreMurajaahEndpointSelectionOnCurrentPage();
        updateMurajaahActions();
    }

    /**
     * Révision active only. The six exact first/last page-word landmarks are
     * retained. The historical Ibn Kathir/Munir cue holes are REPLACED by the
     * first exact physical QCF word of each approved Ayah Themes passage that
     * begins on the page. None of the corpus, mask code, other Hifz modes,
     * progress counters or page geometry is modified by this display adapter.
     *
     * A page with no new passage beginning still retains six page landmarks.
     * If source data or exact word boxes are missing, fail OPEN (never hide
     * Quran text with an incomplete set of landmarks).
     */
    private boolean activeAyahThemesReady;
    // Stage the new QCF page's exact holes without redrawing the old visible SVG.
    private org.json.JSONArray activePageLandmarks = new org.json.JSONArray();
    private org.json.JSONArray activePageAmorces = new org.json.JSONArray();
    private List<String> applyActiveLandmarks(int page) {
        List<String> lines = geometry.lineIdsOnPage(page);
        activeAyahThemesReady = false;
        activePageLandmarks = new org.json.JSONArray();
        activePageAmorces = new org.json.JSONArray();
        org.json.JSONArray firstLast = new org.json.JSONArray();
        org.json.JSONArray newStarts = new org.json.JSONArray();
        try {
            if (ayahThemesTitles != null) {
                firstLast = WordGeometryRepository.shared(this).pageLandmarkBoxes(page);
                newStarts = ayahThemesTitles.forPage(page);
                if (firstLast.length() == 6) {
                    // One exact source-word box per new Ayah Themes passage start.
                    // Do not synthesize cues or approximate a word position.
                    activeAyahThemesReady = true;
                    for (int i=0;i<newStarts.length();i++) {
                        org.json.JSONObject cue = newStarts.getJSONObject(i);
                        if (cue.getInt("anchorWordCount") != 1 ||
                                cue.getJSONArray("boxes").length() != 1 ||
                                cue.getString("id").isEmpty()) {
                            activeAyahThemesReady = false;
                            break;
                        }
                    }
                }
            }
        } catch (Exception incomplete) {
            activeAyahThemesReady = false;
        }
        // Atomic mask boot alone publishes geometry and prevents a transient
        // unmasked frame. Invalid sidecar geometry keeps this page readable.
        if (activeAyahThemesReady && !lines.isEmpty()) {
            activePageLandmarks = firstLast;
            activePageAmorces = newStarts;
        } else {
            activeAyahThemesReady = false;
        }
        return lines;
    }

    private void updateMurajaahActions() {
        actions.removeAllViews();
        boolean active = MURAJAAH_ACTIVE.equals(mode);
        List<MurajaahSegment> segments = murajaahSegments();
        int currentIndex = murajaahSegmentIndexForPage(segments, currentPage);
        VerseRef nextSegment = murajaahNextSegmentAfterPage(currentPage);
        if (nextSegment != null) {
            VerseRef jumpTarget = nextSegment;
            MurajaahSegment current = currentIndex >= 0 ? segments.get(currentIndex) : null;
            boolean validated = current == null || murajaahValidatedThroughSegment(segments, currentIndex);
            LinearLayout jumpAction = Ui.roundAction(this, "", "Passage suivant du corpus", v -> {
                if (current != null && !murajaahValidatedThroughSegment(segments, currentIndex)) {
                    Toast.makeText(this, "Touchez d’abord le dernier verset de ce passage ("
                        + murajaahVerseLabel(current.end) + ").", Toast.LENGTH_LONG).show();
                    return;
                }
                currentPage = geometry.pageForVerse(jumpTarget);
                currentSelection = Collections.emptyList();
                currentLineIds = active ? applyActiveLandmarks(currentPage) : Collections.emptyList();
                showCurrent();
                restoreMurajaahEndpointSelectionOnCurrentPage();
                updateMurajaahActions();
            });
            actions.addView(jumpAction);
            if (current != null && !validated && geometry.pageForVerse(current.end) == currentPage) {
                mushaf.setSelection(Collections.singletonList(current.end), currentLineIds);
            }
        }
        // Both corpus jumps stay available in Révision (passive and active). Arabic-book order:
        // "suivant" sits left, pointing left; "précédent" right, pointing right. Navigation only.
        if (currentIndex > 0) {
            VerseRef previousStart = segments.get(currentIndex - 1).start;
            actions.addView(Ui.roundAction(this, "", "Passage précédent du corpus", v -> {
                currentPage = geometry.pageForVerse(previousStart);
                currentSelection = Collections.emptyList();
                currentLineIds = active ? applyActiveLandmarks(currentPage) : Collections.emptyList();
                showCurrent();
                restoreMurajaahEndpointSelectionOnCurrentPage();
                updateMurajaahActions();
            }));
        }
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider jusqu’ici", v -> finishMurajaah());
        murajaahFinishButton = (Button) validateAction.getChildAt(0);
        murajaahFinishButton.setEnabled(true);
        actions.addView(validateAction);
        if (active) {
            LinearLayout revealAction = Ui.roundAction(this, "", "Révéler", null);
            revealButton = (Button) revealAction.getChildAt(0);
            configureRevealButton(revealButton);
            actions.addView(revealAction);
            updateRevealButton();
            LinearLayout markAction = Ui.roundAction(this, "", weakMarkMode ? "Touchez le verset…" : "Marquer", v -> {
                weakMarkMode = !weakMarkMode;
                updateMurajaahActions();
            });
            actions.addView(markAction);
        }
    }

    private int murajaahSegmentIndexForPage(List<MurajaahSegment> segments, int page) {
        for (int i = 0; i < segments.size(); i++) {
            MurajaahSegment segment = segments.get(i);
            if (page >= segment.startPage && page <= segment.endPage) return i;
        }
        return -1;
    }

    private int murajaahSegmentIndexForVerse(List<MurajaahSegment> segments, VerseRef verse) {
        int ordinal = GeometryRepository.ordinal(verse);
        for (int i = 0; i < segments.size(); i++) {
            MurajaahSegment segment = segments.get(i);
            if (ordinal >= GeometryRepository.ordinal(segment.start) && ordinal <= GeometryRepository.ordinal(segment.end))
                return i;
        }
        return -1;
    }

    /**
     * A block-jump must be earned by actually touching this segment's own last verse first — not
     * by raw ordinal/page magnitude (see murajaahSegments' wraparound warning), but by segment
     * position: either murajaahActualEnd already sits in a later segment (already read past this
     * one earlier), or it sits in this exact segment and has reached its end.
     */
    private boolean murajaahValidatedThroughSegment(List<MurajaahSegment> segments, int segmentIndex) {
        if (murajaahActualEnd == null) return false;
        int actualIndex = murajaahSegmentIndexForVerse(segments, murajaahActualEnd);
        if (actualIndex < 0) return false;
        if (actualIndex > segmentIndex) return true;
        if (actualIndex < segmentIndex) return false;
        return GeometryRepository.ordinal(murajaahActualEnd)
            >= GeometryRepository.ordinal(segments.get(segmentIndex).end);
    }

    private static final class MurajaahSegment {
        final VerseRef start;
        final VerseRef end;
        final int startPage;
        final int endPage;
        MurajaahSegment(VerseRef start, VerseRef end, int startPage, int endPage) {
            this.start = start; this.end = end; this.startPage = startPage; this.endPage = endPage;
        }
    }

    /**
     * Splits the plan's traversal into maximal contiguous runs (murajaahObjectiveLabel builds the
     * same runs for display). The acquired corpus can hold several disjoint ranges and can wrap
     * mid-plan (e.g. finishing Juz 30's tail and continuing from Al-Baqara), so a later segment's
     * page or verse ordinal can be *lower* than an earlier one's. Callers must never compare
     * across segments by raw page/ordinal magnitude — only by a segment's own [start,end] bounds
     * or by its position in this list.
     */
    private List<MurajaahSegment> murajaahSegments() {
        List<VerseRef> traversal = murajaahPlan.traversalVerses;
        List<MurajaahSegment> segments = new ArrayList<>();
        if (traversal.isEmpty()) return segments;
        VerseRef segmentStart = traversal.get(0);
        VerseRef previous = segmentStart;
        for (int i = 1; i <= traversal.size(); i++) {
            VerseRef current = i < traversal.size() ? traversal.get(i) : null;
            boolean contiguous = current != null
                && GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1;
            if (!contiguous) {
                segments.add(new MurajaahSegment(segmentStart, previous,
                    geometry.pageForVerse(segmentStart), geometry.pageForVerse(previous)));
                if (current != null) segmentStart = current;
            }
            if (current != null) previous = current;
        }
        return segments;
    }

    private VerseRef murajaahNextSegmentAfterPage(int page) {
        List<MurajaahSegment> segments = murajaahSegments();
        if (segments.size() < 2) return null;
        for (int i = 0; i < segments.size(); i++) {
            MurajaahSegment segment = segments.get(i);
            if (page >= segment.startPage && page <= segment.endPage) {
                return i + 1 < segments.size() ? segments.get(i + 1).start : null;
            }
        }
        for (MurajaahSegment segment : segments) if (segment.startPage > page) return segment.start;
        return null;
    }

    private VerseRef murajaahNextSegmentAfter(VerseRef end) {
        if (end == null) return null;
        List<MurajaahSegment> segments = murajaahSegments();
        if (segments.size() < 2) return null;
        int endOrdinal = GeometryRepository.ordinal(end);
        for (int i = 0; i < segments.size(); i++) {
            MurajaahSegment segment = segments.get(i);
            if (endOrdinal >= GeometryRepository.ordinal(segment.start)
                && endOrdinal <= GeometryRepository.ordinal(segment.end)) {
                return i + 1 < segments.size() ? segments.get(i + 1).start : null;
            }
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
                .setMessage("L’objectif du jour continue plus loin (" + murajaahVerseLabel(unread) + "…). Valider maintenant clôturera la séance du jour sans le lire.")
                .setNegativeButton("Continuer la lecture", null)
                .setPositiveButton("Valider quand même", (d, w) -> completeMurajaahValidation())
                .show();
            return;
        }
        completeMurajaahValidation();
    }

    /**
     * Active and passive each advance their own independent cursor over their own corpus (see
     * activeMurajaahCorpus): active excludes "à stabiliser" material, so the two can diverge —
     * there is no guaranteed overlap between what active tests and what passive covers that same
     * day. Active's elapsed time is also excluded from the maintenance speed calibration, since a
     * masked recall pass (thinking time, reveals) runs at a different pace than plain passive
     * reading and would otherwise corrupt the per-line estimate both modes size their line targets
     * from.
     */
    private void completeMurajaahValidation(){
        boolean active = MURAJAAH_ACTIVE.equals(mode);
        long elapsed = clock.elapsedMs();
        int lines = countMurajaahLinesThrough(murajaahActualEnd);
        EligibleCorpus corpus = active ? prefs.activeMurajaahCorpus() : prefs.murajaahCorpus(geometry);
        VerseRef next = corpus.next(murajaahActualEnd);
        if (active) {
            advanceWeakVerseStreaksForActiveSession(corpus);
            String label = "Révision active · testé : " + QuranSurahNames.range(murajaahPlan.start, murajaahActualEnd)
                + " · prochain curseur " + QuranSurahNames.verse(next) + " · " + lines + "L/" + Math.max(0L, elapsed / 1000L) + "s";
            if (!prefs.completeActiveMurajaah(next, sessionDate.toString(), label)) {
                onError("Impossible d’enregistrer la validation de la Révision active.");
                return;
            }
        } else {
            SpeedCalibration.Result calibration = speedStore.calibrateMaintenance(lines, elapsed);
            String raw = HifzSpeedStore.instrumentationLabel(lines, elapsed, calibration);
            if (calibration.status == SpeedCalibration.Status.ATYPICAL) raw += "·atyp";
            String label = "Révision · réel : " + QuranSurahNames.range(murajaahPlan.start, murajaahActualEnd)
                + " · prochain curseur " + QuranSurahNames.verse(next) + " · " + raw;
            if (!prefs.completeMurajaah(next, murajaahPlan.start, murajaahActualEnd, sessionDate.toString(), label)) {
                onError("Impossible d’enregistrer la validation de la Révision.");
                return;
            }
        }
        closeClockForCompletedSession();
        renderMode();
    }

    /**
     * Only flagged verses actually covered by today's active pass (from murajaahPlan.start through
     * murajaahActualEnd, walked the same way countMurajaahLinesThrough does) have their clean-recall
     * streak touched — a flag elsewhere is left exactly as-is, since it wasn't tested today. Révéler
     * is a whole-page reveal, not per-verse, so "clean" is evaluated at page granularity: a flagged
     * verse whose page was never revealed this session counts one clean pass; a verse whose page WAS
     * revealed at any point resets its streak instead, since we can't tell more precisely which verse
     * on that page actually needed the help.
     */
    private void advanceWeakVerseStreaksForActiveSession(EligibleCorpus corpus) {
        List<VerseRef> weak = prefs.murajaahWeakVerses();
        if (weak.isEmpty()) return;
        LinkedHashSet<VerseRef> covered = new LinkedHashSet<>();
        VerseRef cursor = murajaahPlan.start;
        for (int visited = 0; visited < 6236 * 20; visited++) {
            covered.add(cursor);
            if (cursor.equals(murajaahActualEnd)) break;
            cursor = corpus.next(cursor);
        }
        List<VerseRef> clean = new ArrayList<>();
        List<VerseRef> revealed = new ArrayList<>();
        for (VerseRef verse : weak) {
            if (!covered.contains(verse)) continue;
            if (activeRevealedPages.contains(geometry.pageForVerse(verse))) revealed.add(verse);
            else clean.add(verse);
        }
        if (!clean.isEmpty() || !revealed.isEmpty()) {
            mushaf.setHighlightVerses(prefs.advanceWeakVerseStreaks(clean, revealed));
        }
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
        EligibleCorpus corpus = MURAJAAH_ACTIVE.equals(mode) ? prefs.activeMurajaahCorpus() : prefs.murajaahCorpus(geometry);
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
        if (traversal.isEmpty()) return murajaahRangeLabel(murajaahPlan.start, murajaahPlan.actualPlannedEnd);
        StringBuilder label = new StringBuilder();
        VerseRef segmentStart = traversal.get(0);
        VerseRef previous = segmentStart;
        for (int i = 1; i <= traversal.size(); i++) {
            VerseRef current = i < traversal.size() ? traversal.get(i) : null;
            boolean contiguous = current != null
                && GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1;
            if (!contiguous) {
                if (label.length() > 0) label.append(" · puis ");
                label.append(murajaahRangeLabel(segmentStart, previous));
                if (current != null) segmentStart = current;
            }
            if (current != null) previous = current;
        }
        return label.toString();
    }

    /**
     * User decision: each passage reads right-to-left like the Mushaf ("88 ← 1 البقرة"): the
     * range is a right-to-left isolate (RLI … PDI) and the arrow points left, toward An-Nās.
     */
    private String murajaahRangeLabel(VerseRef start, VerseRef end) {
        return QuranSurahNames.range(start, end);
    }

    private String murajaahVerseLabel(VerseRef ref) {
        return QuranSurahNames.name(ref.getSurah()) + " " + ref.getAyah();
    }

    private void updateMurajaahProgress() {
        if (!isMurajaahMode() || progress == null || murajaahPlan == null) return;
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
        if (!isMurajaahMode() || murajaahActualEnd == null || mushaf == null) return;
        if (geometry.pageForVerse(murajaahActualEnd) != currentPage) return;
        mushaf.setSelection(Collections.singletonList(murajaahActualEnd), currentLineIds);
    }

    private void checkpointMurajaah(long elapsed){
        if (!isMurajaahMode() || sessionCompleted) return;
        if (MURAJAAH_ACTIVE.equals(mode)) {
            prefs.setActiveMurajaahActualEnd(murajaahActualEnd);
            prefs.setActiveMurajaahPage(currentPage);
        } else {
            prefs.setMurajaahActualEnd(murajaahActualEnd);
            prefs.setMurajaahPage(currentPage);
        }
    }

    @Override public void onVerseTap(VerseRef verse){
        if (!isMurajaahMode() || murajaahPlan == null || sessionCompleted) return;
        if (MURAJAAH_ACTIVE.equals(mode) && weakMarkMode) { toggleWeakVerse(verse); return; }
        EligibleCorpus corpus = MURAJAAH_ACTIVE.equals(mode) ? prefs.activeMurajaahCorpus() : prefs.murajaahCorpus(geometry);
        if (!corpus.contains(verse)) {
            Toast.makeText(this, "Ce verset n’appartient pas encore au corpus acquis.", Toast.LENGTH_SHORT).show();
            return;
        }
        murajaahActualEnd=verse;
        updateMurajaahProgress();
        mushaf.setSelection(Collections.singletonList(verse),currentLineIds);
        checkpointMurajaah(clock.elapsedMs());
        updateMurajaahActions();
    }

    /** Single-shot: one tap while armed by "Marquer" flips that verse's weak-spot flag, then disarms. */
    private void toggleWeakVerse(VerseRef verse) {
        weakMarkMode = false;
        if (!prefs.toggleMurajaahWeakVerse(verse)) {
            onError("Impossible d’enregistrer le repère.");
            return;
        }
        mushaf.setHighlightVerses(prefs.murajaahWeakVerses());
        updateMurajaahActions();
    }

    @Override public void onPageSwipe(int delta){goPage(delta);}
    @Override public void onSemanticCueTap(String passageId) {
        if(semanticTitleDialog!=null&&semanticTitleDialog.isShowing())semanticTitleDialog.dismiss();
        semanticTitleDialog=null;
        if(ayahThemesTitles==null||passageId==null||!passageId.startsWith("AT:"))return;
        String title=ayahThemesTitles.titleForVisibleStart(currentPage,passageId.substring(3));
        if(title==null)return;
        semanticTitleDialog=SemanticTitlePopup.showEnglish(this,title,null);
        SemanticTitlePopup.positionAboveIcons(this,semanticTitleDialog,actions);
    }
    /** Apprentissage, Stabilisation, Renforcement and Consolidation share one reading focus. */
    private boolean usesReadingFocus() {
        return SABQI.equals(mode) || SABQI_TODAY_REVIEW.equals(mode) || ITQAN.equals(mode)
            || RECENT_SABQI_REVIEW.equals(mode) || LEARNING_CONSOLIDATION.equals(mode)
            || CONSOLIDATION_FINAL.equals(mode) || LEARNING_FINAL.equals(mode);
    }

    private void showCurrent(){
        hasShown=true;
        if(MURAJAAH_ACTIVE.equals(mode)) {
            // No old-page geometry redraw and no unmasked first frame on BOOX:
            // exact 100% SVG mask plus Quran-word holes precede the reveal.
            mushaf.showFreeActivePreparedPage(currentPage,currentLineIds,
                activePageLandmarks,activePageAmorces,activeAyahThemesReady);
            return;
        }
        if (isSundayFinalPageRepetition() || POST_FINAL_RECALL.equals(mode)) {
            if (currentMask == 100) {
                showSundayFinalLectureMaskPage();
                return;
            }
            sundayFinalAnchorUnavailable = false;
            mushaf.setMaskFollowsSelection(true);
            mushaf.clearSemanticCues();
            mushaf.clearPageLandmarkBoxes();
        } else if(ITQAN.equals(mode))applyItqanAnchors();
        boolean contextFocus=usesReadingFocus()&&!currentLineIds.isEmpty();
        mushaf.show(currentPage,currentSelection,currentLineIds,displayedMask(),contextFocus);
    }


    /** A Sunday finale must never count a visible pass as the required masked recall. */
    private boolean sundayFinalAnchorUnavailable;
    /** Set only by MushafView's generation-checked pageShown after atomic mask paint. */
    private boolean sundayFinalMaskedPageReady;

    private boolean isSundayFinalPageRepetition() {
        return consolidationSession != null && !consolidationSession.readyToClose()
            && ((LEARNING_FINAL.equals(mode) || CONSOLIDATION_FINAL.equals(mode))
                && consolidationSession.cycle().units().get(consolidationSession.nextUnitIndex())
                    .protocol() == ConsolidationCycleEngine.Protocol.SUNDAY_PAGE
                || RECENT_SABQI_REVIEW.equals(mode)
                && consolidationSession.cycle().units().get(consolidationSession.nextUnitIndex())
                    .protocol() == ConsolidationCycleEngine.Protocol.POST_NAS_WEEKLY_MASKED);
    }

    /**
     * Reuses exactly the Lecture > Révision active amorce mask:
     * Ayah Themes word-one cues, six exact QCF landmark words, and the
     * atomic prepared first paint in MushafView. No Al-Munīr fallback.
     * Only currentLineIds, kept separately, can earn final credits.
     */
    private void showSundayFinalLectureMaskPage() {
        java.util.List<String> pageLines = java.util.Collections.emptyList();
        org.json.JSONArray landmarks = new org.json.JSONArray();
        org.json.JSONArray anchors = new org.json.JSONArray();
        boolean ready = false;
        try {
            pageLines = geometry.lineIdsOnPage(currentPage);
            if (ayahThemesTitles != null && !pageLines.isEmpty()) {
                landmarks = WordGeometryRepository.shared(this).pageLandmarkBoxes(currentPage);
                anchors = ayahThemesTitles.forPage(currentPage);
                if (landmarks.length() == 6) {
                    ready = true;
                    for (int i = 0; i < anchors.length(); i++) {
                        org.json.JSONObject cue = anchors.getJSONObject(i);
                        if (cue.getInt("anchorWordCount") != 1
                            || cue.getJSONArray("boxes").length() != 1
                            || cue.getString("id").isEmpty()) {
                            ready = false;
                            break;
                        }
                    }
                }
            }
        } catch (Exception missingExactGeometry) {
            ready = false;
        }
        sundayFinalAnchorUnavailable = !ready;
        sundayFinalMaskedPageReady = false;
        // Never show the Arabic glyphs unmasked and then paint the mask over
        // them: BOOX retains the momentary frame as E-Ink ghosting.
        mushaf.showFreeActivePreparedPage(currentPage, pageLines, landmarks, anchors, ready);
    }

    /** True when an anchored post-An-Nās page has an amorce without exact geometry: that page
     *  then stays visible (fail open) rather than hide Quran text behind an unverified anchor. */
    private boolean itqanAnchorFailOpen;

    /**
     * Post-An-Nās maintenance recall (pass 3; legacy OPEN sessions also pass 4):
     * the hizb disappears at 100% except validated anchors — Al-Munīr amorces (audited V2.1
     * start words, SemanticPassageRepository) of every unit beginning inside this Itqān unit on
     * the shown page, cut as exact quran-ws word holes — plus the reader's permanent page cues.
     * The page's first/last three words (a Révision active aid) are deliberately not added.
     * Nothing is invented or approximated: if one of those amorces has no exact boxes, the page
     * fails open. Every other Itqān repetition (visible, deep eraser, validation) clears them.
     */
    private void applyItqanAnchors() {
        itqanAnchorFailOpen = false;
        mushaf.setLandmarkLines(null, null);
        boolean anchored = !sessionCompleted && currentMask == 100 && itqanUnit != null
            && ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, prefs.itqanRep())
            && currentLineIds != null && !currentLineIds.isEmpty();
        if (!anchored || semanticPassages == null || !semanticPassages.isAvailable()) {
            mushaf.clearSemanticCues();
            mushaf.clearPageLandmarkBoxes();
            if (anchored) itqanAnchorFailOpen = true; // anchors unavailable: never recall blind
            return;
        }
        org.json.JSONArray cues = new org.json.JSONArray();
        try {
            org.json.JSONArray page = semanticPassages.readerCuesForPage(currentPage);
            for (int i = 0; i < page.length(); i++) {
                org.json.JSONObject cue = page.getJSONObject(i);
                SemanticPassageRepository.Cue meta = semanticPassages.cue(cue.getString("id"));
                if (meta == null || !ItqanMaintenancePolicy.amorceInsideUnit(
                        meta.startVerse, itqanUnit.start, itqanUnit.end)) continue;
                if (cue.getJSONArray("boxes").length() != cue.getInt("anchorWordCount")) {
                    itqanAnchorFailOpen = true;
                    break;
                }
                cues.put(cue);
            }
        } catch (org.json.JSONException malformed) {
            itqanAnchorFailOpen = true;
        }
        mushaf.clearPageLandmarkBoxes();
        // A unit that has no Ibn Kathir beginning on the displayed page must
        // remain visible; a totally covered page is not a valid recall aid.
        if (cues.length() == 0) itqanAnchorFailOpen = true;
        if (itqanAnchorFailOpen) { mushaf.clearSemanticCues(); return; }
        mushaf.setSemanticCues(cues, true, false);
    }

    /** Fail open on active-recall pages with incomplete QCF landmark/cue geometry too. */
    private int displayedMask() {
        if (ITQAN.equals(mode) && itqanAnchorFailOpen) return 0;
        if ((isSundayFinalPageRepetition()||POST_FINAL_RECALL.equals(mode)) && sundayFinalAnchorUnavailable) return 0;
        if (MURAJAAH_ACTIVE.equals(mode) && currentMask > 0
                && !activeAyahThemesReady) return 0;
        return currentMask;
    }

    private void goPage(int delta) {
        int target=Math.max(1,Math.min(604,currentPage+delta));
        boolean limited=SABQI.equals(mode)||SABQI_TODAY_REVIEW.equals(mode)||ITQAN.equals(mode)
            ||RECENT_SABQI_REVIEW.equals(mode)||LEARNING_CONSOLIDATION.equals(mode)
            ||CONSOLIDATION_FINAL.equals(mode)||LEARNING_FINAL.equals(mode)
            ||POST_FINAL_RECALL.equals(mode);
        if(limited)target=Math.max(unitFirstPage,Math.min(unitLastPage,target));
        if(target==currentPage)return;closeAudio();currentPage=target;
        if(MURAJAAH_ACTIVE.equals(mode))currentLineIds=applyActiveLandmarks(currentPage);
        showCurrent();
        boolean groupedCycle=RECENT_SABQI_REVIEW.equals(mode)||LEARNING_CONSOLIDATION.equals(mode)
            ||CONSOLIDATION_FINAL.equals(mode)||LEARNING_FINAL.equals(mode);
        if(groupedCycle&&consolidationSession!=null&&!consolidationSession.readyToClose())updateGroupedCycleRepAction();
        if(isMurajaahMode()&&murajaahPlan!=null&&!sessionCompleted)updateMurajaahActions();
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
                if(currentMask<=0)return false;revealedThisRep=true;
                if(MURAJAAH_ACTIVE.equals(mode)){
                    activeRevealedPages.add(currentPage);
                    if(!revealDoesNotMarkHintShown){
                        revealDoesNotMarkHintShown=true;
                        Toast.makeText(this,"Révéler n’enregistre rien : utilisez « Marquer » pour signaler un verset difficile.",Toast.LENGTH_LONG).show();
                    }
                }
                view.setPressed(true);mushaf.setMask(0);return true;
            }
            if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL){
                view.setPressed(false);mushaf.setMask(displayedMask());if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;
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
        return SABQI_TODAY_REVIEW.equals(mode) || MURAJAAH.equals(mode) || MURAJAAH_ACTIVE.equals(mode);
    }
    private boolean isMurajaahMode() {
        return MURAJAAH.equals(mode) || MURAJAAH_ACTIVE.equals(mode);
    }
    private int targetMinutes(){
    SessionKind kind;
    if (SABQI.equals(mode)) kind = SessionKind.SABQI_NEW;
    else if (SABQI_TODAY_REVIEW.equals(mode)) kind = SessionKind.SABQI_TODAY_REVIEW;
    else if (ITQAN.equals(mode)) kind = SessionKind.ITQAN;
    else if (RECENT_SABQI_REVIEW.equals(mode) || LEARNING_CONSOLIDATION.equals(mode)
        || CONSOLIDATION_FINAL.equals(mode) || LEARNING_FINAL.equals(mode)) kind = SessionKind.RECENT_SABQI_REVIEW;
    else if (MURAJAAH_ACTIVE.equals(mode)) kind = SessionKind.ACTIVE_MURAJAAH;
    else kind = SessionKind.OLD_ITQAN_MURAJAAH;
    if (kind == SessionKind.OLD_ITQAN_MURAJAAH || kind == SessionKind.ACTIVE_MURAJAAH) {
        // User decision 22 October 2026: both revisions use the identical 30/45+
        // minute coverage policy, starting now. The active mode still has its own
        // acquired-only corpus, masked reader, clock, independent cursor, and never
        // feeds masked-recall elapsed time into passive speed calibration.
        return MaintenanceCoveragePolicy.minutes(
            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
    }
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
        if (POST_FINAL_RECALL.equals(mode)) return "Lecture cumulative masquée";
        if (MURAJAAH_ACTIVE.equals(mode)) return "Révision active";
        return "Révision";
    }
    @Override public void onReady(){
        if(StructuredSessionPolicy.shouldInitialReaderShow(hasShown, sessionCompleted))showCurrent();
        else if(!hasShown&&sessionCompleted&&!awaitingValidation)showClosingFatiha();
    }
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){
        currentPage=page;
        annotationOverlay.setPage(page);
        if ((isSundayFinalPageRepetition() && currentMask == 100)
                || (POST_FINAL_RECALL.equals(mode) && currentPostFinalReview != null
                    && page == currentPostFinalReview.page && currentMask == 100)) {
            // This callback happens only after the prepared QCF word-hole layer
            // exists; an unloaded/visible frame must not earn a masked pass.
            sundayFinalMaskedPageReady = !sundayFinalAnchorUnavailable;
            if(POST_FINAL_RECALL.equals(mode))updatePostFinalRecallAction();
            else updateGroupedCycleRepAction();
        }
        if(isMurajaahMode()&&!sessionCompleted){
            if(MURAJAAH_ACTIVE.equals(mode)){
                currentLineIds=applyActiveLandmarks(page);
                prefs.setActiveMurajaahPage(page);
            } else {
                prefs.setMurajaahPage(page);
            }
            restoreMurajaahEndpointSelectionOnCurrentPage();
        }
        if(semanticTitleDialog!=null&&semanticTitleDialog.isShowing())semanticTitleDialog.dismiss();
        semanticTitleDialog=null;
        boolean titleTargets=isMurajaahMode() || (ITQAN.equals(mode) && currentMask==100
            && itqanUnit!=null && ItqanMaintenancePolicy.anchoredRecallRep(itqanRegime,prefs.itqanRep()));
        if(titleTargets)AyahThemesReviewDisplay.apply(this,mushaf,ayahThemesTitles,page);
    }
    @Override protected void onResume(){
        super.onResume();
        if(clock==null)return;
        clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&(!timedSessionLimitReached||isMurajaahMode()))clock.resume();
    }
    @Override protected void onPause(){
        if(clock==null){super.onPause();return;}
        long elapsed=clock.pause();
        if(sessionCompleted&&!awaitingValidation)prefs.setElapsedFor(mode,0L);
        else{prefs.setElapsedFor(mode,elapsed);checkpointMurajaah(elapsed);}
        super.onPause();
    }
    @Override protected void onDestroy(){if(semanticTitleDialog!=null&&semanticTitleDialog.isShowing())semanticTitleDialog.dismiss();closeAudio();if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(clock==null)return super.onKeyDown(code,e);if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
