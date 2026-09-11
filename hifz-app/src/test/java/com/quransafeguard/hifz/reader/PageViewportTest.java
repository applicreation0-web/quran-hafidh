package com.quransafeguard.hifz.reader;

import android.graphics.RectF;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PageViewportTest {
    private static final float EPS = 0.001f;

    @Test
    public void portraitSourceIsCenteredWithoutDistortion() {
        RectF out = PageViewport.fitCenter(1000f, 1500f, 1200f, 1600f);
        assertEquals(1066.6666f, out.width(), EPS);
        assertEquals(1600f, out.height(), EPS);
        assertEquals(66.6666f, out.left, EPS);
        assertEquals(0f, out.top, EPS);
        assertEquals(1000f / 1500f, out.width() / out.height(), EPS);
    }

    @Test
    public void tallDestinationCentersVertically() {
        RectF out = PageViewport.fitCenter(1000f, 1500f, 1000f, 2000f);
        assertEquals(1000f, out.width(), EPS);
        assertEquals(1500f, out.height(), EPS);
        assertEquals(0f, out.left, EPS);
        assertEquals(250f, out.top, EPS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidDimensions() {
        PageViewport.fitCenter(0f, 1500f, 1000f, 2000f);
    }
}
