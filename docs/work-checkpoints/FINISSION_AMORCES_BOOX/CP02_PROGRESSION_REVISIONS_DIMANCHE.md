# CP-02 — Progression et révisions du dimanche

Identifiant : `CP-02`

Date : 2026-10-14

État : **EN COURS**

## GitHub réel

- HEAD avant : `54f991be3633a5c61e4bc51a11590b10116b6406`.
- HEAD après : à renseigner après vérification du commit distant.

## Fichiers modifiés à ce stade

- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/SundayPromotionActiveCorpusIntegrationTest.java`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP02_PROGRESSION_REVISIONS_DIMANCHE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`

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

## Matrice contractuelle provisoire

| Règle | Preuve prévue | État |
|---|---|---|
| `Appris → finale Renforcement → Acquis → active` | scénario Sabqi QCF + redémarrage | NON EXÉCUTÉ |
| `Stabilisé → finale Consolidation → Acquis → active` | scénario Itqān QCF + redémarrage | NON EXÉCUTÉ |
| Aucune entrée active prématurée | assertions avant finales | NON EXÉCUTÉ |
| Aucun double crédit / saut de ligne | ensembles exacts + rejeu idempotent | NON EXÉCUTÉ |
| Curseurs actif/passif indépendants | écritures et deux redémarrages | NON EXÉCUTÉ |
| Aucune finale déjà validée ne réapparaît | listes finales vides après validation | NON EXÉCUTÉ |
| Aucune Révision active/passive le dimanche | politique réelle + contrats du routeur | NON EXÉCUTÉ |

## Tests exécutés

- `SundayPromotionActiveCorpusIntegrationTest` : **NON EXÉCUTÉ** — le poste
  Work possède JDK 17 mais pas de distribution Gradle locale ; exécution à
  effectuer par la CI existante après sauvegarde de ce checkpoint EN COURS.
- Suites historiques M8 180/365/548 : non rejouées conformément à la consigne.

## Problèmes restants et raisons

- Le test doit être compilé et exécuté par la CI. Aucun correctif moteur ne
  sera appliqué avant qu'un échec reproductible ne l'exige.
- Le commit et les identifiants de CI doivent ensuite être inscrits ici ; le
  jalon restera EN COURS tant que ces résultats ne sont pas lus.

## SHA-256 des livrables concernés

- Aucun livrable binaire pour CP-02 à ce stade.

## Références récupérables

- Branche GitHub autorisée, commit à renseigner après le push de sauvegarde.
- CI source/JVM et Android de cette branche, identifiants à renseigner.

## Reprise exacte

Première action : vérifier le HEAD distant du commit de sauvegarde CP-02 puis
lire la CI source/JVM. Si le nouveau test échoue, conserver la sortie exacte,
reproduire le défaut et appliquer le plus petit correctif nécessaire ; s'il
passe, ne modifier aucun moteur Hifz et clôturer CP-02.
