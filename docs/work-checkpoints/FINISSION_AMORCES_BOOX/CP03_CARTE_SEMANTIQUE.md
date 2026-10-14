# CP-03 — Carte sémantique Ibn Kathīr

Identifiant : `CP-03`

Date : 2026-10-14

État : **EN COURS**

## GitHub réel

- HEAD avant : `a3daccd6a3a87fef862f76d8cf016cec902f14bb`.
- HEAD après : à renseigner après implémentation et vérification distante.

## Références comparées sans fusion

- `origin/work/islahi-prototype-p1-from-1-17-1` :
  `fbe557331faf58a06d4a1b2b8f43c80d9a95e5e9` — parcours graphique,
  miniatures QCF et retour d'état.
- `origin/work/haafidh-v3-ibn-kathir-from-1-17-1` :
  `ea15769ac68f32c59c39aae7088de25ad93717bd` — index Ibn Kathīr,
  `MushafView`, pages multipages et ouverture temporaire de Lecture.
- Branche courante : index vérifié de 1 903 groupes et chargeur privé des
  1 708 titres déjà présents, mais aucune activité de carte.

## Fichiers modifiés à ce stade

- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/IbnKathirMapJourneyTest.java`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP03_CARTE_SEMANTIQUE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`

## Travail terminé

- Inventaire différentiel des deux références et de la branche courante.
- Décision d'implémentation : nœuds graphiques verticaux sans `ListView`, titre
  approuvé stable ou état `Sans titre`, miniature QCF `MushafView`, surlignage
  exact du groupe et ouverture temporaire de Lecture.
- Test écrit pour bloc court, bloc multipage, grande sourate, intégrité du
  surlignage, absence de l'ancienne liste et restauration de position.

## Tests exécutés

- Précondition RED : le test cible `IbnKathirMapActivity` et
  `IbnKathirMapModel`, absents du HEAD initial : **FAIL attendu constaté par
  inspection de fichiers**.
- JVM/Android : **NON EXÉCUTÉ** à ce stade.

## Problèmes restants et raisons

- Implémenter le modèle pur et l'activité sans importer l'ancienne liste.
- Adapter les contrats sources qui interdisaient l'ancienne carte, décision
  désormais remplacée explicitement par CP-03.
- Exécuter CI, produire les captures de blocs courts/multipages/grande sourate
  et vérifier le retour Lecture.

## SHA-256 des livrables concernés

- Aucun livrable binaire à ce stade.

## Références récupérables

- Branche GitHub autorisée ; commit EN COURS à renseigner après push.

## Reprise exacte

Première action : implémenter `IbnKathirMapModel` puis
`IbnKathirMapActivity` avec la miniature `MushafView`, ajouter le manifeste et
le raccourci Accueil, et rendre les tests CP-03 verts avant toute capture.
