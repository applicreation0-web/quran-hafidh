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

CP-05 est PASS au commit `f22bfcdd8ff66860c55828eac27cc5738fad0fba`.
L'arbre applicatif gelé `92cb64b26c43d61db4e4605139fb6a725781fec6`
est celui déjà validé par les CI CP-04 `38690267562` et `38690267503` ; CP-05
n'a modifié que deux harnais locaux et n'a pas rejoué M8.

CP-06 est **EN COURS**. La clé AEAD et la JKS M9 sont retrouvées et le certificat
attendu est vérifié. L'ancien APK récupéré est tronqué et ne doit pas être
utilisé. Le workflow public existant est épinglé au HEAD CP-05 exact
`6db0e342684344634892d7da4b3c695a3138a232` et à l'arbre applicatif
`92cb64b26c43d61db4e4605139fb6a725781fec6`.

Reprise exacte : déclencher manuellement `Quran Haafidh CP06 — intermediate APK
only, no private titles`, attendre son résultat réel et récupérer l'artefact
sans titres. Ensuite seulement, reconstruire les huit chunks hors dépôt,
injecter, aligner et signer localement. Ne jamais envoyer le corpus clair, la
clé, le mot de passe ou l'APK vers un artefact GitHub public.
