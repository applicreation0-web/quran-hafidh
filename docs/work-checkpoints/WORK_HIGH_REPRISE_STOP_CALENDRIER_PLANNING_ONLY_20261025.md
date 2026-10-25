# WORK HIGH — REPRISE APRÈS ARRÊT — QURAN HAAFIDH 1.17.5

## Statut réellement vérifié
- Branche : `work/haafidh-1175-six-day-calendar-audit-20261025`.
- HEAD contrôlé après interruption du Work : `c2127d5cfbe742a5ace2b8da4bdbc2a32e469360`.
- Comparaison GitHub : aucun commit supplémentaire depuis ce HEAD sur cette branche.
- L'autre Work a commencé son analyse mais a été **interrompu volontairement** avant toute correction de code enregistrée sur cette branche. Ses hypothèses ou modifications locales non commitées ne sont **pas** validées.
- Référence contractuelle obligatoire : `docs/releases/HAAFIDH_1175_SIX_DAY_COMPLEMENTARY_CALENDAR_AUDIT_20261025.md`. La version actuelle remplace l'ancienne proposition de « refaire le moteur Itqān ».

## MODIFICATION PRIORITAIRE DU MANDAT
**PLANIFICATION ET AFFICHAGE SEULEMENT. NE PAS REFAIRE OU CHANGER LES MOTEURS HIFZ.**

L'objectif est d'enlever la régression de calendrier « quatre séances Itqān ordinaire sur sept jours » et de **réutiliser l'implémentation historique de 1,5 page/semaine**. Le code contient déjà `PreviewConfig.STABILIZATION_WEEKLY_LINES = 22` et le séquençage **8/7/7** sur trois séances (mardi, jeudi, samedi) ; `HifzPrefs.physicalUnitsInLeg` réutilise `geometry.eligibleWeeklyStabilizationUnit`, sans besoin d'inventer une fenêtre QCF à 1,5 page.

## Contrat sans ambiguïté
1. Six jours **ordinaires** exclusivement lundi–samedi, chacun affecté à une seule famille.
2. Paramètre de jours Sabqi S ∈ [0,6], jours Itqān I = 6−S, et vice versa ; ne PAS rendre ces nombres indépendants.
3. Programme de référence 3/3 : Sabqi lundi/mercredi/vendredi **5 lignes/séance = 15 lignes/semaine** ; Itqān mardi/jeudi/samedi **moteur historique 8+7+7, soit ~1,5 page/semaine**.
4. Dimanche premier régime : **finale Sabqi MATIN + finale Itqān SOIR**, aucune séance Itqān ordinaire.
5. Dimanche second régime : **seulement finale Sabqi**, pas de finale Itqān ni Itqān ordinaire.
6. Aucun dimanche : révision active ou passive, ni Sabqi ordinaire.
7. Itqān premier régime : protocoles de répétition, géométrie, masque, cadence physique inchangés. Itqān second régime : un hizb par séance, ×3 (2 visibles + 1 masquée avec ancrages), inchangé.
8. Modifier uniquement les libellés de navigation : Itqān · Consolidation / Itqān · Maintien de la maîtrise ; supprimer « avant/après An-Nās » sous les icônes, dans les intitulés de journée et dans les sous-titres. Le statut interne persistant reste la source de vérité.
9. Ne pas changer les initialisations d'une installation existante, les bornes/corpus, ni importer les réglages personnels dans les valeurs imposées à une nouvelle installation.

## Périmètre autorisé
`WeeklyProgramPolicy`, réglages facultatifs de cadence / sélection des jours (`HifzPrefs` *uniquement* pour ces préférences et, si absolument nécessaire, le garde de routage vers le moteur existant), `SettingsActivity`, `MainActivity` (Aujourd'hui, raccourcis, finales), `WeeklyDashboardPlanner`, `HifzBacklog`, `WeeklyBilan`, tests de calendrier et contrats UI.

La nouvelle politique de calendrier **datée** peut être utilisée si nécessaire pour préserver les jours anciens, mais elle ne doit introduire **aucun nouveau moteur de répétition ni nouveau format de fenêtre Itqān**. Examiner d'abord l'historique `weeklyProgramTransitionsV1` existant avant d'ajouter des clés.

## Barrière stricte sur les moteurs
Aucun changement à `PreviewConfig.SABQI_LINES`, `PreviewConfig.STABILIZATION_WEEKLY_LINES`, géométrie QCF, moteur de sélection de lignes, `ItqanMaintenancePolicy`, répétitions, masques, promotions et validations `V6`, formats de backup V3/`sourceSnapshot`, moteur Sabqi, processus Atlas, corpus privés.

**Exception étroite** : empêcher qu'une **nouvelle** séance entre par `openOrResumeFrozenItqan` dans le plan 2 pages/4 séances ; rétablir son passage vers le moteur hebdomadaire **historique**. Un `ItqanFourSessionSnapshot` V1 **OPEN** doit toutefois rester repris, sans changement de ses 4 séances, dates, pages, compteur, empreinte ni règles de crédit. Ne pas supprimer les classes de compatibilité `ItqanFourSession*`.

Si un changement de moteur semble nécessaire : **STOP, rapporter le blocage concret, ne pas contourner la règle**.

## Vérifications d'architecture avant tout commit
- Localiser toutes les branches qui traitent le dimanche comme une séance ordinaire et toutes les dépendances à `7−Sabqi` et à `4 Itqān`.
- Examiner `HifzSchedule` historique : ne pas changer sa sémantique pour les anciens programmes.
- Mesurer les conséquences du calendrier nouveau sur `cadenceComplete`, `nextDueCadence`, `todaySessions`, le backlog, `WeeklyBilan`, `DashboardLedger`, la boule de neige `SNOWBALL/SNOWBALL_EXTENDED` et les reçus de finales.
- Déterminer le traitement du basculement de régime à An-Nās : jamais proposer de finale Itqān du second régime ; une finale ancienne OPEN reste reprise, sans crédit anticipé.
- Ne pas effacer du travail local provenant du Work stoppé sans inspection du `git status` et `git diff`. Les éléments non commitées ne sont pas des preuves de correction.

## Plan d'exécution minimisant les risques et crédits Work
1. `git status`, `git log` et lecture de la spécification mise à jour. Pas de nouvel audit général.
2. Tableau des fichiers / dépendances à toucher, et exclusions moteur explicitement contrôlées.
3. Modification atomique de l'ordonnanceur et de son routage vers le moteur préexistant, **sans modifier son algorithme**.
4. Alignement des écrans/paramètres, backlog/bilan, finales et libellés.
5. Tests ciblés S=0..6, I=6−S, référence 3/3, deux régimes du dimanche, semaines traversant des changements, historique sous anciennes politiques, OPEN Itqān V1 4 séances, sauvegarde réelle copiée (309 Acquis, deux OPEN, snowballs), no double-credit, no reset. Ensuite suite globale JVM/source/Android et simulations 180/365/548 jours selon environnement disponible.
6. Compte rendu probant avec HEAD, liste des fichiers modifiés, preuves PASS/FAIL/Non testé et verdict GO/NO-GO.

**Ne pas publier de APK ; ne pas fusionner ; ne pas annoncer de tests non exécutés.**
