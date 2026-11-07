package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Refrain visual rail, using the same GRAPHIC vocabulary as Atlas:
 * horizontal axis, circular nodes and concentric selection rings.
 * English mnemonic labels derive ONLY from actual neighbouring Quran verses.
 * Never load, quote, or infer any Atlas titles to build a refrain label.
 * Read-only navigation: no Hifz progress, timing or preferences writes.
 */
final class RefrainTimeline {
    private RefrainTimeline() {}
    interface OnSelect { void selected(String verse); }

    static final class State {
        int selected=-1;
        boolean open;
        final Set<String> stations=new HashSet<>(),nodes=new HashSet<>();
        State(Bundle saved) {
            if(saved==null)return;
            selected=saved.getInt("friseSelected",-1);
            open=saved.getBoolean("friseOpen",false);
            ArrayList<String> s=saved.getStringArrayList("friseStations");
            ArrayList<String> n=saved.getStringArrayList("friseNodes");
            if(s!=null)stations.addAll(s);
            if(n!=null)nodes.addAll(n);
        }
        void save(Bundle out){
            out.putInt("friseSelected",selected);
            out.putBoolean("friseOpen",open);
            out.putStringArrayList("friseStations",new ArrayList<>(stations));
            out.putStringArrayList("friseNodes",new ArrayList<>(nodes));
        }
    }

    private static final class Marks extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int count,slot;
        private int current;
        Marks(Activity a,int count,int slot,int selected){
            super(a);this.count=count;this.slot=slot;current=selected;
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        void select(int i){current=i;invalidate();} // No E-Ink animation or full QCF refresh.
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float y=Ui.dp(getContext(),33),half=slot/2f;
            paint.setColor(Ui.INK);
            paint.setStrokeWidth(Math.max(1,Ui.dp(getContext(),1)));
            if(count>1){
                paint.setStyle(Paint.Style.STROKE);
                c.drawLine(half,y,(count-1)*slot+half,y,paint);
            }
            for(int i=0;i<count;i++){
                float x=i*slot+half;
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(x,y,Ui.dp(getContext(),i==current?8:4),paint);
                if(i==current){
                    paint.setStyle(Paint.Style.STROKE);
                    c.drawCircle(x,y,Ui.dp(getContext(),12),paint);
                    c.drawCircle(x,y,Ui.dp(getContext(),16),paint);
                }
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private static void reading(Activity a,String ref,String page){
        a.startActivity(new Intent(a,StudyReaderActivity.class)
            .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,Integer.parseInt(page))
            .putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE,ref));
    }

