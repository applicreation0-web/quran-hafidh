package com.quransafeguard.hifz.preview;

import org.junit.After;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public final class HifzClockTest {
    @After public void reset() { HifzClock.resetClockForTests(); }

    @Test public void fixedClockMakesDateDependentFlowsDeterministicAcrossMidnight() {
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-09-13T23:30:00Z"), ZoneId.of("Europe/London")));
        assertEquals(LocalDate.of(2026, 9, 14), HifzClock.today());
    }
}
