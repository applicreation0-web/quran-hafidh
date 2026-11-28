# Quran Haafidh 2.0.0

## Références et preuves

- Développement sur `work/haafidh-200-universal`. Référence historique immuable `fe271ca6c3dff7b9e31ca5b0a2801c9fece3be0c`. Cible `2.0.0`, code 63, package de release `com.quransafeguard.hifz`.
- [Échec SDK de 05500b5d](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46067817238). Correction du chemin `sdkmanager` dans `adebb522`.
- [Campagne Android de e256c8a1](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46121622624). SDK et KVM opérationnels, JVM et contrats exécutés, deux tests Android réussis sur API 35 à 320 dp. Le test des préférences effectue 120 changements et compare les cinq ensembles V3. Le test Fragilités vérifie les pixels des états visible, masqué, dévoilé et repris, le report et l’absence de crédit. L’artefact de cette campagne ne contient pas les PNG car le chemin de collecte différait du chemin utilisé dans le test. La campagne suivante exige ces fichiers.
- [Campagne de 5b4468f](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46122440176). Les trois tests Android ont réussi, dont la recréation dans les six directions après dévoilement. La campagne a échoué à la collecte, après désinstallation des APK de test par le runner Gradle. Le script suivant installe explicitement les deux APK de test sur émulateur, vérifie le code et le nombre de tests, puis récupère les fichiers avant toute désinstallation. Les 24 points de trace du rejeu JVM M8 à 180, 365 et 548 jours concordent octet pour octet avec ceux de fe271ca6.
- Les répertoires Git du Mushaf et de la géométrie partagée ont les mêmes IDs d’arbre sur la référence et le candidat, respectivement `eb8a2594e06dec43f7c7cd68b7de5e3831afe8a8` et `cb500e4ed3e0363894ac56a3c02857d87bd28315`. La comparaison porte sur tous les octets de ces répertoires.
- Le processus d’audit distinct a exécuté des reproductions JVM sur le moteur, avec géométrie QCF et données synthétiques. Les défauts confirmés et les correctifs sont décrits ci-dessous.

## Cartographie et exigences

[Inventaire des activités, stores, tâches et interactions](inventaire.md). Le manifeste et la navigation réelle distinguent notamment l’Atlas moderne de l’activité historique et l’import par le sélecteur système d’une synchronisation autonome.

Les états concernent la portée décrite dans chaque ligne. Le mandat reste applicable aux sous-cas encore ouverts.

