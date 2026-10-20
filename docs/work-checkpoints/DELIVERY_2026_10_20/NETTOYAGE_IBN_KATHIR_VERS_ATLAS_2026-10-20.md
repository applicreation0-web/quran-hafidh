# Nettoyage du chantier Ibn Kathīr → Atlas + Correspondances — 20/10/2026

## Nouveau point de travail
- Branche active créée sans réécrire l'histoire : `work/haafidh-1171-atlas-correspondances`.
- Branche historique conservée : `work/haafidh-1171-ibn-kathir-integration-independent`. Tous les audits/exportations anciens y restent consultables.
- Aucun changement de corpus Jalalayn, des 366 Unités / 1 053 Passages Atlas, des 110 relations historiques (80 documentées, 24 en attente, 6 écartées), des 8 icônes ou des moteurs Hifz.

## Nettoyage utilisateur réel
- `MainActivity` n'ouvre plus `IbnKathirMapActivity` pour le bouton **Atlas**.
- L'activité utilisateur `AtlasMapActivity` appelle uniquement `MawdudiMapScreen` (Unités Atlas, Lecture et `MapJalalaynDialog`, QCF authentique).
- Lorsque le catalogue privé vérifié des 114 sourates n'est pas disponible, elle échoue sans retour à l'ancienne liste Ibn Kathīr ou au pilote Baqarah.
- Retour Lecture → Atlas, point exact, état et fermeture du mushaf maintenus par délégation au composant Atlas existant.

## Workflows actifs
- Retargeté `correspondence-j4-qa.yml` sur la nouvelle branche.
- Retargeté `haafidh-private114-offline-finalize.yml` sur la nouvelle branche, en ajoutant `AtlasMapActivity.java` et `MainActivity.java` aux fichiers surveillés.
- Ajouté `haafidh-atlas-correspondances-regression.yml` : garde gel 1.17.1 Hifz, contrôles pictogrammes 23+8, tests Hifz+Atlas+QCF JVM/source et 604 pages authentiques. **Sans compilation d'APK, sans publication ni clé privée**.
- Retirés de la nouvelle branche seulement :
  1. `ibn-kathir-engine-frozen.yml` (audits 1 903 groupes devenus hors périmètre),
  2. `haafidh-ibn-kathir-debug.yml` (intermédiaire ancien, source CP05 figée),
  3. `quran-com-original-ibn-kathir-titles.yml` (export de titres historique).
- Les scripts `guard_frozen_hifz_engines.sh` et `audit_hifz_quiz_against_1171.sh` n'ajoutent **que** `AtlasMapActivity.java` aux fichiers documentaires/UI autorisés ; aucune exception globale ni affaiblissement des contrôles moteurs.

## Compatibilité / réserve connue
- Les classes `IbnKathir*` historiques sont encore compilables et `IbnKathirMapActivity` reste enregistrée en `exported=false` **uniquement pour ne pas casser l'ancienne instrumentation** `HifzAndMapRuntimeSmokeInstrumentedTest` et ses scénarios de comparaison.
- Ces anciens chemins ne sont **pas** accessibles depuis l'accueil Atlas. La suppression physique des classes/manifest requiert d'abord la migration de tous les tests d'instrumentation vers Atlas 114 sans perdre la couverture ; à ne pas confondre avec un nettoyage de texte de surface.
- Aucun ancien texte Ibn Kathīr n'est une source éditoriale approuvée pour l'actuel Atlas ; Jalalayn demeure la référence des commentaires.
- Le déploiement privé doit encore franchir le contrôle catalogue, droits, QCF et BOOX ; pas d'activation automatique ni de release.

## Tests
- `AtlasActiveEntrySourceContractTest` garantit à la compilation que l'accueil appelle exclusivement Atlas, et que l'activité moderne ne peut réintroduire le repli ancien.
- Les tests de parcours historique et de portrait BOOX contrôlent maintenant explicitement cette route.
- Vérifier les statuts des GitHub Actions de la nouvelle branche ; ne pas annoncer « PASS » avant réussite réelle.

## Usage futur
Toute nouvelle tâche et tout Work doit utiliser **la nouvelle branche** `work/haafidh-1171-atlas-correspondances`. Lire ce checkpoint et `PROGRESSION_JALONS.md`, ne pas réintégrer d'anciens headings Ibn Kathīr dans le produit.
