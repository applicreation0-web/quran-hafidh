# CP-03 — Carte sémantique Ibn Kathīr

Identifiant : `CP-03`

Date : 2026-10-14 ; reprise et clôture : 2026-10-15

État : **PASS**

## GitHub réel

- HEAD avant CP-03 : `a3daccd6a3a87fef862f76d8cf016cec902f14bb`.
- Checkpoint RED/reprise : `609ef6d44239910126431bcbf0dac388dd5b40c4`.
- HEAD après code/tests : `709ec47908d3bcdc4f6db1d31a6fa65edebb4f77`.
- Arbre local et arbre GitHub vérifiés identiques :
  `c69b22146a4302e256db7c950db34f3e8e35c649`.

## Références comparées sans fusion

- `origin/work/islahi-prototype-p1-from-1-17-1` :
  `fbe557331faf58a06d4a1b2b8f43c80d9a95e5e9` — parcours graphique,
  miniatures QCF et retour d'état.
- `origin/work/haafidh-v3-ibn-kathir-from-1-17-1` :
  `ea15769ac68f32c59c39aae7088de25ad93717bd` — éléments utiles de
  navigation QCF seulement ; l'ancienne liste n'a pas été reprise.

## Fichiers modifiés

- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapModel.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranSurahNames.java`
- `hifz-app/src/main/AndroidManifest.xml`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/IbnKathirMapJourneyTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/BooxPortraitHomeSourceContractTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/EveryScreenContractualIconTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/NoSemanticListsAndOfflineAmorcesContractTest.java`
- `hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzAndMapRuntimeSmokeInstrumentedTest.java`
- `scripts/verify_ibn_kathir_map_contract.py`
- `scripts/verify_j6_icon_roles.py`
- `scripts/ci_android_runtime_visual.sh`
- `scripts/guard_frozen_hifz_engines.sh`
- `scripts/audit_hifz_quiz_against_1171.sh`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP03_CARTE_SEMANTIQUE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/IMPLEMENTATION_PLAN.md`

## Travail terminé

- Modèle pur des groupes, plages de pages QCF, bornage multipage,
  surlignage exact et état sauvegardable.
- Nouvelle carte graphique verticale à nœuds alternés ; aucune `ListView` ni
  `ArrayAdapter` et aucun retour à l'ancienne interface.
- Choix direct des 114 sourates et affichage des 1 903 groupes documentaires.
- Un titre approuvé stable est lu par le chargeur privé existant ; l'absence
  de clé/corpus privé échoue fermée sur `Sans titre`.
- Miniature authentique `MushafView`, passage exact surligné, navigation entre
  pages d'un groupe multipage et ouverture de `StudyReaderActivity` à la page
  et au verset exacts.
- Retour de Lecture conservant sourate, groupe, page d'aperçu et position de
  défilement ; aucun état Hifz ou crédit de progression n'est écrit.
- Accès `Carte` ajouté à l'accueil, manifeste déclaré et pictogrammes existants
  réutilisés.
- Les garde-fous historiques qui supprimaient toute Carte ont été remplacés
  par des contrats positifs exigeant la carte graphique et interdisant encore
  l'ancienne liste, Quiz, le réseau et les sources de titres retirées.

## Matrice de conformité

| Contrat | Preuve | État |
|---|---|---|
| Sourate → bloc graphique | modèle/JVM + Android | PASS |
| Titre stable ou absence explicite | chargeur privé / `Sans titre` | PASS |
| Miniature QCF authentique | `MushafView` réellement rendue | PASS |
| Surlignage exact court et multipage | géométrie QCF réelle | PASS |
| Miniature → Lecture | activité Android page 10 | PASS |
| Retour conservant sélection et page | capture + assertions Android | PASS |
| Grande sourate | Al-Baqara, 173 groupes, groupe 173 visible | PASS |
| Ancienne liste / réseau / Quiz absents | contrats source | PASS |
| Portrait BOOX physique | appareil non disponible | NON EXÉCUTÉ |

## Tests exécutés

- Précondition RED au commit `609ef6d…` : modèle et activité absents, échec
  attendu constaté avant implémentation.
- Garde-fous locaux : gel Hifz, audit 1.17.1, contrat carte, 23 pictogrammes,
  silhouettes et inventaire Android 25 fichiers/98 tests : **PASS**.
- CI source/JVM `38689001952` : **PASS**. Sorties `BUILD SUCCESSFUL`, contrat
  CP-03 graphique PASS, index 1 903 groupes PASS et J6 23 pictogrammes PASS.
- CI Android `38689001924` : **PASS**. Sortie `OK (98 tests)`, fermeture
  forcée/reprise `OK (1 test)` deux fois et 19 captures, dont les cinq captures
  CP-03 obligatoires.
- Le premier essai source `38688842508` a échoué sur l'ancien périmètre du
  garde-fou pour `QuranSurahNames.java`; cause reproduite, puis correction
  minimale RED→GREEN au commit `709ec479…`.

## Problèmes restants et raisons

- Aucun problème logiciel restant dans le périmètre CP-03.
- Les pixels de titres approuvés ne peuvent pas être validés dans la CI
  publique sans clé privée ; ce contrôle est réservé à l'APK privé CP-06.
- Le rendu sur BOOX physique reste **NON EXÉCUTÉ** jusqu'à CP-07.

## SHA-256 des livrables concernés

- Archive complète des preuves Android :
  `c9170cf1e374d929728ddaf9b44a61f4b93daef7febef88a695b870b46788e90`.
- Carte courte :
  `eb8dc89c5b0ddad63a5e1228639f56dd7cad1abd908875d57a405644e5bf04c4`.
- Carte multipage :
  `f3128437aed4bb09d9bc0b5bc83928b6cdc4c15721f9d296d5ac31dc2db38978`.
- Lecture ouverte depuis la carte :
  `0b513f12b14afd90935c50398293d9c37d4fb97f4212d1310ea314b8cd5d89ce`.
- Retour vers la carte :
  `4ac74b676a2ed0281507d5a349025f861dadb609b5d0d7d41e7f4b943f7bc61b`.
- Grande sourate :
  `f8d6b2513bcaf76aaf9714961c60b78fece373de6408779c53fe0f0d6bafc180`.

## Références récupérables

- Commit GitHub code/tests : `709ec47908d3bcdc4f6db1d31a6fa65edebb4f77`.
- CI source/JVM : run `38689001952`.
- CI Android : run `38689001924`, artefact `11951796542`,
  `haafidh-android-runtime-709ec47908d3bcdc4f6db1d31a6fa65edebb4f77`.
- Copie durable privée de l'archive :
  `libfile_90ebbaa18b0c81919fe690f11cf04e21`.

## Reprise exacte

Première action : ouvrir `CP04_VISUEL_MEMORISATION_LIBRE.md`, inventorier les
écrans et preuves existants des 23 pictogrammes et de Mémorisation libre, puis
écrire un test ciblé uniquement pour tout écart contractuel encore non couvert.
