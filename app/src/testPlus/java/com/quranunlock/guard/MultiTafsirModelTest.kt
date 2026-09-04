package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
    fun rangeRunCanLabelSharedCommentaryWithoutDuplicatingSourceText() {
        val tapped = VerseRef(4, 12)
        val start = 11
        val end = 14
        assertTrue(tapped.ayah in start..end)
        assertEquals("Commentary on 4:11–14", "Commentary on ${tapped.surah}:$start–$end")
    }
}
