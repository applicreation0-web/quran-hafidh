package com.quransafeguard.hifz.domain

import com.quransafeguard.hifz.core.LineGeometry
import com.quransafeguard.hifz.core.SabqiBlock
import com.quransafeguard.hifz.core.SabqiPlanner

class SabqiEngine(geometry: LineGeometry) {
    private val planner = SabqiPlanner(geometry)

    fun plan(start: MushafPosition): SabqiBlock = planner.plan(start)
}
