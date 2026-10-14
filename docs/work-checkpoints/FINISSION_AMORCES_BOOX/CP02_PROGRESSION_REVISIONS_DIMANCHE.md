# CP-02 — Progression et révisions du dimanche

Identifiant : `CP-02`

Date : 2026-10-14

État : **PASS**

## GitHub réel

- HEAD avant : `54f991be3633a5c61e4bc51a11590b10116b6406`.
- HEAD après code/tests : `22d4d43eb28715936c73eae93af1e05de6bc5c10`.
- Arbre local et arbre GitHub vérifiés identiques :
  `0b10a87901a7c423a21f8c551e8d5b0027888ece`.

## Fichiers modifiés à ce stade

- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/SundayPromotionActiveCorpusIntegrationTest.java`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP02_PROGRESSION_REVISIONS_DIMANCHE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

## Travail terminé

- Lecture ciblée des méthodes réelles `completeSabqiBlockV6`,
  `completeStabilizationBlockV6`, `completeSnowballEvening`,
  `completeLearningConsolidationSessionV6` et
  `completeConsolidationSessionV6`.
- Ajout d'un test d'intégration ciblé utilisant la géométrie QCF réelle, les
  deux moteurs de boule de neige, les ensembles de progression schema 6 et un
  stockage `SharedPreferences` conservé à travers plusieurs instances
  `HifzPrefs` (redémarrage simulé).
- Le test couvre les promotions Sabqi et Itqān, l'inclusion effective dans
  `activeMurajaahCorpus()`, l'absence d'entrée prématurée, l'idempotence, les
  lignes exactes, la non-réapparition des finales, l'indépendance des curseurs
  et le routage dominical.
- Les quatre scénarios passent sur le moteur existant. Aucun défaut
  reproductible n'a été observé ; conformément au contrat, aucun fichier de
  production Hifz n'a été modifié.

## Matrice contractuelle

| Règle | Preuve prévue | État |
|---|---|---|
| `Appris → finale Renforcement → Acquis → active` | scénario Sabqi QCF + redémarrage | PASS |
| `Stabilisé → finale Consolidation → Acquis → active` | scénario Itqān QCF + redémarrage | PASS |
| Aucune entrée active prématurée | assertions avant finales | PASS |
| Aucun double crédit / saut de ligne | ensembles exacts + rejeu idempotent | PASS |
| Curseurs actif/passif indépendants | écritures et deux redémarrages | PASS |
| Aucune finale déjà validée ne réapparaît | listes finales vides après validation | PASS |
| Aucune Révision active/passive le dimanche | politique réelle + contrats du routeur | PASS |

## Tests exécutés

- CI source/JVM `38626984145` : **PASS**. Commande exécutée par le workflow :
  `gradle --no-daemon :hifz-core:test :hifz-app:testDebugUnitTest --stacktrace` ;
  sortie `BUILD SUCCESSFUL`. Le task `testDebugUnitTest` inclut les quatre
  méthodes de `SundayPromotionActiveCorpusIntegrationTest`.
- CI Android `38626983992` : **PASS**. Sortie `OK (98 tests)`, puis les deux
  contrôles externes fermeture forcée/reprise `OK (1 test)` et `OK (1 test)`.
- La CI source existante a aussi rejoué automatiquement son garde M8R ; aucune
  relance manuelle de la matrice 180/365/548 n'a été demandée dans CP-02.

## Problèmes restants et raisons

- Aucun problème restant dans le périmètre CP-02.
- Le contrôle physique BOOX ne relève pas de ce jalon et reste réservé à
  CP-07.

## SHA-256 des livrables concernés

- Test d'intégration :
  `b9ce94951f3eb6f0eaf8834b0bc987e4d8ce298efcb38655247abd0d0f2ed805`.
- Artefact Android de preuve :
  `sha256:02f532a1c871d8ee67f478510bfbd3c63ad077613403f749c462a432e1fbdab7`.

## Références récupérables

- Commit GitHub code/tests : `22d4d43eb28715936c73eae93af1e05de6bc5c10`.
- CI source/JVM : run `38626984145`, artefact d'audit
  `11924164351` (`ibn-kathir-amorce-review-38626984145`).
- CI Android : run `38626983992`, artefact `11923784776`
  (`haafidh-android-runtime-22d4d43eb28715936c73eae93af1e05de6bc5c10`).

## Reprise exacte

Première action : ouvrir CP-03, comparer sans fusion les références graphiques
`origin/work/islahi-prototype-p1-from-1-17-1` et
`origin/work/haafidh-v3-ibn-kathir-from-1-17-1` à l'implémentation courante,
puis écrire les tests du parcours `Sourate → bloc → miniature → Lecture → retour`.
