package com.quransafeguard.writingtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.quransafeguard.hifz.core.TrajectoryComparison;

import java.util.ArrayList;
import java.util.List;

/**
 * The real writing-exercise canvas: a blank sheet (write from memory, the letterform is never
 * shown) that keeps the real ayah-end markers visible at their exact printed positions, in the
 * same Mushaf page-space coordinates as geometry.json/WordShapeRepository/AyahMarkerRepository —
 * so scoring against the real reference geometry never needs a second coordinate system.
 *
 * Adapts inktest-app's proven DrawView stroke-capture pattern (three real on-device tests this
 * session): touch capture and ML Kit's Ink stay in raw screen-pixel space exactly like DrawView,
 * since ink recognition is shape-based and doesn't care which coordinate system it's given. Only
 * the geometric Palier 3 (trajectory) scoring needs page-space points, produced on demand by
 * inverting this view's own fit-to-viewport transform — the same "SVG viewBox meet" math the
 * WebView-based Mushaf reader gets for free from the browser, replicated by hand here since this
 * is a plain Canvas view, not a WebView.
 */
public final class WritingCanvasView extends View {
    private static final char[] ARABIC_DIGITS = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};

    private float vpX, vpY, vpW, vpH;
    private float[][] markersPageSpace = new float[0][];
    private int[] markerAyahNumbers = new int[0];

    private final List<List<float[]>> strokes = new ArrayList<>();
    private final List<Path> visiblePaths = new ArrayList<>();
    private final Paint inkPaint = new Paint();
    private final Paint markerPaint = new Paint();
    private final Paint markerTextPaint = new Paint();
    private final Paint guidePaint = new Paint();
    private long strokeStartMs = -1;
    private List<float[]> currentStroke;
    private Path currentVisiblePath;

    public WritingCanvasView(Context context) {
        super(context);
        setBackgroundColor(0xfffdfbf6);

        inkPaint.setColor(0xff121211);
        inkPaint.setStyle(Paint.Style.STROKE);
        inkPaint.setStrokeWidth(6f);
        inkPaint.setStrokeJoin(Paint.Join.ROUND);
        inkPaint.setStrokeCap(Paint.Cap.ROUND);
        inkPaint.setAntiAlias(true);

        // Drawn as a hollow ring with the real ayah number inside — the same "end of verse"
        // convention as the printed Mushaf's rosette, so a tester can see exactly which verse
        // ends where instead of a plain unlabeled dot.
        markerPaint.setColor(0xff8a6d3b);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeWidth(2.5f);
        markerPaint.setAntiAlias(true);

        markerTextPaint.setColor(0xff8a6d3b);
        markerTextPaint.setAntiAlias(true);
        markerTextPaint.setTextAlign(Paint.Align.CENTER);

        guidePaint.setColor(0xffcec9be);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(1f);
        guidePaint.setAntiAlias(true);
    }

    /**
     * Sets the page-space viewport this canvas displays (e.g. one physical Mushaf line's full
     * width band) and the ayah-end markers to draw within it, in that same page-space coordinate
     * system. {@code ayahNumbers} labels each marker (same index) with the real ayah number it
     * ends, or is null/shorter than {@code markers} when that isn't known — those markers are
     * still drawn, just unlabeled. Clears any in-progress writing.
     */
    public void configure(float pageSpaceX, float pageSpaceY, float pageSpaceWidth, float pageSpaceHeight, float[][] markers, int[] ayahNumbers) {
        vpX = pageSpaceX;
        vpY = pageSpaceY;
        vpW = Math.max(1e-3f, pageSpaceWidth);
        vpH = Math.max(1e-3f, pageSpaceHeight);
        markersPageSpace = markers == null ? new float[0][] : markers;
        markerAyahNumbers = ayahNumbers == null ? new int[0] : ayahNumbers;
        clear();
    }

    public void clear() {
        strokes.clear();
        visiblePaths.clear();
        strokeStartMs = -1;
        invalidate();
    }

    public boolean isEmpty() {
        return strokes.isEmpty();
    }

    /**
     * Raw screen-space strokes, one flat [x0,y0,t0,x1,y1,t1,...] array per pen-lift stroke.
     * This test app only exercises the geometric Palier 3 (trajectory) scoring, never ML Kit.
     */
    public List<float[]> rawStrokesFlatXYT() {
        List<float[]> result = new ArrayList<>(strokes.size());
        for (List<float[]> stroke : strokes) {
            float[] flat = new float[stroke.size() * 3];
            for (int i = 0; i < stroke.size(); i++) {
                float[] p = stroke.get(i);
                flat[i * 3] = p[0];
                flat[i * 3 + 1] = p[1];
                flat[i * 3 + 2] = p[2];
            }
            result.add(flat);
        }
        return result;
    }

    /**
     * The user's strokes converted into this view's page-space coordinate system (one list of
     * {x, y} points per pen-lift stroke), for real geometric Palier 3 scoring against the actual
     * letterform trajectory via TrajectoryComparison.scoreStrokes.
     */
    public List<List<TrajectoryComparison.Pt>> strokesInPageSpace() {
        List<List<TrajectoryComparison.Pt>> result = new ArrayList<>(strokes.size());
        float scaleX = fitScaleX();
        float scaleY = fitScaleY();
        for (List<float[]> stroke : strokes) {
            List<TrajectoryComparison.Pt> converted = new ArrayList<>(stroke.size());
            for (float[] p : stroke) {
                double pageX = PageViewportTransform.toPageX(p[0], vpX, scaleX);
                double pageY = PageViewportTransform.toPageY(p[1], vpY, scaleY);
                converted.add(new TrajectoryComparison.Pt(pageX, pageY));
            }
            result.add(converted);
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // This view can sit inside a ScrollView (this test app's own layout does); without
                // this, the ScrollView's touch interception steals any gesture with vertical motion
                // before a stroke can be drawn, making the canvas effectively unwritable.
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
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
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                currentStroke = null;
                currentVisiblePath = null;
                break;
            default:
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
        if (vpW <= 0f || vpH <= 0f) return;
        float scaleX = fitScaleX();
        float scaleY = fitScaleY();

        canvas.drawRect(0f, 0f, getWidth(), getHeight(), guidePaint);

        float density = getResources().getDisplayMetrics().density;
        float markerRadius = Math.max(9f, 12f * density);
        markerTextPaint.setTextSize(Math.max(10f, 13f * density));
        for (int i = 0; i < markersPageSpace.length; i++) {
            float[] marker = markersPageSpace[i];
            float sx = PageViewportTransform.toScreenX(marker[0], vpX, scaleX);
            float sy = PageViewportTransform.toScreenY(marker[1], vpY, scaleY);
            canvas.drawCircle(sx, sy, markerRadius, markerPaint);
            if (i < markerAyahNumbers.length && markerAyahNumbers[i] > 0) {
                float textY = sy - (markerTextPaint.descent() + markerTextPaint.ascent()) / 2f;
                canvas.drawText(arabicDigits(markerAyahNumbers[i]), sx, textY, markerTextPaint);
            }
        }

        for (Path p : visiblePaths) canvas.drawPath(p, inkPaint);
    }

    private float fitScaleX() {
        return PageViewportTransform.scaleX(getWidth(), vpW);
    }

    private float fitScaleY() {
        return PageViewportTransform.scaleY(getHeight(), vpH);
    }

    private static String arabicDigits(int n) {
        String digits = Integer.toString(n);
        StringBuilder result = new StringBuilder(digits.length());
        for (int i = 0; i < digits.length(); i++) result.append(ARABIC_DIGITS[digits.charAt(i) - '0']);
        return result.toString();
    }
}
