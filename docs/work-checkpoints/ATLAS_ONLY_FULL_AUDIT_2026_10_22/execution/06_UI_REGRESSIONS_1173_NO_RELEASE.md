# Quran Haafidh 1.17.3 — Corrections après retour BOOX, audit avant toute release
Date : 22 octobre 2026. Dépôt public `applicreation0-web/quran-hafidh`, branche `work/haafidh-1171-atlas-only-pre20-mask`.
**Statut impératif : PAUSE DE LIVRAISON. AUCUN NOUVEL APK SIGNÉ NI RELEASE GITHUB AUTORISÉ À CETTE ÉTAPE.**

## Défauts constatés sur captures fournies par l'utilisateur
1. Menu Révision : étiquette « Révision active · 15 min » alors que le moteur de séance et l'Entretien exigent 30 min initiales puis 45+ min à 15 juz. **Cause** : littéral conservé dans `RevisionSelector.show`. Correction au commit `caac8031` : même `maintenanceCue` que l'Entretien. Un test source réaffirme la parité de calcul et les deux affichages.
2. Révision active / Mushaf QCF : chaque amorce visible portait un cadre rectangulaire gris. **Cause prouvée** : `ayah_themes_display/review_titles.js` montait explicitement une balise SVG `rect` avec `stroke='currentColor'` et `stroke-opacity=0.45`, servant de cible tactile de titre. Ce contour est indépendant des six repères QCF et des trous `hifz-exact-word-holes`.
   - Correction `e0691113` : conserver exactement les boîtes QCF comme hit-zones SVG `pointer-events=all` et la propagation du toucher du verset et du titre, mais appliquer `stroke=none`, largeur et opacité nulles, `fill=transparent`. Les vrais mots, le masque et les trous SVG restent inchangés ; aucun nouveau texte coranique, aucune géométrie reconstruite.
   - Test JS `scripts/test_ayah_themes_review_overlay.js` : rejette toute bordure, vérifie les touches, le rejet des rectangles invalides et l'immutabilité des nœuds masque. Contrôle Node exécuté dans `scripts/guard_frozen_hifz_engines.sh`.
   - Test Android ajouté au `AyahThemesReviewDisplayInstrumentedTest` : vérifie les attributs SVG des cibles et l'identité stricte des couches masque/trous avant et après montage.
3. Apprentissage : frise hebdomadaire dépourvue des compteurs 1/3, 2/3, 3/3. **Cause** : `WeeklyDashboardPlanner` ne calculait des compteurs que pour les quatre séances de Stabilisation, pas pour les trois jours d'Apprentissage du programme recommandé.
   - Correction `e0691113` : libellé `séance 1/3` lundi, `2/3` mercredi, `3/3` vendredi, remis à zéro le lundi suivant ; intégration aux lignes planifiées et validées. Le calendrier historique CLASSIC et manuel ne reçoivent aucun faux compteur.
   - Test `WeeklyProgramPolicyTest` : 4 semaines consécutives, L/M/V, pas de libellé les jours sans Apprentissage, ancien programme sans changement.
   - Compteurs purement **présentation**, sans écriture de curseur, statut d'achèvement, validation d'Itqān ou de Sabqi.

## Périmètre du contrôle non-régression
- Sources moteurs `hifz-core`, `HifzPrefs`, calendrier du programme, états Itqān ×40 / post-An-Nās ×3 / anciennes ×4, anciennes acquisitions, crédits Sabqi, Consolidation, Révision active/passive et sauvegardes V6 : aucune mutation introduite par ces correctifs UI.
- Mushaf SVG partagé, 604 folios QCF, lecteur Hifz JS/HTML, masque, trous de mots, curseurs Hifz et géométrie : identiques à l'état avant ces correctifs ; seul le calque tactile indépendant des titres perd son trait visible.
- Audit Hifz/Atlas/QCF initial du correctif UI : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39850981391
- Suite Android originale, avec masquage et journal de sauvegarde : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39850980911
- Compilation de contrôle (non publiée) : **PASS** https://github.com/applicreation0-web/quran-hafidh/actions/runs/39850979830
- Audit Android élargi (écran réel, SVG invisible, migrations V6, reprise après mort du processus) : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39851925203 — résultat à relever explicitement à clôture.
- Contrôle Hifz/Atlas pour l'audit élargi : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39851925354 — résultat à relever explicitement à clôture.

## Limitations et décision
- Les captures utilisateur prouvent les anomalies de la version installée, mais **pas** leur disparition physique après correction : aucun nouvel APK n'est envoyé ni installé en respect de la pause.
- La suppression du trait SVG est testable numériquement ; l'absence de ghosting et l'ergonomie tactile sur la BOOX réelle restent à vérifier avec un futur candidat accepté.
- Pas de release publique dans le dépôt GitHub public contenant les sept ressources privées sans vérification des droits de redistribution ; la clé historique ne doit jamais être téléversée.
- **NO-GO pour l'APK/release tant que l'audit natif élargi n'est pas clos et que l'utilisateur n'a pas levé la pause sur les APK.**
