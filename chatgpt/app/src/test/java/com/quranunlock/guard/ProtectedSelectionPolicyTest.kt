package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtectedSelectionPolicyTest {
    private val installed = setOf("social.a", "social.b", "browser.c")
    private val today = 20_000L

    @Test
    fun addition_is_immediate() {
        val result = ProtectedSelectionPolicy.update(
            state = ProtectedSelectionState(
                active = setOf("social.a"),
                pendingRemoval = emptySet(),
                removalEffectiveEpochDay = null
            ),
            requested = setOf("social.a", "browser.c"),
            installedTargets = installed,
            todayEpochDay = today
        )

        assertEquals(setOf("social.a", "browser.c"), result.active)
        assertEquals(emptySet<String>(), result.pendingRemoval)
        assertNull(result.removalEffectiveEpochDay)
    }

    @Test
    fun removal_stays_active_until_next_day() {
        val result = ProtectedSelectionPolicy.update(
            state = ProtectedSelectionState(
                active = setOf("social.a", "social.b"),
                pendingRemoval = emptySet(),
                removalEffectiveEpochDay = null
            ),
            requested = setOf("social.b"),
            installedTargets = installed,
            todayEpochDay = today
        )

        assertEquals(setOf("social.a", "social.b"), result.active)
        assertEquals(setOf("social.a"), result.pendingRemoval)
        assertEquals(today + 1L, result.removalEffectiveEpochDay)
    }

    @Test
    fun reselecting_cancels_pending_removal() {
        val result = ProtectedSelectionPolicy.update(
            state = ProtectedSelectionState(
                active = setOf("social.a", "social.b"),
                pendingRemoval = setOf("social.a"),
                removalEffectiveEpochDay = today + 1L
            ),
            requested = setOf("social.a", "social.b"),
            installedTargets = installed,
            todayEpochDay = today
        )

        assertEquals(setOf("social.a", "social.b"), result.active)
        assertEquals(emptySet<String>(), result.pendingRemoval)
        assertNull(result.removalEffectiveEpochDay)
    }

    @Test
    fun pending_removal_is_applied_on_next_day() {
        val result = ProtectedSelectionPolicy.reconcile(
            active = setOf("social.a", "social.b"),
            pendingRemoval = setOf("social.a"),
            removalEffectiveEpochDay = today + 1L,
            installedTargets = installed,
            todayEpochDay = today + 1L
        )

        assertEquals(setOf("social.b"), result.active)
        assertEquals(emptySet<String>(), result.pendingRemoval)
        assertNull(result.removalEffectiveEpochDay)
    }

    @Test
    fun uninstall_is_immediate_and_clears_pending_entry() {
        val result = ProtectedSelectionPolicy.reconcile(
            active = setOf("social.a", "social.b"),
            pendingRemoval = setOf("social.a"),
            removalEffectiveEpochDay = today + 1L,
            installedTargets = setOf("social.b"),
            todayEpochDay = today
        )

        assertEquals(setOf("social.b"), result.active)
        assertEquals(emptySet<String>(), result.pendingRemoval)
        assertNull(result.removalEffectiveEpochDay)
    }

    @Test
    fun missing_legacy_effective_day_is_repaired_to_tomorrow() {
        val result = ProtectedSelectionPolicy.reconcile(
            active = setOf("social.a"),
            pendingRemoval = setOf("social.a"),
            removalEffectiveEpochDay = null,
            installedTargets = installed,
            todayEpochDay = today
        )

        assertEquals(setOf("social.a"), result.active)
        assertEquals(setOf("social.a"), result.pendingRemoval)
        assertEquals(today + 1L, result.removalEffectiveEpochDay)
    }
}
