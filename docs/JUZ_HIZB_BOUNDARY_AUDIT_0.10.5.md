# Audit Juz / Hizb — Quran Safeguard 0.10.5

## Verdict

La structure canonique embarquée est correcte, mais deux écarts fonctionnels existent dans la construction des plans de lecture.

## Sources de contrôle

- Tanzil Quran Metadata 1.0 (`quran-data.xml`) pour les 30 débuts de juz et les 240 quarts de hizb.
- Pagination Madinah 604 pages (`pages.json`) pour le placement des bornes dans les pages.
- Le code de Quran Safeguard (`QuranStructureMetadata.kt`, `QuranPageSelector.kt`, `SafeguardCyclePrefs.kt`).

## 1. Bornes canoniques

### Juz

Les 30 débuts de juz de `QuranStructureMetadata` concordent avec Tanzil :
1:1, 2:142, 2:253, 3:93, 4:24, 4:148, 5:82, 6:111, 7:88, 8:41, 9:93, 11:6, 12:53, 15:1, 17:1, 18:75, 21:1, 23:1, 25:21, 27:56, 29:46, 33:31, 36:28, 39:32, 41:47, 46:1, 51:31, 58:1, 67:1, 78:1.

Les fins codées sont le verset immédiatement antérieur au début du juz suivant, avec 114:6 pour le juz 30.

### Hizb

Un hizb commence tous les quatre quarts de hizb. Les 60 débuts codés concordent avec les quarts Tanzil 1, 5, 9, ..., 237.

Les fins codées sont le verset immédiatement antérieur au début du hizb suivant, avec 114:6 pour le hizb 60.

## 2. Placement dans les pages du Mushaf de Médine

Les 60 pages de début de hizb ont été recoupées contre la table de pagination 604 pages. Les indicateurs `startsInsidePage` concordent avec le premier verset imprimé de la page.

Exemples de frontières partagées vérifiés :
- page 11 commence à 2:70 ; le hizb 2 commence à 2:75 : page partagée hizb 1 / hizb 2 ;
- page 62 commence à 3:92 ; le juz 4 / hizb 7 commence à 3:93 : page partagée ;
- page 121 commence à 5:77 ; le juz 7 / hizb 13 commence à 5:82 : page partagée ;
- page 201 commence à 9:87 ; le juz 11 / hizb 21 commence à 9:93 : page partagée ;
- page 502 commence à 45:33 ; le juz 26 / hizb 51 commence à 46:1 : page partagée ;
- page 591 commence à 86:1 ; le hizb 60 commence à 87:1 : page partagée.

Pages partagées entre deux juz voisins : 62, 121, 201, 502.

Pages partagées entre deux hizb voisins : 11, 51, 62, 72, 92, 112, 121, 192, 201, 231, 272, 292, 312, 371, 413, 431, 451, 491, 502, 513, 531, 591.

Le lecteur affiche déjà toutes les divisions correspondant à une page et ajoute un message explicite lorsque la borne commence ou finit au milieu de la page.

## 3. Longueur réelle en pages physiques

Une page frontière peut appartenir à deux sections. La longueur en « pages touchées » n'est donc pas une constante :

- juz : généralement 20 pages touchées, avec 20 à 23 selon la section et les pages partagées ;
- hizb : 9 à 14 pages touchées ;
- distribution des hizb : 31 hizb touchent 10 pages, 19 en touchent 11, 5 en touchent 9, 4 en touchent 12 et le hizb 60 en touche 14.

Cette variation est normale. Il ne faut pas présenter « 10 pages = un hizb » comme une équivalence canonique.

## 4. Écart fonctionnel A — quota fixe de 10 pages

`QuranPageSelector.tenPageQuotaFromHizb()` prend toujours les dix pages à partir de `startPage`, sans tenir compte de `endPage`.

Conséquence : les hizb qui touchent seulement 9 pages (15, 18, 42, 52, 56) produisent un plan dont la dixième page appartient au hizb suivant. Inversement, un hizb plus long n'est pas parcouru en entier par ce plan fixe.

Le quota produit (10 pages au palier correspondant) peut rester une règle Safeguard, mais il ne doit pas être confondu avec la longueur réelle du hizb ni contredire une sélection utilisateur censée limiter la lecture à ce hizb.

## 5. Écart fonctionnel B — sélection par Juz non appliquée aux plans multi-pages

Dans `SafeguardCyclePrefs.ensurePlan()` :
- `MORNING` construit le plan uniquement depuis `selectedHizb` ;
- `HIZB` construit le plan uniquement depuis `selectedHizb` ;
- `selectionMode` et `selectedJuz` ne sont consultés que pour `MICRO`.

Donc un utilisateur choisissant « par Juz » ne pilote pas réellement les plans multi-pages du matin / palier 90 minutes.

## Statut release

- Bornes religieuses / structurelles : **VERT**.
- Pagination / pages partagées : **VERT**.
- Présentation des frontières dans le lecteur : **VERT**.
- Respect de la sélection Juz/Hizb par le générateur de plans : **ROUGE jusqu'à correction**.
- Ne pas modifier les bornes canoniques elles-mêmes ; corriger uniquement la logique de planification et renforcer les tests.