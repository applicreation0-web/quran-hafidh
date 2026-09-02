package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Canonical in-app source for the 264 Al-Hikam al-ʿAṭāʾiyya retained for 0.9.1.
 *
 * The Arabic matn and the internal French translations are loaded from the frozen,
 * release-audited asset hikam/al_hikam_verified.json. No AI summary or explanation
 * is stored or generated. Ibn ʿAjība commentary is attached only where a separately
 * sourced commentary excerpt has already been verified.
 */
data class HikmaCommentary(
    val arabicText: String,
    val frenchText: String,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val isExcerpt: Boolean,
    val textIntegrity: ClassicalTextIntegrity
) {
    val displayEligible: Boolean
        get() =
            arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(arabicText, frenchText)
}

data class HikmaEntry(
    val canonicalId: String,
    val sourceNumber: Int,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val commentary: HikmaCommentary?,
    val additionalCommentaries: List<HikmaCommentary> = emptyList(),
    val textIntegrity: ClassicalTextIntegrity
) {
    /**
     * Classical commentaries are intentionally kept as independent source units.
     * Never merge, synthesize or paraphrase multiple commentators into one text.
     */
    val commentaries: List<HikmaCommentary>
        get() = listOfNotNull(commentary) + additionalCommentaries

    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                sourceNumber in 1..264 &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(arabicText, frenchText)
}

object HikamRepository {
    private const val ASSET = "hikam/al_hikam_verified.json"

    private const val AJIBA_EDITION =
        "Ibn ʿAjība, Īqāẓ al-Himam fī Sharḥ al-Ḥikam, éd./corr. " +
            "Muḥammad ʿAbd al-Qādir Naṣṣār, Dār Jawāmiʿ al-Kalim, Le Caire, 632 p."

    private var cached: List<HikmaEntry>? = null

