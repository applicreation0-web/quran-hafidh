# Quran Haafidh 1.17.5 — contrat du calendrier à six jours (25 octobre 2026)

**Statut : AUDIT / SPECIFICATION UNIQUEMENT — pas encore implémenté. NO-GO APK/release.**

## Décision du propriétaire, source de vérité

- Exactement **six jours ordinaires** possibles : lundi–samedi.
- Jours Sabqi : entier **S dans [0,6]** ; jours Itqān : **I = 6 − S**. Réciproquement, S = 6 − I.
- Tout changement d'un nombre recalcule automatiquement l'autre. **Pas deux réglages indépendants**, pas de septième séance ordinaire le dimanche.
- Répartition de référence pour S = 3 et I = 3 : Sabqi lundi/mercredi/vendredi, objectif **une page QCF (15 lignes normales) par semaine**, avec séances usuelles de 5 lignes ; Itqān mardi/jeudi/samedi, objectif **une page et demie QCF par semaine** (quantité physique exacte déterminée par les lignes et bornes réelles, pas par 22,5 lignes fictives).
- Dimanche, régime Itqān initial : **finale Sabqi matin + finale Itqān soir**, aucune séance Itqān ordinaire et aucune révision active/passive.
- Dimanche, régime Itqān de maintien : **finale Sabqi seulement**, aucune finale Itqān programmée et aucune séance Itqān ordinaire.
- Le passage de régime reste fondé sur l'état déjà persisté à la fin d'An-Nās ; pas de transition inférée des libellés ou de la date.
- UI sans « avant An-Nās » / « après An-Nās » sous les icônes, dans les cartes et les titres de séance ; dénominations fonctionnelles autorisées : Itqān · Consolidation / Itqān · Maintien de la maîtrise.
- Sauvegardes et états historiques : aucune réécriture silencieuse des compteurs, crédits Acquis, curseurs, dates, protocoles SNOWBALL / SNOWBALL_EXTENDED, fenêtres Itqān OPEN.

## Diagnostic des écarts *effectivement lus* au HEAD 6fb3c073

| Source | Écart démontré |
|---|---|
| WeeklyProgramPolicy.java | RECOMMENDED classe dimanche comme STABILIZATION ; cibles 8 lignes mardi et dimanche + 7 jeudi et samedi = 30 lignes / 4 séances. MANUAL_SEVEN classe dimanche STABILIZATION aussi. |
| HifzPrefs.java:3915–3918 | itqanDaysPerWeek = 4 en RECOMMENDED et **7 − learningDaysPerWeek** en MANUAL_SEVEN ; incompatibilité arithmétique. |
| SettingsActivity.java | « Standard · 3 Sabqi / 4 Itqān » ; réglage manuel présente « jours Apprentissage + 7 − jours Stabilisation = 7 ». |
| MainActivity.java | recommendedSunday -> computeNextMode exige ITQAN avant les finales ; todaySessions ajoute une ITQAN ordinaire dimanche ; cadenceComplete exige la même chose. |
| WeeklyDashboardPlanner.java | Prévision itqan 2 pages / 4 slots figés ; dimanche additionne Itqān normal + finales ; dépend du modèle CadenceAction qui n'admet qu'une activité principale par date. |
| HifzBacklog.java | Une dette par jour selon un enum exclusif ; toute reclassification de jours déjà dus serait une régression. |
| WeeklyBilan.java | Même hypothèse mono-action quotidienne ; doit exclure finale/absence de séance ordinaire du dénominateur et préserver les justificatifs. |
| ItqanFourSessionPolicy/Snapshot/Store, HifzPrefs.openOrResumeFrozenItqan | **2 pages / 4 séances** persistées et contrôlées avec empreinte : changer le nombre de slots sur place rendrait des OPEN illisibles. |
| HifzPrefs.weeklySnowballAppendUnits | Plafond lié aux jours Sabqi/Itqān, exception 30 sous-blocs pour fenêtre 4 slots ; à découpler du nombre de segments QCF tout en préservant l'historique non Acquis. |
| HifzSchedule dans hifz-core | CLASSSIC historique se base sur 0..6 jours Sabqi et dimanche révision. Ne pas changer ce core pour « corriger » le nouveau programme. |

