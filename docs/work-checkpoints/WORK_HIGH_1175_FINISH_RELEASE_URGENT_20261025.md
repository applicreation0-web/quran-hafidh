# WORK HIGH — QURAN HAAFIDH 1.17.5
## Reprise urgente du Work arrêté · Planification uniquement · Finalisation release conditionnelle
**Date de référence : dimanche 25 octobre 2026. Priorité à l'exécution immédiate.**

### 0. POINT DE DÉPART — IMPORTANT
Dépôt `applicreation0-web/quran-hafidh`.
- GitHub branche documentaire `work/haafidh-1175-six-day-calendar-audit-20261025`, HEAD contrôlé **`785f8d55054d41103ef22d48c6e860824f11006d`**. Ce commit contient les derniers ajouts sur l'affichage obligatoire de toutes les sourates/versets.
- Work arrêté : branche **LOCALE NON POUSSÉE** `work/haafidh-1175-planning-only-20261025`, base `bf24f51083a9a34e6fe2fcb0d0306db918b423e2`, avec modifications **non commitées**. Aucun push de cette branche visible sur GitHub. **Reprendre le même espace Work si possible ; préserver impérativement `git status`, `git diff`, les fichiers non suivis et logs avant checkout/reset/rebase. NE PAS refaire de zéro si les modifications locales subsistent.**
- Rapport Work arrêté : `WORK_HIGH_PLANNING_ONLY_BLOCKED_20261025.md`, remis dans la conversation, **NO-GO**, 17/18 tests Itqān ciblés PASS, 1 FAIL critique; 839 tests globaux, 15 FAIL, tests Android et nouvelles simulations non effectués. Récupérer le fichier dans les pièces jointes Work ou utiliser les détails précis ci-dessous ; ne pas supposer ce rapport présent sur GitHub.
- Sources de contrat : `docs/work-checkpoints/WORK_HIGH_REPRISE_STOP_CALENDRIER_PLANNING_ONLY_20261025.md` et `docs/releases/HAAFIDH_1175_SIX_DAY_COMPLEMENTARY_CALENDAR_AUDIT_20261025.md` (lecture et réconciliation des ajouts après base locale `bf24f51`). Les textes antérieurs demandant une refonte du moteur Itqān **sont annulés**.

### 1. MISSION, PRIORITÉS ET CONTRAINTES
L'utilisateur demande de **terminer au plus vite les corrections afin de disposer d'une release 1.17.5**. Ne pas transformer « dans 1 heure » en permission de sauter une preuve ou d'annoncer une publication fictive. Avancer immédiatement, sans longues reconfirmations, mais appliquer le verrou GO/NO-GO.

**CONTRAT DE FONCTIONNEMENT**
- **6 jours ordinaires lundi-samedi** ; exactement **S jours Sabqi (0..6) et I jours Itqān (6-S)** ; deux contrôles UI couplés si nécessaire. Jamais 7 jours ordinaires ni jour ordinaire le dimanche.
- Programme 3/3 de référence : Sabqi lundi/mercredi/vendredi, **5 lignes chacune = 15 lignes/semaine** ; Itqān mardi/jeudi/samedi, **ancien programme 22 lignes/semaine (8+7+7, ~1,5 page QCF)**.
- Dimanche **premier régime Itqān** : finale Sabqi **MATIN**, finale Itqān **SOIR**. **Second régime Itqān** : finale Sabqi **seule**. Jamais de séance Itqān/Sabqi ordinaire ni Révision active/passive le dimanche.
- **Itqān historique de 1,5 page EXISTE DÉJÀ** : `PreviewConfig.STABILIZATION_WEEKLY_LINES=22` ; `GeometryRepository.eligibleWeeklyStabilizationUnit` ; `HifzPrefs.physicalUnitsInLeg` ; **réutiliser, ne pas reconstruire**. Second régime Itqān garde **1 hizb par séance ×3 (2 visibles + 1 masquée, amorces)**. Première phase Itqān garde ses répétitions originales ×40 ; Sabqi ×37 ; finale par page QCF ×3 (1 visible + 2 masquées) ; boules de neige existantes inchangées.
- **Aucun changement aux moteurs de répétition, masque, sélection des lignes QCF, crédits/progression, rotation globale, rétrocompatibilité backups V3/schema6, init, ressources Atlas.** Seules planification, routage de calendrier, interface, présentation, backlog, bilan et gardes de compatibilité indispensables sont autorisés. Une exception explicite et LIMITÉE est autorisée : **adaptateur physique de compatibilité V1→sélecteur historique**, uniquement si sans mutation de protocole ni réécriture des OPEN ; sinon STOP avec preuve.
- Programmes et dates antérieurs inchangés, nouvelles répartitions à partir de la date d'effet sûre (lundi suivant, si nécessaire), sans imposer les choix personnalisés à une nouvelle installation.
- Libellés fonctionnels **Itqān · Consolidation** / **Itqān · Maintien de la maîtrise**. Ne pas écrire « avant/après An-Nās » en titres/sous-titres ; uniquement information technique interne/aide.

