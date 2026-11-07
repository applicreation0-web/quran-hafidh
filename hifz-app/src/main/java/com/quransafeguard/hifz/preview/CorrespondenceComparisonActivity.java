package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Read-only correspondence comparison using the SAME authentic paired KFQC SVG
 * renderer and Jalalayn repository as Similitudes. No HifzPrefs/receipts/timers.
 * Admission is documentary; release validation is tracked separately in GitHub.
 */
public final class CorrespondenceComparisonActivity extends Activity {
    static final String EXTRA_SURAH="correspondenceSurah";
    static final String EXTRA_AYAH="correspondenceAyah";
    static final String EXTRA_ID="correspondenceId";
    static final String EXTRA_ORIGIN="correspondenceOrigin";

    static AlertDialog chooseCase(Activity activity, java.util.List<CorrespondenceCases.Entry> entries,
                                  java.util.function.Predicate<CorrespondenceCases.Entry> open,
                                  java.util.function.Consumer<Boolean> finished) {
        if(entries.size()<2){
            finished.accept(!entries.isEmpty()&&open.test(entries.get(0)));
            return null;
        }
        String[] labels=new String[entries.size()];
        for(int i=0;i<entries.size();i++){
            CorrespondenceCases.Entry entry=entries.get(i);
            labels[i]=entry.type+" · "+entry.title;
        }
        boolean[] opened={false};
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Correspondances")
            .setItems(labels,(ignored,index)->opened[0]=open.test(entries.get(index)))
            .setNegativeButton("Fermer",null).create();
        dialog.setOnDismissListener(ignored->finished.accept(opened[0]));
        dialog.show();
        return dialog;
    }

