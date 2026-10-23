package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Atlas-inspired book flow: one gently curved ink line, never a boxed card.
 * Static rendering, no movement or transparency (BOOX E-Ink safe).
 */
final class BooxFlowStroke extends View {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    BooxFlowStroke(Context context) {
        super(context);
        ink.setColor(Ui.LINE);
        ink.setStrokeWidth(Math.max(1f, context.getResources().getDisplayMetrics().density));
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setMinimumHeight(Ui.dp(context, 22));
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Ui.dp(getContext(), 22));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        if (w <= 0f) return;
        float h = getHeight();
        path.reset();
        path.moveTo(0f, h * .42f);
        path.cubicTo(w * .21f, h * .42f, w * .30f, h * .80f, w * .50f, h * .60f);
        path.cubicTo(w * .71f, h * .42f, w * .79f, h * .42f, w, h * .42f);
        canvas.drawPath(path, ink);
    }
}
