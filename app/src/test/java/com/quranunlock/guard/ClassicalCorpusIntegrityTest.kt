package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicalCorpusIntegrityTest {
    private val verification = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = "internal translation test"
    )

    private fun source(locator: String = "Hikma 1") = ClassicalSource(
        author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
        workTitle = "Al-Hikam al-ʿAṭāʾiyya",
        edition = "source",
        editor = null,
        volume = null,
        locator = locator,
        sourceUrl = "https://example.test/source",
        translator = "Traduction interne Quran Safeguard"
    )

    private val integrity = ClassicalTextIntegrity(
        form = ClassicalTextForm.COMPLETE_TEXT,
        reconstructedOrAssembled = false,
        hasInternalOmissions = false,
        contextChecked = true,
        passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
    )

    @Test
    fun classicalTextCannotDisplayWithoutArabic() {
        val entry = HikmaEntry(
            canonicalId = "test",
            sourceNumber = 1,
            arabicText = "",
            frenchText = "Traduction",
            theme = "test",
            tags = setOf("test"),
            source = source(),
            verification = verification,
            textIntegrity = integrity
        )
        assertFalse(entry.displayEligible)
    }

    @Test
    fun classicalTextCannotDisplayWithoutLocator() {
        val entry = HikmaEntry(
            canonicalId = "test",
            sourceNumber = 1,
            arabicText = "نص",
            frenchText = "Traduction",
            theme = "test",
            tags = setOf("test"),
            source = source(locator = ""),
            verification = verification,
            textIntegrity = integrity
        )
        assertFalse(entry.displayEligible)
    }

    @Test
    fun classicalSourceRequiresAuthorWorkAndSourceUrl() {
        val base = source()

        assertFalse(
            base.copy(author = "").documentaryComplete
        )
        assertFalse(
            base.copy(workTitle = "").documentaryComplete
        )
        assertFalse(
            base.copy(sourceUrl = "").documentaryComplete
        )
        assertFalse(
            base.copy(locator = "").documentaryComplete
        )
        assertFalse(
            base.copy(translator = null).documentaryComplete
        )
        assertFalse(
            base.copy(translator = "").documentaryComplete
        )
        assertTrue(base.documentaryComplete)
    }

    @Test
    fun classicalAttributionIsMandatoryEvenWithTranslation() {
        val missingAttribution = verification.copy(attributionVerified = false)
        assertFalse(missingAttribution.displayEligible)
    }

    @Test
    fun internalTranslationCanDisplayWithoutHumanVerification() {
        assertFalse(verification.humanVerified)
        assertTrue(verification.displayEligible)
    }

    @Test
    fun unmarkedInternalOmissionIsRejected() {
        val excerptIntegrity = ClassicalTextIntegrity(
            form = ClassicalTextForm.CONTINUOUS_EXCERPT,
            reconstructedOrAssembled = false,
            hasInternalOmissions = true,
            contextChecked = true,
            passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
        )
        assertFalse(
            excerptIntegrity.allows(
                arabicText = "نص مختصر",
                frenchText = "Texte abrégé"
            )
        )
        assertTrue(
            excerptIntegrity.allows(
                arabicText = "نص […] مختصر",
                frenchText = "Texte […] abrégé"
            )
        )
    }

    @Test
    fun reconstructedClassicalPassageIsRejected() {
        val reconstructed = ClassicalTextIntegrity(
            form = ClassicalTextForm.CONTINUOUS_EXCERPT,
            reconstructedOrAssembled = true,
            hasInternalOmissions = false,
            contextChecked = true,
            passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
        )
        assertFalse(reconstructed.allows("نص", "Texte"))
    }
}
