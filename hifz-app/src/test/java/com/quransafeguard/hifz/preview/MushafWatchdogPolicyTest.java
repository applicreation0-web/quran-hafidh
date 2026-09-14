package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MushafWatchdogPolicyTest {
    @Test public void timeoutUsesThreeTimesObservedRenderWithSafeFloorAndCap() {
        assertEquals(8_000L, MushafView.timeoutAfterObservedRender(500L));
        assertEquals(9_000L, MushafView.timeoutAfterObservedRender(3_000L));
        assertEquals(20_000L, MushafView.timeoutAfterObservedRender(7_000L));
        assertEquals(20_000L, MushafView.timeoutAfterObservedRender(12_000L));
        assertEquals(8_000L, MushafView.timeoutAfterObservedRender(0L));
        assertEquals(20_000L, MushafView.timeoutAfterObservedRender(Long.MAX_VALUE));
    }

    @Test public void isolatedSlowRenderDoesNotPoisonLaterNormalTimeouts() {
        long observed = MushafView.updateObservedRender(0L, 30_000L);
        assertEquals(20_000L, observed);
        assertEquals(20_000L, MushafView.timeoutAfterObservedRender(observed));
        observed = MushafView.updateObservedRender(observed, 3_000L);
        assertEquals(3_000L, observed);
        assertEquals(9_000L, MushafView.timeoutAfterObservedRender(observed));
    }
}