### 2. BLOQUEUR NUMÉRO 1 — CONTINUITÉ PHYSIQUE V1→HISTORIQUE
Échec exactement reproduit : `ItqanFourSessionStoreTest.finalFourDateCreditPinsPhysicalCursorWithoutLosingTheFollowingLine`.
- Ancien OPEN V1 quatre séances pages 520–521, clôture par API de production ×40.
- `itqanFourNextPhysicalLineIdV1 = 522:0` mais `physicalUnitsInLeg` + `ItqanRotationPolicy.pick` reprend **`522:6`** : 6 premières lignes sautées.
- **Corriger d'abord** par un adaptateur **strictement à la jonction** du pointeur physique V1 et du sélecteur historique ; lire précisément `HifzPrefs.completeFrozenItqanSubblockV6`, `HifzPrefs.currentAnchoringEntry`, `cachedUnitsInLeg`, `GeometryRepository.eligibleWeeklyStabilizationUnit`, `ItqanRotationPolicy.pick`.
- Le nouvel adaptateur ne doit jamais créditer/réinitialiser les six lignes ; garantir reprise de la première ligne due, avec identité QCF exacte et géométrie propriétaire, sans décaler l'état de rotation d'une séance réelle ni répéter une ligne déjà créditée.
- Conserver lecture, restauration et clôture des snapshots V1 `ItqanFourSessionSnapshot` ainsi que `sourceSnapshot`. **Pas de nouvelle fenêtre Itqan4 créée pour les nouveaux cycles**.
- Matrice ciblée : 522:0, transitions intra-verset/intra-page, page/sourate, duplications, lignes déjà Acquises, OPEN à répétition n, redémarrage, interruption entre validations, fin de régime à An-Nās. Test initial obligatoire PASS et tests adversariaux ajoutés. Si impossible sans modification profonde du moteur : STOP, NO-GO, donner diagnostic et patch non destructeur proposé.

