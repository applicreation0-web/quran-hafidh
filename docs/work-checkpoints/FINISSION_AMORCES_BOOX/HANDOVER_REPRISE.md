# Handover — reprise immédiate

## Lecture obligatoire

1. `STATUS_CURRENT.md`.
2. `INDEX.md`.
3. Le dernier `CP*.md` terminé ou en cours.
4. `IMPLEMENTATION_PLAN.md`.

## Contrôles avant reprise

```bash
git rev-parse HEAD
git status --short --branch
git log -5 --oneline --decorate
```

Comparer le HEAD à la branche distante et vérifier les CI associées. Ne jamais
rejouer M7-B, M8 180/365/548 ou les 98 tests historiques si aucun fichier
concerné n’a changé.

## Sécurité des données privées

- Les entrées privées sont récupérables hors dépôt ; ne jamais les copier dans
  un chemin suivi par Git.
- Ne jamais afficher, committer ou transmettre la clé AEAD ni le mot de passe
  de signature dans les journaux.
- Le dépôt ne reçoit que du chiffré authentifié, des manifestes sans secret et
  des outils de vérification.
- L’APK, la JKS et leurs secrets restent hors Git et hors artefacts publics.

## Reprise en cas d’interruption

1. Lire la ligne `EN COURS` dans `STATUS_CURRENT.md`.
2. Vérifier le commit du checkpoint précédent dans `INDEX.md` et sur GitHub.
3. Inspecter les fichiers non committés sans les écraser.
4. Reprendre à la rubrique « Reprise exacte » du dernier checkpoint.
5. Exécuter seulement le test interrompu ou invalidé par un changement.
6. Mettre à jour le checkpoint avant toute nouvelle étape.

CP-03 est PASS au commit `709ec47908d3bcdc4f6db1d31a6fa65edebb4f77`
(CI source/JVM `38689001952`, Android `38689001924`, preuves Android
`11951796542`, copie durable `libfile_90ebbaa18b0c81919fe690f11cf04e21`).
La reprise exacte est CP-04 : ouvrir `CP04_VISUEL_MEMORISATION_LIBRE.md`,
vérifier les preuves existantes des 23 pictogrammes et l'indépendance de
Mémorisation libre, sans toucher aux moteurs Hifz, M7-B, M8, Quiz ou QUL.
