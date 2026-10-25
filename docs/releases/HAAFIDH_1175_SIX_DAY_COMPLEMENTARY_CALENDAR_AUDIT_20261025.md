# Quran Haafidh 1.17.5 — audit et contrat définitif : PLANIFICATION UNIQUEMENT (25 octobre 2026)

**Statut : cahier de correction, aucun changement applicatif réalisé. NO-GO release.**
**RECTIFICATIF PRIORITAIRE : ce document remplace entièrement la spécification antérieure « refactorisation du moteur / nouvelles fenêtres Itqān ». L'utilisateur interdit cette refonte.**

## 1. Contrat utilisateur

- Six jours ordinaires : **lundi à samedi** ; un seul type principal par jour.
- L'utilisateur règle les jours Sabqi entre **0 et 6** ; jours Itqān = **6 - Sabqi**, et inversement. Paramétrage lié, jamais indépendant.
- Programme de référence : **Sabqi lundi, mercredi, vendredi (3 × 5 lignes, 1 page/semaine)** ; **Itqān mardi, jeudi, samedi (programme historique ~1,5 page/semaine)**.
- Le volume de 1,5 page Itqān est **déjà implémenté historiquement** : **ne pas recréer de modèle QCF, de plan de répétition ni de moteur**.
- Premier régime Itqān : **dimanche matin finale Sabqi, dimanche soir finale Itqān**.
- Second régime Itqān : **dimanche finale Sabqi seulement** (pas de finale Itqān).
- Aucun dimanche : séance Itqān ordinaire, séance Sabqi ordinaire, révision active ou révision passive.
- La bascule de régime reste exclusivement fondée sur le statut Itqān persistant déjà validé ; aucune déduction à partir de la date, du libellé ou de la vue.
- Titres/sous-titres des cartes sans « avant/après An-Nās » ; terminologie fonctionnelle : **Itqān · Consolidation** et **Itqān · Maintien de la maîtrise**. L'explication technique peut rester dans l'aide.
- En cas de 0 jour pour une famille : aucune nouvelle séance ordinaire de cette famille ; la finale n'est proposée que si des unités historiques éligibles existent, sans crédit fictif.

## 2. Preuve : le programme historique 1,5 page existe déjà

**Comparaison de code directement vérifiée** sur le commit 1.17.4 `8e7b132d7e075bc3e6f8f1eb6216b48e7178c95e` et le HEAD 1.17.5 `6fb3c073f6b11ee524f7733152b59d0d0cffc55d` :

- `hifz-app/.../PreviewConfig.java` : `STABILIZATION_WEEKLY_LINES = 22`, commentaire : **trois séances 8/7/7 mardi/jeudi/samedi = ~1,5 page/semaine**.
- `HifzPrefs.physicalUnitsInLeg` utilise toujours `geometry.eligibleWeeklyStabilizationUnit(...)` pour le premier régime ; **un hizb** dans le second.
- `HifzSessionActivity.renderItqan` a toujours le **parcours historique de sélection** après le branchement conditionnel `openOrResumeFrozenItqan`.
- **Régression de planification introduite séparément** : `ItqanFourSessionPolicy/Snapshot/Store` met à disposition 2 pages / 4 séances, le mode RECOMMENDED inclut le dimanche comme quatrième Itqān, le mode MANUAL_SEVEN calcule `7-learningDaysPerWeek`.
- **Nuance physique** : 22 lignes est la cible historique arrondie (~1,5 page) ; ne pas fabriquer de demi-ligne ni élargir la plage hors corpus. Conserver les limites QCF du moteur.

## 3. Frontière stricte des modifications

**Autorisé** : uniquement **l'ordonnanceur / configuration datée de jours**, le routage existant vers le programme Itqān historique, les menus, l'écran Aujourd'hui, la frise, le bilan, les reports et les tests liés au calendrier.

**Interdit** : modifier les fonctions de répétition, masquage, sélection des versets/lignes, reconnaissance QCF, moteur de crédit, finale ×3, promotion Acquis, règle ×40, règle hizb ×3, sabqi 5 lignes, volumes du premier régime, rotation, données Atlas, bootstrap/init existants, protocole de sauvegarde, format OPEN, mécanismes de neige. **Ne pas modifier HifzSessionActivity, ItqanMaintenancePolicy, ItqanRegimeStore, GeometryRepository ni le core Hifz à des fins de calendrier.**

