# Quran Haafidh 1.17.9 — conformité différentielle

26 octobre 2026. Mandat : modernisation Accueil / Parcours / Calendrier, sans publication automatique.

## Identité

- Base distante vérifiée : `ae68ee1a46e195e9da1068aed981f3363869a4e0`, branche `fix/haafidh-1178-family-postfinal-20261026`.
- Copie précédente propre `1fe5d470` ; fetch a révélé les étapes de candidate signée plus récentes. Aucun travail local préexistant écrasé. Ancienne copie au rattachement Git manquant laissée intacte.
- Branche dédiée : `work/haafidh-1179-boox-ui-20261026`.
- Version candidate : `1.17.9`, code `47` (> 1.17.8/code46).
- 1.17.8 : audit technique CI40405581372, candidate privée signée ; validation physique toujours ouverte. Référence officielle antérieure documentée 1.17.7 : c42bc5deca80012b982ec8ec6e5a658cd20484d6. Marqueur GitHub latest toujours1.17.1 ; ne pas l'utiliser pour déduire la version réellement installée.

## Inventaire et architecture

Écrans natifs Android Views conservés. Aucun framework/dépendance ajouté. `MainActivity` porte les trois surfaces et garde les destinations et gardes existantes. `CalendarPresentation` est un adaptateur immutable sans API d'écriture ; `WeeklyDashboardPlanner` sépare la date réelle et la fenêtre consultée. Les lectures prospectives tournent sur l'exécuteur existant, avec rejet des résultats obsolètes.

| Domaine | Avant | 1.17.9 / préservation |
|---|---|---|
| Accueil | Quatre grandes icônes et priorité | Même grille 2 × 2, icônes 48 dp max, état du jour, frise, reprises datées |
| Parcours | Six grands pictogrammes | Même six fonctions, pictogrammes 40 dp max, trois paires ; sous-titres conservés |
| Matin | Deux activités concaténées | Deux cartes séparées si planifiées, horaires des préférences |
| Soir | Leçon et révision cumulative concaténées | Cartes distinctes, propre reçu/état ; pas de fausse validation sans matériau |
| Calendrier | Aujourd'hui et six dates suivantes | Semaines lundi–dimanche, jour sélectionné, flèches, aujourd'hui, mois |
| Historique | Bilan et reprises globales | Reçus réels, OPEN sur date initiale ; données manquantes explicitement inconnues |
| Finales | Reprises datées existantes | Sabqi/Itqān séparées, aucune nouvelle finale Itqān après Nās |
| Programme | Deux choix et confirmation | Choix identiques, accès distinct et paramètres détaillés ; aucune modification par navigation |
| Révision | Active et passive | Modes, corpora, durées et masques inchangés |
| Lecture | Mushaf, masquage, Amorces | Fichiers runtime identiques |
| Atlas | Unités, frises et commentaires | Fichiers runtime identiques |
| Sauvegarde | V3 / schéma 6, cinq magasins | Format et code identiques, aucun champ ajouté |
| Reprise | Files existantes | Finales, cumulatifs et consolidations OPEN ; répétitions ordinaires reliées à leur vraie dette FIFO |

### Informations et actions toujours accessibles

Paramètres, date, Aujourd'hui et liste de séances, rattrapages FIFO, avis Sabqi, quatre destinations, six modes, état Maintien après Nās, choix programme avec confirmation, détails complets matin/soir, bilan des contrôles/retards, récapitulatif des lectures cumulatives, reprises datées et masques. Chaque carte conserve le détail intégral original développé au toucher et un accès protégé au parcours. Les futures séances et les reçus terminés restent consultatifs. Aucun nombre de répétitions ou de minutes n'est fabriqué lorsqu'il manque au planning.

### Modifications de présentation et navigation

En-tête 40 dp, typographie adaptée, suppression du séparateur d'accueil, tuiles resserrées sans changer les vecteurs ; fond blanc ; contour 1/2 dp de sélection ; statut textuel et compte n/N, pas seulement une nuance ; cibles 48 dp pour commandes et expansion. À 360 dp de largeur, sept cellules se partagent la largeur utile : les cellules de jour peuvent être inférieures à 48 dp ; les commandes principales gardent 48 dp. Aucun nouveau paysage, décor, dégradé, ombre ou animation permanente.

