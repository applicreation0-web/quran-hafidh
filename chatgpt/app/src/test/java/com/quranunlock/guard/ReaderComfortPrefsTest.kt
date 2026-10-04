package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderComfortPrefsTest {
    @Test
    fun everyLegacyVisualModeNowUsesTheSameCreamBackground() {
        assertEquals("#F7F2E8", ReaderComfortPrefs.pageBackground())
        ReaderVisualMode.entries.forEach { mode ->
            assertEquals("#F7F2E8", ReaderComfortPrefs.pageBackground(mode))
        }
    }
}
