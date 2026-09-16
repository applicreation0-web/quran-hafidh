package com.quransafeguard.hifz.haafidh

import kotlin.test.Test
import kotlin.test.assertEquals

class HaafidhIntegrationRulesTest {
    @Test
    fun cycleAndSnowballConstants() {
        assertEquals(5, HaafidhRules.SNOWBALL_REPETITIONS)
        assertEquals(10, HaafidhRules.SUNDAY_REPETITIONS)
        assertEquals(14L, HaafidhRules.REVIEW_DELAY_DAYS)
    }

    @Test
    fun hizbQuotaIsCycleBased() {
        assertEquals(3, HizbRevision.quota(CycleWeek.A))
        assertEquals(2, HizbRevision.quota(CycleWeek.B))
    }
}
