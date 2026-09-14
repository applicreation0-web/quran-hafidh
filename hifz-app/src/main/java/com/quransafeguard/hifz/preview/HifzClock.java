package com.quransafeguard.hifz.preview;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/** Single wall-clock boundary for every date-sensitive Hifz flow. */
final class HifzClock {
    private static volatile Clock clock = Clock.systemDefaultZone();

    private HifzClock() {}

    static LocalDate today() {
        return LocalDate.now(clock);
    }

    static void setClockForTests(Clock value) {
        clock = Objects.requireNonNull(value, "clock");
    }

    static void resetClockForTests() {
        clock = Clock.systemDefaultZone();
    }
}