### 3. SAUVEGARDE UTILISATEUR DE RÉFÉRENCE — 25 OCTOBRE, 08:02:41 UTC
Fichier privé : `quran-hifz-sauvegarde-2026-10-25.json` disponible dans les fichiers de la conversation/Library ; utiliser **seulement une copie** isolée dans Work, **jamais dans GitHub ni dans un artifact public**.
Vérifié directement sur les données : backup V3, **309 Acquis, 1 098 Appris, 21 Stabilisés**, programme en vigueur dans le JSON `RECOMMENDED` depuis 2026-10-23.
- `v6ConsolidationStabilizationState` contient **OPEN** `stabilization-final-2026-10-25` (ancien slot, migration sûre vers `FinalV1` si le code existant le prévoit), **4 unités, 0 répétition**, `SNOWBALL_EXTENDED`. Dernière finale Itqān validée `2026-10-18`.
- `v6ConsolidationLearningState` contient **OPEN** `learning-2026-10-24`, **4 unités, 0 répétition**. Finale Sabqi précédente `2026-10-18`.
- Dernière Itqān ordinaire validée `2026-10-24`, dans `Aṭ-Ṭūr 52:1–33`, `itqanBlockIndex=2`, prochain bloc encore dû selon protocole gelé, `itqanRep=0`.
- **ABSENTS** : `itqanFourDatePlanV1` et `itqanFourNextPhysicalLineIdV1` ; le test 522:0 est donc **une exigence de compatibilité générale**, pas une fenêtre directement présente dans la sauvegarde personnelle.
- Ce JSON est un **instantané à 08:02**, pas une preuve de l'état du BOOX après cette heure. L'utilisateur a déclaré ne pas avoir encore effectué l'Itqān de dimanche ; ne pas présumer d'autres actions. Demander/valider un **nouvel export de sauvegarde** juste avant la mise à jour réelle sur BOOX, puis tester la restauration et la mise à jour sur copie. Ne jamais remplacer l'original.
- **Cas de transition dimanche 25** : final Itqān OPEN doit être reprenable et achevable même si le nouveau calendrier réserve dimanche aux finales ; ancienne boule de neige Sabqi du samedi OPEN doit être préservée avant sa finale ; aucun Itqān ordinaire fictif de dimanche ne doit être créé comme nouvelle dette bloquant lundi 26 ; ne pas requalifier rétroactivement des justificatifs existants.

### 4. TRAVAUX RESTANTS, UNIQUEMENT APRÈS LE BLOQUEUR
1. Conserver le brouillon local `WeeklyProgramPolicy / HifzPrefs` préférences datées et ratio `6-S` ; corriger toutes les dates, backlog, présélections, accès directs, `MainActivity` (`recommendedSunday`, `cadenceComplete`, `computeNextMode`, `todaySessions`), `WeeklyDashboardPlanner`, `WeeklyBilan`, `SettingsActivity`, sans conversion silencieuse des historiques `CLASSIC, RECOMMENDED, MANUAL_SEVEN`.
2. Retirer du **nouveau** planning Itqān ordinaire du dimanche et les projections 2 pages/4 jours ; conserver les anciennes OPEN V1 et les snapshots de finales hérités.
3. Dimanche second régime : finale Itqān absente. **BLOQUANT À PROUVER** : si une séance de maintien par hizb peut encore produire des lignes `Stabilisées` non Acquises, vérifier leur voie légale de promotion sans finale Itqān ; ne laisser aucun acquis bloqué et ne pas ajouter de promotion artificielle / ne pas toucher au moteur en cachette. Si contradiction insoluble sous périmètre, NO-GO documenté.
4. Renforcement Sabqi soir : unités cumulées et protocole déjà existant ×10 ; Consolidation Itqān soir : ancien corpus continu ×1, nouveaux segments ×7 ; finales QCF par page ×3. Ne pas modifier ces fonctions moteurs, faire seulement afficher les échéances correctes et reprendre chaque état OPEN gelé.
5. **AFFICHAGE OBLIGATOIRE** à la sélection et sur les vues détaillées : intitulé séance, nom(s) sourate(s) avec arabe, **versets précis**, pages QCF et lignes réellement dues, fraction 1/3 etc., répétitions et état (prévu/OPEN/validé). Chaque bloc de boule de neige et page de finale a sa propre plage ; ne jamais fusionner des plages disjointes en une fausse plage. Utiliser `QuranSurahNames.range/block` existants et isolats bidi, sans modifier Mushaf/Atlas. Aucun libellé de régime « avant/après An-Nās » sous les icônes. Pour état incertain : « plage à vérifier », jamais inventer.
6. Ne pas modifier les initialisations, valeurs personnelles installées, données Acquis, clés de sauvegarde, Atlas, pack privé ou signature.

