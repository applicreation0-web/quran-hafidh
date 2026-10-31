# Quran Haafidh 1.17.9 — Audit des mots discriminants V2 (sans mutation runtime)

**Statut : revue éditoriale et technique, NON intégrée à l'APK, aucun changement Hifz.**

- Branche dérivée : `work/haafidh-1179-pivots-review-20261031`
- Base exacte Work : `326c64ace61762ae226fa812cf286966d95c5929`
- Rapport détaillé : `research/mutash_pivot_review_v2_20261031.json`
- Entrée : `research/alabbad_audit_294.json` + `scripts/data/quran-uthmani-tanzil.txt`
- Périmètre conservé : **294 dossiers**, dont **278 admis**, **10 en attente**, **6 exclus**.
- Contrôle textuel : les 278 jeux d'indices actuels concordent avec le texte coranique local, après exclusion correcte des ornements de page (`۞`) du comptage des mots.
- Le fichier de revue **ne modifie pas** `research/alabbad_catalogue_approved.json`, `AtlasDiscriminantCases.java`, le schéma Hifz ou les sauvegardes.

## Principe éditorial — priorité à l'erreur possible à l'oral

Le premier caractère ou le premier mot différent **n'est pas nécessairement** le meilleur repère de récitation.

1. **Bifurcation APRÈS des mots identiques :** choisir le premier changement réellement audible après l'amorce commune; ajouter au plus les mots indispensables au sens.
2. **Variation AVANT une suite identique :** retenir la variation précédant le tronc commun, même lorsqu'un algorithme préfère une fin de verset identique plus longue.
3. **Ajout/omission :** ne jamais présenter le premier mot commun suivant comme un mot substitué. Représenter « présent / absent », ancré à la bonne position QCF.
4. **Inversion :** montrer dans l'ordre les deux verbes ou expressions, sans réduire artificiellement l'inversion à un seul élément.
5. **Formes grammaticales/phonétiques :** différencier un contraste de graphie d'une différence perceptible pendant la récitation. Une graphie seule ne suffit pas automatiquement.
6. **Plusieurs bifurcations indépendantes :** autoriser deux petits repères si une seule paire en contient deux, plutôt que masquer une seconde erreur probable.
7. **Traduction française :** glosses courtes, relues, sur les mots réellement choisis. Le tafsīr al-Jalālayn conserve sa source distincte et ne s'ouvre qu'à la demande.

Ne pas générer de commentaire religieux automatique. Un pivot n'est admis qu'après contrôle **des mots source, du contexte, de la référence du livre, des coordonnées des mots QCF et du rendu Android**.

## Dix dossiers laissés en attente : orientation V2, non encore approuvée

| N° | Versets | Repère arabe V2 (source authentique) | Risque principal |
|---|---|---|---|
| 46 | 40:85 ↔ 48:23 | `فِي عِبَادِهِ` / `مِن قَبْلُ` | Ne pas choisir seulement `سُنَّتَ` / `سُنَّةَ` : la graphie n'isole pas une divergence orale nette dans l'enchaînement. Provenance transpage à finaliser. |
| 62 | 13:18 ↔ 39:47 | `أُولَٰئِكَ` / `مِن سُوءِ الْعَذَابِ` | Suite différente après huit mots communs; `مِن` seul est trop générique. |
| 137 | 8:41 ↔ 59:7 | `إِن كُنتُمْ` / `كَيْ لَا` | Bifurcation après sept mots communs; seconde citation à attribuer à la bonne page. |
| 138 | 8:53 ↔ 13:11 | `وَأَنَّ` / `وَإِذَا` | Après `حَتَّىٰ يُغَيِّرُوا مَا بِأَنْفُسِهِمْ`, deux continuations distinctes. |
| 176 | 35:25 ↔ 22:42 | `كَذَّبَ` / `كَذَّبَتْ` | Terminaison distincte après trois mots communs; vérifier provenance. |
| 187 | 11:25 ↔ 23:23 | `إِنِّي` / `فَقَالَ` | La transcription numérique du livre mélange une autre citation; le pivot est après cinq mots communs. |
| 188 | 23:23 ↔ 29:14 | `فَقَالَ` / `فَلَبِثَ` | Même problème de source transcrite, mais divergence du texte coranique explicite. |
| 201 | 10:60 ↔ 40:61 | `أَكْثَرَهُمْ` / `أَكْثَرَ النَّاسِ` | Sept mots communs, puis `eux` / `les gens`. |
| 212 | 39:52 ↔ 42:12 | `إِنَّ فِي ذَٰلِكَ` / `إِنَّهُ بِكُلِّ شَيْءٍ عَلِيمٌ` | Après cinq mots communs; souligner la première bifurcation, mais conserver assez de contexte pour une traduction intelligible. |
| 221 | 21:16 ↔ 44:38 | `السَّمَاءَ` / `السَّمَاوَاتِ` | Singulier / pluriel **avant** la fin commune. |

