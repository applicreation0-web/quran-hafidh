# Quran Haafidh Work 5.6 M1–M6 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the evidence and compliance defects reported at audited SHA `43a991e5a6b75f939e50da313078a1c2b1419240` without degrading Hifz engines, persistent data, BOOX UI, Quran geometry, or the 23 contractual icons.

**Architecture:** Keep the official 1.17.1 Hifz core and protected runtime behavior frozen. Restore Android-test evidence first, then make only RED→GREEN fixes for reproduced defects. Handle the private Quran.com title corpus through an untracked ephemeral mount and keep all public checkpoints free of licensed text.

**Tech Stack:** Android Java/Kotlin, Gradle 9.5, JUnit 4, AndroidX instrumentation, GitHub Actions, Python and shell verification scripts.

**Spec:** External private packet `QURAN_HAAFIDH_WORK_56_MEDIUM_M1_M6_PACK_UNIQUE_2026-10-11.zip`, operational prompt `00_PROMPT_UNIQUE_WORK_56_MEDIUM.md`.

## Global Constraints

- Repository: `applicreation0-web/quran-hafidh`.
- Exclusive branch: `work/haafidh-1171-ibn-kathir-integration-independent`.
- Audited starting SHA: `43a991e5a6b75f939e50da313078a1c2b1419240`.
- Official 1.17.1 reference: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- No new branch, repository, project, pull request, release, force-push, or distributable APK.
- No full Ibn Kathir tafsir and no Jalalayn modification.
- Never commit private Quran.com data, private captures, secrets, or personal data.
- Preserve 1,903 passage boundaries, 6,236 verses, 604 pages, 77,432 QCF boxes, and 23 contractual icon blobs.
- Do not modify a Hifz engine without a reproduced defect, a failing test, a minimal fix, and a passing non-regression run.
- Treat `NON_EXECUTE` as missing proof, never as PASS.
- Require one pushed public checkpoint before starting the next movement.

## Review Focus

- All 98 instrumented tests must be discovered and accounted for; no workflow deletion or silent exclusion is allowed.
- Migration and process-death tests must exercise durable state, not only in-memory helpers.
- Sunday must allow Itqan/final validations while scheduling no active or general review.
- Private-title tests must prove verbatim ordering and fail closed without publishing source text.
- Emulator evidence must never be described as physical BOOX ghosting evidence.

---

### Task 1: M1 freeze, inventory, and recoverable checkpoint

**Files:**
- Create: `docs/work-checkpoints/INDEX.md`
- Create: `docs/work-checkpoints/STATUS_CURRENT.md`
- Create: `docs/work-checkpoints/ANDROID_TEST_INVENTORY.md`
- Create: `docs/work-checkpoints/CP-01-M1.md`

**Interfaces:**
- Consumes: audited SHA, High audit matrix, current workflow and test source tree.
- Produces: frozen baseline, exact 25-file/98-test inventory, movement ledger, and M2 acceptance criteria.

- [ ] Verify branch, HEAD, clean status, and ancestry from official 1.17.1.
- [ ] Verify all supplied SHA-256 values before reading audit evidence.
- [ ] Count and classify all Android instrumented tests and record why only seven currently execute.
- [ ] Record protected-engine delta and the 15-minute official active-review constant without changing behavior.
- [ ] Commit and push CP-01-M1; verify the remote SHA and same-SHA Actions URL.

### Task 2: M2 restore Android coverage

**Files:**
- Modify: `.github/workflows/diagnose-quiz-launch-android.yml`
- Modify only as RED evidence requires: `hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/*InstrumentedTest.java`
- Update: `docs/work-checkpoints/ANDROID_TEST_INVENTORY.md`
- Create: `docs/work-checkpoints/CP-02-M2.md`

**Interfaces:**
- Consumes: M1 inventory and compile failures from an unfiltered same-SHA CI run.
- Produces: a workflow that compiles/discovers all 98 tests and a complete executed/failed/ignored manifest.

- [ ] Remove the ephemeral test-source deletion and add an explicit 98-test discovery assertion.
- [ ] Run CI to capture the unfiltered RED compile/runtime evidence.
- [ ] Repair obsolete fixtures/assertions only where the official contract proves them obsolete.
- [ ] Re-run the entire instrumented suite and public JVM/source-contract suites.
- [ ] Commit and push CP-02-M2 with exact counts and same-SHA runs.

