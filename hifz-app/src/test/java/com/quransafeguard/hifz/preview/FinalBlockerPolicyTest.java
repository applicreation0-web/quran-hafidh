package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.SessionKind;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Final blocker regressions before Quran Hifz 0.7.5 closeout. */
public final class FinalBlockerPolicyTest {
    /**
     * Neither Sabqi nor Ancrage (individual protocol or their separate Renforcement/boule de
     * neige counters) are ever reusable J10 time: only genuine Entretien (Murajaah) is. ITQAN
     * historically named the anchoring morning slot before it became the demi-page Ancrage
     * queue, which is why it must be excluded here just like SABQI_TODAY_REVIEW.
     */
    @Test public void consolidationIsNeverReusableJ10Host() {
        assertFalse(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.RECENT_SABQI_REVIEW));
        assertFalse(J10ReviewPlanner.isReusableJ10Kind(SessionKind.RECENT_SABQI_REVIEW));
        assertFalse(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.SABQI_TODAY_REVIEW));
        assertFalse(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.ITQAN));
        assertFalse(J10ReviewPlanner.isReusableJ10Kind(SessionKind.ITQAN));
        assertTrue(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.MURAJAAH));
    }

    @Test public void j10ForecastCapacityUsesCanonicalRuntimeCadenceOnly() {
        LocalDate monday = LocalDate.of(2026, 9, 14);
        assertArrayEquals(
            "J10 capacity is Entretien-only: 0 on Sabqi/Ancrage days, 60 on Tue/Thu/Sat/Sun evenings",
            new int[]{0, 60, 0, 60, 0, 60, 60},
            J10ReviewPlanner.scheduledCapacityByDayMinutes(monday, 7, 0, false));
        assertArrayEquals(
            "progression-triggered Consolidation must not change weekly J10 capacity",
            new int[]{0, 60, 0, 60, 0, 60, 60},
            J10ReviewPlanner.scheduledCapacityByDayMinutes(monday, 7, 99, true));
    }
}