### 5. TESTS ET PREUVES — GATES POUR GO
**Ordre de priorité**, sans maquiller les échecs :
A. Ciblé : `ItqanFourSessionStoreTest.finalFourDateCreditPinsPhysicalCursorWithoutLosingTheFollowingLine` PASS, sans affaiblir les assertions ; tests de non-doublon/transitions des lignes.
B. Réexécuter **les 839** tests JVM/source du brouillon ; corriger les **15 échecs** un par un, distinguer modifications UI légitimes vs vraie régression. Pas de suppressions de tests ni skip silencieux.
C. Calendrier S=0..6, I=6−S sur 7 jours (dimanche séparé), 3/3 et volumes QCF existants, les deux régimes, dimanches, retards, changements de semaine, sorties et reprises après crash, preuve de stabilité des moteurs et états `SNOWBALL/SNOWBALL_EXTENDED`.
D. Restaurer l'export utilisateur sur **copie isolée**, puis simuler : finale Itqān OPEN de ce dimanche, boule de neige Sabqi du samedi OPEN, reprise Itqān ordinaire restante, redémarrages. Comparer **ensemble exact des IDs** Appris/Stabilisé/Acquis, pas seulement les compteurs 309/1098/21 ; aucun recul ni crédit double. Prouver sauvegarde V3, `sourceSnapshot`, historiques intacts.
E. Simulations nouveau calendrier **180/365/548 jours** ; core+safeguard Gradle, JVM, source-contracts, Android instrumenté API35, captures émulateur BOOX-like, logs GitHub Actions au commit final. Les 906 PASS d'un ancien commit ne valent pas pour ce code ; NO-GO sur un test critique non exécuté.
F. Revoir diff de production : **aucune mutation des moteurs**, classe whitelist limitée, exception compatibilité documentée. Expliquer tout changement d'un garde de routage, et confirmer aucune mutation de sauvegarde initiale.

### 6. CONDITION DE RELEASE ET CONFIDENTIALITÉ
- Dès que TOUS les gates critiques sont PASS, **préparer une candidate de release** `1.17.5`, **versionCode > 42** (vérifier 43), même `applicationId=com.quransafeguard.hifz` et **même certificat de signature officiel** compatible avec l'APK 1.17.4 installé (empreinte historique SHA-256 du certificat `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14` ; re-vérifier source de certification). Valider avec `apksigner`, manifest et installabilité; tests d'upgrade sans désinstaller/supprimer les données quand possible.
- Le corpus Atlas privé (7 ressources) doit rester **local et hors GitHub public**, ainsi que APK privé, keystore, contenu de sauvegarde utilisateur et secrets. Ne jamais pousser d'APK contenant Atlas privé vers un artifact GitHub public. Si la signature officielle est disponible uniquement dans les secrets Actions, privilégier le mécanisme historique de **signature à distance de digest seul**, sans export de clé ni du contenu privé ; ne jamais utiliser une autre clé.
- **Si un seul gate critique échoue ou manque**, livrer un **NO-GO franc**, les logs, le SHA, le fichier/ligne en défaut, et l'état local préservé. Si tout passe, fournir APK candidate privée et son SHA256, preuve signature cert, liste des changements, plans de restauration et GO technique ; **aucune prétention d'installation physique BOOX sans essai réellement effectué**.
- Ne pas publier publiquement, merger ni distribuer une APK invalide pour respecter la demande d'urgence. Si publication officielle réellement demandée, n'effectuer qu'après GO et conformité de signature/confidentialité.

### 7. LIVRABLE FINAL
Rapport compact :
- HEAD de départ et HEAD final ; branche réellement utilisée ; `git status`, liste diff, journal de reprise du brouillon ;
- tableau des **15 anciens FAIL** et leur résolution ; preuve PASS précise du cas 522:0→522:6 ;
- test de votre sauvegarde du dimanche et restauration/upgrade ; résultat longitudinal ; captures QCF avec sourate/versets ;
- GO/NO-GO ; APK privée installable signée et checksum **uniquement en cas de GO**, ou blocage précis, sans promesse de délais ni résultats inventés.

**Agir maintenant ; ne pas refaire les audits précédents. Limiter les travaux au planning et à l'adaptateur V1 exactement justifié.**