    private final ExecutorService tafsirIo=Executors.newSingleThreadExecutor();
    private final MapQcfThumbnails[] pages=new MapQcfThumbnails[2];
    private final TextView[] pageLabels=new TextView[2];
    private final Button[] focusActions=new Button[2];
    private final TextView[] commentaries=new TextView[2];
    private final int[] shownPages=new int[2];
    private final int[] navigationSurahs=new int[2];
    private final TextView[] tafsirLabels=new TextView[2];
    private final Button[] previousVerses=new Button[2],nextVerses=new Button[2];
    private final VerseRef[] selectedVerses=new VerseRef[2];
    private final int[] tafsirTokens=new int[2];
    private final boolean[] authenticQcfRendered=new boolean[2];
    private CorrespondenceCases.Entry relation;
    private GeometryRepository geometry;
    private TafsirRepository tafsir;
    private VerseRef origin;
    private LinearLayout root,folioRow,tafsirPanel,tafsirHead,pairHeader;
    private final LinearLayout[] folioColumns=new LinearLayout[2];
    private TextView relationLine,tafsirHeading;
    private ImageView relationIcon;
    private boolean booxPortrait,tafsirOpen,expanded;
    private int enlarged=-1,partnerOffset;
    private boolean disposed;
    private RefrainTimeline.State timelineState;
    private boolean sourcesOpen;
    private AlertDialog sourcesDialog;
    private int normalFolioHeightDp,expandedFolioHeightDp;
    private ScrollView folioViewport;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(!CorrespondenceCases.enabled()){finish();return;}
        try {
            geometry=GeometryRepository.get(this);
            origin=new VerseRef(getIntent().getIntExtra(EXTRA_SURAH,-1),
                getIntent().getIntExtra(EXTRA_AYAH,-1));
            relation=CorrespondenceCases.forIdAndVerse(getIntent().getStringExtra(EXTRA_ID),origin);
            if(relation==null){finish();return;}
            tafsir=new TafsirRepository(this);
        }catch(RuntimeException invalid){finish();return;}
        int width=getResources().getConfiguration().screenWidthDp;
        booxPortrait=getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT
            &&width>=900;
        partnerOffset=state==null?0:Math.max(0,state.getInt("partnerOffset",0));
        enlarged=state==null?-1:state.getInt("enlarged",-1);
        if(enlarged<-1||enlarged>1)enlarged=-1;
        tafsirOpen=state==null?booxPortrait:state.getBoolean("tafsirOpen",booxPortrait);
        selectedVerses[0]=origin;
        selectedVerses[1]=relation.partner(origin,partnerOffset);
        navigationSurahs[0]=origin.getSurah();
        navigationSurahs[1]=selectedVerses[1].getSurah();
        shownPages[0]=geometry.pageForVerse(selectedVerses[0]);
        shownPages[1]=geometry.pageForVerse(selectedVerses[1]);
        if(state!=null){
            for(int i=0;i<2;i++){
                // Folios stay fixed to the relation partner, including after recreation.
                int surah=state.getInt("selectedSurah"+i,0);
                int ayah=state.getInt("selectedAyah"+i,0);
                if(state.containsKey("selectedSurah"+i))
                    selectedVerses[i]=surah>0&&ayah>0?new VerseRef(surah,ayah):null;
                if(!isOnPage(selectedVerses[i],shownPages[i])){
                    selectedVerses[i]=i==0?origin:relation.partner(origin,partnerOffset);
                }
                navigationSurahs[i]=selectedVerses[i].getSurah();
            }
        }
        timelineState=new RefrainTimeline.State(state);
        build();
        updateRelationLabel();
        for(int i=0;i<2;i++){
            showFolio(i);
            if(tafsirOpen)loadTafsir(i,selectedVerses[i]);
        }
        if(state!=null&&state.getBoolean("sourcesOpen",false))root.post(this::showSources);
    }

    private TextView caption(String value,float size,boolean bold){
        TextView t=Ui.text(this,value,size,bold);t.setTextColor(Ui.INK);return t;
    }
    private int dp(int n){return Ui.dp(this,n);}
    private Button action(String glyph,String label,Runnable runnable){
        return Ui.iconButton(this,glyph,label,v->runnable.run());
    }
    private void build(){
        int width=getResources().getConfiguration().screenWidthDp;
        int height=getResources().getConfiguration().screenHeightDp;
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);
        LinearLayout top=Ui.row(this);top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(action("‹","Retour",this::finish),
            new LinearLayout.LayoutParams(dp(48),dp(48)));
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_ui_correspondances);
        icon.setContentDescription("Parallèles · Correspondances");
        icon.setPadding(dp(3),dp(3),dp(3),dp(3));
        top.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
        TextView title=caption("Correspondances",16,true);
        title.setGravity(Gravity.CENTER);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        top.addView(action("ⓘ","Sources",this::showSources),
            new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));
        root.addView(Ui.divider(this));
        pairHeader=Ui.row(this);pairHeader.setGravity(Gravity.CENTER_VERTICAL);
        relationIcon=new ImageView(this);
        relationIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams relationIconLayout=new LinearLayout.LayoutParams(dp(32),dp(32));
        relationIconLayout.setMarginStart(dp(8));
        pairHeader.addView(relationIcon,relationIconLayout);
        relationLine=caption("",13,true);relationLine.setGravity(Gravity.CENTER_VERTICAL);
        relationLine.setSingleLine(false);relationLine.setMaxLines(2);relationLine.setPadding(dp(8),0,0,0);
        pairHeader.addView(relationLine,new LinearLayout.LayoutParams(0,dp(40),1));
        if(relation.partnerCount(origin)>1&&!isRefrain()){ 
            pairHeader.addView(action("‹","Occurrence précédente",()->changePartner(-1)),
                new LinearLayout.LayoutParams(dp(44),dp(40)));
            pairHeader.addView(action("›","Occurrence suivante",()->changePartner(1)),
                new LinearLayout.LayoutParams(dp(44),dp(40)));
        }
        root.addView(pairHeader,new LinearLayout.LayoutParams(-1,dp(40)));
        // Atlas-style refrain rail is visible at once, not buried in the Sources notice.
        RefrainTimeline.append(this,root,relation.id,origin.toString(),timelineState,this::selectRefrain);

        ScrollView middle=new ScrollView(this);
        folioViewport=middle;
        middle.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitFoliosToViewport());
        middle.setFillViewport(false);
        middle.setVerticalScrollBarEnabled(false);
        LinearLayout content=Ui.column(this);
        content.setPadding(dp(4),0,dp(4),0);
        middle.addView(content,new ScrollView.LayoutParams(-1,-2));
        folioRow=Ui.row(this);folioRow.setGravity(Gravity.TOP);
        int halfWidth=Math.max(130,(width-18)/2);
        int natural=Math.round(halfWidth*550f/345f);
        int cap=Math.max(195,height-(booxPortrait?340:landscape?125:185));
        int paperHeight=Math.min(natural,cap);
        normalFolioHeightDp=paperHeight;
        expandedFolioHeightDp=Math.max(paperHeight,Math.min(
            Math.round((width-16)*550f/345f),
            Math.max(195,height-(landscape?125:booxPortrait?140:150))));
        for(int side=0;side<2;side++){
            final int pane=side;
            LinearLayout column=Ui.column(this);
            column.setPadding(0,0,0,0);
            folioColumns[side]=column;
            LinearLayout bar=Ui.row(this);bar.setGravity(Gravity.CENTER);
            TextView loc=caption("",12,true);loc.setGravity(Gravity.CENTER);
            loc.setSingleLine(true);
            column.addView(loc,new LinearLayout.LayoutParams(-1,dp(24)));
            pageLabels[side]=loc;
            Button zoom=booxPortrait
                ?Ui.smallButton(this,"Agrandir "+(side==0?"A":"B"),v->toggleEnlarge(pane))
                :action("⛶","Agrandir le Mushaf "+(side==0?"A":"B"),()->toggleEnlarge(pane));
            focusActions[side]=zoom;
            bar.addView(zoom,new LinearLayout.LayoutParams(dp(booxPortrait?110:44),dp(44)));
            column.addView(bar,new LinearLayout.LayoutParams(-1,dp(44)));
            MapQcfThumbnails surface=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
                @Override public void onPageChosen(int page){/* Fixed relation folio. */}
                @Override public void onRendered(int page,boolean success){
                    if(page==shownPages[pane]){
                        authenticQcfRendered[pane]=success;
                        VerseRef active=selectedVerses[pane];
                        if(success&&active!=null)pages[pane].setSelectedVerse(active.getSurah(),active.getAyah());
                    }
                }
                @Override public void onOpenReading(int page){openReading(pane);}
                @Override public void onVerseChosen(int page,int surah,int ayah){
                    selectVerse(pane,page,surah,ayah);
                }
            });
            surface.setAtlasSinglePageCompact(true);
            pages[side]=surface;
            column.addView(surface,new LinearLayout.LayoutParams(-1,dp(paperHeight)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
            lp.setMargins(dp(3),0,dp(3),0);folioRow.addView(column,lp);
        }
        content.addView(folioRow);
        root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));

        tafsirHead=Ui.row(this);tafsirHead.setGravity(Gravity.CENTER_VERTICAL);
        tafsirHeading=caption(tafsirOpen?"Tafsīr al-Jalālayn · ouvert":"Tafsīr al-Jalālayn · replié",14,true);
        tafsirHeading.setPadding(dp(8),0,0,0);
        tafsirHead.addView(tafsirHeading,new LinearLayout.LayoutParams(0,dp(42),1));
        tafsirHead.addView(action("⌃","Afficher ou masquer le Tafsīr",()->setTafsirOpen(!tafsirOpen)),
            new LinearLayout.LayoutParams(dp(44),dp(42)));
        root.addView(tafsirHead,new LinearLayout.LayoutParams(-1,dp(42)));
        tafsirPanel=Ui.row(this);
        for(int side=0;side<2;side++){
            final int pane=side;
            LinearLayout part=Ui.column(this);
            LinearLayout header=Ui.row(this);header.setGravity(Gravity.CENTER_VERTICAL);
            String sideName=side==0?"A":"B";
            previousVerses[side]=action("‹","Verset précédent "+sideName,()->stepVerse(pane,-1));
            nextVerses[side]=action("›","Verset suivant "+sideName,()->stepVerse(pane,1));
            header.addView(previousVerses[side],new LinearLayout.LayoutParams(dp(44),dp(44)));
            TextView label=caption("",booxPortrait?14:12,true);
            label.setGravity(Gravity.CENTER);label.setMaxLines(2);
            label.setOnClickListener(v->openReading(pane));
            tafsirLabels[side]=label;
            header.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));
            header.addView(nextVerses[side],new LinearLayout.LayoutParams(dp(44),dp(44)));
            part.addView(header,new LinearLayout.LayoutParams(-1,dp(44)));
            ScrollView scroll=new ScrollView(this);
            TextView commentary=caption("",booxPortrait?18:14,false);commentary.setPadding(dp(8),0,dp(8),dp(8));
            commentaries[side]=commentary;
            scroll.addView(commentary,new ScrollView.LayoutParams(-1,-2));
            part.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            tafsirPanel.addView(part,new LinearLayout.LayoutParams(0,-1,1));
        }
        root.addView(tafsirPanel,new LinearLayout.LayoutParams(-1,dp(booxPortrait?220:150)));
        tafsirPanel.setVisibility(tafsirOpen?View.VISIBLE:View.GONE);
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
        updateExpanded();
    }
    private void setTafsirOpen(boolean value){
        tafsirOpen=value;
        if(tafsirHeading!=null)tafsirHeading.setText(value?
            "Tafsīr al-Jalālayn · ouvert":"Tafsīr al-Jalālayn · replié");
        if(enlarged<0)tafsirPanel.setVisibility(value?View.VISIBLE:View.GONE);
        if(value)for(int i=0;i<2;i++)loadTafsir(i,selectedVerses[i]);
    }
    private void toggleEnlarge(int pane){
        enlarged=enlarged==pane?-1:pane;
        updateExpanded();
    }
    private void updateExpanded(){
        // Focus is a real QCF enlargement, not merely hiding the second column:
        // reclaim the Tafsir's reserved height but restore its prior state on exit.
        boolean focus=enlarged>=0;
        for(int i=0;i<2;i++)if(focusActions[i]!=null){
            String pane=i==0?"A":"B";
            boolean selected=enlarged==i;
            focusActions[i].setContentDescription(selected?
                "Réduire le Mushaf "+pane+" et revenir à la comparaison":
                "Agrandir le Mushaf "+pane);
            if(booxPortrait)focusActions[i].setText(selected?"Réduire "+pane:"Agrandir "+pane);
        }
        if(tafsirHead!=null)tafsirHead.setVisibility(focus?View.GONE:View.VISIBLE);
        if(tafsirPanel!=null)tafsirPanel.setVisibility(focus?View.GONE:tafsirOpen?View.VISIBLE:View.GONE);
        for(int i=0;i<2;i++)if(folioColumns[i]!=null){
            folioColumns[i].setVisibility(!focus||enlarged==i?View.VISIBLE:View.GONE);
            if(pages[i]!=null&&pages[i].getLayoutParams()!=null){
                android.view.ViewGroup.LayoutParams params=pages[i].getLayoutParams();
                params.height=dp(focus&&enlarged==i?expandedFolioHeightDp:normalFolioHeightDp);
                pages[i].setLayoutParams(params);
                if(!focus||enlarged==i){
                    final MapQcfThumbnails page=pages[i];
                    page.post(()->page.evaluateJavascript(
                        "if(typeof fit==='function'){fit();requestAnimationFrame(fit);}",null));
                }
            }
        }
        fitFoliosToViewport();
        if(folioViewport!=null)folioViewport.scrollTo(0,0);
    }
    private void fitFoliosToViewport(){
        if(folioViewport==null||folioViewport.getHeight()<=dp(68))return;
        // Configuration screenHeightDp includes space outside this ScrollView.
        // Fit the complete original SVG below the actual 24+44dp pane controls,
        // after system insets and Tafsir have consumed their measured space.
        int available=folioViewport.getHeight()-dp(68);
        for(int i=0;i<2;i++)if(pages[i]!=null&&folioColumns[i].getVisibility()==View.VISIBLE){
            android.view.ViewGroup.LayoutParams lp=pages[i].getLayoutParams();
            int desired=Math.min(available,dp(enlarged==i?expandedFolioHeightDp:normalFolioHeightDp));
            if(lp.height!=desired){lp.height=desired;pages[i].setLayoutParams(lp);}
        }
    }
    private boolean isRefrain(){
        return "R047".equals(relation.id)||"R048".equals(relation.id)||"R059".equals(relation.id);
    }
    private void selectRefrain(String ref){
        // A is the fixed anchor; B follows the rail, without changing Hifz state.
        if(origin.toString().equals(ref))return;
        for(int i=0;i<relation.partnerCount(origin);i++)if(relation.partner(origin,i).toString().equals(ref)){
            if(partnerOffset!=i){partnerOffset=i;changePartner(0);}
            return;
        }
    }
    private void changePartner(int delta){
        int alternatives=relation.partnerCount(origin);
        partnerOffset=Math.floorMod(partnerOffset+delta,alternatives);
        int previousPage=shownPages[1];
        selectedVerses[1]=relation.partner(origin,partnerOffset);
        navigationSurahs[1]=selectedVerses[1].getSurah();
        ++tafsirTokens[1];
        shownPages[1]=geometry.pageForVerse(selectedVerses[1]);
        updateRelationLabel();
        // A different verse on the same authentic QCF folio needs only a
        // polygon/label update, not another asynchronous WebView reload.
        // Prevents BOOX ghosting and stale "loading" after fast refrain taps.
        if(previousPage==shownPages[1]){
            updateSelection(1);
        }else{
            showFolio(1);
        }
        if(tafsirOpen)loadTafsir(1,selectedVerses[1]);
    }
    private java.util.List<VerseRef> navigationVerses(int side){
        java.util.ArrayList<VerseRef> result=new java.util.ArrayList<>();
        for(VerseRef verse:geometry.versesOnLines(geometry.lineIdsOnPage(shownPages[side])))
            if(verse.getSurah()==navigationSurahs[side])result.add(verse);
        result.sort(java.util.Comparator.comparingInt(VerseRef::getAyah));
        return result;
    }
    private void stepVerse(int side,int delta){
        java.util.List<VerseRef> allowed=navigationVerses(side);
        int index=allowed.indexOf(selectedVerses[side]);
        int next=index+(delta<0?-1:1);
        if(index<0||next<0||next>=allowed.size())return;
        VerseRef verse=allowed.get(next);
        selectVerse(side,shownPages[side],verse.getSurah(),verse.getAyah());
    }
    private void updateSelection(int side){
        VerseRef verse=selectedVerses[side];
        String name=side==0?"A":"B";
        pageLabels[side].setText(name+" · "+verse+" · p."+shownPages[side]);
        tafsirLabels[side].setText(name+" · Tafsīr\n"+verse);
        tafsirLabels[side].setContentDescription("Lire le verset "+verse+" · "+name);
        java.util.List<VerseRef> allowed=navigationVerses(side);
        int index=allowed.indexOf(verse);
        previousVerses[side].setEnabled(index>0);
        nextVerses[side].setEnabled(index>=0&&index<allowed.size()-1);
        pages[side].setSelectedVerse(verse.getSurah(),verse.getAyah());
    }
    private boolean isOnPage(VerseRef verse,int page){
        return verse!=null&&geometry.pageForVerse(verse)<=page
            &&MawdudiMapModel.lastPageForVerse(verse,geometry)>=page;
    }
    private void selectVerse(int side,int page,int surah,int ayah){
        if(page!=shownPages[side])return;
        try{
            VerseRef verse=new VerseRef(surah,ayah);
            if(!isOnPage(verse,page))return;
            selectedVerses[side]=verse;
            navigationSurahs[side]=verse.getSurah();
            ++tafsirTokens[side];
            updateSelection(side);
            if(tafsirOpen)loadTafsir(side,verse);
        }catch(RuntimeException invalid){/* Ignore stale polygon taps. */}
    }
    private void showFolio(int side){
        authenticQcfRendered[side]=false;
        VerseRef target=selectedVerses[side];
        pageLabels[side].setText((side==0?"A":"B")+" · "+target+" · p."+shownPages[side]);
        pages[side].showPages(shownPages[side],shownPages[side],shownPages[side],
            target.getSurah(),target.getAyah(),target.getAyah(),false);
        updateSelection(side);
    }
    private String pattern(){
        switch(relation.type){
            case "Répétition":return "A = A′";
            case "Progression":return "A → A′";
            case "Opposition":return "A ≠ B";
            case "Parallélisme":case "Ressemblance":return "A ∥ A′";
            case "Alternance":return "A B A′ B′";
            case "Inversion":return "A B / B′ A′";
            case "Centre":return "A B X B′ A′";
            case "Début–Fin":return "A … A′";
            default:return "";
        }
    }
    static int relationIconFor(String type) {
        switch(type){
            case "Répétition": return R.drawable.ic_corr_relation_repetition;
            case "Parallélisme":case "Ressemblance": return R.drawable.ic_corr_relation_ressemblance;
            case "Opposition": return R.drawable.ic_corr_relation_opposition;
            case "Progression": return R.drawable.ic_corr_relation_progression;
            case "Alternance": return R.drawable.ic_corr_relation_alternance;
            case "Inversion": return R.drawable.ic_corr_relation_inversion;
            case "Centre": return R.drawable.ic_corr_relation_centre;
            case "Début–Fin": return R.drawable.ic_corr_relation_debut_fin_inclusio;
            default: return 0;
        }
    }

    private void updateRelationLabel(){
        relationIcon.setImageResource(relationIconFor(relation.type));
        if(isRefrain()){
            relationLine.setText(relation.title);
            return;
        }
        relationLine.setText(relation.type+("R070".equals(relation.id)?" · lecture proposée\n":" · ")+pattern()+" · "+relation.id+" · "+
            (partnerOffset+1)+"/"+(relation.partnerCount(origin)));
    }
    private void loadTafsir(int side,VerseRef verse){
        if(disposed||tafsir==null)return;
        final int request=++tafsirTokens[side];
        if(verse==null){
            commentaries[side].setText("Touchez un verset du folio.");
            return;
        }
        commentaries[side].setText("…");
        tafsirIo.submit(()->{
            String content;
            try{
                TafsirRepository.Entry entry=tafsir.load(verse);
                if(entry==null)content="Commentaire indisponible.";
                else{
                    StringBuilder text=new StringBuilder();
                    for(TafsirRepository.Run run:entry.commentaryRuns)text.append(run.text);
                    content=text.toString().trim();
                }
            }catch(Exception unavailable){content="Commentaire indisponible.";}
            final String result=content;
            runOnUiThread(()->{
                if(!disposed&&request==tafsirTokens[side])
                    commentaries[side].setText(result);
            });
        });
    }
    private void sourceSection(LinearLayout column,String heading,String text){
        TextView title=caption(heading,16,true);title.setPadding(0,dp(12),0,dp(4));
        column.addView(title);column.addView(caption(text,16,false));
    }
    private void showSources(){
        if(relation==null)return;
        ScrollView scroll=new ScrollView(this);
        LinearLayout column=Ui.column(this);column.setPadding(dp(20),dp(8),dp(20),dp(16));
        scroll.addView(column,new ScrollView.LayoutParams(-1,-2));
        boolean refrain="R047".equals(relation.id)||"R048".equals(relation.id)
            ||"R059".equals(relation.id);
        if(refrain){
            String shortRef="R047".equals(relation.id)
                ?"4 refrains · 54:17, 22, 32, 40"
                :"R048".equals(relation.id)?"31 refrains · 55:13–77"
                :"10 refrains · 77:15–49";
            sourceSection(column,"Relation",shortRef);
            String[] currentRefrain=null;
            for(String[] node:RefrainTimelineData.NODES){
                if(relation.id.equals(node[0])&&Integer.parseInt(node[3])==timelineState.selected+1){
                    currentRefrain=node;break;
                }
            }
            if(currentRefrain!=null){
                sourceSection(column,"Contexte coranique",
                    currentRefrain[8]+" · "+currentRefrain[15]+"\n"+
                    currentRefrain[5]+" · Refrain\n"+
                    currentRefrain[11]+" · "+currentRefrain[16]);
                sourceSection(column,"Repère mnémotechnique",
                    currentRefrain[14]+" · Mots-clés éditoriaux en anglais fondés sur les "+
                    "versets voisins. La frise reprend UNIQUEMENT le graphisme d'Atlas, "+
                    "et non ses titres.");
            }
            String bibliography="R047".equals(relation.id)
                ?"al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 54:17\n"+
                  "https://quran.ksu.edu.sa/tafseer/qortobi/sura54-aya17.html"
                :"R048".equals(relation.id)
                ?"al-Baghawī, Maʿālim al-tanzīl, 55:13\n"+
                  "https://quran.ksu.edu.sa/tafseer/baghawy/sura55-aya13.html\n"+
                  "al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 55:13\n"+
                  "https://quran.ksu.edu.sa/tafseer/qortobi/sura55-aya13.html"
                :"al-Qurṭubī, al-Jāmiʿ li-aḥkām al-Qurʾān, 77:50\n"+
                  "https://quran.ksu.edu.sa/tafseer/qortobi/sura77-aya50.html\n"+
                  "Ibn Kathīr, Tafsīr al-Qurʾān al-ʿAẓīm, 77:15\n"+
                  "https://quran.ksu.edu.sa/tafseer/katheer/sura77-aya15.html";
            sourceSection(column,"Exégèses · fonction du refrain",bibliography);
            LinearLayout complete=Ui.column(this);
            complete.setVisibility(View.GONE);
            sourceSection(complete,"Bibliographie complète",relation.source);
            Button expand=Ui.smallButton(this,"Toutes les références",v->
                complete.setVisibility(complete.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
            column.addView(expand);
            column.addView(complete);
        }else{
            String[] parts=relation.mechanism.split("\\n\\n");
            column.addView(caption(parts[0],14,true));
            String explanation=parts.length>1?parts[1]:relation.mechanism;
            sourceSection(column,"En bref",relation.brief);
            sourceSection(column,"Sources",relation.source);
            for(String part:parts)if(part.startsWith("Limites : "))
                sourceSection(column,"Limites",part.substring("Limites : ".length()));
            if(!relation.brief.equals(explanation))sourceSection(column,"Notice complète",explanation);
        }
        if(sourcesDialog!=null&&sourcesDialog.isShowing())return;
        sourcesOpen=true;
        sourcesDialog=new AlertDialog.Builder(this).setTitle(relation.title+" · "+relation.id)
            .setView(scroll).setPositiveButton("Fermer",null).create();
        sourcesDialog.setOnDismissListener(dialog->sourcesOpen=false);
        sourcesDialog.show();
    }
    private void openReading(int side){
        VerseRef target=selectedVerses[side];
        Intent reader=new Intent(this,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,shownPages[side]);
        if(target!=null)reader.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,target.toString());
        startActivity(reader);
    }
    boolean bothAuthenticQcfPanesReadyForTest(){return authenticQcfRendered[0]&&authenticQcfRendered[1];}
    boolean visibleTafsirReadyForTest(){
        if(!tafsirOpen)return true;
        for(TextView commentary:commentaries){
            String text=commentary.getText().toString();
            if(text.isEmpty()||"…".equals(text)||"Commentaire indisponible.".equals(text))return false;
        }
        return true;
    }
    boolean booxTafsirOpenForTest(){return booxPortrait&&tafsirOpen&&tafsirPanel.getVisibility()==View.VISIBLE;}
    MapQcfThumbnails[] qcfPagesForTest(){return pages.clone();}
    @Override protected void onSaveInstanceState(Bundle out){
        out.putBoolean("sourcesOpen",sourcesOpen);timelineState.save(out);
        out.putInt("partnerOffset",partnerOffset);out.putInt("page0",shownPages[0]);
        out.putInt("page1",shownPages[1]);out.putBoolean("tafsirOpen",tafsirOpen);
        out.putInt("enlarged",enlarged);
        for(int i=0;i<2;i++){
            VerseRef selected=selectedVerses[i];
            out.putInt("navigationSurah"+i,navigationSurahs[i]);
            out.putInt("selectedSurah"+i,selected==null?0:selected.getSurah());
            out.putInt("selectedAyah"+i,selected==null?0:selected.getAyah());
        }
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy(){
        disposed=true;tafsirIo.shutdownNow();
        if(sourcesDialog!=null)sourcesDialog.dismiss();
        for(MapQcfThumbnails page:pages)if(page!=null)page.destroy();
        super.onDestroy();
    }
}
