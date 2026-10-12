# Handover to independent Work High M7

Prepared: 2026-10-12. This document prepares, but does not execute, the
independent READ-ONLY High audit.

## Immutable scope and final identity

- Repository: `applicreation0-web/quran-hafidh`.
- Branch: `work/haafidh-1171-ibn-kathir-integration-independent`.
- Official 1.17.1 reference: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Final functional/code evidence SHA:
  `7289b2f18cfc69dab2b2d37c227e6486e29ba30c`.
- Handover publication: the documentation-only commit containing this file.
  Its exact commit SHA is supplied with the final handoff because a Git commit
  cannot self-embed its own content-dependent SHA.
- Distribution/debug verdict: **NO-GO**.
- No distributable APK, debug or release was built or published by this Work.

## Public checkpoint chain

| Milestone | Public evidence |
|---|---|
| M1 | `CP-01-M1.md` |
| M2 | `CP-02-M2-RED.md`, `CP-03-M2-FIXTURE-1.md`, `CP-04-M2-RUNTIME-RED.md`, `CP-05-M2-RUNTIME-30-RED.md`, `CP-06-M2-FIXTURE-2.md`, `CP-07-M2-FIXTURE-3.md`, `CP-08-M2-FINAL-GATE.md`, `CP-09-M2-RED-HANDOFF.md` |
| M3 | `CP-10-M3-MIGRATION-RED.md`, `CP-11-M3-MIGRATION-GREEN.md`, `M3_HIFZ_EVIDENCE_MATRIX.md` |
| M4 | `CP-12-M4-QURANCOM-SOURCE-SAFE.md` |
| M5 | `CP-13-M5-BOOX-VISUAL-CONTRACT.md` |
| M6 | `CP-14-M6-PRIVATE-ISOLATION.md` |

`INDEX.md` and `STATUS_CURRENT.md` are the navigation/current-state records.

## M3 Hifz and durable persistence

Migration RED run `38207906981` executed all 98 tests and failed the two
unfinished-tail cases. The migration fix was GREEN at `e61a27f`: source/JVM
run `38208907474`, Android run `38208907462`, 98/98.

The first external-force-stop Android run `38235307061` then found a separate
real process-death defect: asynchronous elapsed-time storage lost `420321ms`
although Itqan and Sabqi survived. The minimal checked synchronous write was
GREEN at evidence SHA `84bee60f1fb364a97ac8622c53f92ba484d121c1`:

- source/JVM `38236706373`: PASS;
- Android `38236706405`: PASS;
- main inventory/presence/compile/discovery/execution: 98/98/98/98;
- external preparation 1/1, real `am force-stop`, fresh-process verification
  1/1; those two calls re-run tests from the 98-file inventory and are not
  represented as 100 distinct tests.

The published M3 checkpoint `8110185c4825b612994bc852a51683bf26145b04`
was independently re-run: source/JVM `38237473916` PASS and Android
`38237474035` PASS with the same 98 + force-stop phases.

The strict seven-case classification is in `M3_HIFZ_EVIDENCE_MATRIX.md`:
T-040, T-054, T-077, T-130 and T-131 PASS; T-009 and T-010 remain
NON_EXECUTE without physical BOOX/pixel proof. Itqan pre/post An-Nas,
four dates/40 repetitions/masks, consolidation ×7, old acquired material,
single credit, Sunday policy, seven-day planning, delays, programme changes,
eight-week snowball, progression, histories and V6 preferences are mapped
there to executed evidence. 98/98 is not a general product or BOOX certificate.

## M4 Quran.com titles and exact amorces

The private V2 package and eight chunks were verified in isolation. Public
Git contains no private title text/chunk/capture. Boundaries remain 1,903
groups / 114 surahs / 6,236 verses; geometry remains 604 QCF pages / 77,432
word boxes.

Mechanical V2 was 1,709 staged / 154 without h2 / 40 quarantined. Targeted
review identified `69:44–52` as a false-positive and produced an isolated
candidate of 1,710 staged / 2,891 source headings / 39 quarantined. The 17
possible title/commentary splits remain NON_EXECUTE because the supplied
extraction does not independently prove their original HTML delimiter.
Extraction counts are not editorial certification.

Four reviewed first-display offsets are public only as group IDs and integers:
11:1–4 → 2, 18:1–5 → 2, 20:1–8 → 1, 79:1–14 → 1. Source strings are never
rewritten or reordered; cycling keeps every heading. Missing data fails closed.
M4 same-SHA CI is recorded in CP-12.

The private review archive and title-bearing evidence must be transferred
outside public GitHub. Archive SHA-256:
`5019acd727f423515977d9759d4c25e8f7b20f4dde1c59e943f46a3b882949fa`.
Work High must not print or commit its title contents.

## M5 visual/icon result

The audit method was corrected: blob identity is no longer treated as graphic
conformity. The old `ic_ui_recall_key.xml` blob
`bb6670756b22dfb43ab94d9c56e2c66ac87ce7d3` did not match approved
`key-round`. Its body, keyhole, 2-unit stroke and round caps/joins now match the
pinned Lucide reference; the historical and corrected SHAs are retained in
CP-13. CI separately checks byte integrity and approved geometry.

