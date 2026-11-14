package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Graphical Cap Hifz timeline: chronological left-to-right and canonical
 * QCF physical lines bottom-to-top (Juz 1 -> 30).
 * Sabqi and the first Itqan tail are DISTINCT tracks; never join or reverse.
 * No chart point ever counts as a credit or changes an engine state.
 */
final class CapHifzTimelineView extends View {
    private static final int INK=Color.rgb(24,24,24);
    private static final int SECONDARY=Color.rgb(82,82,82);
    private static final int GRID=Color.rgb(165,165,165);
    private final GeometryRepository geometry;
    private final List<CapHifzForecast.Quarter> quarters;
    private final boolean showSabqi, showTail;
    private final IntConsumer selectYear;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

    CapHifzTimelineView(Context context, GeometryRepository geometry,
            List<CapHifzForecast.Quarter> quarters, boolean showSabqi,
            boolean showTail, IntConsumer selectYear) {
        super(context);
        if(geometry==null||quarters==null||quarters.isEmpty())
            throw new IllegalArgumentException("Real QCF and forecast required");
        CapHifzTimelineOrder.verify(quarters);
        this.geometry=geometry;
        this.quarters=quarters;
        this.showSabqi=showSabqi;
        this.showTail=showTail;
        this.selectYear=selectYear;
        setContentDescription("Simulation de trajectoire jusqu'aux 30 Juz. "
            +"Temps de gauche à droite, Juz 1 à 30 du bas vers le haut. "
            +"Trait continu Sabqi, pointillé Itqān premier parcours. "
            +"Touchez une année pour afficher les jalons.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setClickable(selectYear!=null);
        // Fixed, fine monotone strokes remain visible on grayscale E-Ink.
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }

    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private float sp(float value){return value*getResources().getDisplayMetrics().scaledDensity;}
    private void stroke(int color,float width){
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(width));paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);paint.setPathEffect(null);
    }
    private void drawText(Canvas canvas,String value,float x,float y,int size,int color,Paint.Align align){
        paint.setPathEffect(null);paint.setColor(color);paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(sp(size));paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        paint.setTextAlign(align);canvas.drawText(value,x,y,paint);
    }
    private float x(LocalDate date,float left,float width){
        long from=quarters.get(0).date.toEpochDay(),to=quarters.get(quarters.size()-1).date.toEpochDay();
        return to<=from?left:left+width*(date.toEpochDay()-from)/(float)(to-from);
    }
    private float y(VerseRef verse,float top,float height){
        int line=geometry.lastLineIndex(verse);
        return top+height*(1f-line/(float)(geometry.lineCount()-1));
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight(),left=dp(44),right=w-dp(10);
        float top=dp(22),bottom=h-dp(48);
        float height=bottom-top,width=right-left;
        if(width<=0||height<=0)return;

        drawText(canvas,"Juz",dp(3),dp(12),12,SECONDARY,Paint.Align.LEFT);
        int[] boundaries=JuzProgressSummary.boundaries();
        for(int juz:new int[]{1,10,20,30}){
            int first=JuzProgressSummary.firstOwnedLine(geometry,boundaries[juz-1]);
            float yy=top+height*(1f-first/(float)(geometry.lineCount()-1));
            yy=Math.max(top,Math.min(bottom,yy));
            stroke(GRID,.65f);canvas.drawLine(left,yy,right,yy,paint);
            drawText(canvas,Integer.toString(juz),left-dp(8),yy+dp(4),11,INK,Paint.Align.RIGHT);
        }
        int firstYear=quarters.get(0).date.getYear(),lastYear=quarters.get(quarters.size()-1).date.getYear();
        int count=Math.max(1,lastYear-firstYear+1);
        int every=count<=5?1:count<=10?2:count<=16?3:5;
        for(int year=firstYear;year<=lastYear;year++){
            if((year-firstYear)%every!=0 && year!=lastYear)continue;
            LocalDate date=year==firstYear?quarters.get(0).date:LocalDate.of(year,1,1);
            float xx=x(date,left,width);
            drawText(canvas,Integer.toString(year),xx,h-dp(13),11,SECONDARY,
                year==firstYear?Paint.Align.LEFT:year==lastYear?Paint.Align.RIGHT:Paint.Align.CENTER);
        }
        if(showSabqi)plot(canvas,left,width,top,height,true);
        if(showTail)plot(canvas,left,width,top,height,false);

        stroke(INK,1.5f);
        canvas.drawLine(left+dp(1),dp(8),left+dp(17),dp(8),paint);
        drawText(canvas,"Sabqi",left+dp(22),dp(12),12,INK,Paint.Align.LEFT);
        if(showTail){
            float lx=right-dp(92);
            stroke(SECONDARY,1.3f);
            paint.setPathEffect(new DashPathEffect(new float[]{dp(4),dp(3)},0));
            canvas.drawLine(lx,dp(8),lx+dp(17),dp(8),paint);
            paint.setPathEffect(null);
            drawText(canvas,"Itqān",lx+dp(22),dp(12),12,SECONDARY,Paint.Align.LEFT);
        }
    }

    private void plot(Canvas canvas,float left,float width,float top,float height,boolean sabqi){
        Path path=new Path();
        boolean hasPoint=false;
        float endX=0,endY=0;
        for(CapHifzForecast.Quarter quarter:quarters){
            VerseRef verse=sabqi?quarter.sabqi:quarter.itqan;
            // Show first An-Nas completion ONCE, never extend an Itqan line after it.
            if(!sabqi&&quarter.firstTailComplete)verse=ItqanRotationPolicy.TAIL_END;
            if(verse==null)continue;
            float xx=x(quarter.date,left,width),yy=y(verse,top,height);
            if(hasPoint)path.lineTo(xx,yy);
            else{path.moveTo(xx,yy);hasPoint=true;}
            endX=xx;endY=yy;
            if(!sabqi&&quarter.firstTailComplete)break;
            if(sabqi&&quarter.sabqiComplete)break;
        }
        if(!hasPoint)return;
        stroke(sabqi?INK:SECONDARY,sabqi?1.6f:1.3f);
        if(!sabqi)paint.setPathEffect(new DashPathEffect(new float[]{dp(4),dp(3)},0));
        canvas.drawPath(path,paint);paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(endX,endY,dp(2.8f),paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event){
        if(selectYear==null)return super.onTouchEvent(event);
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()==MotionEvent.ACTION_UP){
            float left=dp(44),width=Math.max(1,getWidth()-dp(54));
            float part=Math.max(0,Math.min(1,(event.getX()-left)/width));
            long from=quarters.get(0).date.toEpochDay(),to=quarters.get(quarters.size()-1).date.toEpochDay();
            LocalDate date=LocalDate.ofEpochDay(from+Math.round(part*(to-from)));
            selectYear.accept(date.getYear());
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
