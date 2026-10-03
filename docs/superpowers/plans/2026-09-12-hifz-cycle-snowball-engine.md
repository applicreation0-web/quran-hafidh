# Quran Hifz Cycle + Snowball Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the mixed Murajaah A/B runtime with the approved weekly engine: cyclic Itqan order plus snowball corpus growth, fixed independent sessions, schema-v3 persistence, and truthful dashboard/runtime agreement.

**Architecture:** Keep the canonical Mushaf/reader untouched. Put schedule and cyclic-order rules in the pure `hifz-core` domain, persistence/consolidation state in `HifzPrefs`, session execution in `HifzSessionActivity`, and projection/navigation in `WeeklyDashboardPlanner`/`MainActivity`. New promoted Sabqi joins the Itqan work corpus in canonical position without jumping `itqanCursor`; it becomes newly eligible for global Murajaah only after its natural-cycle ×40 is validated.

**Tech Stack:** Kotlin/JVM domain core, Java Android app, SharedPreferences/JSON persistence, JUnit/Kotlin test, GitHub Actions Gradle CI.

**Spec:** `docs/superpowers/specs/2026-09-12-hifz-cycle-snowball-engine-design.md`

## Global Constraints

- Branch: `release/hifz-0.7.3-final`.
- No publication or merge.
- Preserve package `com.quransafeguard.hifz`, versionCode 10, versionName `0.7.3-boox`.
- Preserve 604 canonical Mushaf assets, Tafsir assets, audio delivery boundary, random cell masking, verse rosettes, and BOOX vertical-fit contract.
- No priority `pendingPromotedItqan` queue; cycle order is authoritative.
- No manual daily cursor management. Only `itqanRotationStart` remains user-selectable; current cursors are read-only status.
- No transfer of minutes between session types.

---

### Task 1: Pure weekly schedule and anchored cyclic corpus order

**Files:**
- Modify: `hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt`
- Modify/Test: `hifz-core/src/test/kotlin/com/quransafeguard/hifz/core/HifzCoreTest.kt`

**Interfaces:**
- Produce `SessionKind`, `DailyPlan`, `HifzSchedule.planFor(day)`, and `EligibleCorpus.nextAnchored(current, anchor)`.
- Morning/evening plan: Mon/Wed/Fri SABQI_NEW + SABQI_TODAY_REVIEW; Tue/Thu ITQAN + OLD_ITQAN_MURAJAAH; Sat/Sun RECENT_SABQI_REVIEW + OLD_ITQAN_MURAJAAH.

- [ ] Add failing tests for all seven day plans, fixed duration metadata, and anchor order `49:1 -> ... -> 114:6 -> earlier eligible ranges -> ... -> anchor`.
- [ ] Run `gradle --no-daemon :hifz-core:test` and confirm RED for missing plan/cycle behavior.
- [ ] Implement the minimum domain API.
- [ ] Re-run `:hifz-core:test` and confirm GREEN.

### Task 2: Schema-v3 snowball/consolidation persistence

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java`
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- `SCHEMA_VERSION = 3`.
- `itqanWorkCorpus()` = base Itqan + all promoted ranges.
- `murajaahCorpus()` = base Itqan + consolidated promoted ranges + migration-compatibility historical ranges.
- `unconsolidatedPromotedRanges()` durable/idempotent.
- New promotion adds to `promotedRanges` and `unconsolidatedPromotedRanges` atomically without cursor movement.
- v2->v3 migration preserves cursors/active units and copies historical promoted ranges to both unconsolidated state and compatibility Murajaah visibility.

- [ ] Add source-contract tests that require schema 3, explicit v2->v3 migration, separate work/Murajaah corpora, and no `pendingPromotedItqan` priority queue.
- [ ] Run `:hifz-app:testDebugUnitTest` and confirm RED.
- [ ] Implement schema-v3 migration and corpus/consolidation APIs.
- [ ] Re-run unit tests and confirm GREEN.

### Task 3: Runtime sessions and fixed timers

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java`
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/PreviewConfigTest.java`
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- Internal modes: `SABQI`, `SABQI_TODAY_REVIEW`, `ITQAN`, `RECENT_SABQI_REVIEW`, `MURAJAAH`.
- Fixed targets: same-day Sabqi 30m; Tue/Thu Murajaah 60m; Sat/Sun recent Sabqi 30m; Sat/Sun old-Itqan Murajaah 30m.
- `MURAJAAH` contains old consolidated Itqan only; remove A/B transfer logic.
- Weekend recent review loops modulo `recentSabqi.size()` until 30m, including one-item loops.
- Itqan completion advances with anchored cycle and marks any covered unconsolidated promoted material consolidated.

- [ ] Replace obsolete one-pass recent-Murajaah test with fixed-duration/no-transfer tests and source/runtime contracts.
- [ ] Run app tests and confirm RED.
- [ ] Implement modes, independent elapsed keys/completion keys, loop behavior, old-only Murajaah, anchored Itqan advance, and consolidation on ×40 validation.
- [ ] Run app tests and confirm GREEN.

### Task 4: Today/dashboard/settings interface alignment

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/DashboardLedger.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java`
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- Today opens the first incomplete block in the day plan; morning/evening completion are independent.
- Dashboard uses exact daily plan and fixed durations.
- No new action buttons for cursor management.
- `itqanRotationStart` remains editable; `itqanCursor` and `murajaahCursor` are muted/read-only status text.

- [ ] Add failing interface/source contracts for exact morning/evening labels, no cursor reposition actions, and read-only cursor status.
- [ ] Run app tests and confirm RED.
- [ ] Implement planner/launcher/ledger/settings alignment.
- [ ] Run app tests and confirm GREEN.

### Task 5: Regression, adversarial audit, candidate handoff

**Files:**
- Modify only test/contract files if an audit gap is found.
- Create: `docs/audit/HIFZ_0.7.3_WORK_AUDIT_HANDOFF.md`

- [ ] Run full CI-equivalent suite: `:hifz-core:test`, `:hifz-app:testDebugUnitTest`, product boundary, cosmetic contract, convergence rules, mask/vertical scripts, release assemble/inspect.
- [ ] Compare final SHA against `0d1e94779b0a85eda0cfe189272788efddfde9e1`; enumerate every changed path and prove no true-word coordinate dependency, no unexpected Mushaf/Tafsir/audio changes.
- [ ] Adversarially inspect hidden time transfers, cursor mutation/reset, migration idempotency, active-unit restart, one-block recent loop, empty recent state, high/low speed, dashboard/runtime divergence, and morning/evening completion collision.
- [ ] If a defect is found, add a failing regression test first, fix minimally, rerun full verification.
- [ ] Write Work audit handoff with exact final SHA, CI run/artifact IDs, hashes/evidence, changed files, remaining physical BOOX crop check, and explicit NO-PUBLISH gate.
