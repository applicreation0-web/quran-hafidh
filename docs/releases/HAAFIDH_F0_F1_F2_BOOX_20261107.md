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
1. **Progression** : exactement 30 silhouettes de pages QCF, chacune remplie verticalement en fonction des lignes physiques réellement effectuées / lignes totales du juz, sans barre extérieure. Trois niveaux de remplissage exclusifs et réels : apprentissage = points, stabilisation = hachures, acquis = noir, non commencé = vide. Répartition par frontières canoniques `QuranRubBoundaries`, sans double compte d'une ligne. **À l'écran, seulement « Juz N » sous chaque page miniature** : ni pourcentage, ni nombre de lignes, ni plages ou numéros de pages. Tap Juz → ses pages représentées par des cases au statut visuel conservé, sans chiffres imprimés → tap page → Mushaf exact. Aucun libellé explicatif généré ou redondant. Les estimations de dates et lignes restantes ne figurent plus sur Progression (Diagnostic les conserve). Les données réelles ne sont jamais modifiées par la consultation.
2. Bandeau de séance : améliorer les tailles de police du texte du mode, de la progression **réelle** et du chronomètre ; ne pas créer de compteur artificiel.
3. Comparaison des folios : libellés explicites Agrandir/Réduire A/B, Tafsīr encore repliable et défilable ; état de comparaison restaurable.
4. Repères Similitudes / Correspondances : réserver une marge extérieure si une seule famille, deux marges si deux familles ; conserver l'authentique ratio QCF 345:550 et 44 px tactiles. Aucun chevauchement du texte arabe.
5. Version code52/name1.17.14, tests source/Android, garde de signature hors ligne `build_11714_private_offline.py` avant tout projet de publication.

## Preuves visuelles
La **source autoritative** des captures est l'exécution Android instrumentée du workflow `.github/workflows/haafidh-freeze-boox-visual.yml`, profil émulé BOOX portrait 1404×1872 / 226 dpi. Ne présenter aucune maquette comme une capture réelle. Les preuves doivent comprendre accueil, calendrier, les 30 pages-jauges à 0 %, une fixture de remplissages partiels clairement étiquetée comme données de test, Juz 1 et Juz 30 (F2), Hifz, frises al-Qamar/ar-Rahman/al-Mursalat, et comparaison.

## Conditions GO / NO-GO
- CI source/JVM complète et classes Android instrumentées sur les commits finaux F1 et F2.
- Captures BOOX réelles inspectées, page QCF non rognée, pas d'icônes superposées à l'encre, textes lisibles.
- Zéro modification non démontrée des règles Hifz ; pas de validation fantôme, ni perte de curseur, ni mutation par consultation des frises, du calendrier ou de la progression.
- Ressources privées authentifiées par les hashes et signature historique identique avant/après installation en place.
- En cas de preuves manquantes, conserver les PR en brouillon : **NO-GO publication**.