Nineteen pinned Lucide silhouettes PASS geometry comparison. Reading,
historical documentary network, Tafsir and pencil remain NON_CERTIFIABLE
because no independent approved reference image was supplied; no replacement
was invented. The separate Itqan anchor is retained. The dormant Quiz vector
is present only in the historical 23-icon manifest and is not a screen.

Existing screens have source/runtime/emulator evidence as classified in CP-13.
Final private-title captures AND-01..AND-10 and all physical BOOX captures
BOOX-01..BOOX-03 remain NON_EXECUTE. Public no-title screenshots do not prove
the final popup, clipping, landscape or E-Ink result.

M5 checkpoint SHA `92f93e5ec73408c0456d8ebaa504f816cc9863b8`:
source/JVM `38273528132` PASS; Android `38273528162` PASS with exact
98/98, external force-stop preparation 1/1 and fresh-process verification 1/1.

## M6 private isolation and rights/storage result

Official Quran Foundation terms dated 2026-10-11 and Content Sync documents
were rechecked. Ordinary API content cannot be kept indefinitely. Content Sync
is the documented offline path for supported resources, with at-least-seven-
day synchronization when reachable and prompt sync after an outage; its
storage exception does not itself authorize a prepackaged build-time bundle.

The user confirmed a strictly personal, non-public configuration and prohibited
new network/account/sync architecture. M6 therefore completes the feasible
isolation work without treating personal use as a legal exemption. The eight
private chunks remain outside Git and public CI. No sync/network/backend or
account requirement was implemented.

A concrete packaging risk was corrected: the eight historical spa5k assets and
two obsolete Java title providers remain in Git as audit inputs but were moved
from `src/main` to `src/audit/legacy-ibn-kathir`, outside Android's default
source sets. A RED→GREEN check and CI gate prove that the reader uses only the
isolated Quran.com provider and that the obsolete sources cannot enter the
application payload. The initial unsupported AGP `exclude` attempt and its
two pre-test CI failures are recorded in CP-14.

No targeted written authorization was supplied, and generic `tafsirs`
Content Sync documentation does not prove publication/access or exact title
fields for `tafsirs:169`. Durable rights therefore remain unresolved.
Distribution containing private titles stays NO-GO.

M6 evidence SHA `7289b2f18cfc69dab2b2d37c227e6486e29ba30c`:
source/JVM `38275169555` PASS; Android `38275169442` PASS with exact
98/98 plus external force-stop preparation/verification. The prior
`73627055968e8b789f4e3067a158f71230ed16d3` and runs `38274414563` /
`38274414561` are RED evidence: both failed before tests on unsupported AGP
source-set exclusion syntax and were superseded by physical `src/audit`
relocation.

## Open anomalies for independent audit

| Severity | Item | Status |
|---|---|---|
| P0 | Durable authorization for embedded private Quran.com titles | **NO-GO DISTRIBUTION** |
| P0 | Final private-title Android visual set | **NON_EXECUTE** |
| P0 | Physical BOOX glyph/mask/title/orientation verification | **NON_EXECUTE** |
| P1 | Independent approved references for four historical icons | **NON_CERTIFIABLE** |
| P1 | Seventeen possible title/commentary splits | **NON_EXECUTE**, quarantined |
| P1 | Content Sync 169 publication, fields and access | **NON_EXECUTE**, no implementation authorized |
| P2 | Five dynamically resolved icon call sites | Device review still required |

## Negative-scope proof to recheck READ-ONLY

At final HEAD, Work High should verify:

1. `QuizActivity.java` is absent; home/runtime tests reject Quiz and the retired
   semantic-list Carte. The historical drawable alone is not a feature.
2. No private directory is tracked under
   `hifz-app/src/main/assets/ibn_kathir_qurancom_private/`, and no
   title-bearing capture, token, secret or private chunk exists in Git.
3. Historical `ibn_kathir_headings/**`, `IbnKathirOfflineTitles.java` and
   `IbnKathirTitlePack.java` exist only below
   `hifz-app/src/audit/legacy-ibn-kathir`, not `src/main`.
4. M5 changes touch only the recall-key drawable, icon contracts/checkers,
   workflow/checkpoints; M6 adds only packaging isolation and its checks. No
   Hifz engine, persistence, mask, counter, scheduler, Jalalayn or QCF geometry
   file is changed by M5/M6.
5. The authorized M3 production deltas are limited to the documented V5→V6
   adapter and durable elapsed-time write, with RED→GREEN evidence.
6. M4 production logic is limited to source-safe first-title offsets and keeps
   title data isolated from HifzPrefs, masks, counters and progression.
7. Jalalayn content/logic and the 604-page/77,432-box QCF geometry are unchanged
   by M3–M6.

## Independent High entry condition

Run Work High separately and READ-ONLY on the handover publication commit,
whose functional/code parent evidence is the exact SHA above. Do not use this
handover as a PASS, do not publish private evidence, and do not build a debug
or release. A new debug can be considered only after contradictory audit, P0
resolution and explicit user authorization.
