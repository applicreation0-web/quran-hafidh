package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class HifzV6ConstantsTest {
    @Test public void schemaAndUncalibratedMaintenanceDefaultAreFrozenFor075() {
        assertEquals(6, PreviewConfig.SCHEMA_VERSION);
        assertEquals(8.0, PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING, 0.0);
    }

    @Test public void individualProtocolsRemainBitForBitAtApprovedTotals() {
        assertEquals(37, PreviewConfig.SABQI_TOTAL_REPS);
        assertEquals(35, PreviewConfig.ITQAN_LIGHT_TOTAL_REPS);
        assertEquals(40, PreviewConfig.ITQAN_TOTAL_REPS);
        assertEquals(15, PreviewConfig.SABQI_VISIBLE_REPS);
        assertEquals(5, PreviewConfig.SABQI_25_REPS);
        assertEquals(5, PreviewConfig.SABQI_50_REPS);
        assertEquals(5, PreviewConfig.SABQI_75_REPS);
        assertEquals(7, PreviewConfig.SABQI_100_REPS);
    }
}