### Task 3: M3 prove protected Hifz behavior

**Files:**
- Test first: relevant JVM or instrumented test file for each reproduced defect.
- Modify production Hifz files only after a matching RED test.
- Create: `docs/work-checkpoints/CP-03-M3.md`

**Interfaces:**
- Consumes: M2 full-suite harness.
- Produces: explicit outcomes for T-009, T-010, T-040, T-054, T-077, T-130, and T-131 plus persistence and eight-week planning evidence.

- [ ] Map each required audit case to one or more executed tests.
- [ ] Exercise migrations, interruption/restart, physical page/surah boundaries, Sunday, lateness, and eight-week schedules.
- [ ] For each real bug, record RED, implement the minimal fix, then prove GREEN and non-regression.
- [ ] Leave production engines unchanged when a defect cannot be reproduced.
- [ ] Commit and push CP-03-M3 with evidence links.

### Task 4: M4 verify private Quran.com title selection

**Files:**
- Modify only if evidence requires: deterministic private title index/selection adapter.
- Test: private untracked validation fixtures plus public source-safe contract tests.
- Create: `docs/work-checkpoints/CP-04-M4.md`

**Interfaces:**
- Consumes: private source pack and the 18-group High audit queue.
- Produces: source-verbatim targeted decisions, validated 1,903-group mapping, and private evidence ZIP.

- [ ] Mount the private pack outside the Git index and verify its manifest.
- [ ] Review the 18 quarantines and four known initial-order cases against source.
- [ ] Write failing selection tests before any deterministic-index correction.
- [ ] Validate all 1,903 mappings and eight chunks without emitting private text publicly.
- [ ] Commit and push CP-04-M4; provide the private decision annex separately.

### Task 5: M5 Android visual evidence and BOOX limit

**Files:**
- Modify UI only after a measurable RED layout/pixel failure.
- Create: `docs/work-checkpoints/CP-05-M5.md`
- Create privately: visual evidence ZIP and manifest.

**Interfaces:**
- Consumes: private corpus mount and M4 candidate.
- Produces: Android measurements/captures for pages 294, 518, 520, 526–528, and 604; physical BOOX remains `NON_EXECUTE` without device evidence.

- [ ] Build and install only an ephemeral local debug candidate.
- [ ] Capture short/long/multiple title, margins, orientation, QCF, masks, Itqan, and Sabqi states.
- [ ] Verify title baseline is at least 4dp above icons and QCF geometry/font remains unchanged.
- [ ] Keep title-bearing captures private and report only hashes/verdicts publicly.
- [ ] Commit and push CP-05-M5 with honest BOOX status.

### Task 6: M6 storage/compliance decision

**Files:**
- Create: `docs/work-checkpoints/CP-06-M6.md`
- Modify network/storage architecture only after explicit user approval.

**Interfaces:**
- Consumes: official current Quran Foundation terms, written authorization if supplied, and offline product constraints.
- Produces: an evidence-backed A/B decision or `BLOQUÉ — DÉCISION UTILISATEUR`.

- [ ] Verify current official terms for durable regular-API storage and Content Sync tafsir support.
- [ ] Compare written-authorization and Content Sync options, including expiry, resync, no-network, state safety, and BOOX impact.
- [ ] Stop for user validation before introducing Internet, account, weekly sync, or loss of permanent offline behavior.
- [ ] If approved, write failing tests before implementation and run the full regression suite.
- [ ] Commit and push CP-06-M6 with the decision and unresolved limits.

### Task 7: M7 handover only

**Files:**
- Create: `docs/work-checkpoints/HANDOVER_WORK_HIGH_M7.md`

**Interfaces:**
- Consumes: CP-01 through CP-06 and same-SHA Action runs.
- Produces: a public source-safe package for independent Work High READ-ONLY review.

- [ ] List the full SHA/run chronology and all PASS/FAIL/NON_EXECUTE outcomes.
- [ ] Record the seven initial P0 proof gaps, private annex hashes, icon/freeze status, BOOX limits, and pending user decisions.
- [ ] Push the handover without producing an APK, release, or Medium GO verdict.

