package com.applicreation0.quransafeguard

data class SourceVerification(
    val sourceUrl: String,
    val verificationDate: String,
    val note: String
)

object ReligiousSourceRegistry {
    private const val VERIFIED_ON = "2026-08-31"

    val reminderSources: Map<String, SourceVerification> = mapOf(
        "hadith_bukhari_1" to v("https://sunnah.com/bukhari:1", "Sahih al-Bukhari 1"),
        "hadith_muslim_2593" to v("https://sunnah.com/muslim:2593", "Sahih Muslim 2593"),
        "hadith_bukhari_6018" to v("https://sunnah.com/bukhari:6018", "Sahih al-Bukhari 6018"),
        "hadith_bukhari_5027" to v("https://sunnah.com/bukhari:5027", "Sahih al-Bukhari 5027"),
        "hadith_bukhari_6114" to v("https://sunnah.com/bukhari:6114", "Sahih al-Bukhari 6114"),
        "hadith_muslim_223" to v("https://sunnah.com/muslim:223", "Sahih Muslim 223"),
        "hadith_nasai_5" to v("https://sunnah.com/nasai:5", "Sunan an-Nasa’i 5 • Sahih"),
        "hadith_bukhari_5971" to v("https://sunnah.com/bukhari:5971", "Sahih al-Bukhari 5971"),
        "hadith_bukhari_5997" to v("https://sunnah.com/bukhari:5997", "Sahih al-Bukhari 5997"),
        "hadith_tirmidhi_3895" to v("https://sunnah.com/tirmidhi:3895", "Jami’ at-Tirmidhi 3895 • Hasan Gharib Sahih / Sahih"),
        "hadith_muslim_2588" to v("https://sunnah.com/muslim:2588", "Sahih Muslim 2588"),
        "hadith_muslim_2999" to v("https://sunnah.com/muslim:2999", "Sahih Muslim 2999"),
        "hadith_tirmidhi_1956" to v("https://sunnah.com/tirmidhi:1956", "Jami’ at-Tirmidhi 1956 • Hasan"),
        "hadith_bukhari_6138" to v("https://sunnah.com/bukhari:6138", "Sahih al-Bukhari 6138"),
        "hadith_muslim_223_quran" to v("https://sunnah.com/muslim:223", "Sahih Muslim 223"),
        "hadith_tirmidhi_1956_common_space" to v("https://sunnah.com/tirmidhi:1956", "Jami’ at-Tirmidhi 1956 • Hasan"),
        "hadith_muslim_2699_help" to v("https://sunnah.com/muslim:2699a", "Sahih Muslim 2699a"),



)

    val adhkarSources: Map<String, SourceVerification> = mapOf(
        "ikhlas_3" to v("https://sunnah.com/abudawud:5082", "Sunan Abi Dawud 5082 • Hasan"),
        "falaq_3" to v("https://sunnah.com/abudawud:5082", "Sunan Abi Dawud 5082 • Hasan"),
        "nas_3" to v("https://sunnah.com/abudawud:5082", "Sunan Abi Dawud 5082 • Hasan"),
        "sayyid_istighfar" to v("https://sunnah.com/bukhari:6306", "Sahih al-Bukhari 6306"),
        "bismillah_no_harm" to v("https://sunnah.com/tirmidhi:3388", "Jami’ at-Tirmidhi 3388 • Hasan Sahih Gharib / Hasan"),
        "afwa_afiyah" to v("https://sunnah.com/abudawud:5074", "Sunan Abi Dawud 5074 • Sahih"),
        "bika_asbahna" to v("https://sunnah.com/tirmidhi:3391", "Jami’ at-Tirmidhi 3391 • Hasan / Sahih"),
        "bika_amsayna" to v("https://sunnah.com/tirmidhi:3391", "Jami’ at-Tirmidhi 3391 • Hasan / Sahih"),
        "alim_alghayb" to v("https://sunnah.com/tirmidhi:3392", "Jami’ at-Tirmidhi 3392 • Hasan Sahih"),
        "afini_badani" to v("https://sunnah.com/abudawud:5090", "Sunan Abi Dawud 5090 • Hasan in chain"),
        "subhanallah_100" to v("https://sunnah.com/muslim:2692", "Sahih Muslim 2692"),
        "juwayriya_3" to v("https://sunnah.com/muslim:2726a", "Sahih Muslim 2726a"),
        "raditu_evening" to v("https://sunnah.com/tirmidhi:3389", "Jami’ at-Tirmidhi 3389 • Hasan Gharib")
    )

    private fun v(url: String, note: String) =
        SourceVerification(url, VERIFIED_ON, note)
}
