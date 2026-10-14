# CP-01 — Titres Amorces chiffrés dans GitHub

Date : 2026-10-14 (Europe/London).

## Identité Git

- HEAD GitHub avant : `3597602ff82cb0240ac4b2d9e7600e4ce0117407`.
- Branche : `work/haafidh-1171-ibn-kathir-integration-independent`.
- Commit produit/checkpoint : `42ef8f29f6a995330f63d2f22ab937a0086503aa`.
- L’attestation du SHA est un commit documentaire distinct, car un commit ne
  peut pas contenir son propre identifiant.
- Référence officielle 1.17.1 : inchangée.

## Travail terminé

- Les six fichiers M7-B et les huit chunks M9 approuvés ont été vérifiés sans
  régénérer ni modifier le corpus.
- Un ZIP privé déterministe regroupe les décisions/provenances M7-B et les huit
  chunks ; son SHA-256 est
  `3723f16d5ddf43fce7bd34d09c5ba8b7ee7d5ed765c70e02d49371f1b02ef642`.
- Ce ZIP a été chiffré avec AES-256-GCM et un nonce aléatoire. Le magic, le
  nonce et le contexte fixe `1903/1708/195/8` sont authentifiés comme AAD.
- Git reçoit seulement le conteneur `QHAEAD01`, le manifeste, les sommes et les
  outils sans dépendance externe. Ni la clé, ni le ZIP privé, ni les titres en
  clair ne sont versionnés.
- La clé brute de 32 octets a été sauvegardée séparément sous le nom
  `quran-haafidh-amorces-aead-2026-10-14.key`. Son empreinte publique de
  contrôle est
  `466a9affce1118bafa946c7824edeb4fe02994216b3ff786e2f01d07c4864945`.
- Le chiffrement ne change pas le statut de droits : cette sauvegarde technique
  n’est pas une autorisation de redistribution du contenu déchiffré.

## Invariants vérifiés

| Invariant | Résultat |
|---|---|
| 1 903 identifiants uniques | PASS |
| 1 708 titres exacts | PASS |
| 195 absences intentionnelles | PASS |
| Huit chunks et SHA-256 d’origine | PASS |
| Un titre stable par groupe | PASS, égalité exacte M7-B ↔ chunks |
| Frontières de versets inchangées | PASS, corpus M7-B inclus sans réécriture |
| Déchiffrement/reconstruction locale | PASS |
| Mauvaise clé rejetée | PASS |
| Conteneur altéré rejeté avant écriture | PASS |
| Titres en clair dans les fichiers Git proposés | 0 |

## Cycle TDD et commandes

RED initial :

```text
test_repository_bundle_manifest_is_source_safe ... FAIL (conteneur absent)
test_round_trip_and_tamper_rejection ... FAIL (outil absent)
```

GREEN final :

```bash
python3 scripts/test_amorce_aead_bundle.py -v
(cd secure-corpus/ibn-kathir-amorces-v1 && sha256sum -c SHA256SUMS.txt)
python3 scripts/verify_private_amorce_recovery.py \
  --secret-file /chemin/prive/quran-haafidh-amorces-aead-2026-10-14.key \
  --container secure-corpus/ibn-kathir-amorces-v1/amorces-m7b-1903.qhaead
```

Résultat : 2/2 tests PASS ; manifeste et conteneur PASS ; reconstruction privée
PASS pour 1 903 / 1 708 / 195 / 8.

## Fichiers modifiés ou créés

- `tools/amorce-aead/AmorceAeadTool.java`
- `scripts/pack_private_amorces.py`
- `scripts/test_amorce_aead_bundle.py`
- `scripts/verify_private_amorce_recovery.py`
- `secure-corpus/ibn-kathir-amorces-v1/README.md`
- `secure-corpus/ibn-kathir-amorces-v1/manifest.json`
- `secure-corpus/ibn-kathir-amorces-v1/SHA256SUMS.txt`
- `secure-corpus/ibn-kathir-amorces-v1/amorces-m7b-1903.qhaead`
- Documents du checkpoint courant.

## SHA-256 des livrables

- Conteneur public :
  `32f49284a0e5845b71b398cfcfd6e01a3f0e9413fad568c1eabb71c7f9656ce8`.
- Manifeste public :
  `48071f72ca415bd01e6eaa6d2d8a03b0bad59c4eb71bfffa0b2ef313729097a7`.
- Archive privée reconstruite :
  `3723f16d5ddf43fce7bd34d09c5ba8b7ee7d5ed765c70e02d49371f1b02ef642`.

## Références récupérables

- Public : dossier `secure-corpus/ibn-kathir-amorces-v1/` sur la branche.
- Privé : fichier de clé sauvegardé séparément, nom et empreinte ci-dessus.
- Sources approuvées : paquets M7-B et M9 listés dans CP-00.

## Problèmes restants

- Aucun défaut CP-01 connu.
- Le debug final devra déchiffrer hors CI, vérifier le corpus, copier seulement
  les huit chunks dans le chemin Android ignoré, puis prouver l’absence de clé
  dans l’APK.

## Reprise exacte

Ouvrir CP-02 et écrire les tests d’intégration des promotions dominicales Sabqi
et Itqān jusque dans `activeMurajaahCorpus()`, avant toute correction moteur.
