# Quran Haafidh — protocoles de gel F0 / F1 / F2 (7 novembre 2026)

## Contexte et ordre impératif
- **F0** — base source validée 1.17.13/code51, SHA immuable `331b07173f7082eece22f557b53e70686280e5af`, branche `freeze/haafidh-11713-f0-20261107`.
- **F1** — 1.17.13/code51, branche `freeze/haafidh-11713-f1-20261107`, PR #14 contre F0. Ne modifier que les surfaces de navigation/présentation.
- **F2** — 1.17.14/code52, branche `freeze/haafidh-11714-f2-20261107`, PR #15 contre F1. Ne pas publier F2 sans intégration et validation préalables de F1.
- Aucune de ces branches n'est une APK privée installable. Toute livraison demeure conditionnée à sept corpus éditoriaux privés, à la signature historique du package `com.quransafeguard.hifz` et à un essai de mise à jour sans perte de données.

## F1 : périmètre autorisé
1. Rendre le calendrier lisible : frise de sept jours **aujourd'hui → J+6**, vocabulaire graphique Atlas (trait et points), **aucun titre Atlas** ; états futurs jamais « Validé » et seuls les reçus Hifz validés autorisent une coche.
2. Fournir une sélection tactile directe du jour et afficher les vrais détails matin/soir, dont le dimanche/les finales.
3. Alléger les espacements verticaux du Parcours sans changer les six pictogrammes, les couleurs ou les parcours.
4. Afficher une action explicite sur l'accueil, conserver sa zone tactile minimale de 120 dp.
5. Augmenter la lisibilité des **mots-clés éditoriaux anglais existants** des refrains (4/31/10), sans reprendre les thèmes/titres Atlas.
6. Le moteur Hifz, la géométrie Mushaf, les crédits, les reports et les règles de finales ne changent pas.

## F2 : périmètre autorisé
1. **Progression principale** : exactement 30 juz officiels, chacun avec une jauge monochrome et son pourcentage de **versets entièrement Acquis** (toutes les lignes physiques authentiquement créditées dans `progressionSnapshotV6().acquired`). Aucun crédit anticipé pour Sabqi, Itqān, répétitions, états LEARNED ou STABILIZED. Les bornes des juz dérivent des 240 rubʿ du fichier `QuranRubBoundaries` et non de 30 tronçons de 20 pages inventés. **Un tap sur Juz n → ses pages QCF numérotées → un tap sur page → Lecture** ; Retour ramène aux 30 jauges. Pas de grille murale 604 pages par défaut, pas de dialogue de choix de juz, pas de regroupement inventé. Les quatre statuts graphiques existants restent inchangés sur chaque page.
2. Bandeau de séance : améliorer les tailles de police du texte du mode, de la progression **réelle** et du chronomètre ; ne pas créer de compteur artificiel.
3. Comparaison des folios : libellés explicites Agrandir/Réduire A/B, Tafsīr encore repliable et défilable ; état de comparaison restaurable.
4. Repères Similitudes / Correspondances : réserver une marge extérieure si une seule famille, deux marges si deux familles ; conserver l'authentique ratio QCF 345:550 et 44 px tactiles. Aucun chevauchement du texte arabe.
5. Version code52/name1.17.14, tests source/Android, garde de signature hors ligne `build_11714_private_offline.py` avant tout projet de publication.

## Preuves visuelles
La **source autoritative** des captures est l'exécution Android instrumentée du workflow `.github/workflows/haafidh-freeze-boox-visual.yml`, profil émulé BOOX portrait 1404×1872 / 226 dpi. Ne présenter aucune maquette comme une capture réelle. Les preuves doivent comprendre accueil, calendrier, vue des 30 jauges, Juz 1 et Juz 30 (F2), Hifz, frises al-Qamar/ar-Rahman/al-Mursalat, et comparaison.

## Conditions GO / NO-GO
- CI source/JVM complète et classes Android instrumentées sur les commits finaux F1 et F2.
- Captures BOOX réelles inspectées, page QCF non rognée, pas d'icônes superposées à l'encre, textes lisibles.
- Zéro modification non démontrée des règles Hifz ; pas de validation fantôme, ni perte de curseur, ni mutation par consultation des frises, du calendrier ou de la progression.
- Ressources privées authentifiées par les hashes et signature historique identique avant/après installation en place.
- En cas de preuves manquantes, conserver les PR en brouillon : **NO-GO publication**.
