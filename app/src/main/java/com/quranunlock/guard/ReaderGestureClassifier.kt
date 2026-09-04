package com.applicreation0.quransafeguard

import kotlin.math.abs

enum class ReaderSwipe {
    PREVIOUS,
    NEXT
}

class ReaderGestureClassifier(
    private val swipeThresholdPx: Float
) {
    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var cancelled = false

    fun onDown(x: Float, y: Float, pointerCount: Int) {
        downX = x
        downY = y
        tracking = pointerCount == 1
        cancelled = pointerCount != 1
    }

    fun onMove(x: Float, y: Float, pointerCount: Int) {
        if (pointerCount != 1) {
            cancelled = true
            return
        }
        if (!tracking) return
        val deltaX = abs(x - downX)
        val deltaY = abs(y - downY)
        if (deltaY > swipeThresholdPx && deltaY > deltaX) {
            cancelled = true
        }
    }

    fun onAdditionalPointer() {
        cancelled = true
    }

    fun onCancel() {
        tracking = false
        cancelled = true
    }

    fun onUp(x: Float, y: Float, gesturesEnabled: Boolean): ReaderSwipe? {
        if (!tracking || cancelled || !gesturesEnabled) {
            tracking = false
            return null
        }
        tracking = false
        val deltaX = x - downX
        val deltaY = y - downY
        if (abs(deltaX) < swipeThresholdPx || abs(deltaX) <= abs(deltaY) * 1.25f) {
            return null
        }
        // Arabic-book convention: moving the page to the right advances the Mushaf;
        // moving it to the left returns to the previous page.
        return if (deltaX > 0f) ReaderSwipe.NEXT else ReaderSwipe.PREVIOUS
    }
}