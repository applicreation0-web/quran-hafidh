package com.applicreation0.quransafeguard

/**
 * Contract for long-form classical material shown behind "Approfondir".
 *
 * No generated summary or paraphrase is allowed here. A displayable record must
 * contain the source Arabic and a matching French translation, plus provenance.
 */
enum class ClassicalDepthKind {
    HIKAM_CLASSICAL_COMMENTARY,
    GHAZALI_SAME_AUTHOR_CONTEXT
}

enum class TranslationRightsStatus {
    CLEARED,
    PUBLIC_DOMAIN,
    INTERNAL_TRANSLATION_CLEARED,
    UNRESOLVED
}

data class ClassicalDepthEntry(
    val reminderId: String,
    val kind: ClassicalDepthKind,
    val sourceAuthor: String,
    val sourceWork: String,
    val arabicText: String,
    val frenchText: String,
    val sourceUrl: String,
    val sourceReference: String,
    val verificationDate: String,
    val translationRightsStatus: TranslationRightsStatus,
    val humanVerified: Boolean,
    val completePassage: Boolean
) {
    val isDisplayReady: Boolean
        get() =
            arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                sourceUrl.startsWith("https://") &&
                sourceReference.isNotBlank() &&
                humanVerified &&
                translationRightsStatus != TranslationRightsStatus.UNRESOLVED

    val buttonLabel: String
        get() = when (kind) {
            ClassicalDepthKind.HIKAM_CLASSICAL_COMMENTARY ->
                "Approfondir — commentaire classique"
            ClassicalDepthKind.GHAZALI_SAME_AUTHOR_CONTEXT ->
                "Approfondir — contexte dans l’œuvre"
        }
}

object ClassicalDepthRepository {
    /**
     * Intentionally empty until each long-form source has been collated,
     * translated, rights-cleared and human-verified.
     *
     * Adding a record here makes the Approfondir button visible automatically.
     */
    val entries: List<ClassicalDepthEntry> = emptyList()

    fun displayEntry(reminderId: String): ClassicalDepthEntry? =
        entries.firstOrNull { it.reminderId == reminderId && it.isDisplayReady }

    init {
        require(entries.map { it.reminderId }.distinct().size == entries.size) {
            "Duplicate classical depth entries"
        }

        entries.forEach { entry ->
            when (entry.kind) {
                ClassicalDepthKind.HIKAM_CLASSICAL_COMMENTARY -> {
                    require(entry.reminderId.startsWith("hikma_")) {
                        "Hikam commentary must target a Hikma"
                    }
                    require(entry.sourceAuthor.contains("Ajība") || entry.sourceAuthor.contains("Ajiba")) {
                        "Current Hikam deepening policy requires Ibn Ajiba to be explicitly identified"
                    }
                }

                ClassicalDepthKind.GHAZALI_SAME_AUTHOR_CONTEXT -> {
                    require(entry.reminderId.startsWith("ghazali_")) {
                        "Ghazali context must target a Ghazali excerpt"
                    }
                    require(entry.sourceAuthor.contains("Ghaz")) {
                        "Ghazali deepening must remain the author's own surrounding text"
                    }
                }
            }
        }
    }
}
