package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.TextView;
import com.quransafeguard.hifz.core.VerseRef;

/**
 * Atlas-native, read-only, monochrome E-Ink comparison of two Quran passages.
 * Two equal-size authentic KFQC Mushaf pages and contextual full Jalalayn
 * commentary from the existing audited offline corpus.
 * The comparison notes are authored and source-backed; no AI-generated commentary.
 *
 * Deliberately separate from Révision active (no HifzPrefs or mutable session state).
 */
public final class AtlasDiscriminantActivity extends Activity {
    static final String EXTRA_SURAH="discriminantSurah";
    static final String EXTRA_AYAH="discriminantAyah";
    static final String EXTRA_ORIGIN="discriminantOrigin";
    static final String EXTRA_PAGE="sourceQcfPage";
    static final String EXTRA_CASE_INDEX="discriminantCaseIndex";
    private java.util.List<Integer> caseIndices;
    private Button previousCase,nextCase;

    private int selectedIndex;
    private GeometryRepository geometry;
    private MapQcfThumbnails left, right;
    private QcfAtlasVerseStrips strips;
    private final ExecutorService tafsirIo=Executors.newSingleThreadExecutor();
    private TafsirRepository tafsirRepository;
    private int leftTafsirRequest,rightTafsirRequest;
    private VerseRef selectedLeftVerse,selectedRightVerse;
    private TextView leftTafsir,rightTafsir,tafsirHeading;
    private LinearLayout tafsirPanel;
    private ScrollView tafsirScroll,comparisonScroll;
    private LinearLayout workspace,pagePair;
    private int pageHeaderHeight,comparisonDetailHeight;
    private TextView firstTafsirTab,secondTafsirTab;
    private int selectedTafsir=-1;
    private boolean lateralTafsir;
    private TextView title, leftHeader, rightHeader;
    private TextView leftRead, rightRead, returnToSource;
    private boolean disposed;
    private int currentLeftPage, currentRightPage;
    private int leftPageStatus, rightPageStatus; // 0 pending, +1 decoded, -1 failed

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        tafsirRepository=new TafsirRepository(this);
        try {
            geometry=GeometryRepository.get(this);
            int sourcePage=getIntent().getIntExtra(EXTRA_PAGE,0);
            caseIndices=sourcePage>0?AtlasDiscriminantCases.indicesForPage(sourcePage):
                AtlasDiscriminantCases.indicesForVerse(getIntent().getIntExtra(EXTRA_SURAH,0),
                    getIntent().getIntExtra(EXTRA_AYAH,0));
            if(caseIndices.isEmpty())caseIndices=java.util.Collections.singletonList(0);
            selectedIndex=state!=null?state.getInt("caseIndex",caseIndices.get(0)):
                getIntent().getIntExtra(EXTRA_CASE_INDEX,caseIndices.get(0));
            if(!caseIndices.contains(selectedIndex))selectedIndex=caseIndices.get(0);
            selectedTafsir=state==null?-1:state.getInt("tafsirSide",-1);
            if(state!=null){
                String savedLeft=state.getString("tafsirLeftVerse","");
                String savedRight=state.getString("tafsirRightVerse","");
                if(!savedLeft.isEmpty())selectedLeftVerse=GeometryRepository.parseVerse(savedLeft);
                if(!savedRight.isEmpty())selectedRightVerse=GeometryRepository.parseVerse(savedRight);
            }
            build();
            render();
        } catch (Exception unavailable) {
            Ui.showFatal(this,"Comparaison Atlas indisponible : coordonnées QCF à vérifier.");
        }
    }

    private TextView text(String s,float sp,boolean bold) {
        TextView t=Ui.text(this,s,sp,bold);
        t.setTextColor(Ui.INK);
        return t;
    }

    /**
     * The comparison belongs to the invoking Lecture/Révision context.
     * No browseable Atlas catalogue, no huge decorative rail, no stretched
     * empty excerpt region, and two independently labelled Lecture destinations.
     */
    private void build() {
        float density=getResources().getDisplayMetrics().density;
        int dpHeight=(int)(getResources().getDisplayMetrics().heightPixels/density);
        int dpWidth=(int)(getResources().getDisplayMetrics().widthPixels/density);
        boolean compact=dpHeight<740;
        lateralTafsir=dpWidth>=900;
        LinearLayout root=Ui.column(this);
        root.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,3));
        root.setBackgroundColor(Ui.PAPER);
        LinearLayout top=Ui.row(this);top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(Ui.iconButton(this,"","Retour",v->finish()));
        title=text("",compact?15:17,true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        previousCase=new Button(this);previousCase.setText("‹");
        previousCase.setContentDescription("Comparaison précédente de cette page");
        previousCase.setOnClickListener(v->moveCase(-1));
        nextCase=new Button(this);nextCase.setText("›");
        nextCase.setContentDescription("Comparaison suivante de cette page");
        nextCase.setOnClickListener(v->moveCase(1));
        top.addView(previousCase,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        top.addView(nextCase,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        root.addView(top,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        workspace=lateralTafsir?Ui.row(this):Ui.column(this);
        root.addView(workspace,new LinearLayout.LayoutParams(-1,0,1f));
        // Re-fit QCF after the Tafsir pane changes the real viewport, not before it.
        workspace.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(comparisonScroll!=null)comparisonScroll.post(this::fitVisiblePages);
        });
        ScrollView contentScroll=new ScrollView(this);
        comparisonScroll=contentScroll;
        contentScroll.setFillViewport(false);
        LinearLayout body=Ui.column(this);
        contentScroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        workspace.addView(contentScroll,lateralTafsir?
            new LinearLayout.LayoutParams(0,-1,1f):new LinearLayout.LayoutParams(-1,0,1f));
        contentScroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitVisiblePages());
        LinearLayout heads=Ui.row(this);heads.setGravity(Gravity.CENTER_VERTICAL);
        leftHeader=text("",compact?12:13.5f,true);
        rightHeader=text("",compact?12:13.5f,true);
        leftHeader.setGravity(Gravity.CENTER);rightHeader.setGravity(Gravity.CENTER);
        heads.addView(leftHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        heads.addView(rightHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        body.addView(heads);
        pageHeaderHeight=Ui.dp(this,compact?35:40);
        comparisonDetailHeight=Ui.dp(this,48+(compact?90:108));

        LinearLayout pair=Ui.row(this);
        pagePair=pair;
        left=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
            @Override public void onPageChosen(int page){selectTafsir(0);}
            @Override public void onOpenReading(int page){selectTafsir(0);}
            @Override public void onVerseChosen(int page,int surah,int ayah){
                selectExactTafsir(0,page,new VerseRef(surah,ayah));
            }
            @Override public void onRendered(int page,boolean success){
                if(page!=currentLeftPage)return;
                leftPageStatus=success?1:-1;
                android.util.Log.i("HaafidhAtlasQCF","Left SVG "+page+" rendered="+success);
                showRightAfterLeft();
            }
        });
        right=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
            @Override public void onPageChosen(int page){selectTafsir(1);}
            @Override public void onOpenReading(int page){selectTafsir(1);}
            @Override public void onVerseChosen(int page,int surah,int ayah){
                selectExactTafsir(1,page,new VerseRef(surah,ayah));
            }
            @Override public void onRendered(int page,boolean success){
                if(page!=currentRightPage)return;
                rightPageStatus=success?1:-1;
                android.util.Log.i("HaafidhAtlasQCF","Right SVG "+page+" rendered="+success);
                showStripsAfterPages();
            }
        });
        left.setAtlasSinglePageCompact(true);
        right.setAtlasSinglePageCompact(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);
        lp.setMargins(0,0,Ui.dp(this,3),0);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1f);
        rp.setMargins(Ui.dp(this,3),0,0,0);
        pair.addView(left,lp);pair.addView(right,rp);
        // Both genuine 345:550 KFQC pages remain intact, tappable and proportional.
        int availableWidth=Ui.dp(this,dpWidth-(lateralTafsir?330:8));
        int natural=Math.round((availableWidth-Ui.dp(this,18))/2f*550f/345f);
        int cap=Ui.dp(this,Math.max(170,dpHeight-(lateralTafsir?260:365)));
        int pairedHeight=Math.min(natural,cap);
        body.addView(pair,new LinearLayout.LayoutParams(-1,pairedHeight));

        LinearLayout readingLinks=Ui.row(this);
        leftRead=readingLink(true);
        rightRead=readingLink(false);
        readingLinks.addView(leftRead,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        readingLinks.addView(rightRead,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        body.addView(readingLinks);

        // A short source-vector context stays beside the same two localizations.
        strips=new QcfAtlasVerseStrips(this);
        strips.setOnRendered(()->{
            if(!disposed&&leftPageStatus==1&&rightPageStatus==1&&strips.bothRenderedForTest())
                strips.setAlpha(1f);
        });
        body.addView(strips,new LinearLayout.LayoutParams(-1,Ui.dp(this,compact?90:108)));
        tafsirPanel=Ui.column(this);
        tafsirPanel.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,4));
        LinearLayout tabs=Ui.row(this);
        firstTafsirTab=tafsirTab(0);secondTafsirTab=tafsirTab(1);
        tabs.addView(firstTafsirTab,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        tabs.addView(secondTafsirTab,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        tafsirPanel.addView(tabs);
        tafsirHeading=text("",14,true);
        tafsirHeading.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,8));
        tafsirPanel.addView(tafsirHeading);
        leftTafsir=tafsirText();rightTafsir=tafsirText();
        LinearLayout tafsirBodies=Ui.column(this);
        tafsirBodies.addView(leftTafsir);tafsirBodies.addView(rightTafsir);
        tafsirScroll=new ScrollView(this);
        tafsirScroll.addView(tafsirBodies,new ScrollView.LayoutParams(-1,-2));
        tafsirPanel.addView(tafsirScroll,new LinearLayout.LayoutParams(-1,0,1));
        if(lateralTafsir)workspace.addView(tafsirPanel,
            new LinearLayout.LayoutParams(Ui.dp(this,322),-1));
        else workspace.addView(tafsirPanel,new LinearLayout.LayoutParams(-1,
            Ui.dp(this,Math.max(180,dpHeight*2/5))));
        // No empty commentary region until a Mushaf is selected.
        tafsirPanel.setVisibility(selectedTafsir<0?View.GONE:View.VISIBLE);
        tafsirPanel.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->
            comparisonScroll.post(this::fitVisiblePages));

        LinearLayout bottom=Ui.row(this);bottom.setGravity(Gravity.CENTER_VERTICAL);
        returnToSource=text("",compact?13:14,false);
        returnToSource.setGravity(Gravity.CENTER_VERTICAL);
        returnToSource.setPadding(Ui.dp(this,13),0,0,0);
        returnToSource.setContentDescription("Retour à la séance d’origine");
        returnToSource.setOnClickListener(v->finish());
        bottom.addView(returnToSource,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        Button explanation=Ui.iconButton(this,"","Indice et référence",v->showExplanation());
        bottom.addView(explanation,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        root.addView(bottom,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));

        // Root retains a fixed return bar, with a scrollable comparison above it.
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
    }

    private TextView tafsirText(){
        TextView body=Ui.bookText(this,"",16f,false);
        body.setTextColor(Ui.INK);
        body.setTextIsSelectable(true);
        body.setPadding(Ui.dp(this,4),0,Ui.dp(this,4),Ui.dp(this,12));
        return body;
    }

    private TextView tafsirTab(int side){
        TextView tab=text("",14,true);
        tab.setGravity(Gravity.CENTER);
        tab.setContentDescription(side==0?"Tafsīr du premier verset":"Tafsīr du second verset");
        tab.setOnClickListener(v->selectTafsir(side));
        return tab;
    }

    private void selectExactTafsir(int side,int page,VerseRef verse){
        if(verse==null||page!=(side==0?currentLeftPage:currentRightPage)
                ||geometry.pageForVerse(verse)!=page)return;
        VerseRef old=side==0?selectedLeftVerse:selectedRightVerse;
        if(side==0)selectedLeftVerse=verse;else selectedRightVerse=verse;
        if(!verse.equals(old))loadContextualTafsir(side,verse);
        selectTafsir(side);
    }

    private void selectTafsir(int side){
        if(side<0||side>1)return;
        selectedTafsir=side;
        tafsirPanel.setVisibility(View.VISIBLE);
        leftTafsir.setVisibility(side==0?View.VISIBLE:View.GONE);
        rightTafsir.setVisibility(side==1?View.VISIBLE:View.GONE);
        firstTafsirTab.setSelected(side==0);secondTafsirTab.setSelected(side==1);
        firstTafsirTab.setBackgroundColor(side==0?Ui.LINE:Ui.PAPER);
        secondTafsirTab.setBackgroundColor(side==1?Ui.LINE:Ui.PAPER);
        VerseRef verse=side==0?selectedLeftVerse:selectedRightVerse;
        if(verse==null)verse=side==0?current().first:current().second;
        tafsirHeading.setText("Tafsīr al-Jalālayn · "+verse+" · anglais");
        tafsirScroll.scrollTo(0,0);
        if(!lateralTafsir){
            // Keep commentary in a separate pane: opening it never scrolls the
            // two source pages out of the upper comparison viewport.
            int available=workspace.getHeight();
            if(available>0){
                int desired=Math.max(Ui.dp(this,180),Math.round(available*0.40f));
                int panelHeight=Math.min(desired,Math.round(available*0.50f));
                LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)tafsirPanel.getLayoutParams();
                params.height=panelHeight;tafsirPanel.setLayoutParams(params);
            }
        }
        comparisonScroll.scrollTo(0,0);
        comparisonScroll.post(this::fitVisiblePages);
    }

    private void fitVisiblePages(){
        if(pagePair==null||comparisonScroll==null)return;
        android.graphics.Rect visible=new android.graphics.Rect();
        int viewportHeight=comparisonScroll.getHeight();
        if(comparisonScroll.getGlobalVisibleRect(visible))
            viewportHeight=Math.min(viewportHeight,visible.height());
        if(viewportHeight<=pageHeaderHeight)return;
        if(selectedTafsir>=0&&!lateralTafsir&&workspace.getHeight()>0){
            int available=workspace.getHeight();
            int panelHeight=Math.min(Math.max(Ui.dp(this,180),Math.round(available*0.40f)),
                Math.round(available*0.50f));
            LinearLayout.LayoutParams panelParams=(LinearLayout.LayoutParams)tafsirPanel.getLayoutParams();
            if(panelParams.height!=panelHeight){panelParams.height=panelHeight;tafsirPanel.setLayoutParams(panelParams);}
        }
        // The entire physical 345:550 page always fits in its own viewport;
        // links and the optional detail strips remain reachable below it.
        int natural=Math.round(Math.max(1,(comparisonScroll.getWidth()-Ui.dp(this,18))/2f)*550f/345f);
        int available=Math.max(Ui.dp(this,32),viewportHeight-pageHeaderHeight-Ui.dp(this,8)
            -(selectedTafsir<0?comparisonDetailHeight:0));
        int height=Math.min(natural,available);
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)pagePair.getLayoutParams();
        if(params.height!=height){params.height=height;pagePair.setLayoutParams(params);}
    }

    private void loadBothTafsirs(VerseRef first,VerseRef second){
        final int leftToken=++leftTafsirRequest,rightToken=++rightTafsirRequest;
        leftTafsir.setText("Chargement du Tafsīr al-Jalālayn…");
        rightTafsir.setText("Chargement du Tafsīr al-Jalālayn…");
        tafsirIo.execute(()->{
            loadOneTafsir(first,leftTafsir,0,leftToken);
            loadOneTafsir(second,rightTafsir,1,rightToken);
        });
    }

    private void loadContextualTafsir(int side,VerseRef verse){
        TextView target=side==0?leftTafsir:rightTafsir;
        final int token=side==0?++leftTafsirRequest:++rightTafsirRequest;
        target.setText("Chargement du Tafsīr al-Jalālayn…");
        tafsirIo.execute(()->loadOneTafsir(verse,target,side,token));
    }

    private void loadOneTafsir(VerseRef verse,TextView target,int side,int token){
        TafsirRepository.Entry entry=null;
        try {entry=tafsirRepository.load(verse);}
        catch(Exception unavailable){android.util.Log.w("HaafidhAtlasTafsir",
            "Jalalayn unavailable for "+verse,unavailable);}
        final CharSequence result=entry==null
            ?"Tafsīr al-Jalālayn indisponible pour ce verset."
            :BaqaraPilotScreen.renderExistingJalalayn(entry);
        runOnUiThread(()->{
            if(disposed||isFinishing()
                    ||token!=(side==0?leftTafsirRequest:rightTafsirRequest))return;
            target.setText(result);
        });
    }

    private TextView readingLink(boolean first){
        TextView link=text("",14f,true);
        link.setGravity(Gravity.CENTER);
        link.setPaintFlags(link.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        link.setContentDescription(first?"Ouvrir le premier verset en Lecture":
            "Ouvrir le second verset en Lecture");
        link.setOnClickListener(v->openReader(first,first?currentLeftPage:currentRightPage));
        return link;
    }

    private AtlasDiscriminantCases.Entry current(){
        return AtlasDiscriminantCases.PILOT.get(selectedIndex);
    }

    private void render() {
        // Never retain a previous pair's glyphs under the new reference labels.
        // Keep the WebView attached/visible so Chromium can complete animation
        // frame readiness callbacks, while making every old glyph transparent.
        strips.setAlpha(0f);
        strips.clearComparison();
        AtlasDiscriminantCases.Entry c=current();
        if(c.first.equals(c.second))throw new IllegalStateException("Self-comparison forbidden");
        currentLeftPage=geometry.pageForVerse(c.first);
        currentRightPage=geometry.pageForVerse(c.second);
        if(currentLeftPage<1||currentLeftPage>604||currentRightPage<1||currentRightPage>604)
            throw new IllegalStateException("Invalid QCF page");

        if(selectedLeftVerse==null||geometry.pageForVerse(selectedLeftVerse)!=currentLeftPage)
            selectedLeftVerse=c.first;
        if(selectedRightVerse==null||geometry.pageForVerse(selectedRightVerse)!=currentRightPage)
            selectedRightVerse=c.second;
        leftHeader.setText(QuranSurahNames.name(c.first.getSurah())+" · "+c.first+" · p."+currentLeftPage);
        rightHeader.setText(QuranSurahNames.name(c.second.getSurah())+" · "+c.second+" · p."+currentRightPage);
        // BOOX: never decode two 600 KB original SVG pages and two crops simultaneously.
        // Load the second page only after the first confirms a real image.onload;
        // otherwise one failed image could hide a missing QCF resource indefinitely.
        leftPageStatus=0;rightPageStatus=0;
        right.cancelPendingAtlasRender();
        right.setVisibility(View.INVISIBLE);
        // Highlights the exact original SVG ayah polygon.
        left.setAtlasWordDifferences(c.first,c.firstWordIndices);
        left.showPages(currentLeftPage,currentLeftPage,currentLeftPage,
            c.first.getSurah(),c.first.getAyah(),c.first.getAyah(),false);

        int position=caseIndices.indexOf(selectedIndex);
        title.setText(c.first+" ↔ "+c.second+(caseIndices.size()>1?
            " · "+(position+1)+"/"+caseIndices.size():""));
        previousCase.setVisibility(caseIndices.size()>1?View.VISIBLE:View.GONE);
        nextCase.setVisibility(caseIndices.size()>1?View.VISIBLE:View.GONE);
        previousCase.setEnabled(position>0);nextCase.setEnabled(position+1<caseIndices.size());
        firstTafsirTab.setText(c.first.toString());
        secondTafsirTab.setText(c.second.toString());
        loadBothTafsirs(selectedLeftVerse,selectedRightVerse);
        if(selectedTafsir>=0)selectTafsir(selectedTafsir);
        leftRead.setText("Lire "+c.first+" en Lecture");
        rightRead.setText("Lire "+c.second+" en Lecture");
        String origin=getIntent().getStringExtra(EXTRA_ORIGIN);
        String label="Révision active".equals(origin)?"Révision active":
            "Révision passive".equals(origin)?"Révision passive":"Lecture";
        returnToSource.setText("← Retour à "+label);

    }

    private void moveCase(int direction){
        int next=caseIndices.indexOf(selectedIndex)+direction;
        if(next<0||next>=caseIndices.size())return;
        selectedIndex=caseIndices.get(next);
        selectedLeftVerse=null;selectedRightVerse=null;render();
    }

    private void showRightAfterLeft(){
        if(rightPageStatus!=0)return;
        AtlasDiscriminantCases.Entry c=current();
        right.setVisibility(View.VISIBLE);
        right.setAtlasWordDifferences(c.second,c.secondWordIndices);
        right.showPages(currentRightPage,currentRightPage,currentRightPage,
            c.second.getSurah(),c.second.getAyah(),c.second.getAyah(),false);
    }

    private void showStripsAfterPages() {
        if(leftPageStatus==1 && rightPageStatus==1 && strips!=null){
            AtlasDiscriminantCases.Entry entry=current();
            strips.show(entry.first,currentLeftPage,entry.firstWordIndices,
                entry.second,currentRightPage,entry.secondWordIndices);
        }
    }
    boolean qcfStripsVisibleForTest(){return strips!=null&&strips.getVisibility()==View.VISIBLE&&strips.getAlpha()>0f;}
    int qcfStripsGenerationForTest(){return strips.generationForTest();}
    boolean qcfStripsReadyForTest(){return strips!=null&&strips.bothRenderedForTest();}
    android.graphics.Rect[] qcfCropBoundsForTest(){
        android.graphics.Rect first=new android.graphics.Rect(),second=new android.graphics.Rect();
        if(strips!=null){
            android.graphics.Rect whole=new android.graphics.Rect();
            if(strips.getGlobalVisibleRect(whole)){
                int half=(whole.left+whole.right)/2;
                first.set(whole.left,whole.top,half-3,whole.bottom);
                second.set(half+3,whole.top,whole.right,whole.bottom);
            }
        }
        return new android.graphics.Rect[]{first,second};
    }

    android.graphics.Rect[] qcfPageBoundsForTest(){
        android.graphics.Rect first=new android.graphics.Rect(),second=new android.graphics.Rect();
        if(left!=null)left.getGlobalVisibleRect(first);
        if(right!=null)right.getGlobalVisibleRect(second);
        return new android.graphics.Rect[]{first,second};
    }
    boolean qcfPagesReadyForTest(){return leftPageStatus==1&&rightPageStatus==1;}
    boolean tafsirColumnsReadyForTest(){return leftTafsir!=null&&rightTafsir!=null;}
    CharSequence[] tafsirTextsForTest(){return new CharSequence[]{leftTafsir==null?"":leftTafsir.getText(),
        rightTafsir==null?"":rightTafsir.getText()};}
    int qcfPagesFailedForTest(){return (leftPageStatus==-1?1:0)+(rightPageStatus==-1?1:0);}
    String qcfFailureDetailForTest(){
        return "leftPage="+currentLeftPage+" status="+leftPageStatus+" cause="+left.rendererDiagnosticForTest()
            +" rightPage="+currentRightPage+" status="+rightPageStatus
            +" tafsir="+(leftTafsir!=null&&rightTafsir!=null?"paired":"unavailable");
    }

    private void showExplanation(){
        AtlasDiscriminantCases.Entry c=current();
        new AlertDialog.Builder(this)
            .setTitle("Indice de distinction · "+c.first+" / "+c.second)
            .setMessage(c.explanation+"\n\nRéférence : "+c.source+
                "\n"+c.sourceUrl+"\n\nLes versets sont visibles dans le Muṣḥaf QCF original.")
            .setPositiveButton("Fermer",null)
            .show();
    }

    private void openReader(boolean first,int page){
        AtlasDiscriminantCases.Entry c=current();
        VerseRef target=first?c.first:c.second;
        int min=geometry.pageForVerse(target);
        int max=MawdudiMapModel.lastPageForVerse(target,geometry);
        int shown=Math.max(min,Math.min(max,page));
        Intent intent=new Intent(this,StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,shown);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,target.toString());
        startActivity(intent);
    }

    int selectedTafsirForTest(){return selectedTafsir;}
    VerseRef selectedTafsirVerseForTest(){
        return selectedTafsir==0?selectedLeftVerse:selectedTafsir==1?selectedRightVerse:null;
    }
    boolean tafsirPanelVisibleForTest(){return tafsirPanel!=null&&tafsirPanel.getVisibility()==View.VISIBLE;}
    boolean lateralTafsirForTest(){return lateralTafsir;}
    boolean tafsirHeadingVisibleForTest(){
        android.graphics.Rect visible=new android.graphics.Rect();
        return tafsirHeading!=null&&tafsirHeading.getGlobalVisibleRect(visible)
            &&visible.height()==tafsirHeading.getHeight();
    }
    boolean bothQcfPagesFullyVisibleForTest(){
        for(MapQcfThumbnails page:new MapQcfThumbnails[]{left,right}){
            android.graphics.Rect visible=new android.graphics.Rect();
            if(page==null||!page.getGlobalVisibleRect(visible)||visible.width()!=page.getWidth()
                ||visible.height()!=page.getHeight())return false;
        }
        return true;
    }
    MapQcfThumbnails[] qcfViewsForTest(){return new MapQcfThumbnails[]{left,right};}

    int getCurrentLeftPageForTest() { return currentLeftPage; }

    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("caseIndex",selectedIndex);
        state.putInt("tafsirSide",selectedTafsir);
        if(selectedLeftVerse!=null)state.putString("tafsirLeftVerse",selectedLeftVerse.toString());
        if(selectedRightVerse!=null)state.putString("tafsirRightVerse",selectedRightVerse.toString());
        super.onSaveInstanceState(state);
    }
    /** Chromium WebViews must be removed from the view hierarchy before destroy().
     * This also prevents renderer teardown racing with the next Atlas comparison. */
    private static void detachQcf(android.webkit.WebView view) {
        if(view==null)return;
        android.view.ViewParent parent=view.getParent();
        if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(view);
        view.destroy();
    }

    @Override protected void onDestroy(){
        if(!disposed){
            disposed=true;
            detachQcf(left);
            detachQcf(right);
            detachQcf(strips);
            leftTafsirRequest++;
            rightTafsirRequest++;
            tafsirIo.shutdownNow();
        }
        super.onDestroy();
    }

}
