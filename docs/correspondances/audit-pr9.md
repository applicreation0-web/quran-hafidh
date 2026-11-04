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
