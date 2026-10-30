# Quran Haafidh — Recherche éditoriale mutashābihāt (source exclusive al-ʿAbbād)
Date : 2026-10-30. **Recherche documentaire uniquement — pas d'intégration Android, pas d'APK, aucun changement Hifz.**

## Source
ʿAbd al-Muḥsin al-ʿAbbād al-Badr, *آيات متشابهات الألفاظ في القرآن الكريم وكيف التمييز بينها*, Dār al-Faḍīla, 1423 H / 2002.
Texte : https://quranpedia.app/books/b10901/index.html ; chapitres numériques s1 à s14, intégralité de cinq sections ; conclusion hors périmètre.

## Résultats du premier relevé
- **Section V (mêmes débuts, bifurcations)** : 59 couples candidats identifiés ; 26 intra-sourate, 33 inter-sourates — fichier `alabbad_section5_bifurcations.csv`.
- **Sections I–IV (substitution, ajout, omission)** : 232 couples candidats identifiés ; 41 intra-sourate, 191 inter-sourates — fichier `alabbad_sections1-4_pivots_candidates.csv`.
- **Total provisoire : 291 couples**, dont 67 intra et 224 inter. Il ne s'agit pas d'un décompte exhaustif des couples de l'ouvrage et encore moins d'un inventaire exhaustif du Coran.
- Les noms des 114 sourates et les bornes des versets ont été vérifiés pour ces 291 entrées : **0 référence manifestement invalide**. La validité de la paire, du surlignage et du mot pivot n'est **pas** établie par ce seul test.

## Méthode et limites
Extraction automatique de références placées après les citations coraniques du livre ; comparaison des amorces lexicales et du plus long segment commun ; dédoublonnage des paires par leurs références. Aucun texte explicatif de l'auteur n'est reproduit.

**Limites :**
1. Des citations réparties sur plusieurs fragments, citations multiples sous un même extrait et groupes de 3 versets ou plus peuvent échapper à la méthode.
2. Quand les premières paroles diffèrent mais le cœur de l'extrait est commun, `point_de_difference_*` est la première divergence des extraits sélectionnés, pas forcément le mot discriminant le plus utile. **Ne pas utiliser directement ces colonnes comme positions QCF.**
3. Un texte narrativement proche peut se trouver parmi les candidats des sections I–IV : chaque entrée nécessite une décision humaine selon le critère strict (amorce et bifurcation ou mot pivot).
4. La section V montre des débuts communs dans les **extraits cités** ; vérifier le contexte complet si l'extrait commence au milieu d'une āya.
5. La totalité du livre ne peut être déclarée couverte par ces 291 entrées : contrôle éditorial et citations manquantes encore nécessaires.
6. Les pages numériques `sN` ne sont **pas** les pages imprimées de l'édition. L'ouvrage donne par ailleurs une pagination imprimée à conserver lors d'un contrôle final.

## Exception nominative — priorité demandée par l'utilisateur

**Al-Baqarah 2:63 ↔ 2:93**, même sourate, **priorité P0** car cette confusion de récitation est fréquemment rencontrée par l'utilisateur.

- Dix premiers mots identiques jusqu'à **بِقُوَّةٍ** inclus.
- Première divergence au **11e mot** : 2:63 **وَاذْكُرُوا** / 2:93 **وَاسْمَعُوا**.
- Suite utile : 2:63 **مَا فِيهِ لَعَلَّكُمْ تَتَّقُونَ** / 2:93 **قَالُوا سَمِعْنَا وَعَصَيْنَا**.
- Contrôle textuel indépendant : https://quran.com/2/63 et https://quran.com/2/93 .
- **Provenance : ajout explicite de l'utilisateur, vérifié sur les versets ; ne pas l'attribuer à al-ʿAbbād.** Ce cas n'a pas été établi comme une entrée de l'ouvrage.
- Catalogue enrichi : `mutashabihat_292_dont_1_priorite_utilisateur.csv` = 291 candidats du livre + 1 cas utilisateur (68 intra / 224 inter). Le catalogue original de 291 entrées demeure inchangé.
- Pour une future intégration Android, vérifier les indices de mots QCF avant le soulignement, conserver les deux Tafsīr et l'accès depuis Lecture / Révision. **Aucune insertion dans l'APK n'est réalisée par ce commit.**

## Contrat d'intégration futur
Pour chaque paire approuvée : référence sourate/verset du Mushaf (et éventuelle plage), type exact de divergence, **mots de différence réellement vérifiés**, position des mots dans la géométrie QCF, deux lectures Tafsīr al-Jalālayn existantes et accès contextuel depuis Lecture et Révision. Pas d'entrée autonome Atlas, pas d'annotation durant la restitution de révision active, pas de modification des acquis, chronomètres ou sauvegardes.