| ID du mandat | État | Fichiers et preuve | Travail restant |
|---|---|---|---|
| 0, 1, 2 | Testé en développement | `build.gradle.kts`, workflow 2.0, références ci-dessus | Qualification de la version installée et du certificat officiel |
| 3 | Testé sur les preuves disponibles | Tests historiques, `M8TargetedLongitudinalRegressionTest`, arbres Mushaf et `reader109` identiques. Inventaires historiques dans `docs/audit` et `docs/work-checkpoints` | Compléter la cartographie de toutes les opérations temporaires et des états Android hors export |
| 4 | Testé sur synthétique | `HifzBackup`, `HaafidhV3BackupCompatibility`, `UniversalLearningRouteTest`. Export/import cinq stores, reprises à cinq points d’interruption, intégrité des entrées typées, import universel malformé refusé | Rejouer sur le backup privé de référence et produire le comparatif privé. Tester une mise à jour sur appareil sans désinstallation |
| 5 | Implémenté partiellement | `MainActivity` utilise des destinations, états et ressources stables. `HifzPrefs` et `UniversalLearningRoute` pilotent le parcours. Pictogrammes natifs et état du calendrier pilotés par des identifiants. La vérification compare 95 boutons convertis avec leurs anciens pictogrammes et descriptions | Terminer la séparation des projections formatées et des WebViews. Préserver les identités et les anciens reçus |
| 6 | Testé partiellement | `UniversalLearningRouteTest`, vraies répétitions Sabqi traversant deux plages disjointes dans l’ordre saisi, bornes QCF, poursuite sans crédit de l’espace entre plages | Saisie de plages en pages, versets, lignes, hizb et juz testée. Volume de 1 à 5 lignes configurable pour l’apprentissage, le premier bloc ouvert garde ses lignes et ses répétitions. Les jours Sabqi se choisissent du lundi au samedi, avec prise d’effet le lundi suivant. Sabqi propose aussi 10 à 45 répétitions par pas de cinq pour les prochaines leçons. Les répétitions et calendriers des autres disciplines restent ouverts |
| 7 | Testé partiellement | `JuzMaintenanceStore` et `HifzPrefs`. Juz déclaré admissible dans le maintien indépendant, état suspendu et repris, révision cumulative masquée attendue après achèvement du juz, consolidation admise sur une plage déclarée | Étendre la preuve aux calendriers réels et brouillons simultanés, notamment minuit et langues, et aux passes dont une partie a déjà été travaillée |
| 8 | Testé partiellement | Tests de calendrier, actifs, finales, audio et boule de neige. Les 40 + 5 minutes du profil historique restent inchangées. Les profils universels choisissent un budget de 30, 40, 45, 60 ou 70 min pour les prochaines révisions. Une séance ouverte conserve son budget | Comparaison événement par événement de toutes les disciplines et aides Android |
| 9 | Implémenté partiellement | `MainActivity.showUniversalSetup`, `configureNewProfile`, six étapes et cinq modèles volontaires, récapitulatif avant enregistrement et restauration par l’entrée existante | Exécuter la nouvelle preuve Android de continuité de l’assistant. Sélecteur par sourates et versets intégré et compilé. Étendre le rythme fin par discipline |
| 10, 10.1, 10.2, 10.3 | En cours | [Glossaire](terminologie.md), attestations séparées des compositions et protocole d’entretien | Compléter les attestations orales, le dossier IFAN et l’évaluation avec des locuteurs qualifiés |
| 11 | Implémenté partiellement | `HaafidhActivity`, `values`, `values-en`, `values-wo` couvrent les nouveaux parcours, paramètres, assistant, commandes de sauvegarde et Fragilités. Test 120 changements passé sur `e256c8a1` | Achever chaque écran, dialogues et contenus dynamiques, pluriels, WebView et réinitialisation locale. Vérifier le comportement avec la synchronisation |
| 12.1, 12.2 | En cours | Palette `Ui` crème et monochrome. Chiffres tabulaires demandés au chrono Fragilités | Qualifier typographie, contraste et grands caractères sur toutes surfaces, modes applicables et appareils |
| 12.3, 12.4 | Testé partiellement | `HifzSessionActivity` donne une commande au masquage et une à l’affichage, pictos stables, rangées de 4 et 3 sous 370 dp. Test de visibilité et masque en CI | Tester les sept zones 48 dp réelles, les états désactivés, les descriptions et info-bulles trilingues sur les autres barres |
| 12.5 | Testé partiellement | 604 pages, assets et coordonnées QCF identiques. Rendu et masquage exercés sur l’émulateur d’e256c8a1 | Mesures avant/après à dimensions identiques avec folio centré, barre d’état et zones rendues |
| 12.6, 12.7, 12.8 | Implémenté partiellement | Navigation Parcours et familles de paramètres localisées. États d’accès pilotés par des types et non par les libellés | Revoir accueil, calendrier complet, rythmes et réglages avec grandes polices et pour les trois langues |
| 12.9 | Implémenté partiellement | `CapHifzPanel` affiche les catégories d’acquis réels dans les nouveaux profils | Qualifier graphiques, motifs et unités sur les vues restantes |
| 12.10 | Implémenté | Emblème unique de livre ouvert et lutrin dans l’accueil et l’icône adaptative noire et crème. L’icône adaptative dispose d’une zone protégée par sa marge interne | Vérifier le rendu sur plusieurs masques et tailles de lanceur |
| 12.11 | Bloqué pour essai physique | Préservations QCF, règles d’affichage et commandes physiques testables dans Android | Ghosting, modes de rafraîchissement et commandes physiques sur BOOX |
| 12.12, 13 | Testé partiellement | Historique Android 1.17.24 et test Android 2.0 sur écran étroit. Collecte des PNG corrigée après e256c8a1 | Écrans standard, tablette, paysage pris en charge, échelle de police, captures FR/EN/WO et comparatif chiffré complet |
| 14 | Testé partiellement | 120 cycles des six transitions et sauvegardes stables sur e256c8a1. Test de recréation après une aide exécuté sur 5b4468f. Ajout d’un choix de langue dans Fragilités et test du vrai bouton | Exécuter le test du vrai bouton et étendre la couverture aux chronos, à l’audio, aux répétitions et aux interruptions système |
| 15 | Testé partiellement | Rejeu JVM M8 ciblé à 180/365/548 jours avec traces comparables | Compléter le rejeu Android et les comparaisons de tous les états après chaque événement |
| 16 | Testé partiellement | Déclarations disjointes, aucune progression au démarrage, quatre états de bascule de maintien et consolidation | Tester tous les profils et toutes les modifications des paramètres en conditions réelles |
| 17 | Testé partiellement | Moteur V3 avec journal de restauration, injection de cinq interruptions, vérification de type et de route. CI avec données synthétiques et garde des assets privés | Scénarios Android système, espace insuffisant, conflits Drive. Inventorier contenus réellement empaquetés et droits avant redistribution |
| 18 | Testé partiellement | Audit distinct des règles universelles, exécution de cas synthétiques qui provoquaient des défauts, mutation isolée vérifiant que le tri implicite des plages déclenche deux échecs | Étendre la contre-expertise à l’ensemble des axes et rejouer les autres mutations sur le candidat final |
| 19, 19.1 | En cours | Commits distincts et qualification liée à chaque SHA, présente matrice | Enregistrer le dernier SHA qualifié par porte après chaque campagne |
| 20 | Bloqué | [Continuité historique de signature](../RELEASE_SIGNING_CONTINUITY.md) | Vérifier certificat, mise à jour Android signée et empreinte finale après les autres portes |
| 21 | En cours | Cette matrice, glossaire, code, campagnes et rapports historiques | Assembler les 17 livrables en fin de qualification, avec migration privée et preuves Android |
| 22 | NO-GO | En particulier wolof humain, BOOX physique, restauration privée sur 2.0.0, mise à jour, droits et couverture technique | Aucune release avant franchissement de toutes les portes |

