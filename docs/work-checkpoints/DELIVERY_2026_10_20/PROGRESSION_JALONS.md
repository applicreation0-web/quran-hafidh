# Quran Haafidh — frise des jalons, état au 20 octobre 2026

**Contrat utilisateur : aucune remise d'APK tant que toutes les fonctionnalités Atlas, Hifz, Correspondances ne sont pas terminées et testées ensemble.** Aucun APK partiel, aucune publication publique sans accord.

## Frise de livraison à reprendre au prochain jalon

`[J0 ATLAS/HIFZ ✓]──[J1 110 DOSSIERS ✓]──[J2 LECTURE 110 ✓ / CONTRE-AVIS ◐]──[J3 TRIAGE 110 ✓ / CERTIFICATIONS ◐]──[J4 CATALOGUE/QCF ○]──[J5 TESTS/LIVRAISON ○]`

| Jalon | État vérifié | Preuve ou blocage | Action suivante |
|---|---|---|---|
| J0 Socle Atlas/Hifz | **ACQUIS** | Atlas 366+1 053 titres verrouillés, 1 419 correspondances approuvées ; code applicatif 26f7f619, 3 CI réussies dont 108 tests Android synthétiques. BOOX physique non testé. | Ne pas modifier les acquis ; tests globaux en J5. |
| J1 Inventaire documentaire | **ACQUIS** | 110/110 IDs ; 80 retenues/documentées, 24 en attente, 6 écartées, références originales préservées. | Distinguer admission et certification. |
| J2 Lectures et audit contradictoire | **COUVERTURE DOCUMENTAIRE 110/110 ; CONTRE-AVIS INDÉPENDANT EN COURS** | 15 revues ciblées historiques + 39 approfondies + 56 courtes ; chaque dossier a une objection. Jalalayn 734 locateurs recollés ; 1 157 occurrences de versets et 9 693 positions de mots recomptées sur les quatre lots de l'ancien APK, hashes conformes ; aucune divergence statique. | Ne pas confondre recollation et vérification indépendante ; obtenir des preuves spécifiques et contradicteurs externes avant certification. |
| J3 Certification par dossier | **EN COURS — TRIAGE 110/110, REGISTRE CRITIQUE RÉCONCILIÉ** | 110/110 dossiers ont reçu un contre-test documentaire/lexical gradué (46 deux témoins arabes, 13 progressions, 24 ressemblances, 27 structures ou types ouverts). **Source supplémentaire vérifiée dans l’original arabe : al-Suyūṭī, *Marāṣid al-Maṭāliʿ*, p. 51/54/69/70/73/75/83 : sept attestations ciblées R038/R049/R051/R057/R079/R085/R089 ; contre-épreuve de bornes R084 (64:4 vs 64:1).** 4 certifications historiques, 0 nouvelle ; 0 production. | Valider individuellement la catégorie exacte et les frontières ; indépendance savante/droits/QCF/QA avant activation. |
| J4 Catalogue utilisable et QCF | **À TERMINER** | Architecture Android existante ; 0 relation autorisée en production, ancrages focaux et QA native non certifiés. | Catalogue approuvé, 8 types/icônes, vrais foyers QCF, fade-away, restauration sans régression. |
| J5 QA globale puis APK complet | **À TERMINER** | 108 tests Android sur corpus public/synthétique ; aucune recette BOOX de l'application complète. | Test intégré privé, BOOX, audits et corrections ; un seul APK complet à soumettre après validation. |

**Règle de mesure : jamais de pourcentage estimé ; utiliser les états fondés sur preuves et les vrais compteurs.** Mise à jour de ce fichier et de la frise interactive HTML après chaque jalon terminé, non en arrière-plan. Le ZIP privé contenant le détail des 110 dossiers ne doit pas être versé dans GitHub public.

## Contre-expertise ciblée : état des preuves (non-certificateur)

- **R011 :** 17:111 / 18:1, répétition partielle de formule de louange ; ni exclusivité ni grande structure entre sourates démontrée.
- **R021 :** al-Suyūṭī compare 55/56 sur une structure plus large que la plage 55:37–78 retenue initialement : attribution savante attestée mais focus et limites à distinguer.
- **R026 :** inversion jour/nuit locale 91:3–4 / 92:1–2 ; 91:1–2 = contexte, pas un chiasme global.
- **R032 :** Farrin décrit sept unités concentriques dans toute al-Baqarah, 2:142–152 = centre D ; pas symétrie interne à ces onze versets.
- **R033 :** Farrin propose neuf membres dans 2:255 et un centre lié à la connaissance divine ; géométrie mot QCF à démontrer avant toute hachure.
- **R038 :** 18:1 ↔ 18:109–110, écho thématique possible ; *inclusio* lexicale insuffisamment prouvée : ne pas certifier sous ce type.
- **R078 :** 109:2–5, alternance textuelle je/vous, 109:3 et 109:5 identiques ; nuance de 109:2/109:4.