    private fun verifiedInternalTranslation(note: String) = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = note
    )

    private fun ajibaCommentary(
        sourceNumber: Int,
        page: Int,
        arabicText: String,
        frenchText: String
    ) = HikmaCommentary(
        arabicText = arabicText,
        frenchText = frenchText,
        source = ClassicalSource(
            author = "Ibn ʿAjība",
            workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
            edition = AJIBA_EDITION,
            editor = "Muḥammad ʿAbd al-Qādir Naṣṣār",
            volume = null,
            locator = "Hikma $sourceNumber • p. $page",
            sourceUrl = "https://ablibrary.net/book_content/b/9684/$page",
            translator = "Traduction interne Quran Safeguard"
        ),
        verification = verifiedInternalTranslation(
            "Passage arabe continu vérifié à la p. $page et rattaché à la Hikma $sourceNumber ; traduction interne du même passage."
        ),
        isExcerpt = true,
        textIntegrity = ClassicalTextIntegrity(
            form = ClassicalTextForm.CONTINUOUS_EXCERPT,
            reconstructedOrAssembled = false,
            hasInternalOmissions = false,
            contextChecked = true,
            passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
        )
    )

    private val commentaryByNumber: Map<Int, HikmaCommentary> by lazy {
        mapOf(
            1 to ajibaCommentary(
                sourceNumber = 1,
                page = 27,
                arabicText = "فالاعتماد على النفوس من علامة الشقاء والبؤس ، والاعتماد على الأعمال من عدم التحقق بالزوال ، والاعتماد على الكرامة والأحوال من عدم صحبة الرجال ، والاعتماد على اللّه من تحقق المعرفة باللّه ، وعلامة الاعتماد على اللّه أنه لا ينقص رجاؤه إذا وقع في العصيان ، ولا يزيد رجاؤه إذا صدر منه إحسان .",
                frenchText = "S’appuyer sur soi est un signe de malheur et de misère ; s’appuyer sur les œuvres procède de l’absence de réalisation de leur disparition ; s’appuyer sur les prodiges et les états procède de l’absence de compagnie des hommes de la Voie ; et s’appuyer sur Allah procède de la réalisation de la connaissance d’Allah. Le signe de l’appui sur Allah est que l’espérance ne diminue pas lorsqu’on tombe dans la désobéissance et n’augmente pas lorsqu’une bonne action émane de soi."
            ),
            2 to ajibaCommentary(
                sourceNumber = 2,
                page = 31,
                arabicText = "وأما عند الصوفية فهو على ثلاثة أقسام : تجريد الظاهر فقط أو الباطن فقط أو هما معا . فتجريد الظاهر هو ترك الأسباب الدنيوية وخرق العوائد الجسمانية . والتجريد الباطني هو ترك العلائق النفسانية والعوائق الوهمية . وتجريدهما معا هو ترك العلائق الباطنية والعوائد الجسمانية . أو تقول : تجريد الظاهر هو ترك كل ما يشغل الجوارح عن طاعة اللّه ، وتجريد الباطن هو ترك كل ما يشغل القلب عن الحضور مع اللّه ، وتجريدهما هو إفراد القلب والقالب للّه .",
                frenchText = "Chez les soufis, le dépouillement est de trois sortes : celui de l’extérieur seulement, celui de l’intérieur seulement, ou les deux ensemble. Le dépouillement extérieur consiste à délaisser les causes mondaines et les habitudes corporelles ; le dépouillement intérieur, à délaisser les attaches de l’âme et les obstacles imaginaires ; et les deux ensemble, à délaisser les attaches intérieures et les habitudes corporelles. Autrement dit, dépouiller l’extérieur consiste à écarter tout ce qui détourne les membres de l’obéissance à Allah ; dépouiller l’intérieur, tout ce qui détourne le cœur de la présence avec Allah ; les réunir, c’est vouer à Allah le cœur et le corps."
            ),
            3 to ajibaCommentary(
                sourceNumber = 3,
                page = 35,
                arabicText = "قلت : السوابق جمع سابقة وهي المتقدمة ، والهمم جمع همة والهمة قوة انبعاث القلب في طلب الشيء والاهتمام به ، فإن كان ذلك الأمر رفيعا كمعرفة اللّه وطلب رضاه سميت همة عالية وإن كان أمرا خسيسا كطلب الدنيا وحظوظها سميت همة دنية .",
                frenchText = "J’ai dit : « les élans qui devancent » désignent ce qui prend les devants, et les aspirations sont le pluriel d’aspiration. L’aspiration est la force avec laquelle le cœur s’élance à la recherche d’une chose et s’en préoccupe. Si l’objet est élevé, comme la connaissance d’Allah et la recherche de Son agrément, elle est appelée aspiration élevée ; s’il est vil, comme la recherche de ce bas monde et de ses parts, elle est appelée aspiration basse."
            ),
            4 to ajibaCommentary(
                sourceNumber = 4,
                page = 37,
                arabicText = "التدبير على ثلاثة أقسام : قسم مذموم وقسم مطلوب وقسم مباح . فأما القسم المذموم فهو الذي يصحبه الجزم والتصميم سواء كان دينيا أو دنيويا لما فيه من قلة الأدب ، وما يتعجله لنفسه من التعب إذ ما قام به الحي القيوم عنك لا تقوم به أنت عن نفسك وغالب ما تدبره لنفسك لا تساعده رياح الأقدار ، وتعقبه الهموم والأكدار ، ولذلك قال أحمد بن مسروق : من ترك التدبير فهو في راحة .",
                frenchText = "L’organisation anticipée est de trois sortes : blâmable, requise ou permise. La sorte blâmable est celle qu’accompagnent la certitude arrêtée et la détermination, qu’elle porte sur une affaire religieuse ou mondaine, car elle comporte un manque de convenance et procure à l’âme une fatigue prématurée. Ce dont le Vivant, le Subsistant, S’est chargé pour toi, ne t’en charge pas toi-même ; le plus souvent, les vents des décrets ne secondent pas ce que tu organises pour toi, et les soucis et les troubles s’ensuivent. C’est pourquoi Aḥmad ibn Masrūq a dit : celui qui abandonne cette organisation anticipée est dans le repos."
            ),
            5 to ajibaCommentary(
                sourceNumber = 5,
                page = 39,
                arabicText = "قلت : الاجتهاد في الشيء استفراغ الجهد والطاقة في طلبه ، والتقصير هو التفريط والتضييع والبصيرة ناظر القلب كما أن البصر ناظر القالب ، فالبصيرة لا ترى إلا المعاني والبصر لا يرى إلا المحسوسات ، أو تقول البصيرة لا ترى إلا اللطيف ، والبصر لا يرى إلا الكثيف ، أو تقول البصيرة لا ترى إلا القديم والبصر لا يرى إلا الحادث ، أو تقول البصيرة لا ترى إلا المكون ، والبصر لا يرى إلا الكون ، فإذا أراد اللّه فتح بصيرة العبد أشغله في الظاهر بخدمته وفي الباطن بمحبته ، فكلما عظمت المحبة في الباطن والخدمة في الظاهر قوي نور البصيرة حتى يستولي على البصر ، فيغيب نور البصر في نور البصيرة فلا يرى إلا ما تراه البصيرة من المعاني اللطيفة والأنوار القديمة .",
                frenchText = "J’ai dit : s’efforcer dans une chose, c’est déployer tout son effort et toute son énergie pour la rechercher ; négliger, c’est relâcher et délaisser. La clairvoyance est le regard du cœur, comme la vue est le regard du corps. La clairvoyance ne voit que les significations, tandis que la vue ne voit que les choses sensibles ; ou bien, la clairvoyance ne voit que le subtil et la vue que l’épais ; la clairvoyance ne voit que l’Éternel et la vue ce qui est advenu ; la clairvoyance ne voit que Celui qui donne l’existence et la vue le monde créé. Lorsqu’Allah veut ouvrir la clairvoyance de Son serviteur, Il l’occupe extérieurement à Le servir et intérieurement à L’aimer. Plus l’amour grandit au-dedans et le service au-dehors, plus la lumière de la clairvoyance se fortifie, jusqu’à dominer la vue : la lumière de la vue s’efface alors dans celle de la clairvoyance, qui ne voit plus que les significations subtiles et les lumières anciennes."
            ),
            6 to ajibaCommentary(
                sourceNumber = 6,
                page = 41,
                arabicText = "فإذا تعلق قلبك بحاجة من حوائج الدنيا والآخرة فارجع إلى وعد اللّه واقنع بعلم اللّه ولا تحرص ففي الحرص تعب ومذلة .",
                frenchText = "Lorsque ton cœur s’attache à un besoin de ce monde ou de l’autre, reviens à la promesse d’Allah, contente-toi de la science qu’Allah en a et ne t’acharne pas : l’acharnement porte fatigue et humiliation."
            ),
            7 to ajibaCommentary(
                sourceNumber = 7,
                page = 42,
                arabicText = "التشكيك في الشيء هو التردد في الوقوع وعدمه والوعد هو الإخبار بوقوع الشيء في محله والموعود هو المخبر به والقدح في الشيء هو التنقيص له والغض من مرتبته والبصيرة هي القوة المهيئة لإدراك المعاني والسريرة هي القوة المستعدة لتمكن العلم والمعرفة .",
                frenchText = "Mettre une chose en doute, c’est hésiter entre sa réalisation et sa non-réalisation. La promesse est l’annonce qu’une chose se produira en son lieu, et l’objet promis est ce qui est annoncé. Porter atteinte à une chose, c’est la diminuer et abaisser son rang. La clairvoyance est la faculté préparée à saisir les significations, et le for intérieur est la faculté disposée à l’enracinement de la science et de la connaissance."
            ),
            8 to ajibaCommentary(
                sourceNumber = 8,
                page = 45,
                arabicText = "قلت : إذا تجلى لك الحق تعالى باسمه الجليل أو باسمه القهار وفتح لك منها بابا ووجهة لتعرفه منها ، فاعلم أن اللّه تعالى قد اعتنى بك ، وأراد أن يجتبيك لقربه ويصطفيك لحضرته فالتزم الأدب معه بالرضا والتسليم وقابله بالفرح والسرور ولا تبال بما يفوتك معها من الأعمال البدنية فإنما هي وسيلة للأعمال القلبية فإنه ما فتح هذا الباب إلا وهو يريد أن يرفع بينك وبينه الحجاب .",
                frenchText = "J’ai dit : lorsque le Réel — exalté soit-Il — Se manifeste à toi par Son Nom le Majestueux ou par Son Nom le Dominateur, et t’ouvre par là une porte et une voie afin que tu Le connaisses, sache qu’Allah a pris soin de toi et veut t’attirer vers Sa proximité et te choisir pour Sa présence. Tiens-toi donc envers Lui avec la convenance du contentement et de l’abandon confiant, accueille cela avec joie et allégresse, et ne te soucie pas des œuvres corporelles qui peuvent alors t’échapper : elles ne sont qu’un moyen vers les œuvres du cœur. Il ne t’a ouvert cette porte que parce qu’Il veut lever le voile entre toi et Lui."
            ),
            9 to ajibaCommentary(
                sourceNumber = 9,
                page = 48,
                arabicText = "قلت : قد تنوعت أجناس الأعمال الظاهرة بتنوع الأحوال الباطنة أو تقول أعمال الجوارح تابعة لأحوال القلوب ، فإن ورد على القلب قبض ظهر على الجوارح أثره من السكون ، وإن ورد عليه بسط ظهر على الجوارح أثره من الخفة والحركة ، وإن ورد على القلب زهد وورع ظهر على الجوارح أثره وهو ترك وإحجام .",
                frenchText = "J’ai dit : les genres d’œuvres extérieures se diversifient selon la diversité des états intérieurs ; autrement dit, les œuvres des membres suivent les états des cœurs. Si un resserrement survient dans le cœur, son effet de calme apparaît sur les membres ; si une dilatation y survient, son effet de légèreté et de mouvement apparaît sur eux ; si le détachement et la retenue scrupuleuse surviennent dans le cœur, leur effet apparaît sur les membres sous la forme du renoncement et de l’abstention."
            ),
            10 to ajibaCommentary(
                sourceNumber = 10,
                page = 50,
                arabicText = "قلت : الأعمال كلها أشباح وأجساد وأرواحها وجود الإخلاص فيها فكما لا قيام للأشباح إلا بالأرواح وإلا كانت ميتة ساقطة كذلك لا قيام للأعمال البدنية أو القلبية إلا بوجود الإخلاص فيها وإلا كانت صورا قائمة وأشباحا خاوية لا عبرة بها .",
                frenchText = "J’ai dit : toutes les œuvres sont des formes et des corps, et leurs âmes sont la présence de la sincérité en elles. De même que les formes ne subsistent que par les âmes, faute de quoi elles sont mortes et inertes, les œuvres corporelles ou celles du cœur ne subsistent que par la présence de la sincérité ; sans elle, elles ne sont que des formes dressées et des silhouettes vides, sans valeur."
            ),
            11 to ajibaCommentary(
                sourceNumber = 11,
                page = 52,
                arabicText = "الدفن هو التغطية والستر ، والخمول سقوط المنزلة عند الناس ، ونتاج الشجرة ثمرتها استعير هنا للحكم والمواهب والعلوم التي يجتنيها العبد من المعرفة باللّه ، وذلك عند موت نفسه وحياة روحه . قلت : استر نفسك أيها المريد وادفنها في أرض الخمول حتى تستأنس به وتستحليه ، ويكون عندها أحلى من العسل ويصير الظهور عندها أمر من الحنظل ، فإذا دفنتها في أرض الخمول وامتدت عروقها فيه ، فحينئذ تجني ثمرتها ، ويتم لك نتاجها وهو سر الإخلاص والتحقق بمقام خواص الخواص .",
                frenchText = "Enfouir signifie couvrir et cacher ; l’effacement est la chute du rang que l’on occupe auprès des gens ; et le fruit de l’arbre est ici une image des sagesses, des dons et des sciences que le serviteur recueille de la connaissance d’Allah, lorsque son âme meurt et que son esprit vit. J’ai dit : cache-toi, ô aspirant, et enfouis-toi dans la terre de l’effacement jusqu’à ce qu’elle te devienne familière et agréable, plus douce que le miel, tandis que la mise en vue devient plus amère que la coloquinte. Lorsque tu t’y es enfoui et que tes racines s’y sont étendues, tu peux alors en cueillir le fruit : son résultat s’accomplit pour toi, à savoir le secret de la sincérité et la réalisation de la station des plus particuliers parmi les particuliers."
            ),
            12 to ajibaCommentary(
                sourceNumber = 12,
                page = 58,
                arabicText = "قلت : لا شيء أنفع للقلب من عزلة مصحوبة بفكرة لأن العزلة كالحمية والفكرة كالدواء ، فلا ينفع الدواء من غير حمية ، ولا فائدة في الحمية من غير دواء ، فلا خير في عزلة لا فكرة فيها ولا نهوض لفكرة لا عزلة معها ؛ إذ المقصود من العزلة هو تفرغ القلب ، والمقصود من التفرغ هو جولان القلب واشتغال الفكرة والمقصود من اشتغال الفكرة تحصيل العلم وتمكنه من القلب ، وتمكين العلم باللّه من القلب هو دواؤه وغاية صحته .",
                frenchText = "J’ai dit : rien n’est plus bénéfique au cœur qu’une retraite accompagnée de réflexion, car la retraite est comme une diète et la réflexion comme un remède. Le remède n’est d’aucune utilité sans diète, et la diète sans remède n’apporte aucun bénéfice. Il n’y a donc aucun bien dans une retraite dépourvue de réflexion, ni d’essor pour une réflexion sans retraite : le but de la retraite est de libérer le cœur ; le but de cette disponibilité est le parcours du cœur et l’activité de la réflexion ; le but de cette réflexion est d’acquérir la science et de l’enraciner dans le cœur ; et l’enracinement dans le cœur de la science d’Allah est son remède et l’achèvement de sa santé."
            ),
            13 to ajibaCommentary(
                sourceNumber = 13,
                page = 64,
                arabicText = "« ومنطبعة » أي ثابتة ، وانطبع الشيء في الشيء ظهر أثره فيه ، و « المرآة » بكسر الميم آلة صقيلة ينطبع فيها ما يقابلها ، فكلما قوي صقلها قوي ظهور ما يقابلها فيها ، واستعيرت هنا للبصيرة التي هي عين القلب التي تتجلى فيها الأشياء حسنها وقبيحها . قلت : جعل اللّه سبحانه قلب الإنسان كالمرآة الصقيلة ينطبع فيها كل ما يقابلها وليس لها إلا وجهة واحدة ، فإذا أراد اللّه عنايته بعبد أشغل فكرته بأنوار ملكوته وأسرار جبروته ، ولم يعلق قلبه بمحبة شيء من الأكوان الظلمانية والخيالات الوهمية ، فانطبعت في مرآة قلبه أنوار الإيمان والإحسان ، وأشرقت فيها أقمار التوحيد وشموس العرفان .",
                frenchText = "« Imprimées » signifie ici fixées : lorsqu’une chose s’imprime dans une autre, son empreinte y apparaît. Le miroir est un instrument poli dans lequel s’imprime ce qui lui fait face ; plus son poli est fort, plus ce qui lui fait face y apparaît nettement. Il est employé ici comme image de la clairvoyance, cet œil du cœur où les choses se manifestent dans leur beauté comme dans leur laideur. J’ai dit : Allah — glorifié soit-Il — a fait du cœur humain un miroir poli où s’imprime tout ce qui lui fait face, et il n’a qu’une seule orientation. Lorsqu’Allah veut prendre un serviteur en sollicitude, Il occupe sa pensée aux lumières de Son Royaume et aux secrets de Sa Toute-Puissance ; Il n’attache pas son cœur à l’amour des êtres ténébreux ni aux imaginations illusoires. Alors les lumières de la foi et de l’excellence spirituelle s’impriment dans le miroir de son cœur, où se lèvent les lunes de l’unicité et les soleils de la connaissance."
            )
        )
    }

    @Synchronized
    fun entries(context: Context): List<HikmaEntry> {
        cached?.let { return it }
        val raw = context.assets.open(ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val parsed = parse(raw)
        require(parsed.size == 264) { "Expected exactly 264 verified Hikam, got ${parsed.size}" }
        require(parsed.map { it.sourceNumber }.toSet() == (1..264).toSet()) {
            "Hikam numbering must cover exactly 1..264"
        }
        require(parsed.all { it.displayEligible }) {
            "Every bundled Hikma must satisfy the classical authenticity contract"
        }
        cached = parsed
        return parsed
    }

    fun byId(context: Context, id: String): HikmaEntry? =
        entries(context).firstOrNull { it.canonicalId == id && it.displayEligible }

    fun asDailyReminders(context: Context): List<DailyReminder> =
        entries(context).filter { it.displayEligible }.map { hikma ->
            DailyReminder(
                id = hikma.canonicalId,
                type = ReminderType.HIKAM,
                theme = hikma.theme,
                arabicText = hikma.arabicText,
                frenchText = hikma.frenchText,
                author = hikma.source.author,
                book = hikma.source.workTitle,
                reference = hikma.source.locator,
                authenticity = null,
                tags = hikma.tags
            )
        }

    internal fun parse(raw: String): List<HikmaEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseOne(array.getJSONObject(index)))
            }
        }
    }

    private fun parseOne(obj: JSONObject): HikmaEntry {
        val sourceNumber = obj.getString("source_number").toInt()
        val arabic = obj.getString("arabic").trim()
        val french = obj.getString("french").trim()
        val verificationSources = obj.stringList("verification_sources")
        val translationSources = obj.stringList("translation_sources")
        val themes = obj.stringList("themes").toSet()
        val sourcePage = obj.optString("source_page").trim().takeIf { it.isNotBlank() }

        val sourceVerified =
            obj.optString("verification_status") == "verified" &&
                verificationSources.isNotEmpty()
        val translationVerified =
            obj.optString("translation_status") == "verified" &&
                french.isNotBlank() &&
                translationSources.isNotEmpty()
        val attributionVerified =
            obj.optString("author") == "Ibn Ata Allah al-Iskandari" &&
                obj.optString("text_type") == "author_wisdom"

        val verification = ClassicalVerification(
            sourceVerified = sourceVerified,
            attributionVerified = attributionVerified,
            translationAvailable = translationVerified,
            humanVerified = false,
            rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
            authenticityStatus =
                if (sourceVerified && attributionVerified && translationVerified) {
                    ClassicalAuthenticityStatus.VERIFIED_SOURCE
                } else {
                    ClassicalAuthenticityStatus.NOT_VERIFIED
                },
            verificationNote = obj.optString("verification_notes").ifBlank {
                "Matn arabe et traduction interne contrôlés dans le corpus gelé 1–264."
            }
        )

        val locator = buildString {
            append("Hikma ")
            append(sourceNumber)
            sourcePage?.let {
                append(" • p. ")
                append(it)
            }
        }

        return HikmaEntry(
            canonicalId = "hikma_" + sourceNumber,
            sourceNumber = sourceNumber,
            arabicText = arabic,
            frenchText = french,
            theme = themes.firstOrNull() ?: "spiritual_presence",
            tags = themes.ifEmpty { setOf("Al-Hikam") },
            source = ClassicalSource(
                author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                workTitle = "Al-Hikam al-ʿAṭāʾiyya",
                edition = obj.getString("source_edition"),
                editor = "ʿĀṣim Ibrāhīm al-Kayyālī",
                volume = null,
                locator = locator,
                sourceUrl = verificationSources.firstOrNull().orEmpty(),
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verification,
            commentary = commentaryByNumber[sourceNumber],
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            )
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
