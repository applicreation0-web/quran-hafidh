# Audit pilote — checkpoint préparatoire INCOMPLET

Ce document n'atteste ni l'achèvement du pilote ni une application fonctionnelle.

## État précis

- Base 1.17 et branche exclusive vérifiées.
- Audit technique statique écrit ; aucune modification de l'application Android.
- Dix sourates candidates : 1, 2, 4, 36, 37, 50, 67, 103, 108, 112.
- Quatre sections Qaf contrôlées sur les scans, titres et bornes.
- Zéro sourate entièrement validée AR/EN ; zéro traduction anglaise validée.
- Les axes et les neuf autres ensembles de sections restent NULL/non résolus.
- Aucun corpus Al-Munir/Al-Wasit réutilisé.
- Prototype, captures et APK : non produits. Le prérequis de validation du corpus
  prévu par la mission n'est pas satisfait ; aucune activation UI prématurée.

## Validation automatique

`python3 -m unittest discover -s scripts/semantic_map -p 'test_*.py'`

Huit tests réussis : données valides et immuabilité ; références invalides ; bornes
inversées ; fin de verset sur deux pages ; ordre/trou/chevauchement/doublon ; champs
manquants ; anglais non résolu ; doublon de sourate et absence de sections.

`python3 scripts/semantic_map/validate.py docs/semantic-map/SEMANTIC_MAP_PILOT_DATA.json app/src/main/assets/reader109/geometry.json`

Résultat attendu à ce stade : code 1, publishable=false. Les constats sont enregistrés
dans VALIDATION_FINDINGS.json : 56 constats bloquants (10 axes arabes manquants,
14 anglais non résolus, 14 anglais manquants, 9 ensembles de sections absents,
9 couvertures incomplètes). Ce refus est une protection correcte, pas un corpus
validé. Le mapping repose sur la géométrie de la base, première page du début et
dernière page de la fin. Aucun correctif automatique des données.

Limites : validateur de données structurées, pas analyse sémantique ; la hiérarchie
multi-niveaux devra être validée niveau par niveau, pas comme une liste aplatie.

## Dix questions critiques

| Question | Réponse à ce stade |
|---|---|
| Régularité des 114 sourates ? | Non établie ; variations déjà observées |
| Représentation uniforme ? | Une liste de sections unique ne convient pas à tous les cas |
| Complexité ? | Hiérarchie d'al-Baqarah à préserver ; afficher un niveau à la fois |
| Titres longs ? | Oui, notamment an-Nisa ; texte repliable, jamais réécrit pour tenir |
| Bornes explicites ? | Qaf oui ; autres cas à contrôler ; niveaux divergents dans al-Mulk |
| Anglais publié fidèle ? | Aucun intitulé accepté comme traduction exacte |
| Traduction ou paraphrase ? | Distinction nécessaire ; article sur la méthode insuffisant |
| Carte → Mushaf intuitif ? | Pas encore testé dynamiquement |
| Compréhension accrue ? | Hypothèse, à tester avec utilisateur ; aucun bénéfice démontré |
| Confusion Amorces/Munir ? | Risque réel ; provenance visible, corpus et vue séparés |

## Décision provisoire

**GO_WITH_CHANGES pour poursuivre la recherche uniquement.**
L'intégration et l'extension 114 sourates restent bloquées. Ce n'est PAS la décision
finale de fin de pilote demandée par la mission.

Changements de conception nécessaires : hiérarchie explicite ; types introduction,
section, conclusion ; niveaux de source distincts ; état sans découpage documenté ;
axes parfois développés, sans résumés présentés comme citations de l'auteur.

Prochain apport requis : PDF arabes fournis par l'utilisateur, afin de produire le
corpus étendu d'intitulés et leurs traductions. Ensuite, finir l'extraction des dix
sourates et les contrôles éditoriaux, puis seulement le prototype indépendant.

## Anti-régression et builds

Aucun fichier existant modifié. Ni moteur, ni renderer, ni navigation existante,
ni version, ni Manifest modifiés. Cela est vérifiable par diff ; ce n'est pas une
preuve de tests dynamiques. Tests Android/JVM et démarrage non exécutés ; Gradle et
SDK non présents lors de l'inspection. BOOX physique, téléphone et paysage : non testés.

## Diff

Ajouts uniquement : cinq livrables nommés par la mission, plan, résultats JSON,
validateur et huit tests. Documentation et outillage de recherche uniquement.
Aucune release, aucun tag, aucune PR, aucune branche Claude ou main modifiés.
