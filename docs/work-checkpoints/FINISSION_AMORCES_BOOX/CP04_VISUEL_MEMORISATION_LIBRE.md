# CP-04 — Finition visuelle et Mémorisation libre

Identifiant : `CP-04`

Date : 2026-10-15

État : **PASS**

## GitHub réel

- HEAD avant : `6ff178b4dd8013d259a69bea3a8bb26aed800e1a`.
- HEAD après code/tests : `54f9a825caf0966572d26506b018a39bb7ecadf3`.
- Arbre local et arbre GitHub vérifiés identiques :
  `cc2696f6154b1ee6f02808dfb5279eae56186f4d`.

## Fichiers modifiés

- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/FreeMemVisualIndependenceSourceContractTest.java`
- `hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzAndMapRuntimeSmokeInstrumentedTest.java`
- `scripts/ci_android_runtime_visual.sh`
- `scripts/guard_frozen_hifz_engines.sh`
- `scripts/audit_hifz_quiz_against_1171.sh`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP04_VISUEL_MEMORISATION_LIBRE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

## Travail terminé

- Audit byte à byte des 23 pictogrammes contractuels : PASS.
- Vérification de 19 silhouettes Lucide contre le commit épinglé
  `f93beca5957187620e30ae771d220d88501be621` : PASS.
- Clé Amorces confirmée sur `key-round`, commandes compactes sans libellés
  imprimés et zones principales à 48 dp.
- Les quatre historiques `Lecture`, `Carte`, `Tafsir` et `Crayon` sont intacts,
  mais restent explicitement non certifiables contre une référence graphique
  indépendante absente ; aucune substitution arbitraire n'a été faite.
- La légende du passage de Mémorisation libre peut désormais occuper deux
  lignes en portrait au lieu d'être forcée sur une ligne tronquée.
- Les cinq cibles de masque 0/25/50/75/100 utilisent désormais la même zone
  tactile minimale 48 × 48 dp que les commandes principales.
- Le test Android sélectionne réellement 1:1–1:2, applique 75 %, ajoute une
  répétition, recrée l'activité et vérifie la conservation du passage, du
  masque et du compteur.
- Un instantané complet des préférences Hifz avant les interactions est comparé
  après reprise : aucune écriture ni crédit de progression ou de révision.
- Audio local, navigation page/sourate/hizb, sélection, compteur, masques et
  sauvegarde/reprise sont conservés ; aucun moteur Hifz n'a été modifié.

## Matrice de conformité

| Contrat | Preuve | État |
|---|---|---|
| 23 pictogrammes présents et intègres | manifestes/hash | PASS |
| Amorces = `key-round` | manifeste gelé | PASS |
| Icônes sans libellés visibles | contrat `Ui.iconButton` | PASS |
| Quatre historiques inchangés | hash ; référence indépendante absente | PASS intégrité / NON CERTIFIABLE silhouette |
| Passage et compteur | Android réel | PASS |
| Masques 0/25/50/75/100 | source + interaction 75 % | PASS |
| Audio local | source/contrat existant | PASS |
| Navigation page/sourate/hizb | source/contrats existants | PASS |
| Sauvegarde et reprise | recréation Android | PASS |
| Indépendance progression/révisions | snapshot Hifz inchangé | PASS |
| BOOX physique | appareil non disponible | NON EXÉCUTÉ |

## Tests exécutés

- RED ciblé : `selection.setMaxLines(2)` et cibles masque 48 dp absents,
  échec attendu constaté avant correction.
- Contrats locaux après correction : gel Hifz, audit 1.17.1, 23 pictogrammes,
  19 silhouettes épinglées, J6 et inventaire 25 fichiers/98 tests : **PASS**.
- CI source/JVM `38690267562` : **PASS** ; compilation/tests Gradle et contrats
  visuels réussis.
- CI Android `38690267503` : **PASS** ; `OK (98 tests)`, deux contrôles de
  fermeture forcée/reprise `OK (1 test)` et 21 captures.
- Premier essai source `38690118561` : FAIL sur l'ancien périmètre de gel qui
  n'autorisait pas `FreeMemActivity.java`; cause reproduite puis garde-fous
  corrigés minimalement au HEAD `54f9a825…`.

## Problèmes restants et raisons

- Les quatre pictogrammes historiques n'ont toujours pas de référence
  graphique indépendante permettant une certification de silhouette ; leur
  intégrité exacte est néanmoins vérifiée.
- Le comportement tactile et le rafraîchissement E-Ink sur BOOX physique
  restent **NON EXÉCUTÉ** jusqu'à CP-07.

## SHA-256 des livrables concernés

- Archive Android :
  `ca42d4a32b95e8b346acb5c0cf54888047a5e92f30962ed07fa8461dadf1ea8f`.
- Capture avant sélection :
  `3f2e2797bc01b0d605b5d8a51fdfaa368e9686e320b6e0bd1bcb23b8cf76b0fa`.
- Capture après sélection, 75 % et compteur 1 :
  `ab951d3eba9c01f65a142dca77d147d4d97404bac12dbf9f546dfdcfd2730c14`.

## Références récupérables

- Commit GitHub code/tests : `54f9a825caf0966572d26506b018a39bb7ecadf3`.
- CI source/JVM : run `38690267562`.
- CI Android : run `38690267503`, artefact `11951608972`,
  `haafidh-android-runtime-54f9a825caf0966572d26506b018a39bb7ecadf3`.
- Copie durable privée : `libfile_5a9485b8e3908191be0dcf0e18d932ae`.

## Reprise exacte

Première action : ouvrir `CP05_NON_REGRESSION.md`, geler le HEAD candidat
`54f9a825…`, dresser la matrice des fonctionnalités à partir des preuves
fraîches CP-02/03/04 et exécuter seulement les contrôles bloquants non encore
couverts, sans rejouer M8.
