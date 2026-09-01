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

data class ClassicalVerification(
    val sourceVerified: Boolean,
    val attributionVerified: Boolean,
    val translationVerified: Boolean,
    val humanVerified: Boolean,
    val rightsStatus: TranslationRightsStatus,
    val authenticityStatus: ClassicalAuthenticityStatus,
    val verificationNote: String
) {
    val displayEligible: Boolean
        get() =
            sourceVerified &&
                attributionVerified &&
                translationVerified &&
                humanVerified &&
                rightsStatus != TranslationRightsStatus.UNRESOLVED &&
                authenticityStatus == ClassicalAuthenticityStatus.VERIFIED_SOURCE
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
)
