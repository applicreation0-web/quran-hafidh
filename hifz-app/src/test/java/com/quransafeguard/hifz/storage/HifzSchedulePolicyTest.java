package com.quransafeguard.hifz.storage;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class HifzSchedulePolicyTest {
    @Test public void mondayWednesdayFridayHaveMorningAndEveningSabqiSlots() {
        assertEquals(Arrays.asList(HifzScheduleStore.SABQI, HifzScheduleStore.SABQI_REVIEW),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 14))); // Monday
        assertEquals(Arrays.asList(HifzScheduleStore.SABQI, HifzScheduleStore.SABQI_REVIEW),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 16))); // Wednesday
        assertEquals(Arrays.asList(HifzScheduleStore.SABQI, HifzScheduleStore.SABQI_REVIEW),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 18))); // Friday
    }

    @Test public void itqanAndMurajaahRemainSingleSlots() {
        assertEquals(Collections.singletonList(HifzScheduleStore.ITQAN),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 15))); // Tuesday
        assertEquals(Collections.singletonList(HifzScheduleStore.ITQAN),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 17))); // Thursday
        assertEquals(Collections.singletonList(HifzScheduleStore.MURAJAAH),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 19))); // Saturday
        assertEquals(Collections.singletonList(HifzScheduleStore.MURAJAAH),
            HifzScheduleStore.slotsFor(LocalDate.of(2026, 9, 20))); // Sunday
    }
}
