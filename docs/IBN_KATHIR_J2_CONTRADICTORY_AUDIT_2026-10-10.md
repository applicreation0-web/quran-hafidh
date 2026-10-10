# Checkpoint J2 — Ibn Kathīr : revue contradictoire, 2026-10-10

**NO GO éditorial/runtime**. Provenance verrouillée (Darussalam abrégé via index documentaire spa5k, Quran.ws Hafs et boîtes KFQC). Les 1 903 groupes ont un statut explicite UNREVIEWED ; zéro clé approuvée et zéro clé active.

## Résultats mesurés

- 1 068 propositions d'un seul mot (non synonymes de rappel mnémotechnique).
- 924 débuts de groupe avec premier mot fréquent dans l'index (seuil documentaire >=10 groupes).
- 35 groupes dont le verset initial Tanzil se répète parmi les débuts de groupes.
- 22 propositions lexicales exigeant >=7 mots pour se distinguer sur la page.
- 291 groupes multipages, 488 groupes mono-verset.
- Exception sans préfixe distinct sur une page : IKEN002_047_047.
- Exception de concordance Tanzil/QCF : 15:7, dans IKEN015_006_009.
- Échantillon contradictoire priorisé : calculé dans ik-j2-review.json (Fātiḥa, Qāf, Dhāriyāt, Najm, An-Nās, Baqarah, duplications, préfixes longs et multipages).

Les catégories se recoupent. Vérification indépendante : 2:47 et 2:122 ont exactement le même verset initial **dans le corpus Tanzil épinglé**, et ne se distinguent donc pas lexicalement par les mots de ce verset. Qāf 50:12–15 est un seul intervalle documentaire Ibn Kathīr ; ne jamais introduire une frontière 50:15 non attestée.

## Critères de suite

- Chaque groupe peut légitimement avoir **zéro, une ou plusieurs** clés ; le nombre de groupes ne fixe pas un quota d'amorces.
- Toute clé requiert les mots QCF vérifiés, les coordonnées, la justification du déclic (sens, contexte, rareté, minimalité) et une preuve de droits de reproduction avant activation.
- La génération automatique d'un préfixe unique ne constitue pas une validation religieuse ou mnémotechnique.
- Les nouveaux fichiers sont **audit-only** dans les artefacts GitHub CI, jamais dans les assets Android.
- Les fichiers gelés 1.17.1, Itqān, Quiz et Mushaf restent inchangés. Il est interdit de produire un APK.

## Preuves techniques

Script : `scripts/audit_ibn_kathir_j2.py`. Tests négatifs : activation prématurée, changement SHA, discordance de verset, préfixe falsifié, groupe manquant. La CI archive `ik-j2-review.json`, `ik-j2-review.tsv`, `ik-j2-summary.md`.
