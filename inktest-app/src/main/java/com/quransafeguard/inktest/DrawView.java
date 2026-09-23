package com.quransafeguard.inktest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;
import java.util.List;

/** Minimal touch-stroke capture surface, recording (x, y, t) exactly as ML Kit's Ink expects. */
final class DrawView extends View {
    private final List<List<float[]>> strokes = new ArrayList<>();
    private final List<Path> visiblePaths = new ArrayList<>();
    private final Paint paint = new Paint();
    private long strokeStartMs = -1;
    private List<float[]> currentStroke;
    private Path currentVisiblePath;

    DrawView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    void clear() {
        strokes.clear();
        visiblePaths.clear();
        strokeStartMs = -1;
        invalidate();
    }

    boolean isEmpty() {
        return strokes.isEmpty();
    }

    Ink buildInk() {
        Ink.Builder inkBuilder = Ink.builder();
        for (List<float[]> stroke : strokes) {
            Ink.Stroke.Builder strokeBuilder = Ink.Stroke.builder();
            for (float[] p : stroke) {
                strokeBuilder.addPoint(Ink.Point.create(p[0], p[1], (long) p[2]));
            }
            inkBuilder.addStroke(strokeBuilder.build());
        }
        return inkBuilder.build();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (strokeStartMs < 0) strokeStartMs = SystemClock.elapsedRealtime();
                currentStroke = new ArrayList<>();
                currentVisiblePath = new Path();
                currentVisiblePath.moveTo(x, y);
                addPoint(x, y);
                strokes.add(currentStroke);
                visiblePaths.add(currentVisiblePath);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    currentVisiblePath.lineTo(x, y);
                    addPoint(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentStroke = null;
                currentVisiblePath = null;
                break;
        }
        invalidate();
        return true;
    }

    private void addPoint(float x, float y) {
        long t = SystemClock.elapsedRealtime() - strokeStartMs;
        currentStroke.add(new float[]{x, y, t});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path p : visiblePaths) {
            canvas.drawPath(p, paint);
        }
    }
}
