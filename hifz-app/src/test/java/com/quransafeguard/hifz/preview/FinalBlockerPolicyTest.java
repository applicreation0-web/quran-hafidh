package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.SessionKind;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Final blocker regressions before Quran Hifz 0.7.5 closeout. */
public final class FinalBlockerPolicyTest {
    @Test public void consolidationIsNeverReusableJ10Host() {
        assertFalse(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.RECENT_SABQI_REVIEW));
        assertFalse(J10ReviewPlanner.isReusableJ10Kind(SessionKind.RECENT_SABQI_REVIEW));
        assertTrue(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.SABQI_TODAY_REVIEW));
        assertTrue(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.ITQAN));
        assertTrue(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.MURAJAAH));
    }

    @Test public void j10ForecastCapacityUsesCanonicalRuntimeCadenceOnly() {
        LocalDate monday = LocalDate.of(2026, 9, 14);
        assertArrayEquals(
            "J10 capacity must match runtime cadence: learning, stabilization, revision; Consolidation has no fixed slot",
            new int[]{30, 60, 30, 60, 30, 45, 45},
            J10ReviewPlanner.scheduledCapacityByDayMinutes(monday, 7, 0, false));
        assertArrayEquals(
            "progression-triggered Consolidation must not change weekly J10 capacity",
            new int[]{30, 60, 30, 60, 30, 45, 45},
            J10ReviewPlanner.scheduledCapacityByDayMinutes(monday, 7, 99, true));
    }
}
