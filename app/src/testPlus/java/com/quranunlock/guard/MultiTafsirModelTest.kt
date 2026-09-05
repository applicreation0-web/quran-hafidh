package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiTafsirModelTest {
    @Test
    fun editionStorageValuesRemainStable() {
        assertEquals("jalalayn", PrivateTafsirEdition.JALALAYN.storageValue)
        assertEquals("qurtubi", PrivateTafsirEdition.QURTUBI.storageValue)
        assertEquals("qushayri", PrivateTafsirEdition.QUSHAYRI.storageValue)
        assertEquals(PrivateTafsirEdition.JALALAYN, PrivateTafsirEdition.fromStorage("unknown"))
    }

    @Test
    fun requestKeyChangesWhenOnlyEditionChanges() {
        val verse = VerseRef(2, 68)
        assertNotEquals(
            MultiTafsirRequestKey(verse, PrivateTafsirEdition.QUSHAYRI),
            MultiTafsirRequestKey(verse, PrivateTafsirEdition.QURTUBI)
        )
    }

    @Test
    fun preferredEditionIsKeptWhenItReallyCoversVerse() {
        val availability = MultiTafsirAvailability(
            mapOf(
                PrivateTafsirEdition.JALALAYN to entry(),
                PrivateTafsirEdition.QUSHAYRI to entry()
            )
        )

        assertEquals(
            PrivateTafsirEdition.QUSHAYRI,
            availability.resolveEdition(PrivateTafsirEdition.QUSHAYRI)
        )
    }

    @Test
    fun jalalaynIsFirstFallbackWhenRememberedEditionHasNoEntry() {
        val availability = MultiTafsirAvailability(
            mapOf(
                PrivateTafsirEdition.JALALAYN to entry(),
                PrivateTafsirEdition.QURTUBI to entry()
            )
        )

        assertEquals(
            PrivateTafsirEdition.JALALAYN,
            availability.resolveEdition(PrivateTafsirEdition.QUSHAYRI)
        )
        assertEquals(
            listOf(PrivateTafsirEdition.JALALAYN, PrivateTafsirEdition.QURTUBI),
            availability.editions
        )
    }

    @Test
    fun firstRealEditionIsFallbackOnlyWhenJalalaynIsUnavailable() {
        val availability = MultiTafsirAvailability(
            mapOf(PrivateTafsirEdition.QURTUBI to entry())
        )

        assertEquals(
            PrivateTafsirEdition.QURTUBI,
            availability.resolveEdition(PrivateTafsirEdition.QUSHAYRI)
        )
    }

    @Test
    fun emptyCoverageDoesNotInventAnEdition() {
        assertNull(
            MultiTafsirAvailability(emptyMap()).resolveEdition(
                PrivateTafsirEdition.QUSHAYRI
            )
        )
    }

    private fun entry(): TafsirEntry = TafsirEntry(
        verse = VerseRef(2, 68),
        commentaryRuns = listOf(TafsirRun(TafsirRunStyle.REGULAR, "source")),
        notes = emptyList()
    )
}
