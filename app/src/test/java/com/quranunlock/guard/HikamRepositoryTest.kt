package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamRepositoryTest {
    private val verification = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = "verified corpus test"
    )

    private val integrity = ClassicalTextIntegrity(
        form = ClassicalTextForm.COMPLETE_TEXT,
        reconstructedOrAssembled = false,
        hasInternalOmissions = false,
        contextChecked = true,
        passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
    )

    private fun source(translator: String? = "Traduction interne Quran Safeguard") =
        ClassicalSource(
            author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
            workTitle = "Al-Hikam al-ʿAṭāʾiyya",
            edition = "édition arabe vérifiée",
            editor = "éditeur",
            volume = null,
            locator = "Hikma 1",
            sourceUrl = "https://example.test/hikam/1",
            translator = translator
        )

    private fun entry(commentary: HikmaCommentary? = null) =
        HikmaEntry(
            canonicalId = "hikma_1",
            sourceNumber = 1,
            arabicText = "من علامات الاعتماد على العمل نقصان الرجاء عند وجود الزلل",
            frenchText = "Parmi les signes de l’appui sur l’œuvre : la diminution de l’espérance lorsqu’une faute survient.",
            theme = "spiritual_presence",
            tags = setOf("spiritual_presence"),
            source = source(),
            verification = verification,
            commentary = commentary,
            textIntegrity = integrity
        )

    @Test
    fun sourcedHikmaCanDisplayWithoutUnverifiedCommentary() {
        assertTrue(entry(commentary = null).displayEligible)
    }

    @Test
    fun multipleCommentatorsRemainSeparateSourceUnits() {
        fun commentary(author: String, work: String, locator: String) =
            HikmaCommentary(
                arabicText = "نص تعليق موثق […]",
                frenchText = "Texte de commentaire traduit […].",
                source = ClassicalSource(
                    author = author,
                    workTitle = work,
                    edition = "édition vérifiée",
                    editor = "éditeur",
                    volume = null,
                    locator = locator,
                    sourceUrl = "https://example.test/commentary/" + author.hashCode(),
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verification,
                isExcerpt = true,
                textIntegrity = ClassicalTextIntegrity(
                    form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                    reconstructedOrAssembled = false,
                    hasInternalOmissions = true,
                    contextChecked = true,
                    passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
                )
            )

        val ibnAjiba = commentary("Ibn ʿAjība", "Īqāẓ al-Himam", "Hikma 1 • p. 10")
        val zarruq = commentary("Aḥmad Zarrūq", "Sharḥ al-Ḥikam", "Hikma 1 • p. 20")

        val model = entry(commentary = ibnAjiba).copy(
            additionalCommentaries = listOf(zarruq)
        )

        assertTrue(model.commentaries.size == 2)
        assertTrue(model.commentaries[0].source.author == "Ibn ʿAjība")
        assertTrue(model.commentaries[1].source.author == "Aḥmad Zarrūq")
        assertTrue(model.commentaries.all { it.displayEligible })
    }

    @Test
    fun productionCommentariesAreExactlyTheReviewedSourceUnits() {
        val expected = ((1..15) + (17..20)).toSet()
        assertEquals(expected, HikamRepository.commentaryByNumber.keys)

        HikamRepository.commentaryByNumber.forEach { (sourceNumber, commentary) ->
            assertTrue(commentary.displayEligible)
            assertTrue(commentary.isExcerpt)
            assertEquals("Ibn ʿAjība", commentary.source.author)
            assertEquals("Īqāẓ al-Himam fī Sharḥ al-Ḥikam", commentary.source.workTitle)
            assertTrue(commentary.source.locator.startsWith("Hikma " + sourceNumber + " • p. "))
            assertTrue(commentary.source.sourceUrl.endsWith("/" + commentary.source.locator.substringAfter("p. ")))
            assertTrue(commentary.source.translator == "Traduction interne Quran Safeguard")
            assertFalse(commentary.arabicText.contains("auto_stories"))
            assertFalse(commentary.arabicText.contains("chevron_right"))
            assertFalse(commentary.arabicText.contains("الرئيسية/"))
            assertFalse(commentary.arabicText.contains("[…]"))
        }
    }

    @Test
    fun translationProvenanceIsMandatory() {
        assertTrue(source().documentaryComplete)
        assertFalse(source(translator = null).documentaryComplete)
        assertFalse(source(translator = "").documentaryComplete)
    }

    @Test
    fun invalidArabicOrFrenchCannotDisplay() {
        assertFalse(entry().copy(arabicText = "").displayEligible)
        assertFalse(entry().copy(frenchText = "").displayEligible)
    }

    @Test
    fun sourceNumberMustStayInsideRetained264Range() {
        assertTrue(entry().displayEligible)
        assertFalse(entry().copy(sourceNumber = 0).displayEligible)
        assertFalse(entry().copy(sourceNumber = 265).displayEligible)
    }
}
