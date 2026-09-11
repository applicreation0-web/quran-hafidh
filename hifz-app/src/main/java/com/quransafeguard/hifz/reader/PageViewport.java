package com.quransafeguard.hifz.reader;

import android.graphics.RectF;

/** Pure geometry helper: fit a source rectangle inside a destination while preserving aspect ratio. */
public final class PageViewport {
    private PageViewport() {}

    public static RectF fitCenter(float sourceWidth, float sourceHeight, float destinationWidth, float destinationHeight) {
        if (sourceWidth <= 0f || sourceHeight <= 0f || destinationWidth <= 0f || destinationHeight <= 0f) {
            throw new IllegalArgumentException("All dimensions must be > 0");
        }
        float scale = Math.min(destinationWidth / sourceWidth, destinationHeight / sourceHeight);
        float width = sourceWidth * scale;
        float height = sourceHeight * scale;
        float left = (destinationWidth - width) * 0.5f;
        float top = (destinationHeight - height) * 0.5f;
        return new RectF(left, top, left + width, top + height);
    }
}
