# Atlas simplifié — debug signé du 16 octobre 2026

Code final : `600058942b51a5775a5dec774d321ef0a39a236d`.
APK : `Quran-Haafidh-ATLAS-SIMPLIFIE-60005894-PRIVATE-DEBUG.apk`.
SHA-256 : `8132e5fceccf06ad1b5588f8dee2d1558993a060f5394c2c286cb07d6e0c48c1`.

Page entière et titre intégré sans fenêtre opaque ; pastilles de page, curseurs agrandis, règle arrondie, feuilletage dans l'unité et deux actions restantes. Android 38960138661 PASS (101 + 1 + 1, 29 captures), JVM/source 38960138983 PASS, compilation sans textes 38960139016 PASS. Signature v2/v3 et compatibilité statique avec le debug 91890d74 PASS.

**RELEASE BLOQUÉE** : runtime privé signé, audit indépendant et recette physique BOOX non exécutés. Sources et textes Hifz/Jalalayn/arabes et les trois assets privés inchangés. Aucun secret ou contenu privé dans GitHub/CI.
Voir [ATLAS_QCF_114/CP_ATLAS_SIMPLIFICATION_2026-10-16.md](ATLAS_QCF_114/CP_ATLAS_SIMPLIFICATION_2026-10-16.md). Ce checkpoint est documentaire ; les résultats appartiennent au code final 60005894, sans relance inutile de CI après documentation.

---

# Atlas QCF — signature personnelle vérifiée, 16 octobre 2026

Le debug personnel `Quran-Haafidh-ATLAS-91890d74-PRIVATE-DEBUG.apk` est signé avec le certificat du Soleil de référence. SHA-256 : `5e06fb6d343b9173bf203991437fef94d6ed511262e8ddfc4aaca64f9c08a3f6`.

Signatures v2/v3, alignement, CRC et compatibilité statique avec l'APK Soleil de référence : PASS. Même package et versionCode 39. Aucun contenu applicatif modifié par la signature. Aucun keystore, mot de passe ou texte privé dans GitHub/CI. Aucune installation ni modification de données Hifz effectuée.

**RELEASE BLOQUÉE** : exécution du binaire privé signé, audit indépendant favorable, recette BOOX physique et sauvegarde/restauration restent à valider. Voir [ATLAS_QCF_114/SIGNATURE_VERIFIED_2026-10-16.md](ATLAS_QCF_114/SIGNATURE_VERIFIED_2026-10-16.md).
Le code reste `91890d7412821103113c5faf98f3aafd750643bb` ; ce checkpoint ne modifie que la documentation, sans nouvelle compilation ni CI inutile.

---

# Atlas QCF — reprise technique du 16 octobre 2026

Code compilé et testé : `91890d7412821103113c5faf98f3aafd750643bb`.
CI Android `38952593226` PASS (101 + 1 + 1), JVM/source `38952593472` PASS, compilation code privé sans données `38952593215` PASS.

**CP-ATLAS-04 PARTIEL — DEBUG INSTALLABLE ET RELEASE BLOQUÉS.** Le candidat privé est assemblé mais non signé. La clé du certificat `85cd816f5213ae81f450146789c13ec476df54d7be8cc5f806decc9ed3f7061e` manque ; audit indépendant et recette BOOX physique NON EXÉCUTÉS. Aucune désinstallation ni remplacement de signature.

Détails et limites : [ATLAS_QCF_114/STATUS_CURRENT.md](ATLAS_QCF_114/STATUS_CURRENT.md).
Ce checkpoint est documentaire ; les résultats sont rattachés au SHA de code ci-dessus, pas à un binaire nouvellement signé.

---

# Current Work 5.6 status

Date: 2026-10-12.

- Repository: `applicreation0-web/quran-hafidh`.
- Branch: `work/haafidh-1171-ibn-kathir-integration-independent`.
- Official reference: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Last completed functional milestone: M6 evidence SHA
  `7289b2f18cfc69dab2b2d37c227e6486e29ba30c`.
- Current movement: publish the independent Work High handover packet.
- Distribution/debug verdict: **NO-GO**.

## M3 — complete

Checkpoint `8110185c4825b612994bc852a51683bf26145b04`:
source/JVM `38237473916` PASS; Android `38237474035` PASS. Exact Android
result: 98/98 main tests, preparation 1/1, external `am force-stop`,
fresh-process verification 1/1. T-009 and T-010 remain NON_EXECUTE without
physical BOOX/pixel proof.

## M4 — complete, source-safe

Checkpoint `20c9bd5707c3903aa6fa45e2a391b796a96a585a`:
source/JVM `38238427503` PASS; Android `38238427459` PASS with 98/98 plus
the external force-stop phases. The reviewed private candidate remains outside
Git. Final private-title pixels remain NON_EXECUTE.

## M5 — complete with explicit visual limits

Checkpoint `92f93e5ec73408c0456d8ebaa504f816cc9863b8`:
source/JVM `38273528132` PASS; Android `38273528162` PASS with exact
`OK (98 tests)`, preparation 1/1, external force-stop and fresh-process 1/1.

The Amorces key now matches pinned Lucide `key-round` geometry. Nineteen
Lucide silhouettes pass geometry comparison. Reading, documentary Carte,
Tafsir and pencil are NON_CERTIFIABLE because no independent approved
reference was supplied. Private-title captures and physical BOOX checks remain
NON_EXECUTE; emulator/source results are not a physical-device certificate.

## M6 — complete with rights restriction

The baseline private sidecar verifies 1,903 groups / 6,236 verses / eight
chunks. The 69:44–52 candidate is 1,710 staged / 154 absent / 39 quarantined;
the old baseline validator rejects this expected one-group delta and is not
misreported as PASS.

The historical spa5k assets and two obsolete providers remain audit inputs but
are relocated from `src/main` to `src/audit/legacy-ibn-kathir`, outside
Android's default source sets. A RED→GREEN verifier and CI gate cover the
isolation. The first unsupported AGP `exclude` attempt failed runs
`38274414563` and `38274414561` before tests and was replaced, not hidden.
No Hifz engine, persistence, Jalalayn or QCF geometry was modified.

The personal-only decision is applied without adding network, account, server
or synchronization. It does not establish durable-content authorization:
distribution containing the private titles remains NO-GO. Content Sync 169
publication/access and physical/private visual proofs remain NON_EXECUTE.

Same-SHA source/JVM `38275169555` and Android `38275169442` are PASS.
Android executed 98/98 plus preparation 1/1, external `am force-stop`, and
fresh-process verification 1/1. The predecessor runs `38274414563` and
`38274414561` failed before tests on unsupported AGP exclusion syntax and
remain documented as RED evidence.

## Next gate

Publish `HANDOVER_WORK_HIGH_M7.md` as a documentation-only descendant of the
M6 evidence SHA. Work High itself remains a separate READ-ONLY audit and is not
launched here.
