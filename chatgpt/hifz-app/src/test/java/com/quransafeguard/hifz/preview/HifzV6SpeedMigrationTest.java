package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class HifzV6SpeedMigrationTest {
    @Test public void calibratedMaintenanceSpeedIsPreservedExactly() {
        assertEquals(7.43,
            HifzV6Migration.migratedMaintenanceSecondsPerLine(7.43, true),
            0.0);
        assertEquals(9.0,
            HifzV6Migration.migratedMaintenanceSecondsPerLine(9.0, true),
            0.0);
        assertEquals(12.875,
            HifzV6Migration.migratedMaintenanceSecondsPerLine(12.875, true),
            0.0);
    }

    @Test public void everyUncalibratedHistoricalValueMigratesToApprovedEightSecondDefault() {
        assertEquals(8.0,
            HifzV6Migration.migratedMaintenanceSecondsPerLine(9.0, false),
            0.0);
        assertEquals(8.0,
            HifzV6Migration.migratedMaintenanceSecondsPerLine(7.5, false),
            0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calibratedNonFiniteSpeedFailsClosed() {
        HifzV6Migration.migratedMaintenanceSecondsPerLine(Double.NaN, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calibratedNonPositiveSpeedFailsClosed() {
        HifzV6Migration.migratedMaintenanceSecondsPerLine(0.0, true);
    }
}