### Références directes, non épuisement du corpus

- Farrin (2014), entretien d'auteur : https://en.alukah.net/library/2/4842
- Farrin (2010), `Surat al-Baqara: A Structural Analysis` : https://onlinelibrary.wiley.com/doi/10.1111/j.1478-1913.2009.01299.x
- al-Suyūṭī, `Asrār Tartīb al-Qurʾān`, passage traduit à propos de la sourate 56 : https://www.imamghazali.org/blog/tafsir-of-surah-al-waqiah-verses-1-26
- Texte coranique 109 : https://quran.com/al-kafirun ; 18 : https://quran.com/al-kahf

**Aucune attestation bibliographique générale n'est convertie en preuve par ID automatiquement.** La grille documentaire et les corrections restent hors de l'application jusqu'à certification individuelle. Les ressources privées demeurent hors GitHub.

## Mise à jour probante — 20 octobre, J2/J3

- Sources de contrôle : *registres privés conservés hors GitHub*, base Jalalayn de l'ancien APK signé, quatre JSON `assets/reader109/word-boxes` dont SHA-256 revérifiés, et deux lots de corroborations externes.
- 110/110 dossiers de J2 couverts, dont **56 lectures courtes** ; clôture de la **couverture locale**, **pas** d'un audit contradictoire humain ou intermodèle réellement indépendant.
- **14 dossiers** font l'objet d'un recoupement externe ciblé : R028, R031, R040, R048, R057, R059, R071, R058, R068, R069, R073, R075, R078, R083. Aucun passage automatique de « documentée » à « certifiée ».
- **R071** : 94:5 diffère de 94:6 par `فَ` initial ; seule la proposition commune constitue une répétition exacte.
- **R078** : 109:3 et 109:5 sont répétés ; 109:2 et 109:4 diffèrent. L'alternance des positions énonciatives reste défendable, pas une répétition intégrale des 4 versets.
- **R083** : 62:10 permet la dispersion après la prière ; 62:11 réprouve la dispersion de l'assemblée avant sa clôture. Éviter une fausse opposition entre dispersion en soi et prière.
- **R033** : les rectangles des 50 mots présents en page 42 sont confirmés par les références statiques, avec segmentation 9 membres ; cela ne prouve ni la structure par expertise scientifique indépendante ni la réussite du surlignage hachuré sur Android/BOOX.
- Tout contrôle du contenu privé reste **hors dépôt**, dépôt uniquement pour les compteurs, corrections de méthode et sources publiques.
- ZIP privé produit pour transmission : `QURAN_HAAFIDH_CORRESPONDANCES_J3_14_ET_FRISE_2026-10-20.zip`; SHA-256 `7531014070e1e6059c318e13987dd826d8f657df93694246963f2864be56087d`. Frise interactive locale J3_14.html, table 110 IDs, sources et manifest.
- Règle absolue : **0 relation en production, 0 certification nouvelle, 0 APK transmis**. Atlas et Hifz inchangés.

## Reprise effective du 20 octobre 2026 — consolidation J3 sans activation

- HEAD de départ vérifié : `c6950feb8fcd716f268bb0b85b27f9db8733fb21` (comparaison branche identique).
- Contrôle local lecture seule des deux registres privés de 110 dossiers : 110 identifiants uniques, plages originales identiques dans les deux archives, aucune approbation de production; 80 documentées, 24 en attente, six écartées.
- Les sept verdicts du rapport Work sont repris comme **contre-lecture ciblée**, non comme certification générale : R011/R033 validés dans une acception littéraire limitée; R021/R026/R032/R078 à corriger; R038 formellement non démontré comme inclusio.
- Al-Suyūṭī, *Marāṣid al-Maṭāliʿ*, éd. al-ʿAskar, Dār al-Minhāj, 1426 H/2005 : sept attestations thématiques ciblées (R038 p. 54, R049 p. 69, R051 p. 70, R057 p. 75, R079 p. 51, R085 p. 73, R089 p. 83). R084 p. 72 cite 64:4/64:18, **pas** les bornes historiques 64:1/64:18.
- Relectures textuelles des 14 répétitions déjà consignées dans les dossiers privés : conservation de leurs nuances entre versets identiques et formules partielles; la présence de deux corpus électroniques ne vaut pas seconde expertise scientifique externe.
- Artefacts **privés conservés hors de ce dépôt public** : registre non certifiant par 110 fiches, CSV de suivi, rapport, manifeste SHA-256 et frise HTML interactive. Aucun texte, actif, secret ou registre privé incorporé au commit.
- Audit technique J4 sur le code existant : `CorrespondenceRuntime.APPROVED_SHA256` est vide ; le catalogue est **fail-closed**, toutes les relations restent absentes en production. Les contrats d'ancres, de géométrie et des huit types existent déjà mais ne permettent aucune activation sans certification, droits, rendu et QA réels.
- J4 intégration des données finales, J5 Android/BOOX signé : **NON EXÉCUTÉS dans cette reprise**. Aucun APK livré ; aucun moteur Hifz ni titre Atlas modifié. Ne pas interpréter ce checkpoint de preuve comme une certification finale.