    /** Exact same 3 dossiers: 4 Al-Qamar, 31 Ar-Rahman, 10 Al-Mursalat. */
    static void append(Activity a,LinearLayout host,String id,String initialRef,State state,OnSelect onSelect){
        ArrayList<String[]> nodes=new ArrayList<>();
        for(String[] n:RefrainTimelineData.NODES)if(id.equals(n[0]))nodes.add(n);
        if(nodes.isEmpty())return;
        for(String[] n:nodes)if(n.length!=17||n[14].isEmpty()||n[15].isEmpty()||n[16].isEmpty())
            throw new IllegalStateException("Unverified Quran-context refrain station");

        final int visible=Math.min(6, nodes.size());
        final int screenDp=a.getResources().getConfiguration().screenWidthDp;
        final int slotDp=Math.max(40,(screenDp-104)/visible);
        final int slotPx=Ui.dp(a,slotDp);
        final int[] current={state.selected};
        if(current[0]<0||current[0]>=nodes.size()){
            current[0]=0;
            for(int i=0;i<nodes.size();i++)
                if(initialRef.equals(nodes.get(i)[5])){current[0]=i;break;}
        }
        state.selected=current[0];

        LinearLayout panel=Ui.column(a);
        panel.setPadding(Ui.dp(a,2),Ui.dp(a,2),Ui.dp(a,2),Ui.dp(a,2));
        LinearLayout line=Ui.row(a);
        line.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView scroll=new HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        FrameLayout rail=new FrameLayout(a);
        int totalWidth=slotPx*nodes.size();
        Marks marks=new Marks(a,nodes.size(),slotPx,current[0]);
        rail.addView(marks,new FrameLayout.LayoutParams(totalWidth,Ui.dp(a,76)));
        LinearLayout stations=Ui.row(a);
        ArrayList<TextView> labels=new ArrayList<>();
        for(int i=0;i<nodes.size();i++){
            String[] n=nodes.get(i);
            LinearLayout card=Ui.column(a);
            // Ui.column is padded/opaque by default: keep the Atlas circles and line visible.
            card.setPadding(0,0,0,0);
            card.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView number=Ui.text(a,n[3],11,false);
            number.setGravity(Gravity.CENTER);
            card.addView(number,new LinearLayout.LayoutParams(-1,Ui.dp(a,17)));
            View dotSpacer=new View(a);
            card.addView(dotSpacer,new LinearLayout.LayoutParams(-1,Ui.dp(a,32)));
            TextView title=Ui.text(a,n[15],screenDp<540?10:12,false);
            title.setTextColor(Ui.INK);
            title.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            title.setMaxLines(2);
            card.addView(title,new LinearLayout.LayoutParams(-1,Ui.dp(a,27)));
            labels.add(title);
            card.setContentDescription("Refrain "+n[3]+" sur "+n[4]+
                ", "+n[5]+" : "+n[15]);
            stations.addView(card,new LinearLayout.LayoutParams(slotPx,Ui.dp(a,76)));
        }
        rail.addView(stations,new FrameLayout.LayoutParams(totalWidth,Ui.dp(a,76)));
        scroll.addView(rail,new HorizontalScrollView.LayoutParams(totalWidth,Ui.dp(a,76)));

        TextView detail=Ui.text(a,"",screenDp<540?11:12,false);
        detail.setTextColor(Ui.INK);
        detail.setGravity(Gravity.CENTER);
        detail.setMaxLines(2);
        detail.setMinHeight(Ui.dp(a,42));
        detail.setOnClickListener(v->{
            String[] n=nodes.get(current[0]);reading(a,n[5],n[6]);
        });

        Runnable[] redraw=new Runnable[1];
        redraw[0]=()->{
            state.selected=current[0];marks.select(current[0]);
            String[] n=nodes.get(current[0]);
            detail.setText(n[14]+" · "+n[5]+"\n"+
                n[8]+" "+n[15]+"  →  "+n[11]+" "+n[16]);
            detail.setContentDescription("Lire "+n[5]+" dans le Mushaf. Before "+
                n[8]+" "+n[15]+". After "+n[11]+" "+n[16]);
            for(int i=0;i<labels.size();i++)
                labels.get(i).setTypeface(null,i==current[0]?
                    android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
            int left=Math.max(0,(current[0]-visible+1)*slotPx);
            int right=(current[0]+1)*slotPx;
            int x=scroll.getScrollX();
            int viewport=scroll.getWidth()>0?scroll.getWidth():visible*slotPx;
            if(x>left)scroll.scrollTo(left,0);
            else if(x+viewport<right)scroll.scrollTo(Math.max(0,right-viewport),0);
        };
        // Both taps and arrows share the same selection, never alter Hifz.
        for(int i=0;i<stations.getChildCount();i++){
            final int index=i;
            View card=stations.getChildAt(i);
            card.setOnClickListener(v->{
                current[0]=index;redraw[0].run();
                if(onSelect!=null)onSelect.selected(nodes.get(index)[5]);
            });
        }
        Button previous=Ui.iconButton(a,"‹","Occurrence précédente",v->{
            current[0]=(current[0]+nodes.size()-1)%nodes.size();redraw[0].run();
            if(onSelect!=null)onSelect.selected(nodes.get(current[0])[5]);
        });
        Button next=Ui.iconButton(a,"›","Occurrence suivante",v->{
            current[0]=(current[0]+1)%nodes.size();redraw[0].run();
            if(onSelect!=null)onSelect.selected(nodes.get(current[0])[5]);
        });
        line.addView(previous,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,76)));
        line.addView(scroll,new LinearLayout.LayoutParams(0,Ui.dp(a,76),1f));
        line.addView(next,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,76)));
        panel.addView(line,new LinearLayout.LayoutParams(-1,Ui.dp(a,76)));
        panel.addView(detail,new LinearLayout.LayoutParams(-1,Ui.dp(a,42)));
        host.addView(panel,new LinearLayout.LayoutParams(-1,Ui.dp(a,122)));
        scroll.post(redraw[0]);
    }
}
