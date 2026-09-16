package com.quransafeguard.hifz.haafidh

data class HizbSelection(
    val selectedHizb: List<Int>
)

object HizbRevision {
    fun quota(week: CycleWeek): Int = week.hizbQuota
}
