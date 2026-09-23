package com.quransafeguard.writingtest;

/**
 * Pure "stretch to fill" math for mapping a Mushaf page-space viewport (a geometry.json-style
 * [x, y, width, height] rectangle) onto an arbitrary on-screen pixel rectangle.
 *
 * Deliberately NOT aspect-ratio-preserving: a real Mushaf line is very wide and short (width:height
 * routinely 10:1+), so fitting it into a comfortably tall writing canvas while preserving that
 * exact ratio leaves most of the canvas as unusable dead space above/below a thin writable strip
 * (confirmed confusing on a real device — a tester couldn't tell where to write). Stretching X and
 * Y independently to fill the whole canvas makes every pixel of it a real writing surface. This is
 * self-consistent for scoring: the same per-axis scale is used both to draw (page->screen) and to
 * convert a trace back (screen->page), so an anisotropic stretch cancels out exactly.
 *
 * Kept free of any android.* import so it can be exercised by a plain JVM unit test — this
 * project has no Robolectric, so a real android.view.View subclass can't be unit-tested directly.
 */
final class PageViewportTransform {
    private PageViewportTransform() {}

    static float scaleX(float viewWidth, float vpWidth) {
        if (viewWidth <= 0f || vpWidth <= 0f) return 1f;
        return viewWidth / vpWidth;
    }

    static float scaleY(float viewHeight, float vpHeight) {
        if (viewHeight <= 0f || vpHeight <= 0f) return 1f;
        return viewHeight / vpHeight;
    }

    static float toScreenX(float pageX, float vpX, float scaleX) {
        return (pageX - vpX) * scaleX;
    }

    static float toScreenY(float pageY, float vpY, float scaleY) {
        return (pageY - vpY) * scaleY;
    }

    static float toPageX(float screenX, float vpX, float scaleX) {
        return vpX + screenX / scaleX;
    }

    static float toPageY(float screenY, float vpY, float scaleY) {
        return vpY + screenY / scaleY;
    }
}
