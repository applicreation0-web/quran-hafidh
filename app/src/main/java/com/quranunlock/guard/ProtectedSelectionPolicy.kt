package com.applicreation0.quransafeguard

data class ProtectedSelectionState(
    val active: Set<String>,
    val pendingRemoval: Set<String>,
    val removalEffectiveEpochDay: Long?
)

object ProtectedSelectionPolicy {
    fun reconcile(
        active: Set<String>,
        pendingRemoval: Set<String>,
        removalEffectiveEpochDay: Long?,
        installedTargets: Set<String>,
        todayEpochDay: Long
    ): ProtectedSelectionState {
        val installed = installedTargets.toSet()
        val cleanActive = active.intersect(installed)
        val cleanPending = pendingRemoval.intersect(cleanActive)

        if (cleanPending.isNotEmpty() &&
            removalEffectiveEpochDay != null &&
            todayEpochDay >= removalEffectiveEpochDay
        ) {
            return ProtectedSelectionState(
                active = cleanActive - cleanPending,
                pendingRemoval = emptySet(),
                removalEffectiveEpochDay = null
            )
        }

        return ProtectedSelectionState(
            active = cleanActive,
            pendingRemoval = cleanPending,
            removalEffectiveEpochDay = if (cleanPending.isEmpty()) {
                null
            } else {
                removalEffectiveEpochDay
                    ?.takeIf { it > todayEpochDay }
                    ?: (todayEpochDay + 1L)
            }
        )
    }

    fun update(
        state: ProtectedSelectionState,
        requested: Set<String>,
        installedTargets: Set<String>,
        todayEpochDay: Long
    ): ProtectedSelectionState {
        val current = reconcile(
            active = state.active,
            pendingRemoval = state.pendingRemoval,
            removalEffectiveEpochDay = state.removalEffectiveEpochDay,
            installedTargets = installedTargets,
            todayEpochDay = todayEpochDay
        )
        val desired = requested.intersect(installedTargets)
        val updatedActive = current.active + desired
        val updatedPending = current.pendingRemoval.toMutableSet().apply {
            removeAll(desired)
            addAll(current.active - desired)
            retainAll(updatedActive)
        }

        return ProtectedSelectionState(
            active = updatedActive,
            pendingRemoval = updatedPending,
            removalEffectiveEpochDay = if (updatedPending.isEmpty()) {
                null
            } else {
                current.removalEffectiveEpochDay ?: (todayEpochDay + 1L)
            }
        )
    }
}
