package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.SessionKind;
import org.junit.Test;

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
}
