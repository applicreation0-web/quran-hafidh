# CP-00 — État initial

Date : 2026-10-14 (Europe/London).

## Identité Git

- Dépôt : `applicreation0-web/quran-hafidh`.
- Branche : `work/haafidh-1171-ibn-kathir-integration-independent`.
- HEAD avant inventaire : `1aa4bac93b932614388801b3cfdcd2ad313df855`.
- HEAD GitHub observé : `1aa4bac93b932614388801b3cfdcd2ad313df855`.
- Arbre local avant inventaire : propre.
- HEAD après travail produit : inchangé ; ce checkpoint ne modifie que la
  documentation. Le SHA du commit de checkpoint est attesté dans `INDEX.md`
  après sa création, car un commit ne peut pas contenir son propre SHA.

## Preuves retrouvées

- CI Android du HEAD : run `38600109156`, terminé `success`, artefact
  `haafidh-android-runtime-1aa4bac93b932614388801b3cfdcd2ad313df855`.
  Le journal contient `numtests=98`, `OK (98 tests)`, préparation arrêt forcé
  `OK (1 test)` et vérification après reprise `OK (1 test)`.
- CI source/JVM du HEAD : run `38600109417`, terminé `success`.
- M8 longitudinal : XML récupéré, `tests=3`, `failures=0`, `errors=0`,
  `skipped=0` pour 180/365/548 jours.
- M7-B : manifeste SHA-256 intégralement valide ; 1 903 groupes, 1 708 titres,
  195 absences selon le rapport et le JSON approuvés.
- M9 : deux paquets privés récupérés ; le paquet corrigé passe son manifeste
  `CONTENTS_SHA256.txt` pour ses 22 fichiers.
- Debug M9 précédent : APK réel, somme enregistrée, notice et clé JKS récupérés
  hors dépôt. Leur réutilisation éventuelle reste soumise au contrôle du
  certificat et du mot de passe lors de CP-06.
- Audit contradictoire du 14 octobre récupéré, y compris ses deux captures.

## Carte sémantique et contrat visuel

- Le HEAD courant ne contient pas `IbnKathirMapActivity` et les tests publics
  interdisent encore l’ancienne Carte-liste : la nouvelle carte graphique reste
  donc à construire.
- Référence graphique disponible : `IslahiMapActivity.java` au commit
  `fbe557331faf58a06d4a1b2b8f43c80d9a95e5e9`.
- Référence Ibn Kathīr disponible : `IbnKathirMapActivity.java` au commit
  `ea15769ac68f32c59c39aae7088de25ad93717bd`.
- Le contrat public des 23 pictogrammes et la géométrie Lucide `key-round`
  existent déjà dans les checkpoints M5 ; quatre icônes historiques restent à
  juger par conformité interne et capture, sans inventer de nouvelle référence.

## Liste fermée des travaux manquants

1. Versionner le corpus approuvé sous chiffrement authentifié, sans clé dans Git.
2. Prouver par intégration la promotion dominicale Sabqi/Itqān jusque dans le
   corpus réellement parcouru par Révision active et sa persistance.
3. Construire la carte sémantique graphique Ibn Kathīr avec miniatures QCF,
   surlignage exact, Lecture et restauration d’état.
4. Certifier/corriger le visuel restant et l’indépendance de Mémorisation libre.
5. Exécuter les contrôles finaux pertinents sur le nouveau HEAD.
6. Produire un unique APK privé signé et vérifié, puis la checklist BOOX.

M1–M9 et les simulations historiques ne sont pas rouverts.

## Tests CP-00

| Contrôle | Résultat |
|---|---|
| HEAD local = HEAD GitHub | PASS |
| Arbre local initial propre | PASS |
| Deux workflows du HEAD terminés | PASS |
| Android 98/98 + arrêt/reprise | PASS (preuve récupérée) |
| M8 180/365/548 | PASS (preuve récupérée, non rejouée) |
| Manifeste M7-B | PASS |
| Manifeste paquet M9 corrigé | PASS |
| BOOX physique | NON EXÉCUTÉ |

## Artefacts et SHA-256

- `M7B_CLOTURE_FINALE_1903_2026-10-13.zip` :
  `a95d6a89a1f212a21589374ff456325396d64b3bde0587887926c567245acf16`.
- `M8R_180_365_548_PROOFS_2026-10-14.zip` :
  `33c8b8cb2b9f421bb2b3ca8cd87caf68db7c2993c3bb003c89aebfb2eacc14de`.
- `QURAN_HAAFIDH_M9_PAQUET_PRIVE_FINAL_SHA_AUDITE_2026-10-14.zip` :
  `761a2598f4c8cc0566cec39718e12a6151e37d397bcaf0312a65a6b349ffa17e`.
- `QURAN_HAAFIDH_M9_BUILD_PRIVE_CORRIGE_2026-10-14.zip` :
  `39f1a2f8f7c0295120b114179d502fc05cf9e0fee0bd8e8c35e55b8e38878c92`.
- APK M9 précédent :
  `107394fb8d514b96eedd45a219c440be27812cd811eee3d10457f1aea33dfa17`.
- Clé JKS M9 précédente :
  `fc5506f17e4dc5335be49d715900198ed0c3780d76bff7ae650d1fc6ab7a56cd`.

Références récupérables : fichiers Library d’origine et artefact GitHub Actions
du run `38600109156`. Aucun titre, secret ou APK n’est ajouté au dépôt.

## Reprise exacte

Première action : écrire les tests du format AEAD et du manifeste CP-01, puis
les faire échouer avant la création du conteneur chiffré.
