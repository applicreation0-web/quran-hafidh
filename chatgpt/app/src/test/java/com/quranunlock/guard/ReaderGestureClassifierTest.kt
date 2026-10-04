package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderGestureClassifierTest {
    private val threshold = 50f

    @Test
    fun swipeRightAdvancesArabicBook() {
        val classifier = ReaderGestureClassifier(threshold)
        classifier.onDown(x = 100f, y = 100f, pointerCount = 1)

        assertEquals(
            ReaderSwipe.NEXT,
            classifier.onUp(x = 180f, y = 102f, gesturesEnabled = true)
        )
    }

    @Test
    fun swipeLeftReturnsToPreviousPage() {
        val classifier = ReaderGestureClassifier(threshold)
        classifier.onDown(x = 180f, y = 100f, pointerCount = 1)

        assertEquals(
            ReaderSwipe.PREVIOUS,
            classifier.onUp(x = 100f, y = 102f, gesturesEnabled = true)
        )
    }

    @Test
    fun verticalScrollIsNotMisclassifiedAsPageTurn() {
        val classifier = ReaderGestureClassifier(threshold)
        classifier.onDown(x = 100f, y = 100f, pointerCount = 1)
        classifier.onMove(x = 105f, y = 180f, pointerCount = 1)

        assertNull(classifier.onUp(x = 105f, y = 200f, gesturesEnabled = true))
    }

    @Test
    fun disabledGesturesDoNotTurnPageWhileTafsirIsOpen() {
        val classifier = ReaderGestureClassifier(threshold)
        classifier.onDown(x = 100f, y = 100f, pointerCount = 1)

        assertNull(classifier.onUp(x = 180f, y = 100f, gesturesEnabled = false))
    }
}