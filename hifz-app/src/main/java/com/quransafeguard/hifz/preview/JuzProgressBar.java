package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Static, high-contrast monochrome fill. No Android theme tint or E-Ink animation. */
final class JuzProgressBar extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int percent;
    JuzProgressBar(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void setPercent(int value){
        if(value<0||value>100)throw new IllegalArgumentException("Invalid Juz percentage");
        percent=value;invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.PAPER);
        canvas.drawRect(0,0,w,h,paint);
        paint.setColor(Ui.INK);
        float filled=w*percent/100f;
        if(filled>0)canvas.drawRect(0,0,filled,h,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f,Ui.dp(getContext(),1)));
        paint.setColor(Ui.LINE);
        canvas.drawRect(0,0,w,h,paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
