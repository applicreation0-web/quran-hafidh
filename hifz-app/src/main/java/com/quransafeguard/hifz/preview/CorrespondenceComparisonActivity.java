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
 * The pilot is never included in a release until source and BOOX gates pass.
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
    private final TextView[] commentaries=new TextView[2];
    private final int[] shownPages=new int[2];
    private final VerseRef[] selectedVerses=new VerseRef[2];
    private final int[] tafsirTokens=new int[2];
    private final boolean[] authenticQcfRendered=new boolean[2];
    private CorrespondenceCases.Entry relation;
    private GeometryRepository geometry;
    private TafsirRepository tafsir;
    private VerseRef origin;
    private LinearLayout root,folioRow,tafsirPanel,pairHeader;
    private final LinearLayout[] folioColumns=new LinearLayout[2];
    private TextView relationLine,tafsirHeading;
    private boolean booxPortrait,tafsirOpen,expanded;
    private int enlarged=-1,partnerOffset;
    private boolean disposed;
    private int normalFolioHeightDp,expandedFolioHeightDp;

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
        shownPages[0]=geometry.pageForVerse(selectedVerses[0]);
        shownPages[1]=geometry.pageForVerse(selectedVerses[1]);
        if(state!=null){
            for(int i=0;i<2;i++){
                int p=state.getInt("page"+i,shownPages[i]);
                if(p>=1&&p<=604)shownPages[i]=p;
                int surah=state.getInt("selectedSurah"+i,0);
                int ayah=state.getInt("selectedAyah"+i,0);
                if(state.containsKey("selectedSurah"+i))
                    selectedVerses[i]=surah>0&&ayah>0?new VerseRef(surah,ayah):null;
                if(!isOnPage(selectedVerses[i],shownPages[i]))selectedVerses[i]=null;
            }
        }
        build();
        updateRelationLabel();
        for(int i=0;i<2;i++){
            showFolio(i);
            if(tafsirOpen)loadTafsir(i,selectedVerses[i]);
        }
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
        relationLine=caption("",13,true);relationLine.setGravity(Gravity.CENTER_VERTICAL);
        relationLine.setSingleLine(true);relationLine.setPadding(dp(8),0,0,0);
        pairHeader.addView(relationLine,new LinearLayout.LayoutParams(0,dp(40),1));
        if(relation.verses.size()>2){
            pairHeader.addView(action("‹","Occurrence précédente",()->changePartner(-1)),
                new LinearLayout.LayoutParams(dp(44),dp(40)));
            pairHeader.addView(action("›","Occurrence suivante",()->changePartner(1)),
                new LinearLayout.LayoutParams(dp(44),dp(40)));
        }
        root.addView(pairHeader,new LinearLayout.LayoutParams(-1,dp(40)));

        ScrollView middle=new ScrollView(this);
        middle.setFillViewport(false);
        middle.setVerticalScrollBarEnabled(false);
        LinearLayout content=Ui.column(this);
        middle.addView(content,new ScrollView.LayoutParams(-1,-2));
        folioRow=Ui.row(this);folioRow.setGravity(Gravity.TOP);
        int halfWidth=Math.max(130,(width-18)/2);
        int natural=Math.round(halfWidth*550f/345f);
        int cap=Math.max(195,height-(booxPortrait?340:landscape?125:185));
        int paperHeight=Math.min(natural,cap);
        normalFolioHeightDp=paperHeight;
        expandedFolioHeightDp=Math.max(paperHeight,Math.min(
            Math.round((width-16)*550f/345f),
            Math.max(195,height-(booxPortrait?340:landscape?125:165))));
        for(int side=0;side<2;side++){
            final int pane=side;
            LinearLayout column=Ui.column(this);
            folioColumns[side]=column;
            LinearLayout bar=Ui.row(this);bar.setGravity(Gravity.CENTER_VERTICAL);
            TextView loc=caption("",12,true);loc.setGravity(Gravity.CENTER_VERTICAL);
            pageLabels[side]=loc;
            bar.addView(action("‹","Page précédente "+(side==0?"A":"B"),()->selectPage(pane,shownPages[pane]-1)),
                new LinearLayout.LayoutParams(dp(44),dp(44)));
            bar.addView(loc,new LinearLayout.LayoutParams(0,dp(44),1));
            bar.addView(action("›","Page suivante "+(side==0?"A":"B"),()->selectPage(pane,shownPages[pane]+1)),
                new LinearLayout.LayoutParams(dp(44),dp(44)));
            bar.addView(action("⛶","Agrandir le Mushaf "+(side==0?"A":"B"),()->toggleEnlarge(pane)),
                new LinearLayout.LayoutParams(dp(44),dp(44)));
            column.addView(bar,new LinearLayout.LayoutParams(-1,dp(44)));
            MapQcfThumbnails surface=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
                @Override public void onPageChosen(int page){selectPage(pane,page);}
                @Override public void onRendered(int page,boolean success){
                    if(page==shownPages[pane])authenticQcfRendered[pane]=success;
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

        LinearLayout tafsirHead=Ui.row(this);tafsirHead.setGravity(Gravity.CENTER_VERTICAL);
        tafsirHeading=caption("Tafsīr al-Jalālayn",14,true);
        tafsirHeading.setPadding(dp(8),0,0,0);
        tafsirHead.addView(tafsirHeading,new LinearLayout.LayoutParams(0,dp(42),1));
        tafsirHead.addView(action("⌃","Afficher ou masquer le Tafsīr",()->setTafsirOpen(!tafsirOpen)),
            new LinearLayout.LayoutParams(dp(44),dp(42)));
        root.addView(tafsirHead,new LinearLayout.LayoutParams(-1,dp(42)));
        tafsirPanel=Ui.row(this);
        for(int side=0;side<2;side++){
            final int pane=side;
            LinearLayout part=Ui.column(this);
            TextView label=caption(side==0?"A · Tafsīr":"B · Tafsīr",12,true);
            label.setPadding(dp(8),0,0,0);
            label.setOnClickListener(v->openReading(pane));
            part.addView(label,new LinearLayout.LayoutParams(-1,dp(28)));
            ScrollView scroll=new ScrollView(this);
            TextView commentary=caption("",14,false);commentary.setPadding(dp(8),0,dp(8),dp(8));
            commentaries[side]=commentary;
            scroll.addView(commentary,new ScrollView.LayoutParams(-1,-2));
            part.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            tafsirPanel.addView(part,new LinearLayout.LayoutParams(0,-1,1));
        }
        root.addView(tafsirPanel,new LinearLayout.LayoutParams(-1,dp(booxPortrait?260:150)));
        tafsirPanel.setVisibility(tafsirOpen?View.VISIBLE:View.GONE);
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
        updateExpanded();
    }
    private void setTafsirOpen(boolean value){
        tafsirOpen=value;tafsirPanel.setVisibility(value?View.VISIBLE:View.GONE);
        if(value)for(int i=0;i<2;i++)loadTafsir(i,selectedVerses[i]);
    }
    private void toggleEnlarge(int pane){
        enlarged=enlarged==pane?-1:pane;
        updateExpanded();
    }
    private void updateExpanded(){
        for(int i=0;i<2;i++)if(folioColumns[i]!=null){
            folioColumns[i].setVisibility(enlarged<0||enlarged==i?View.VISIBLE:View.GONE);
            if(pages[i]!=null&&pages[i].getLayoutParams()!=null){
                android.view.ViewGroup.LayoutParams params=pages[i].getLayoutParams();
                params.height=dp(enlarged==i?expandedFolioHeightDp:normalFolioHeightDp);
                pages[i].setLayoutParams(params);
            }
        }
    }
    private void changePartner(int delta){
        int alternatives=relation.verses.size()-1;
        partnerOffset=Math.floorMod(partnerOffset+delta,alternatives);
        selectedVerses[1]=relation.partner(origin,partnerOffset);
        shownPages[1]=geometry.pageForVerse(selectedVerses[1]);
        updateRelationLabel();
        showFolio(1);
        if(tafsirOpen)loadTafsir(1,selectedVerses[1]);
    }
    private void selectPage(int side,int page){
        if(page<1||page>604||side<0||side>1||page==shownPages[side])return;
        shownPages[side]=page;
        if(!isOnPage(selectedVerses[side],page)){
            selectedVerses[side]=null;
            if(tafsirOpen)loadTafsir(side,null);
        }
        showFolio(side);
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
            if(tafsirOpen)loadTafsir(side,verse);
        }catch(RuntimeException invalid){/* Ignore stale polygon taps. */}
    }
    private void showFolio(int side){
        authenticQcfRendered[side]=false;
        VerseRef target=side==0?origin:relation.partner(origin,partnerOffset);
        pageLabels[side].setText((side==0?"A":"B")+" · "+target+" · p."+shownPages[side]);
        pages[side].showPages(shownPages[side],shownPages[side],shownPages[side],
            target.getSurah(),target.getAyah(),target.getAyah(),false);
    }
    private String pattern(){
        switch(relation.type){
            case "Répétition":return "A = A′";
            case "Progression":return "A → A′";
            case "Opposition":return "A ≠ B";
            case "Parallélisme":return "A ∥ A′";
            case "Alternance":return "A B A′ B′";
            case "Inversion":return "A B / B′ A′";
            case "Centre":return "A B X B′ A′";
            case "Début–Fin":return "A … A′";
            default:return "";
        }
    }
    private void updateRelationLabel(){
        relationLine.setText(relation.type+" · "+pattern()+" · "+relation.id+" · "+
            (partnerOffset+1)+"/"+(relation.verses.size()-1));
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
    private void showSources(){
        if(relation==null)return;
        new AlertDialog.Builder(this).setTitle(relation.title+" · "+relation.id)
            .setMessage(relation.mechanism+"\n\nSource : "+relation.source+
                "\n\nAperçu éditorial privé. Les validations QCF et BOOX restent obligatoires.")
            .setPositiveButton("Fermer",null).show();
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
        out.putInt("partnerOffset",partnerOffset);out.putInt("page0",shownPages[0]);
        out.putInt("page1",shownPages[1]);out.putBoolean("tafsirOpen",tafsirOpen);
        out.putInt("enlarged",enlarged);
        for(int i=0;i<2;i++){
            VerseRef selected=selectedVerses[i];
            out.putInt("selectedSurah"+i,selected==null?0:selected.getSurah());
            out.putInt("selectedAyah"+i,selected==null?0:selected.getAyah());
        }
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy(){
        disposed=true;tafsirIo.shutdownNow();
        for(MapQcfThumbnails page:pages)if(page!=null)page.destroy();
        super.onDestroy();
    }
}