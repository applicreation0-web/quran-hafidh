# Glossaire de travail et design

Les propositions wolof sont destinées à une revue humaine. Le sens d’un terme traditionnel et son adéquation à un protocole de l’application doivent être examinés ensemble.

| Français | English | Wolof à examiner | Base |
|---|---|---|---|
| Révision | Review | Nafar | MEN 2020, p. 29. Dramé, p. 250, dans les daaras |
| Révision active | Recall practice | Nafar gumba | Proposition prioritaire de l’utilisateur. Usage en daara et graphie de l’expression à confirmer |
| Mémoriser | Memorize | Mokkal | UCAD 2021, forme *mokal*, et terminologie des daaras |
| Réciter | Recite | Tari | MEN 2020, p. 28 |
| Renforcement | Reinforcement | Ndëgëral | MEN 2020, p. 29. Vérifier l’emploi pour le protocole Sabqi |
| Répéter | Repeat | Baamtu | MEN 2020, p. 29 |
| Continuer ou reprendre | Continue or resume | Wéyal | MEN 2020, p. 10 |
| Relecture | Read-through review | Njàngaat | MEN 2020, p. 29 |
| Travailler une fragilité | Work on a weak spot | Njoyyanti | MEN 2020, p. 29, remédiation |
| Perfectionnement | Refinement | Gëndel | Diakhaté et Samb, extrait indexé sur le perfectionnement d’un élève |
| Progression traditionnelle dans l’ordre canonique | Canonical-order stage | Ñajji | Diakhaté et Samb, extrait indexé, usage historique |
| Méthode de travail ou parcours | Study method or plan | À valider | Les termes méthodologiques et le programme de séances sont deux libellés distincts |
| Consolidation approfondie, consolidation cumulative, maintien | Intensive consolidation, cumulative consolidation, retention maintenance | À valider séparément | Trois disciplines distinctes |
| Amorces et ancrages | Recall cues and anchors | À valider séparément | Distinguer l’indice de récitation et le repère mémoriel |
| Masquer et dévoiler | Hide and reveal | Nëbb / Wone | Propositions lexicales à valider en situation |
| Validation, acquis, progression et report | Confirmation, confirmed memorization, progress and defer | À valider | Préserver le sens de chaque action et de chaque état |
| Paramètres, sauvegarde et restauration | Settings, backup and restore | À valider | Confirmer des commandes naturelles sur smartphone |

