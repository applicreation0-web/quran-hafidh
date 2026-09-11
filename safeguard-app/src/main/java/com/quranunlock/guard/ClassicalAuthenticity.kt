package com.applicreation0.quransafeguard

enum class ClassicalAuthenticityStatus {
    VERIFIED_SOURCE,
    PARTIALLY_VERIFIED,
    NOT_VERIFIED
}

enum class TranslationRightsStatus {
    PUBLIC_DOMAIN,
    LICENSED,
    INTERNAL_TRANSLATION_ALLOWED,
    UNRESOLVED
}

enum class ClassicalTextForm {
    COMPLETE_TEXT,
    CONTINUOUS_EXCERPT
}

enum class ClassicalPassageRole {
    AUTHOR_OWN_WORDS,
    REPORTED_QUOTATION,
    EXAMPLE,
    OBJECTION,
    MIXED_CONTEXT
}

/**
 * Documentary authenticity and translation provenance are deliberately separate.
 *
 * humanVerified is informative metadata only. A non-human internal translation may
 * be displayed when the Arabic text, attribution, source/locator and translation
 * availability are all verified under the rights policy.
 */
data class ClassicalVerification(
    val sourceVerified: Boolean,
    val attributionVerified: Boolean,
    val translationAvailable: Boolean,
    val humanVerified: Boolean,
    val rightsStatus: TranslationRightsStatus,
    val authenticityStatus: ClassicalAuthenticityStatus,
    val verificationNote: String
) {
    val displayEligible: Boolean
        get() =
            sourceVerified &&
                attributionVerified &&
                translationAvailable &&
                rightsStatus != TranslationRightsStatus.UNRESOLVED &&
                authenticityStatus == ClassicalAuthenticityStatus.VERIFIED_SOURCE
}

data class ClassicalTextIntegrity(
    val form: ClassicalTextForm,
    val reconstructedOrAssembled: Boolean,
    val hasInternalOmissions: Boolean,
    val contextChecked: Boolean,
    val passageRole: ClassicalPassageRole
) {
    fun allows(arabicText: String, frenchText: String): Boolean =
        !reconstructedOrAssembled &&
            contextChecked &&
            (!hasInternalOmissions ||
                (arabicText.contains("[…]") && frenchText.contains("[…]")))
}

data class ClassicalSource(
    val author: String,
    val workTitle: String,
    val edition: String,
    val editor: String?,
    val volume: String?,
    val locator: String,
    val sourceUrl: String,
    val translator: String?
) {
    val documentaryComplete: Boolean
        get() =
            author.isNotBlank() &&
                workTitle.isNotBlank() &&
                edition.isNotBlank() &&
                locator.isNotBlank() &&
                sourceUrl.isNotBlank() &&
                !translator.isNullOrBlank()
}
