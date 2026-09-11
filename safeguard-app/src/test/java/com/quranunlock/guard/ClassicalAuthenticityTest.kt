package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicalAuthenticityTest {
    private fun verification(
        source: Boolean = true,
        attribution: Boolean = true,
        translation: Boolean = true,
        human: Boolean = true,
        rights: TranslationRightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        status: ClassicalAuthenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE
    ) = ClassicalVerification(
        sourceVerified = source,
        attributionVerified = attribution,
        translationAvailable = translation,
        humanVerified = human,
        rightsStatus = rights,
        authenticityStatus = status,
        verificationNote = "test"
    )

    @Test
    fun documentaryAuthenticityRequiresTranslationButNotHumanReview() {
        assertTrue(verification().displayEligible)
        assertFalse(verification(source = false).displayEligible)
        assertFalse(verification(attribution = false).displayEligible)
        assertFalse(verification(translation = false).displayEligible)
        assertTrue(verification(human = false).displayEligible)
        assertFalse(
            verification(rights = TranslationRightsStatus.UNRESOLVED).displayEligible
        )
        assertFalse(
            verification(status = ClassicalAuthenticityStatus.PARTIALLY_VERIFIED)
                .displayEligible
        )
    }
}
