package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay stacked directly above the Mushaf WebView in Lecture, letting the learner
 * draw free-form pen annotations (a margin note, an underline) with a Boox stylus — exactly like
 * marking a physical Mushaf, never verified or recognized. Only MotionEvent.TOOL_TYPE_STYLUS is
 * handled here; every other input (a finger tap or swipe) is left unconsumed on ACTION_DOWN so the
 * parent ViewGroup falls through to the MushafView beneath for its own normal navigation/verse-tap
 * handling, completely undisturbed.
 *
 * Strokes are persisted (see AnnotationStore) as points normalized to this view's own width/height
 * at capture time, then denormalized against the view's *current* width/height on every redraw —
 * so a stroke keeps its same relative position even if this view is ever remeasured (a rotation, a
 * different device) between sessions.
 */
final class AnnotationOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AnnotationStore store;
    private int page = -1;
    private final List<float[]> strokes = new ArrayList<>();
    private final List<Float> activeStroke = new ArrayList<>();
    private boolean drawingEnabled = true;

    AnnotationOverlayView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1.6f * context.getResources().getDisplayMetrics().density);
    }

    void setStore(AnnotationStore store) {
        this.store = store;
    }

    /** Loads and redraws the given physical page's (1..604) saved strokes. Safe to call repeatedly
     *  for the same page (a no-op reload), matching every other page-change hook in this reader. */
    void setPage(int page) {
        this.page = page;
        activeStroke.clear();
        strokes.clear();
        if (store != null) strokes.addAll(store.strokesForPage(page));
        invalidate();
    }

    void setDrawingEnabled(boolean enabled) {
        drawingEnabled = enabled;
        if (!enabled) activeStroke.clear();
        invalidate();
    }

    void clearCurrentPage() {
        if (store == null || page < 0) return;
        strokes.clear();
        activeStroke.clear();
        store.clearPage(page);
        invalidate();
    }

    void undoLastStroke() {
        if (store == null || page < 0 || strokes.isEmpty()) return;
        strokes.remove(strokes.size() - 1);
        store.undoLastStroke(page);
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!drawingEnabled || page < 0 || event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) return false;
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeStroke.clear();
                activeStroke.add(event.getX() / w);
                activeStroke.add(event.getY() / h);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeStroke.isEmpty()) return false;
                activeStroke.add(event.getX() / w);
                activeStroke.add(event.getY() / h);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (activeStroke.size() >= 4 && store != null) {
                    float[] flat = new float[activeStroke.size()];
                    for (int i = 0; i < flat.length; i++) flat[i] = activeStroke.get(i);
                    strokes.add(flat);
                    store.addStroke(page, flat);
                }
                activeStroke.clear();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                activeStroke.clear();
                invalidate();
                return true;
            default:
                return false;
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        for (float[] stroke : strokes) drawStroke(canvas, stroke, w, h);
        if (activeStroke.size() >= 4) {
            float[] flat = new float[activeStroke.size()];
            for (int i = 0; i < flat.length; i++) flat[i] = activeStroke.get(i);
            drawStroke(canvas, flat, w, h);
        }
    }

    private void drawStroke(Canvas canvas, float[] normalized, float w, float h) {
        if (normalized.length < 4) return;
        Path path = new Path();
        path.moveTo(normalized[0] * w, normalized[1] * h);
        for (int i = 2; i + 1 < normalized.length; i += 2) {
            path.lineTo(normalized[i] * w, normalized[i + 1] * h);
        }
        canvas.drawPath(path, paint);
    }
}
