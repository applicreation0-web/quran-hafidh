# Quran Haafidh 1.17.11 — P0 Itqān physical-proof SOURCE JVM GREEN / RELEASE NO-GO

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

- **SOURCE JVM GATE GREEN at 9afb9fc only**: the complete Android Java compile, P0 focused tests, JVM/Core regression and source-contract jobs all returned success in CI 42260147939. Prior scalar-era fixtures were rewritten to earn true token-bound physical repetitions and test canonical subblocks. This **does not** certify Android device behavior, full independent T01–T30, original data import, or private release readiness.
- In particular, tests that inject `setItqanProgress(total)` directly and then complete a block, or substitute a five-line arbitrary fragment for a canonical Itqān plan, must be corrected using authentic geometry/session tokens and repeated protocol steps. Merely removing or skipping these tests is forbidden.
- No full T01–T30 matrix evidence on latest HEAD; no Android instrumentation or two-profile QCF screenshots; no disk persistence-failure proof, fault injection, DST/import/corrupt snapshot adjudication, release assemble/sign/physical BOOX upgrade proof.
- **NO-GO APK**: no private assets uploaded; no signed `com.quransafeguard.hifz` code-49 version 1.17.11 APK created by this work.
- **NO-GO BOOX PHYSIQUE**: no device available for an actual in-place upgrade and backup comparison.
- PRs #9/#10 intentionally left unmerged/unmodified. Do not transfer tests of an ancestor to a descendant or label this candidate production-ready.

Source CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42256238011
Follow-up CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42257020033
Latest CI: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42257457600

## GREEN JVM CLOSEOUT — 2026-11-06

- Tested exact source/test SHA: `9afb9fc17b071ae158116da6c4941a50cb373d43`
- GitHub Actions: https://github.com/applicreation0-web/quran-hafidh/actions/runs/42260147939 — `status=completed, conclusion=success`, job `real-jvm-and-android-compile` success.
- Real Android Java compile: PASS (`hifz-app:compileDebugJavaWithJavac`).
- Existing physical P0 test and legacy fraudulent payload test: PASS (targeted `testDebugUnitTest`).
- Full Hifz JVM + core tests: PASS (`hifz-core:test`, `hifz-app:testDebugUnitTest`).
- Source contracts: PASS (`hifz-app:sourceContractTest`).
- No APK assembled or signed. The no-APK guard passed.
- The previously failing 56 parameterized absent-week cases now run through actual QCF 8/7/7 blocks and earned physical repetition receipts; all prior failing classes reached success in the full suite.
- The physically authenticated test helper never creates credits via `setItqanProgress(total)`; historic fake scalar fixtures were repaired rather than skipped.
- Exactly the 11 commits ahead of PR #10 audit starting HEAD are isolated in the P0 branch. Candidate PR #9 unchanged.
- Remaining release obligations: audit full adversarial T01–T30, Android instrumentation and BOOX portrait emulator, process-death/backup/restore and encryption/key certificate review, in-place upgrade on actual BOOX before recommending private install. **RELEASE/DEVICE: NO-GO**.

This last section supersedes the provisional earlier CI status and the earlier source-JVM NO-GO wording. It does not establish a signed user-installable 1.17.11.
