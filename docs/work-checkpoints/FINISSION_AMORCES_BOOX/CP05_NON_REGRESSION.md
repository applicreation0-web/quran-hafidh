# CP-05 — Contrôles finaux et non-régression

Identifiant : `CP-05`

Date : 2026-10-15

État : **PASS**

## GitHub réel et gel

- HEAD avant : `f1a8fb5d62d2753a2fb8e5f4a240828e40f2fcdb`.
- HEAD après maintenance des harnais CP-05 :
  `f22bfcdd8ff66860c55828eac27cc5738fad0fba`.
- Arbre GitHub vérifié : `3f8c5c34406829e6b6438f0e77c3827cc85b2fb7`.
- Arbre applicatif gelé, identique avant/après CP-05 :
  `92cb64b26c43d61db4e4605139fb6a725781fec6`.
- Aucun fichier de production Hifz n'a changé depuis le checkpoint CP-02.

## Fichiers modifiés

- `scripts/test_hifz_mask_segments.js`
- `scripts/test_hifz_claude_audit_regressions.py`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP05_NON_REGRESSION.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

## Travail terminé

- Gel du candidat issu de CP-04 et comparaison ciblée avec CP-02 : aucun
  changement dans `hifz-core`, `HifzPrefs`, `HifzSessionActivity` ou
  `ConsolidationCycleEngine`.
- Réutilisation des preuves fraîches CP-02, CP-03 et CP-04 ; aucune relance
  manuelle des simulations M8 180/365/548.
- Exécution des garde-fous locaux sur le gel Hifz, la version officielle, les
  23 icônes, les 19 silhouettes Lucide, J6, la carte graphique, le paquet AEAD,
  le protocole Lecture et les masques.
- Deux harnais anciens ont été réalignés sur des comportements de production
  déjà présents et prouvés : effacement 100 % sans résidu, politique
  hebdomadaire datée, réparation sûre du curseur de Révision. Aucun code de
  production n'a été modifié.

## Matrice fonctionnelle

| Fonction / invariant | Preuve | État |
|---|---|---|
| Accueil et programme sept jours | Android CP-04 + politique datée | PASS |
| Sabqi / Itqān | Android CP-04, modes réels | PASS |
| Renforcement / Consolidation | Android CP-04 + gel moteurs | PASS |
| Révision active / passive | Android CP-04 + protocole 813 transitions | PASS |
| Promotions du dimanche | intégration CP-02 + redémarrage | PASS |
| Mémorisation libre indépendante | Android CP-04 + snapshot préférences | PASS |
| Carte de progression | suites Android CP-04, persistance | PASS |
| Carte sémantique | Android CP-03, blocs court/multipage/grande sourate | PASS |
| Lecture et Amorces | Android CP-03/04 + chargeur privé fail-closed | PASS |
| Sauvegarde / restauration | suite Android 98/98 | PASS |
| Fermeture forcée / reprise | deux phases Android externes | PASS |
| Aucune perte, double écriture ou ligne sautée | CP-02 + gel moteurs | PASS |
| Aucun changement involontaire des plages Hifz | diff ciblé vide | PASS |
| BOOX physique | appareil non disponible | NON EXÉCUTÉ |

## Tests exécutés

- `test_hifz_mask_segments.js` : **PASS** (`HIFZ_RANDOM_SOURCE_INK_MASK_OK`).
- `test_reader109_protocol.js` : **PASS**, 813 transitions.
- `test_hifz_vertical_fit.js` : **PASS**.
- `test_hifz_claude_audit_regressions.py` : **PASS**, 5/5.
- `test_amorce_aead_bundle.py` : **PASS**, 2/2.
- `verify_ibn_kathir_map_contract.py` : **PASS**.
- `verify_public_amorce_keys.py` : **PASS**, 1 903 groupes, 1 708 titres,
  195 absences, aucun texte privé.
- `guard_frozen_hifz_engines.sh` et
  `audit_hifz_quiz_against_1171.sh` : **PASS**.
- `verify_contractual_ui_icons.py`, `verify_approved_icon_silhouettes.py` et
  `verify_j6_icon_roles.py` : **PASS**.
- CI source/JVM de l'arbre applicatif CP-04 `38690267562` : **PASS**.
- CI Android de l'arbre applicatif CP-04 `38690267503` : **PASS** ; 98 tests,
  deux reprises après arrêt forcé et 21 captures.
- Commit de maintenance des seuls tests CP-05 : `[skip ci]` ; aucune CI M8 ou
  Android inutilement relancée.

## Incidents qualifiés pendant CP-05

- RED initial du harnais JS : `window.addEventListener` absent du faux DOM,
  géométrie 100 % restée sur l'ancien retrait 0,6/1,2 et assertion dépendante
  d'un commentaire. Le rendu de production datait du commit `6f0b3802` ; le
  test seul a été corrigé puis est passé.
- RED initial de l'audit Python : il exigeait l'ancien `planFor()` et
  l'absence du setter de curseur, alors que la cadence datée et la réparation
  Al-Fātiḥa les ont remplacés. Les assertions ont été mises en conformité avec
  les appels réels ; aucun moteur n'a été modifié.

## Problèmes restants et raisons

- Aucun blocage logiciel connu pour passer à CP-06.
- Les contrôles tactiles et E-Ink sur BOOX restent **NON EXÉCUTÉ** sans appareil.
- Le certificat personnel, les données déchiffrées et l'APK doivent rester hors
  GitHub et hors artefacts publics pendant CP-06.

## SHA-256 des livrables concernés

- Harnais masques :
  `6146ae445f382274ef596d6392891af33cfe55f0dd36b72327805ded6a8bce1a`.
- Audit Python :
  `c83f3ea18c6e3676da115b9d7e7acfbf6938b2adf7082f087c6120c75d33e58d`.
- Archive Android réutilisée CP-04 :
  `ca42d4a32b95e8b346acb5c0cf54888047a5e92f30962ed07fa8461dadf1ea8f`.

## Références récupérables

- Commit GitHub des harnais CP-05 :
  `f22bfcdd8ff66860c55828eac27cc5738fad0fba`.
- CI source/JVM : run `38690267562`.
- CI Android : run `38690267503`, artefact `11951608972`.
- Captures/rapports CP-04 : copie durable
  `libfile_5a9485b8e3908191be0dcf0e18d932ae`.
- Preuve promotions dimanche CP-02 : runs `38626984145` et `38626983992`.
- Preuve carte CP-03 : runs `38689001952` et `38689001924`.

## Reprise exacte

Première action : ouvrir `CP06_APK_PRIVE_FINAL.md` et `CP00_ETAT_INITIAL.md`,
localiser sans les afficher la clé AEAD et la clé de signature M9, vérifier le
certificat attendu, puis reconstruire les huit fichiers privés hors dépôt à
partir du conteneur CP-01 avant de produire l'APK au HEAD CP-05 exact.
