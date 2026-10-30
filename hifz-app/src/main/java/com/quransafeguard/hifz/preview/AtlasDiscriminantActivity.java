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
import android.widget.TextView;
import com.quransafeguard.hifz.core.VerseRef;

/**
 * Atlas-native, read-only, monochrome E-Ink comparison of two Quran passages.
 * The two full pages are shipped original KFQC SVGs; the lower close-ups crop those
 * same SVG paths, with verified physical word underlines. The notes are authored,
 * sourced editorial content, never an AI-generated Quran explanation at runtime.
 *
 * Deliberately separate from Révision active (no HifzPrefs or mutable session state).
 */
public final class AtlasDiscriminantActivity extends Activity {
    static final String EXTRA_SURAH="discriminantSurah";
    static final String EXTRA_AYAH="discriminantAyah";
    static final String EXTRA_ORIGIN="discriminantOrigin";

    private int selectedIndex;
    private GeometryRepository geometry;
    private MapQcfThumbnails left, right;
    private QcfAtlasVerseStrips strips;
    private TextView title, leftHeader, rightHeader, kind, leftCue, rightCue;
    private TextView leftRead, rightRead, returnToSource;
    private boolean disposed;
    private int currentLeftPage, currentRightPage;
    private int leftPageStatus, rightPageStatus; // 0 pending, +1 decoded, -1 failed

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            geometry=GeometryRepository.get(this);
            selectedIndex=state!=null
                ? state.getInt("caseIndex",0)
                : AtlasDiscriminantCases.preferredIndex(
                    getIntent().getIntExtra(EXTRA_SURAH,0),
                    getIntent().getIntExtra(EXTRA_AYAH,0));
            selectedIndex=Math.max(0,Math.min(AtlasDiscriminantCases.PILOT.size()-1,selectedIndex));
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
        int h=getResources().getDisplayMetrics().heightPixels;
        int dpHeight=(int)(h/getResources().getDisplayMetrics().density);
        boolean compact=dpHeight<740;
        boolean veryCompact=dpHeight<565;
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
        root.addView(top,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));

        LinearLayout heads=Ui.row(this);heads.setGravity(Gravity.CENTER_VERTICAL);
        leftHeader=text("",compact?12:13.5f,true);
        rightHeader=text("",compact?12:13.5f,true);
        leftHeader.setGravity(Gravity.CENTER);rightHeader.setGravity(Gravity.CENTER);
        heads.addView(leftHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        heads.addView(rightHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        root.addView(heads);

        LinearLayout pair=Ui.row(this);
        left=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
            @Override public void onPageChosen(int page){openReader(true,page);}
            @Override public void onOpenReading(int page){openReader(true,page);}
            @Override public void onRendered(int page,boolean success){
                if(page!=currentLeftPage)return;
                leftPageStatus=success?1:-1;
                android.util.Log.i("HaafidhAtlasQCF","Left SVG "+page+" rendered="+success);
                showRightAfterLeft();
            }
        });
        right=new MapQcfThumbnails(this,new MapQcfThumbnails.Listener(){
            @Override public void onPageChosen(int page){openReader(false,page);}
            @Override public void onOpenReading(int page){openReader(false,page);}
            @Override public void onRendered(int page,boolean success){
                if(page!=currentRightPage)return;
                rightPageStatus=success?1:-1;
                android.util.Log.i("HaafidhAtlasQCF","Right SVG "+page+" rendered="+success);
                if(leftPageStatus==1 && rightPageStatus==1 && strips!=null){
                    AtlasDiscriminantCases.Entry entry=current();
                    strips.show(entry.first,currentLeftPage,entry.firstWordIndices,
                        entry.second,currentRightPage,entry.secondWordIndices);
                }
            }
        });
        left.setAtlasSinglePageCompact(true);
        right.setAtlasSinglePageCompact(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);
        lp.setMargins(0,0,Ui.dp(this,3),0);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1f);
        rp.setMargins(Ui.dp(this,3),0,0,0);
        pair.addView(left,lp);pair.addView(right,rp);
        // Both genuine 345:550 KFQC pages remain intact and tappable.
        root.addView(pair,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout readingLinks=Ui.row(this);
        leftRead=readingLink(true);
        rightRead=readingLink(false);
        readingLinks.addView(leftRead,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        readingLinks.addView(rightRead,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        root.addView(readingLinks);

        if(!veryCompact){
            strips=new QcfAtlasVerseStrips(this);
            // Fixed-height QCF excerpts: the old weight=1 stretched this region
            // into hundreds of pixels of blank space on phones.
            root.addView(strips,new LinearLayout.LayoutParams(-1,
                Ui.dp(this,compact?106:126)));
        }

        kind=text("",compact?13:15,true);
        kind.setGravity(Gravity.CENTER);
        kind.setMaxLines(2);
        root.addView(kind,new LinearLayout.LayoutParams(-1,Ui.dp(this,compact?40:46)));

        LinearLayout cues=Ui.row(this);
        leftCue=text("",compact?12.5f:14f,false);
        rightCue=text("",compact?12.5f:14f,false);
        leftCue.setPadding(Ui.dp(this,5),0,Ui.dp(this,5),0);
        rightCue.setPadding(Ui.dp(this,5),0,Ui.dp(this,5),0);
        leftCue.setGravity(Gravity.CENTER);
        rightCue.setGravity(Gravity.CENTER);
        leftCue.setMaxLines(compact?2:3);rightCue.setMaxLines(compact?2:3);
        leftCue.setEllipsize(TextUtils.TruncateAt.END);
        rightCue.setEllipsize(TextUtils.TruncateAt.END);
        cues.addView(leftCue,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?54:76),1f));
        View line=new View(this);line.setBackgroundColor(Ui.LINE);
        cues.addView(line,new LinearLayout.LayoutParams(Math.max(1,Ui.dp(this,1)),-1));
        cues.addView(rightCue,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?54:76),1f));
        root.addView(cues);

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

        // Only the original QCF full pages take flexible height; Arabic excerpts
        // are no longer stretched on tall phone/BOOX screens.
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob)->{
            if(pair.getWidth()<Ui.dp(this,120)||root.getHeight()<Ui.dp(this,400))return;
            int fixed=root.getPaddingTop()+root.getPaddingBottom();
            for(int i=0;i<root.getChildCount();i++){
                View child=root.getChildAt(i);
                if(child!=pair)fixed+=child.getMeasuredHeight();
            }
            int available=root.getHeight()-fixed;
            int natural=Math.round(Math.max(0,(pair.getWidth()-Ui.dp(this,8))/2f)*550f/345f);
            int desired=Math.min(available,natural+Ui.dp(this,4));
            if(desired>Ui.dp(this,110)){
                LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)pair.getLayoutParams();
                if(params.height!=desired||params.weight!=0f){
                    params.height=desired;params.weight=0f;pair.setLayoutParams(params);
                }
            }
        });
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
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
        AtlasDiscriminantCases.Entry c=current();
        if(c.first.equals(c.second))throw new IllegalStateException("Self-comparison forbidden");
        currentLeftPage=geometry.pageForVerse(c.first);
        currentRightPage=geometry.pageForVerse(c.second);
        if(currentLeftPage<1||currentLeftPage>604||currentRightPage<1||currentRightPage>604)
            throw new IllegalStateException("Invalid QCF page");

        leftHeader.setText(QuranSurahNames.name(c.first.getSurah())+" · "+c.first+" · p."+currentLeftPage);
        rightHeader.setText(QuranSurahNames.name(c.second.getSurah())+" · "+c.second+" · p."+currentRightPage);
        // BOOX: never decode two 600 KB original SVG pages and two crops simultaneously.
        // Load the second page only after the first confirms a real image.onload;
        // otherwise one failed image could hide a missing QCF resource indefinitely.
        leftPageStatus=0;rightPageStatus=0;
        right.setVisibility(View.INVISIBLE);
        if(strips!=null)strips.setVisibility(View.INVISIBLE);
        // Highlights the exact original SVG ayah polygon.
        left.showPages(currentLeftPage,currentLeftPage,currentLeftPage,
            c.first.getSurah(),c.first.getAyah(),c.first.getAyah(),false);

        title.setText("Comparer · "+c.first+" ↔ "+c.second);
        kind.setText(c.kind);
        leftRead.setText("Lire "+c.first+" en Lecture");
        rightRead.setText("Lire "+c.second+" en Lecture");
        String origin=getIntent().getStringExtra(EXTRA_ORIGIN);
        String label="Révision active".equals(origin)?"Révision active":
            "Révision passive".equals(origin)?"Révision passive":"Lecture";
        returnToSource.setText("← Retour à "+label);
        leftCue.setText(c.firstCue);
        rightCue.setText(c.secondCue);
    }

    private void showRightAfterLeft(){
        if(rightPageStatus!=0)return;
        AtlasDiscriminantCases.Entry c=current();
        right.setVisibility(View.VISIBLE);
        right.showPages(currentRightPage,currentRightPage,currentRightPage,
            c.second.getSurah(),c.second.getAyah(),c.second.getAyah(),false);
        if(strips!=null)strips.setVisibility(View.VISIBLE);
    }

    android.graphics.Rect[] qcfCropBoundsForTest(){
        if(strips==null)return new android.graphics.Rect[0];
        android.graphics.Rect whole=new android.graphics.Rect();
        if(!strips.getGlobalVisibleRect(whole)||whole.width()<100)return new android.graphics.Rect[0];
        int midpoint=whole.left+whole.width()/2;
        return new android.graphics.Rect[]{
            new android.graphics.Rect(whole.left,whole.top,midpoint,whole.bottom),
            new android.graphics.Rect(midpoint,whole.top,whole.right,whole.bottom)
        };
    }

    android.graphics.Rect[] qcfPageBoundsForTest(){
        android.graphics.Rect first=new android.graphics.Rect(),second=new android.graphics.Rect();
        if(left!=null)left.getGlobalVisibleRect(first);
        if(right!=null)right.getGlobalVisibleRect(second);
        return new android.graphics.Rect[]{first,second};
    }
    boolean qcfPagesReadyForTest(){return leftPageStatus==1&&rightPageStatus==1;}
    boolean qcfStripsReadyForTest(){return strips==null || strips.bothRenderedForTest();}
    int qcfPagesFailedForTest(){return (leftPageStatus==-1?1:0)+(rightPageStatus==-1?1:0)
        +(strips==null?0:strips.failureCountForTest());}
    String qcfFailureDetailForTest(){
        return "leftPage="+currentLeftPage+" status="+leftPageStatus+" cause="+left.rendererDiagnosticForTest()
            +" rightPage="+currentRightPage+" status="+rightPageStatus
            +" strips="+(strips==null?"disabled":strips.renderStatusForTest());
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

    int getCurrentLeftPageForTest() { return currentLeftPage; }

    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("caseIndex",selectedIndex);
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
        }
        super.onDestroy();
    }

}
