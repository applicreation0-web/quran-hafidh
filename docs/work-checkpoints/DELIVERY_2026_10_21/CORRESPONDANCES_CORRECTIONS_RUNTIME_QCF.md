# Correspondances — correctifs de navigation et présentation (2026-10-21)

## Référence Git

Branche active : `work/haafidh-1171-atlas-correspondances`.
Base : `b5943ac8a7a7ee0c712472efcc65393156307280`, CI intégrité Hifz/Atlas/Correspondances SUCCESS (run 39552353445).

## Correctifs réellement committés

1. `0a4843f` — ajout du test de garde des icônes de dossiers non certifiés et des actions QCF sans références.
2. `ddcdfe1` — `CorrespondenceDirectoryActivity` : seul un dossier présent dans le catalogue approuvé utilise une des huit icônes de type. Les dossiers simplement historiques utilisent l'icône d'information. L'action « Comparer les pages QCF » est absente sans 1–40 références disponibles.
3. `a7bcee6` — `CorrespondenceReferenceActivity` : mémoriser indépendamment page, zoom, pan X/Y par référence, restaurer chaque feuillet lors du déplacement dans les références et après une recréation Android. La lecture du Bundle rejette les valeurs hors bornes/non finies; les feuilles QCF et les données de relation ne changent pas.
4. `ad6290b` — test de contrat du maintien des positions QCF entre changement de référence et restauration Android.

## Invariants

- Aucun moteur Hifz, réglage, état de progression ou corpus QCF modifié.
- Aucun fichier Atlas ou titre de ses 366 Unités / 1 053 Passages modifié.
- Huit icônes Correspondances et 23 icônes historiques inchangées.
- Aucun statut scientifique automatiquement certifié, aucune relation historique promue.
- Catalogue privé `approved_v1.json`, secrets et signature non ajoutés au dépôt public.
- Aucune release ni APK final distribué.

## Portes de validation

- Run J4 `39553286411` : SUCCESS après la correction du répertoire.
- Run J4 `39553470337` : contrôle du dernier test de navigation (en cours lors de ce checkpoint).
- CI intégrité `39553427744`, compilation privée `39553427767`, staging BOOX `39553427745` : en cours au checkpoint, sur le code `a7bcee6`; le commit suivant `ad6290b` ne change que le test.
- Les contrôles physiques BOOX et le catalogue privé effectivement intégré ne sont pas vérifiés ici.

## Reprise

Lire la conclusion finale des quatre runs ci-dessus; ne pas déclarer les tests Android/BOOX verts sur cette base seule. Continuer la certification séparée des 110 dossiers sans approbation fictive. Aucun paquet incomplet à livrer.
