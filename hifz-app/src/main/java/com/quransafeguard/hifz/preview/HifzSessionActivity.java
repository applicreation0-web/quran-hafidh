package com.quransafeguard.hifz.preview;

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
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Structured Sabqi / Itqan / Murajaah session using the independent domain engine. */
public final class HifzSessionActivity extends android.app.Activity implements MushafView.Listener {
    public static final String EXTRA_MODE = "mode";
    public static final String SABQI = "SABQI";
    public static final String ITQAN = "ITQAN";
    public static final String MURAJAAH = "MURAJAAH";

    private String mode;
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private MushafView mushaf;
    private TextView heading, program, progress, timerText;
    private LinearLayout actions;
    private Button prevPage, nextPage;
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
    private boolean murajaahBlockB;
    private boolean repActionLocked;
    private boolean hasShown;
    private boolean sessionCompleted;
    private boolean awaitingValidation;
    private int unitFirstPage = 1;
    private int unitLastPage = 1;
    private boolean revealedThisRep;
    private Button revealButton;
    private int recentLinesDone;
    private long murajaahBlockAElapsedMs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !ITQAN.equals(mode) && !MURAJAAH.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);
        murajaahBlockB = MURAJAAH.equals(mode) && "B".equals(prefs.murajaahPhase());
        recentLinesDone = prefs.murajaahRecentLinesDone();
        murajaahBlockAElapsedMs = prefs.murajaahBlockAElapsedMs();
        murajaahActualEnd = prefs.murajaahActualEnd();
        clock = new SessionClock(prefs.elapsedFor(mode), elapsed -> {
            if (timerText != null) timerText.setText("Actif " + SessionClock.format(elapsed) + " · repère " + targetMinutes() + " min");
            long bucket = elapsed / 5_000L;
            if (bucket != lastCheckpointBucket) {
                lastCheckpointBucket = bucket;
                prefs.setElapsedFor(mode, elapsed);
                checkpointMurajaah(elapsed);
            }
            if (MURAJAAH.equals(mode) && !murajaahBlockB
                    && elapsed >= PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING * 60_000L) {
                transitionToBlockB(elapsed);
            }
        });
        buildUi();
        renderMode();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,8),Ui.dp(this,3),Ui.dp(this,8),Ui.dp(this,2));
        top.addView(Ui.roundButton(this,"‹","Retour",v->finish()));
        heading = Ui.text(this, displayModeName(), 17, true); Ui.weight(heading,1f); heading.setGravity(Gravity.CENTER); top.addView(heading); root.addView(top);
        program = Ui.text(this,"Chargement du programme…",13.5f,true); program.setPadding(Ui.dp(this,10),1,Ui.dp(this,10),1); root.addView(program);
        timerText = Ui.text(this,"",11.5f,false); timerText.setPadding(Ui.dp(this,10),1,Ui.dp(this,10),1); root.addView(timerText);
        progress = Ui.text(this,"",12.5f,false); progress.setPadding(Ui.dp(this,10),1,Ui.dp(this,10),Ui.dp(this,3)); root.addView(progress);
        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        actions = Ui.row(this); actions.setGravity(Gravity.CENTER); actions.setPadding(Ui.dp(this,6),Ui.dp(this,2),Ui.dp(this,6),Ui.dp(this,2)); root.addView(actions);

        // Arabic-book direction: next canonical page (+1) is visually on the LEFT; previous (-1) on the RIGHT.
        LinearLayout nav=Ui.row(this);nav.setGravity(Gravity.CENTER);nav.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),Ui.dp(this,5));
        nextPage=Ui.roundButton(this,"›","Page suivante",v->goPage(1));
        prevPage=Ui.roundButton(this,"‹","Page précédente",v->goPage(-1));
        nav.addView(nextPage);
        if (new HifzAudioGate(this).available()) nav.addView(Ui.roundButton(this,"♪","Audio",v->openAudio()));
        nav.addView(prevPage);root.addView(nav);
        setContentView(root);
        Ui.respectSystemBars(this, root, 0, 0, 0, 0);
    }

    private void renderMode() {
        actions.removeAllViews();
        revealButton = null;
        awaitingValidation = false;
        unitFirstPage = 1;
        unitLastPage = 1;
        try {
            if (SABQI.equals(mode)) renderSabqi();
            else if (ITQAN.equals(mode)) renderItqan();
            else renderMurajaah();
        } catch (RuntimeException error) {
            sessionCompleted = true;
            clock.pause();
            String detail = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            program.setText(displayModeName() + " — état illisible");
            progress.setText("Détail : " + detail + "\nAucune donnée n’a été modifiée. Vérifiez les paramètres du parcours.");
        }
        updatePageButtons();
    }

    private void renderSabqi() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiDate())) {
            sessionCompleted = true;
            program.setText("Sabqi — séance validée");
            progress.setText((prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : prefs.lastSabqiLabel())
                + "\nLe bloc reste dans la fenêtre Sabqi récent jusqu’à saturation des 30 min.");
            return;
        }
        int startLimit = geometry.firstLineIndex(prefs.sabqiStart());
        int endLimit = geometry.lastLineIndex(prefs.sabqiEnd());
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) { cursor = startLimit; prefs.setSabqiLineCursor(cursor); }
        if (cursor < startLimit || cursor > endLimit) {
            sessionCompleted = true;
            program.setText("Sabqi — curseur hors de la plage configurée");
            progress.setText("Plage : " + prefs.sabqiStart() + " → " + prefs.sabqiEnd()
                + "\nChoisissez explicitement un nouveau début dans Paramètres.");
            return;
        }
        if (cursor + PreviewConfig.SABQI_LINES - 1 > endLimit) {
            sessionCompleted = true;
            int remaining = endLimit - cursor + 1;
            program.setText("Sabqi — fin de plage configurée");
            progress.setText("Il reste " + remaining + " ligne(s), moins que le bloc obligatoire de 5 lignes. "
                + "Aucune borne n’est dépassée silencieusement : ajustez la fin Sabqi si vous souhaitez continuer.");
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
            progress.setText("37/37 · prêt à valider · révélations " + prefs.sabqiAssisted()
                + "\nLe curseur et Sabqi récent ne bougent qu’après validation.");
            showCurrent();
            addRoundAction("✓","Valider",v->validateSabqi());
            return;
        }
        sessionCompleted = false;
        currentMask = PreviewConfig.sabqiMaskForNextRep(rep);
        program.setText("Sabqi · " + sabqiBlock.verseLabel() + " · 5 lignes"
            + (unitLastPage > unitFirstPage ? " · pages " + unitFirstPage + "–" + unitLastPage : ""));
        updateSabqiProgress(rep, prefs.sabqiAssisted());
        showCurrent();
        addRoundAction("↻","Répétition",v->completeSabqiRep());
        revealButton = Ui.roundButton(this,"◉","Révéler",null);configureRevealButton(revealButton);actions.addView(revealButton);updateRevealButton();
    }

    private void updateSabqiProgress(int rep, int reveals) {
        progress.setText("Suivante " + Math.min(rep+1,PreviewConfig.SABQI_TOTAL_REPS) + "/37 · masque " + currentMask + "% · révélations " + reveals);
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
        if (currentPage != unitFirstPage) { currentPage = unitFirstPage; showCurrent(); updatePageButtons(); }
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

    /**
     * Sliding recent-Sabqi window. Capacity is 30 minutes at the measured recent-review speed.
     * The queue remains chronological because review actions never remove/reorder it. When a new
     * Sabqi makes the window too large, remove the oldest complete five-line groups up to a safe
     * verse boundary and promote only verses fully covered by those removed lines.
     * A few extra recent lines are intentionally allowed when verse integrity requires it.
     */
    private void rebalanceRecentWindow() {
        List<HifzPrefs.RecentSabqi> recent = prefs.recentSabqi();
        if (recent.isEmpty()) return;
        int capacity = Math.max(PreviewConfig.SABQI_LINES,
            (int)Math.floor(PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING * 60.0 / prefs.recentSecondsPerLine()));
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

    private void renderItqan() {
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastItqanDate())){
            sessionCompleted = true;
            program.setText("Itqān — unité validée");
            progress.setText((prefs.lastItqanLabel().isEmpty()?"×"+PreviewConfig.ITQAN_TOTAL_REPS+" terminé":prefs.lastItqanLabel())+"\nLe curseur est sauvegardé pour le prochain créneau.");
            return;
        }
        EligibleCorpus corpus=prefs.corpus();
        if (!prefs.isItqanCursorValid()) {
            sessionCompleted = true;
            program.setText("Itqān — curseur hors des plages");
            progress.setText("Les plages ont changé. Aucun repositionnement automatique : choisissez explicitement le début de rotation dans Paramètres.");
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
            progress.setText(PreviewConfig.ITQAN_TOTAL_REPS+"/"+PreviewConfig.ITQAN_TOTAL_REPS+" · prêt à valider · révélations "+prefs.itqanAssisted()+"\nLe curseur ne bouge qu’après validation.");
            showCurrent();addRoundAction("✓","Valider",v->validateItqan());return;
        }
        sessionCompleted=false;
        currentMask=PreviewConfig.itqanMaskForNextRep(rep);
        program.setText("Itqān · "+itqanUnit.start+" → "+itqanUnit.end+" · ×"+PreviewConfig.ITQAN_TOTAL_REPS+" · corpus cyclique");
        updateItqanProgress(rep,prefs.itqanAssisted());showCurrent();
        addRoundAction("↻","Répétition",v->completeItqanRep());
        revealButton=Ui.roundButton(this,"◉","Révéler",null);configureRevealButton(revealButton);actions.addView(revealButton);updateRevealButton();
    }

    private void updateItqanProgress(int rep,int reveals){
        progress.setText("Suivante "+Math.min(rep+1,PreviewConfig.ITQAN_TOTAL_REPS)+"/"+PreviewConfig.ITQAN_TOTAL_REPS+" · masque "+currentMask+"% · révélations "+reveals);
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
        updateRevealButton();if(currentPage!=unitFirstPage){currentPage=unitFirstPage;showCurrent();updatePageButtons();}
        updateItqanProgress(rep,reveals);
    }

    private void validateItqan(){
        if(itqanUnit==null||prefs.itqanRep()<PreviewConfig.ITQAN_TOTAL_REPS)return;
        VerseRef next=prefs.corpus().next(itqanUnit.end);
        String label=itqanUnit.start+" → "+itqanUnit.end+" · ×"+PreviewConfig.ITQAN_TOTAL_REPS+" · révélations "+prefs.itqanAssisted();
        boolean ok=prefs.completeItqanUnit(next,LocalDate.now().toString(),label);
        if(!ok){onError("Impossible d’enregistrer atomiquement la validation Itqān.");return;}
        awaitingValidation=false;closeClockForCompletedSession();mushaf.cycleCompleted();renderMode();
    }

    private void renderMurajaah(){
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastMurajaahDate())){
            sessionCompleted=true;program.setText("Murājaʿah — séance validée");
            progress.setText(prefs.lastMurajaahLabel().isEmpty()?"Curseur sauvegardé":prefs.lastMurajaahLabel());return;
        }
        sessionCompleted=false;
        murajaahBlockB = "B".equals(prefs.murajaahPhase());
        if (!murajaahBlockB) renderRecentMurajaah(); else renderOldMurajaah();
    }

    private void renderRecentMurajaah() {
        List<HifzPrefs.RecentSabqi> recent=prefs.recentSabqi();
        long elapsed=clock.elapsedMs();
        if(recent.isEmpty() || elapsed>=PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60_000L){
            transitionToBlockB(elapsed);return;
        }
        int reviewIndex=(recentLinesDone/PreviewConfig.SABQI_LINES)%recent.size();
        HifzPrefs.RecentSabqi item=recent.get(reviewIndex);
        GeometryRepository.FiveLineBlock b=geometry.fiveLineBlock(item.startLine);
        currentPage=geometry.line(item.startLine).page;
        unitFirstPage=currentPage;unitLastPage=geometry.line(item.endLine).page;
        currentSelection=b.verses;currentLineIds=b.lineIds;currentMask=0;
        int capacity=(int)Math.floor(PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60.0/prefs.recentSecondsPerLine());
        program.setText("Murājaʿah · Bloc A · Sabqi récent · 30 min\n"+b.verseLabel());
        progress.setText("Fenêtre récente "+recent.size()+" bloc(s) · capacité ~"+capacity+" lignes · revues "+recentLinesDone
            +"\nLa promotion reste pilotée par la capacité, jamais par l’absence de faute.");
        showCurrent();
        addRoundAction("✓","Revu",v->reviewRecent(false));
        addRoundAction("!","À renforcer",v->reviewRecent(true));
    }

    /** Review quality is a signal only. It never removes/promotes/reorders recent material. */
    private void reviewRecent(boolean difficult) {
        if (!takeRepLock()) return;
        if (prefs.recentSabqi().isEmpty()) { transitionToBlockB(clock.elapsedMs()); return; }
        recentLinesDone+=PreviewConfig.SABQI_LINES;
        long elapsed=clock.elapsedMs();
        prefs.setMurajaahRuntime("A",null,recentLinesDone,elapsed,0L);
        if (elapsed>=PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60_000L) transitionToBlockB(elapsed);
        else renderMode();
    }

    private void transitionToBlockB(long elapsed) {
        if (!MURAJAAH.equals(mode) || murajaahBlockB) return;
        murajaahBlockAElapsedMs=Math.min(elapsed,PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60_000L);
        calibrateRecentSpeed(murajaahBlockAElapsedMs,recentLinesDone);
        murajaahBlockB=true;
        prefs.setMurajaahRuntime("B",null,recentLinesDone,murajaahBlockAElapsedMs,Math.max(0L,elapsed-murajaahBlockAElapsedMs));
        renderMode();
    }

    private void renderOldMurajaah(){
        if(!prefs.isMurajaahCursorValid()){
            sessionCompleted=true;program.setText("Murājaʿah — curseur hors des plages Itqān");
            progress.setText("Aucun déplacement automatique. Corrigez les plages ou le curseur dans Paramètres.");return;
        }
        murajaahBlockAElapsedMs=prefs.murajaahBlockAElapsedMs();
        long unusedA=Math.max(0L,PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60_000L-murajaahBlockAElapsedMs);
        long availableB=PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING*60_000L+unusedA;
        int lines=(int)Math.floor((availableB/1000.0)/prefs.murajaahSecondsPerLine());lines=Math.max(1,lines);
        murajaahPlan=geometry.planEligibleLines(prefs.murajaahCursor(),lines,prefs.corpus());
        murajaahActualEnd=prefs.murajaahActualEnd();
        currentPage=geometry.pageForVerse(murajaahPlan.start);currentSelection=murajaahPlan.traversalVerses;currentLineIds=Collections.emptyList();currentMask=0;
        double minutes=availableB/60_000.0;
        program.setText("Murājaʿah · Bloc B · cycle Itqān · "+String.format(Locale.ROOT,"%.0f",minutes)+" min\nPrévision : "+murajaahPlan.start+" → "+murajaahPlan.actualPlannedEnd);
        progress.setText(lines+" lignes prévues · "+String.format(Locale.ROOT,"%.2f",prefs.murajaahSecondsPerLine())+" s/ligne"
            +(murajaahActualEnd==null?"\nTouchez le dernier verset réellement terminé.":"\nFin réelle sélectionnée : "+murajaahActualEnd));
        showCurrent();
        if(murajaahActualEnd!=null)mushaf.setSelection(Collections.singletonList(murajaahActualEnd),geometry.lineIdsForVerseRange(murajaahActualEnd,murajaahActualEnd));
        Button finish=Ui.roundButton(this,"✓","Valider",v->finishMurajaah());finish.setEnabled(murajaahActualEnd!=null);actions.addView(finish);
    }

    private void finishMurajaah(){
        if(murajaahActualEnd==null){Toast.makeText(this,"Touchez d’abord le dernier verset réellement révisé.",Toast.LENGTH_LONG).show();return;}
        VerseRef itqanBefore=prefs.itqanCursor();
        VerseRef next=prefs.corpus().next(murajaahActualEnd);
        long total=clock.elapsedMs();
        long blockBElapsed=Math.max(0L,total-murajaahBlockAElapsedMs);
        calibrateOldSpeed(murajaahPlan.start,murajaahActualEnd,blockBElapsed);
        String label="Réel : "+murajaahPlan.start+" → "+murajaahActualEnd+" · prochain curseur "+next;
        boolean ok=prefs.completeMurajaah(next,LocalDate.now().toString(),label);
        if(!ok){onError("Impossible d’enregistrer la validation Murājaʿah.");return;}
        if(!prefs.itqanCursor().equals(itqanBefore)){
            prefs.setItqanCursor(itqanBefore);onError("État Murājaʿah incohérent annulé : curseur Itqān restauré.");return;
        }
        closeClockForCompletedSession();renderMode();
    }

    private void calibrateRecentSpeed(long elapsedMs,int lines){
        if(lines<PreviewConfig.SPEED_MIN_LINES || elapsedMs<PreviewConfig.SPEED_MIN_SECONDS*1000L)return;
        double measured=(elapsedMs/1000.0)/lines,old=prefs.recentSecondsPerLine();
        prefs.setRecentSecondsPerLine(smoothedClamped(old,measured));
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
        if(!MURAJAAH.equals(mode)||sessionCompleted)return;
        if(murajaahBlockB){
            long a=murajaahBlockAElapsedMs>0?murajaahBlockAElapsedMs:prefs.murajaahBlockAElapsedMs();
            prefs.setMurajaahRuntime("B",murajaahActualEnd,recentLinesDone,a,Math.max(0L,elapsed-a));
        }else{
            prefs.setMurajaahRuntime("A",null,recentLinesDone,elapsed,0L);
        }
    }

    @Override public void onVerseTap(VerseRef verse){
        if(MURAJAAH.equals(mode)&&murajaahBlockB&&murajaahPlan!=null&&prefs.corpus().contains(verse)){
            murajaahActualEnd=verse;
            progress.setText("Fin réelle sélectionnée : "+verse+"\n✓ Valider enregistrera cette borne.");
            eink.local(progress);mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
            checkpointMurajaah(clock.elapsedMs());
            renderMode();
        }
    }

    @Override public void onPageSwipe(int delta){goPage(delta);}
    private void showCurrent(){hasShown=true;mushaf.show(currentPage,currentSelection,currentLineIds,currentMask);}

    private void goPage(int delta) {
        int target=Math.max(1,Math.min(604,currentPage+delta));
        boolean limited=SABQI.equals(mode)||ITQAN.equals(mode)||(MURAJAAH.equals(mode)&&!murajaahBlockB);
        if(limited)target=Math.max(unitFirstPage,Math.min(unitLastPage,target));
        if(target==currentPage)return;currentPage=target;showCurrent();updatePageButtons();
    }

    private void updatePageButtons() {
        boolean free=MURAJAAH.equals(mode)&&murajaahBlockB;
        boolean multiPage=unitLastPage>unitFirstPage;
        int visibility=(free||multiPage)?View.VISIBLE:View.GONE;
        prevPage.setVisibility(visibility);nextPage.setVisibility(visibility);
        prevPage.setEnabled(free?currentPage>1:currentPage>unitFirstPage);
        nextPage.setEnabled(free?currentPage<604:currentPage<unitLastPage);
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
    private void openAudio(){new HifzAudioDialog(this,mushaf,currentSelection).show();}

    private void closeClockForCompletedSession(){sessionCompleted=true;awaitingValidation=false;clock.pause();clock.reset();prefs.setElapsedFor(mode,0L);}
    private int targetMinutes(){return SABQI.equals(mode)?PreviewConfig.SABQI_MINUTES_WORKING:ITQAN.equals(mode)?PreviewConfig.ITQAN_MINUTES_WORKING:PreviewConfig.MURAJAAH_MINUTES_WORKING;}
    private String displayModeName(){return SABQI.equals(mode)?"Sabqi":ITQAN.equals(mode)?"Itqān":"Murājaʿah";}
    @Override public void onReady(){if(!hasShown)showCurrent();}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){currentPage=page;updatePageButtons();}
    @Override protected void onResume(){super.onResume();if(!sessionCompleted&&!awaitingValidation)clock.resume();}
    @Override protected void onPause(){
        long elapsed=clock.pause();
        if(sessionCompleted&&!awaitingValidation)prefs.setElapsedFor(mode,0L);
        else{prefs.setElapsedFor(mode,elapsed);checkpointMurajaah(elapsed);}
        super.onPause();
    }
    @Override protected void onDestroy(){if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
