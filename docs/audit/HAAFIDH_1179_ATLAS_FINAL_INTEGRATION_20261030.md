# Quran Haafidh 1.17.9 — validation de l’intégration Atlas

Date : 30 octobre 2026.
Dépôt : `applicreation0-web/quran-hafidh`.
Branche : `work/haafidh-atlas-discriminants-pilot-20261029`.

**Commit technique vérifié : `5ea88ab1b0e09b7a19cc74ba8346c9688d8adb48`.**
Base propre contrôlée : `7efa362c4b8ea65d4d95277d4c08b978a7d95efa`.
L’audit de départ 41034637683 a été vérifié et réutilisé.

## Résultat

[CI finale 41055535483 : succès](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41055535483).

- 14/14 tests Android instrumentés réussis ; aucun ignoré.
- 617 tests JVM application réussis ; 2 tests historiques conditionnels Ibn Kathīr ignorés, faute de `IK_J2_REVIEW_PATH`. Aucun nouveau test de cette intégration n’est ignoré.
- 285 contrats de source réussis.
- `hifz-core:test` réussi.
- 604 pages QCF épinglées vérifiées ; coordonnées de mots et polygones des versets conservés.
- Quatre captures Android réelles récupérées et exigées par le script CI.

## Comportement

**Révision passive.** Repère dans la marge du Mushaf et bouton contextuel. La référence doit être documentée, sur la page affichée et dans le parcours de la séance. Un toucher du repère n’est pas un toucher de validation du dernier verset révisé.

Le chronomètre est arrêté avant l’ouverture d’Atlas. Il reste arrêté dans la Lecture ouverte depuis Atlas. Le retour conserve l’instance de séance, la page, le dernier verset révisé, le masque et le choix de comparaison ; le chronomètre reprend selon le cycle de vie existant.

**Révision active.** Aucun repère ni accès Atlas pendant la restitution, même après choix du dernier verset ou utilisation de « Révéler ». L’autorisation d’affichage est créée seulement après réussite de la validation existante de la séance, pour une référence effectivement couverte. Une erreur de rendu, un masque levé ou un simple choix de verset ne déverrouillent rien.

Décision d’intégration : la comparaison attend la validation de la séance entière. Aucun reçu autonome de validation par page n’existe ; aucun nouveau protocole de validation ni crédit intermédiaire n’a été introduit. Cette restriction évite de dévoiler une suite encore en restitution.

**Atlas.** Les deux pages QCF authentiques sont conservées. Les extraits sont limités aux mots discriminants et à un mot voisin de contexte, avec les coordonnées exactes du corpus. Les chemins des glyphes ne sont ni reconstruits ni remis en ligne. Chaque extrait utilise son propre identifiant de clip SVG, vérifié dans le vrai DOM pour les quatre couples. Le pictogramme de deux Mushaf reliés est harmonisé entre marge et barre native. Aucun ajout de liste ou de pavé.

## Vérifications Android

| Groupe | Nombre | Contrôle |
|---|---:|---|
| Atlas | 5 | Deux Mushaf, quatre couples, extraits QCF, clips distincts, Lecture ↔ comparaison, invariance Hifz |
| Révision | 2 | Consultation imbriquée, chronomètre, retour exact, sauvegardes inchangées, verrou actif, recréation et validation réelle |
| Sauvegardes | 2 | Rejet des schémas invalides et des entrées surdimensionnées sans altération des magasins |
| Finales | 3 | Persistance et séparation des séances ordinaires et finales des deux familles |
| Masquage des finales | 2 | Aucune répétition ni crédit avant disponibilité effective du masque |

Le test de révision active vérifie **le masque effectivement peint** : valeur JavaScript 100, couche SVG de masque et trous de mots exacts présents, avant et après recréation. Il ne se contente pas du champ Java `currentMask`.

