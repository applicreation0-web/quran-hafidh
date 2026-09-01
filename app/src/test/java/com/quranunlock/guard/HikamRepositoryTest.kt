package com.applicreation0.quransafeguard

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
