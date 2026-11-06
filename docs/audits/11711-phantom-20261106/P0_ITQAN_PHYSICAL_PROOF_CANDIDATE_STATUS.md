# Quran Haafidh 1.17.11 — P0 Itqān physical-proof candidate (NO-GO)

Date: 2026-11-06. This is measured progress, **not** a release attestation.

## Exact baseline and isolated work

- Audit PR #10: `audit/haafidh-11711-phantom-validations-20261106`, verified HEAD `99c713fafdaed4fd8e847302abc4cf05e7b2dc79`. It is deliberately still draft/NO-GO.
- PR #9 candidate: `work/haafidh-correspondances-context-qcf-20261104`, verified at the start; unmodified by this work.
- Isolated branch: `fix/haafidh-11711-itqan-physical-proof-p0-20261106`, rooted in the audit HEAD, without rewriting or merging either PR.
- Corrective commits: `6f25c625d1089d05a6e4835f1a8c80a31868f844` (V7 proof and tests), `107c0751169eada8f080c1e5053160f5cb3a3dbf` (real Android checked JSON exceptions), `048e6fbb042c3b4557d5a61cebf122700897f663` (retire unauthenticated legacy advance endpoints, correct source contracts and icon), `a635cd7761d6c807f854e116b8d2369324008f06` (stale-window reset token protection).

## Causal finding

The prior legacy closure authenticated a scalar repetition count and parent verse bounds, yet trusted arbitrary caller-supplied `lineIds`, `finalBlock` and `nextCursor`; a full counter for Itqān 52:1–52:10 could be spent on foreign physical lines 604:0–604:1, crediting Stabilisé and changing the cursor. Historical scalar-only counters were not independent physical/session/date certificates.

The isolated V7 candidate introduces a persisted physical-block witness with canonical geometry, ordered lines, accepted bonus, origin date, protocol, quota, block index, expected cursor and unique session token. It records repetitions with token + expected counter, rejects forged/future block payloads and double taps, closes completion using the proof and clears it in the same commit. Historical OPENs without a matching physical certificate are preserved but require explicit restart of the repetition protocol; no automatic receipt is granted. This recovery still requires independent migration and BOOX checks.

A genuinely executed Android SDK compile exposed 43 checked-JSONException compilation errors in the uncompiled audit Sabqi source; fixed without changing source-protocol quotas. A second regression found a stale window could clear V7 via the old zero scalar setter; guarded and replaced in UI with token-bound explicit restart. The ancient advance-block endpoints were retired.

## Measured CI, no APK

- `42255436883` (`6b0e3c89`): RED, 43 Java checked-JSON compile errors, no JUnit executed.
- `42256238011` (`107c075`): actual Android compile **PASS**, targeted physical-proof and existing legacy RED-case checks **PASS**; full JVM **855 cases, 71 failed, 2 skipped**; source contracts **297 cases, 3 failed**. Produced test XML/reports as artifact `13522248367`.
- `42257020033` (`048e6fb`): actual Android compile and focused checks **PASS**; full JVM **855 cases, 70 failed, 2 skipped**. The three source contracts no longer fail. Remaining failures prominently involve old fake scalar `MorningProtocolFixture` paths and noncanonical test setups; do not waive them or reinterpret them as accepted real lessons.
- `42257457600` (`a635cd7`): actual Android Java compile and focused P0 tests **PASS**, source-contract suite **PASS**; entire JVM suite **855 tests, 70 failures, 2 skipped**. No source GO. The failing classes are exactly: `PartialFinalEightWeekStressTest` (56 parameterized cases), `AtomicItqanRotationRegressionTest` (3), `EarnedEveningProtocolRegressionTest` (3), `ItqanPostNasMaintenanceTest` (3), `SnowballValidationIdempotenceTest` (2), and one case each in `CarryoverEightWeekStressTest`, `ItqanFourSessionStoreTest`, `SundayPromotionActiveCorpusIntegrationTest`. No currently red `HifzActionRoleCoverageTest`. This is a negative regression gate, not a release result.

## Conditions not met

- **NO-GO SOURCE** until full JVM/Core/source suite is green and independently reviewed, old fixtures are rewritten to actually earn physical repetitions (not simply bypassed), and audit T01–T30 fully tested for both negative and canonical positive paths.
- In particular, tests that inject `setItqanProgress(total)` directly and then complete a block, or substitute a five-line arbitrary fragment for a canonical Itqān plan, must be corrected using authentic geometry/session tokens and repeated protocol steps. Merely removing or skipping these tests is forbidden.
- No full T01–T30 matrix evidence on latest HEAD; no Android instrumentation or two-profile QCF screenshots; no disk persistence-failure proof, fault injection, DST/import/corrupt snapshot adjudication, release assemble/sign/physical BOOX upgrade proof.
- **NO-GO APK**: no private assets uploaded; no signed `com.quransafeguard.hifz` code-49 version 1.17.11 APK created by this work.
- **NO-GO BOOX PHYSIQUE**: no device available for an actual in-place upgrade and backup comparison.
- PRs #9/#10 intentionally left unmerged/unmodified. Do not transfer tests of an ancestor to a descendant or label this candidate production-ready.

Source CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42256238011
Follow-up CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42257020033
Latest CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42257457600
