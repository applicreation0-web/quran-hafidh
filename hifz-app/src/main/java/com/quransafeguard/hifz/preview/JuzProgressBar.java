package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** A Medina-page silhouette mapping each earned QCF line to its real vertical position.
 * Top = beginning of Juz; bottom = end of Juz, including gaps and partial Juz.
 * Solid = acquired; hatch = stabilized; dots = learned; blank = untouched.
 * No animation, tint, percentage labels or transient color on E-Ink.
 */
final class JuzProgressBar extends View {
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline=new Path();
    private final RectF body=new RectF();
    private int[] lineStatuses=new int[0];
    JuzProgressBar(Context c){
        super(c);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Strictly read-only visual projection; no credit or mutation of HifzPrefs. */
    void setLineStatuses(int[] qcfLineStatuses){
        if(qcfLineStatuses==null||qcfLineStatuses.length==0)
            throw new IllegalArgumentException("Missing physical Juz lines");
        int[] copy=java.util.Arrays.copyOf(qcfLineStatuses,qcfLineStatuses.length);
        for(int status:copy)
            if(status<ProgressGridView.VIDE||status>ProgressGridView.ACQUIS)
                throw new IllegalArgumentException("Invalid Juz line state");
        lineStatuses=copy;
        invalidate();
    }

    int totalLines(){return lineStatuses.length;}
    int filledLines(){
        int result=0;
        for(int status:lineStatuses)if(status!=ProgressGridView.VIDE)result++;
        return result;
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        int height=Math.round(width*550f/345f);
        setMeasuredDimension(width,resolveSize(height,heightSpec));
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        float stroke=Math.max(1f,Ui.dp(getContext(),1));
        float fold=Math.min(w*.17f,Ui.dp(getContext(),18));
        float inset=stroke*1.5f;
        outline.reset();
        outline.moveTo(inset,inset);
        outline.lineTo(w-fold-inset,inset);
        outline.lineTo(w-inset,fold+inset);
        outline.lineTo(w-inset,h-inset);
        outline.lineTo(inset,h-inset);
        outline.close();
        pen.setStyle(Paint.Style.FILL);
        pen.setColor(Ui.PAPER);
        canvas.drawPath(outline,pen);

        // Every QCF physical line retains its canonical vertical position.
        // A start-of-Juz lesson fills at TOP; an end-of-Juz lesson fills at BOTTOM.
        // No bottom-anchored sums, no fake contiguous progress across gaps.
        body.set(inset+stroke,inset+stroke,w-inset-stroke,h-inset-stroke);
        canvas.save();
        canvas.clipPath(outline);
        int total=lineStatuses.length;
        for(int start=0;start<total;){
            int state=lineStatuses[start];
            int end=start+1;
            while(end<total && lineStatuses[end]==state)end++;
            if(state!=ProgressGridView.VIDE){
                float top=body.top+body.height()*start/total;
                float bottom=body.top+body.height()*end/total;
                if(state==ProgressGridView.ACQUIS){
                    pen.setColor(Ui.INK);
                    pen.setStyle(Paint.Style.FILL);
                    canvas.drawRect(body.left,top,body.right,bottom,pen);
                }else if(state==ProgressGridView.STABILISER){
                    canvas.save();
                    canvas.clipRect(body.left,top,body.right,bottom);
                    pen.setColor(Ui.LINE);
                    pen.setStrokeWidth(Math.max(stroke,Ui.dp(getContext(),1)));
                    pen.setStyle(Paint.Style.STROKE);
                    float step=Math.max(Ui.dp(getContext(),5),w*.09f);
                    for(float x=body.left-body.height();x<body.right+body.height();x+=step)
                        canvas.drawLine(x,bottom,x+body.height(),top,pen);
                    canvas.restore();
                }else if(state==ProgressGridView.APPRENTISSAGE){
                    canvas.save();
                    canvas.clipRect(body.left,top,body.right,bottom);
                    pen.setStyle(Paint.Style.FILL);
                    pen.setColor(Ui.INK);
                    float step=Math.max(Ui.dp(getContext(),6),w*.12f);
                    float radius=Math.max(1f,Ui.dp(getContext(),1));
                    for(float cy=Math.min(top+step/2,(top+bottom)/2);cy<bottom;cy+=step)
                        for(float cx=body.left+step/2;cx<body.right;cx+=step)
                            canvas.drawCircle(cx,cy,radius,pen);
                    canvas.restore();
                }
            }
            start=end;
        }
        canvas.restore();
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(filledLines()==0 ? stroke*1.3f : stroke);
        pen.setColor(Ui.INK);
        canvas.drawPath(outline,pen);
        Path dogEar=new Path();
        dogEar.moveTo(w-fold-inset,inset);
        dogEar.lineTo(w-fold-inset,fold+inset);
        dogEar.lineTo(w-inset,fold+inset);
        canvas.drawPath(dogEar,pen);
        pen.setStyle(Paint.Style.FILL);
    }
}
