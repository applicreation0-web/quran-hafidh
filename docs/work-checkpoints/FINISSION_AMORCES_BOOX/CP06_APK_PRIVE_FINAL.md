# CP-06 — APK debug personnel BOOX

Identifiant : `CP-06`

Date : 2026-10-15

État : **EN COURS**

## Source gelée

- HEAD CP-05 à compiler : `6db0e342684344634892d7da4b3c695a3138a232`.
- Arbre applicatif : `92cb64b26c43d61db4e4605139fb6a725781fec6`.
- Le workflow public ne transporte aucun titre privé ; il produit seulement un
  APK intermédiaire et les outils Android de signature nécessaires.

## Inventaire privé vérifié

- Clé AEAD CP-01 retrouvée hors dépôt.
- JKS M9 retrouvée hors dépôt ; SHA-256 :
  `fc5506f17e4dc5335be49d715900198ed0c3780d76bff7ae650d1fc6ab7a56cd`.
- Certificat de la JKS lisible ; SHA-256 :
  `b75ba0a3dbe7c8f00c80799460d890864142b585f669dad5db468b8e2edb7aa4`.
- L'ancien APK récupéré est tronqué : 40 685 568 octets, archive ZIP
  incomplète, SHA-256
  `9911ddd79a5bde4f2d01f069fa153130682690f81334417eb3dc8d62422c0ead`.
  Il est exclu du livrable final.

## Fichiers modifiés à ce stade

- `.github/workflows/haafidh-ibn-kathir-debug.yml`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/CP06_APK_PRIVE_FINAL.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/STATUS_CURRENT.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/INDEX.md`
- `docs/work-checkpoints/FINISSION_AMORCES_BOOX/HANDOVER_REPRISE.md`

## Travail terminé à ce stade

- Le workflow intermédiaire existant est épinglé au HEAD CP-05 exact et à
  l'arbre applicatif attendu.
- La relance M8 est retirée de cette seule construction : le workflow conserve
  les audits de gel, les contrats source, l'assemblage et la vérification de
  l'absence de titres privés.
- L'artefact intermédiaire exportera `aapt`, `zipalign` et `apksigner.jar` pour
  l'injection et la signature strictement locales.

## Tests exécutés à ce stade

- Lecture JKS et certificat : **PASS**.
- Intégrité ZIP de l'ancien APK : **FAIL**, fichier tronqué ; exclu.
- Syntaxe YAML et bloc shell du workflow : **PASS**.
- Compilation du nouvel intermédiaire : **NON EXÉCUTÉ**.
- Injection privée, signature et vérifications APK : **NON EXÉCUTÉ**.

## Références récupérables

- Paquet chiffré GitHub CP-01 :
  `secure-corpus/ibn-kathir-amorces-v1/amorces-m7b-1903.qhaead`.
- Clé AEAD et JKS : sauvegardes privées hors dépôt.
- Commit du workflow CP-06 : à inscrire après push.

## Reprise exacte

Première action : déclencher manuellement le workflow
`Quran Haafidh CP06 — intermediate APK only, no private titles`, attendre son
état final réel, récupérer l'artefact s'il est PASS, puis vérifier son manifeste
avant toute injection privée locale.