- [Terminologie bilingue du ministère de l’Éducation nationale du Sénégal, 2020](https://senprof.education.sn/PROGRAMME%20LECTURE%20POUR%20TOUS/documentation/Terminologies/Terminologie%20wolof%20fusion.pdf)
- [Techniques d’enseignement dans les daaras traditionnels, UCAD, 2021](https://fastef.ucad.sn/liens/LIEN32/liens32_article14.pdf)
- [Khassim Diakhaté et Moussa Samb, enseignement arabo-islamique au Sénégal](https://dspace.univ-guelma.dz/jspui/bitstream/123456789/14083/1/ARTICLE%2030.pdf), extrait indexé lors de l’étude.
- [Journal officiel du Sénégal, n° 6280, décret d’orthographe wolof, p. 539](https://www.vie-publique.sn/documents/14178/JO-6280-du-27-mai-2006)

## Repères visuels à qualifier

- Conserver le Mushaf QCF V2 à 604 pages, la géométrie, le centrage et la barre d’état.
- Garder les sept commandes de Fragilités accessibles avec des cibles tactiles de 48 dp. Tester l’écran de 320 dp, le dévoilement ciblé et la distinction entre validation et report.
- Tester la palette crème, noir et gris, les grands caractères et les glyphes wolof. Réduire les rafraîchissements sur BOOX.
- La capture synthétique de référence mesure 1 080 × 2 400 px. Le folio couvre 883 × 1 407 px, soit 81,8 % de la largeur et 47,9 % de la surface totale. Le point de contrôle n’a pas de mesure équivalente sur la nouvelle branche.
- [Accessibilité Android](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) · [Langues par application](https://developer.android.com/guide/topics/resources/app-languages) · [Écran e-ink BOOX](https://help.boox.com/41d97/838da/cd623)

## Contrôle des sources

- Le décret fixe la graphie et la séparation des mots. Il ne fixe pas les noms des disciplines de mémorisation.
- La terminologie du MEN atteste le sens pédagogique général de *nafar*, *tari*, *baamtu* et *ndëgëral*. Le passage à une discipline coranique spécialisée reste une décision éditoriale.
- Les entrées de *Ay Baati Wolof* et du dictionnaire de Fal, Santos et Doneux n’ont pas été consultées intégralement. Les catalogues en documentent les références bibliographiques. [Institut français du Sénégal](https://mediatheque.ifs.sn/index.php?id=33889&lvl=notice_display) · [Université de Columbia](https://guides.library.columbia.edu/wolof-language)
- La recherche de l’expression exacte *nafar gumba* n’a pas fourni d’attestation de daara vérifiable. Pour les écrans d’essai, son statut reste « proposition ». Faire distinguer par les relecteurs le titre d’une séance, la récitation sans support et la commande qui masque le folio.
- Les ressources `values-wo` sont des libellés de développement. Revoir en contexte la longueur des commandes et la lisibilité des formes accentuées sur écran étroit.

## Candidats à comparer en situation

Les identifiants de cette grille désignent les concepts éditoriaux. Certaines ressources utilisent un identifiant Android distinct. Les aides en wolof sont des propositions de travail. Le niveau de confiance porte sur le lien entre la forme et le sens de la séance ou de la commande.

| ID | Français / English | Action vérifiable | Formes courtes à comparer | Aide wolof à tester | Indice et état |
|---|---|---|---|---|---|
| study_method | Méthodologie de travail / Study method | Expliquer le déroulement des exercices | Amalinu liggéey · Amalinu mokkal | Ni ngay liggéeye ci njàngum Alxuraan. | Comparer la méthode générale avec la seule mémorisation. Proposition, faible |
| programme | Parcours / Study plan | Voir les séances et objectifs configurés | Nasum njàng · Yoonu njàng | Xool sa jotaayi njàng ak li nga bëgg a jot. | MEN p. 28 et 30, vocabulaire scolaire. Proposition, faible |
| new_learning | Apprentissage / New memorization | Apprendre des lignes nouvelles et effectuer leurs répétitions | Mokkal · Mokkal lu bees | Mokkalal pàcc bu bees. | Dramé p. 256 écrit *mokal* dans le sens de réciter en contexte de mémorisation. Extension au nom de tâche à vérifier. Adaptation à valider, moyenne |
| reinforcement | Renforcement / Reinforcement | Reprendre la matière récente pour la fixer | Ndëgëral · Baamtu li nga jàng | Baamtul pàcc yi nga jàng léegi. | MEN p. 29, nom de renforcement. La paraphrase aide à isoler le travail récent. Adaptation à valider, moyenne |
| deep_consolidation | Consolidation approfondie / Intensive consolidation | Retravailler un passage admissible avec répétitions, masquage et validation | « Degerek li nga mokal » · Dëgërek li nga mokkal · Dëgëral li nga mokkal · Mokkal bu dëgër | Baamtul pàcc bi nga mokkal ba nga man koo tari ci sa xel. | MEN p. 29 confirme *dëgëral*. La formulation orale de l’utilisateur est conservée telle quelle et son éventuelle analyse reste ouverte. Risque de confusion avec *Ndëgëral* et l’apprentissage. Non résolu |
| cumulative_consolidation | Consolidation cumulative / Cumulative consolidation | Joindre les passages déjà travaillés et celui de la dernière séance selon le cycle | Nafar bu boole · Boole pàcc yi | Booleel pàcc yi nga jàngoon ak bi nga jàng tey. | MEN p. 29 emploie *boole* en contexte de révision générale. Dramé p. 257 documente le cumul à Coki. Nom proposé, confiance moyenne sur l’explication |
| snowball | Effet boule de neige / Cumulative review | Retrouver une suite cumulative qui s’élargit au fil des séances | Boole ak yokk · Nafar bu yokku | Nga baamtu li jiitu, nga yokk ci li nga jàng léegi. | Montrer une série de trois séances au relecteur. Proposition, faible |
| retention_maintenance | Maintien de la maîtrise / Retention maintenance | Entretenir durablement les acquis par rotation | Sàmm li nga mokkal · Nafar li nga mokkal | Faral di tari pàcc yi nga mokkal ngir bañ leen a fàtte. | Koki, rubrique « Révision », atteste la pratique en français. Vérifier que *sàmm* évoque le maintien dans le temps. Proposition, moyenne |
| recall_review | Révision active / Recall practice | Réciter de mémoire et traiter les fragilités à la fin | Nafar gumba · Tari te xooloo · Nafar ci sa xel | Tari ci sa xel te doo xool xët wi. Ginnaaw loolu nga liggéey ci pàcc yi la jafe. | Dramé p. 250 atteste *nafar* en daara et MEN p. 29 dans la pédagogie générale. LLACAN, p. 14, donne le sens littéral de *gumba*. *Nafar gumba* reste prioritaire à l’entretien. Vérifier le sens spontané et le registre. Proposition, moyenne pour la paraphrase |
| reading_review | Révision avec le Mushaf / Review with the mushaf | Consulter le texte pendant une révision facultative | Nafar ak xool · Njàngaat | Xoolal ci musaf bi boo dee nafar. | MEN p. 28–29, regarder et relecture. Vérifier la distinction lecture/récitation. Proposition, moyenne |
| recitation | Récitation / Recitation | Réciter le passage sélectionné | Tari | Taril pàcc bi. | MEN p. 28. Adaptation à valider, moyenne |
| repetition | Répétition / Repetition | Réciter à nouveau la même unité | Baamtu | Baamtul aaya bi. | MEN p. 29. Attesté pour l’action générale, confiance élevée |
| resume | Reprise / Resume | Continuer le travail sauvegardé à la même étape | Wéyal | Wéyalal fa nga taxawoon. | MEN p. 10. Comparer avec *defaat* p. 29, qui peut évoquer recommencer. Adaptation à valider, moyenne |
| weak_spots | Fragilités / Weak spots | Retrouver la liste des passages à travailler | Pàcc yu jafe · Fi nga faral di juum | Xoolal pàcc yi la jafe, nga liggéey ci. | Dramé p. 250 atteste *gaqq* pour l’arrêt ponctuel en récitation de mémoire. MEN p. 29 documente la remédiation. La liste des difficultés durables couvre un autre usage. Proposition, moyenne |
| similarities | Similitudes / Similar passages | Comparer deux passages coraniques qui se ressemblent | Aaya yu niroo · Pàcc yu niroo | Mengaleel aaya yi niroo. | *Wagni/wàññi* renvoie à la réduction dans MEN p. 28 et Péncum Wolof. L’entrée « semblable » du [lexique wolof.fr](https://www.wolof.fr/mot/niroo/) appuie *niroo*. Le nom de fonction reste à vérifier. Confiance élevée sur l’écart de sens, faible sur le choix final |
| recall_cue | Amorce / Recall cue | Donner un bref indice au début du passage | Njàlbéenu aaya · Ndimbalu tàmbali | Xoolal baat bu jëkk bi ngir dellu ci tari bi. | Tester en présence de la clé et d’un indice réel. Proposition, faible |
| memory_anchor | Ancrage / Memory anchor | Donner un repère durable dans le passage | Màndarga · Màndargay fàttaliku | Màndarga mi dafay dimbali nga fàttaliku fi ngay tari. | Vérifier la différence avec l’amorce, en contexte. Proposition, faible |
| hide | Masquage / Hide | Masquer l’affichage du folio | Nëbb xët wi | Nëbbal xët wi, nga tari ci sa xel. | Libellé de développement à tester. Proposition, moyenne |
| reveal | Dévoilement ciblé / Reveal a verse | Montrer le verset sélectionné sous le masque | Wone aaya bi · Wone pàcc bi | Tànnal aaya bi nga bëgg a xool. | Distinguer du bouton qui rend visible le folio entier. Proposition, moyenne |
| validate | Validation / Confirm recitation | Enregistrer une récitation terminée si le protocole l’autorise | Dëggal · Tari te amul ndimbal | Dëggal ne taril nga xët wi te amul ndimbal. | Tester le statut et la commande séparément. L’action peut être conditionnée par les aides. Proposition, faible |
| acquired | Acquis / Memorized material | Compter séparément les passages déclarés et les acquisitions vérifiées | Li nga mokkal · Li nga mokkal te dëggal ko | Li nga wax ne mokkal nga ko ak li ñu dëggal, ñaari mbir lañu. | Montrer un passage déclaré et un passage confirmé. Proposition, faible |
| progress | Progression / Progress | Voir l’avancement réel, avec unités séparées | Sa tolluwaay · Li nga def ba léegi | Xoolal fa nga toll ci sa njàng. | Vérifier le sens d’avancement plutôt que de niveau scolaire. Proposition, faible |
| defer | Report / Defer | Laisser une page dans la file pour plus tard | Bàyyi ko ba beneen yoon | Dina des ci li ngay liggéeyaat beneen yoon. | Vérifier que le report ne se comprend pas comme une suppression. Proposition, faible |
| settings | Paramètres / Settings | Changer les options du programme ou de l’interface | Tànneef yi | Fi ngay soppee sa tànneef yi. | Valider le sens numérique avec le dialogue ouvert. Proposition, faible |
| backup | Sauvegarde / Backup | Exporter un fichier de récupération | Denc sa dogal yi · Denc sa liggéey | Dencal sa liggéey ci benn dencukaay ngir man koo jëlaat. | Vérifier que le terme couvre les données et le programme. Proposition, faible |
| restore | Restauration / Restore | Restaurer une sauvegarde sélectionnée | Delloo li nga denc · Jëlaat li nga denc | Tànnal dencukaay bi nga dencoon ngir jëlaat sa liggéey. | Vérifier que l’action a été comprise avant de l’exécuter. Proposition, faible |

### Recoupements et limites

- Dramé, p. 250, documente *nafar* pour revoir ou assimiler une leçon au daara et *gaqq* pour l’arrêt brusque lors d’une récitation de mémoire. Cela appuie le nom d’un événement ponctuel et laisse ouvert le nom de la rubrique des fragilités récurrentes. [UCAD, p. 250](https://fastef.ucad.sn/liens/LIEN32/liens32_article14.pdf#page=4)
- Dramé, p. 256–257, décrit la récitation *mokal* et la récitation quotidienne de plusieurs hizb à Coki. L’institut décrit aujourd’hui mémorisation et révision régulière sur sa page publique. Ces deux sources permettent de comparer les pratiques. Les libellés wolof proposés ci-dessus restent à vérifier avec des pratiquants. [Article intégral UCAD](https://fastef.ucad.sn/liens/LIEN32/liens32_article14.pdf#page=10) · [Daara Koki](https://daarakoki.com/enseignants/enseignement-coranique/)
- La paire *ndëgëral/dëgëral* figure à la même page du MEN, respectivement comme nom et verbe. *Ma dëgëral* apparaît également dans une section d’exercices de lecture du manuel CI, p. 130. Employer les deux seuls comme étiquettes de deux disciplines créerait donc un risque de confusion. [MEN, p. 29](https://senprof.education.sn/PROGRAMME%20LECTURE%20POUR%20TOUS/documentation/Terminologies/Terminologie%20wolof%20fusion.pdf#page=30) · [Njàng, Téere kalaas CI](https://jang-wolof.com/wp-content/uploads/2020/02/T%C3%A9ere-kalaas-CI.pdf)
- Le sens de *wàññi* ressort de deux sources qui l’expliquent par le fait de réduire. Rien dans ces usages ne justifie le choix pour les versets similaires. [MEN, p. 28](https://senprof.education.sn/PROGRAMME%20LECTURE%20POUR%20TOUS/documentation/Terminologies/Terminologie%20wolof%20fusion.pdf#page=29) · [Péncum Wolof](https://pencumwolof.com/waat-yi/wanynyi/)
- La description grammaticale donne le sens lexical de *gumba*. Elle apporte une question de registre pour les entretiens et ne permet pas de décider seule de l’usage métaphorique de la formule. [LLACAN, p. 14](https://www.llacan.cnrs.fr/publications/a12-Wolof.pdf)
- Le texte de Babacar Mbengue (2010) concerne les communautés musulmanes ouest-africaines aux États-Unis. Il a été identifié et interrogé pour les termes recherchés. L’ouvrage de Mamadou Ndiaye a été identifié bibliographiquement, sans lecture du texte intégral. Les fonds IFAN et Boston restent des pistes pour des attestations datées et contextualisées. [Mbengue](https://naaims.org/wp-content/uploads/2021/01/Babacar_Mbengue_-_39FP.pdf) · [IRCICA](https://www.ircica.org/fr/publications/histoire-de-la-civilisation-islamique/afrique/lenseignement-arabo-islamique-au-senegal) · [Boston University](https://www.bu.edu/africa/research/projects/african-ajami/)

### Séance de validation linguistique

Présenter séparément des écrans entièrement en wolof et les scénarios ci-dessous. Faire décrire ce que le locuteur ferait avant de proposer des équivalents FR/EN. Noter sa formulation spontanée, la région et le contexte de formation, son avis sur chaque candidat, puis demander la graphie et une reformulation. Pour une forme orale ambiguë, consigner la prononciation avec le consentement du participant et le découpage qu’il propose.

1. Apprendre aujourd’hui trois lignes nouvelles. Reprendre ces trois lignes le soir. Le lendemain les joindre aux lignes nouvelles. Demander trois noms distincts et ce qui change d’une séance à l’autre.
2. Reprendre un passage déjà mémorisé avec une série intensive, puis entretenir le passage sur plusieurs semaines. Lire « Degerek li nga mokal » tel que proposé et comparer à la reformulation du locuteur. Vérifier qu’il distingue les deux cas du renforcement du jour.
3. Réciter un passage que l’écran cache, puis recommencer en regardant le Mushaf. Demander le contraste de sens entre *Nafar gumba* et les deux paraphrases. Recueillir le registre et les contextes dans lesquels le locuteur emploierait spontanément ces mots.
4. Montrer une erreur de récitation, un blocage ponctuel, une page fragile récurrente et une aide de mémoire. Demander quel libellé désigne chaque cas, y compris lorsqu’une aide empêche la validation.
5. Déclarer un acquis, reporter une page, interrompre une séance, exporter puis restaurer une sauvegarde. Demander au locuteur de prédire l’effet de chaque bouton avant d’y toucher.

Évaluer chaque candidat après les entretiens sur une échelle de 0 à 4 par critère. Pondérations du mandat: fidélité 30 %, naturel 25 %, usage attesté 20 %, compréhension 15 %, concision 10 %. Conserver les notes brutes et les citations autorisées dans le dossier privé. Toute ambiguïté qui peut conduire à la mauvaise discipline ou à une validation accidentelle bloque le choix même si son score est élevé. État actuel de la revue humaine: en attente.

## Formes historiques et usage actuel

L’extrait indexé de Diakhaté et Samb, p. 616, décrit deux étapes successives. *Ñajji* désigne un travail dans l’ordre canonique. *Gëndel* intervient ensuite pour combler les lacunes. Les deux termes sont écartés des libellés par défaut à ce stade, conformément au retour sur leur faible usage dans l’environnement de l’utilisateur. Le même passage mentionne *wanñ* comme technique historique de raccourcis et de signes mémo­riels. Cette forme doit rester distincte de *wàññi*, attesté dans le sens de réduire, et des propositions pour les versets qui se ressemblent. La consultation intégrale du PDF original a échoué. Le dossier consigne la page de l’extrait et la limite de preuve. [Diakhaté et Samb, p. 616](https://dspace.univ-guelma.dz/jspui/bitstream/123456789/14083/1/ARTICLE%2030.pdf)
