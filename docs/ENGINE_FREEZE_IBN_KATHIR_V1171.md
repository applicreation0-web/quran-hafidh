# Quran Haafidh 1.17.1 — gel absolu des moteurs Hifz

**Décision prioritaire de l'utilisateur : aucun impact sur les moteurs.**

## Base figée
Commit immuable pré-migration : `0b70b2b34aca9334680ad8f282273706cf868313` (dernier état BOOX validé automatiquement).

## Interdictions absolues
- Aucun changement dans `hifz-core/`.
- Aucune modification des moteurs, états, politiques de promotion, cadence, calendrier, répétitions, quotas, réconciliation, sauvegardes, métriques, sélection des corpus, quiz ou données de progression.
- **Aucune modification dans `HifzSessionActivity.java`, `HifzPrefs.java`, `AnchoringQueue.java`, `RevisionSessionEngine.java`, `ConsolidationCycleEngine.java`, `ItqanMaintenancePolicy.java`, `ItqanRegimeStore.java`, `StabilizationHalfPagePolicy.java`, `WeeklyDashboardPlanner.java`, etc.**
- `MushafView.java` et `hifz-app/src/main/assets/hifzreader/` restent également inchangés afin de préserver les interactions, masques, rafraîchissements et positions.
- L'Itqān « AnchoringQueue » et son pictogramme `ic_hifz_anchor` n'ont **aucun lien** avec les anciennes amorces Al-Munīr : ne jamais les retirer.

## Surface modifiable en code Java
**Uniquement** des adaptateurs documentaires et les contrôleurs d'interface déjà autorisés :
`MainActivity`, `Ui` (**affectation des pictogrammes uniquement**), `StudyReaderActivity`, `SemanticPassageRepository`, `SemanticTitlePopup`,
`IbnKathirMapActivity`, `IbnKathirGroupIndex`, classes nouvelles `IbnKathir*`,
`QuranicCue*`, `QuranicAmorce*`.

Cette permission n'autorise **pas** à modifier les règles Hifz depuis ces classes.
Toutes les autres classes Java/Kotlin existantes sont gelées.

## Approche de migration obligatoire
1. Extraire les 1 243 repères sous **coordonnées des mots coraniques + longueur** ; supprimer leurs titres et données éditoriales Al-Munīr.
2. Maintenir la **compatibilité de l'API consommée par HifzSessionActivity et le lecteur** : remplacer l'implémentation derrière cette API, **sans changer les appelants Hifz**.
3. Faire cohabiter indépendamment l'index documentaire de 1 903 groupes Ibn Kathīr et le dépôt coranique de repères.
4. N'afficher aucune amorce dont la géométrie exacte n'est pas vérifiée. Ne jamais dériver des mots coraniques en générant des textes.
5. Conserver les données utilisateur existantes, en particulier les curseurs et historiques. Si une migration est nécessaire et non sûre, **bloquer la livraison**, documenter le problème, ne pas forcer un nouveau schéma.
6. Si le retrait Al-Munīr exige de modifier un moteur, **STOP / NO GO** et chercher une solution d'adaptateur. Ne pas « corriger rapidement » le moteur.

## Porte CI automatisée
`scripts/guard_frozen_hifz_engines.sh` compare les fichiers de production à la base figée. Le workflow
`.github/workflows/ibn-kathir-engine-frozen.yml` lance :
- blocage de toute modification des moteurs/core/lecteur ;
- `:hifz-core:test` ;
- `:hifz-app:testDebugUnitTest` ;
- `:hifz-app:sourceContractTest` ;
- `:hifz-app:assembleDebug` et contrôle d'identité du paquet de test.

Aucune publication release automatique. Toute défaillance interdit l'APK intégré final.

## Contrôles après compilation
Une CI verte atteste seulement les garanties testées. Le test sur appareil BOOX doit également valider :
- progression Hifz et état sauvegardé avant/après lecture via Carte ;
- Itqān : répétitions, passages, après-Nās, état de queue ;
- Sabqi, Consolidation, Renforcement : mêmes transitions, mêmes compteurs, mêmes dates ;
- Révision active : amorces visibles seulement si exactes, repérage des faiblesses conservé ;
- Quiz : corpus éligible et suppression audio temporaire inchangés ;
- aucun signet normal déplacé par une visite temporaire de la Carte.

**Le critère principal est l'absence de changement comportemental des moteurs, pas seulement leur compilation.**
