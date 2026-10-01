package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HifzPersistenceIsolationTest {
    @Test
    fun structuredHifzCannotShareFreeMemorisationPreferencesFile() {
        assertEquals("reader109", QuranPersistenceNamespaces.FREE_READER_MEMORIZATION)
        assertEquals("hifz_01010", HifzStateStore.FILE)
        assertEquals("hifz_reader_01010", QuranPersistenceNamespaces.HIFZ_READER)
        assertNotEquals(QuranPersistenceNamespaces.FREE_READER_MEMORIZATION, HifzStateStore.FILE)
        assertNotEquals(QuranPersistenceNamespaces.FREE_READER_LAST_PAGE, HifzStateStore.FILE)
        assertNotEquals(QuranPersistenceNamespaces.FREE_READER_MEMORIZATION, QuranPersistenceNamespaces.HIFZ_READER)
        assertNotEquals(HifzStateStore.FILE, QuranPersistenceNamespaces.HIFZ_READER)
    }
}
