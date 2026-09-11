package com.quransafeguard.hifz.domain

import com.quransafeguard.hifz.core.HifzSchedule
import com.quransafeguard.hifz.core.LineGeometry
import java.time.LocalDate

/** Single domain entry point. UI must not reimplement Sabqi/Itqan/Murajaah rules. */
class HifzEngine(
    geometry: LineGeometry,
    val sabqi: SabqiEngine = SabqiEngine(geometry),
    val itqan: ItqanEngine = ItqanEngine(),
    val murajaah: MurajaahEngine = MurajaahEngine()
) {
    fun scheduledSession(date: LocalDate, programStartDate: LocalDate): SessionKind? =
        HifzSchedule.scheduledKind(date, programStartDate)
}
