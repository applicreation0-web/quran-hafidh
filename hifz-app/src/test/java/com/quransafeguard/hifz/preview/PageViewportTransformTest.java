package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * WritingCanvasView has no Robolectric to exercise directly (this project has none — a real
 * android.view.View subclass can't be unit-tested), so the actual "fit and center" page-space to
 * screen-pixel math it depends on lives in this plain class instead, exercised here directly.
 */
public final class PageViewportTransformTest {
    // A real page 1 viewBox: [-53.3109, -198.4777, 345.0, 550.0].
    private static final float VP_X = -53.3109f;
    private static final float VP_Y = -198.4777f;
    private static final float VP_W = 345.0f;
    private static final float VP_H = 550.0f;

    @Test public void widerViewLetterboxesOnTheSides() {
        // View much wider than the viewport's aspect ratio: height is the limiting dimension.
        float scale = PageViewportTransform.scale(2000f, 1000f, VP_W, VP_H);
        assertEquals(1000f / VP_H, scale, 1e-4f);
        float offX = PageViewportTransform.offsetX(2000f, VP_W, scale);
        float offY = PageViewportTransform.offsetY(1000f, VP_H, scale);
        assertEquals(0f, offY, 1e-3f);
        assertEquals((2000f - VP_W * scale) / 2f, offX, 1e-3f);
        assertTrue(offX > 0f);
    }

    @Test public void tallerViewLetterboxesTopAndBottom() {
        // View much taller than the viewport's aspect ratio: width is the limiting dimension.
        float scale = PageViewportTransform.scale(500f, 3000f, VP_W, VP_H);
        assertEquals(500f / VP_W, scale, 1e-4f);
        float offX = PageViewportTransform.offsetX(500f, VP_W, scale);
        float offY = PageViewportTransform.offsetY(3000f, VP_H, scale);
        assertEquals(0f, offX, 1e-3f);
        assertTrue(offY > 0f);
    }

    @Test public void pageToScreenAndBackRoundTripsForAnyPointInsideTheViewport() {
        float scale = PageViewportTransform.scale(1080f, 1920f, VP_W, VP_H);
        float offX = PageViewportTransform.offsetX(1080f, VP_W, scale);
        float offY = PageViewportTransform.offsetY(1920f, VP_H, scale);

        for (float[] pagePoint : new float[][]{
            {VP_X, VP_Y}, {VP_X + VP_W, VP_Y + VP_H}, {VP_X + VP_W / 2f, VP_Y + VP_H / 2f}, {66.48f, 34.46f}
        }) {
            float sx = PageViewportTransform.toScreenX(pagePoint[0], VP_X, offX, scale);
            float sy = PageViewportTransform.toScreenY(pagePoint[1], VP_Y, offY, scale);
            float backX = PageViewportTransform.toPageX(sx, VP_X, offX, scale);
            float backY = PageViewportTransform.toPageY(sy, VP_Y, offY, scale);
            assertEquals(pagePoint[0], backX, 1e-2f);
            assertEquals(pagePoint[1], backY, 1e-2f);
        }
    }

    @Test public void theTopLeftPageCornerMapsToTheLetterboxOrigin() {
        float scale = PageViewportTransform.scale(2000f, 1000f, VP_W, VP_H);
        float offX = PageViewportTransform.offsetX(2000f, VP_W, scale);
        float offY = PageViewportTransform.offsetY(1000f, VP_H, scale);
        float sx = PageViewportTransform.toScreenX(VP_X, VP_X, offX, scale);
        float sy = PageViewportTransform.toScreenY(VP_Y, VP_Y, offY, scale);
        assertEquals(offX, sx, 1e-3f);
        assertEquals(offY, sy, 1e-3f);
    }

    @Test public void degenerateInputsFallBackToIdentityScaleRatherThanDividingByZero() {
        assertEquals(1f, PageViewportTransform.scale(0f, 100f, VP_W, VP_H), 1e-6f);
        assertEquals(1f, PageViewportTransform.scale(100f, 100f, 0f, VP_H), 1e-6f);
    }
}
