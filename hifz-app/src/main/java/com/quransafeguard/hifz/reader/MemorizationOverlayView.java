package com.quransafeguard.hifz.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.quransafeguard.hifz.data.GeometryRepository;
import com.quransafeguard.hifz.data.LineGeometryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * White overlay for memorization. It never changes the underlying Mushaf SVG.
 * Progressive masking is deterministic and therefore stable on E-Ink.
 */
public final class MemorizationOverlayView extends View {
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private MushafRenderer renderer;
    private List<LineGeometryRepository.PageLine> selectedLines = Collections.emptyList();
    private GeometryRepository.AyahRegion focusedAyah;
    private int maskPercent;
    private boolean revealAll;

    public MemorizationOverlayView(Context context) { this(context, null); }
    public MemorizationOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setBackgroundColor(Color.TRANSPARENT);
        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(Color.WHITE);
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density));
        focusPaint.setColor(Color.DKGRAY);
    }

    public void bindRenderer(MushafRenderer renderer) { this.renderer = renderer; }

    public void setSelection(List<LineGeometryRepository.PageLine> lines, GeometryRepository.AyahRegion focusedAyah) {
        selectedLines = lines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(lines));
        this.focusedAyah = focusedAyah;
        invalidate();
    }

    public void setMaskPercent(int percent) {
        maskPercent = Math.max(0, Math.min(100, percent));
        revealAll = false;
        invalidate();
    }

    public int getMaskPercent() { return maskPercent; }

    /** Temporarily reveals the current selection without changing persisted mask level. */
    public void setRevealAll(boolean reveal) {
        revealAll = reveal;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        MushafRenderer r = renderer;
        if (r == null) return;

        if (!revealAll && maskPercent > 0) {
            float fraction = maskPercent / 100f;
            for (LineGeometryRepository.PageLine line : selectedLines) {
                if (line.page != r.getCurrentPage()) continue;
                RectF rect = r.mapDocumentToView(line.documentBounds);
                if (rect == null) continue;
                // Arabic is RTL: progressive masking starts at the visual right edge.
                float left = rect.right - rect.width() * fraction;
                canvas.drawRect(left, rect.top, rect.right, rect.bottom, maskPaint);
            }
        }

        if (focusedAyah != null && focusedAyah.page == r.getCurrentPage()) {
            RectF rect = r.mapDocumentToView(focusedAyah.getBounds());
            if (rect != null) canvas.drawRect(rect, focusPaint);
        }
    }
}
