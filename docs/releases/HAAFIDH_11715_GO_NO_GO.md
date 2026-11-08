# Quran Haafidh 1.17.15 — BOOX UI-only release evidence

**Decision: NO-GO for installation, signing, merge and publication until the actual gates pass.** This is a draft candidate, NOT an official 1.17.15 release.

## Source lock

- Exact 1.17.14 ancestor: `64943fe77480869bd654fe7f7ba555a46fdba31f`.
- Candidate branch: `work/haafidh-11715-boox-visual-20261108`.
- PR 16 and existing fallback points: unchanged; this work is on a separate branch.
- Proposed native app identity: `com.quransafeguard.hifz`, `versionName=1.17.15`, `versionCode=53`. The existing private signing workflow was not changed.

## Requested visual modifications (display layer only)

| Area | 1.17.14 finding | 1.17.15 candidate |
| --- | --- | --- |
| A — Consolidation ×5 | Redundant `toutes les parties`, `passage final ×5` | `Consolidation · Cumul` only for the genuine complete-pass protocols; original frozen stage counter still shows ×5 (or historical ×1). |
| A — Renforcement ×10 | Last continuous A+B or A+B+C used the same `SNOWBALL` protocol as individual blocks | `Renforcement · Cumul` only when the last frozen unit matches the exact physically contiguous concatenation of preceding source lines. Existing ×10 frozen counter unchanged. |
| B — Parcours | Empty 0/N weekly summary, empty optional post-final recap and duplicated `Détails disponibles` | Empty information hidden. Existing clickable morning/evening details, OPEN sessions, saved history and Sunday finals remain available. |
| C — BOOX Mushaf | Side and center markers overemphasized | BOOX-only passive CSS opacity 0.88/0.90; QCF SVG, page sizing, masking, navigation, refresh calls and ink colors unchanged. |
| D — Frises | English title text slightly small | 11.5/15sp titles, tiny node spacing adjustment; Al-Qamar/Ar-Rahman/Al-Mursalat fixed content, Atlas graphic style and navigation unchanged. |
| E — Juz | Empty page silhouette subtle | 1.3× original stroke width **only if empty**. Nonempty shapes and acquired/stabilized/learned physical-line fills unchanged; no counters added. |
| F — Al-Fātiḥa | A blocked, empty or corrupted session could set `sessionCompleted` and spuriously show the closing folio | Both rendering entry points now require a verified existing **dated receipt** and no render error before invoking the *unchanged* closing Al-Fātiḥa method. |

## Immutable components

No change authorized to `HifzPrefs`, `ConsolidationCycleEngine`, `HifzBackup`, `PostFinalFamilyCreditLedger`, `MushafView`, `reader.js`, the 604 canonical QCF SVGs, Atlas/Tafsir/correspondence source data, protocol quotas, V6 state schema, private signing material or official source worktree.

## Automated gate

Workflow: `.github/workflows/haafidh-11715-boox-visual-gate.yml`.

1. Exact parent and strict allowlist: asserts no engine, credits, data backup, mask JavaScript or private path changed.
2. JVM and source-contract suite: `:hifz-core:test`, `:hifz-app:testDebugUnitTest`, `:hifz-app:sourceContractTest`.
3. Release-code compiler twice, normal and private-flags, through `scripts/no_apk.init.gradle`: no APK, no keystore, no upload.
4. Exact baseline capture: worktree at **64943fe**, same Android instrumented screenshot fixture copied into baseline, then candidate capture.
5. BOOX-emulated Android 1404×1872 / 226dpi actual screenshot PNGs: home, Parcours, 30 juz, selected Juz 1/30, real QCF reading, Al-Qamar R047, Ar-Rahman R048, Al-Mursalat R059, genuine persisted OPEN Itqān composite ×5 and Sabqi composite ×10.
6. Instrumented persistence, carryover, Sunday finals, real QCF physical groups, V6 migration, cumulative recall, process-death and active replay.

**Status on initial report creation: pending GitHub Actions run; screenshots and test evidence not yet available.** Before/after are authentic screenshots from an Android emulator configured to BOOX's dimensions and density, **not photographs or direct captures of BOOX hardware**. The 1.17.15 candidate must not be declared GO based on configuration alone.

## Mandatory release blockers outside public CI

- Review real PNG files and contrast/ghosting behavior on BOOX hardware; automated emulator capture cannot prove E-Ink refresh quality.
- Inspect every Android/JVM test result and compare before/after including active ×5 and ×10 with no fake final-receipt credits.
- Execute historical **private/offline** checks for exact installed package identity, 1.17.14 backup compatibility and historical signing certificate; current public CI checks only the preserved safety-guard code path, not the sensitive inputs or genuine in-place data migration.
- No final APK assembly/signature/publication without a separate explicit, verified private GO.

## Current decision

**NO-GO for official release.** Code changes are committed in an isolated candidate branch; evidence gates are configured and must finish successfully before a release decision changes. This status must be revised only against actual workflow run logs and SHA-pinned screenshots.