Frise actuelle : `[J0 ✓]──[J1 ✓]──[J2 ◐]──[J3 ◐: 110 réconciliés, certification ouverte]──[J4 ○]──[J5 ○]`.

## J4/J5 — contrôle préparatoire après consolidation, 20 octobre 2026

- **Test local exécuté** contre le registre privé de 110 : 110 IDs uniques, ventilation 80/24/6, quatre certifications éditoriales historiques R030/R037/R047/R048 et drapeaux de production bloqués 110/110 : **PASS**. Test négatif (production activée artificiellement) : **REJET CORRECT**. Script et résultat conservés uniquement dans le pack privé, pas dans le dépôt public.
- **Revue statique J4** : `CorrespondenceCatalog` vérifie SHA-256, schéma, gates et droits ; `CorrespondenceRuntime` conserve le hash approuvé vide (aucune activation). Contrôles de géométrie ayah/mot, correspondance directe, RESTAURATION, contexte Atlas/Lecture/Sabqi, WebView QCF hors ligne et activité non exportée inspectés, sans garantie d'exécution native.
- **Non-régression par Git** : depuis le dernier SHA applicatif `26f7f619c11c855424786e0380463fc33850db5e`, la comparaison avec la branche affiche seulement des fichiers de documentation. **Aucun changement au code**, pas de tests Android nouveaux.
- **J4 encore bloqué** par certification individuelle, droits, foyers QCF et portes de QA non attestées. **J5 non clos** : Gradle/Android/BOOX physique, restauration installée, signature compatible et APK final ne sont pas validés.
- Pack privé de cette reprise : `QURAN_HAAFIDH_J3_CONSOLIDATION_PRIVEE_ET_FRISE_2026-10-20.zip`, SHA-256 `fb94bfde39e4ae16698754883ef65d36dd7049c6bce9ae816a1f651a289b11c7`. Ce ZIP reste hors dépôt public.

## J4 — correspondance type/icône et pré-assemblage vérifiés (20 octobre 2026)

- J3 **typologie seulement** : 110 références originales préservées ; 80 documentées et typées, 14 typages provisoires sur 24 en attente, 10 non classés, six écartés. Typé != certifié. Les N001–N008 restent hors périmètre.
- Test de rapprochement entre le registre historique et les 110 lignes de J3 : **110/110 bornes exactes**, sans modification des IDs ni des limites ; 80 dossiers documentés répartis en **69 `TWO_PAGES` / 9 `SERIES` / 2 `INTERNAL`**, sans cardinalité actuellement incompatible.
- Les huit SVG/Android drawables contractuels et les associations Java déjà présentes ont été vérifiés. Nouveau contrôle de régression public uniquement, sans contenu privé : `scripts/test_correspondence_type_icon_wiring.py` (SHA de création `947cf4fb6cc1475e65d7fd0d1614e4a7bf727e79`). Son exécution CI sur Android reste **à faire** ; ne pas annoncer de PASS Android.
- **Cinq contrôles Python privés J4 exécutés et PASS** : comptage, préservation des bornes, 8 correspondances de type/icône, cas litigieux, schéma non activable. Pack privé : `QURAN_HAAFIDH_J4_PREPARATION_110_MODES_ICONES_SANS_ACTIVATION_2026-10-20.zip`, SHA-256 `a38c32e64f5e115f53182814dbb7b08152549d0a0e33276f5a4c194d11c278a6`. Conserver ce pack **hors dépôt public**.
- Écarts de rendu identifiés et non masqués : R033 centre sur mots QCF et preuve focale ; R032 centre à l'échelle de toute la sourate ; R078 quatre positions alternées ; R026 sous-plage jour/nuit ; R038 inclusio formelle non démontrée.
- **J4 non terminé** : cette grille est un *staging documentaire*, pas `approved_v1.json`. Source, droits, foyers visuels et QA native ne sont pas validés en masse. `APPROVED_SHA256` demeure vide. **0 relation activée, 0 APK livré, Hifz/Atlas inchangés.**

