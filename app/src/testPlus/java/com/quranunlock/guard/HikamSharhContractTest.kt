package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamSharhContractTest {
    @Test
    fun fullyVerifiedEntryIsDisplayEligible() {
        val entry = HikamSharhEdition.parse(sampleJson()).single()

        assertTrue(entry.verified)
        assertTrue(entry.displayEligible)
        assertTrue(entry.sourceEdition.isNotBlank())
        assertTrue(entry.translationCredit.isNotBlank())
        assertTrue(entry.boundaryLocator.isNotBlank())
    }

    @Test
    fun unverifiedBoundaryIsRejectedAtRuntime() {
        val entry = HikamSharhEdition.parse(
            sampleJson(sourceBoundaryStatus = "candidate")
        ).single()

        assertFalse(entry.verified)
        assertFalse(entry.displayEligible)
    }

    @Test
    fun unverifiedAlignmentIsRejectedAtRuntime() {
        val entry = HikamSharhEdition.parse(
            sampleJson(alignmentStatus = "candidate")
        ).single()

        assertFalse(entry.verified)
        assertFalse(entry.displayEligible)
    }

    @Test(expected = org.json.JSONException::class)
    fun missingBoundaryLocatorIsRejectedByParser() {
        HikamSharhEdition.parse(
            sampleJson().replace(
                "\"commentary_boundary_locator\":\"matn A → next matn B\",",
                ""
            )
        )
    }

    private fun sampleJson(
        sourceBoundaryStatus: String = "verified",
        alignmentStatus: String = "verified"
    ): String = """
        [
          {
            "source_number": 1,
            "commentator_id": "sharnubi",
            "commentary_work": "شرح الحكم العطائية",
            "commentary_source_edition": "Dar Ibn Kathir, 2e éd., 1410/1989",
            "commentary_arabic": "هذا نص عربي تجريبي طويل بما يكفي لاختبار عقد البيانات فقط ولا يمثل محتوى منشورا.",
            "commentary_french": "Texte de test suffisamment long pour vérifier le contrat de données sans constituer un commentaire publié.",
            "commentary_translation_credit": "Quran Safeguard — test",
            "commentary_source_url": "https://archive.org/example",
            "commentary_print_locator": "p. 12",
            "commentary_boundary_locator": "matn A → next matn B",
            "commentary_status": "verified",
            "translation_status": "verified",
            "hikma_alignment_status": "$alignmentStatus",
            "source_boundary_status": "$sourceBoundaryStatus"
          }
        ]
    """.trimIndent()
}
