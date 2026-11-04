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
