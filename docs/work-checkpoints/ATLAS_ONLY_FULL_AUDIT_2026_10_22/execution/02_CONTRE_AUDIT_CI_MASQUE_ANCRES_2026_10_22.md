# Quran Haafidh 1.17.2 — contre-audit ciblé CI / Lecture libre / ancres
Date : 22 octobre 2026. Périmètre : branche `work/haafidh-1171-atlas-only-pre20-mask` ; **ce document n'est PAS un GO release**.

## 1. Preuves GitHub et causes d'échec

- Premier workflow native audit sur `e1c78d37` : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39776668400 . Échec avant Gradle : `android-actions/setup-android@v3` sans `packages: ''`, tentative d'installation obsolète `sdkmanager tools`, `Warning: Failed to find package 'tools'`. Tous les tests natifs de ce run : NON EXÉCUTÉS.
- Correctif de configuration au commit `178972e0b59f1bc7336c3e2ee4eeafd7c20ad853`, workflow https://github.com/applicreation0-web/quran-hafidh/actions/runs/39791673362 . SDK installé, **37 tâches Gradle et suite JVM réussies** (BUILD SUCCESSFUL). Échec ensuite **dans l'installation de l'image système de l'émulateur** API 35/default/x86_64 : `Error on ZipFile unknown archive`. La commande connectedDebugAndroidTest n'a donc pas été lancée : test WebView et stores NON EXÉCUTÉS. Ne pas compter ceci comme une défaillance du comportement applicatif.
- Un essai sur une autre image (`target: google_apis`) et les corrections applicatives sont inclus dans `05d69d2500d4a2ebbcdf7c44c409be0984a22766` : https://github.com/applicreation0-web/quran-hafidh/commit/05d69d2500d4a2ebbcdf7c44c409be0984a22766 .
- Nouveau workflow native audit : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39792640883 . État à relire : en file au moment de cet enregistrement, **aucun PASS instrumenté attribué**.

## 2. P1 confirmé par analyse du code (non encore reproduit sur émulateur)

**Avant patch** `MushafView.runWhenReady` retenait une seule commande `pending` lorsque `ready=false`. Un second réglage écrasait le précédent. `StudyReaderActivity.onPageShown` déclenche successivement `setMaskFollowsSelection`, `setLandmarkLines`, `setPageLandmarkBoxes`, `setSemanticCues` et `setMask(100)`. `reader.js` envoie `pageShown` avant `ready`. L'application peut donc activer le masque sans ses paramètres nécessaires, source plausible du comportement erratique utilisateur.

**Autre incohérence avant patch :** `showFreeActiveMaskPage()` autorisait la liste `lineIds=[]` en cas d'indisponibilité de géométrie. `applyFreeActiveMask()` pouvait néanmoins indiquer `freeActiveMaskPageReady=true` sur la seule présence de six repères et ancres, alors que `reader.js` n'effectue pas le masquage si `lineIds.length===0`.

## 3. Correctif soumis (à certifier, pas déjà PASS)

Commit `05d69d25` :
1. `MushafView.java` : file FIFO `pendingActions` des commandes pré-ready, effacée à chaque chargement/erreur/destruction ; compteur `renderGeneration` ; seuls `ready(generation)` et `pageShown(page,generation)` du chargement actuel agissent. Cela concerne le moteur partagé par Hifz, donc requiert tous les tests de non-régression.
2. `reader.js` : transmet `boot.renderGeneration` avec les rappels natifs.
3. `StudyReaderActivity.java` : les lignes physiques QCF non vides sont aussi nécessaires avant `freeActiveMaskPageReady=true`; sinon fail-open lisible.
4. Workflow d'audit : l'image émulateur est `google_apis` au lieu de `default`.

**Ne PAS déclarer ces changements valides tant que les JVM, instruments Android réels, tests de passage de page et Hifz ne sont pas PASS.**

## 4. Tests et lacunes de couverture constatés

- `MushafPendingCommandsInstrumentedTest.everyCommandSubmittedBeforeReadyRunsInOrder()` soumet 1, 2, 3 commandes à une vraie WebView et attend les trois effets ordonnés. Bonne régression élémentaire ; exécution encore nécessaire.
- `BackupSchemaRejectedInstrumentedTest` teste les corruptions de schéma et l'absence de mutation des SharedPreferences et du journal. Exécution encore nécessaire.
- `ReaderFreeActiveMaskSourceContractTest` et `FullEraseNoResidueSourceContractTest` vérifient principalement des **chaînes dans les sources**, pas le résultat visuel/JS réel. Ajouter tests instrumentés ON/OFF, retour, vitesse, pausing, retry/watchdog, absence d'ancres, 604 pages, et comportement de la même WebView en Sabqi/Itqān/Murājaʿah.
- `AtlasDisplayCatalogTest` utilise des **titres synthétiques**, donc ne démontre PAS les 1 419 titres réels de la variante privée. Les 366 Unités + 1 053 Passages doivent être vérifiés sur le pack effectivement assemblé, sans publication de ses données sur ce dépôt public.
- `verify_contractual_ui_icons.py` contrôle manifest et **empreintes de 23 dessins contractuels**, mais précise lui-même ne pas certifier le rendu visuel. Recette écran BOOX des 23 placements/dimensions/interactions encore requise.

## 5. Gate release

**NO-GO 1.17.2** tant que tests Android instrumentés non exécutés, stabilité masquage/ancres non démontrée, audit des 1 419 titres privés, contrôle rendu 23 icônes, tests longitudinal Hifz et recette BOOX/migration/signature non clos. Correspondances volontairement exclue. Ne pas publier sur GitHub public le binaire final privé, le corpus privé ou les secrets.

Compte rendu établi à partir du GitHub HEAD et logs du 22/10 ; aucun test BOOX physique prétendu.

## 6. Deuxième contre-audit sur le correctif — garde-fous et contrats sources
Sur le commit `05d69d25`, les trois jobs de contrôle Hifz/Atlas et de compilation privée ont relevé une violation de la règle gelant `MushafView.java` : les modifications du moteur de rendu doivent être **autorisées précisément**, jamais rendues globalement libres. La JVM a aussi signalé trois tests source liés aux chaînes `N?.pageShown(currentPage);` et `N?.ready();`, maintenant remplacées par des callbacks qui portent la génération.
- Preuves : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39792640940 et https://github.com/applicreation0-web/quran-hafidh/actions/runs/39792641050 .
- Résolution au commit `5b42c4830b0a2996d7b3091d9ae3936eae2e682d` : exception **strictement épinglée aux empreintes exactes** du rendu Java et JS dans `scripts/verify_authorized_mushaf_reader_delta.py`, tout le reste du gel Hifz inchangé ; trois tests de contrats vérifient désormais que les callbacks natifs ont un identifiant de génération.
- Nouvelles exécutions de référence : https://github.com/applicreation0-web/quran-hafidh/actions/runs/39793493879 (Hifz/Atlas), https://github.com/applicreation0-web/quran-hafidh/actions/runs/39793494188 (audit natif), https://github.com/applicreation0-web/quran-hafidh/actions/runs/39793493905 (compilation privée sans APK). États **non conclus au moment de cette mise à jour** : aucun PASS inventé.

**Les deux correctifs visent la 1.17.2, mais il reste impératif d'obtenir un test WebView réel PASS et une vérification de non-régression Hifz sur leur HEAD final avant GO.**