Les positions de mots proposées, par référence coranique et comptage 1-based, figurent dans le JSON. **Aucune des dix entrées n'est promue automatiquement** : la provenance de l'ouvrage et les glosses françaises sont encore à approuver.

## Douze cas ADMIS à revoir en priorité

Cette liste résulte d'un diagnostic d'alignement, **pas** d'un jugement automatique d'erreur : certains choix sont corrects et doivent rester inchangés.

| N° | Versets | Recommandation |
|---|---|---|
| 120 | 4:124 ↔ 16:97 | Conserver `يَعْمَلْ` / `عَمِلَ` et proposer un second repère `فَأُولَٰئِكَ` / `فَلَنُحْيِيَنَّهُ` après la formule commune. |
| 124 | 6:5 ↔ 26:6 | Opposer `فَسَوْفَ يَأْتِيهِمْ` à `فَسَيَأْتِيهِمْ` ; pas seulement les premiers morceaux. |
| 142 | 11:60 ↔ 11:99 | `الدُّنْيَا` présent / absent ; `لَعْنَة` n'est pas remplacé. |
| 145 | 39:72 ↔ 40:76 | `قِيلَ` présent / absent ; `ادْخُلُوا` est commun. |
| 147 | 22:5 ↔ 40:67 | `مِن مُّضْغَةٍ` / `يُخْرِجُكُمْ` après la formule commune; conserver la préposition. |
| 149 | 22:62 ↔ 31:30 | `هُوَ` présent / absent ; `الْبَاطِلُ` est commun. |
| 161 | 2:174 ↔ 3:77 | `وَلَا يَنْظُرُ إِلَيْهِمْ` inséré seulement en 3:77 avant `يَوْمَ الْقِيَامَةِ`. |
| 185 | 7:54 ↔ 25:59 | `وَمَا بَيْنَهُمَا` inséré seulement en 25:59 ; `وَالْأَرْضَ` est commun. |
| 197 | 8:31 ↔ 10:15 | **Conserver** `قَالُوا` / `بَيِّنَات` : la suggestion automatique n'est pas meilleure. |
| 198 | 9:87 ↔ 9:93 | **Conserver** `وَطُبِعَ` / `وَطَبَعَ اللَّهُ` : passif / actif, différence de lecture importante. |
| 199 | 9:94 ↔ 9:105 | Deux repères possibles : ajout de `وَالْمُؤْمِنُونَ`, puis `ثُمَّ تُرَدُّونَ` / `وَسَتُرَدُّونَ`. |
| 293 | 2:58 ↔ 7:161 | **Conserver** l'inversion `ادْخُلُوا / قُولُوا` ; ne pas la remplacer par le premier mot non aligné. |

### Insertion et omission — verrou technique obligatoire

Le modèle actuel de l'application exige un mot QCF de chaque côté. Il ne doit pas remplacer une absence par le prochain mot commun. Pour les cas 142, 145, 149, 161 et 185 :

- Autoriser une **absence explicite** d'un côté dans les données d'affichage.
- Garder un point d'ancrage QCF vérifié sur l'expression commune, mais **ne pas souligner l'ancre comme si elle était différente**.
- Montrer une étiquette sobre `absent` côté manquant, avec sa localisation contextuelle.
- Garder les pages QCF authentiques et les deux Tafsīr accessibles sur sélection.
- Faire échouer le contrôle qualité si un ajout/une omission est encodé comme deux substitutions lexicales fausses.

### Industrialisation sans régression

1. Ajouter à la donnée éditoriale **type de contraste** (`substitution / ajout / omission / inversion / bifurcation / deux pivots`) et **indice QCF de chaque mot réellement distinct**.
2. Proposer les alignements automatiquement mais **ne jamais les promouvoir automatiquement** : certains choix pertinents précèdent une longue suite commune.
3. Relire les traductions françaises courtes. Un mot fonctionnel isolé (`مِن`, `إِنَّ`) exige parfois un groupe minimal supplémentaire.
4. Préserver les décisions originales et régénérer la Java runtime uniquement après audit humain des pivots concernés.
5. Tester en JVM les références, indices, groupes vides autorisés uniquement pour les vraies omissions et l'absence de faux surlignage; tester ensuite le rendu QCF, l'ouverture du Tafsīr et les révisions sur émulateurs BOOX/téléphone.
6. **Ne pas modifier le Hifz ni produire de release** avant validation indépendante de toutes les portes 1.17.8 → 1.17.9.

Les rapprochements lexicaux automatiques de ce dossier sont des **outils de diagnostic**, pas une nouvelle édition coranique ni une preuve de qualité mémorielle.