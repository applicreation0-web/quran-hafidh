package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamTechnicalLexiconTest {
    private fun keys(arabic: String) = HikamTechnicalLexicon
        .forArabicText(arabic)
        .map { it.key }
        .toSet()

    @Test
    fun technicalNounsAreMatchedAfterRemovingArabicDiacritics() {
        val found = keys("الْقَبْضُ والبَسْطُ والوَارِدُ")
        assertTrue("qabd" in found)
        assertTrue("bast" in found)
        assertTrue("warid" in found)
    }

    @Test
    fun ordinaryVerbOrCarpetMetaphorDoesNotCreateFalseTechnicalTerm() {
        assertFalse("qabd" in keys("تارة يقبض ذلك عنك"))
        assertFalse("bast" in keys("الفاقات بسط المواهب"))
    }

    @Test
    fun bareNafsIsNotOverclassifiedButDefiniteTechnicalNafsIsAvailable() {
        assertFalse("nafs" in keys("ما من نفس تبديه"))
        assertTrue("nafs" in keys("والقبض لا حظ للنفس فيه"))
    }
}
