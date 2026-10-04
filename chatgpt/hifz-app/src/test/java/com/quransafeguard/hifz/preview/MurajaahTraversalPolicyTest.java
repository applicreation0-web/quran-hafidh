package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MurajaahTraversalPolicyTest {
    private static VerseRef v(int ayah) { return new VerseRef(2, ayah); }

    @Test public void uniqueEndpointCanCalibrateNormally() {
        assertFalse(MurajaahTraversalPolicy.endpointAmbiguous(
            Arrays.asList(v(1), v(2), v(3)), v(3)));
    }

    @Test public void repeatedEndpointAcrossCyclesIsAmbiguous() {
        assertTrue(MurajaahTraversalPolicy.endpointAmbiguous(
            Arrays.asList(v(1), v(2), v(1), v(2)), v(2)));
    }
}