### ADDENDUM BLOQUANT — FINALE SABQI DU DIMANCHE 25 OCTOBRE (VÉRIFIÉ DANS LE JSON 08:02)

**La finale Sabqi du dimanche matin est permanente dans les DEUX régimes Itqān ; elle ne doit JAMAIS être omise du calendrier, ni remplacée par la finale Itqān.**

Analyse vérifiée des clés de la sauvegarde originale `quran-hifz-sauvegarde-2026-10-25.json` (export 08:02:41 UTC) :

- `lastLearningConsolidationDate = 2026-10-18` : dernière finale Sabqi validée le dimanche précédent.
- `v6ConsolidationLearningStateFinalV1` **absent** ; aucune finale Sabqi du 25 ouverte à l'export. `learning_finalElapsedMs=0`. Ne pas prétendre qu'elle a été exécutée.
- `learningSnowballUnitIds` = **trois blocs de cinq lignes physiques de la page QCF 16** : `16:0..4`, `16:5..9`, `16:10..14` (15 lignes hebdomadaires). Les versets exacts doivent être résolus depuis la géométrie QCF, pas devinés.
- `v6LearningEarnedEveningUnitsV1` = **DEUX reçus uniquement** : `16:0..4` et `16:5..9`. **Reçu absent pour `16:10..14`.**
- `v6ConsolidationLearningState` = **OPEN** `learning-2026-10-24` avec **4 unités gelées** (`16:0..4`, `16:5..9`, `16:10..14`, lecture continue `16:0..14`), stade 0, répétition 0, `SNOWBALL`. Cette séance doit pouvoir se terminer en reprenant EXACTEMENT son snapshot et sa date du samedi.
- La finale Itqān, distincte, est elle-même OPEN en `stabilization-final-2026-10-25`, protocole `SNOWBALL_EXTENDED` ; **ne jamais intervertir, fusionner, ou écraser** ses clés avec celles de Sabqi.

**DANGER LOGIQUE ACTUEL À TESTER :** `HifzPrefs.sundayFinalPageUnits(LEARNING,...)` construit son corpus à partir des seuls reçus déjà acquis. Avec deux blocs reçus sur trois, il peut construire une finale partielle pour dix lignes de la page 16 tout en omettant les cinq autres. La clôture `completeLearningConsolidationSessionV6` écrit la date finale et vide l'accumulateur hebdomadaire. On ne doit pas valider/présenter cette clôture comme **finale complète des 15 lignes** et perdre les cinq lignes non traitées. Ne pas ajouter une approbation implicite de validation partielle : l'utilisateur n'a pas arbitré un tel comportement.

**Comportement requis dans ce cas réel :**
1. Le dimanche matin affiche **« Finale Renforcement Sabqi »**, l'état réel (**« En attente du Renforcement du samedi »** si ses justificatifs manquent), sourate(s)/versets/pages/lignes exacts et bloc(s) non éligibles ; ne pas annoncer « Terminé ».
2. Proposer/reprendre **le Renforcement Sabqi du samedi déjà OPEN**, par l'accès direct de reprise, sans remettre à zéro ses répétitions, sans changer la date/snapshot et sans lancer une nouvelle séance fictive. Après sa validation ×10 selon son protocole original, revérifier les trois reçus, puis autoriser la finale Sabqi du dimanche ×3 par page (**1 lecture visible, 2 récitations masquées avec amorces**) ; promotion `Appris→Acquis` seulement des lignes réellement éligibles.
3. **Si le Renforcement du samedi n'est pas terminé :** ne pas attribuer un crédit fictif, ne pas effacer le troisième bloc, ne pas clore silencieusement une finale « partielle ». Afficher le blocage/reprise possible et conserver les unités en dette jusqu'à la validation légitime à une date ultérieure ; historique daté inchangé. La règle d'éligibilité existante reste autorité, sans réécrire le moteur de promotion.
4. La finale Sabqi doit être proposée **aussi dans le second régime Itqān**, même quand aucune finale Itqān n'est programmée.
5. **Tests reproductibles supplémentaires :** JSON réel sur copie, 10/15 lignes avec reçus et renforcement 24 OPEN, final Sabqi 25 NON OPEN ; après clôture du renforcement, 15/15 reçus ; test de non-clôture partielle ; reprise avant/après redémarrage ; historique de date et `sourceSnapshot` ; aucune interférence avec la finale Itqān OPEN. Vérifier le corps des 15 IDs, pas seulement la quantité.