## Défauts reproduits par l’audit distinct

1. Une plage déclarée ne pouvait pas être choisie aussi pour la consolidation. Le moteur autorise les deux déclarations et la preuve d’une acquisition reste distincte.
2. L’éditeur des paramètres acceptait un chevauchement avec le futur parcours d’apprentissage. Il applique désormais la validation déjà imposée par l’assistant.
3. L’import V3 acceptait une chaîne de plages invalide puis le lecteur ne pouvait plus construire le parcours. La restauration vérifie désormais la liste, les types et le curseur universel avant toute écriture.
4. Le maintien universel validait un juz sans proposer la passe cumulative masquée. Le protocole durable et la passe hebdomadaire sont reliés sans action sur le verrou An-Nās.
5. À la recréation de l’activité, le jour ou le réglage pouvait changer le régime Itqān d’une séance ouverte. Le régime et la date de l’activité sont restaurés pour les profils universels. Reste à exercer réellement la bascule jour/régime sur Android.
6. Le choix de langue existe en Paramètres, dans l’assistant et dans le lecteur autonome de Fragilités et dans la révision active chronométrée. Le bouton attend que la sélection de verset ou la piste audio soit fermée. Un test recrée la révision active en pause dans les six directions et vérifie son temps, son corpus et son absence de reçu.

7. Un profil « Révision seule » créait des retards Itqān fictifs. Les compteurs s’appuient désormais sur la date d’activation des disciplines. Les réglages modifiés conservent les affectations antérieures.
8. Un maintien terminé après minuit créait un reçu sur le jour de clôture, même le dimanche. Le reçu universel est daté du créneau où le brouillon a été ouvert. Une fois le protocole terminé, l’écran confirme la validation sans ouvrir un autre maintien.
9. Une restauration pouvait accepter un emploi du temps incompatible avec les disciplines restaurées. Les capacités du calendrier effectif et futur sont vérifiées avant écriture. Les semaines passées restent consultables.

## Test historique avec source externe

Le test du passeport documentaire Ibn Kathir exige un fichier `IK_J2_REVIEW_PATH` issu du Hafs épinglé. La récupération à la reprise a échoué à la vérification SHA-256 de la source externe. Le test documentaire demeure conditionnel et distinct d’un test synthétique qui exécute le validateur de staging avec toutes les mutations originales, dont activation, approbation, pages, droits et troncature. Ces tests de staging n’accordent aucun droit sur un corpus ou une amorce.

## Confidentialité et provenance

