package com.quransafeguard.writingtest;

/**
 * Pure "fit and center" math for mapping a Mushaf page-space viewport (a geometry.json-style
 * [x, y, width, height] rectangle) onto an arbitrary on-screen pixel rectangle — the same
 * behavior an SVG's default preserveAspectRatio="xMidYMid meet" gives the WebView-based Mushaf
 * reader for free, replicated by hand for WritingCanvasView since it is a plain Canvas view.
 *
 * Kept free of any android.* import so it can be exercised by a plain JVM unit test — this
 * project has no Robolectric, so a real android.view.View subclass can't be unit-tested directly.
 */
final class PageViewportTransform {
    private PageViewportTransform() {}

    static float scale(float viewWidth, float viewHeight, float vpWidth, float vpHeight) {
        if (viewWidth <= 0f || viewHeight <= 0f || vpWidth <= 0f || vpHeight <= 0f) return 1f;
        return Math.min(viewWidth / vpWidth, viewHeight / vpHeight);
    }

    static float offsetX(float viewWidth, float vpWidth, float scale) {
        return (viewWidth - vpWidth * scale) / 2f;
    }

    static float offsetY(float viewHeight, float vpHeight, float scale) {
        return (viewHeight - vpHeight * scale) / 2f;
    }

    static float toScreenX(float pageX, float vpX, float offsetX, float scale) {
        return offsetX + (pageX - vpX) * scale;
    }

    static float toScreenY(float pageY, float vpY, float offsetY, float scale) {
        return offsetY + (pageY - vpY) * scale;
    }

    static float toPageX(float screenX, float vpX, float offsetX, float scale) {
        return vpX + (screenX - offsetX) / scale;
    }

    static float toPageY(float screenY, float vpY, float offsetY, float scale) {
        return vpY + (screenY - offsetY) / scale;
    }
}
