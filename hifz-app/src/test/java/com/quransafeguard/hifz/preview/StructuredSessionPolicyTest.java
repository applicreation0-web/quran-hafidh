package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Runtime regressions found during physical post-signing session tests. */
public final class StructuredSessionPolicyTest {
    @Test public void atMostTwoTotalRevealsMayReceiveNormalCredit() {
        assertTrue(StructuredSessionPolicy.assistancePasses(0));
        assertTrue(StructuredSessionPolicy.assistancePasses(1));
        assertTrue(StructuredSessionPolicy.assistancePasses(2));
        assertFalse(StructuredSessionPolicy.assistancePasses(3));
        assertFalse(StructuredSessionPolicy.assistancePasses(7));
    }

    @Test public void maintenanceValidationDependsOnSelectedRealEndNotFortyFiveMinuteGate() {
        assertFalse(StructuredSessionPolicy.murajaahCanValidate(false));
        assertTrue(StructuredSessionPolicy.murajaahCanValidate(true));
    }

    @Test public void completedSessionNeverFallsBackToDefaultMushafPageOnReaderReady() {
        assertTrue(StructuredSessionPolicy.shouldInitialReaderShow(false, false));
        assertFalse(StructuredSessionPolicy.shouldInitialReaderShow(true, false));
        assertFalse(StructuredSessionPolicy.shouldInitialReaderShow(false, true));
        assertFalse(StructuredSessionPolicy.shouldInitialReaderShow(true, true));
    }
}
