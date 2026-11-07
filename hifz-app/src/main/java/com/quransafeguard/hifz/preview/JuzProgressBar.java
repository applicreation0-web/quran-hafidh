package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** A compact Medina-page silhouette filled by real completed QCF lines.
 * Solid = acquired; hatch = stabilized; dots = learned; blank = untouched.
 * The page itself is the gauge: there is no separate horizontal progress bar.
 * No animation, tint, or transient color on E-Ink.
 */
final class JuzProgressBar extends View {
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline=new Path();
    private final RectF body=new RectF();
    private int total=1,learned,stabilized,acquired;
    JuzProgressBar(Context c){
        super(c);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setLevels(int learnedLines,int stabilizedLines,int acquiredLines,int totalLines){
        if(totalLines<=0||learnedLines<0||stabilizedLines<0||acquiredLines<0
            ||(long)learnedLines+stabilizedLines+acquiredLines>totalLines)
            throw new IllegalArgumentException("Invalid Juz physical line totals");
        total=totalLines;
        learned=learnedLines;
        stabilized=stabilizedLines;
        acquired=acquiredLines;
        invalidate();
    }

    int totalLines(){return total;}
    int filledLines(){return learned+stabilized+acquired;}

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

        // Fill the page from the bottom: real levels, normalized to every line of this Juz.
        body.set(inset+stroke,inset+stroke,w-inset-stroke,h-inset-stroke);
        canvas.save();
        canvas.clipPath(outline);
        float y=body.bottom;
        float scale=body.height()/total;
        float acquiredTop=y-acquired*scale;
        if(acquired>0){
            pen.setStyle(Paint.Style.FILL);
            pen.setColor(Ui.INK);
            canvas.drawRect(body.left,acquiredTop,body.right,y,pen);
        }
        y=acquiredTop;
        float stabilizedTop=y-stabilized*scale;
        if(stabilized>0){
            canvas.save();
            canvas.clipRect(body.left,stabilizedTop,body.right,y);
            pen.setColor(Ui.LINE);
            pen.setStrokeWidth(Math.max(stroke,Ui.dp(getContext(),1)));
            pen.setStyle(Paint.Style.STROKE);
            float step=Math.max(Ui.dp(getContext(),5),w*.09f);
            for(float x=body.left-body.height();x<body.right+body.height();x+=step)
                canvas.drawLine(x,y,x+body.height(),stabilizedTop,pen);
            canvas.restore();
        }
        y=stabilizedTop;
        float learnedTop=y-learned*scale;
        if(learned>0){
            canvas.save();
            canvas.clipRect(body.left,learnedTop,body.right,y);
            pen.setStyle(Paint.Style.FILL);
            pen.setColor(Ui.INK);
            float step=Math.max(Ui.dp(getContext(),6),w*.12f);
            float radius=Math.max(1f,Ui.dp(getContext(),1));
            for(float cy=learnedTop+step/2;cy<y;cy+=step)
                for(float cx=body.left+step/2;cx<body.right;cx+=step)
                    canvas.drawCircle(cx,cy,radius,pen);
            canvas.restore();
        }
        canvas.restore();
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(stroke);
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
