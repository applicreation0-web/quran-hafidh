# Correspondances · audit PR 9

**NO-GO pour l’intégration.** Périmètre de reprise 5f14f48fe629052a0111b29952546120d294729b, comparé à la base 216f4d4b26e048656bc224fe1ade42c99240184d.

| Priorité | Constat et correction | Preuve |
|---|---|---|
| P1 | Similitudes utilisait un tracé différent de link-2. Les deux familles avaient des épaisseurs différentes en XML et en marge. Les tracés officiels et le trait 2/24 sont rétablis. | Comparaison des éléments SVG avec les fichiers officiels Lucide, échouée avant correction et réussie ensuite. |
| P1 | Le placement masquait les repères dès saturation. Une liste réversible par marge rend accessibles les repères déplacés. | Test Node du JavaScript de production sur DOM simulé. Deux marges saturées et 62 repères joignables sans doublon. WebView Android à confirmer. |
| P1 | Lecture et Révision choisissaient systématiquement le premier dossier. Elles partagent désormais un sélecteur qui vérifie de nouveau l’autorisation à l’ouverture. | Revue du flux et du diff. Le pilote ne fournit pas encore de fixture réelle à plusieurs dossiers pour un même verset. |

## Contrat des icônes

Similitudes utilise [link-2](https://github.com/lucide-icons/lucide/blob/main/icons/link-2.svg). Correspondances utilise [git-compare](https://github.com/lucide-icons/lucide/blob/main/icons/git-compare.svg). Vue 24 × 24, trait 2, jointures et terminaisons arrondies. Les tracés Lucide sont indépendants des pictogrammes des huit relations. Les marges sont hors du SVG QCF. Le bouton de regroupement est compté et annonce son rôle de dialogue.

## Registre

Le fichier `registre-documentaire-110.json` rapproche les 110 décisions strictes de l’audit du 21 octobre et les objections individuelles du 20 octobre. Il conserve 52 présélections, 10 suspensions et 48 exclusions documentaires. Une présélection ne vaut ni certification des références et de la structure, ni validation QCF. Aucun dossier admis en production. Les quatre dossiers de démonstration restent limités au debug activé explicitement.

## Contrôles et portes restantes

- Les deux exécutions historiques 41888242718 et 41888235677 sont vérifiées, avec trois jobs verts chacune. Le test instrumenté ouvre directement R037. Il vérifie les deux rendus QCF, le dimensionnement et la règle d’ouverture du Tafsīr. Sa réussite ne couvre pas les chemins Lecture/Révision ni les captures demandées.
- Le diff de cette reprise ne touche pas aux classes de persistance ni aux moteurs Hifz. Une non-régression par exécution, incluant sauvegardes V6, process death, chronomètres et accès en restitution active, reste nécessaire.
- Restent à vérifier sur Android la densité réelle, le parcours à dossiers multiples, les rotations, la lisibilité et les bornes QCF, ainsi que les captures. Validation BOOX physique et comportement E-Ink en attente.
- Restent ouverts l’optimisation complète du comparatif, les huit modes structurels et la certification individuelle des présélections avec références exactes, contrôle des doublons et géométrie. Aucune activation supplémentaire.

## Contre-expertise indépendante

- Le dialogue de regroupement laissait le balayage horizontal atteindre la liseuse et son bouton de fermeture pouvait basculer les commandes. Le dialogue intercepte désormais la fermeture et la WebView suspend le changement de folio par balayage tant qu’il est ouvert. Le test Node couvre les transitions du signal au pont natif. Une vérification tactile Android reste nécessaire.
- Le chronomètre passif est suspendu au départ vers la comparaison. Le sélecteur de dossier, qui sera utilisé lorsqu’un verset aura plusieurs dossiers, laisse le chronomètre tourner. Ce cas reste à éprouver avant l’activation d’un premier verset à dossiers multiples.
- Le test d’agrandissement vérifie la disparition du folio voisin. La qualité du cadre agrandi reste à constater dans les captures.

## Reprise technique du 4 novembre 2026

**NO-GO pour l’intégration.** Base de comparaison PR n°8 `216f4d4b26e048656bc224fe1ade42c99240184d`. La passe précédente reste documentée ci-dessus.

| Priorité | Cause et correction | Reproduction et état |
|---|---|---|
| P0 | Dans `HifzSessionActivity`, la pause était placée dans l’action d’ouverture après le choix. Le commit `dfb92e45` suspend désormais le chrono avant le sélecteur, puis attend le retour de la comparaison ou l’annulation. `SessionClock` expose son état courant. | Contrat de source et test instrumenté `passiveSelectorFreezesClockOnCancelAndThroughComparison`. Le deuxième dossier est synthétique et reste dans le test. |
| P0 | Le contrôle de page courante bloquait le dossier actif sur le folio de clôture. Le commit `dfb92e45` relie l’action de clôture au verset issu de la validation et conserve cette autorisation dans l’état Android. Le chemin des marges est réservé au passif. Le sélecteur en cours sauvegarde sa source et le droit de reprise du chrono dans l’état Android. | Contrat de source reproduit. Le test Android ajouté passe par le bouton de validation, vérifie le reçu, recrée la séance et revient à sa clôture. Exécution à confirmer. |
| P1 | Les captures acceptaient un premier plan système et une mise en page incomplète. L’instrumentation exige désormais le premier plan de l’application, le Tafsīr chargé quand il est ouvert et deux SVG mesurables de géométrie originale. | `CorrespondenceContextInstrumentedTest` couvre R030/R037, l’agrandissement et le toucher du repère R030 depuis Lecture. Les PNG doivent encore être inspectés individuellement. |
| P1 | Un changement d’occurrence redemandait le folio A et réinitialisait sa sélection. Le changement d’occurrence est désormais circonscrit à B. La sélection du Tafsīr est sauvegardée par folio et effacée si elle sort de la page. Les commandes de chaque folio partagent maintenant une seule rangée. | Revue statique. Validation instrumentée de la sélection et de la rotation encore requise. |
| P1 | Le registre confondait le raccord sourcé R004 et sa classification historique, et ne distinguait pas explicitement les noyaux des contextes pour R026/R040/R053. Le statut de R037 est clarifié. | Contrôle de structure des 110 fiches, des décomptes 52/10/48 et de l’isolation des quatre pilotes. Les vérifications bibliographiques et QCF individuelles restent ouvertes. |

Le diff avec la PR n°8 conserve `hifz-core`, `HifzPrefs` et les classes de sauvegarde. La CI Android reprend la matrice d’essais isolés Hifz de la PR n°8 sur les deux profils, avec des données jetables. Ce contrôle du diff ne constitue pas à lui seul une non-régression runtime.

Restent ouverts avant un GO technique. Résultats du parcours actif instrumenté, rotation et destruction du processus en consultation, regroupement au toucher et coexistence des deux familles sur une page, examen visuel de toutes les captures, huit interactions structurelles, ergonomie et certification individuelle. La validation E-Ink sur BOOX physique reste distincte.

### Résultat intermédiaire du candidat 1a9c371a

Sur le run 41901522697, le JVM est vert. Le smartphone a exécuté six tests. Cinq ont passé, dont le toucher du repère R030 et la suspension du sélecteur passif. Le test actif a échoué en localisant le bouton de validation par description, avant tout appel à l’action. Le test cible désormais le vrai bouton créé par la séance. Le registre R032 distingue aussi la proposition locale attribuée à Farrin du découpage global, en maintenant la suspension.

### P0 reproduit sur un petit corpus acquis

Le journal Android de 41902777189, job BOOX émulé 126829314114, localise le bouton absent en amont de l’interface. La Révision active de la fixture 2:47–51 échoue dans `GeometryRepository.planEligibleLines`. La limite historique de vingt tours du Mushaf s’appliquait aussi lorsqu’à chaque tour le planificateur trouvait des lignes acquises. Le garde mesure maintenant un tour complet sans aucune ligne éligible. Le test instrumenté de validation active sert de reproduction. Le changement ne touche ni les crédits ni le stockage des reçus. La nouvelle exécution doit établir le résultat Android après correction.

## Clôture causale du 5 novembre 2026

Base relue avant écriture : `b659c0988997650607c41f6bfc4d1944ef3c7416` ; base PR n°8 inchangée : `216f4d4b26e048656bc224fe1ade42c99240184d`.

### Deux crashes, une cause démontrée

Les runs initiaux PR `41924131048` et push `41924124615` ont le même arbre Git `91c9166d4334fa0681e97158fe7399308cb528af` (la PR exécute le merge synthétique `06325e16`, le push exécute `b659c098`). L'exception de la classe ItqanCarryover sur BOOX et de PostFinalRecall sur smartphone est `IllegalStateException: Missing Hifz state key: sabqiStart`, dans `MainActivity.refreshAll`, via la complétion du chargeur de géométrie. Il ne s'agit pas d'une assertion ignorée ni d'une défaillance native.

La reproduction déterministe `HomeLoaderLifecycleInstrumentedTest` laisse le véritable chargeur terminer pendant que le thread principal est retenu, ferme l'accueil et efface les préférences jetables avant la livraison de son callback. **RED démontré sur les deux profils**, commit `9284e998bb70f0b4a2581b3aec03902db71c9c46`, run push `41977635439`, jobs BOOX `127080099362` et smartphone `127080099178`. Chaque ancienne classe passe séparément trois fois sur chaque profil ; ce résultat n'annule pas le RED causal. Le run PR parallèle est `41977642730`.

`shutdownNow()` ne retire pas un Runnable déjà posté sur le thread principal. La correction refuse les deux callbacks de chargement quand l'activité est en fermeture ou détruite. Elle ne remplace aucune clé absente, n'invente aucun état et ne modifie ni HifzPrefs ni les crédits.

La collecte par classe conserve désormais logcat tous buffers, boot_id avant/après, état des activités et ApplicationExitInfo. Elle ne relance pas un échec pour le masquer : tous les passages doivent terminer avec une assertion JUnit non nulle.

### Défaut visuel réellement observé

Le PNG `haafidh-agrandissement.png` de l'artefact BOOX `13369312311` du run `41924131048` coupe le bas du folio 7. Le SVG grandit, mais sa WebView est calculée depuis screenHeightDp sans soustraire les commandes, les insets et la hauteur réellement allouée au ScrollView. La correction borne les folios par le viewport Android mesuré, moins les 24+44 dp de leurs commandes. Le contrôle instrumenté exige aussi la hauteur et la largeur complètes visibles de chaque WebView. Le QCF original, ses polygones et son rapport 345/550 restent inchangés.

### Icônes, captures et validation finale

Le contrat Lucide link-2/git-compare reste inchangé. Le nom demandé Parallélisme est accepté par le schéma et le pictogramme historique ressemblance, avec compatibilité de cet ancien libellé. Un test instrumenté vérifie les huit en-têtes, leurs schémas non vides et l'égalité pixel à pixel des drawables affichés et contractuels. Les six familles sans dossier admis sont exercées par des fixtures de présentation synthétiques dans le test uniquement ; aucune relation scientifique n'est ajoutée.

Les captures supplémentaires montrent les deux familles en Lecture p.235 et le vrai comparatif Similitudes ouvert depuis son repère. Les huit contrôles Correspondances incluent aussi les deux pilotes R030/R037, le focus, le Tafsir, le retour de Lecture et les restrictions actives/passives. La matrice finale demande les suites JVM complètes hifz-core/hifz-app, source contracts, Android téléphone 1080×1920/420 et BOOX 1404×1872/226, les classes historiques isolées et la destruction réelle du processus.

**Les résultats de cette matrice et le verdict final sont à consigner dans la description PR n°9 après lecture des résultats et des PNG. Aucun PASS ni GO n'est déduit du simple lancement de la CI.**

### Quatre pilotes, sans activation automatique

`pilotes-preuves-20261105.json` consigne les références et les 39 ancres originales contrôlées. L'édition Jalalayn a été téléchargée de sa source et son SHA-256 a été recoupé avec le checkpoint historique. Al-Mahalli/al-Suyuti, traduction Feras Hamza, Fons Vitae / Royal Aal al-Bayt Institute, 2008, ISBN 9781891785160 ; pagination PDF de 1 à 675.

| Dossier | Preuve locale et source Jalalayn, pages PDF | QCF | Statut production |
|---|---|---|---|
| R030 | Répétition exacte 2:47/2:122 ; pp.37 et 48, renvoi explicite du commentaire 2:122 au verset similaire antérieur | p.7 / p.19 | Suspendu : admission éditoriale indépendante non attestée |
| R037 | Vision 12:4 et accomplissement explicitement désigné en 12:100 ; pp.235 et 245 | p.235 / p.247 | Suspendu : même porte d'admission |
| R047 | Texte entier identique exactement quatre fois, 54:17/22/32/40 ; pp.548–550 | p.529 / p.530 | Suspendu : même porte d'admission ; aucune structure globale inférée |
| R048 | Texte entier identique exactement 31 fois ; commentaire 55:13, p.552, confirme ce décompte | p.531–534, 31 ancres | Suspendu : même porte d'admission ; aucune symétrie globale inférée |

Le registre historique 52 présélections / 10 suspensions / 48 exclusions n'est pas reclassé. Aucun dossier activé en production ; les statuts ci-dessus portent sur l'admission finale des quatre pilotes, pas sur la réécriture des décisions historiques.

### Portes distinctes

La PR n°8 reste ouverte et brouillon, non fusionnée, et n'a pas été modifiée. Aucun moteur ou stockage ne doit être déclaré non régressé sans les résultats runtime finaux. La release reste interdite tant que le catalogue n'est pas effectivement admis/activé, que l'intégration 1.17.11 n'est pas validée, et que ressources privées, signature historique et migration sur BOOX physique ne sont pas vérifiées. Aucune APK n'est publiée par ce workflow.
