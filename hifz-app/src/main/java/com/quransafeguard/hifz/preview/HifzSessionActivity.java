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

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Structured Sabqi / Itqan / Murajaah session using the independent domain engine. */
public final class HifzSessionActivity extends android.app.Activity implements MushafView.Listener {
    public static final String EXTRA_MODE = "mode";
    public static final String SABQI = "SABQI";
    public static final String SABQI_TODAY_REVIEW = "SABQI_TODAY_REVIEW";
    public static final String ITQAN = "ITQAN";
    public static final String RECENT_SABQI_REVIEW = "RECENT_SABQI_REVIEW";
    public static final String MURAJAAH = "MURAJAAH";

    private String mode;
    private HifzPrefs prefs;
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
    private int recentReviewIndex;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !SABQI_TODAY_REVIEW.equals(mode) && !ITQAN.equals(mode)
                && !RECENT_SABQI_REVIEW.equals(mode) && !MURAJAAH.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);
        recentReviewIndex = prefs.recentSabqiReviewIndex();
        murajaahActualEnd = prefs.murajaahActualEnd();
        clock = new SessionClock(prefs.elapsedFor(mode), elapsed -> {
            if (timerText != null) {
                timerText.setText(SessionClock.format(elapsed) + " / " + String.format(Locale.ROOT, "%02d:00", targetMinutes()));
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
            String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            program.setText(displayModeName() + " · état à vérifier");
            progress.setText(detail);
        }
    }

    private void renderSabqi() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiDate())) {
            sessionCompleted = true;
            program.setText("Sabqi · séance validée");
            progress.setText(prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : prefs.lastSabqiLabel());
            return;
        }
        int startLimit = geometry.firstLineIndex(prefs.sabqiStart());
        int endLimit = geometry.lastLineIndex(prefs.sabqiEnd());
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) { cursor = startLimit; prefs.setSabqiLineCursor(cursor); }
        if (cursor < startLimit || cursor > endLimit) {
            sessionCompleted = true;
            program.setText("Sabqi · curseur à repositionner");
            progress.setText(prefs.sabqiStart() + " → " + prefs.sabqiEnd());
            return;
        }
        if (cursor + PreviewConfig.SABQI_LINES - 1 > endLimit) {
            sessionCompleted = true;
            int remaining = endLimit - cursor + 1;
            program.setText("Sabqi · fin de plage");
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
            program.setText("Sabqi · " + sabqiBlock.verseLabel() + " · 5 lignes");
            progress.setText("37/37 · prêt à valider · révélations " + prefs.sabqiAssisted());
            showCurrent();
            addRoundAction("✓","Valider",v->validateSabqi());
            return;
        }
        sessionCompleted = false;
        currentMask = PreviewConfig.sabqiMaskForNextRep(rep);
        program.setText("Sabqi · " + sabqiBlock.verseLabel() + " · 5 lignes");
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
        if(!prefs.setSabqiProgress(rep,reveals)){onError("Impossible d’enregistrer la répétition Sabqi.");return;}
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
        String label=sabqiBlock.verseLabel()+" · 37/37 · révélations "+prefs.sabqiAssisted();
        boolean ok = prefs.completeSabqiBlock(
            sabqiBlock.startLineIndex,
            sabqiBlock.endLineIndex,
            sabqiBlock.endLineIndex+1,
            LocalDate.now().toString(),
            label
        );
        if (!ok) { onError("Impossible d’enregistrer atomiquement la validation Sabqi."); return; }
        rebalanceRecentWindow();
        awaitingValidation=false;
        closeClockForCompletedSession();
        mushaf.cycleCompleted();
        renderMode();
    }

    /** Sliding 30-minute recent-Sabqi window; promotion is capacity-driven and preserves whole verses. */
    private void rebalanceRecentWindow() {
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) return;
        int recentWindowMinutes = HifzSchedule.INSTANCE.planFor(DayOfWeek.SATURDAY).getMorning().getTargetMinutes();
        int capacity = Math.max(PreviewConfig.SABQI_LINES,
            (int)Math.floor(recentWindowMinutes * 60.0 / prefs.recentSecondsPerLine()));
        int total = 0;
        for (HifzPrefs.RecentSabqi item : recent) total += Math.max(0, item.endLine - item.startLine + 1);
        if (total <= capacity) return;

        int overflow = total - capacity;
        int removedLines = 0;
        int blocksToRemove = 0;
        int oldestStart = recent.get(0).startLine;
        int safeEnd = -1;
        for (HifzPrefs.RecentSabqi item : recent) {
            removedLines += Math.max(0, item.endLine - item.startLine + 1);
            blocksToRemove++;
            GeometryRepository.FiveLineBlock block = geometry.fiveLineBlock(item.startLine);
            if (removedLines >= overflow && !block.endsInsideVerse) {
                safeEnd = item.endLine;
                break;
            }
        }
        if (safeEnd < oldestStart || blocksToRemove <= 0) return;

        ArrayList<VerseRef> complete = new ArrayList<>(geometry.versesFullyCoveredByLines(oldestStart, safeEnd));
        if (complete.isEmpty()) return;
        if (!prefs.addPromotedVerses(complete)) {
            onError("Impossible d’enregistrer la promotion de la fenêtre Sabqi récent.");
            return;
        }
        for (int i = 0; i < blocksToRemove; i++) prefs.removeFirstRecentSabqi();
    }

    private void renderSabqiTodayReview() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Sabqi du jour · séance validée");
            progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : prefs.lastSabqiTodayReviewLabel());
            return;
        }
        if (!today.equals(prefs.sabqiTodayReviewDate())) {
            sessionCompleted = true;
            program.setText("Sabqi du jour · aucun bloc");
            progress.setText("Validez d’abord les 5 lignes du matin.");
            return;
        }
        int start = prefs.sabqiTodayReviewStartLine();
        int end = prefs.sabqiTodayReviewEndLine();
        if (start < 0 || end < start) throw new IllegalStateException("Bloc Sabqi du jour absent");
        GeometryRepository.FiveLineBlock block = geometry.fiveLineBlock(start);
        currentPage = geometry.line(start).page;
        unitFirstPage = currentPage;
        unitLastPage = geometry.line(end).page;
        currentSelection = block.verses;
        currentLineIds = block.lineIds;
        currentMask = 0;
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        program.setText("Sabqi du jour · " + block.verseLabel() + " · 5 lignes");
        progress.setText(timedSessionLimitReached ? "30 min atteintes" : "Répétez les mêmes 5 lignes.");
        showCurrent();
        if (!timedSessionLimitReached) addRoundAction("↻", "Répétition", v -> renderMode());
    }

    private void renderRecentSabqiReview() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) {
            sessionCompleted = true;
            program.setText("Sabqi récent · séance validée");
            progress.setText("30 min terminées");
            return;
        }
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) {
            if (completeEmptyRecentSabqiSession()) { renderMode(); return; }
            sessionCompleted = true;
            program.setText("Sabqi récent · aucun passage");
            progress.setText("Aucun ancien Itqān n’est ouvert à la place.");
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
        program.setText("Sabqi récent · " + block.verseLabel());
        progress.setText("Bloc " + (recentReviewIndex + 1) + "/" + recent.size() + " · boucle 30 min");
        showCurrent();
        if (!timedSessionLimitReached) {
            addRoundAction("✓", "Revu", v -> advanceRecentReview());
            addRoundAction("!", "À renforcer", v -> advanceRecentReview());
        }
    }

    private void advanceRecentReview() {
        if (!takeRepLock()) return;
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) { renderMode(); return; }
        recentReviewIndex = PreviewConfig.nextRecentReviewIndex(recentReviewIndex, recent.size());
        prefs.setRecentSabqiReviewIndex(recentReviewIndex);
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
            prefs.completeSabqiTodayReview(today, "Mêmes 5 lignes · 30 min");
            closeClockForCompletedSession();
            return true;
        }
        if (RECENT_SABQI_REVIEW.equals(mode)
                && !today.equals(prefs.lastRecentSabqiReviewDate())) {
            timedSessionLimitReached = true;
            prefs.completeRecentSabqiReview(today, recentReviewIndex, "Sabqi récent · 30 min");
            closeClockForCompletedSession();
            return true;
        }
        return false;
    }

    /** Empty recent work is completed explicitly and never falls through to old Itqan. */
    private boolean completeEmptyRecentSabqiSession() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastRecentSabqiReviewDate())) return false;
        boolean ok = prefs.completeRecentSabqiReview(today, 0, "Aucun Sabqi récent");
        if (ok) closeClockForCompletedSession();
        return ok;
    }

    private void onTimedSessionLimit(long elapsed) {
        if (timedSessionLimitReached) return;
        timedSessionLimitReached = true;
        long saved = clock.pause();
        prefs.setElapsedFor(mode, Math.max(elapsed, saved));
        String today = LocalDate.now().toString();
        if (SABQI_TODAY_REVIEW.equals(mode)) {
            prefs.completeSabqiTodayReview(today, "Mêmes 5 lignes · 30 min");
            closeClockForCompletedSession();
            renderMode();
        } else if (RECENT_SABQI_REVIEW.equals(mode)) {
            prefs.completeRecentSabqiReview(today, recentReviewIndex, "Sabqi récent · 30 min");
            closeClockForCompletedSession();
            renderMode();
        } else if (MURAJAAH.equals(mode)) {
            renderMode();
        }
    }

    private void renderItqan() {
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastItqanDate())){
            sessionCompleted = true;
            program.setText("Itqān · unité validée");
            progress.setText(prefs.lastItqanLabel().isEmpty()?"×"+PreviewConfig.ITQAN_TOTAL_REPS+" terminé":prefs.lastItqanLabel());
            return;
        }
        EligibleCorpus corpus=prefs.itqanWorkCorpus();
        if (!prefs.isItqanCursorValid()) {
            sessionCompleted = true;
            program.setText("Itqān · curseur à repositionner");
            progress.setText("Vérifiez les plages Itqān.");
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
            itqanUnit=geometry.eligiblePageUnit(prefs.itqanCursor(),corpus);
            currentPage=itqanUnit.page;unitFirstPage=unitLastPage=currentPage;
        }
        currentSelection=itqanUnit.verses;currentLineIds=itqanUnit.lineIds;
        if(rep>=PreviewConfig.ITQAN_TOTAL_REPS){
            awaitingValidation=true;sessionCompleted=true;clock.pause();currentMask=0;
            program.setText("Itqān · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+PreviewConfig.ITQAN_TOTAL_REPS);
            progress.setText(PreviewConfig.ITQAN_TOTAL_REPS+"/"+PreviewConfig.ITQAN_TOTAL_REPS+" · prêt à valider · révélations "+prefs.itqanAssisted());
            showCurrent();addRoundAction("✓","Valider",v->validateItqan());return;
        }
        sessionCompleted=false;
        currentMask=PreviewConfig.itqanMaskForNextRep(rep);
        program.setText("Itqān · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+PreviewConfig.ITQAN_TOTAL_REPS);
        updateItqanProgress(rep,prefs.itqanAssisted());showCurrent();
        addRoundAction("↻","Répétition",v->completeItqanRep());
        LinearLayout revealAction = Ui.roundAction(this,"","Révéler",null);
        revealButton=(Button)revealAction.getChildAt(0);
        configureRevealButton(revealButton);
        actions.addView(revealAction);
        updateRevealButton();
    }

    private void updateItqanProgress(int rep,int reveals){
        progress.setText(Math.min(rep+1,PreviewConfig.ITQAN_TOTAL_REPS)+"/"+PreviewConfig.ITQAN_TOTAL_REPS+" · masque "+currentMask+"% · révélations "+reveals);
        eink.local(progress);
    }

    private void completeItqanRep(){
        if (!takeRepLock()) return;
        boolean revealed=consumeReveal();
        int rep=prefs.itqanRep();if(rep>=PreviewConfig.ITQAN_TOTAL_REPS)return;
        int oldMask=currentMask;rep++;int reveals=prefs.itqanAssisted()+(revealed?1:0);
        if(!prefs.setItqanProgress(rep,reveals,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition Itqān.");return;}
        if(rep>=PreviewConfig.ITQAN_TOTAL_REPS){
            currentMask=0;mushaf.setMask(0);
            long elapsed=clock.pause();prefs.setElapsedFor(mode,elapsed);
            awaitingValidation=true;sessionCompleted=true;renderMode();return;
        }
        currentMask=PreviewConfig.itqanMaskForNextRep(rep);if(currentMask!=oldMask)mushaf.setMask(currentMask);
        updateRevealButton();if(currentPage!=unitFirstPage){currentPage=unitFirstPage;showCurrent();}
        updateItqanProgress(rep,reveals);
    }

    private void validateItqan(){
        if(itqanUnit==null||prefs.itqanRep()<PreviewConfig.ITQAN_TOTAL_REPS)return;
        EligibleCorpus corpus = prefs.itqanWorkCorpus();
        VerseRef next = corpus.nextAnchored(itqanUnit.end, prefs.itqanRotationStart());
        String label=itqanUnit.start+" → "+itqanUnit.end+" · ×"+PreviewConfig.ITQAN_TOTAL_REPS+" · révélations "+prefs.itqanAssisted();
        boolean ok=prefs.completeItqanUnitAndConsolidate(itqanUnit.start,itqanUnit.end,next,LocalDate.now().toString(),label);
        if(!ok){onError("Impossible d’enregistrer atomiquement la validation Itqān.");return;}
        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void renderMurajaah(){
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastMurajaahDate())) {
            sessionCompleted = true;
            program.setText("Murājaʿah · séance validée");
            progress.setText(prefs.lastMurajaahLabel().isEmpty() ? "Curseur sauvegardé" : prefs.lastMurajaahLabel());
            return;
        }
        if (!prefs.isMurajaahCursorValid()) {
            sessionCompleted = true;
            program.setText("Murājaʿah · curseur à vérifier");
            progress.setText("Le corpus consolidé ne contient pas ce curseur.");
            return;
        }
        sessionCompleted = false;
        timedSessionLimitReached = PreviewConfig.timedSessionComplete(clock.elapsedMs(), targetMinutes());
        EligibleCorpus corpus = prefs.murajaahCorpus();
        int lines = Math.max(1, (int)Math.floor(targetMinutes() * 60.0 / prefs.murajaahSecondsPerLine()));
        murajaahPlan = geometry.planEligibleLines(prefs.murajaahCursor(), lines, corpus);
        murajaahActualEnd = prefs.murajaahActualEnd();
        currentPage = geometry.pageForVerse(murajaahPlan.start);
        currentSelection = murajaahPlan.traversalVerses;
        currentLineIds = Collections.emptyList();
        currentMask = 0;
        program.setText("Murājaʿah · " + murajaahPlan.start + " → " + murajaahPlan.actualPlannedEnd);
        progress.setText(timedSessionLimitReached
            ? (murajaahActualEnd == null ? "Durée atteinte · touchez le dernier verset." : "Fin réelle · " + murajaahActualEnd)
            : "Corpus Itqān consolidé · " + targetMinutes() + " min");
        showCurrent();
        if (murajaahActualEnd != null) {
            mushaf.setSelection(Collections.singletonList(murajaahActualEnd),
                geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd));
        }
        LinearLayout validateAction = Ui.roundAction(this, "", "Valider", v -> finishMurajaah());
        Button finish = (Button) validateAction.getChildAt(0);
        finish.setEnabled(timedSessionLimitReached && murajaahActualEnd != null);
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
        VerseRef itqanBefore = prefs.itqanCursor();
        EligibleCorpus corpus = prefs.murajaahCorpus();
        VerseRef next = corpus.next(murajaahActualEnd);
        long elapsed = clock.elapsedMs();
        calibrateOldSpeed(murajaahPlan.start, murajaahActualEnd, elapsed);
        String label = "Réel : " + murajaahPlan.start + " → " + murajaahActualEnd + " · prochain curseur " + next;
        boolean ok = prefs.completeMurajaah(next, LocalDate.now().toString(), label);
        if (!ok) { onError("Impossible d’enregistrer la validation Murājaʿah."); return; }
        if (!prefs.itqanCursor().equals(itqanBefore)) {
            prefs.setItqanCursor(itqanBefore);
            onError("État Murājaʿah incohérent annulé : curseur Itqān restauré.");
            return;
        }
        closeClockForCompletedSession();
        renderMode();
    }

    private void calibrateOldSpeed(VerseRef start,VerseRef end,long elapsedMs){
        if(GeometryRepository.ordinal(end)<GeometryRepository.ordinal(start))return;
        int lines=geometry.lineCountForVerseRange(start,end);
        if(lines<PreviewConfig.SPEED_MIN_LINES || elapsedMs<PreviewConfig.SPEED_MIN_SECONDS*1000L)return;
        double measured=(elapsedMs/1000.0)/lines,old=prefs.murajaahSecondsPerLine();
        prefs.setMurajaahSecondsPerLine(smoothedClamped(old,measured));
    }

    private double smoothedClamped(double old,double measured){
        double blended=0.7*old+0.3*measured;
        double low=old*(1.0-PreviewConfig.SPEED_MAX_CHANGE_RATIO),high=old*(1.0+PreviewConfig.SPEED_MAX_CHANGE_RATIO);
        return Math.max(low,Math.min(high,blended));
    }

    private void checkpointMurajaah(long elapsed){
        if (!MURAJAAH.equals(mode) || sessionCompleted) return;
        prefs.setMurajaahActualEnd(murajaahActualEnd);
    }

    @Override public void onVerseTap(VerseRef verse){
        if(MURAJAAH.equals(mode)&&murajaahPlan!=null&&prefs.murajaahCorpus().contains(verse)){
            murajaahActualEnd=verse;
            progress.setText("Fin réelle · "+verse);
            eink.local(progress);mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
            checkpointMurajaah(clock.elapsedMs());
            renderMode();
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

    private void closeClockForCompletedSession(){
        sessionCompleted=true;awaitingValidation=false;clock.pause();clock.reset();prefs.setElapsedFor(mode,0L);prefs.clearMaskEntropy(mode);
    }
    private boolean isTimedMode() {
        return SABQI_TODAY_REVIEW.equals(mode) || RECENT_SABQI_REVIEW.equals(mode) || MURAJAAH.equals(mode);
    }
    private int scheduledTargetMinutes(SessionKind kind) {
        DailyPlan plan = HifzSchedule.INSTANCE.planFor(LocalDate.now().getDayOfWeek());
        if (plan.getMorning().getKind() == kind) return plan.getMorning().getTargetMinutes();
        if (plan.getEvening().getKind() == kind) return plan.getEvening().getTargetMinutes();
        throw new IllegalStateException("Mode " + kind + " absent du planning " + LocalDate.now().getDayOfWeek());
    }
    private int targetMinutes(){
        if (SABQI.equals(mode)) return PreviewConfig.SABQI_MINUTES_WORKING;
        if (SABQI_TODAY_REVIEW.equals(mode)) return scheduledTargetMinutes(SessionKind.SABQI_TODAY_REVIEW);
        if (ITQAN.equals(mode)) return PreviewConfig.ITQAN_MINUTES_WORKING;
        if (RECENT_SABQI_REVIEW.equals(mode)) return scheduledTargetMinutes(SessionKind.RECENT_SABQI_REVIEW);
        return scheduledTargetMinutes(SessionKind.OLD_ITQAN_MURAJAAH);
    }
    private String displayModeName(){
        if (SABQI.equals(mode)) return "Sabqi";
        if (SABQI_TODAY_REVIEW.equals(mode)) return "Sabqi du jour";
        if (ITQAN.equals(mode)) return "Itqān";
        if (RECENT_SABQI_REVIEW.equals(mode)) return "Sabqi récent";
        return "Murājaʿah";
    }
    @Override public void onReady(){if(!hasShown)showCurrent();}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){currentPage=page;}
    @Override protected void onResume(){super.onResume();if(!sessionCompleted&&!awaitingValidation&&!timedSessionLimitReached)clock.resume();}
    @Override protected void onPause(){
        long elapsed=clock.pause();
        if(sessionCompleted&&!awaitingValidation)prefs.setElapsedFor(mode,0L);
        else{prefs.setElapsedFor(mode,elapsed);checkpointMurajaah(elapsed);}
        super.onPause();
    }
    @Override protected void onDestroy(){closeAudio();if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
