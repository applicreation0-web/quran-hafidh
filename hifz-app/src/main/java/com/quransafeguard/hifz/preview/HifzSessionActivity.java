package com.quransafeguard.hifz.preview;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Structured Sabqi / Itqan / Murajaah preview using the independent domain engine. */
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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!SABQI.equals(mode) && !ITQAN.equals(mode) && !MURAJAAH.equals(mode)) mode = SABQI;
        prefs = new HifzPrefs(this);
        geometry = GeometryRepository.get(this);
        clock = new SessionClock(prefs.elapsedFor(mode), elapsed -> {
            if (timerText != null) timerText.setText("Temps actif : " + SessionClock.format(elapsed) + " · cible " + targetMinutes() + " min (paramètre de travail)");
            long bucket = elapsed / 5_000L;
            if (bucket != lastCheckpointBucket) {
                lastCheckpointBucket = bucket;
                prefs.setElapsedFor(mode, elapsed);
            }
        });
        buildUi();
        renderMode();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this); root.setPadding(0,0,0,0);
        LinearLayout top = Ui.row(this); top.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4));
        top.addView(Ui.smallButton(this,"‹",v->finish()));
        heading = Ui.text(this, displayModeName(), 18, true); Ui.weight(heading,1f); heading.setGravity(Gravity.CENTER); top.addView(heading); root.addView(top);
        program = Ui.text(this,"Chargement du programme…",15,true); program.setPadding(Ui.dp(this,12),4,Ui.dp(this,12),2); root.addView(program);
        timerText = Ui.text(this,"",13,false); timerText.setPadding(Ui.dp(this,12),2,Ui.dp(this,12),2); root.addView(timerText);
        progress = Ui.text(this,"",14,false); progress.setPadding(Ui.dp(this,12),2,Ui.dp(this,12),6); root.addView(progress);
        mushaf = new MushafView(this); mushaf.setListener(this); root.addView(mushaf,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        actions = Ui.row(this); actions.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4)); root.addView(actions);
        LinearLayout nav=Ui.row(this);nav.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,8));
        prevPage=Ui.smallButton(this,"‹ Page",v->goPage(-1));Button audio=Ui.smallButton(this,"Audio",v->audioGate());nextPage=Ui.smallButton(this,"Page ›",v->goPage(1));
        Ui.weight(prevPage,1);Ui.weight(audio,1);Ui.weight(nextPage,1);nav.addView(prevPage);nav.addView(audio);nav.addView(nextPage);root.addView(nav);
        setContentView(root);
    }

    private void renderMode() {
        actions.removeAllViews();
        boolean pageNavigationAllowed = MURAJAAH.equals(mode);
        prevPage.setEnabled(pageNavigationAllowed);
        nextPage.setEnabled(pageNavigationAllowed);
        if (SABQI.equals(mode)) renderSabqi();
        else if (ITQAN.equals(mode)) renderItqan();
        else renderMurajaah();
    }

    private void renderSabqi() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.lastSabqiDate())) {
            sessionCompleted = true;
            program.setText("Sabqi — séance du jour terminée");
            progress.setText((prefs.lastSabqiLabel().isEmpty() ? "Bloc terminé" : prefs.lastSabqiLabel()) + "\nLe prochain bloc reste réservé au prochain créneau Sabqi.");
            return;
        }
        int cursor = prefs.sabqiLineCursor();
        if (cursor < 0) { cursor = geometry.firstLineIndex(new VerseRef(2,75)); prefs.setSabqiLineCursor(cursor); }
        sabqiBlock = geometry.fiveLineBlock(cursor);
        currentPage = geometry.line(sabqiBlock.startLineIndex).page;
        currentSelection = sabqiBlock.verses; currentLineIds = sabqiBlock.lineIds;
        int rep = prefs.sabqiRep(); currentMask = PreviewConfig.sabqiMaskForNextRep(rep);
        program.setText("Sabqi — Sourate " + sabqiBlock.startVerse.getSurah() + " · " + sabqiBlock.verseLabel() + "\n5 lignes réelles · 37 répétitions");
        updateSabqiProgress(rep, prefs.sabqiAssisted());
        showCurrent();
        Button done=Ui.smallButton(this,"Répétition faite",v->completeSabqiRep(false));Button assisted=Ui.smallButton(this,"Faite avec aide",v->completeSabqiRep(true));
        Ui.weight(done,1);Ui.weight(assisted,1);actions.addView(done);actions.addView(assisted);
    }

    private void updateSabqiProgress(int rep, int aids) {
        progress.setText("Répétition suivante : " + Math.min(rep+1,PreviewConfig.SABQI_TOTAL_REPS) + " / 37 · masque " + currentMask + "% · aides " + aids);
        eink.local(progress);
    }

    private boolean takeRepLock() {
        if (repActionLocked) return false;
        repActionLocked = true;
        getWindow().getDecorView().postDelayed(() -> repActionLocked = false, 800L);
        return true;
    }

    private void completeSabqiRep(boolean assisted) {
        if (!takeRepLock()) return;
        int rep=prefs.sabqiRep(); if(rep>=PreviewConfig.SABQI_TOTAL_REPS)return;
        int oldMask=currentMask;
        rep++; int aids=prefs.sabqiAssisted()+(assisted?1:0);
        if(rep>=PreviewConfig.SABQI_TOTAL_REPS){
            VerseRef promotion=sabqiBlock.endsInsideVerse?GeometryRepository.previous(sabqiBlock.endVerse):sabqiBlock.endVerse;
            String label=sabqiBlock.verseLabel();
            boolean ok = prefs.completeSabqiBlock(
                sabqiBlock.startLineIndex,
                sabqiBlock.endLineIndex,
                promotion,
                sabqiBlock.endLineIndex+1,
                LocalDate.now().toString(),
                label
            );
            if (!ok) { onError("Impossible d’enregistrer atomiquement la fin du bloc Sabqi."); return; }
            closeClockForCompletedSession();
            mushaf.cycleCompleted();
            renderMode();
            return;
        }
        if(!prefs.setSabqiProgress(rep,aids)){onError("Impossible d’enregistrer la répétition Sabqi.");return;}
        currentMask=PreviewConfig.sabqiMaskForNextRep(rep);
        if(currentMask!=oldMask)mushaf.setMask(currentMask);
        updateSabqiProgress(rep,aids);
    }

    private void renderItqan() {
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastItqanDate())){
            sessionCompleted = true;
            program.setText("Itqān — unité du jour terminée");
            progress.setText((prefs.lastItqanLabel().isEmpty()?"×30 terminé":prefs.lastItqanLabel())+"\nLe curseur a été sauvegardé pour le prochain créneau.");
            return;
        }
        EligibleCorpus corpus=prefs.corpus();
        int rep=prefs.itqanRep();
        VerseRef savedStart=prefs.itqanUnitStart();
        VerseRef savedEnd=prefs.itqanUnitEnd();
        if(rep>0 && savedStart!=null && savedEnd!=null){
            currentPage=geometry.pageForVerse(savedStart);
            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=geometry.lineIdsForVerseRange(savedStart,savedEnd);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
        } else {
            itqanUnit=geometry.eligiblePageUnit(prefs.itqanCursor(),corpus);
            currentPage=itqanUnit.page;
        }
        currentSelection=itqanUnit.verses;currentLineIds=itqanUnit.lineIds;
        currentMask=PreviewConfig.itqanMaskForNextRep(rep);
        program.setText("Itqān — Sourate "+itqanUnit.start.getSurah()+" · "+itqanUnit.start+" → "+itqanUnit.end+"\n×30 · masquage obligatoire · corpus cyclique");
        updateItqanProgress(rep,prefs.itqanAssisted());showCurrent();
        Button done=Ui.smallButton(this,"Répétition faite",v->completeItqanRep(false));Button assisted=Ui.smallButton(this,"Faite avec aide",v->completeItqanRep(true));Ui.weight(done,1);Ui.weight(assisted,1);actions.addView(done);actions.addView(assisted);
    }

    private void updateItqanProgress(int rep,int aids){
        progress.setText("Répétition suivante : "+Math.min(rep+1,30)+" / 30 · masque "+currentMask+"% (split de masque = paramètre de travail) · aides "+aids);
        eink.local(progress);
    }

    private void completeItqanRep(boolean assisted){
        if (!takeRepLock()) return;
        int rep=prefs.itqanRep();if(rep>=30)return;
        int oldMask=currentMask;rep++;int aids=prefs.itqanAssisted()+(assisted?1:0);
        if(rep>=30){
            VerseRef next=prefs.corpus().next(itqanUnit.end);
            String label=itqanUnit.start+" → "+itqanUnit.end+" · ×30";
            boolean ok=prefs.completeItqanUnit(next,LocalDate.now().toString(),label);
            if(!ok){onError("Impossible d’enregistrer atomiquement la fin de l’unité Itqān.");return;}
            closeClockForCompletedSession();
            mushaf.cycleCompleted();renderMode();return;
        }
        if(!prefs.setItqanProgress(rep,aids,itqanUnit.start,itqanUnit.end)){onError("Impossible d’enregistrer la répétition Itqān.");return;}
        currentMask=PreviewConfig.itqanMaskForNextRep(rep);if(currentMask!=oldMask)mushaf.setMask(currentMask);updateItqanProgress(rep,aids);
    }

    private void renderMurajaah(){
        String today=LocalDate.now().toString();
        if(today.equals(prefs.lastMurajaahDate())){
            sessionCompleted = true;
            program.setText("Murājaʿah — séance du jour terminée");
            progress.setText(prefs.lastMurajaahLabel().isEmpty()?"Curseur sauvegardé":prefs.lastMurajaahLabel());
            return;
        }
        List<HifzPrefs.RecentSabqi> recent=prefs.recentSabqi();
        if(!murajaahBlockB&&!recent.isEmpty()){
            HifzPrefs.RecentSabqi item=recent.get(0);GeometryRepository.FiveLineBlock b=geometry.fiveLineBlock(item.startLine);currentPage=geometry.line(item.startLine).page;currentSelection=b.verses;currentLineIds=b.lineIds;currentMask=0;
            program.setText("Murājaʿah — Bloc A · Sabqi récent\nSourate "+b.startVerse.getSurah()+" · "+b.verseLabel()+"\n15 min réservées (paramètre de travail)");progress.setText("File de Sabqi récent : "+recent.size()+" bloc(s). Aucune répétition imposée.");showCurrent();
            Button reviewed=Ui.smallButton(this,"Bloc revu",v->{prefs.removeFirstRecentSabqi();renderMode();});Button skip=Ui.smallButton(this,"Passer au Bloc B",v->{murajaahBlockB=true;renderMode();});Ui.weight(reviewed,1);Ui.weight(skip,1);actions.addView(reviewed);actions.addView(skip);return;
        }
        murajaahBlockB=true;int seconds=PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING*60;int lines=(int)Math.floor(seconds/prefs.murajaahSecondsPerLine());lines=Math.max(1,lines);
        murajaahPlan=geometry.planEligibleLines(prefs.murajaahCursor(),lines,prefs.corpus());murajaahActualEnd=null;currentPage=geometry.pageForVerse(murajaahPlan.start);currentSelection=murajaahPlan.traversalVerses;currentLineIds=Collections.emptyList();currentMask=0;
        program.setText("Murājaʿah — Bloc B · cycle Itqān\nPrévision : "+murajaahPlan.start+" → "+murajaahPlan.actualPlannedEnd+"\n"+lines+" lignes prévues · "+PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING+" min · "+String.format(java.util.Locale.ROOT,"%.2f",prefs.murajaahSecondsPerLine())+" s/ligne");progress.setText("Touchez le dernier verset réellement terminé. Le réel remplace toujours la prévision.");showCurrent();
        Button finish=Ui.smallButton(this,"Terminer au verset choisi",v->finishMurajaah());Ui.weight(finish,1);actions.addView(finish);
    }

    private void finishMurajaah(){
        if(murajaahActualEnd==null){Toast.makeText(this,"Touchez d’abord le dernier verset réellement révisé.",Toast.LENGTH_LONG).show();return;}
        VerseRef itqanBefore=prefs.itqanCursor();
        VerseRef next=prefs.corpus().next(murajaahActualEnd);
        String label="Réel : "+murajaahPlan.start+" → "+murajaahActualEnd+" · prochain curseur "+next;
        boolean ok=prefs.completeMurajaah(next,LocalDate.now().toString(),label);
        if(!ok){onError("Impossible d’enregistrer la fin de la Murājaʿah.");return;}
        if(!prefs.itqanCursor().equals(itqanBefore)){
            prefs.setItqanCursor(itqanBefore);
            onError("État Murājaʿah incohérent annulé : curseur Itqān restauré.");
            return;
        }
        closeClockForCompletedSession();
        renderMode();
    }

    @Override public void onVerseTap(VerseRef verse){
        if(MURAJAAH.equals(mode)&&murajaahBlockB&&murajaahPlan!=null&&prefs.corpus().contains(verse)){
            murajaahActualEnd=verse;
            progress.setText("Fin réelle sélectionnée : "+verse+"\nCette borne réelle, même au-delà de la prévision, devient la référence Murājaʿah.");
            eink.local(progress);
            mushaf.setSelection(Collections.singletonList(verse),geometry.lineIdsForVerseRange(verse,verse));
        }
    }
    private void showCurrent(){hasShown=true;mushaf.show(currentPage,currentSelection,currentLineIds,currentMask);}
    private void goPage(int d){
        if(SABQI.equals(mode)||ITQAN.equals(mode)) return;
        currentPage=Math.max(1,Math.min(604,currentPage+d));showCurrent();
    }
    private void audioGate(){HifzAudioGate gate=new HifzAudioGate(this);Toast.makeText(this,gate.status()+". Aucun compteur ni curseur n’est modifié.",Toast.LENGTH_LONG).show();}
    private void closeClockForCompletedSession(){
        sessionCompleted = true;
        clock.pause();
        clock.reset();
        prefs.setElapsedFor(mode,0L);
    }

    private int targetMinutes(){return SABQI.equals(mode)?PreviewConfig.SABQI_MINUTES_WORKING:ITQAN.equals(mode)?PreviewConfig.ITQAN_MINUTES_WORKING:PreviewConfig.MURAJAAH_MINUTES_WORKING;}
    private String displayModeName(){return SABQI.equals(mode)?"Sabqi":ITQAN.equals(mode)?"Itqān":"Murājaʿah";}
    @Override public void onReady(){if(!hasShown)showCurrent();}
    @Override public void onError(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    @Override public void onPageShown(int page){currentPage=page;}
    @Override protected void onResume(){super.onResume();if(!sessionCompleted)clock.resume();}
    @Override protected void onPause(){if(sessionCompleted){clock.pause();prefs.setElapsedFor(mode,0L);}else prefs.setElapsedFor(mode,clock.pause());super.onPause();}
    @Override protected void onDestroy(){if(clock!=null)clock.dispose();if(mushaf!=null)mushaf.destroySafely();super.onDestroy();}
    @Override public boolean onKeyDown(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_PAGE_UP){goPage(-1);return true;}if(code==KeyEvent.KEYCODE_PAGE_DOWN){goPage(1);return true;}return super.onKeyDown(code,e);}
}
