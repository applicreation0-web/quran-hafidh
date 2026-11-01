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
    private QcfAtlasVerseStrips strips,secondaryStrips;
    private LinearLayout secondaryHost,stripHome,tafsirSnippetHost;
    private TextView secondaryCaption,tafsirPivotCaption;
    private final ExecutorService tafsirIo=Executors.newSingleThreadExecutor();
    private TafsirRepository tafsirRepository;
    private int leftTafsirRequest,rightTafsirRequest;
    private VerseRef selectedLeftVerse,selectedRightVerse;
    private TextView leftTafsir,rightTafsir,tafsirHeading;
    private LinearLayout tafsirPanel;
    private ScrollView tafsirScroll,comparisonScroll;
    private LinearLayout workspace,pagePair;
    private int pageHeaderHeight,comparisonDetailHeight,defaultStripHeightDp;
    private TextView firstTafsirTab,secondTafsirTab;
    private int selectedTafsir=-1;
    private boolean lateralTafsir,booxPortraitTafsir;
    private int builtOrientation=android.content.res.Configuration.ORIENTATION_UNDEFINED;
    private TextView title, leftHeader, rightHeader, relationLine;
    private TextView returnToSource;
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
        builtOrientation=getResources().getConfiguration().orientation;
        float density=getResources().getDisplayMetrics().density;
        int dpHeight=(int)(getResources().getDisplayMetrics().heightPixels/density);
        int dpWidth=(int)(getResources().getDisplayMetrics().widthPixels/density);
        boolean compact=dpHeight<740;
        boolean landscape=getResources().getConfiguration().orientation==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        // Portrait BOOX: full-width original 345:550 pages ABOVE Tafsir.
        // Landscape BOOX and phone retain the contextual lateral pane.
        booxPortraitTafsir=!landscape&&dpWidth>=900;
        lateralTafsir=landscape&&dpWidth>=680;
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
        // Visible, read-only classification: the relation must not be buried
        // behind the information dialog. No synthesized Quranic wording.
        relationLine=text("",compact?13:15,true);
        relationLine.setGravity(Gravity.CENTER);
        relationLine.setSingleLine(true);
        relationLine.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(relationLine,new LinearLayout.LayoutParams(-1,Ui.dp(this,34)));
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
        // The exact verse header replaces a whole redundant "Lire ... en Lecture"
        // row. Clicking it retains direct navigation; the QCF page itself opens Tafsir.
        leftHeader.setOnClickListener(v->openReader(true,currentLeftPage));
        rightHeader.setOnClickListener(v->openReader(false,currentRightPage));
        leftHeader.setClickable(true);rightHeader.setClickable(true);
        heads.addView(leftHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        heads.addView(rightHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        body.addView(heads);
        pageHeaderHeight=Ui.dp(this,48);
        // Spend the reclaimed 48dp on genuine enlarged QCF word excerpts,
        // not on extra navigation bars; full physical pages remain proportional.
        int stripHeightDp=compact?130:(dpHeight>=1000?205:158);
        defaultStripHeightDp=stripHeightDp;
        comparisonDetailHeight=Ui.dp(this,stripHeightDp);

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

        // A short source-vector context stays beside the same two localizations.
        strips=new QcfAtlasVerseStrips(this);
        strips.setOnRendered(()->{
            if(!disposed&&leftPageStatus==1&&rightPageStatus==1&&strips.bothRenderedForTest())
                strips.setAlpha(1f);
            if(!disposed)showSecondaryStripsAfterPrimary();
        });
        stripHome=Ui.column(this);
        body.addView(stripHome);
        stripHome.addView(strips,new LinearLayout.LayoutParams(-1,Ui.dp(this,stripHeightDp)));
        // Created lazily only for the two independently reviewed double pivots.
        // Ordinary 279-catalogue comparison retains exactly one pair of QCF strips.
        secondaryHost=Ui.column(this);
        secondaryHost.setVisibility(View.GONE);
        stripHome.addView(secondaryHost);
        tafsirPanel=Ui.column(this);
        tafsirPanel.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),Ui.dp(this,4));
        LinearLayout tabs=Ui.row(this);
        firstTafsirTab=tafsirTab(0);secondTafsirTab=tafsirTab(1);
        tabs.addView(firstTafsirTab,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        tabs.addView(secondTafsirTab,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        tabs.addView(Ui.iconButton(this,"","Fermer le Tafsir",v->closeTafsir()),
            new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        tafsirPanel.addView(tabs);
        tafsirHeading=text("",14,true);
        tafsirHeading.setSingleLine(true);
        tafsirHeading.setEllipsize(TextUtils.TruncateAt.END);
        tafsirHeading.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,4));
        tafsirPanel.addView(tafsirHeading);
        leftTafsir=tafsirText();rightTafsir=tafsirText();
        LinearLayout tafsirBodies=Ui.column(this);
        // Move (never duplicate) the already decoded authentic-QCF difference
        // excerpts into this scrollable pane only when Tafsir is requested.
        // The original al-Jalalayn commentary remains completely unaltered.
        tafsirSnippetHost=Ui.column(this);
        tafsirPivotCaption=text("",12,true);
        tafsirPivotCaption.setPadding(Ui.dp(this,4),0,0,0);
        // This verbal caption is accessibility metadata, not another screen row:
        // the exact original-QCF pivot underlines already carry the visual cue.
        tafsirPivotCaption.setVisibility(View.GONE);
        tafsirSnippetHost.addView(tafsirPivotCaption);
        tafsirBodies.addView(tafsirSnippetHost);
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
        if(verse==null||side<0||side>1)return;
        AtlasDiscriminantCases.Entry pair=current();
        VerseRef allowed=side==0?pair.first:pair.second;
        // Pair-locked Tafsir: other ayah polygons on these physical QCF pages
        // are NOT part of this correspondance and may never switch its source.
        if(!allowed.equals(verse)||page!=(side==0?currentLeftPage:currentRightPage))
            return;
        selectTafsir(side);
    }

    private void selectTafsir(int side){
        if(side<0||side>1)return;
        selectedTafsir=side;
        movePivotIntoTafsir(side);
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
                int panelHeight=contextualTafsirHeight(available);
                LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)tafsirPanel.getLayoutParams();
                params.height=panelHeight;tafsirPanel.setLayoutParams(params);
            }
        }
        comparisonScroll.scrollTo(0,0);
        comparisonScroll.post(()->{
            fitVisiblePages();
            if(comparisonScroll.getScrollY()!=0)comparisonScroll.scrollTo(0,0);
        });
    }

    private void closeTafsir(){
        selectedTafsir=-1;
        movePivotIntoTafsir(-1);
        tafsirPanel.setVisibility(View.GONE);
        comparisonScroll.post(this::fitVisiblePages);
    }

    private void movePivotIntoTafsir(int side){
        if(strips==null||stripHome==null||tafsirSnippetHost==null)return;
        boolean readingTafsir=side>=0;
        LinearLayout destination=readingTafsir?tafsirSnippetHost:stripHome;
        // Keep two complete original-QCF pivot lines within the shorter mobile
        // Tafsir viewport; BOOX keeps its larger 110dp strip.
        // Narrow phone landscape has a lateral Tafsir column but only ~400dp
        // of physical screen height. A 110dp Quran strip hid all commentary.
        // Keep the authentic QCF crop, with 58dp on that short landscape;
        // BOOX landscape keeps its original 110dp.
        int screenHeightDp=(int)(getResources().getDisplayMetrics().heightPixels
            /getResources().getDisplayMetrics().density);
        boolean shortLandscape=lateralTafsir
            &&getResources().getConfiguration().orientation==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            &&screenHeightDp<600;
        int height=readingTafsir?(shortLandscape?58:(lateralTafsir?110:72)):defaultStripHeightDp;
        if(strips.getParent()!=destination){
            if(strips.getParent() instanceof ViewGroup)
                ((ViewGroup)strips.getParent()).removeView(strips);
            destination.addView(strips,readingTafsir?1:0,
                new LinearLayout.LayoutParams(-1,Ui.dp(this,height)));
        }else{
            LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)strips.getLayoutParams();
            if(params.height!=Ui.dp(this,height)){
                params.height=Ui.dp(this,height);strips.setLayoutParams(params);
            }
        }
        if(secondaryHost!=null&&secondaryHost.getParent()!=destination){
            if(secondaryHost.getParent() instanceof ViewGroup)
                ((ViewGroup)secondaryHost.getParent()).removeView(secondaryHost);
            destination.addView(secondaryHost);
        }
        strips.setTafsirFocusSide(side);
        if(secondaryStrips!=null)secondaryStrips.setTafsirFocusSide(side);
        if(readingTafsir){
            AtlasDiscriminantCases.Entry pair=current();
            VerseRef active=side==0?pair.first:pair.second;
            tafsirPivotCaption.setText(AtlasDiscriminantOmissions.explicitlyAbsent(pair,side==0)
                ?"Absence en "+active+" · repère QCF contextuel, non souligné"
                :"Mot(s) de bifurcation souligné(s) · QCF "+active);
            tafsirPivotCaption.setVisibility(View.GONE);
            strips.setContentDescription(tafsirPivotCaption.getText());
        }else{
            tafsirPivotCaption.setVisibility(View.GONE);
        }
    }

    private int contextualTafsirHeight(int available){
        if(booxPortraitTafsir){
            // Preserve ~70% of the actual BOOX portrait height for BOTH enlarged,
            // fully visible original QCF pages. Commentary scrolls in its lower
            // 30% zone, so none of the 15-line SVG geometry is distorted.
            return Math.min(Math.max(Ui.dp(this,250),Math.round(available*0.30f)),
                Math.round(available*0.35f));
        }
        return Math.min(Math.max(Ui.dp(this,210),Math.round(available*0.52f)),
            Math.round(available*0.58f));
    }

    private void fitVisiblePages(){
        if(pagePair==null||comparisonScroll==null)return;
        if(selectedTafsir>=0&&!lateralTafsir&&workspace.getHeight()>0){
            int available=workspace.getHeight();
            int panelHeight=contextualTafsirHeight(available);
            LinearLayout.LayoutParams panelParams=(LinearLayout.LayoutParams)tafsirPanel.getLayoutParams();
            if(panelParams.height!=panelHeight){panelParams.height=panelHeight;tafsirPanel.setLayoutParams(panelParams);}
        }
        android.graphics.Rect visible=new android.graphics.Rect();
        int viewportHeight=comparisonScroll.getHeight();
        if(comparisonScroll.getGlobalVisibleRect(visible))
            viewportHeight=Math.min(viewportHeight,visible.height());
        if(viewportHeight<=pageHeaderHeight)return;
        // The entire physical 345:550 page always fits in its own viewport;
        // links and the optional detail strips remain reachable below it.
        int natural=Math.round(Math.max(1,(comparisonScroll.getWidth()-Ui.dp(this,18))/2f)*550f/345f);
        int available=Math.max(Ui.dp(this,32),viewportHeight-pageHeaderHeight-Ui.dp(this,8)
            -(selectedTafsir<0?comparisonDetailHeight:0));
        int height=Math.min(natural,available);
        // Some phone WebViews contribute extra native bottom padding after the
        // Jalalayn panel becomes visible. The ScrollView is at scrollY=0, yet a
        // physically measured 728px QCF page can have only 707px visible.
        // Cap against the ACTUAL onscreen bottom, preserving every original
        // 345:550 SVG glyph and all strict full-visibility assertions.
        android.graphics.Rect scrollVisible=new android.graphics.Rect();
        if(comparisonScroll.getGlobalVisibleRect(scrollVisible)
                &&pagePair.isAttachedToWindow()&&pagePair.getTop()>0){
            int[] pageOnScreen=new int[2];
            pagePair.getLocationOnScreen(pageOnScreen);
            int trueAvailable=scrollVisible.bottom-pageOnScreen[1]-Ui.dp(this,4);
            if(trueAvailable>=Ui.dp(this,32))
                height=Math.min(height,trueAvailable);
        }
        LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)pagePair.getLayoutParams();
        if(params.height!=height){params.height=height;pagePair.setLayoutParams(params);}
        // Native ScrollView can restore a stale focus scroll after contextual pane resize.
        // Keep the *full* two physical QCF pages visible initially; ordinary manual
        // reading scroll remains available without a layout change.
        if(selectedTafsir>=0&&comparisonScroll.getScrollY()!=0)
            comparisonScroll.scrollTo(0,0);
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

    private void loadOneTafsir(VerseRef verse,TextView target,int side,int token){
        TafsirRepository.Entry entry=null;
        try {entry=tafsirRepository.load(verse);}
        catch(Exception unavailable){android.util.Log.w("HaafidhAtlasTafsir",
            "Jalalayn unavailable for "+verse,unavailable);}
        final CharSequence result=entry==null
            ?"Tafsīr al-Jalālayn indisponible pour ce verset."
            :renderPairOnlyJalalayn(entry);
        runOnUiThread(()->{
            if(disposed||isFinishing()
                    ||token!=(side==0?leftTafsirRequest:rightTafsirRequest))return;
            target.setText(result);
        });
    }

    /**
     * Attribution is already pinned above the two canonical Tafsir tabs.
     * Keep every original Jalalayn commentary run/note/style unchanged but
     * omit the duplicate bibliography line inside this compact pair-only pane.
     * Other Atlas/Lecture Jalalayn renderers are not affected.
     */
    private static CharSequence renderPairOnlyJalalayn(TafsirRepository.Entry entry){
        CharSequence source=BaqaraPilotScreen.renderExistingJalalayn(entry);
        String attribution=entry.metadataLine();
        String prefix=attribution.isEmpty()?"":attribution+"\n\n";
        if(prefix.isEmpty()||!source.toString().startsWith(prefix))return source;
        return new android.text.SpannableStringBuilder(source).delete(0,prefix.length());
    }

    private AtlasDiscriminantCases.Entry current(){
        return AtlasDiscriminantCases.PILOT.get(selectedIndex);
    }

    /** Only presentation vocabulary changes. The frozen 279-case audit and
     * protected 1.17.8 AtlasDiscriminantOmissions adapter stay byte exact. */
    static String displayRelation(AtlasDiscriminantCases.Entry entry){
        String raw=AtlasDiscriminantOmissions.relation(entry);
        if("Deux bifurcations".equals(raw))return "Deux différences successives";
        if("Ajout puis bifurcation".equals(raw))return "Ajout puis mot pivot";
        if("Bifurcation : groupe de mots".equals(raw))return "Groupe de mots différent";
        if("Variation de mot ou de particule".equals(raw))return "Mot pivot différent";
        if("Même amorce, bifurcation".equals(raw)
                ||"Bifurcation après tronc commun".equals(raw))
            return "Même début, suite différente";
        return raw;
    }

    private void render() {
        // Never retain a previous pair's glyphs under the new reference labels.
        // Keep the WebView attached/visible so Chromium can complete animation
        // frame readiness callbacks, while making every old glyph transparent.
        strips.setAlpha(0f);
        strips.clearComparison();
        if(secondaryStrips!=null){
            secondaryStrips.setAlpha(0f);
            secondaryStrips.clearComparison();
        }
        if(secondaryHost!=null)secondaryHost.setVisibility(View.GONE);
        AtlasDiscriminantCases.Entry c=current();
        if(c.first.equals(c.second))throw new IllegalStateException("Self-comparison forbidden");
        currentLeftPage=geometry.pageForVerse(c.first);
        currentRightPage=geometry.pageForVerse(c.second);
        if(currentLeftPage<1||currentLeftPage>604||currentRightPage<1||currentRightPage>604)
            throw new IllegalStateException("Invalid QCF page");

        // The pair is canonical and immutable even after restore/rotation/case switching:
        // never carry arbitrary adjacent-ayah Tafsir references across comparisons.
        selectedLeftVerse=c.first;
        selectedRightVerse=c.second;
        updateContextualHeaders();
        // BOOX: never decode two 600 KB original SVG pages and two crops simultaneously.
        // Load the second page only after the first confirms a real image.onload;
        // otherwise one failed image could hide a missing QCF resource indefinitely.
        leftPageStatus=0;rightPageStatus=0;
        right.cancelPendingAtlasRender();
        right.setVisibility(View.INVISIBLE);
        // Highlights the exact original SVG ayah polygon.
        left.setAtlasWordDifferences(c.first,AtlasDiscriminantOmissions.actualDifference(c,true));
        left.showPages(currentLeftPage,currentLeftPage,currentLeftPage,
            c.first.getSurah(),c.first.getAyah(),c.first.getAyah(),false);

        int position=caseIndices.indexOf(selectedIndex);
        String relationText="Relation · "+displayRelation(c);
        relationLine.setText(relationText);
        relationLine.setContentDescription(relationText);
        title.setText(c.first+" ↔ "+c.second+(caseIndices.size()>1?
            " · "+(position+1)+"/"+caseIndices.size():""));
        previousCase.setVisibility(caseIndices.size()>1?View.VISIBLE:View.GONE);
        nextCase.setVisibility(caseIndices.size()>1?View.VISIBLE:View.GONE);
        previousCase.setEnabled(position>0);nextCase.setEnabled(position+1<caseIndices.size());
        firstTafsirTab.setText(selectedLeftVerse.toString());
        secondTafsirTab.setText(selectedRightVerse.toString());
        loadBothTafsirs(selectedLeftVerse,selectedRightVerse);
        if(selectedTafsir>=0)selectTafsir(selectedTafsir);
        updateContextualHeaders();
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
        right.setAtlasWordDifferences(c.second,AtlasDiscriminantOmissions.actualDifference(c,false));
        right.showPages(currentRightPage,currentRightPage,currentRightPage,
            c.second.getSurah(),c.second.getAyah(),c.second.getAyah(),false);
    }

    private void showStripsAfterPages() {
        if(leftPageStatus==1 && rightPageStatus==1 && strips!=null){
            AtlasDiscriminantCases.Entry entry=current();
            strips.show(entry.first,currentLeftPage,
                AtlasDiscriminantOmissions.actualDifference(entry,true),
                AtlasDiscriminantOmissions.cropAnchor(entry,true),
                entry.second,currentRightPage,
                AtlasDiscriminantOmissions.actualDifference(entry,false),
                AtlasDiscriminantOmissions.cropAnchor(entry,false));
        }
    }
    private void showSecondaryStripsAfterPrimary(){
        AtlasDiscriminantCases.Entry entry=current();
        if(!AtlasDiscriminantOmissions.hasSecondary(entry.auditId)
                ||secondaryHost==null||leftPageStatus!=1||rightPageStatus!=1)return;
        if(secondaryStrips==null){
            secondaryCaption=text("",13,true);
            secondaryCaption.setGravity(Gravity.CENTER);
            secondaryHost.addView(secondaryCaption,
                new LinearLayout.LayoutParams(-1,Ui.dp(this,29)));
            secondaryStrips=new QcfAtlasVerseStrips(this);
            secondaryStrips.setTafsirFocusSide(selectedTafsir);
            secondaryStrips.setOnRendered(()->{
                if(!disposed&&secondaryStrips!=null&&secondaryStrips.bothRenderedForTest())
                    secondaryStrips.setAlpha(1f);
            });
            secondaryHost.addView(secondaryStrips,
                new LinearLayout.LayoutParams(-1,Ui.dp(this,108)));
        }
        secondaryCaption.setText(entry.auditId==199
            ?"Ajout en 9:105 · absent en 9:94"
            :"Seconde bifurcation après l’amorce commune");
        secondaryHost.setVisibility(View.VISIBLE);
        secondaryStrips.setAlpha(0f);
        secondaryStrips.show(entry.first,currentLeftPage,
            AtlasDiscriminantOmissions.secondaryDifference(entry,true),
            AtlasDiscriminantOmissions.secondaryCropAnchor(entry,true),
            entry.second,currentRightPage,
            AtlasDiscriminantOmissions.secondaryDifference(entry,false),
            AtlasDiscriminantOmissions.secondaryCropAnchor(entry,false));
    }

    boolean qcfSecondaryReadyForTest(){
        if(!AtlasDiscriminantOmissions.hasSecondary(current().auditId))return true;
        return secondaryStrips!=null&&secondaryStrips.bothRenderedForTest()
            &&secondaryStrips.getVisibility()==View.VISIBLE&&secondaryStrips.getAlpha()>0f;
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

    /**
     * Short, factual explanation from individually approved editorial metadata.
     * The 279 historical "explanation" fields are deliberately empty: never
     * fill them with invented religious commentary or automatically translated
     * Arabic. Show the audited relation and exact source-word positions instead.
     */
    private void showExplanation(){
        AtlasDiscriminantCases.Entry c=current();
        StringBuilder description=new StringBuilder("Type de relation : ")
            .append(displayRelation(c))
            .append("\n\n").append(c.first).append(" · mot(s) ")
            .append(wordPositions(AtlasDiscriminantOmissions.actualDifference(c,true)))
            .append("\n").append(c.second).append(" · mot(s) ")
            .append(wordPositions(AtlasDiscriminantOmissions.actualDifference(c,false)));
        if(!TextUtils.isEmpty(c.explanation))
            description.append("\n\n").append(c.explanation);
        description.append("\n\nLes différences sont soulignées dans le Muṣḥaf QCF original.")
            .append("\nSource : ").append(c.source);
        if(!TextUtils.isEmpty(c.sourceUrl))description.append("\n").append(c.sourceUrl);
        new AlertDialog.Builder(this)
            .setTitle("Versets ressemblants · "+c.first+" / "+c.second)
            .setMessage(description.toString())
            .setPositiveButton("Fermer",null)
            .show();
    }

    private static String wordPositions(int[] indices){
        if(indices==null||indices.length==0)return "absent";
        StringBuilder value=new StringBuilder();
        for(int index:indices){
            if(value.length()>0)value.append(", ");
            value.append(index);
        }
        return value.toString();
    }

    private void updateContextualHeaders(){
        if(leftHeader==null||rightHeader==null)return;
        AtlasDiscriminantCases.Entry c=current();
        VerseRef first=selectedLeftVerse==null?c.first:selectedLeftVerse;
        VerseRef second=selectedRightVerse==null?c.second:selectedRightVerse;
        leftHeader.setText(QuranSurahNames.name(first.getSurah())+" · "+first+" · p."+currentLeftPage+" ↗"
            +(first.equals(c.first)&&AtlasDiscriminantOmissions.explicitlyAbsent(c,true)?" · absent":""));
        rightHeader.setText(QuranSurahNames.name(second.getSurah())+" · "+second+" · p."+currentRightPage+" ↗"
            +(second.equals(c.second)&&AtlasDiscriminantOmissions.explicitlyAbsent(c,false)?" · absent":""));
        leftHeader.setContentDescription("Ouvrir le premier verset en Lecture : "+first);
        rightHeader.setContentDescription("Ouvrir le second verset en Lecture : "+second);
    }

    private void openReader(boolean first,int page){
        AtlasDiscriminantCases.Entry c=current();
        // The link follows the real QCF ayah chosen for contextual Jalalayn,
        // rather than silently jumping back to the fixed pair anchor.
        VerseRef target=first
            ?(selectedLeftVerse==null?c.first:selectedLeftVerse)
            :(selectedRightVerse==null?c.second:selectedRightVerse);
        int min=geometry.pageForVerse(target);
        int max=MawdudiMapModel.lastPageForVerse(target,geometry);
        int shown=Math.max(min,Math.min(max,page));
        Intent intent=new Intent(this,StudyReaderActivity.class);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,shown);
        intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,target.toString());
        startActivity(intent);
    }

    int comparisonScrollYForTest(){return comparisonScroll==null?0:comparisonScroll.getScrollY();}
    String qcfViewportDetailForTest(){
        android.graphics.Rect scrollBounds=new android.graphics.Rect();
        android.graphics.Rect scrollLocal=new android.graphics.Rect();
        android.graphics.Rect pairBounds=new android.graphics.Rect();
        boolean scrollGlobal=comparisonScroll.getGlobalVisibleRect(scrollBounds);
        boolean scrollInside=comparisonScroll.getLocalVisibleRect(scrollLocal);
        pagePair.getGlobalVisibleRect(pairBounds);
        return "scrollGlobal="+scrollGlobal+" scrollLocal="+scrollInside
            +" scrollMeasured="+comparisonScroll.getWidth()+"x"+comparisonScroll.getHeight()
            +" scrollBounds="+scrollBounds+" localBounds="+scrollLocal
            +" pairMeasured="+pagePair.getWidth()+"x"+pagePair.getHeight()
            +" pairTop="+pagePair.getTop()+" pairBounds="+pairBounds
            +" workspace="+workspace.getWidth()+"x"+workspace.getHeight()
            +" tafsir="+tafsirPanel.getWidth()+"x"+tafsirPanel.getHeight()
            +" lateral="+lateralTafsir+" selectedTafsir="+selectedTafsir;
    }
    int selectedTafsirForTest(){return selectedTafsir;}
    String tafsirTabLabelForTest(int side){
        return (side==0?firstTafsirTab:secondTafsirTab).getText().toString();
    }
    VerseRef selectedTafsirVerseForTest(){
        return selectedTafsir==0?selectedLeftVerse:selectedTafsir==1?selectedRightVerse:null;
    }
    boolean tafsirPanelVisibleForTest(){return tafsirPanel!=null&&tafsirPanel.getVisibility()==View.VISIBLE;}
    boolean tafsirPivotEmbeddedForTest(){
        return selectedTafsir>=0 && strips!=null && strips.getParent()==tafsirSnippetHost
            && strips.tafsirFocusSideForTest()==selectedTafsir
            && !TextUtils.isEmpty(strips.getContentDescription());
    }
    /** The actual Jalalayn text, not just its tabs and QCF pivot, must appear
     * in the initial viewport on phone and BOOX. */
    boolean tafsirTextVisibleForTest(){
        if(selectedTafsir<0||tafsirScroll==null)return false;
        TextView text=selectedTafsir==0?leftTafsir:rightTafsir;
        if(text==null||text.getVisibility()!=View.VISIBLE||text.getText().length()<100)return false;
        android.graphics.Rect viewport=new android.graphics.Rect(),visible=new android.graphics.Rect();
        tafsirScroll.getGlobalVisibleRect(viewport);
        text.getGlobalVisibleRect(visible);
        return !viewport.isEmpty()&&!visible.isEmpty()&&viewport.intersect(visible)
            &&viewport.height()>=Ui.dp(this,16);
    }

    /** First commentary paragraphs (not merely Jalalayn edition metadata)
     * must be visible on opening the mobile pane, with no initial scroll. */
    boolean tafsirCommentaryVisibleForTest(){
        if(!tafsirTextVisibleForTest()||tafsirScroll==null)return false;
        TextView text=selectedTafsir==0?leftTafsir:rightTafsir;
        android.text.Layout layout=text.getLayout();
        if(layout==null||layout.getLineCount()<3)return false;
        android.graphics.Rect viewport=new android.graphics.Rect();
        if(!tafsirScroll.getGlobalVisibleRect(viewport)||viewport.isEmpty())return false;
        int[] position=new int[2];
        text.getLocationOnScreen(position);
        int commentaryLineY=position[1]+text.getTotalPaddingTop()+layout.getLineTop(2);
        return commentaryLineY<viewport.bottom-Ui.dp(this,14)
            &&commentaryLineY>=viewport.top-Ui.dp(this,18);
    }

    /** Landscape acceptance checks first real commentary line, never just
     * a visible Jalalayn edition label. */
    boolean tafsirFirstCommentaryLineVisibleForTest(){
        if(!tafsirTextVisibleForTest())return false;
        TextView text=selectedTafsir==0?leftTafsir:rightTafsir;
        android.text.Layout layout=text.getLayout();
        if(layout==null||layout.getLineCount()<1)return false;
        android.graphics.Rect viewport=new android.graphics.Rect();
        if(!tafsirScroll.getGlobalVisibleRect(viewport)||viewport.isEmpty())return false;
        int[] screen=new int[2];
        text.getLocationOnScreen(screen);
        int center=screen[1]+text.getTotalPaddingTop()+
            (layout.getLineTop(0)+layout.getLineBottom(0))/2;
        return center>viewport.top+Ui.dp(this,2)
            &&center<viewport.bottom-Ui.dp(this,10);
    }

    boolean tafsirPairLockedForTest(){
        AtlasDiscriminantCases.Entry pair=current();
        return pair.first.equals(selectedLeftVerse) && pair.second.equals(selectedRightVerse)
            && pair.first.toString().equals(firstTafsirTab.getText().toString())
            && pair.second.toString().equals(secondTafsirTab.getText().toString());
    }
    boolean lateralTafsirForTest(){return lateralTafsir;}
    boolean booxPortraitTafsirForTest(){return booxPortraitTafsir;}
    boolean tafsirBelowBothPagesForTest(){
        if(!booxPortraitTafsir||selectedTafsir<0||tafsirPanel.getVisibility()!=View.VISIBLE)
            return false;
        int[] lower=new int[2],upper=new int[2];
        tafsirPanel.getLocationOnScreen(lower);
        pagePair.getLocationOnScreen(upper);
        return lower[1]>=upper[1]+pagePair.getHeight()
            &&pagePair.getWidth()>Ui.dp(this,600);
    }
    boolean tafsirHeadingVisibleForTest(){
        android.graphics.Rect visible=new android.graphics.Rect();
        return tafsirHeading!=null&&tafsirHeading.getGlobalVisibleRect(visible)
            &&visible.height()==tafsirHeading.getHeight();
    }
    boolean bothQcfPagesFullyVisibleForTest(){
        // Android WebView can return false from getGlobalVisibleRect() during
        // landscape compositing even when its *returned rectangle* is complete.
        // Require real visibility, exact measured bounds and containment in the
        // visible parent ScrollView. Never accept clipped or hidden QCF pages.
        android.graphics.Rect viewport=new android.graphics.Rect();
        if(comparisonScroll==null||!comparisonScroll.isShown())return false;
        comparisonScroll.getGlobalVisibleRect(viewport);
        if(viewport.isEmpty())return false;
        for(MapQcfThumbnails page:new MapQcfThumbnails[]{left,right}){
            if(page==null||!page.isShown()||page.getAlpha()<=0f)return false;
            android.graphics.Rect visible=new android.graphics.Rect();
            page.getGlobalVisibleRect(visible);
            if(visible.isEmpty()||visible.width()!=page.getWidth()
                    ||visible.height()!=page.getHeight()||!viewport.contains(visible))
                return false;
        }
        return true;
    }
    MapQcfThumbnails[] qcfViewsForTest(){return new MapQcfThumbnails[]{left,right};}

    int getCurrentLeftPageForTest() { return currentLeftPage; }

    @Override public void onConfigurationChanged(android.content.res.Configuration config){
        super.onConfigurationChanged(config);
        if(disposed||pagePair==null)return;
        if(config.orientation==builtOrientation){
            comparisonScroll.post(this::fitVisiblePages);
            return;
        }
        // Atlas handles orientation|screenSize itself in the manifest, so the
        // Activity is NOT automatically recreated on rotation. Rebuild the actual
        // comparison workspace to replace a stale portrait vertical Tafsir pane.
        // Preserve the selected case, both exact Quran references and selected side.
        // Detached Chromium WebViews must never survive into the new orientation.
        detachQcf(left);
        detachQcf(right);
        detachQcf(strips);
        detachQcf(secondaryStrips);
        secondaryStrips=null;
        ++leftTafsirRequest;
        ++rightTafsirRequest;
        build();
        render();
    }

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
            detachQcf(secondaryStrips);
            leftTafsirRequest++;
            rightTafsirRequest++;
            tafsirIo.shutdownNow();
        }
        super.onDestroy();
    }

}
