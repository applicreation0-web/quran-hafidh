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

CP-02 est PASS au commit `22d4d43eb28715936c73eae93af1e05de6bc5c10`
(CI source/JVM `38626984145`, Android `38626983992`). La reprise commence à
CP-03 par l'inventaire différentiel des deux références graphiques et l'écriture
des tests du parcours carte ; ne pas relancer M7-B ni les simulations M8.
