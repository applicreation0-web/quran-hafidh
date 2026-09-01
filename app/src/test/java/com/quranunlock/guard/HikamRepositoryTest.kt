package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamRepositoryTest {
    @Test
    fun canonicalEntriesHaveDocumentaryMetadata() {
        val entries = HikamRepository.entries
        assertTrue(entries.isNotEmpty())
        assertEquals(entries.size, entries.map { it.canonicalId }.distinct().size)
        assertEquals(entries.size, entries.map { it.sourceNumber }.distinct().size)

        entries.forEach { hikma ->
            assertTrue(hikma.arabicText.isNotBlank())
            assertTrue(hikma.sourceNumber in 1..264)
            assertTrue(hikma.frenchText.isNotBlank())
            assertTrue(hikma.source.author.contains("Ibn ʿAṭāʾ Allāh"))
            assertTrue(hikma.source.workTitle.contains("Hikam"))
            assertTrue(hikma.source.locator.isNotBlank())
            assertTrue(hikma.source.sourceUrl.startsWith("https://"))
            assertTrue(hikma.verification.sourceVerified)
            assertTrue(hikma.verification.attributionVerified)
            assertTrue(hikma.verification.translationAvailable)
            assertFalse(hikma.verification.humanVerified)
            assertTrue(hikma.displayEligible)
        }
    }

    @Test
    fun sourcedHikamAreVisibleWithInternalTranslationDisclosure() {
        assertEquals(
            HikamRepository.entries.count { it.displayEligible },
            HikamRepository.asDailyReminders().size
        )
        HikamRepository.entries.forEach {
            assertTrue(HikamRepository.byId(it.canonicalId) != null)
        }
    }

    @Test
    fun ibnAjibaIsAlwaysCommentatorNotHikamAuthor() {
        HikamRepository.entries.forEach { hikma ->
            val commentary = hikma.commentary!!
            assertEquals("Ibn ʿAjība", commentary.source.author)
            assertTrue(commentary.source.workTitle.contains("Īqāẓ al-Himam"))
            assertTrue(commentary.source.locator.contains("Hikma " + hikma.sourceNumber))
            assertTrue(commentary.source.sourceUrl.startsWith("https://"))
            assertTrue(commentary.isExcerpt)
            assertTrue(commentary.arabicText.contains("[…]"))
            assertTrue(commentary.displayEligible)
        }
    }

    @Test
    fun sourceNumberingAndArabicStayLockedToRetainedSource() {
        assertEquals(
            "اجتهادك فيما ضمن لك وتقصيرك فيما طلب منك دليل على انطماس البصيرة منك.",
            HikamRepository.entries.first { it.sourceNumber == 5 }.arabicText
        )
        assertEquals(
            "الأعمال صور قائمة، وأرواحها وجود سر الإخلاص فيها.",
            HikamRepository.entries.first { it.sourceNumber == 10 }.arabicText
        )
        assertEquals(
            "ما نفع القلب شئ مثل عزلة يدخل بها ميدان فكرة.",
            HikamRepository.entries.first { it.sourceNumber == 12 }.arabicText
        )
    }
}