## Statut
**Brouillon documentaire — NON VALIDÉ POUR L'APPLICATION.**
N'assimiler ni le total de 291 à une liste exhaustive ni les mots pivots extraits à des discriminants audités mot à mot.

## Audit d’admission du 30 octobre 2026

Le relevé provisoire ci-dessus reste un document historique. Le catalogue courant est
`alabbad_catalogue_approved.json`, reconstruit par
`python scripts/build_alabbad_catalogue.py --book-cache /tmp/alabbad`.
Le dossier temporaire contient uniquement les chapitres HTML s1 à s14 téléchargés
sur le site source; ces pages et le commentaire de l’auteur ne sont pas versionnés.
Les décisions explicites sont dans `alabbad_editorial_decisions.json`; les deux
suppléments dans `alabbad_supplementary_decisions.json`.

L’audit courant `alabbad_audit_294.json` / `.csv` donne une décision distincte pour
**chacun des 291 candidats**, le cas utilisateur 2:63/2:93 et deux suppléments
source-confirmés : 2:58/7:161 et 30:37/39:52. Bilan sur ces 294 dossiers :

| Décision | Dossiers |
|---|---:|
| retenu | 199 |
| corrigé (pivot ou référence) | 79 |
| exclu | 6 |
| à arbitrer, absent du catalogue applicatif | 10 |
| admis dans le catalogue | 278 |

Les numéros d’audit 1–291 correspondent exactement au CSV initial. Le numéro 292
est exclusivement attribué à la demande utilisateur. Les suppléments 293 et 294
sont attestés respectivement dans s1, page imprimée 13, et s11, page imprimée 99.
Ils n’ont pas été artificiellement ajoutés au décompte historique des 291.

### Décisions particulières

- 59 et 60 : le relevé avait associé deux exemples de **la même disposition** des
  mots dans chaque groupe; ils ne fournissent pas la variation locale recherchée.
- 119, 162 et 277 : juxtaposition accidentelle de deux groupes indépendants du livre.
- 270 : doublon exact du couple 101; pas de seconde entrée applicative.
- 171 : référence 46:3 de la transcription web corrigée en **46:31**, qui correspond
  effectivement à la citation coranique; première suite distinctive
  وَيُجِرْكُم / وَيُؤَخِّرْكُم après ذُنُوبِكُم.
- 46, 137 et 176 : contrôle de provenance entre chapitres incomplet; quarantaine.
- 62 et 138 : choix du pivot local encore insuffisamment délimité; quarantaine.
- 187 et 188 : texte attribué à 23:23 mêlé à celui de 7:59 dans la transcription.
- 201 : le site reproduit deux citations identiques, alors que 10:60 et 40:61 diffèrent.
- 221 : le site cite le singulier pour 44:38; contrôle complémentaire nécessaire.
- 212 : les préfixes entourent une formule terminale identique; le pivot initial
  proposé automatiquement n’a pas été admis.

### Vérification et limites exactes

Le générateur valide les références numériques, le nombre de mots de chaque verset
admis, les indices à base 1, les mots exacts, l’existence et les bornes de chaque
boîte QCF, et la présence du SVG KFQC de la page du **mot discriminant**. Les
empreintes SHA-256 des sources textuelles, géométriques et SVG sont conservées.
Avec `--book-cache`, les deux références doivent figurer dans le chapitre indiqué;
les pages imprimées sont conservées dans l’audit. Une référence sans crochet
ouvrant (7:161 dans s1) est reconnue sans modifier le texte source.

Le texte local Tanzil contient une basmala préfixée aux ouvertures de sourate,
ainsi que U+200A et U+2060 à l’intérieur de mots. Le générateur traite ces éléments
explicitement. Après ce traitement, 6 235 des 6 236 versets ont le même nombre de
mots que les index QCF. L’exception 15:7 (لو ما / لوما) est bloquée pour l’admission.
Aucun couple admis ne repose sur cette exception.

Les SVG KFQC sont des tracés sans texte de mot lisible par machine. Il serait donc
inexact de présenter cette vérification comme une inspection visuelle indépendante
de chaque glyphe. Les coordonnées sont vérifiées contre le sidecar canonique;
les libellés sont alignés au texte canonique local et les rapprochements contrôlés
sur les citations coraniques du livre. Les libellés exportés conservent l’écriture
Unicode du fichier textuel, qui peut différer de la graphie des tracés QCF.

Un mot après une omission est parfois le premier mot de la suite complète de la
āya, au-delà de l’extrait abrégé du livre (par exemple 4:43/5:6). Il est donné comme
**point de bifurcation dans le verset**, et non comme citation du commentaire.
Aucune justification religieuse ou théologique n’est générée.