## J3 — arbitrage de typologie ciblé sur les 24 en attente (20 octobre 2026)

- Mission limitée aux **huit types de relations**, pas aux droits ni à l'autorisation Android. Les 110 identifiants et plages historiques ont été conservés.
- Lecture critique des 24 dossiers à partir des précédents contrôles textuels, bibliographies et objections existants : **7 types établis sur les preuves déjà réunies, 8 types sous réserve de correction de foyers/périmètre, 9 relations non suffisamment démontrées pour leur attribuer un type.**
- Le total documentaire des fiches **avec type retenu ou proposé devient 95/110** : 80 documentées déjà typées, 15 des 24 en attente avec type; neuf sans type et six écartées. Les compteurs historiques **80 documentées / 24 en attente / 6 écartées** restent inchangés; ce n'est **pas** une nouvelle certification scientifique ou un nombre de relations approuvées pour la production.
- Correction exemplaire : une ressemblance entre ouverture et clôture ne doit pas être automatiquement nommée inclusio formelle; un centre attesté au niveau de toute une sourate ne doit pas être présenté comme centre interne d'une plage plus courte.
- Tableau par ID, justification, type, huit icônes et actions ciblées consignés dans un **pack privé hors GitHub public**, `QURAN_HAAFIDH_J3_ARBITRAGE_CIBLE_24_2026-10-20.zip` (SHA-256 `3cc0924fcb41b3b7d9035bdf13eaa706199e8fdb69b03c35dd09650e4992255e`).
- Aucun catalogue `approved_v1.json` ni aucune relation activés. Les huit icônes et Hifz/Atlas inchangés. Les ajustements de focalisation et la recette Android/BOOX sont toujours en J4/J5.

## J3 — contre-épreuve ciblée des huit corrections avec fondement contrôlable (20 octobre 2026)

