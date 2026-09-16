package com.quransafeguard.hifz.haafidh

data class HizbSelection(
    val selectedHizb: List<Int>
)

object HizbRevision {
    fun quota(week: CycleWeek): Int = week.hizbQuota

    fun next(selection: HizbSelection, startIndex: Int, quota: Int): List<Int> {
        if (selection.selectedHizb.isEmpty()) return emptyList()
        return (0 until quota).map { offset ->
            selection.selectedHizb[(startIndex + offset) % selection.selectedHizb.size]
        }
    }
}