Le build CI exclut les titres privés. Sans eux, le comportement de repli visible préexistant est activé. Le contrôle visuel de la première série a détecté cette différence. Le test renforcé emploie des métadonnées de titres synthétiques uniquement dans `androidTest`, sur les limites canoniques et les véritables coordonnées QCF. Aucun texte coranique n’est généré et cette fixture n’est pas livrée dans l’application. Elle prouve le fonctionnement du masque et du verrou Atlas, pas la présence du pack privé dans une future installation.

## Périmètre préservé

Aucune modification de `hifz-core`, `HifzPrefs`, `HifzBackup`, des initialisations, des corpus, des paramètres de progression, des versions, des signatures ou du manifeste. Les algorithmes de planification et d’attribution des crédits restent inchangés. Dans `HifzSessionActivity`, les ajouts sont l’adaptateur d’interface, les liens et l’autorisation d’affichage temporaire après validation réussie.

Le code de l’application est inchangé depuis `cf5b07f1`. Les commits suivants portent sur les tests et leur exécution. Une revue indépendante du diff a identifié la collision possible entre clips SVG ; elle a été corrigée avant validation.

## Commits

| Commit | Objet |
|---|---|
| `cf5b07f1` | Intégration passive/active, extraits QCF, pictogrammes et tests |
| `37d0cbe2` | Contrôle DOM des clips ; contrat de validation limité aux méthodes qu’il protège |
| `92917727` | Respect de la confirmation réelle de validation partielle dans le corpus de test |
| `da68a9df` | Observation de l’ouverture conservée après contrôle négatif ; invariance en Lecture |
| `6a93785d` | Exécution du script dans un shell unique ; 14 tests sur un émulateur |
| `8af9aa8c` | Captures préservées dans MediaStore après désinstallation des paquets de test |
| `5ea88ab1` | Contrôle du masque SVG réel avec fixture de coordonnées canoniques |

[Diff complet depuis la base validée](https://github.com/applicreation0-web/quran-hafidh/compare/7efa362c4b8ea65d4d95277d4c08b978a7d95efa...5ea88ab1b0e09b7a19cc74ba8346c9688d8adb48).

## Captures réelles et résultats bruts

[Télécharger l’artefact CI](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41055535483/artifacts/12977725479).

Dans `atlas-comparison-audit/` :

- `atlas-pilot-emulator-portrait.png` : comparaison de deux pages et extraits QCF ;
- `atlas-revision-passive.png` : repère et bouton en révision passive ;
- `atlas-revision-active-masked.png` : vrai masque de restitution, sans repère Atlas ;
- `atlas-revision-active-validated.png` : accès après validation, avec Al-Fātiḥa de clôture.

Ce sont des rendus Android sur émulateur Pixel 2 / API 35 avec données de test isolées, pas des maquettes ni une modification de la progression personnelle. Les rapports XML/HTML et le journal QCF sont joints. L’archive ne contient pas d’APK.

Empreinte de l’artefact CI : `sha256:24f631d0cd13bc013f263b2e02b33a79978f72b6bd27fe9abd58c6d02b7e9ba0`.
Rétention GitHub annoncée jusqu’au 13 novembre 2026. L’environnement local ayant été interrompu, les captures finales sont fournies par cet artefact ; la dernière série renforcée est certifiée par les assertions Android/DOM et la récupération des fichiers.

## BOOX physique — validation restante

- Présence et intégrité du pack privé d’amorces sur la vraie installation.
- Rémanence E-Ink et absence de dévoilement furtif au chargement ou au retour.
- Lisibilité des deux pages, des extraits réduits et des soulignements.
- Confort des cibles tactiles, dont le bouton de secours si la marge est trop étroite.
- Veille/reprise et consultations prolongées avec contrôle du chronomètre.

Ces points ne sont pas certifiés par l’émulateur. Aucun APK d’installation livré, aucune release, aucune publication publique de l’application. Les paquets éphémères indispensables à l’instrumentation ne sont pas des livrables.