- Scope **typologie seule** (huit catégories), sans réouverture générale des 110, sans écraser les références historiques et sans activation Android.
- Sources nouvellement retrouvées directement dans les textes arabes d’al-Suyūṭī, *Asrār tartīb al-Qurʾān* : **R009**, section « Yūsuf », p. 109 de l'édition **en ligne** d’Islamweb, et **R012**, section « Ṭā-Hā », p. 117 du même site. Attention : cette pagination n'est **pas** attribuée à une édition imprimée non contrôlée. Sources : https://www.islamweb.net/ar/library/content/223/13/ et https://www.islamweb.net/ar/library/content/223/21/.
- **R012** : l’auteur caractérise le récit comme mention concise dans Maryam puis expansion dans Ṭā-Hā ; proposition documentée de type **PROGRESSION (développement narratif)** au lieu de RESSEMBLANCE générique. **19:52**, pertinent pour l’appel, est hors de la borne historique **19:51**. Pas de changement des références ni du catalogue ; traitement visuel distinct.
- **R021** : extrait traduit publié d’al-Suyūṭī (https://www.imamghazali.org/blog/tafsir-of-surah-al-waqiah-verses-1-26) atteste le lien 55:37/56:1 et une inversion à l’échelle globale des sourates ; **RESSEMBLANCE** est maintenu sur les bornes historiques restreintes.
- **R032** : Farrin (2010), *The Muslim World* 100(1), p.17–32, attribue le centre de la macrostructure à 2:142–152 ; comparaison contradictoire des frontières chez Klar, *Text-Critical Approaches to Sura Structure*, tableau 2 p.7. **CENTRE** seulement au niveau de la sourate entière, pas une symétrie automatique interne aux onze versets.
- **R065, R067, R103, R105** : les types existants sont préservés après relecture des marqueurs arabes. Foyers distincts des deux plages d’origine : 85:10/85:11 ; 89:23–26/27–30 ; 63:7/63:8 ; opposition interne 72:14–15. Les foyers tous inclus dans la seconde plage ou hors de la première ne doivent **pas** être rendus sous un mode deux pages trompeur.
- Tableau détaillé de huit décisions, liens exacts, preuves, réserves et bornes conservé dans le pack privé **hors dépôt public** `QURAN_HAAFIDH_J3_8_CORRECTIONS_SOURCES_VERIFIEES_2026-10-20.zip`, SHA-256 `44f5eb0321133ba32daf4fd24e97103cb803b991c7a665fab12818e5a31b9dfa`.
- Ce complément ne vaut pas certification de l'affichage ou des droits. `APPROVED_SHA256` reste vide; aucun `approved_v1.json` installé; **0 activation, 0 APK**, Atlas/Hifz inchangés.

## J4 — implémentation Remembering QCF vérifiée (20 octobre 2026)

**Périmètre :** 8 icônes contractuelles, accès QCF en lecture seule, fenêtre d'occurrences maximale de cinq points, 31 refrains 55:13…77 et quatre refrains 54:17/22/32/40. Les paroles/QCF sont prises dans le corpus existant, sans redessiner le mushaf. Les mots repères sont des étiquettes mémorielles, non des titres de tafsīr.

- J4 **correction fonctionnelle** : le rendu SVG disposait déjà du foyer refrain grisé et des **deux versets voisins normaux**, mais `CorrespondenceModel.Navigation.viewport` ne permettait pas toujours de feuilletter la page du voisin précédent. Corrigé par deux voisins dans le viewport, sans changer aucun ID, plage ou ancre historiques. Tests JVM ajoutés pour précédent/refrain/suivant sur deux folios et bornes de sourate.
- Les étiquettes réelles d'**Ar-Raḥmān** (31 jalons, deux mots arabes adjacents + contexte) et d'**Al-Qamar** (quatre transitions narratives) sont optionnelles, sans quiz, nouveau score, ni écriture dans Hifz. Le rail dessine au plus cinq points et double-cerclage actif.
- Protection des moteurs : deux audits indépendants ont des listes fermées de fichiers autorisés. Uniquement `CorrespondenceRefrainCue.java` a été ajouté à chacune de leurs listes **UI/documentaire**. Aucune exception générique ou désactivation de protection.
- **Preuve CI verte :** [J4 QCF Refrain QA #39459122128](https://github.com/applicreation0-web/quran-hafidh/actions/runs/39459122128). Jobs `Eight icons and read-only UI` **SUCCESS** et `Refrain neighbors and QCF geometry` **SUCCESS**, Gradle `:hifz-app:testDebugUnitTest :hifz-app:sourceContractTest` **BUILD SUCCESSFUL**. Ce sont des tests JVM et contrats sources, PAS la recette physique BOOX.
- Vérification des références documentaires : R047 = 54:17,22,32,40 ; R048 = les 31 positions 55:13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77. Bornes privées inchangées, aucune transcription arabe mise dans le dépôt public.
- **Non clôturé en production :** `CorrespondenceRuntime.APPROVED_SHA256` vide, `approved_v1.json` non autorisé, aucune relation active. Les relations et leurs droits/foyers exacts doivent franchir les portes du catalogue. R032 macro-centre et les autres relations litigieuses nécessitent encore des preuves/rendus. Recette Android instrumentée/BOOX et APK complet **pas encore validés**.
- Source d'audit native Hifz indépendante : workflow `Hifz native emulator regression` ; distinguer son verdict d'une installation BOOX réelle.

## J4 — reprise après pause et contrôle Android (20 octobre 2026)

- **J4 refrains, implémentation fonctionnelle terminée** : Ar-Raḥmān 31 occurrences / frise coulissante de cinq points / repères voisins ; Al-Qamar quatre occurrences ; QCF réel avec voisin précédent et suivant normaux et refrain grisé ; passage de pages QCF autorisé sans modifier les ancrages historiques.
- **Contrôles JVM et contrat UI/icônes PASS** sur `0e1d0798d20faaa9aef4887bba2f2d103c65ac8e` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39459122128
- **Contrôles instrumentés Android sur émulateur PASS** sur `feb044b843621d016d18770464dcbb4365fcd6cc` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39460310500 ; journaux : **108 tests Android**, redémarrage forcé, 32 captures ; tests incluent navigation Correspondances synthétiques, QCF, retour Atlas et préservation état Hifz. Ce **n'est PAS** un contrôle BOOX physique ni une activation du catalogue de production.
- **Bloqueurs restant hors du code de refrains** : catalogue individuel `approved_v1.json` et SHA de certification (actuellement vide), droits et foyers exacts de chaque relation, structures complexes (notamment R032), BOOX physique/signature privée de mise à jour. Pas de release publique ou privée finale sans ces portes.
- **Statut honnête : J4 fonctionnalité Refrains = DONE et automatisation = PASS ; livraison globale Correspondances / APK privé = NON LIVRÉE**.
