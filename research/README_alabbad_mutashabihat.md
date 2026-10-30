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
