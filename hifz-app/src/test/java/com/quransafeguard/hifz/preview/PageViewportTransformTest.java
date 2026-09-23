package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * WritingCanvasView has no Robolectric to exercise directly (this project has none — a real
 * android.view.View subclass can't be unit-tested), so the actual "stretch to fill" page-space
 * to screen-pixel math it depends on lives in this plain class instead, exercised here directly.
 */
public final class PageViewportTransformTest {
    // A real page 1 viewBox: [-53.3109, -198.4777, 345.0, 550.0].
    private static final float VP_X = -53.3109f;
    private static final float VP_Y = -198.4777f;
    private static final float VP_W = 345.0f;
    private static final float VP_H = 550.0f;

    @Test public void scaleXFillsTheFullViewWidthIndependentlyOfHeight() {
        float scaleX = PageViewportTransform.scaleX(2000f, VP_W);
        assertEquals(2000f / VP_W, scaleX, 1e-4f);
    }

    @Test public void scaleYFillsTheFullViewHeightIndependentlyOfWidth() {
        float scaleY = PageViewportTransform.scaleY(3000f, VP_H);
        assertEquals(3000f / VP_H, scaleY, 1e-4f);
    }

    @Test public void pageToScreenAndBackRoundTripsForAnyPointInsideTheViewportEvenWhenStretchedAnisotropically() {
        float scaleX = PageViewportTransform.scaleX(1080f, VP_W);
        float scaleY = PageViewportTransform.scaleY(1920f, VP_H);

        for (float[] pagePoint : new float[][]{
            {VP_X, VP_Y}, {VP_X + VP_W, VP_Y + VP_H}, {VP_X + VP_W / 2f, VP_Y + VP_H / 2f}, {66.48f, 34.46f}
        }) {
            float sx = PageViewportTransform.toScreenX(pagePoint[0], VP_X, scaleX);
            float sy = PageViewportTransform.toScreenY(pagePoint[1], VP_Y, scaleY);
            float backX = PageViewportTransform.toPageX(sx, VP_X, scaleX);
            float backY = PageViewportTransform.toPageY(sy, VP_Y, scaleY);
            assertEquals(pagePoint[0], backX, 1e-2f);
            assertEquals(pagePoint[1], backY, 1e-2f);
        }
    }

    @Test public void theTopLeftPageCornerMapsToTheScreenOrigin() {
        float scaleX = PageViewportTransform.scaleX(2000f, VP_W);
        float scaleY = PageViewportTransform.scaleY(1000f, VP_H);
        float sx = PageViewportTransform.toScreenX(VP_X, VP_X, scaleX);
        float sy = PageViewportTransform.toScreenY(VP_Y, VP_Y, scaleY);
        assertEquals(0f, sx, 1e-3f);
        assertEquals(0f, sy, 1e-3f);
    }

    @Test public void theBottomRightPageCornerMapsToTheFarScreenEdge() {
        float scaleX = PageViewportTransform.scaleX(2000f, VP_W);
        float scaleY = PageViewportTransform.scaleY(1000f, VP_H);
        float sx = PageViewportTransform.toScreenX(VP_X + VP_W, VP_X, scaleX);
        float sy = PageViewportTransform.toScreenY(VP_Y + VP_H, VP_Y, scaleY);
        assertEquals(2000f, sx, 1e-2f);
        assertEquals(1000f, sy, 1e-2f);
    }

    @Test public void degenerateInputsFallBackToIdentityScaleRatherThanDividingByZero() {
        assertEquals(1f, PageViewportTransform.scaleX(0f, VP_W), 1e-6f);
        assertEquals(1f, PageViewportTransform.scaleX(100f, 0f), 1e-6f);
        assertEquals(1f, PageViewportTransform.scaleY(0f, VP_H), 1e-6f);
        assertEquals(1f, PageViewportTransform.scaleY(100f, 0f), 1e-6f);
    }
}