**Exception encadrée** : si la sélection automatique du plan 4 séances depuis `HifzPrefs.openOrResumeFrozenItqan` l'exige, on peut adapter **seulement le garde de sélection du plan**, hors moteur : à plan 4 OPEN existant, reprendre exactement le snapshot V1 ; pour les NOUVELLES séances 3/3, laisser le sélecteur historique de ~22 lignes s'exécuter. Toute nécessité de modifier davantage le moteur impose d'arrêter cette mission et d'exposer le blocage, **pas** de contourner l'interdiction.

Ne pas supprimer les classes `ItqanFourSession*` nécessaires à la lecture des anciennes sauvegardes, même si elles ne sont plus sélectionnées pour de nouvelles semaines. Ne pas « nettoyer » des fonctions historiques sans preuve d'inutilisation : éviter les régressions collatérales.

## 4. Diagnostic des points de planification à aligner

1. `WeeklyProgramPolicy` : six jours uniquement ; 3/3 standard ; dimanche dédié aux finales. Conserver une interprétation versionnée de l'historique RECOMMENDED/MANUAL_SEVEN, sans reclassifier les dates passées.
2. `HifzPrefs.itqanDaysPerWeek` et UI `SettingsActivity` : **6 - Sabqi**, pas 7 - Sabqi ; réglage unique 0..6.
3. `MainActivity` : suppression de l'Itqān ordinaire du dimanche dans `recommendedSunday`, `computeNextMode`, `cadenceComplete`, `todaySessions`, `refreshToday` et les accès directs ; finale Itqān conditionnée au **premier** régime seulement.
4. `WeeklyDashboardPlanner` : afficher le planning historique ~22 lignes / 3 jours pour les nouvelles semaines et les révisions matin ; aucune prévision fictive 2 pages/4 jours ; anciens OPEN gardent leurs informations d'origine.
5. `HifzBacklog` et `WeeklyBilan` : une seule source de dates et de justificatifs ; pas de séance 7e jour créée, pas de dette ou validation inventée par des transitions.
6. `HifzPrefs` : ne toucher, si nécessaire, qu'aux clés **facultatives de calendrier** et au **garde de sélection** du plan 4; ne pas réécrire les valeurs personnelles ou les initialisations.
7. Terminologie cohérente dans Accueil, Parcours, Aujourd'hui, frise, Paramètres, finales. Les changements sont **de surface uniquement**.

## 5. Sécurité avant changement

- Une période historique garde sa politique originelle et ses reçus ; un changement de répartition ne transforme jamais des dates en dette fictive.
- Une séance déjà commencée, notamment `ItqanFourSessionSnapshot` V1 à 4 dates, reste reprise **sans changer quota, pages, répétitions, empreinte ou curseur**. Les anciennes fenêtres ne sont ni effacées ni converties.
- Aucun progrès Acquis/Appris/Stabilisé ni aucune unité SNOWBALL/SNOWBALL_EXTENDED ne doit être perdu ou crédité deux fois.
- Respect du choix précédent de l'utilisateur : **ne pas toucher aux initialisations**. Préserver son installation par mise à jour ; ne pas projeter arbitrairement sa répartition sur une installation neuve.

## 6. Tests obligatoires : même moteur, nouveaux jours

- Tester les 7 couples S/I (0/6, 1/5, 2/4, 3/3, 4/2, 5/1, 6/0) et vérifier **exactement 6 jours ordinaires**, pas 7.
- Confirmer sur QCF du moteur **déjà existant** : Sabqi 3 × 5 lignes, Itqān 8+7+7 lignes, limites exactes, pas de double comptage ni dépassement de sourate.
- Dimanche premier régime = deux finales conditionnées aux unités éligibles ; dimanche second régime = finale Sabqi seule ; aucun Itqān ordinaire ni révision passive/active.
- Comparaison comportementale des moteurs sur les mêmes plages avant/après : **mêmes 37 / 40 / 3 répétitions applicables, mêmes masques et crédits**, aucune modification de l'algorithme.
- Replay sans écriture sur une copie de la sauvegarde réelle du 25 octobre 2026 (309 Acquis, séances OPEN Sabqi/Itqān, SNOWBALL, SNOWBALL_EXTENDED) + états après interruption/restart et autres sauvegardes v3/schema6.
- Transitions en milieu de semaine, backlog passé, 0 jour, migrations et régime basculant pendant une semaine ; ne pas anticiper un progrès futur.
- Regarder les tests historiques qui encodent le **4 Itqān/7 jours** : remplacer seulement leurs attentes de calendrier ; NE PAS réduire les tests moteurs.
- JVM, source contracts, Android instrumentation, simulations longitudinales 180/365/548 jours ; matrice PASS/FAIL avec preuves précises.

**Fin de mission** : commits ciblés en branche isolée, preuves et GO/NO-GO technique. **AUCUN APK, aucune fusion/release sans accord.**