Le diff des commits publics 2.0 comparé à fe271ca6 ne contient ni sauvegarde JSON, ni annotation exportée, ni APK, ni clé. La chaîne Android de qualification conserve seulement des résultats et des captures produites avec les données du test. Le build de l’émulateur utilise un suffixe d’application de développement et une signature de test. Les archives de CI excluent les APK et la configuration de signature. Les préférences locales de langue résident dans `haafidh_display`. Elles sont vérifiées séparément des cinq ensembles métier.

| Groupe de ressources | Contrôle à ce stade | Condition à lever |
|---|---|---|
| Mushaf, géométrie et positions QCF | Inventaire des 604 pages, empreintes d’arbres et contrôle de 77 432 mots. `app/src/main/assets/mushaf/SOURCE.txt` épingle le fournisseur et une révision | Vérifier les conditions chez le titulaire des droits et les relier précisément à ces vecteurs et aux données de géométrie |
| Tanzil Uthmani | Fichier source de tests dans `scripts/data`. Les conditions du titulaire exigent texte intact et attribution lors de sa diffusion | Vérifier les notices et le parcours exact de tout dérivé effectivement empaqueté. [Tanzil](https://tanzil.net/docs/text_license) |
| Polices d’interface | Notices SIL OFL incluses pour Patrick Hand et Kalam. Le fichier Patrick Hand contient bien à, ë, é, ŋ, ñ, ó et leurs capitales | Vérifier l’emplacement des notices dans l’archive candidate |
| Pictogrammes | Les ressources vectorielles existantes ont des métadonnées et contrats Lucide. Les nouveaux dessins du lecteur et de l’icône sont dans le code | Recouper le manifeste de toutes les icônes et leurs notices empaquetées |
| Tafsirs, cartes sémantiques, similitudes et amorces | Les assemblages Gradle et les contrôles de séparation public/privé sont identifiés | Vérifier chaque source, ses droits et son attribution dans l’assemblage exact. Le test de non-activation Ibn Kathir ne vaut pas une autorisation de distribution |
| Audio | Les tests peuvent créer un signal PCM synthétique pour exercer le lecteur. Les sources privées restent exclues de l’émulateur public | Vérifier l’installation et les droits de chaque pack déclaré éligible au livrable privé |

## Comparatif Android exécuté

[Campagne 46123203616, commit 66ab606](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46123203616). Les compteurs de l’instrumentation et les captures figurent dans [l’artefact de preuves](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46123203616/artifacts/15255250577). Sept tests Android ont réussi. Quatre à 320 dp couvrent l’assistant, les 120 transitions, le vrai bouton de langue dans le lecteur après une aide et les commandes de Fragilités. Trois autres tests vérifient les pixels visibles, masqués et dévoilés sur les formats ci-dessous. La barre d’état, les cibles de 48 dp et les contours d’indisponibilité sont visibles dans la capture wolof du lecteur étroit. Le récapitulatif de l’assistant a été contrôlé en anglais.

Le rectangle du folio est mesuré sur la capture réelle de l’état visible, par l’étendue de la couleur du fond de folio. Les deux versions sont mesurées à résolution, densité et échelle de police identiques. L’écart des dimensions est nul dans les trois comparaisons.

| Capture portrait Android | Police | Folio 1.17.24 | Folio 2.0.0 | Largeur / hauteur / surface de l’écran |
|---|---:|---:|---:|---:|
| 1 080 × 2 400 px | 100 % | 883 × 1 407 px | 883 × 1 407 px | 81,76 % / 58,63 % / 47,93 % |
| 900 × 1 200 px | 150 % | 565 × 901 px | 565 × 901 px | 62,78 % / 75,08 % / 47,14 % |
| 1 404 × 1 872 px | 150 % | 950 × 1 514 px | 950 × 1 514 px | 67,66 % / 80,88 % / 54,72 % |

Les mesures des deux écrans 4:3 proviennent d’un émulateur Android. Les essais de rafraîchissement, de ghosting et de commandes matérielles restent réservés au BOOX physique.

## Mise à jour Android instrumentée

Le scénario synthétique vérifie une installation de la référence `fe271ca6`, un arrêt de processus avec journal d’import ouvert, le remplacement en place de la version 62 par la version 63, la stabilité de la date d’installation, le retour du journal à l’état cohérent, et la persistance des acquis et brouillons du scénario. Il utilise la signature de développement sur l’émulateur. La vérification sur l’installation personnelle avec le certificat historique reste une porte distincte.

La [campagne 46123712611](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46123712611) du commit `47d8619` a réussi la JVM mais a échoué sur un contrôle de pixels du masque à 320 dp. La génération signalée prête par le lecteur n’explique pas à elle seule la densité d’encre mesurée par Android. La campagne précédente avait réussi sur ces dimensions. Le seuil est conservé et les échecs doivent maintenant conserver les PNG disponibles et l’état de l’activité pour distinguer une fuite de texte d’une capture Android incorrecte.

La seconde revue contradictoire a reproduit un import qui acceptait des corpus ou un brouillon de maintien malformés, et une reprise de passe masquée impossible après le changement de semaine. Les contrôles ont été étendus aux corpus et au juz admissible avant tout remplacement de stockage. La passe masquée universelle conserve les unités attestées lors de son ouverture jusqu’à la validation. Le test d’installation renforcé exige la récupération de production au démarrage et vérifie les cinq réglages du stockage de mémorisation ponctuelle.

La [campagne 46147235523](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46147235523) a conservé les PNG du contrôle d’encre à 320 dp. La capture visible et la capture masquée montrent toutes deux l’alerte système « Pixel Launcher isn't responding » devant le folio. La préparation suivante désactive uniquement ce lanceur sur l’émulateur jetable. Les seuils et les alertes du processus testé sont conservés. [Captures de l’échec](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46147235523/artifacts/15264142972).

La [campagne 46123995402](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46123995402), sur `ba2c48bc`, a réussi neuf exécutions Android, dont le remplacement synthétique 62 → 63 sur la même installation. [Preuves de la campagne](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46123995402/artifacts/15255343125). Le scénario renforcé de la [campagne 46146465409](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46146465409) sur `a4437363` a également réussi les neuf exécutions Android. Il vérifie la récupération au démarrage sans la déclencher dans le test et vérifie le stockage de mémorisation ponctuelle. [Preuves](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46146465409/artifacts/15264386351).

Sur `22c0db6732ffd7197fcae596f72e8564fc58e110`, la [campagne 46149381935](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46149381935) est verte. Onze tests Android ont été exécutés, dont la mise à jour synthétique en place, la révision chronométrée et Fragilités sur quatre formats. [Preuves](https://github.com/applicreation0-web/quran-hafidh/actions/runs/46149381935/artifacts/15265515677).

## Contrôles supplémentaires de la reprise du 28 novembre

- Apprentissage. L’estimation universelle compte les séances sur les blocs physiques réellement ouvrables. Quatre extraits d’une ligne séparés donnent quatre séances, même avec un plafond de cinq lignes. Le premier bloc ouvert conserve sa longueur, son jeton et son protocole si le réglage change.
- Répétitions et déclarations. Les nouvelles leçons Sabqi universelles peuvent utiliser un nombre choisi, avec masquage progressif et reprise 5+5 après dévoilement final. La leçon ouverte garde son protocole et son jeton. Les dossiers de maintien ou de révision ouverte empêchent de retirer leurs passages déclarés. Les réglages n’accordent aucune ligne acquise.
- Langues et chronométrage. Le test Android vérifie désormais la locale de l’activité recréée et le titre de la révision, en plus de la conservation du brouillon. Il ajoute un changement pendant le chronométrage et vérifie l’intervalle de temps écoulé, le masque et l’absence de reçu.
- Assemblage des ressources de développement. `:hifz-app:mergeDebugAssets` a réussi. L’inventaire contient 604 folios, un fichier d’origine, la géométrie et les waqf, quatre fragments de géométrie des mots, les ressources de lecteur, les notices de polices, les index d’amorces et de cartes, le fichier d’origine audio sans enregistrements, ainsi que quatre fragments SQLite du Jalalayn anglais historique. Aucune clé, sauvegarde ou ressource Atlas privée n’a été relevée dans cet assemblage. Les quatre fragments Jalalayn restent soumis à la vérification des droits de la traduction. [Notice quran-svg](https://github.com/quran-ws/quran-svg/blob/main/NOTICE.md) · [Notice quran-svg-elements](https://github.com/quran-ws/quran-svg-elements/blob/main/NOTICE.md) · [Édition informatique du Complexe](https://qurancomplex.gov.sa/mushaf-computer-publishing)
- Certificat de référence. L’empreinte SHA-256 du certificat public historique archivé a été relue et concorde avec le mandat. La vérification de signature de la 2.0.0 reste liée au futur assemblage privé qualifié.
