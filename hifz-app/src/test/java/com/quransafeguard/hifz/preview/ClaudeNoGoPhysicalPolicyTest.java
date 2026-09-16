package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/** Behavioral regression for a ready unit followed by a partially Stabilized unit. */
public final class ClaudeNoGoPhysicalPolicyTest {
    @Test public void partiallyStabilizedLaterUnitIsSkippedWithoutLosingEarlierReadyUnit() {
        StabilizationHalfPagePolicy.Unit first = new StabilizationHalfPagePolicy.Unit(
            48, 2, Arrays.asList("48:0", "48:1"));
        StabilizationHalfPagePolicy.Unit second = new StabilizationHalfPagePolicy.Unit(
            48, 2, Arrays.asList("48:2", "48:3"));
        List<StabilizationHalfPagePolicy.Unit> planned = Arrays.asList(first, second);
        Set<String> stabilized = new LinkedHashSet<>(Arrays.asList("48:0", "48:1", "48:2"));

        List<StabilizationHalfPagePolicy.Unit> ready = ConsolidationPhysicalUnitPolicy.readyUnits(
            planned, stabilized, Collections.emptySet(), 3);

        assertEquals(1, ready.size());
        assertEquals(Arrays.asList("48:0", "48:1"), ready.get(0).lineIds);
    }
}