La frise d'accueil ouvre le jour choisi dans Parcours. La vue mensuelle native sert de sélecteur de date. Douze semaines sont listées dans l'accès historique ; le mois permet d'aller aux autres dates. Au-delà365 jours de prévision, une vue «à vérifier» remplace les plages inventées. L'historique sans preuve de planning complet ne prend pas l'état Terminé. Les reprises de configurations anciennes gardent les règles historiques et sourceSnapshot.

État de navigation dans Bundle (jour/semaine/écran), aucune nouvelle préférence. La configuration hebdomadaire reste une action indépendante avec confirmation. La copie du récap cumulatif est corrigée :20 pages au total, finale comprise.

## Vérifications locales exécutées

- Compilation complète de109 classes de production avec API Android ; nouveaux composants compilés.
- 617 tests comportementaux et279 contrats de source : zéro échec (logs annexés). Les neuf nouveaux tests de présentation font partie des617.
- 234fichiers runtime établis byte-identiques à 1.17.8 : moteurs, sauvegardes, assets, masque, Atlas, icônes et manifeste.
- Contrat des23 vecteurs :PASS ; les fichiers des icônes contractuelles ne changent pas.
- Audit statique strict : aucune erreur bloquante ;14 éléments REVIEW historiques conservés, pas présentés comme des tests fonctionnels.
- Sauvegarde privéeV3 / schéma 6 rejouée sur copie isolée : export/restore exact des cinq magasins ; cinq interruptions avec rollback complet ; aucune perte/chevauchement. La migration historique existante de l'ancien slot de finale est conservée, non introduite par 1.17.9.

### Simulations et comparaison à la référence

Les mêmes 17 tests de scénarios ont été exécutés sur les classes de la base ae68ee1a et sur la candidate : résultats de sortie exactement identiques, hors temps JUnit. Scénarios4/8 semaines avec séances sautées/rattrapées ; vrais tests M8 de transitions quotidiennes à180/365/548 jours ; files datées, rechargements, doubles appuis et changements de programme. Les compteurs M8 sont identiques :180 jours / 125 acquis,365 jours/260acquis,548 jours / 390 acquis ; quatre absences puis quatre rattrapages par scénario. Ces tests JVM ne sont pas des centaines de jours de gestes sur Android.

Rejeu prospectif supplémentaire sur copie réelle : fenêtres28/56/180/365 jours, soit28/56/182/371 lignes en semaines complètes. Première semaine comparée champ par champ à la référence, résultat identique. Tous les paramètres/acquis/OPEN/curseurs restent identiques, hors cache de plan `itqanLegPlanV1.*` déjà existant. Variante post-Nās :huit dimanches futurs sans nouvelle finale Itqān ; date de l'OPEN historique conservée.

Le parcours synthétique des dates dans CalendarPresentationTest vérifie uniquement l'immuabilité et les frontières de calendrier ; il n'est pas compté comme simulation métier.

## Défauts détectés pendant ce travail

1. Coche de planning prise à tort pour une validation : corrigée par preuve de reçu ; sans matériau, activité exclue.
2. Découpage au signe+ tronquant «2 lectures + 1 avec ancrages» : reproduit par test rouge, corrigé en séparation uniquement devant un titre d'activité.
3. Intitulé de finale répété dans un reçu compté deux fois : test rouge puis correction.
4. OPEN historique absent de la carte du jour : ajouté à sa date originale, sans recréation.
5. OPEN Itqān ancien autorisant à tort de futures finales après Nās : limité à la date gelée ; régime prospectif respecté.
6. Risque de null au premier échec de projection : semaine initialisée avant calcul ; fallback de la fenêtre consultée.
7. Calcul prospectif jusqu'à371 dates sur fil UI : déplacé hors fil UI, résultats périmés rejetés.
8. Tests de source liés aux anciennes tailles/méthodes de rendu : seules assertions de présentation actualisées ; invariants de routage, corpus et fichiers conservés.

## CI, captures et décisions

**Résultat final : GO logiciel pour poursuivre la finalisation privée ; NO-GO publication officielle.**

