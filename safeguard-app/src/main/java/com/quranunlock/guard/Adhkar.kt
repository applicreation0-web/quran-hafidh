package com.applicreation0.quransafeguard

enum class AdhkarPeriod {
    MORNING,
    EVENING
}

data class AdhkarItem(
    val id: String,
    val periods: Set<AdhkarPeriod>,
    val repeatCount: Int,
    val arabicText: String,
    val transliteration: String,
    val frenchText: String,
    val source: String,
    val authenticity: String
)

object AuthenticAdhkarLibrary {
    val items: List<AdhkarItem> = listOf(
        AdhkarItem(
            id = "ikhlas_3",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 3,
            arabicText = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
            transliteration = "Qul huwa Allāhu aḥad. Allāhu ṣ-Ṣamad. Lam yalid wa lam yūlad. Wa lam yakun lahu kufuwan aḥad.",
            frenchText = "Réciter la sourate Al-Ikhlâs trois fois.",
            source = "Sunan Abi Dawud 5082",
            authenticity = "Hasan (Al-Albani)"
        ),
        AdhkarItem(
            id = "falaq_3",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 3,
            arabicText = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            transliteration = "Qul aʿūdhu bi-rabbi l-falaq. Min sharri mā khalaq. Wa min sharri ghāsiqin idhā waqab. Wa min sharri n-naffāthāti fī l-ʿuqad. Wa min sharri ḥāsidin idhā ḥasad.",
            frenchText = "Réciter la sourate Al-Falaq trois fois.",
            source = "Sunan Abi Dawud 5082",
            authenticity = "Hasan (Al-Albani)"
        ),
        AdhkarItem(
            id = "nas_3",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 3,
            arabicText = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
            transliteration = "Qul aʿūdhu bi-rabbi n-nās. Maliki n-nās. Ilāhi n-nās. Min sharri l-waswāsi l-khannās. Alladhī yuwaswisu fī ṣudūri n-nās. Mina l-jinnati wa n-nās.",
            frenchText = "Réciter la sourate An-Nâs trois fois.",
            source = "Sunan Abi Dawud 5082",
            authenticity = "Hasan (Al-Albani)"
        ),
        AdhkarItem(
            id = "sayyid_istighfar",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 1,
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي، فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allāhumma anta rabbī, lā ilāha illā anta. Khalaqtanī wa anā ʿabduka, wa anā ʿalā ʿahdika wa waʿdika mā istaṭaʿtu. Aʿūdhu bika min sharri mā ṣanaʿtu. Abūʾu laka bi-niʿmatika ʿalayya, wa abūʾu laka bi-dhanbī. Faghfir lī, fa-innahu lā yaghfiru dh-dhunūba illā anta.",
            frenchText = "Ô Allah, Tu es mon Seigneur, nul n’est digne d’adoration en dehors de Toi. Tu m’as créé et je suis Ton serviteur. Je demeure autant que possible fidèle à Ton pacte et à Ta promesse. Je cherche refuge auprès de Toi contre le mal que j’ai commis. Je reconnais Tes bienfaits envers moi et je reconnais mon péché. Pardonne-moi, car nul ne pardonne les péchés en dehors de Toi.",
            source = "Sahih al-Bukhari 6306",
            authenticity = "Sahih"
        ),
        AdhkarItem(
            id = "bismillah_no_harm",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 3,
            arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismi llāhi lladhī lā yaḍurru maʿa ismihi shayʾun fī l-arḍi wa lā fī s-samāʾi, wa huwa s-Samīʿu l-ʿAlīm.",
            frenchText = "Au nom d’Allah, avec le Nom duquel rien sur terre ni dans le ciel ne peut nuire ; Il est Celui qui entend tout et sait tout.",
            source = "Jami’ at-Tirmidhi 3388",
            authenticity = "Hasan/Sahih selon at-Tirmidhi ; Hasan (Darussalam)"
        ),
        AdhkarItem(
            id = "afwa_afiyah",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 1,
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ وَمِنْ خَلْفِي وَعَنْ يَمِينِي وَعَنْ شِمَالِي وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي",
            transliteration = "Allāhumma innī asʾaluka l-ʿāfiyata fī d-dunyā wa l-ākhirah. Allāhumma innī asʾaluka l-ʿafwa wa l-ʿāfiyata fī dīnī wa dunyāya wa ahlī wa mālī. Allāhumma-stur ʿawrātī wa āmin rawʿātī. Allāhumma iḥfaẓnī min bayni yadayya wa min khalfī wa ʿan yamīnī wa ʿan shimālī wa min fawqī, wa aʿūdhu bi-ʿaẓamatika an ughtāla min taḥtī.",
            frenchText = "Ô Allah, je Te demande la préservation ici-bas et dans l’au-delà. Je Te demande le pardon et la préservation dans ma religion, ma vie d’ici-bas, ma famille et mes biens. Cache mes défauts, apaise mes craintes et protège-moi devant moi, derrière moi, à ma droite, à ma gauche et au-dessus de moi. Je cherche refuge dans Ta grandeur contre le fait d’être atteint par-dessous.",
            source = "Sunan Abi Dawud 5074",
            authenticity = "Sahih (Al-Albani)"
        ),
        AdhkarItem(
            id = "bika_asbahna",
            periods = setOf(AdhkarPeriod.MORNING),
            repeatCount = 1,
            arabicText = "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيرُ",
            transliteration = "Allāhumma bika aṣbaḥnā wa bika amsaynā, wa bika naḥyā wa bika namūtu, wa ilayka l-maṣīr.",
            frenchText = "Ô Allah, c’est par Toi que nous entrons dans le matin, par Toi que nous entrons dans le soir, par Toi que nous vivons et par Toi que nous mourons, et vers Toi est le retour.",
            source = "Jami’ at-Tirmidhi 3391",
            authenticity = "Hasan selon at-Tirmidhi ; Sahih (Darussalam)"
        ),
        AdhkarItem(
            id = "bika_amsayna",
            periods = setOf(AdhkarPeriod.EVENING),
            repeatCount = 1,
            arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا وَبِكَ أَصْبَحْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ",
            transliteration = "Allāhumma bika amsaynā wa bika aṣbaḥnā, wa bika naḥyā wa bika namūtu, wa ilayka n-nushūr.",
            frenchText = "Ô Allah, c’est par Toi que nous entrons dans le soir, par Toi que nous entrons dans le matin, par Toi que nous vivons et par Toi que nous mourons, et vers Toi est la résurrection.",
            source = "Jami’ at-Tirmidhi 3391",
            authenticity = "Hasan selon at-Tirmidhi ; Sahih (Darussalam)"
        ),
        AdhkarItem(
            id = "alim_alghayb",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 1,
            arabicText = "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ فَاطِرَ السَّمَوَاتِ وَالْأَرْضِ رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ",
            transliteration = "Allāhumma ʿālima l-ghaybi wa sh-shahādah, fāṭira s-samāwāti wa l-arḍ, rabba kulli shayʾin wa malīkah. Ashhadu an lā ilāha illā anta. Aʿūdhu bika min sharri nafsī wa min sharri sh-shayṭāni wa shirkih.",
            frenchText = "Ô Allah, Connaisseur de l’invisible et du visible, Créateur des cieux et de la terre, Seigneur et Maître de toute chose, j’atteste que nul n’est digne d’adoration en dehors de Toi. Je cherche refuge auprès de Toi contre le mal de mon âme et contre le mal de Satan et son association.",
            source = "Jami’ at-Tirmidhi 3392",
            authenticity = "Hasan Sahih selon at-Tirmidhi"
        ),
        AdhkarItem(
            id = "afini_badani",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 3,
            arabicText = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ",
            transliteration = "Allāhumma ʿāfinī fī badanī. Allāhumma ʿāfinī fī samʿī. Allāhumma ʿāfinī fī baṣarī. Lā ilāha illā anta.",
            frenchText = "Ô Allah, accorde-moi la santé dans mon corps. Ô Allah, préserve mon ouïe. Ô Allah, préserve ma vue. Nul n’est digne d’adoration en dehors de Toi.",
            source = "Sunan Abi Dawud 5090",
            authenticity = "Hasan dans la chaîne (Al-Albani)"
        ),
        AdhkarItem(
            id = "subhanallah_100",
            periods = setOf(AdhkarPeriod.MORNING, AdhkarPeriod.EVENING),
            repeatCount = 100,
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            transliteration = "Subḥāna llāhi wa bi-ḥamdih.",
            frenchText = "Gloire et louange à Allah.",
            source = "Sahih Muslim 2692",
            authenticity = "Sahih"
        ),
        AdhkarItem(
            id = "juwayriya_3",
            periods = setOf(AdhkarPeriod.MORNING),
            repeatCount = 3,
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
            transliteration = "Subḥāna llāhi wa bi-ḥamdih, ʿadada khalqih, wa riḍā nafsih, wa zinata ʿarshih, wa midāda kalimātih.",
            frenchText = "Gloire et louange à Allah autant que le nombre de Ses créatures, selon Son agrément, selon le poids de Son Trône et autant que l’encre de Ses paroles.",
            source = "Sahih Muslim 2726a",
            authenticity = "Sahih"
        ),
        AdhkarItem(
            id = "raditu_evening",
            periods = setOf(AdhkarPeriod.EVENING),
            repeatCount = 1,
            arabicText = "رَضِيتُ بِاللَّهِ رَبًّا وَبِالْإِسْلَامِ دِينًا وَبِمُحَمَّدٍ نَبِيًّا",
            transliteration = "Raḍītu bi-llāhi rabban, wa bi-l-islāmi dīnan, wa bi-Muḥammadin nabiyyan.",
            frenchText = "J’agrée Allah comme Seigneur, l’Islam comme religion et Muhammad ﷺ comme Prophète.",
            source = "Jami’ at-Tirmidhi 3389",
            authenticity = "Hasan Gharib selon at-Tirmidhi"
        )
    )

    fun forPeriod(period: AdhkarPeriod): List<AdhkarItem> =
        items.filter { period in it.periods }
}
