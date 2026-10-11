# QURAN HAAFIDH 1.17.1 — AUDIT CRITIQUE CONTRADICTOIRE / BOOX

Date : 11 octobre 2026. Dépôt : `applicreation0-web/quran-hafidh`. Branche exclusive : `work/haafidh-1171-ibn-kathir-integration-independent`. **Aucune release de production autorisée.**

## A. La référence officielle et la méthode de preuve

- Commit officiel immuable **1.17.1** : `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Séparer systématiquement : (1) contenu binaire Git identique, (2) différences de code autorisées et comparées octet par octet, (3) comportements testés dans la JVM, (4) comportements observés en Android *réel* sur émulateur, (5) acceptation visuelle BOOX physique — non certifiable sans l'appareil.
- Aucun résultat JVM ou simple compilation APK ne peut se substituer à l'instrumentation Android.
- **Deux workflows indépendants obligatoires, sur le même SHA que l'APK** : `Ibn Kathir Hifz + Quiz audit ONLY - no APK` et `Quiz launch Android runtime regression ONLY - no APK`. La porte logicielle `scripts/require_same_sha_ci.py` vérifie ces statuts et l'archive des captures (> 50 Ko) avant **toute compilation du debug publiable**.

## B. Matrice 1.17.1 ↔ debug — ce qui est réellement inchangé

| Famille | Comparaison Git source | Épreuve complémentaire |
|---|---|---|
| `hifz-core/src/main/` et cadence/horloge de base | **Identique** | `:hifz-core:test`; test `WeeklyProgramPolicyTest.classicDelegatesOriginalCanonicalCadenceWithoutChange` |
| `MushafView.java`, `GeometryRepository.java`, `WordGeometryRepository.java` et QCF | **Identiques** | 604 pages, 77 432 mots, frontières de versets testées |
| Quiz `QuizActivity.java`, `QuizCorpus.java`, `QuizQuestion.java`, `QuizHistory.java` | **Identiques** | Questions `Appris ∪ Stabilisé ∪ Acquis`; lancement Android avec corpus vide puis vraie page 523 Appris |
| `AnchoringQueue.java`, migrations V6, sauvegarde / restauration | **Identiques** | Tests de progression et de reprise; aucune migration des IDs coraniques |
| `HifzPrefs.java`, `HifzSessionActivity.java` | **Modifiés, jamais assimilés à identiques** | Scripts `verify_authorized_program_preferences_delta.py`, `verify_authorized_hifz_session_delta.py` reconstituant les changements permis depuis 1.17.1 et comparant les fichiers complets octet à octet; tests J3-J7 |
| `ConsolidationCycleEngine.java`, `ItqanRegimeStore.java` | **Modifiés dans leur périmètre explicitement convenu** | Scripts `verify_authorized_consolidation_delta.py`; tests des sessions et des anciens régimes |
| `MainActivity.java`, `SettingsActivity.java`, `WeeklyDashboardPlanner.java`, `StudyReaderActivity.java` | **Modifiés au niveau UI/orchestration** | Tests source + lancement Android, pas d'exemption au nom de « code gelé » |

## C. Bugs reproduits contradictoirement sur Android et corrigés

### C1 — Crash du Quiz à l'ouverture : cause vérifiée

Ancien APK : exception Android `Resources$NotFoundException` sur `ic_ui_play`, cause `java.lang.IllegalArgumentException: M needs to be followed by a multiple of 2 floats`. Le vecteur XML avait `M6 L3 L20 L12 L6 L21 L6 L3 Z`, syntaxe invalide acceptée par la compilation mais rejetée à l'instanciation de `QuizActivity`. **Ce n'était pas un effet direct des nouvelles amorces**. Correction dans `ic_ui_play.xml` : triangle Lucide valide; la méthode et le corpus Quiz officiels restent inchangés.

Tests indépendants dans `QuizLaunchRegressionInstrumentedTest` : Activity réellement lancée, commande « Commencer », maintien de l'écran en cas de corpus vide, questions sur une vraie page Appris (523) et dessin Android de **toutes** les ressources d'icônes `ic_*`, pas uniquement vérification SHA des 23 glyphes.

### C2 — « Fermer Tafsir » représenté par une barre oblique

Le XML `ic_ui_close.xml` contenait une seconde diagonale erronée; remplacer par les deux diagonales d'un X. Le manifeste contractuel conserve un hash mis à jour sur le dessin **corrigé**, et l'instrumentation dessine le vecteur.

### C3 — Carte dite sémantique : ancienne liste technique non conforme

Comparer au vrai prototype `work/islahi-prototype-p1-from-1-17-1`, fichiers `IslahiMapActivity.java` et `IslahiTafsirActivity.java` :
- Navigation Iṣlāḥī = **sourates (114) → liste de passages → fiche de passage**. La vieille carte Ibn Kathīr affichait « 1/173 » etc. avec une miniature géante occupant la moitié de la surface.
- Nouveau `IbnKathirMapActivity` : une seule vue navigable à la fois ; 114 sourates ; liste pleine largeur de passages avec plage réelle et pages QCF ; fiche dédiée au seul passage, aperçu Mushaf borné et action explicite pour lire ; Retour conserve la sélection et le défilement. Le bouton Carte demeure le pictogramme contractuel `network`, pas un pictogramme ajouté arbitrairement.
- **Aucun faux titre** : index de 1 903 groupes numériques, titres originaux seulement quand source vérifiée (ex. Qāf), sinon référence neutre (sourate / plage / pages) et lien externe vers le commentaire. Les notes anglaises pilotes Iṣlāḥī ne sont pas attribuées à Ibn Kathīr.
- Test Android `ibnKathirMapMustShowFullBlockListThenSeparatePassageSheet` : sourate Qāf, liste, sélection, fiche et retour.

### C4 — Calendrier et horaires peu visibles

`MainActivity` présente maintenant le choix actuel **Classique / Recommandé** sur le titre du programme, avec sélection en un geste et confirmation. Le changement n'affecte que le calendrier à partir de la date de choix, pas les crédits ou les séances antérieures. Tableau BOOX : lignes lisibles **jour / matin / soir / état**, horaires 05:30 et 20:00 proposés modifiables dans Paramètres, sans blocage des séances. « Aujourd'hui » reste consultable même après validation.

**Programme Recommandé** : Sabqi 5 lignes lundi/mercredi/vendredi; Itqān mardi 8, jeudi 7, samedi 7 et **dimanche 8 lignes** ; dimanche matin finale Renforcement Sabqi, dimanche soir finale Consolidation Itqān. **Dimanche : aucune Révision active ni passive.** Cible 30 lignes/semaine dépendant des vraies frontières physiques. **Programme Classique** inchangé, dimanches consacrés aux finales. La capture d'un dimanche *sans* Itqān provenait du mode Classique affiché, pas du régime Recommandé.

## D. Critères de non-régression moteur, fondés sur des tests de comportement

| Risque prioritaire | Invariant de sécurité | Contrôle |
|---|---|---|
| Sabqi | 5 lignes, curseur et crédits d'Appris non inventés | `HifzSessionActivity` gelé sauf deltas précis, `:hifz-app:testDebugUnitTest`, Activity Android `SABQI` |
| Itqān | 40 répétitions (20 visibles, 5 à 25%, 5 à 50%, 5 à 75%, 5 à 100%), anciennes séances conservant leur régime | `ItqanBalancedMaskSchedule`, `ItqanRegimeStore` tests; Activity Android `ITQAN` |
| Suppléments Itqān | +1/+2 lignes seulement en fin physique admissible, sans fusion de deux sourates (Qāf → Adh-Dhāriyāt) | `ItqanCompletionBoundaryPolicy` tests |
| Renforcement | Nouveau bloc aux répétitions prévues, ancien cumul non répété artificiellement, finale dimanche distincte | `ConsolidationCycleEngine` / J4-J7 tests; Activity Android deux modes |
| Consolidation | Nouveau bloc **×7**, anciens blocs continus **×1**; jamais revalidation à l'identique le lendemain | `SnowballValidationIdempotenceTest`, `ContradictoryAuditTwoPersistenceTest` |
| Acquis | Promotion **une seule fois après finale achevée** ; filtrage par bloc déjà Acquis même en semaine partielle | `ContradictoryAuditTwoPersistenceTest` + tests de la transition schema V6 |
| Reprise | Une session OPEN datée garde son snapshot; anciennes sauvegardes sans snapshot reconstituées par préfixe exact ou marquées `LEGACY_OPEN`; nouvelles lignes restent dues | tests de reprise de deux familles et de mismatch |
| Révision active/passive | Curseurs indépendants; passive seulement sur versets **entièrement** Appris/Acquis; aucun déplacement de curseur le dimanche | `PassiveApprisAndSundayContractTest`, simulations du calendrier |
| Quiz | Source toujours `learned ∪ stabilized ∪ acquired`; jamais de faux `Acquis` à la génération; moteur officiel 1.17.1 intact | comparaison Git exact + test de vraie question page 523 sous Android |
| Mise en route Android | Ne jamais considérer un écran seulement non-crashé comme un écran valide; rejeter `état à vérifier` | `HifzAndMapRuntimeSmokeInstrumentedTest`: huit modes et captures |
| Icônes | Les 23 du contrat ont une fonction attribuée et *toutes* les icônes emballées se chargent et se dessinent sur Android | hash de contrat + test réel `everyContractualIconInflatesOnAndroid` |

## E. Résultats, limites et publication

- Source/JVM : voir le workflow `Ibn Kathir Hifz + Quiz audit ONLY - no APK` du HEAD livré.
- Tests de l'émulateur Android : voir le workflow `Quiz launch Android runtime regression ONLY - no APK`, qui doit donner **OK (tests)** et téléverser des captures PNG (pas seulement un log texte).
- Le workflow de debug exige les **DEUX conclusions SUCCESS du MÊME SHA et une archive réelle de captures** avant de lancer `assembleDebug`. Signature `apksigner`, identifiant `com.quransafeguard.hifz.ibnkathirtest`, ressources Mushaf et SHA-256 contrôlés après.
- Les anciens tests instrumentés `androidTest` invoquant des APIs V6 supprimées de l'ancien dépôt ne compilent plus en totalité. L'exécution CI **isole les scénarios actuels sans supprimer ni réécrire les anciens fichiers dans Git**, et ce passif reste une dette d'audit. Le moteur `hifz-core` et la persistance doivent rester protégés.
- **BOOX physique non certifié** : rémanence E-Ink, ressenti tactile, rafraîchissement, taille de la calligraphie, retour après veille, orientation paysage, vrais historiques privés et batterie ne sont pas reproduits par un émulateur Android. Un APK debug distinct du package officiel permet de les vérifier sans écraser les données de production.
- **Pas de release** : uniquement un debug, au contenu et au SHA exacts contrôlés et traçables. Ne pas prétendre « audit physique validé ».

### Décision

Le debug ne devient publiable qu'après une preuve verte sur **un seul commit exact**, pas seulement sur des commits précédents ou sur la présence de scripts. Toute anomalie runtime bloque le package, même si le build Gradle est réussi.
