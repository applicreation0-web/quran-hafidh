package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class JalalaynHonorificPresentationTest {
    @Test
    fun prophetHonorificIsNormalizedWithoutTranslatingEnglish() {
        val source = "The Prophet (ṣ) is mentioned in this English commentary."
        assertEquals(
            "The Prophet ﷺ is mentioned in this English commentary.",
            JalalaynHonorificPresentation.normalize(source)
        )
    }

    @Test
    fun otherProphetHonorificIsNormalizedWithoutChangingTheName() {
        val source = "Moses (ʿa) returned to his people."
        assertEquals(
            "Moses عليه السلام returned to his people.",
            JalalaynHonorificPresentation.normalize(source)
        )
    }

    @Test
    fun unrelatedEnglishIsByteForByteUnchanged() {
        val source = "This sentence contains no honorific shorthand."
        assertEquals(source, JalalaynHonorificPresentation.normalize(source))
    }
}
