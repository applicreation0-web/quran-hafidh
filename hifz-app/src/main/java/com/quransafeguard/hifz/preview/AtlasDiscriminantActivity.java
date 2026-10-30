package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
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

    private int selectedIndex;
    private GeometryRepository geometry;
    private MapQcfThumbnails left, right;
    private QcfAtlasVerseStrips strips;
    private TextView title, position, leftHeader, rightHeader, kind, leftCue, rightCue;
    private RelationRail rail;
    private boolean disposed;
    private Button prev,next;
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
        title=text("Versets ressemblants",compact?15:17,true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        prev=Ui.iconButton(this,"","Passage précédent du corpus",v->move(-1));
        next=Ui.iconButton(this,"","Passage suivant du corpus",v->move(1));
        top.addView(prev);top.addView(next);
        root.addView(top,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));

        rail=new RelationRail();
        root.addView(rail,new LinearLayout.LayoutParams(-1,Ui.dp(this,compact?66:85)));

        LinearLayout heads=Ui.row(this);heads.setGravity(Gravity.CENTER_VERTICAL);
        leftHeader=text("",compact?11.5f:13f,true);
        rightHeader=text("",compact?11.5f:13f,true);
        leftHeader.setGravity(Gravity.CENTER);rightHeader.setGravity(Gravity.CENTER);
        heads.addView(leftHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        heads.addView(rightHeader,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?35:40),1));
        root.addView(heads);

        LinearLayout pair=Ui.row(this);
        pair.setPadding(0,0,0,0);
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
        // The genuine 15-line QCF page gets all remaining vertical space, never a stretched font.
        root.addView(pair,new LinearLayout.LayoutParams(-1,0,1f));

        if(!veryCompact){
            strips=new QcfAtlasVerseStrips(this);
            root.addView(strips,new LinearLayout.LayoutParams(-1,
                Ui.dp(this,compact?94:125)));
        }

        LinearLayout rel=Ui.row(this);rel.setGravity(Gravity.CENTER_VERTICAL);
        kind=text("",compact?12:13.5f,true);
        kind.setGravity(Gravity.CENTER);
        kind.setMaxLines(2);
        rel.addView(kind,new LinearLayout.LayoutParams(-1,Ui.dp(this,compact?44:48)));
        root.addView(rel);

        LinearLayout cues=Ui.row(this);
        leftCue=text("",compact?11.5f:13f,false);
        rightCue=text("",compact?11.5f:13f,false);
        leftCue.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);
        rightCue.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),0);
        leftCue.setGravity(Gravity.CENTER);
        rightCue.setGravity(Gravity.CENTER);
        leftCue.setMaxLines(compact?2:3);rightCue.setMaxLines(compact?2:3);
        leftCue.setEllipsize(TextUtils.TruncateAt.END);
        rightCue.setEllipsize(TextUtils.TruncateAt.END);
        cues.addView(leftCue,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?52:75),1f));
        View line=new View(this);line.setBackgroundColor(Ui.LINE);
        cues.addView(line,new LinearLayout.LayoutParams(Ui.dp(this,1),-1));
        cues.addView(rightCue,new LinearLayout.LayoutParams(0,Ui.dp(this,compact?52:75),1f));
        root.addView(cues);

        LinearLayout bottom=Ui.row(this);bottom.setGravity(Gravity.CENTER_VERTICAL);
        Button explanation=Ui.smallButton(this,"Explication",v->showExplanation());
        explanation.setContentDescription("Indice détaillé et référence bibliographique");
        bottom.addView(explanation,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        position=text("",12f,false);
        position.setGravity(Gravity.CENTER);
        bottom.addView(position,new LinearLayout.LayoutParams(0,Ui.dp(this,48),0.6f));
        Button open=Ui.smallButton(this,"Lecture",v->openReader(true,currentLeftPage));
        open.setContentDescription("Ouvrir la source dans le vrai Mushaf");
        bottom.addView(open,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1f));
        root.addView(bottom,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        setContentView(root);
        Ui.respectSystemBars(this,root,0,0,0,0);
    }

    private void move(int d){
        int nextIndex=selectedIndex+d;
        if(nextIndex<0||nextIndex>=AtlasDiscriminantCases.PILOT.size())return;
        selectedIndex=nextIndex;render();
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
        // Highlight the ACTUAL ayahPolygon of the Quran, not approximated rectangles.
        left.showPages(currentLeftPage,currentLeftPage,currentLeftPage,
            c.first.getSurah(),c.first.getAyah(),c.first.getAyah(),false);

        kind.setText("Différence · "+c.kind);
        leftCue.setText(c.firstCue);
        rightCue.setText(c.secondCue);
        position.setText((selectedIndex+1)+" / "+AtlasDiscriminantCases.PILOT.size());
        prev.setEnabled(selectedIndex>0);
        next.setEnabled(selectedIndex<AtlasDiscriminantCases.PILOT.size()-1);
        rail.invalidate();
    }

    private void showRightAfterLeft(){
        if(rightPageStatus!=0)return;
        AtlasDiscriminantCases.Entry c=current();
        right.setVisibility(View.VISIBLE);
        right.showPages(currentRightPage,currentRightPage,currentRightPage,
            c.second.getSurah(),c.second.getAyah(),c.second.getAyah(),false);
        if(strips!=null)strips.setVisibility(View.VISIBLE);
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
    @Override protected void onDestroy(){
        if(!disposed){
            disposed=true;
            if(left!=null)left.destroy();
            if(right!=null)right.destroy();
            if(strips!=null)strips.destroy();
        }
        super.onDestroy();
    }

    /** Original Atlas visual grammar: one fine rail, filled nodes and a double-ring active point. */
    private final class RelationRail extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        RelationRail(){super(AtlasDiscriminantActivity.this);}
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            AtlasDiscriminantCases.Entry e=current();
            float w=getWidth(),h=getHeight();
            float y=h*.55f,a=w*.25f,b=w*.75f;
            p.setColor(Ui.INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(getContext(),1));
            c.drawLine(a+Ui.dp(getContext(),8),y,b-Ui.dp(getContext(),8),y,p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(a,y,Ui.dp(getContext(),7),p);
            c.drawCircle(b,y,Ui.dp(getContext(),5),p);
            p.setStyle(Paint.Style.STROKE);
            c.drawCircle(a,y,Ui.dp(getContext(),13),p);
            c.drawCircle(a,y,Ui.dp(getContext(),17),p);
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Ui.dp(getContext(),12));
            c.drawText(e.first.toString(),a,Ui.dp(getContext(),17),p);
            c.drawText(e.second.toString(),b,Ui.dp(getContext(),17),p);
        }
    }
}