## Architecture de correction exigée avant implémentation

1. **Calendrier unique, daté et immutable par période** : modèle de journées ordinaires Mon–Sam, S+I=6, dimanche séparé. Ne pas reclassifier les dates passées, même après une modification de réglages ou une restauration v3/schema6.
2. **Versionnement de politique** : préserver la signification de CLASSIC/RECOMMENDED/MANUAL_SEVEN dans l'historique existant ; les nouveaux réglages utilisent un calendrier versionné à partir d'une date d'effet choisie de manière sûre, jamais un remplacement silencieux des transitions existantes.
3. **Une source de vérité** pour les échéances : générer (date, famille) pour Sabqi et Itqān ; toutes les vues, la sélection Aujourd'hui, l'accès direct, le backlog, le bilan et le garde de validation interrogent cette même source. Ne pas fabriquer de « date acquittée » sans reçu.
4. **Volumes / géométrie QCF** : 15 lignes Sabqi par semaine en référence 3 jours ; 1,5 page Itqān comme fenêtre physique réelle, bornée par le corpus éligible, avec nombre de lignes entier et interdiction de traverser silencieusement les frontières de sourates ; zéro jour = zéro nouveau bloc ordinaire, mais pas effacement des OPEN ou des finals historiques.
5. **Fenêtre Itqān nouvelle version** : nouveau type/payload versionné pour 0–6 séances, jamais changer le décodage des fenêtres ITQAN4 V1. Un ancien snapshot OPEN finit exactement sur ses anciennes 4 dates et répétitions ; pas de redistribution rétroactive.
6. **Finales conditionnées aux reçus** : première phase deux finales dimanche ; seconde phase seule finale Sabqi. Une finale Itqān déjà OPEN lors du changement reste conservée et accessible par reprise explicite, sans être réinjectée comme nouvelle finale.
7. **Conservation de snowball** : quantité de sous-blocs et nombre de séances séparés ; pas de suppression due au passage 4→3, notamment sur frontières de sourates, backlog multi-semaines ou post-An-Nās.
8. **Aucun changement implicite des initialisations** sur installation existante ; tout nouveau choix se programme à date d'effet, aucune nouvelle initialisation ne réécrit une sauvegarde antérieure. Ne pas convertir le programme personnel de l'utilisateur en contrainte obligatoire pour une installation neuve.
9. **Présentation** : libellés Itqān fonctionnels (Consolidation / Maintien de la maîtrise) sans référence An-Nās dans titres/sous-titres. L'interface reflète le régime effectif et ne pilote jamais le régime.

## Matrice de tests obligatoires

- Tous les S=0..6 et I=6−S ; chaque semaine Mon–Sam exactement 6 jours classés une fois, Dim jamais ordinaire.
- Standard 3/3 : LMV Sabqi × 5 lignes = 15 ; MJS Itqān total objectif 1,5 page QCF, limites réelles non dupliquées.
- Dimanche dans chaque régime : révision active/passive absente, Itqān ordinaire absent, finales éligibles exactes (2 puis 1).
- 0 Sabqi/6 Itqān et 6 Sabqi/0 Itqān ; 0 unité éligible ; première semaine ; changement de régime un samedi, un dimanche et au milieu de semaine.
- Semaine à cheval sur changement de programme, dette antérieure, plusieurs semaines non exécutées, redémarrage et restauration.
- Fenêtre Itqān V1 4 dates ×40 à mi-répétition, SNOWBALL/EXTENDED déjà OPEN, collision de finales, vérification stricte de fingerprint, sourceSnapshot et codec V3.
- Refus de double crédit, d'avancement anticipé, de recul de répétitions, de perte de réception, de fail-open masqué.
- Contrats de libellés pour Accueil, Paramètres, Parcours, Aujourd'hui, bilan, frise et fenêtres finales.
- JVM, source contracts, Android instrumentation, tests longitudinaux 180/365/548 jours, comparaison de copie réelle de sauvegarde ; final APK privé signé uniquement après GO.

**Garde de publication : aucune APK signée, aucune fusion vers la branche stable, aucune réécriture des initialisations tant que cette matrice n'est pas verte.**
