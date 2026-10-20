# Reprise F1–F5 — 20 octobre 2026

HEAD vérifié deux fois avant ce commit documentaire : `66d7d5b5ac020ed0de2541282047959af5d8bda2`.
Branche exclusive : `work/haafidh-1171-atlas-correspondances`. Ancienne branche inchangée.

## Autorité du mandat actuel

Le mandat utilisateur de finalisation F1–F5 remplace les anciennes demandes générales de certification des checkpoints J3/J4. Aucun nouveau système de certification ni expert externe obligatoire. Réutiliser les acquis, ne rouvrir que les types discutés, écarter individuellement les dossiers insuffisamment étayés sans bloquer les autres. Conserver les contrôles réels d'intégrité, de géométrie et de non-régression.

Ancienne fonction Amorces : libellé Ancres et icône ancre approuvée partout, sans changer les identifiants ni les états persistants. Visuels Atlas et Correspondances figés. Hifz inchangé. APK debug privé complet autorisé après vérifications automatisées ; validation physique BOOX ensuite. Aucune publication publique ni release finale autorisée.

## Vérifications réellement réalisées dans cette reprise

- Lecture de PROGRESSION_JALONS.md, NETTOYAGE_IBN_KATHIR_VERS_ATLAS_2026-10-20.md, J4_REMEMBERING_MUSHAF_UI_CONTRACT.md et PRIVATE114_SIGNED_BUILD_HANDOFF.md via GitHub.
- Lecture de CorrespondenceRuntime.java au SHA ci-dessus : APPROVED_SHA256 vide ; load renvoie un catalogue vide. Ce constat est statique, pas un test Android.
- Archive privée de matrice 110 retrouvée ; lecture du contenu ZIP impossible par la route textuelle disponible. Aucun contenu privé copié dans GitHub.
- Commande locale minimale impossible : outil d'exécution renvoie « No usable execution environment is available. Wait for an environment to become available before using this tool. » Le connecteur GitHub reste accessible.
- Aucun code applicatif modifié, aucun test exécuté, aucun APK créé dans cette reprise. Les PASS historiques restent des résultats historiques, pas des validations de ce mandat.

## Frise réelle de cette reprise

| Jalon | Statut | Preuve / suite |
|---|---|---|
| F1 types et table 110 | BLOQUÉ | Prélecture acquise ; registre ZIP retrouvé mais non extrait. Reprendre sa lecture dans un environnement opérationnel. Aucun verdict individuel nouveau. |
| F2 Atlas et Ancres | NON DÉMARRÉ | Checkpoints lus ; inspection et corrections applicatives, assets privés et rendu restent à exécuter. |
| F3 catalogue et mushaf | NON DÉMARRÉ | Hash runtime vide confirmé dans CorrespondenceRuntime.java. Aucun catalogue activé. |
| F4 qualité globale | BLOQUÉ | Exécution locale indisponible ; aucun Gradle, émulateur ou contrôle de captures exécuté. |
| F5 APK privé | BLOQUÉ | F1–F4 non terminés ; environnement de build indisponible. Disponibilité actuelle des assets et de la signature non vérifiée. |

## Reprise exacte

Rétablir un environnement d'exécution ; relire le vrai HEAD pour préserver tout travail concurrent. Extraire les registres privés existants hors dépôt public, puis poursuivre F1 → F5 conformément au mandat. Ne pas recommencer J0/J1 ni transformer ce checkpoint documentaire en progrès applicatif. Ne pas demander à l'utilisateur de renvoyer les archives déjà retrouvées. Aucune raison d'inventer un nouveau verrou éditorial.