**Ce correctif est une exigence de PLANIFICATION, de garde d'éligibilité et d'AFFICHAGE, pas une autorisation de modifier `ConsolidationCycleEngine` ou les fonctions de crédit.** Si impossible sans moteur, STOP avec fail précis. **NO-GO tant que ce cas personnel n'est pas testé et sûr.**

### PRÉCISION UTILISATEUR — SABQI PAGE ACHEVÉE ET ITQĀN RATTRAPAGE (25/10 SOIR)

**Correction fondamentale : « 10/15 » désigne LES REÇUS DE RENFORCEMENT, PAS LES LIGNES SABQI APPRISES.** Lecture directe du JSON du 25/10 à 08:02 : les **quinze IDs** `16:0` à `16:14` appartiennent **tous** à `v6LearnedLineIds` ; aucun n'est encore dans `v6AcquiredCreditLineIds`. `lastSabqiDate=2026-10-23`, dernier bloc `Al-Baqarah 2:104–105` avec **37/37** répétitions. Le corpus de la page QCF 16 est **appris en entier**, pas inachevé. Les DEUX reçus de Renforcement concernent `16:0..4` et `16:5..9`. Le troisième groupe `16:10..14` est bien appris mais son reçu de Renforcement du samedi n'a pas été enregistré à l'heure de l'export ; la session `learning-2026-10-24` OPEN contient le cumul du samedi, stage 0, done 0. **NE JAMAIS obliger à répéter ou réapprendre la page Sabqi / relancer 37x.** Uniquement finir/reprendre la séance de Renforcement gelée si elle reste ouverte, puis la finale complète lorsque ses reçus sont éligibles. Ne pas effacer les 15 Appris.

**DÉCISION D'ARBITRAGE — RATTRAPAGE ITQĀN DE MAINTIEN AUTORISÉ, CONDITIONNEL** : dans le second régime, il n'existe toujours **aucune finale Itqān hebdomadaire obligatoire ni finale Itqān le dimanche**. Une ligne NOUVELLE peut cependant être classée `Stabilisé` après le travail d'un hizb ×3, puis être éligible à Consolidation. Si les justificatifs de Consolidation sont complets, proposer une **finale de rattrapage facultative et ponctuelle** sur un **jour ordinaire Itqān**, en substitution plutôt qu'en travail supplémentaire, utilisant la **finale QCF existante** par page ×3 (1 visible, 2 masquées avec amorces) ; passage `Stabilisé→Acquis` uniquement sur IDs admissibles et receipt vérifié. Aucune création automatique si tout est déjà `Acquis`, si aucun bloc `Stabilisé` nouveau, ou si les reçus manquent. Ne pas déclencher au dimanche et ne pas modifier les répétitions, masques, crédits, moteurs ou initialisations. Si cette fonctionnalité est impossible sans dépasser le périmètre, STOP/NO-GO explicite plutôt que réécrire le moteur.

**Exemples QA :** (A) semaine première phase, blocs 8/7/7 et finale Itqān dimanche ; (B) second régime hizb déjà intégralement Acquis : aucun rattrapage ; (C) second régime hizb contenant un nouveau sous-bloc Stabilisé, consolidation dûment reçue : rattrapage offert sur journée Itqān ordinaire, jamais dimanche ; (D) même sous-bloc sans reçu : pas de promotion ; (E) sauvegarde utilisateur : 15/15 lignes Sabqi Apprises, deux reçus Renforcement sur trois, OPEN du samedi reprenable, finale Sabqi non commencée ; pas de répétition Sabqi imposée. Montrer les noms arabes de sourates et plages de versets/pages QCF réels, calculés du corpus.
