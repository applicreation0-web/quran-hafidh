# CP-08F — Ouvertures des sourates et pictogramme Carte
État : **TRIAGE PASS / VALIDATION ÉDITORIALE EN COURS / ICÔNE EN ATTENTE D'APPROBATION**.
Date : 2026-10-15. Référence : branche de CP-08 sur `fcb76642b9e03b1d030212e6cc5cd134ff272f44`.

## Périmètre exact
- La source M7 existante recense **1 903 groupes**. Le tri vise la **première unité de chacune des 114 sourates**, puis les 2e/3e unités lorsque présentes, soit **290 unités au plus sur les trois premiers blocs**.
- Premier tri déterministe sur les **114 groupes initiaux** : **31** titres introductifs/génériques à vérifier en priorité ; **12** candidats demandant vérification de portée ; **24** sans titre conservés (aucune invention) ; **47** titres thématiques plausibles, non certifiés par ce tri.
- Ces nombres désignent une **heuristique de tri**, pas 31 erreurs démontrées ni une validation Darussalam supplémentaire.
- Un rapport PRIVÉ détaillé a été préparé dans les livrables conversationnels, contenant les titres et alternatives ; **NE PAS** copier ce rapport, les titres retenus ou le texte des commentaires dans GitHub public.

## Défauts éditoriaux recherchés
- Une introduction (vertus, récitation, contexte de révélation, hadith historique) préférée à la section traitant réellement des versets de l'unité.
- Exemple de priorité P0 éditoriale : **`IKEN050_001_005`** ; source existante M7 est authentique mais traite de l'introduction, alors qu'un titre de section interne sur 50:1–5 est disponible. Ne pas modifier le titre sans vérification des pages originales et sans validation du remplacement.
- Autres dossiers initiaux prioritaires (identifiants uniquement) : `IKEN017_001_001`, `IKEN021_001_006`, `IKEN037_001_005`, `IKEN039_001_004`, `IKEN048_001_003`, `IKEN089_001_014`, `IKEN092_001_011`, `IKEN095_001_008`, `IKEN109_001_006`.
- Pour chaque unité : comparer versets → toutes rubriques authentiques du même commentaire (M7/Quran.com) → titre littéral dans le volume/page Darussalam → titre le plus représentatif ; ne pas considérer « VALIDÉ » M7 comme une certification du **meilleur emplacement sémantique**.
- Conserver 1 titre fixe maximum, ne pas scinder ni fusionner les 1 903 frontières. En cas de conflit : **LITIGIEUX / NON MODIFIÉ**.

## Critères de clôture
1. 114 premières unités triées, 31+12 dossiers à vérification documentaire systématique ; 24 absences reconsidérées uniquement si la source prouve réellement un titre.
2. Vérifier les unités 2–3 (290 groupes au total), à la recherche d'une introduction traînante ou du titre correspondant en réalité au groupe voisin.
3. Matrice finale des propositions avec : identifiant, intervalle, ancien titre [en privé], nouveau candidat attesté [en privé], volume/page/section, motif et niveau de preuve. Aucune réécriture AI du Tafsir.
4. Reconstruire **hors GitHub** le paquet chiffré seulement après arbitrage, vérifier les 1 903 frontières et le nombre de titres mis à jour, puis lancer les tests pertinents. Ne pas modifier l'APK CP-06 déjà livré avant un nouveau debug validé.

## Icône Carte : proposition, non encore appliquée
- Icône courante : `hifz-app/src/main/res/drawable/ic_ui_semantic_map.xml`, pictogramme historique réseau à trois cases (silhouette organigramme), figé par `docs/FROZEN_UI_ICONS_13_PLUS_10.tsv` et `scripts/verify_contractual_ui_icons.py`.
- **Proposition préférée : Lucide `waypoints`**, succession de points connectés évoquant les groupes graphiques ; alternative Lucide `route`, plus simple à 24 dp.
- L'icône `chart-no-axes-column` de Progression doit rester distincte.
- Attendre choix utilisateur explicite avant de remplacer la ressource. Une fois approuvé : modifier uniquement la ressource Carte, adapter de manière cohérente le manifeste des 23 icônes, la vérification géométrique Lucide et les hashes, conserver les 22 autres, puis vérifier source/JVM/Android et captures BOOX/émulateur. Pas de déguisement des contrats ni désactivation des tests.

## Reprise
Procéder à la vérification documentaire ciblée des premiers blocs ; proposer les titres et cas litigieux à l'utilisateur avant édition du paquet. En parallèle, demander la validation du pictogramme ; ne pas modifier les moteurs Hifz, le Muṣḥaf ni QUL.

## CP-08F complémentaire — titres d'amorces « Israelites » (décision utilisateur du 15 octobre)

- Étendre la vérification terminologique au **corpus entier des clés d'amorces approuvées** et non uniquement aux titres des premiers blocs ou aux thèmes Ibn ʿĀshūr.
- Réaliser d'abord un inventaire privé des titres contenant `Jews`, `Jewish`, `Children of Israel`, `Israelites`, etc. et de leurs ID `IKEN...`, avec référence Ibn Kathīr, passage coranique, contexte, terme original et décision.
- **Modifier le libellé de la clé d'amorce visible à l'écran**, et synchroniser ce même libellé sur la carte graphique, la recherche et l'en-tête cliquable du commentaire lorsque `Israelites` est fidèle aux versets et au contexte. Les textes anglais originaux et les citations ne sont jamais modifiés.
- Préserver `Jews` lorsque l'auteur désigne `al-Yahūd` en tant que communauté religieuse, et ne pas remplacer `People of the Book` lorsque cette expression est la plus fidèle.
- Mettre les variantes approuvées dans la couche d'affichage du corpus privé, avec la preuve éditoriale, puis reconstituer ses huit chunks chiffrés et vérifier les invariants 1 903 groupes / 1 708 titres / 195 sans titre sauf arbitrage utilisateur distinct.
- Tester, pour chaque adaptation : même intitulé stable dans les amorces, la carte et le Tafsir, aucune altération du commentaire original, aucune frontière de verset modifiée, aucune régression des moteurs Hifz. Ne jamais committer les titres en clair sur ce dépôt public.
- **État : À AUDITER ET À APPLIQUER — non encore réalisé dans le paquet privé ni dans l'APK CP-06.**