Commit applicatif audité : [`60f79057446fb9dae32ea0967593350a56fa7710`](https://github.com/applicreation0-web/quran-hafidh/commit/60f79057446fb9dae32ea0967593350a56fa7710).
Branche : [work/haafidh-1179-boox-ui-20261026](https://github.com/applicreation0-web/quran-hafidh/tree/work/haafidh-1179-boox-ui-20261026).
[CI finale40413003551](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40413003551) : **SUCCESS**.
[Artefact12693569089](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40413003551/artifacts/12693569089) téléchargé et SHA-256 contrôlé : `d5ae37a450071bb2e2d6c937de15473a01b33bed40f11c9c49601ac2a511a4a8`.

Rapports XML inspectés : **926 tests JVM/source +55 tests Android =981 tests**, zéro échec, zéro erreur, zéro ignoré. Quatre phases supplémentaires de mort du processus :4/4 PASS, soit985 exécutions en les comptant. Les190 rapports XML JVM et le rapport Android agrégé sont joints aux preuves. Les neuf nouveaux tests de présentation et les trois tests Android calendrier/lifecycle sont inclus, pas additionnés une seconde fois.

La CI précédente40411734266 est un échec réel corrigé, pas un PASS. La CI intermédiaire40410646793 avait passé925 tests JVM/source et54 Android ; ses captures ne remplacent pas celles du commit final.

Captures finales natives :18 PNG (neuf états à chaque profil), avec XML d'accessibilité. Accueil, six fonctions, calendrier, carte développée, semaine suivante, mois, historique, configuration et retour accueil ont été produits par la vraie application. Les valeurs visibles viennent de données de test Android, pas de la restauration de l'état courant du BOOX.

Démarrages à froid mesurés par `am start -W` :1255 ms compact,1108 ms tablette. PSS après parcours :62 447 Ko compact,61 844 Ko tablette. Ce sont deux observations d'émulateur, pas un benchmark matériel ni une certification de mémoire/latence BOOX.

| Porte | État final |
|---|---|
| Compilation et suites JVM/source | PASS,926 tests CI |
| Android et correction de fermeture | PASS,55 tests CI |
| Arrêt externe/reprise | PASS,4 phases |
| Simulations 4/8 semaines et 180/365 jours | PASS, comparaison à la base décrite ci-dessous |
| Données et sauvegardes | PASS sur copies et tests ; moteurs/format inchangés |
| Rendu compact/tablette | PASS émulateur, captures inspectées |
| Signature historique de 1.17.9 | NON EXÉCUTÉE |
| Installation par-dessus l'application réelle | NON EXÉCUTÉE |
| Ghosting, veille et lisibilité BOOX physique | NON EXÉCUTÉS |
| Publication officielle | NO-GO ; aucune publication |


## Portes de publication

Aucune publication officielle autorisée ou effectuée. L'absence de ghosting et la lisibilité e-ink ne peuvent être certifiées par émulateur. Signature historique, mise à jour par-dessus la version réellement installée et conservation sur BOOX physique exigent leurs preuves propres.

## Fichiers modifiés depuis la base 1.17.8

- `.github/workflows/haafidh-1179-boox-ui-audit.yml`
- `docs/superpowers/plans/2026-10-26-haafidh-1179.md`
- `docs/superpowers/specs/2026-10-26-haafidh-1179-design.md`
- `hifz-app/build.gradle.kts`
- `hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/Calendar1179InstrumentedTest.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/CalendarPresentation.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java`
- `hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/BooxPortraitHomeSourceContractTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/CalendarPresentationTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/PhoneProgrammeSourceContractTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/PreDebugVisualRestorationSourceContractTest.java`
- `hifz-app/src/test/java/com/quransafeguard/hifz/preview/WeeklyDashboardBooxRecoveryTest.java`
- `scripts/Calendar1179DifferentialReplay.java`
- `scripts/audit_haafidh_1176.py`
- `scripts/capture_1179_ui.py`
- `scripts/verify_1178_family_invariants.py`
- `scripts/verify_1179_ui_only.py`

## Justification des limites historiques

Une date passée ne devient Terminé que lorsque les reçus suffisent à prouver toutes ses activités. Lorsque les anciens détails ou le régime historique ne sont plus démontrables, En attente / Historique incomplet reste affiché ; les reçus individuels restent marqués terminés. Cette prudence ne crée aucune dette ni séance. Le nombre dans une journée historique incomplète représente seulement les activités connues, pas une reconstitution garantie de tout son planning.

## Profils Android et limites BOOX

Captures de l'émulateur API 35 : 1080 × 1920 px à densité 480 (360 × 640 dp), puis 1404 × 1872 px à densité 240 (936 × 1248 dp). Ces profils sont des contrôles adaptatifs : ils ne constituent pas une lecture des paramètres matériels du BOOX. La surface utile respecte les barres système via `Ui.respectSystemBars`. Aucun dp n'est déduit des photographies.

La revue visuelle de la CI intermédiaire montre : six fonctions et frise dans le viewport tablette, détails matin/soir en dessous ; accueil plus compact avec quatre accès et reprises ; retour d'écran et mois fonctionnels. Sur téléphone compact, les cartes se déroulent verticalement. La version finale ajoute les statuts de chaque activité et la carte développée aux captures de contrôle.

Sur appareil physique restent à tester : ghosting au changement de jour/semaine, expansion, retour accueil, veille/reprise ; contraste effectif, tailles tactiles et comportement des rafraîchissements. Aucune API de full refresh BOOX ni animation permanente n'a été ajoutée.

## Défaut Android détecté par la validation du dernier commit

Le run 40411734266 (c014fbd6) a effectivement échoué pendant `PostFinalRecallInstrumentedTest.fourFutureSundayProjectionsDoNotInventValidatedFinales` : un retour tardif du chargement géométrique pouvait appeler refreshAll sur une Activity détruite, puis resoumettre une projection à son exécuteur fermé (`RejectedExecutionException`). La suite a été interrompue après 34 tests ; elle n'est pas déclarée PASS.

Correction 60f79057 : ignorer les retours tardifs et les rafraîchissements d'un écran détruit/en fermeture, invalider sa génération de calendrier avant d'arrêter l'exécuteur. Test Android déterministe supplémentaire : `closedScreenRejectsLateCalendarRefreshWithoutSubmittingWork`. Cette correction porte uniquement sur MainActivity et le test ; moteurs et données restent gelés.

## Reproduction des contrôles publics

Sur le commit applicatif exact et avec les dépendances Android du workflow :

```bash
python3 scripts/verify_1179_ui_only.py
python3 scripts/verify_1178_family_invariants.py
python3 scripts/verify_contractual_ui_icons.py
gradle --no-daemon --continue :hifz-core:test :safeguard-core:test :hifz-app:testDebugUnitTest :hifz-app:sourceContractTest
```

Le workflow `haafidh-1179-boox-ui-audit.yml` prépare le corpus QCF public, les ressources publiques de test, l'émulateur et les phases séparées d'arrêt/reprise. Le rejeu privé `Calendar1179DifferentialReplay.java` prend un fichier externe et une géométrie, utilise uniquement des préférences en mémoire, et n'intègre jamais la sauvegarde au dépôt. Pour sa comparaison directe, la classe de planification de référence est extraite de ae68ee1a et renommée WeeklyDashboardPlannerBaseline lors de la compilation isolée.

## APK et mise à jour

Un APK debug de vérification est assemblé par la CI : code 47, nom 1.17.9-IbnKathir-DEBUG, package de test `com.quransafeguard.hifz.ibnkathirtest`. Il sert aux tests et captures, avec des ressources publiques. Ce package ne remplace pas l'application officielle `com.quransafeguard.hifz` et ne constitue pas une candidate BOOX complète contenant les sept ressources Atlas privées. Il n'est donc pas fourni comme APK de mise à jour.

La candidate privée installable en mise à jour reste à finaliser hors CI publique : sept ressources Atlas conformes, signature historique attendue `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`, paquet officiel, code 47, vérification de l'APK produit, puis essai `install -r` avec contrôle des données et des OPEN. Aucun keystore, mot de passe, corpus privé ou JSON personnel n'a été publié.

## Décision de release

**NO-GO pour une release officielle** tant que la candidate privée compatible, la mise à jour réelle et les essais BOOX physiques ne sont pas prouvés. Les contrôles logiciels et leurs preuves détaillées sont séparés de ces portes matérielles. Aucune release, aucun tag de publication, aucune fusion main et aucun envoi de l'APK officiel n'ont été effectués. Une autorisation explicite restera nécessaire avant toute publication officielle.

## Précision sur les écritures existantes

La projection réutilise le cache de plan Itqān existant. Sa signature couvre la branche de rotation, la frontière réelle et le régime : une prévision après Nās ne devient pas le plan réel avant Nās ; la consultation suivante revalide sa signature. Les tests de comparaison autorisent uniquement ce cache calculé à changer. Acquis, répétitions, curseurs, dates, OPEN et paramètres sont comparés strictement. Les migrations historiques déjà présentes dans HifzPrefs restent inchangées ; aucune nouvelle migration ni initialisation n'est introduite par la refonte.
