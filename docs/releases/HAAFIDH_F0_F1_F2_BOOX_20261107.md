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
1. **Progression** : 30 vignettes en **format page QCF**, sans barre horizontale. Chaque page se remplit verticalement au prorata des **lignes physiques effectivement traitées / toutes les lignes du juz** (et non en fonction du nombre de versets). Les trois niveaux réels et exclusifs de `progressionSnapshotV6()` sont conservés : appris = points ; stabilisé = hachures ; acquis = noir plein ; non commencé = vide. Aucune séance planifiée, simple lecture, répétition non validée ou plage déclarée ne crée un crédit. Une ligne physique à cheval sur deux juz appartient à celui de son premier verset et ne compte jamais deux fois. Référence canonique des 30 bornes : `QuranRubBoundaries`. **Juz → pages physiques numérotées → Lecture** ; Retour ramène aux 30 vignettes. Aucun sélecteur modal, aucune catégorie supplémentaire. Sur les deux écrans, pas de consigne redondante de type « toucher pour afficher/ouvrir » : conserver seulement « Progression », « Juz N », pourcentage, lignes effectuées/total et les véritables statuts graphiques des pages.
2. Bandeau de séance : améliorer les tailles de police du texte du mode, de la progression **réelle** et du chronomètre ; ne pas créer de compteur artificiel.
3. Comparaison des folios : libellés explicites Agrandir/Réduire A/B, Tafsīr encore repliable et défilable ; état de comparaison restaurable.
4. Repères Similitudes / Correspondances : réserver une marge extérieure si une seule famille, deux marges si deux familles ; conserver l'authentique ratio QCF 345:550 et 44 px tactiles. Aucun chevauchement du texte arabe.
5. Version code52/name1.17.14, tests source/Android, garde de signature hors ligne `build_11714_private_offline.py` avant tout projet de publication.

## Preuves visuelles
La **source autoritative** des captures est l'exécution Android instrumentée du workflow `.github/workflows/haafidh-freeze-boox-visual.yml`, profil émulé BOOX portrait 1404×1872 / 226 dpi. Ne présenter aucune maquette comme une capture réelle. Les preuves doivent comprendre accueil, calendrier, les 30 pages-jauges, Juz 1 et Juz 30 (F2), Hifz, frises al-Qamar/ar-Rahman/al-Mursalat, et comparaison.

## Conditions GO / NO-GO
- CI source/JVM complète et classes Android instrumentées sur les commits finaux F1 et F2.
- Captures BOOX réelles inspectées, page QCF non rognée, pas d'icônes superposées à l'encre, textes lisibles.
- Zéro modification non démontrée des règles Hifz ; pas de validation fantôme, ni perte de curseur, ni mutation par consultation des frises, du calendrier ou de la progression.
- Ressources privées authentifiées par les hashes et signature historique identique avant/après installation en place.
- En cas de preuves manquantes, conserver les PR en brouillon : **NO-GO publication**.
