# Registre CI et tests

Les API de statuts et de runs PR renvoient des listes vides : cela ne signifie pas absence de CI.
Lecture REST des cinq derniers runs de branche effectuée le 15 octobre 2026.

| Run | Commit | Conclusion | Nature |
|---|---|---|---|
| 38738763440 | 00fb6986 | success | historique : régression Android |
| 38738763412 | 00fb6986 | success | historique : staging public sans corpus privé |
| 38738763463 | 00fb6986 | success | historique : audit Hifz/Amorces |

Aucun de ces runs ne prouve la présence d’un corpus Mawdudi final ni une recette BOOX physique.

## Nouveaux contrôles CP-00

- Python 3 / stdlib : `python -B scripts/test_audit_mawdudi_epub.py` : 8 tests PASS.
- `audit_mawdudi_epub.py` : source CRC correct, 114 en-têtes, 6127 labels reconnus, 109 non résolus. Ce résultat n’est PAS un PASS éditorial.
- Lors du premier essai sur la source réelle : FAIL technique sur la spine (référence `cover` non définie). Anomalie de source reproduite par test synthétique, puis signalée explicitement sans modifier le source ; seconde exécution réussie.
- Une première version de la commande exploratoire demandait bs4 : NON EXÉCUTÉE (module absent). Extraction définitive avec la bibliothèque standard, sans installation.
- Diff app/hifz-app/chatgpt depuis le HEAD de départ : vide. Aucun test Android/JVM/runtime nouveau, aucun APK.
- Écriture initiale GitHub par git push : refus automatique avant publication, puis échec d’authentification shell après expurgation. Sauvegarde expurgée réussie par connecteur GitHub au commit 9354af43 et relue à distance.
- Sauvegarde privée ZIP : FAIL (quota de stockage), aucune persistance nouvelle revendiquée.

## Reprise après libération d’espace

- Nouvelle sauvegarde privée : PASS ; récupération et empreintes : PASS.
- Simulation isolée depuis le paquet récupéré : PASS ; JSON égal au résultat historique. Aucun nouveau PASS éditorial.
- Vérification CI de branche : toujours runs 38738763440 / 38738763412 / 38738763463 au HEAD applicatif 00fb6986 ; historiques.

## CP-01 lot 001–010

Captures complètes : PASS de collecte ; couverture numérique 1473/1473 : PASS de contrôle de bornes. Archive privée sauvegardée/récupérée/hash : PASS. Revue de tous les titres, terminologie, texte et QCF : EN COURS ou NON EXÉCUTÉ selon les matrices. Aucun test Android nouveau.

CP-01 lot 011–020 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_011_020_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 021–030 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_021_030_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 031–040 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_031_040_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 041–050 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_041_050_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 051–060 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_051_060_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 061–070 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_061_070_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 071–080 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_071_080_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 081–090 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_081_090_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 091–100 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_091_100_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 101–110 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_101_110_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP-01 lot 111–114 : collecte exécutée, contrôles de plages archivés sous CP01_LOT_111_114_COVERAGE.json, sauvegarde/récupération/hash PASS. Sémantique, QCF et Android non validés.

CP01 parcours : vérification indépendante des IDs/plages, 6236 uniques, zéro doublon ; douze restaurations privées avec SHA concordant. Android, miniatures et validation éditoriale non exécutés.

QCF numérique : vérification locale racine, 17 tests Python PASS avec PYTHONPATH=scripts. Première invocation sans ce chemin : erreur import du test EPUB, conservée dans le rapport. Android non exécuté. Préparation QCF seulement ; rendu et miniatures non validés.


## Reprise CI 2026-10-15 — ne pas confondre PASS ancien et nouveau

- `80fc9427` : audit et régression Android historiques relancés — PASS. Fix correctif du statut des titres comme brouillons, trois tests ajoutés.
- `95e86bb2` : audit Ibn Kathir a échoué sur la **liste de fichiers gelés**, avant tests ; pas une régression Hifz confirmée.
- `db97b233` : première liste corrigée ; deuxième audit de baseline gelé encore FAIL pour les deux nouveaux adaptateurs ; correction ciblée apportée ensuite.
- `d7e7022f` : `38786649459` audit source Hifz PASS ; `38786649298` pilote PUBLIC STAGING Al-Baqarah PASS. Les CI de ce staging n'attestent ni corpus privé, ni rendu physique BOOX, ni APK final.
- `38786649334` régression Android instrumentée : **EN COURS au pointage**, ne pas déclarer PASS sans vérifier la conclusion finale et les jobs.

## Continuation QCF page-end bug (2026-10-15)

Commit `872fac3479af70bac3581b0568f4261b33c66927` changes ONLY MawdudiMapModel.lastPage to use the last physical line of the final ayah; adds `MawdudiMapModelRealQcfTest` using 1,053 real passages and the canonical 604-page geometry. CI runs `38788572252` (audit) and `38788572349` (Android regression) were **in progress when inspected**. Do not mark PASS until actual conclusion; public baseline success on `d7e7022f` is historical proof only.

## CP02 EPUB/hOCR safe locator — 15 October 2026

Six synthetic Python tests passed locally; CI step added to audit-only workflow and must be checked on its new HEAD. Original EPUB and user-provided hOCR metadata: 109 cases, 44 exact hOCR word labels, 107 cases <=5 candidate scan pages. Five browser scan candidates examined; 2 visible and 3 not confirmed. No source prose exported, 0 textual editorial approvals and 0 automated repairs. Previous two JVM+Hifz CI runs PASS on af238389. This new change MUST obtain its own CI PASS before promoting.

## CP02 1210-page retention / QCF pilot correction — 15 October 2026

New code-only metadata audit `scripts/mawdudi_hocr_epub_retention.py` and five synthetic regression tests (5/5 local PASS); existing six scan-locator tests (6/6 local PASS). Baseline CI for fcd1b786: audit PASS and instrumented Android PASS, separately recorded. Commit de07de56 adds the retention tool and fixes the Al-Baqarah pilot's last-page calculation via shared MawdudiMapModel; new audit, CP08P staging, and Android CI initiated on de07de56 — outcomes must be checked before PASS claims. 1210 real pages compared privately; 19 severe/high source OCR loss candidates, 1189 without major differences, 1 review, 1 low-evidence; 41 previously absent EPUB verse labels on the high-risk pages. No proof of correct notes, verified prose or rights; no private sources uploaded publicly.
